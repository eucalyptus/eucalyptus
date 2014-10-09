/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.cloudformation.resources.standard.actions;


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
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.auth.principal.User;
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
import com.eucalyptus.cloudformation.workflow.StackActivity;
import com.eucalyptus.cloudformation.workflow.steps.MultiStepWithRetryCreatePromise;
import com.eucalyptus.cloudformation.workflow.steps.MultiStepWithRetryDeletePromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.netflix.glisten.WorkflowOperations;

import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSS3BucketResourceAction extends ResourceAction {

  private AWSS3BucketProperties properties = new AWSS3BucketProperties();
  private AWSS3BucketResourceInfo info = new AWSS3BucketResourceInfo();

  public AWSS3BucketResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }
  }

  private enum CreateSteps implements Step {
    CREATE_BUCKET {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSS3BucketResourceAction action = (AWSS3BucketResourceAction) resourceAction;
        URI serviceURI = ServiceUris.remotePublicify(ObjectStorage.class);
        User user = Accounts.lookupUserById(action.getResourceInfo().getEffectiveUserId());
        final EucaS3Client s3c = EucaS3ClientFactory.getEucaS3Client(user);
        String bucketName = action.properties.getBucketName() != null ? action.properties.getBucketName() : action.getDefaultPhysicalResourceId(63).toLowerCase();
        if (s3c.doesBucketExist(bucketName)) {
          throw new Exception("Bucket " + bucketName + " exists");
        }
        s3c.createBucket(bucketName);
        action.info.setPhysicalResourceId(bucketName);
        return action;
      }

      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
      }
    },
    ADD_BUCKET_STUFF {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSS3BucketResourceAction action = (AWSS3BucketResourceAction) resourceAction;
        URI serviceURI = ServiceUris.remotePublicify(ObjectStorage.class);
        User user = Accounts.lookupUserById(action.getResourceInfo().getEffectiveUserId());
        final EucaS3Client s3c = EucaS3ClientFactory.getEucaS3Client(user);
        String bucketName = action.info.getPhysicalResourceId();
        if (action.properties.getAccessControl() != null) {
          s3c.setBucketAcl(bucketName, CannedAccessControlList.valueOf(action.properties.getAccessControl()));
        }
        if (action.properties.getCorsConfiguration() != null) {
          s3c.setBucketCrossOriginConfiguration(bucketName, action.convertCrossOriginConfiguration(action.properties.getCorsConfiguration()));
        }
        if (action.properties.getLifecycleConfiguration() != null) {
          s3c.setBucketLifecycleConfiguration(bucketName, action.convertLifecycleConfiguration(action.properties.getLifecycleConfiguration()));
        }
        if (action.properties.getLoggingConfiguration() != null) {
          s3c.setBucketLoggingConfiguration(action.convertLoggingConfiguration(bucketName, action.properties.getLoggingConfiguration()));
        }
        if (action.properties.getNotificationConfiguration() != null) {
          s3c.setBucketNotificationConfiguration(bucketName, action.convertNotificationConfiguration(action.properties.getNotificationConfiguration()));
        }
        List<CloudFormationResourceTag> tags = TagHelper.getCloudFormationResourceStackTags(action.info, action.getStackEntity());
        if (action.properties.getTags() != null && !action.properties.getTags().isEmpty()) {
          TagHelper.checkReservedCloudFormationResourceTemplateTags(action.properties.getTags());
          tags.addAll(action.properties.getTags()); // TODO: can we do aws: tags?
        }
        s3c.setBucketTaggingConfiguration(bucketName, action.convertTags(tags));

        if (action.properties.getVersioningConfiguration() != null) {
          s3c.setBucketVersioningConfiguration(action.convertVersioningConfiguration(bucketName, action.properties.getVersioningConfiguration()));
        }
        // TODO: website configuration throws an error if called (currently)
        if (action.properties.getWebsiteConfiguration() != null) {
          s3c.setBucketWebsiteConfiguration(bucketName, action.convertWebsiteConfiguration(action.properties.getWebsiteConfiguration()));
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

      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
      }
    }

  }

  private enum DeleteSteps implements Step {
    KICK_BUCKET {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSS3BucketResourceAction action = (AWSS3BucketResourceAction) resourceAction;
        if (action.info.getPhysicalResourceId() == null) return action;
        User user = Accounts.lookupUserById(action.getResourceInfo().getEffectiveUserId());
        final EucaS3Client s3c = EucaS3ClientFactory.getEucaS3Client(user);
        s3c.deleteBucket(action.info.getPhysicalResourceId());
        return action;
      }

      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
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

  private BucketWebsiteConfiguration convertWebsiteConfiguration(S3WebsiteConfiguration websiteConfiguration) {
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

  private SetBucketVersioningConfigurationRequest convertVersioningConfiguration(String bucketName, S3VersioningConfiguration versioningConfiguration) {
    BucketVersioningConfiguration bucketVersioningConfiguration = new BucketVersioningConfiguration(versioningConfiguration.getStatus());
    return new SetBucketVersioningConfigurationRequest(bucketName, bucketVersioningConfiguration);
  }

  private BucketTaggingConfiguration convertTags(List<CloudFormationResourceTag> tags) {
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

  private BucketNotificationConfiguration convertNotificationConfiguration(S3NotificationConfiguration notificationConfiguration) {
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

  private SetBucketLoggingConfigurationRequest convertLoggingConfiguration(String bucketName, S3LoggingConfiguration loggingConfiguration) {
    BucketLoggingConfiguration bucketLoggingConfiguration = new BucketLoggingConfiguration();
    bucketLoggingConfiguration.setDestinationBucketName(loggingConfiguration.getDestinationBucketName());
    bucketLoggingConfiguration.setLogFilePrefix(loggingConfiguration.getLogFilePrefix());
    return new SetBucketLoggingConfigurationRequest(bucketName, bucketLoggingConfiguration);
  }

  private BucketLifecycleConfiguration convertLifecycleConfiguration(S3LifecycleConfiguration lifecycleConfiguration) throws AuthenticationException {
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

  private BucketCrossOriginConfiguration convertCrossOriginConfiguration(S3CorsConfiguration corsConfiguration) {
    BucketCrossOriginConfiguration bucketCrossOriginConfiguration = new BucketCrossOriginConfiguration();
    if (corsConfiguration.getCorsRule() != null) {
      List<CORSRule> rules = Lists.newArrayList();
      for (S3CorsConfigurationRule s3CorsConfigurationRule: corsConfiguration.getCorsRule()) {
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

  @Override
  public Promise<String> getCreatePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(CreateSteps.values()), StepTransform.INSTANCE);
    return new MultiStepWithRetryCreatePromise(workflowOperations, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId);
  }

  @Override
  public Promise<String> getDeletePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(DeleteSteps.values()), StepTransform.INSTANCE);
    return new MultiStepWithRetryDeletePromise(workflowOperations, stepIds, this).getDeletePromise(resourceId, stackId, accountId, effectiveUserId);
  }

}


