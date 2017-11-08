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


import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.eucalyptus.cloudformation.Limits;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.bootstrap.CloudFormationAWSCredentialsProvider;
import com.eucalyptus.cloudformation.entity.SignalEntity;
import com.eucalyptus.cloudformation.entity.SignalEntityManager;
import com.eucalyptus.cloudformation.entity.StackEventEntityManager;
import com.eucalyptus.cloudformation.entity.StackResourceEntity;
import com.eucalyptus.cloudformation.entity.StackResourceEntityManager;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSCloudFormationWaitConditionHandleResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.info.AWSCloudFormationWaitConditionResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSCloudFormationWaitConditionProperties;
import com.eucalyptus.cloudformation.template.CreationPolicy;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.workflow.ResourceFailureException;
import com.eucalyptus.cloudformation.workflow.RetryAfterConditionCheckFailedException;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;
import com.eucalyptus.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.eucalyptus.cloudformation.Limits.DEFAULT_MAX_LENGTH_WAIT_CONDITION_SIGNAL;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSCloudFormationWaitConditionResourceAction extends StepBasedResourceAction {
  private static final Logger LOG = Logger.getLogger(AWSCloudFormationWaitConditionResourceAction.class);
  private AWSCloudFormationWaitConditionProperties properties = new AWSCloudFormationWaitConditionProperties();
  private AWSCloudFormationWaitConditionResourceInfo info = new AWSCloudFormationWaitConditionResourceInfo();

  public AWSCloudFormationWaitConditionResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), null, null);
  }

  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSCloudFormationWaitConditionProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSCloudFormationWaitConditionResourceInfo) resourceInfo;
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    return UpdateType.UNSUPPORTED;
  }
  private static class BucketAndKey {
    String bucket;
    String key;

    public BucketAndKey(String bucket, String key) {
      this.bucket = bucket;
      this.key = key;
    }

    public String getKey() {
      return key;
    }

    public String getBucket() {
      return bucket;
    }
  }

  private static BucketAndKey getBucketAndKey(AWSCloudFormationWaitConditionResourceAction action) throws Exception {
    LOG.trace("Looking for handle : " + action.properties.getHandle());
    List<StackResourceEntity> stackResourceEntityList = StackResourceEntityManager.getStackResources(action.getStackEntity().getStackId(), action.info.getAccountId(), action.getStackEntity().getStackVersion());
    StackResourceEntity handleEntity = null;
    for (StackResourceEntity stackResourceEntity : stackResourceEntityList) {
      if (stackResourceEntity.getPhysicalResourceId() != null && stackResourceEntity.getPhysicalResourceId().equals(action.properties.getHandle())) {
        LOG.trace("found something with the same physical id, type:" + stackResourceEntity.getResourceType());
        if (stackResourceEntity.getResourceType().equals("AWS::CloudFormation::WaitConditionHandle")) {
          handleEntity = stackResourceEntity;
          break;
        }
      }
    }
    if (handleEntity == null) {
      throw new Exception("Handle URL:" + action.properties.getHandle() + " does not match a WaitConditionHandle from this stack");
    }
    AWSCloudFormationWaitConditionHandleResourceInfo handleResourceInfo = (AWSCloudFormationWaitConditionHandleResourceInfo) StackResourceEntityManager.getResourceInfo(handleEntity);
    ObjectNode objectNode = (ObjectNode) JsonHelper.getJsonNodeFromString(handleResourceInfo.getEucaParts());
    if (!"1.0".equals(objectNode.get("version").asText())) throw new Exception("Invalid version for eucaParts");
    String bucketName = objectNode.get("bucket").asText();
    LOG.trace("bucketName=" + bucketName);
    String keyName = objectNode.get("key").asText();
    return new BucketAndKey(bucketName, keyName);
  }

  private enum CreateSteps implements Step {
    INIT_CREATE_WAIT_CONDITION {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSCloudFormationWaitConditionResourceAction action = (AWSCloudFormationWaitConditionResourceAction) resourceAction;
        CreationPolicy creationPolicy = CreationPolicy.parse(action.info.getCreationPolicyJson());
        if (creationPolicy != null && creationPolicy.getResourceSignal() != null) {
          action.info.setPhysicalResourceId(action.getDefaultPhysicalResourceId());
        } else {
          BucketAndKey bucketAndKey = getBucketAndKey(action);
          String bucketName = bucketAndKey.getBucket();
          String keyName = bucketAndKey.getKey();;
          action.info.setPhysicalResourceId(keyName);
        }
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        action.info.setEucaCreateStartTime(JsonHelper.getStringFromJsonNode(new TextNode("" + System.currentTimeMillis())));
        return action;
      }
      @Nullable
      @Override
      public Integer getTimeout() {
        return null;
      }
    },
    CHECK_SIGNALS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        LOG.trace("Checking for signals");
        AWSCloudFormationWaitConditionResourceAction action = (AWSCloudFormationWaitConditionResourceAction) resourceAction;
        CreationPolicy creationPolicy = CreationPolicy.parse(action.info.getCreationPolicyJson());
        if (creationPolicy != null && creationPolicy.getResourceSignal() != null) {
          // check for signals
          Collection<SignalEntity> signals = SignalEntityManager.getSignals(action.getStackEntity().getStackId(), action.info.getAccountId(), action.info.getLogicalResourceId(),
            action.getStackEntity().getStackVersion());
          int numSuccessSignals = 0;
          if (signals != null) {
            for (SignalEntity signal : signals) {
              if (signal.getStatus() == SignalEntity.Status.FAILURE) {
                throw new ResourceFailureException("Received FAILURE signal with UniqueId " + signal.getUniqueId());
              }
              if (!signal.getProcessed()) {
                StackEventEntityManager.addSignalStackEvent(signal);
                signal.setProcessed(true);
                SignalEntityManager.updateSignal(signal);
              }
              numSuccessSignals++;
            }
          }
          if (numSuccessSignals < creationPolicy.getResourceSignal().getCount()) {
            long durationMs = System.currentTimeMillis() - Long.valueOf(JsonHelper.getJsonNodeFromString(action.info.getEucaCreateStartTime()).asText());
            if (TimeUnit.MILLISECONDS.toSeconds(durationMs) > creationPolicy.getResourceSignal().getTimeout()) {
              throw new ResourceFailureException("Failed to receive " + creationPolicy.getResourceSignal().getCount() + " resource signal(s) within the specified duration");
            }
            throw new RetryAfterConditionCheckFailedException("Not enough success signals yet");
          }
          ObjectNode dataNode = JsonHelper.createObjectNode();
          action.info.setData(JsonHelper.getStringFromJsonNode(new TextNode(dataNode.toString())));
          return action;
        } else {
          if (action.properties.getTimeout() == null) {
            throw new ValidationErrorException("Timeout is a required field");
          }
          if (action.properties.getHandle() == null) {
            throw new ValidationErrorException("Handle is a required field");
          }
          int numSignals = action.properties.getCount() != null && action.properties.getCount() > 0 ? action.properties.getCount() : 1;
          LOG.trace("num signals = " + numSignals);
          if (action.properties.getTimeout() > 43200) {
            throw new ValidationErrorException("timeout can not be more than 43200");
          }
          LOG.trace("Timeout = " + action.properties.getTimeout());
          BucketAndKey bucketAndKey = getBucketAndKey(action);
          String bucketName = bucketAndKey.getBucket();
          String keyName = bucketAndKey.getKey();;
          boolean foundFailure = false;
          final Map<String, String> dataMap = Maps.newHashMap();
          try (final EucaS3Client s3c = EucaS3ClientFactory.getEucaS3Client(new CloudFormationAWSCredentialsProvider())) {
            LOG.trace("Handle:" + action.properties.getHandle());
            VersionListing versionListing = s3c.listVersions(bucketName, "");
            LOG.trace("Found " + versionListing.getVersionSummaries() + " versions to check");
            for (S3VersionSummary versionSummary : versionListing.getVersionSummaries()) {
              LOG.trace("Key:" + versionSummary.getKey());
              if (!versionSummary.getKey().equals(keyName)) {
                continue;
              }
              LOG.trace("Getting version: " + versionSummary.getVersionId());
              try {
                GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, keyName, versionSummary.getVersionId());
                S3Object s3Object = s3c.getObject(getObjectRequest);
                JsonNode jsonNode = null;
                try (S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent()) {
                  long maxLength = DEFAULT_MAX_LENGTH_WAIT_CONDITION_SIGNAL;
                  try {
                    maxLength = Long.parseLong(System.getProperty("cloudformation.max_length_wait_condition_signal"));
                  } catch (Exception ignore) {
                  }
                  jsonNode = Json.parse(ByteStreams.limit(s3ObjectInputStream, maxLength));
                }
                if (!jsonNode.isObject()) {
                  LOG.trace("Read object, json but not object..skipping file");
                  continue;
                }
                ObjectNode localObjectNode = (ObjectNode) jsonNode;
                String status = localObjectNode.get("Status").asText();
                if (status == null) {
                  LOG.trace("Null status, skipping");
                  continue;
                }
                String data = localObjectNode.get("Data").asText();
                if (data == null) {
                  LOG.trace("Null data, skipping");
                  continue;
                }
                String uniqueId = localObjectNode.get("UniqueId").asText();
                if (uniqueId == null) {
                  LOG.trace("Null uniqueId, skipping");
                  continue;
                }
                if ("FAILURE".equals(status)) {
                  foundFailure = true;
                  LOG.trace("found failure, gonna die");
                  break;
                } else if (!"SUCCESS".equals(status)) {
                  LOG.trace("weird status...skipping");
                  continue;
                } else {
                  LOG.trace("found success, uniqueId=" + uniqueId);
                  dataMap.put(uniqueId, data);
                }
              } catch (Exception ex) {
                LOG.error(ex, ex);
                LOG.trace("Exception while going through the objects, will skip this one.");
              }
            }
          }
          if (foundFailure) {
            throw new ResourceFailureException("Found failure signal");
          }
          LOG.trace("Have " + dataMap.size() + " success signals, need " + numSignals);
          if (dataMap.size() >= numSignals) {
            LOG.trace("Success");
            ObjectNode dataNode = JsonHelper.createObjectNode();
            for (String uniqueId : dataMap.keySet()) {
              dataNode.put(uniqueId, dataMap.get(uniqueId));
            }
            action.info.setData(JsonHelper.getStringFromJsonNode(new TextNode(dataNode.toString())));
            return action;
          } else {
            long durationMs = System.currentTimeMillis() - Long.valueOf(JsonHelper.getJsonNodeFromString(action.info.getEucaCreateStartTime()).asText());
            if (TimeUnit.MILLISECONDS.toSeconds(durationMs) > action.properties.getTimeout()) {
              throw new ResourceFailureException("Timeout exeeded waiting for success signals");
            }
            throw new RetryAfterConditionCheckFailedException("Not enough success signals yet");
          }
        }
      }
      @Nullable
      @Override
      public Integer getTimeout() {
        return (int) TimeUnit.HOURS.toSeconds(12);
      }
    };

  }

  private enum DeleteSteps implements Step {
    DO_NOTHING {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        return resourceAction; // nothing to do really
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

}


