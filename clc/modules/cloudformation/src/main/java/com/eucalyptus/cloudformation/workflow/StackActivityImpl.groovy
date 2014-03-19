package com.eucalyptus.cloudformation.workflow

import com.eucalyptus.cloudformation.StackEvent
import com.eucalyptus.cloudformation.entity.StackEntity
import com.eucalyptus.cloudformation.entity.StackEntityHelper
import com.eucalyptus.cloudformation.entity.StackEntityManager
import com.eucalyptus.cloudformation.entity.StackEventEntityManager
import com.eucalyptus.cloudformation.entity.StackResourceEntity
import com.eucalyptus.cloudformation.entity.StackResourceEntityManager
import com.eucalyptus.cloudformation.resources.ResourceAction
import com.eucalyptus.cloudformation.resources.ResourceInfo
import com.eucalyptus.cloudformation.resources.ResourcePropertyResolver
import com.eucalyptus.cloudformation.resources.ResourceResolverManager
import com.eucalyptus.cloudformation.template.FunctionEvaluation
import com.eucalyptus.cloudformation.template.IntrinsicFunctions
import com.eucalyptus.cloudformation.template.JsonHelper
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.netflix.glisten.ActivityOperations
import com.netflix.glisten.impl.swf.SwfActivityOperations
import org.apache.log4j.Logger

/**
 * Created by ethomas on 2/18/14.
 */
public class StackActivityImpl implements StackActivity{
  @Delegate
  ActivityOperations activityOperations = new SwfActivityOperations();

  private static final Logger LOG = Logger.getLogger(StackActivityImpl.class);
  @Override
  public String createGlobalStackEvent(String stackId, String accountId, String resourceStatus, String resourceStatusReason) {
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    String stackName = stackEntity.getStackName();
    StackEvent stackEvent = new StackEvent();
    stackEvent.setStackId(stackId);
    stackEvent.setStackName(stackName);
    stackEvent.setLogicalResourceId(stackName);
    stackEvent.setPhysicalResourceId(stackId);
    stackEvent.setEventId(UUID.randomUUID().toString()); //TODO: AWS has a value related to stack id. (I think)
    ObjectNode properties = new ObjectMapper().createObjectNode();
    for (StackEntity.Parameter parameter: StackEntityHelper.jsonToParameters(stackEntity.getParametersJson())) {
      properties.put(parameter.getKey(), parameter.getStringValue());
    }
    stackEvent.setResourceProperties(JsonHelper.getStringFromJsonNode(properties));
    stackEvent.setResourceType("AWS::CloudFormation::Stack");
    stackEvent.setResourceStatus(resourceStatus);
    stackEvent.setResourceStatusReason(resourceStatusReason);
    stackEvent.setTimestamp(new Date());
    StackEventEntityManager.addStackEvent(stackEvent, accountId);
    // Good to update the global stack too
    StackEntity.Status status = StackEntity.Status.valueOf(resourceStatus);
    stackEntity.setStackStatus(status);
    stackEntity.setStackStatusReason(resourceStatusReason);
    if ((status == StackEntity.Status.DELETE_IN_PROGRESS) && (stackEntity.getDeleteOperationTimestamp() == null)) {
      stackEntity.setDeleteOperationTimestamp(new Date()); // AWS only records the first delete attempt timestamp
    }
    StackEntityManager.updateStack(stackEntity);
    return ""; // promiseFor() doesn't work on void return types
  }

