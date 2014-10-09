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


import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.bootstrap.CloudFormationAWSCredentialsProvider;
import com.eucalyptus.cloudformation.entity.StackResourceEntity;
import com.eucalyptus.cloudformation.entity.StackResourceEntityManager;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSCloudFormationWaitConditionHandleResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.info.AWSCloudFormationWaitConditionResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSCloudFormationWaitConditionProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.workflow.ResourceFailureException;
import com.eucalyptus.cloudformation.workflow.StackActivity;
import com.eucalyptus.cloudformation.workflow.ValidationFailedException;
import com.eucalyptus.cloudformation.workflow.steps.MultiStepWithRetryCreatePromise;
import com.eucalyptus.cloudformation.workflow.steps.MultiStepWithRetryDeletePromise;
import com.eucalyptus.cloudformation.workflow.steps.StandardResourceRetryPolicy;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.glisten.WorkflowOperations;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSCloudFormationWaitConditionResourceAction extends ResourceAction {
  private static final Logger LOG = Logger.getLogger(AWSCloudFormationWaitConditionResourceAction.class);
  private AWSCloudFormationWaitConditionProperties properties = new AWSCloudFormationWaitConditionProperties();
  private AWSCloudFormationWaitConditionResourceInfo info = new AWSCloudFormationWaitConditionResourceInfo();

  public AWSCloudFormationWaitConditionResourceAction() {
    CreateStep createStep = new CreateStep();
    createSteps.put(createStep.name(), createStep);
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }
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

  // Special case Create step, requires value from the properties in retry so can not be static
  private class CreateStep implements Step {
    @Override
    public ResourceAction perform(ResourceAction resourceAction) throws Exception {
      LOG.trace("Creating wait condition");
      AWSCloudFormationWaitConditionResourceAction action = (AWSCloudFormationWaitConditionResourceAction) resourceAction;
      int numSignals = action.properties.getCount() != null && action.properties.getCount() > 0 ? action.properties.getCount() : 1;
      LOG.trace("num signals = " + numSignals);
      if (action.properties.getTimeout() > 43200) {
        throw new ValidationErrorException("timeout can not be more than 43200");
      }
      LOG.trace("Timeout = " + action.properties.getTimeout());
      LOG.trace("Looking for handle : " + action.properties.getHandle());
      List<StackResourceEntity> stackResourceEntityList = StackResourceEntityManager.getStackResources(action.getStackEntity().getStackId(), action.info.getAccountId());
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
      if (!"1.0".equals(objectNode.get("version").textValue())) throw new Exception("Invalid version for eucaParts");
      String bucketName = objectNode.get("bucket").textValue();
      LOG.trace("bucketName=" + bucketName);
      String keyName = objectNode.get("key").textValue();
      LOG.trace("keyName=" + bucketName);
      final EucaS3Client s3c = EucaS3ClientFactory.getEucaS3Client(new CloudFormationAWSCredentialsProvider().getCredentials());
      boolean foundFailure = false;
      LOG.trace("Handle:" + action.properties.getHandle());
      VersionListing versionListing = s3c.listVersions(bucketName, "");
      LOG.trace("Found " + versionListing.getVersionSummaries() + " versions to check");
      Map<String, String> dataMap = Maps.newHashMap();
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
            jsonNode = new ObjectMapper().readTree(s3ObjectInputStream);
          }
          if (!jsonNode.isObject()) {
            LOG.trace("Read object, json but not object..skipping file");
            continue;
          }
          ObjectNode localObjectNode = (ObjectNode) jsonNode;
          String status = localObjectNode.get("Status").textValue();
          if (status == null) {
            LOG.trace("Null status, skipping");
            continue;
          }
          String data = localObjectNode.get("Data").textValue();
          if (data == null) {
            LOG.trace("Null data, skipping");
            continue;
          }
          String uniqueId = localObjectNode.get("UniqueId").textValue();
          if (data == null) {
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
      if (foundFailure) {
        throw new ResourceFailureException("Found failure signal");
      }
      LOG.trace("Have " + dataMap.size() + " success signals, need " + numSignals);
      if (dataMap.size() >= numSignals) {
        LOG.trace("Success");
        ObjectNode dataNode = new ObjectMapper().createObjectNode();
        for (String uniqueId : dataMap.keySet()) {
          dataNode.put(uniqueId, dataMap.get(uniqueId));
        }
        action.info.setData(JsonHelper.getStringFromJsonNode(dataNode));
        action.info.setPhysicalResourceId(keyName);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      } else {
        throw new ValidationFailedException("Not enough success signals yet");
      }
    }

    @Override
    public RetryPolicy getRetryPolicy() {
      // This depends on the timeout value of the particular action, this is why this class is not static
      return new StandardResourceRetryPolicy(properties.getTimeout()).getPolicy();
    }

    @Override
    public String name() {
      return "CREATE_WAIT_CONDITION";
    }
  }
  private enum DeleteSteps implements Step {
    DO_NOTHING {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        return resourceAction; // nothing to do really
      }
      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
      }
    }
  }

  @Override
  public Promise<String> getCreatePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.newArrayList(new CreateStep().name());
    return new MultiStepWithRetryCreatePromise(workflowOperations, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId);
  }

  @Override
  public Promise<String> getDeletePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(DeleteSteps.values()), StepTransform.INSTANCE);
    return new MultiStepWithRetryDeletePromise(workflowOperations, stepIds, this).getDeletePromise(resourceId, stackId, accountId, effectiveUserId);
  }
}


