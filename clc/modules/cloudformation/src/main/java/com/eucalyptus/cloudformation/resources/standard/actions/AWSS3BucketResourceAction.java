/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.cloudformation.resources.standard.actions;


import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.BucketLoggingConfiguration;
import com.amazonaws.services.s3.model.BucketNotificationConfiguration;
import com.amazonaws.services.s3.model.BucketTaggingConfiguration;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.BucketWebsiteConfiguration;
import com.amazonaws.services.s3.model.CORSRule;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.RedirectRule;
import com.amazonaws.services.s3.model.RoutingRule;
import com.amazonaws.services.s3.model.RoutingRuleCondition;
import com.amazonaws.services.s3.model.SetBucketLoggingConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.model.TagSet;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.tokens.SecurityTokenAWSCredentialsProvider;
import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.InternalFailureException;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSS3BucketResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSS3BucketProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.CloudFormationResourceTag;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.S3CorsConfiguration;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.S3CorsConfigurationRule;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.S3LifecycleConfiguration;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.S3LifecycleRule;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.S3LoggingConfiguration;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.S3NotificationConfiguration;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.S3NotificationTopicConfiguration;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.S3VersioningConfiguration;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.S3WebsiteConfiguration;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.S3WebsiteConfigurationRoutingRule;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSS3BucketResourceAction extends StepBasedResourceAction {

  private AWSS3BucketProperties properties = new AWSS3BucketProperties();
  private AWSS3BucketResourceInfo info = new AWSS3BucketResourceInfo();

  public AWSS3BucketResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }


  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSS3BucketResourceAction otherAction = (AWSS3BucketResourceAction) resourceAction;
    if (!Objects.equals(properties.getAccessControl(), otherAction.properties.getAccessControl())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getBucketName(), otherAction.properties.getBucketName())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getCorsConfiguration(), otherAction.properties.getCorsConfiguration())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getLifecycleConfiguration(), otherAction.properties.getLifecycleConfiguration())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getLoggingConfiguration(), otherAction.properties.getLoggingConfiguration())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getNotificationConfiguration(), otherAction.properties.getNotificationConfiguration())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getTags(), otherAction.properties.getTags())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getVersioningConfiguration(), otherAction.properties.getVersioningConfiguration())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getWebsiteConfiguration(), otherAction.properties.getWebsiteConfiguration())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getReplicationConfiguration(), otherAction.properties.getReplicationConfiguration())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_BUCKET {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSS3BucketResourceAction action = (AWSS3BucketResourceAction) resourceAction;
        User user = Accounts.lookupPrincipalByUserId(action.getResourceInfo().getEffectiveUserId());
        if ( action.properties.getVersioningConfiguration() != null && action.properties.getLifecycleConfiguration() != null) {
          throw new ValidationErrorException("Unable to set both lifecycle configuration and versioning configuration for buckets");
        }

        try ( final EucaS3Client s3c = EucaS3ClientFactory.getEucaS3Client( new SecurityTokenAWSCredentialsProvider( user ) ) ) {
          String bucketName = action.properties.getBucketName() != null ? action.properties.getBucketName() : action.getDefaultPhysicalResourceId(63).toLowerCase();
          if (s3c.doesBucketExist(bucketName)) {
            throw new Exception("Bucket " + bucketName + " exists");
          }
          s3c.createBucket(bucketName);
          action.info.setPhysicalResourceId(bucketName);
          action.info.setCreatedEnoughToDelete(true);
        }
        return action;
      }
    },
    ADD_BUCKET_STUFF {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSS3BucketResourceAction action = (AWSS3BucketResourceAction) resourceAction;
        URI serviceURI = ServiceUris.remotePublicify(ObjectStorage.class);
        User user = Accounts.lookupPrincipalByUserId(action.getResourceInfo().getEffectiveUserId());
        String bucketName = action.info.getPhysicalResourceId();
        try ( final EucaS3Client s3c = EucaS3ClientFactory.getEucaS3Client( new SecurityTokenAWSCredentialsProvider( user ) ) ) {
          if ( action.properties.getAccessControl() != null ) {
            s3c.setBucketAcl( bucketName, CannedAccessControlList.valueOf( action.properties.getAccessControl() ) );
          }
          if ( action.properties.getCorsConfiguration() != null ) {
            s3c.setBucketCrossOriginConfiguration(bucketName, convertCrossOriginConfiguration(action.properties.getCorsConfiguration()));
          }
          if ( action.properties.getLifecycleConfiguration() != null ) {
            // only allow lifecycle configuration if no versioning exists
            BucketVersioningConfiguration bucketVersioningConfiguration = s3c.getBucketVersioningConfiguration(bucketName);
            if (!"Off".equals(bucketVersioningConfiguration.getStatus())) {
              throw new ValidationErrorException("Unable to set lifecycle configuration on bucket " + bucketName + ", versioning is not Off, it is " + bucketVersioningConfiguration.getStatus());
            }
            s3c.setBucketLifecycleConfiguration(bucketName, convertLifecycleConfiguration(action.properties.getLifecycleConfiguration()));
          }
          if ( action.properties.getLoggingConfiguration() != null ) {
            throw new InternalFailureException("LoggingConfiguration not yet implemented");
//            s3c.setBucketLoggingConfiguration(convertLoggingConfiguration(bucketName, action.properties.getLoggingConfiguration()));
          }
          if ( action.properties.getNotificationConfiguration() != null ) {
            throw new InternalFailureException("NotificationConfiguration not yet implemented");
//            s3c.setBucketNotificationConfiguration(bucketName, convertNotificationConfiguration(action.properties.getNotificationConfiguration()));
          }
          if ( action.properties.getReplicationConfiguration() != null ) {
            throw new InternalFailureException("ReplicationConfiguration not yet implemented");
//            s3c.setBucketReplicationConfiguration(bucketName, convertReplicationConfiguration(action.properties.getReplicationConfiguration()));
          }
          setBucketTags(action, user, bucketName, s3c);

          if ( action.properties.getVersioningConfiguration() != null ) {
            // only allow versioning if no lifecycle configuration exists
            BucketLifecycleConfiguration bucketLifecycleConfiguration = s3c.getBucketLifecycleConfiguration(bucketName);
            if (bucketLifecycleConfiguration.getRules().size() > 0) {
              throw new ValidationErrorException("Unable to set versioning configuration bucket " + bucketName + ".  Lifecycle configuration has been set.");
            }
            s3c.setBucketVersioningConfiguration(convertVersioningConfiguration(bucketName, action.properties.getVersioningConfiguration()));
          }
          // TODO: website configuration throws an error if called (currently)
          if ( action.properties.getWebsiteConfiguration() != null ) {
            throw new InternalFailureException("WebsiteConfiguration not yet implemented");
//            s3c.setBucketWebsiteConfiguration(bucketName, convertWebsiteConfiguration(action.properties.getWebsiteConfiguration()));
          }
        }
        String domainName = null;
        if ((serviceURI.getPath() == null || serviceURI.getPath().replace("/","").isEmpty())) {
          domainName = bucketName + "." + serviceURI.getHost() + (serviceURI.getPort() != -1 ? ":" + serviceURI.getPort() : "");
        } else {
          domainName = serviceURI.getHost() + (serviceURI.getPort() != -1 ? ":" + serviceURI.getPort() : "") + serviceURI.getPath() + "/" + bucketName;
        }
        action.info.setDomainName(JsonHelper.getStringFromJsonNode(new TextNode(domainName)));
        action.info.setWebsiteURL(JsonHelper.getStringFromJsonNode(new TextNode("http://" + domainName)));
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    };
  }

  private enum DeleteSteps implements Step {
    KICK_BUCKET {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSS3BucketResourceAction action = (AWSS3BucketResourceAction) resourceAction;
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        User user = Accounts.lookupPrincipalByUserId(action.getResourceInfo().getEffectiveUserId());
        try ( final EucaS3Client s3c = EucaS3ClientFactory.getEucaS3Client( new SecurityTokenAWSCredentialsProvider( user ) ) ) {
          s3c.deleteBucket(action.info.getPhysicalResourceId());
        } catch (AmazonS3Exception ex) {
          if ("NoSuchBucket".equalsIgnoreCase(ex.getErrorCode())) {
            // do nothing.  (We check existence this way rather than if -> delete due to possible race conditions
          } else {
            throw ex;
          }
        }
        return action;
      }
    }
  }

  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSS3BucketProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSS3BucketResourceInfo) resourceInfo;
  }

  private static BucketWebsiteConfiguration convertWebsiteConfiguration(S3WebsiteConfiguration websiteConfiguration) {
    BucketWebsiteConfiguration bucketWebsiteConfiguration = new BucketWebsiteConfiguration();
    bucketWebsiteConfiguration.setErrorDocument(websiteConfiguration.getErrorDocument());
    bucketWebsiteConfiguration.setIndexDocumentSuffix(websiteConfiguration.getIndexDocument());
    if (websiteConfiguration.getRedirectAllRequestsTo() != null) {
      RedirectRule redirectAllRequestsTo = new RedirectRule();
      redirectAllRequestsTo.setHostName(websiteConfiguration.getRedirectAllRequestsTo().getHostName());
      redirectAllRequestsTo.setProtocol(websiteConfiguration.getRedirectAllRequestsTo().getProtocol());
      bucketWebsiteConfiguration.setRedirectAllRequestsTo(redirectAllRequestsTo);
    }
    if (websiteConfiguration.getRoutingRules() != null) {
      List<RoutingRule> routingRules = Lists.newArrayList();
      for (S3WebsiteConfigurationRoutingRule s3WebsiteConfigurationRoutingRule: websiteConfiguration.getRoutingRules()) {
        RoutingRule routingRule = new RoutingRule();
        if (s3WebsiteConfigurationRoutingRule.getRoutingRuleCondition() != null) {
          RoutingRuleCondition condition = new RoutingRuleCondition();
          condition.setHttpErrorCodeReturnedEquals(s3WebsiteConfigurationRoutingRule.getRoutingRuleCondition().getHttpErrorCodeReturnedEquals());
          condition.setKeyPrefixEquals(s3WebsiteConfigurationRoutingRule.getRoutingRuleCondition().getKeyPrefixEquals());
          routingRule.setCondition(condition);
        }
        if (s3WebsiteConfigurationRoutingRule.getRedirectRule() != null) {
          RedirectRule redirect = new RedirectRule();
          redirect.setReplaceKeyWith(s3WebsiteConfigurationRoutingRule.getRedirectRule().getReplaceKeyWith());
          redirect.setReplaceKeyPrefixWith(s3WebsiteConfigurationRoutingRule.getRedirectRule().getReplaceKeyPrefixWith());
          redirect.setProtocol(s3WebsiteConfigurationRoutingRule.getRedirectRule().getProtocol());
          redirect.setHttpRedirectCode(s3WebsiteConfigurationRoutingRule.getRedirectRule().getHttpRedirectCode());
          redirect.setHostName(s3WebsiteConfigurationRoutingRule.getRedirectRule().getHostName());
          routingRule.setRedirect(redirect);
        }
        routingRules.add(routingRule);
      }
      bucketWebsiteConfiguration.setRoutingRules(routingRules);
    }
    return bucketWebsiteConfiguration;
  }

  private static SetBucketVersioningConfigurationRequest convertVersioningConfiguration(String bucketName, S3VersioningConfiguration versioningConfiguration) {
    BucketVersioningConfiguration bucketVersioningConfiguration = new BucketVersioningConfiguration(versioningConfiguration.getStatus());
    return new SetBucketVersioningConfigurationRequest(bucketName, bucketVersioningConfiguration);
  }

  private static BucketTaggingConfiguration convertTags(List<CloudFormationResourceTag> tags) {
    BucketTaggingConfiguration bucketTaggingConfiguration = new BucketTaggingConfiguration();
    // In theory BucketTaggingConfiguration
    Collection<TagSet> tagSets = Lists.newArrayList();
    TagSet tagSet = new TagSet();
    for (CloudFormationResourceTag cloudformationResourceTag: tags) {
      tagSet.setTag(cloudformationResourceTag.getKey(), cloudformationResourceTag.getValue());
    }
    tagSets.add(tagSet);
    bucketTaggingConfiguration.setTagSets(tagSets);
    return bucketTaggingConfiguration;
  }

  private static BucketNotificationConfiguration convertNotificationConfiguration(S3NotificationConfiguration notificationConfiguration) {
    BucketNotificationConfiguration bucketNotificationConfiguration = new BucketNotificationConfiguration();
    if (notificationConfiguration.getTopicConfigurations() != null) {
      Collection<BucketNotificationConfiguration.TopicConfiguration> topicConfigurations = Lists.newArrayList();
      for (S3NotificationTopicConfiguration s3NotificationTopicConfiguration : notificationConfiguration.getTopicConfigurations()) {
        topicConfigurations.add(new BucketNotificationConfiguration.TopicConfiguration(s3NotificationTopicConfiguration.getTopic(), s3NotificationTopicConfiguration.getEvent()));
      }
      bucketNotificationConfiguration.setTopicConfigurations(topicConfigurations);
    }
    return bucketNotificationConfiguration;
  }

  private static SetBucketLoggingConfigurationRequest convertLoggingConfiguration(String bucketName, S3LoggingConfiguration loggingConfiguration) {
    BucketLoggingConfiguration bucketLoggingConfiguration = new BucketLoggingConfiguration();
    bucketLoggingConfiguration.setDestinationBucketName(loggingConfiguration.getDestinationBucketName());
    bucketLoggingConfiguration.setLogFilePrefix(loggingConfiguration.getLogFilePrefix());
    return new SetBucketLoggingConfigurationRequest(bucketName, bucketLoggingConfiguration);
  }

  private static BucketLifecycleConfiguration convertLifecycleConfiguration(S3LifecycleConfiguration lifecycleConfiguration) throws AuthenticationException {
    BucketLifecycleConfiguration bucketLifecycleConfiguration = new BucketLifecycleConfiguration();
    if (lifecycleConfiguration.getRules() != null) {
      List<BucketLifecycleConfiguration.Rule> rules = Lists.newArrayList();
      for (S3LifecycleRule s3LifecycleRule : lifecycleConfiguration.getRules()) {
        BucketLifecycleConfiguration.Rule rule = new BucketLifecycleConfiguration.Rule();
        rule.setId(s3LifecycleRule.getId());
        if (s3LifecycleRule.getExpirationDate() != null) {
          rule.setExpirationDate(Timestamps.parseIso8601Timestamp(s3LifecycleRule.getExpirationDate()));
        }
        if (s3LifecycleRule.getExpirationInDays() != null) {
          rule.setExpirationInDays(s3LifecycleRule.getExpirationInDays());
        }
        rule.setPrefix(s3LifecycleRule.getPrefix());
        rule.setStatus(s3LifecycleRule.getStatus());
        if (s3LifecycleRule.getTransition() != null) {
          BucketLifecycleConfiguration.Transition transition = new BucketLifecycleConfiguration.Transition();
          if (s3LifecycleRule.getTransition().getStorageClass() != null) {
            transition.setStorageClass(StorageClass.valueOf(s3LifecycleRule.getTransition().getStorageClass()));
          }
          if (s3LifecycleRule.getExpirationDate() != null) {
            transition.setDate(Timestamps.parseIso8601Timestamp(s3LifecycleRule.getExpirationDate()));
          }
          if (s3LifecycleRule.getExpirationInDays() != null) {
            transition.setDays(s3LifecycleRule.getExpirationInDays());
          }
          rule.setTransition(transition);
        }
        rules.add(rule);
      }
      bucketLifecycleConfiguration.setRules(rules);
    }
    return bucketLifecycleConfiguration;
  }

  private static BucketCrossOriginConfiguration convertCrossOriginConfiguration(S3CorsConfiguration corsConfiguration) {
    BucketCrossOriginConfiguration bucketCrossOriginConfiguration = new BucketCrossOriginConfiguration();
    if (corsConfiguration.getCorsRules() != null) {
      List<CORSRule> rules = Lists.newArrayList();
      for (S3CorsConfigurationRule s3CorsConfigurationRule: corsConfiguration.getCorsRules()) {
        CORSRule rule = new CORSRule();
        rule.setAllowedHeaders(s3CorsConfigurationRule.getAllowedHeaders());
        if (s3CorsConfigurationRule.getAllowedMethods() != null) {
          List<CORSRule.AllowedMethods> allowedMethods = Lists.newArrayList();
          for (String allowedMethodStr: s3CorsConfigurationRule.getAllowedMethods()) {
            allowedMethods.add(CORSRule.AllowedMethods.valueOf(allowedMethodStr));
          }
          rule.setAllowedMethods(allowedMethods);
        }
        rule.setAllowedOrigins(s3CorsConfigurationRule.getAllowedOrigins());
        rule.setExposedHeaders(s3CorsConfigurationRule.getExposedHeaders());
        rule.setId(s3CorsConfigurationRule.getId());
        rule.setMaxAgeSeconds(s3CorsConfigurationRule.getMaxAge());
        rules.add(rule);
      }
      bucketCrossOriginConfiguration.setRules(rules);
    }
    return bucketCrossOriginConfiguration;
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_BUCKET_STUFF {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSS3BucketResourceAction oldAction = (AWSS3BucketResourceAction) oldResourceAction;
        AWSS3BucketResourceAction newAction = (AWSS3BucketResourceAction) newResourceAction;
        if ( newAction.properties.getVersioningConfiguration() != null && newAction.properties.getLifecycleConfiguration() != null) {
          throw new ValidationErrorException("Unable to set both lifecycle configuration and versioning configuration for buckets");
        }
        URI serviceURI = ServiceUris.remotePublicify(ObjectStorage.class);
        User user = Accounts.lookupPrincipalByUserId(oldAction.getResourceInfo().getEffectiveUserId());
        String bucketName = oldAction.info.getPhysicalResourceId();
        try (final EucaS3Client s3c = EucaS3ClientFactory.getEucaS3Client(new SecurityTokenAWSCredentialsProvider(user))) {
          if (newAction.properties.getAccessControl() != null) {
            s3c.setBucketAcl(bucketName, CannedAccessControlList.valueOf(newAction.properties.getAccessControl()));
          } else {
            s3c.setBucketAcl(bucketName, CannedAccessControlList.BucketOwnerFullControl);
          }
          if (newAction.properties.getCorsConfiguration() != null) {
            s3c.setBucketCrossOriginConfiguration(bucketName, convertCrossOriginConfiguration(newAction.properties.getCorsConfiguration()));
          } else {
            s3c.deleteBucketCrossOriginConfiguration(bucketName);
          }
          if (newAction.properties.getLifecycleConfiguration() != null) {
            // only set if versioning off
            BucketVersioningConfiguration bucketVersioningConfiguration = s3c.getBucketVersioningConfiguration(bucketName);
            if (!"Off".equals(bucketVersioningConfiguration.getStatus())) {
              throw new ValidationErrorException("Unable to set lifecycle configuration on bucket " + bucketName + ", versioning is not Off, it is " + bucketVersioningConfiguration.getStatus());
            }
            s3c.setBucketLifecycleConfiguration(bucketName, convertLifecycleConfiguration(newAction.properties.getLifecycleConfiguration()));
          } else {
            s3c.deleteBucketLifecycleConfiguration(bucketName);
          }
          if (newAction.properties.getLoggingConfiguration() != null) {
            throw new InternalFailureException("LoggingConfiguration not yet implemented");
//            s3c.setBucketLoggingConfiguration(convertLoggingConfiguration(bucketName, newAction.properties.getLoggingConfiguration()));
//          } else {
//            s3c.setBucketLoggingConfiguration(new SetBucketLoggingConfigurationRequest(bucketName, new BucketLoggingConfiguration()));
          }
          if (newAction.properties.getNotificationConfiguration() != null) {
            throw new InternalFailureException("NotificationConfiguration not yet implemented");
//            s3c.setBucketNotificationConfiguration(bucketName, convertNotificationConfiguration(newAction.properties.getNotificationConfiguration()));
//          } else {
//            s3c.setBucketNotificationConfiguration(bucketName, new BucketNotificationConfiguration());
          }
          if (newAction.properties.getReplicationConfiguration() != null) {
            throw new InternalFailureException("ReplicationConfiguration not yet implemented");
//            s3c.setBucketReplicationConfiguration(bucketName, convertReplicationConfiguration(newAction.properties.getReplicationConfiguration()));
//          } else {
//            s3c.setBucketReplicationConfiguration(bucketName, new BucketReplicationConfiguration());
          }
          setBucketTags(newAction, user, bucketName, s3c);
          if (newAction.properties.getVersioningConfiguration() != null) {
            BucketLifecycleConfiguration bucketLifecycleConfiguration = s3c.getBucketLifecycleConfiguration(bucketName);
            if (bucketLifecycleConfiguration.getRules().size() > 0) {
              throw new ValidationErrorException("Unable to set versioning configuration bucket " + bucketName + ".  Lifecycle configuration has been set.");
            }
            s3c.setBucketVersioningConfiguration(convertVersioningConfiguration(bucketName, newAction.properties.getVersioningConfiguration()));
          } else {
            BucketVersioningConfiguration bucketVersioningConfiguration = s3c.getBucketVersioningConfiguration(bucketName);
            if (bucketVersioningConfiguration == null) {
              throw new InternalFailureException("Unable to get bucketVersioningConfiguration for " + bucketName + " , does bucket exist?");
            }
            // Once a bucket is versioned, Off is never a valid status again, so if the current value is not "Off", and there is no configuration option,
            // use suspended instead.
            if (!bucketVersioningConfiguration.getStatus().equals("Off")) {
              s3c.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest(bucketName, new BucketVersioningConfiguration("Suspended")));
            }
          }

          // TODO: website configuration throws an error if called (currently)
          if (newAction.properties.getWebsiteConfiguration() != null) {
            throw new InternalFailureException("WebsiteConfiguration not yet implemented");
//            s3c.setBucketWebsiteConfiguration(bucketName, convertWebsiteConfiguration(newAction.properties.getWebsiteConfiguration()));
//          } else {
//            s3c.deleteBucketWebsiteConfiguration(bucketName);
          }
          String domainName = null;
          if ((serviceURI.getPath() == null || serviceURI.getPath().replace("/", "").isEmpty())) {
            domainName = bucketName + "." + serviceURI.getHost() + (serviceURI.getPort() != -1 ? ":" + serviceURI.getPort() : "");
          } else {
            domainName = serviceURI.getHost() + (serviceURI.getPort() != -1 ? ":" + serviceURI.getPort() : "") + serviceURI.getPath() + "/" + bucketName;
          }
          newAction.info.setDomainName(JsonHelper.getStringFromJsonNode(new TextNode(domainName)));
          newAction.info.setWebsiteURL(JsonHelper.getStringFromJsonNode(new TextNode("http://" + domainName)));
          newAction.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(newAction.info.getPhysicalResourceId())));
          return newAction;
        }
      }
    }
  }

  private static void setBucketTags(AWSS3BucketResourceAction newAction, User user, String bucketName, EucaS3Client s3c) throws CloudFormationException, AuthException {
    // Dealing with tags has to be done differently than with other resources.
    // 1) there is only the s3c.setBucketTaggingConfiguration(), tags can not be "added", they can only be replaced wholesale.
    //    This means that the user must have the ability to add tags even to use system tags.
    // 2) There is no (current) verification of aws: or euca: prefixes
    // Given the above, and given that we have a few system tags that must be added, we will add tags all at once either as the admin
    // (if no non-system tags exist) or as the user (if non system-tags exist).  If the user version fails due to IAM, it would have done so anyway.
    List<CloudFormationResourceTag> systemTags = TagHelper.getCloudFormationResourceSystemTags(newAction.info, newAction.getStackEntity());
    List<CloudFormationResourceTag> nonSystemTags = TagHelper.getCloudFormationResourceStackTags( newAction.getStackEntity() );
    if ( newAction.properties.getTags() != null && !newAction.properties.getTags().isEmpty() ) {
      TagHelper.checkReservedCloudFormationResourceTemplateTags( newAction.properties.getTags() );
      nonSystemTags.addAll(newAction.properties.getTags());
    }
    if (!nonSystemTags.isEmpty()) { // add as user
      List<CloudFormationResourceTag> allTags = Lists.newArrayList();
      allTags.addAll(systemTags);
      allTags.addAll(nonSystemTags);
      s3c.setBucketTaggingConfiguration( bucketName, convertTags( allTags ) );
    } else { // add as admin
      try ( EucaS3Client s3cAdmin = EucaS3ClientFactory.getEucaS3Client(new SecurityTokenAWSCredentialsProvider(AccountFullName.getInstance(user.getAccountNumber()))) ) {
        s3cAdmin.setBucketTaggingConfiguration( bucketName, AWSS3BucketResourceAction.convertTags( systemTags ));
      }
    }
  }
}