  @Override
  public String createResource(String resourceId, String stackId, String accountId, String effectiveUserId, String reverseDependentResourcesJson) {
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId);
    ArrayList<String> reverseDependentResourceIds =  (reverseDependentResourcesJson == null) ? new ArrayList<String>()
      : (ArrayList<String>) new ObjectMapper().readValue(reverseDependentResourcesJson, new TypeReference<ArrayList<String>>(){})
    Map<String, ResourceInfo> resourceInfoMap = Maps.newLinkedHashMap();
    for (String reverseDependentResourceId: reverseDependentResourceIds) {
      resourceInfoMap.put(reverseDependentResourceId, StackResourceEntityManager.getResourceInfo(stackId, accountId, reverseDependentResourceId));
    }
    ObjectNode returnNode = new ObjectMapper().createObjectNode();
    returnNode.put("resourceId", resourceId);
    String stackName = stackEntity.getStackName();
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    if (!resourceInfo.getAllowedByCondition()) {
      return JsonHelper.getStringFromJsonNode(returnNode);
    };
    // Finally evaluate all properties
    if (resourceInfo.getPropertiesJson() != null) {
      JsonNode propertiesJsonNode = JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson());
      List<String> propertyKeys = Lists.newArrayList(propertiesJsonNode.fieldNames());
      for (String propertyKey: propertyKeys) {
        JsonNode evaluatedPropertyNode = FunctionEvaluation.evaluateFunctions(propertiesJsonNode.get(propertyKey), stackEntity, resourceInfoMap);
        if (IntrinsicFunctions.NO_VALUE.evaluateMatch(evaluatedPropertyNode).isMatch()) {
          ((ObjectNode) propertiesJsonNode).remove(propertyKey);
        } else {
          ((ObjectNode) propertiesJsonNode).put(propertyKey, evaluatedPropertyNode);
        }
      }
      resourceInfo.setPropertiesJson(JsonHelper.getStringFromJsonNode(propertiesJsonNode));
    }
    ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
    resourceAction.setStackEntity(stackEntity);
    resourceInfo.setEffectiveUserId(effectiveUserId);
    resourceAction.setResourceInfo(resourceInfo);
    ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()));
    StackEvent stackEvent = new StackEvent();
    stackEvent.setStackId(stackId);
    stackEvent.setStackName(stackName);
    stackEvent.setLogicalResourceId(resourceInfo.getLogicalResourceId());
    stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
    stackEvent.setEventId(resourceInfo.getLogicalResourceId() + "-" + StackResourceEntity.Status.CREATE_IN_PROGRESS.toString() + "-" + System.currentTimeMillis()); //TODO: see if this really matches
    stackEvent.setResourceProperties(resourceInfo.getPropertiesJson());
    stackEvent.setResourceType(resourceInfo.getType());
    stackEvent.setResourceStatus(StackResourceEntity.Status.CREATE_IN_PROGRESS.toString());
    stackEvent.setResourceStatusReason(null);
    stackEvent.setTimestamp(new Date());
    StackEventEntityManager.addStackEvent(stackEvent, accountId);
    stackResourceEntity.setResourceStatus(StackResourceEntity.Status.CREATE_IN_PROGRESS);
    stackResourceEntity.setResourceStatusReason(null);
    stackResourceEntity.setDescription(""); // deal later
    stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
    StackResourceEntityManager.updateStackResource(stackResourceEntity);
    try {
      resourceAction.create();
      resourceInfo.setReady(Boolean.TRUE);
      stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
      stackResourceEntity.setResourceStatus(StackResourceEntity.Status.CREATE_COMPLETE);
      stackResourceEntity.setResourceStatusReason("");
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
      stackEvent.setEventId(resourceInfo.getLogicalResourceId() + "-" + StackResourceEntity.Status.CREATE_COMPLETE.toString() + "-" + System.currentTimeMillis()); //TODO: see if this really matches
      stackEvent.setResourceStatus(StackResourceEntity.Status.CREATE_COMPLETE.toString());
      stackEvent.setResourceStatusReason("");
      stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
      stackEvent.setTimestamp(new Date());
      StackEventEntityManager.addStackEvent(stackEvent, accountId);
      return JsonHelper.getStringFromJsonNode(returnNode);
    } catch (Exception ex) {
      LOG.error(ex, ex);
      stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
      stackResourceEntity.setResourceStatus(StackResourceEntity.Status.CREATE_FAILED);
      stackResourceEntity.setResourceStatusReason(""+ex.getMessage());
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
      stackEvent.setEventId(resourceInfo.getLogicalResourceId() + "-" + StackResourceEntity.Status.CREATE_FAILED.toString() + "-" + System.currentTimeMillis()); //TODO: see if this really matches
      stackEvent.setResourceStatus(StackResourceEntity.Status.CREATE_FAILED.toString());
      stackEvent.setTimestamp(new Date());
      stackEvent.setResourceStatusReason(""+ex.getMessage());
      stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
      StackEventEntityManager.addStackEvent(stackEvent, accountId);
      throw ex;
    }
  }
  @Override
  public String logInfo(String message) {
    LOG.info(message);
    return "";
  }

  @Override
  public String deleteResource(String resourceId, String stackId, String accountId, String effectiveUserId) {
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    String stackName = stackEntity.getStackName();
    ObjectNode returnNode = new ObjectMapper().createObjectNode();
    returnNode.put("resourceId", resourceId);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId);
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    resourceInfo.setEffectiveUserId(effectiveUserId);
    if (stackResourceEntity != null && stackResourceEntity.getResourceStatus() != StackResourceEntity.Status.DELETE_COMPLETE
      && stackResourceEntity.getResourceStatus() != StackResourceEntity.Status.NOT_STARTED) {
      try {
        ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
        resourceAction.setStackEntity(stackEntity);
        resourceAction.setResourceInfo(resourceInfo);
        ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()));
        if (resourceInfo.getDeletionPolicy() == "Retain") {
          StackEvent stackEvent = new StackEvent();
          stackEvent.setStackId(stackId);
          stackEvent.setStackName(stackName);
          stackEvent.setLogicalResourceId(resourceInfo.getLogicalResourceId());
          stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
          stackEvent.setEventId(resourceInfo.getLogicalResourceId() + "-" + StackResourceEntity.Status.DELETE_SKIPPED.toString() + "-" + System.currentTimeMillis()); //TODO: see if this really matches
          stackEvent.setResourceProperties(resourceInfo.getPropertiesJson());
          stackEvent.setResourceType(resourceInfo.getType());
          stackEvent.setResourceStatus(StackResourceEntity.Status.DELETE_SKIPPED.toString());
          stackEvent.setResourceStatusReason(null);
          stackEvent.setTimestamp(new Date());
          StackEventEntityManager.addStackEvent(stackEvent, accountId);
          stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
          stackResourceEntity.setResourceStatus(StackResourceEntity.Status.DELETE_SKIPPED);
          stackResourceEntity.setResourceStatusReason(null);
          StackResourceEntityManager.updateStackResource(stackResourceEntity);
          returnNode.put("status", "failure");
        } else {
          StackEvent stackEvent = new StackEvent();
          stackEvent.setStackId(stackId);
          stackEvent.setStackName(stackName);
          stackEvent.setLogicalResourceId(resourceInfo.getLogicalResourceId());
          stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
          stackEvent.setEventId(resourceInfo.getLogicalResourceId() + "-" + StackResourceEntity.Status.DELETE_IN_PROGRESS.toString() + "-" + System.currentTimeMillis()); //TODO: see if this really matches
          stackEvent.setResourceProperties(resourceInfo.getPropertiesJson());
          stackEvent.setResourceType(resourceInfo.getType());
          stackEvent.setResourceStatus(StackResourceEntity.Status.DELETE_IN_PROGRESS.toString());
          stackEvent.setResourceStatusReason(null);
          stackEvent.setTimestamp(new Date());
          StackEventEntityManager.addStackEvent(stackEvent, accountId);
          stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
          stackResourceEntity.setResourceStatus(StackResourceEntity.Status.DELETE_IN_PROGRESS);
          stackResourceEntity.setResourceStatusReason(null);
          StackResourceEntityManager.updateStackResource(stackResourceEntity);
          resourceAction.delete();
          stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
          stackResourceEntity.setResourceStatus(StackResourceEntity.Status.DELETE_COMPLETE);
          stackResourceEntity.setResourceStatusReason(null);
          StackResourceEntityManager.updateStackResource(stackResourceEntity);
          stackEvent.setEventId(resourceInfo.getLogicalResourceId() + "-" + StackResourceEntity.Status.DELETE_COMPLETE.toString() + "-" + System.currentTimeMillis()); //TODO: see if this really matches
          stackEvent.setResourceStatus(StackResourceEntity.Status.DELETE_COMPLETE.toString());
          stackEvent.setResourceStatusReason(null);
          stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
          stackEvent.setTimestamp(new Date());
          StackEventEntityManager.addStackEvent(stackEvent, accountId);
          returnNode.put("status", "success");
        }
      } catch (Exception ex) {
        LOG.error(ex, ex);
        returnNode.put("status", "failure");
      }
    } else {
      returnNode.put("status", "success"); // already deleted
    }
    return JsonHelper.getStringFromJsonNode(returnNode);
  }

  @Override
  public String finalizeCreateStack(String stackId, String accountId) {
    try {
      StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
      Map<String, ResourceInfo> resourceInfoMap = Maps.newLinkedHashMap();
      for (StackResourceEntity stackResourceEntity: StackResourceEntityManager.getStackResources(stackId, accountId)) {
        resourceInfoMap.put(stackResourceEntity.getLogicalResourceId(), StackResourceEntityManager.getResourceInfo(stackResourceEntity));
      }
      List<StackEntity.Output> outputs = StackEntityHelper.jsonToOutputs(stackEntity.getOutputsJson());
      for (StackEntity.Output output: outputs) {
        output.setReady(true);
        output.setStringValue(FunctionEvaluation.evaluateFunctions(JsonHelper.getJsonNodeFromString(output.getJsonValue()), stackEntity, resourceInfoMap).textValue());
      }
      stackEntity.setOutputsJson(StackEntityHelper.outputsToJson(outputs));
      stackEntity.setStackStatus(StackEntity.Status.CREATE_COMPLETE);
      StackEntityManager.updateStack(stackEntity);
    } catch (Exception e) {
      LOG.error(e,e);
      throw e;
    }
    return ""; // promiseFor() doesn't work on void return types
  }

  @Override
  public String logException(Throwable t) {
    LOG.error(t, t);
    return "";  // promiseFor() doesn't work on void return types
  }

  @Override
  public String deleteAllStackRecords(String stackId, String accountId) {
    StackResourceEntityManager.deleteStackResources(stackId, accountId);
    StackEventEntityManager.deleteStackEvents(stackId, accountId);
    StackEntityManager.deleteStack(stackId, accountId);
    return ""; // promiseFor() doesn't work on void return types
  }
}

