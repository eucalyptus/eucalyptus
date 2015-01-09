/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.cloudformation.workflow

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow
import com.amazonaws.services.simpleworkflow.model.DescribeWorkflowExecutionRequest
import com.amazonaws.services.simpleworkflow.model.RequestCancelWorkflowExecutionRequest
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionDetail
import com.eucalyptus.cloudformation.CloudFormation
import com.eucalyptus.cloudformation.InternalFailureException
import com.eucalyptus.cloudformation.StackEvent
import com.eucalyptus.cloudformation.ValidationErrorException
import com.eucalyptus.cloudformation.entity.StackEntity
import com.eucalyptus.cloudformation.entity.StackEntityHelper
import com.eucalyptus.cloudformation.entity.StackEntityManager
import com.eucalyptus.cloudformation.entity.StackEventEntityManager
import com.eucalyptus.cloudformation.entity.StackResourceEntity
import com.eucalyptus.cloudformation.entity.StackResourceEntityManager
import com.eucalyptus.cloudformation.entity.StackWorkflowEntity
import com.eucalyptus.cloudformation.entity.StackWorkflowEntityManager
import com.eucalyptus.cloudformation.resources.ResourceAction
import com.eucalyptus.cloudformation.resources.ResourceInfo
import com.eucalyptus.cloudformation.resources.ResourcePropertyResolver
import com.eucalyptus.cloudformation.resources.ResourceResolverManager
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSCloudFormationWaitConditionProperties
import com.eucalyptus.cloudformation.template.FunctionEvaluation
import com.eucalyptus.cloudformation.template.IntrinsicFunctions
import com.eucalyptus.cloudformation.template.JsonHelper
import com.eucalyptus.cloudformation.workflow.steps.Step
import com.eucalyptus.component.annotation.ComponentPart
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.base.Throwables
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.netflix.glisten.ActivityOperations
import com.netflix.glisten.impl.swf.SwfActivityOperations
import groovy.transform.CompileStatic
import org.apache.log4j.Logger

import static com.eucalyptus.cloudformation.entity.StackWorkflowEntity.WorkflowType.CREATE_STACK_WORKFLOW

@ComponentPart(CloudFormation)
@CompileStatic
public class StackActivityImpl implements StackActivity {

  @Delegate
  ActivityOperations activityOperations = new SwfActivityOperations();

  private static final Logger LOG = Logger.getLogger(StackActivityImpl.class);

  @Override
  public String createGlobalStackEvent(String stackId, String accountId, String resourceStatus, String resourceStatusReason) {
    LOG.info("Creating global stack event: " + resourceStatus);
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    String stackName = stackEntity.getStackName();
    StackEvent stackEvent = new StackEvent();
    stackEvent.setStackId(stackId);
    stackEvent.setStackName(stackName);
    stackEvent.setLogicalResourceId(stackName);
    stackEvent.setPhysicalResourceId(stackId);
    stackEvent.setEventId(UUID.randomUUID().toString()); //TODO: AWS has a value related to stack id. (I think)
    ObjectNode properties = new ObjectMapper().createObjectNode();
    for (StackEntity.Parameter parameter : StackEntityHelper.jsonToParameters(stackEntity.getParametersJson())) {
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
    LOG.info("Done creating global stack event: " + resourceStatus);
    return ""; // promiseFor() doesn't work on void return types
  }

  @Override
  public String initCreateResource(String resourceId, String stackId, String accountId, String effectiveUserId, String reverseDependentResourcesJson) {
    LOG.debug("Creating resource " + resourceId);
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId);
    ArrayList<String> reverseDependentResourceIds = (reverseDependentResourcesJson == null) ? new ArrayList<String>()
      : (ArrayList<String>) new ObjectMapper().readValue(reverseDependentResourcesJson, new TypeReference<ArrayList<String>>() {
    })
    Map<String, ResourceInfo> resourceInfoMap = Maps.newLinkedHashMap();
    for (String reverseDependentResourceId : reverseDependentResourceIds) {
      resourceInfoMap.put(reverseDependentResourceId, StackResourceEntityManager.getResourceInfo(stackId, accountId, reverseDependentResourceId));
    }
    ObjectNode returnNode = new ObjectMapper().createObjectNode();
    returnNode.put("resourceId", resourceId);
    String stackName = stackEntity.getStackName();
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    if (!resourceInfo.getAllowedByCondition()) {
      LOG.debug("Resource " + resourceId + " not allowed by condition, skipping");
      return "SKIP"; //TODO: codify this somewhere...
    };
    // Evaluate all properties
    if (resourceInfo.getPropertiesJson() != null) {
      JsonNode propertiesJsonNode = JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson());
      List<String> propertyKeys = Lists.newArrayList(propertiesJsonNode.fieldNames());
      for (String propertyKey : propertyKeys) {
        JsonNode evaluatedPropertyNode = FunctionEvaluation.evaluateFunctions(propertiesJsonNode.get(propertyKey), stackEntity, resourceInfoMap);
        if (IntrinsicFunctions.NO_VALUE.evaluateMatch(evaluatedPropertyNode).isMatch()) {
          ((ObjectNode) propertiesJsonNode).remove(propertyKey);
        } else {
          ((ObjectNode) propertiesJsonNode).put(propertyKey, evaluatedPropertyNode);
        }
      }
      resourceInfo.setPropertiesJson(JsonHelper.getStringFromJsonNode(propertiesJsonNode));
    }
    // Update metadata:
    if (resourceInfo.getMetadataJson() != null) {
      JsonNode metadataJsonNode = JsonHelper.getJsonNodeFromString(resourceInfo.getMetadataJson());
      List<String> metadataKeys = Lists.newArrayList(metadataJsonNode.fieldNames());
      for (String metadataKey : metadataKeys) {
        JsonNode evaluatedMetadataNode = FunctionEvaluation.evaluateFunctions(metadataJsonNode.get(metadataKey), stackEntity, resourceInfoMap);
        if (IntrinsicFunctions.NO_VALUE.evaluateMatch(evaluatedMetadataNode).isMatch()) {
          ((ObjectNode) metadataJsonNode).remove(metadataKey);
        } else {
          ((ObjectNode) metadataJsonNode).put(metadataKey, evaluatedMetadataNode);
        }
      }
      resourceInfo.setMetadataJson(JsonHelper.getStringFromJsonNode(metadataJsonNode));
    }
    // Update update policy:
    if (resourceInfo.getUpdatePolicyJson() != null) {
      JsonNode updatePolicyJsonNode = JsonHelper.getJsonNodeFromString(resourceInfo.getUpdatePolicyJson());
      List<String> updatePolicyKeys = Lists.newArrayList(updatePolicyJsonNode.fieldNames());
      for (String updatePolicyKey : updatePolicyKeys) {
        JsonNode evaluatedUpdatePolicyNode = FunctionEvaluation.evaluateFunctions(updatePolicyJsonNode.get(updatePolicyKey), stackEntity, resourceInfoMap);
        if (IntrinsicFunctions.NO_VALUE.evaluateMatch(evaluatedUpdatePolicyNode).isMatch()) {
          ((ObjectNode) updatePolicyJsonNode).remove(updatePolicyKey);
        } else {
          ((ObjectNode) updatePolicyJsonNode).put(updatePolicyKey, evaluatedUpdatePolicyNode);
        }
      }
      resourceInfo.setUpdatePolicyJson(JsonHelper.getStringFromJsonNode(updatePolicyJsonNode));
    }

    ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
    resourceAction.setStackEntity(stackEntity);
    resourceInfo.setEffectiveUserId(effectiveUserId);
    resourceAction.setResourceInfo(resourceInfo);
    StackEvent stackEvent = new StackEvent();
    stackEvent.setStackId(stackId);
    stackEvent.setStackName(stackName);
    stackEvent.setLogicalResourceId(resourceInfo.getLogicalResourceId());
    stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
    stackEvent.setEventId(resourceInfo.getLogicalResourceId() + "-" + StackResourceEntity.Status.CREATE_IN_PROGRESS.toString() + "-" + System.currentTimeMillis());
    //TODO: see if this really matches
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
    return "";
  }

  @Override
  public String getResourceType(String stackId, String accountId, String resourceId) {
    return StackResourceEntityManager.getStackResource(stackId, accountId, resourceId).getResourceType();
  }

  @Override
  public String initDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId) {
    LOG.info("Deleting resource " + resourceId);
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    String stackName = stackEntity.getStackName();
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId);
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    resourceInfo.setEffectiveUserId(effectiveUserId);
    if (stackResourceEntity != null && stackResourceEntity.getResourceStatus() != StackResourceEntity.Status.DELETE_COMPLETE
      && stackResourceEntity.getResourceStatus() != StackResourceEntity.Status.NOT_STARTED) {
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
      resourceAction.setStackEntity(stackEntity);
      resourceAction.setResourceInfo(resourceInfo);
      if ("Retain".equals(resourceInfo.getDeletionPolicy())) {
        LOG.info("Resource " + resourceId + " has a 'Retain' DeletionPolicy, skipping.");
        StackEvent stackEvent = new StackEvent();
        stackEvent.setStackId(stackId);
        stackEvent.setStackName(stackName);
        stackEvent.setLogicalResourceId(resourceInfo.getLogicalResourceId());
        stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
        stackEvent.setEventId(resourceInfo.getLogicalResourceId() + "-" + StackResourceEntity.Status.DELETE_SKIPPED.toString() + "-" + System.currentTimeMillis());
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
        return "SKIP";
      } else {
        StackEvent stackEvent = new StackEvent();
        stackEvent.setStackId(stackId);
        stackEvent.setStackName(stackName);
        stackEvent.setLogicalResourceId(resourceInfo.getLogicalResourceId());
        stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
        stackEvent.setEventId(resourceInfo.getLogicalResourceId() + "-" + StackResourceEntity.Status.DELETE_IN_PROGRESS.toString() + "-" + System.currentTimeMillis());
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
        return "";
      }
    }
    return "SKIP";
    // TODO: deal with either properties messing up or wrong resource type
  }

  @Override
  public String determineCreateResourceFailures(String stackId, String accountId) {
    Collection<String> failedResources = Lists.newArrayList();
    Collection<StackResourceEntity> cancelledResources = Lists.newArrayList();
    for (StackResourceEntity stackResourceEntity : StackResourceEntityManager.getStackResources(stackId, accountId)) {
      if (stackResourceEntity.getResourceStatus() == StackResourceEntity.Status.CREATE_FAILED) {
        failedResources.add(stackResourceEntity.getLogicalResourceId());
      } else if (stackResourceEntity.getResourceStatus() == StackResourceEntity.Status.CREATE_IN_PROGRESS) {
        cancelledResources.add(stackResourceEntity);
        failedResources.add(stackResourceEntity.getLogicalResourceId());
      }
    }
    for (StackResourceEntity stackResourceEntity : cancelledResources) {
      stackResourceEntity.setResourceStatus(StackResourceEntity.Status.CREATE_FAILED);
      stackResourceEntity.setResourceStatusReason("Resource creation cancelled");
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
    }
    return "The following resource(s) failed to create: " + failedResources + ".";
  }

  @Override
  public String determineDeleteResourceFailures(String stackId, String accountId) {
    Collection<String> failedResources = Lists.newArrayList();
    Collection<StackResourceEntity> cancelledResources = Lists.newArrayList();
    for (StackResourceEntity stackResourceEntity : StackResourceEntityManager.getStackResources(stackId, accountId)) {
      if (stackResourceEntity.getResourceStatus() == StackResourceEntity.Status.DELETE_FAILED) {
        failedResources.add(stackResourceEntity.getLogicalResourceId());
      } else if (stackResourceEntity.getResourceStatus() == StackResourceEntity.Status.DELETE_IN_PROGRESS) {
        cancelledResources.add(stackResourceEntity);
        failedResources.add(stackResourceEntity.getLogicalResourceId());
      }
    }
    for (StackResourceEntity stackResourceEntity : cancelledResources) {
      stackResourceEntity.setResourceStatus(StackResourceEntity.Status.DELETE_FAILED);
      stackResourceEntity.setResourceStatusReason("Resource deletion cancelled");
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
    }
    return "The following resource(s) failed to delete: " + failedResources + ".";
  }

  @Override
  public String finalizeCreateStack(String stackId, String accountId) {
    LOG.info("Finalizing create stack");
    try {
      StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
      Map<String, ResourceInfo> resourceInfoMap = Maps.newLinkedHashMap();
      for (StackResourceEntity stackResourceEntity : StackResourceEntityManager.getStackResources(stackId, accountId)) {
        resourceInfoMap.put(stackResourceEntity.getLogicalResourceId(), StackResourceEntityManager.getResourceInfo(stackResourceEntity));
      }
      List<StackEntity.Output> outputs = StackEntityHelper.jsonToOutputs(stackEntity.getOutputsJson());

      for (StackEntity.Output output : outputs) {
        output.setReady(true);
        output.setReady(true);
        JsonNode outputValue = FunctionEvaluation.evaluateFunctions(JsonHelper.getJsonNodeFromString(output.getJsonValue()), stackEntity, resourceInfoMap);
        if (outputValue == null || !outputValue.isValueNode()) {
          throw new ValidationErrorException("Cannot create outputs: All outputs must be strings.")
        }
        output.setStringValue(outputValue.asText());
        output.setJsonValue(JsonHelper.getStringFromJsonNode(outputValue));
      }
      stackEntity.setOutputsJson(StackEntityHelper.outputsToJson(outputs));
      stackEntity.setStackStatus(StackEntity.Status.CREATE_COMPLETE);
      StackEntityManager.updateStack(stackEntity);
    } catch (Exception e) {
      LOG.error(e, e);
      throw e;
    }
    LOG.info("Done finalizing create stack");
    return ""; // promiseFor() doesn't work on void return types
  }

  @Override
  public Boolean performCreateStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId) {
    LOG.info("Performing creation step " + stepId + " on resource " + resourceId);
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId);
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    try {
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
      resourceAction.setStackEntity(stackEntity);
      resourceInfo.setEffectiveUserId(effectiveUserId);
      resourceAction.setResourceInfo(resourceInfo);
      ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()));
      Step createStep = resourceAction.getCreateStep(stepId);
      resourceAction = createStep.perform(resourceAction);
      resourceInfo = resourceAction.getResourceInfo();
      stackResourceEntity.setResourceStatus(StackResourceEntity.Status.CREATE_IN_PROGRESS);
      stackResourceEntity.setResourceStatusReason(null);
      stackResourceEntity.setDescription(""); // deal later
      stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
      StackResourceEntityManager.updateStackResource(stackResourceEntity);

      // Create a stack event after success
      StackEvent stackEvent = new StackEvent();
      stackEvent.setStackId(stackId);
      stackEvent.setStackName(resourceAction.getStackEntity().getStackName());
      stackEvent.setLogicalResourceId(resourceInfo.getLogicalResourceId());
      stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
      stackEvent.setEventId(resourceInfo.getLogicalResourceId() + "-" + StackResourceEntity.Status.CREATE_IN_PROGRESS.toString() + "-" + System.currentTimeMillis());
      stackEvent.setResourceProperties(resourceInfo.getPropertiesJson());
      stackEvent.setResourceType(resourceInfo.getType());
      stackEvent.setResourceStatus(StackResourceEntity.Status.CREATE_IN_PROGRESS.toString());
      stackEvent.setResourceStatusReason(null);
      stackEvent.setTimestamp(new Date());
      StackEventEntityManager.addStackEvent(stackEvent, accountId);

    } catch (NotAResourceFailureException ex) {
      LOG.info( "Create step not yet complete: ${ex.message}" );
      LOG.debug(ex, ex);
      return false;
    } catch (Exception ex) {
      LOG.debug("Error creating resource " + resourceId);
      LOG.error(ex, ex);
      stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
      stackResourceEntity.setResourceStatus(StackResourceEntity.Status.CREATE_FAILED);
      Throwable rootCause = Throwables.getRootCause(ex);
      stackResourceEntity.setResourceStatusReason("" + rootCause.getMessage());
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
      StackEvent stackEvent = new StackEvent();
      stackEvent.setStackId(stackId);
      stackEvent.setStackName(stackEntity.getStackName());
      stackEvent.setLogicalResourceId(resourceInfo.getLogicalResourceId());
      stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
      stackEvent.setEventId(resourceInfo.getLogicalResourceId() + "-" + StackResourceEntity.Status.CREATE_FAILED.toString() + "-" + System.currentTimeMillis());
      stackEvent.setResourceProperties(resourceInfo.getPropertiesJson());
      stackEvent.setResourceType(resourceInfo.getType());
      stackEvent.setResourceStatus(StackResourceEntity.Status.CREATE_FAILED.toString());
      stackEvent.setResourceStatusReason("" + rootCause.getMessage());
      stackEvent.setTimestamp(new Date());
      StackEventEntityManager.addStackEvent(stackEvent, accountId);
      throw new ResourceFailureException(rootCause.getClass().getName() + ":" + rootCause.getMessage());
    }
    return true;
  }

  @Override
  public Boolean performDeleteStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId) {
    LOG.info("Performing delete step " + stepId + " on resource " + resourceId);
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId);
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    try {
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
      resourceAction.setStackEntity(stackEntity);
      resourceInfo.setEffectiveUserId(effectiveUserId);
      resourceAction.setResourceInfo(resourceInfo);
      boolean errorWithProperties = false;
      try {
        ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()));
      } catch (Exception ex) {
        errorWithProperties = true;
      }
      if (!errorWithProperties) {
        // if we have errors with properties we had them on create too, so we didn't start (really)
        Step deleteStep = resourceAction.getDeleteStep(stepId);
        resourceAction = deleteStep.perform(resourceAction);
        resourceInfo = resourceAction.getResourceInfo();
        stackResourceEntity.setResourceStatus(StackResourceEntity.Status.DELETE_IN_PROGRESS);
        stackResourceEntity.setResourceStatusReason(null);
        stackResourceEntity.setDescription(""); // deal later
        stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
        StackResourceEntityManager.updateStackResource(stackResourceEntity);
      }
    } catch (NotAResourceFailureException ex) {
      LOG.info( "Delete step not yet complete: ${ex.message}" );
      LOG.debug(ex, ex);
      return false;
    } catch (Exception ex) {
      LOG.debug("Error deleting resource " + resourceId);
      LOG.error(ex, ex);
      Throwable rootCause = Throwables.getRootCause(ex);
      throw new ResourceFailureException(rootCause.getMessage());
      // Don't put the delete failed step here as we need to return "failure" but this must be done in the caller
    }
    return true;
  }

  @Override
  public String finalizeCreateResource(String resourceId, String stackId, String accountId, String effectiveUserId) {
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId);
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
    ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()));
    resourceAction.setStackEntity(stackEntity);
    resourceInfo.setEffectiveUserId(effectiveUserId);
    resourceAction.setResourceInfo(resourceInfo);
    resourceInfo.setReady(Boolean.TRUE);
    stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
    stackResourceEntity.setResourceStatus(StackResourceEntity.Status.CREATE_COMPLETE);
    stackResourceEntity.setResourceStatusReason("");
    StackResourceEntityManager.updateStackResource(stackResourceEntity);

    StackEvent stackEvent = new StackEvent();
    stackEvent.setStackId(stackId);
    stackEvent.setStackName(resourceAction.getStackEntity().getStackName());
    stackEvent.setLogicalResourceId(resourceInfo.getLogicalResourceId());
    stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
    stackEvent.setEventId(resourceInfo.getLogicalResourceId() + "-" + StackResourceEntity.Status.CREATE_COMPLETE.toString() + "-" + System.currentTimeMillis());
    stackEvent.setResourceProperties(resourceInfo.getPropertiesJson());
    stackEvent.setResourceType(resourceInfo.getType());
    stackEvent.setResourceStatus(StackResourceEntity.Status.CREATE_COMPLETE.toString());
    stackEvent.setResourceStatusReason("");
    stackEvent.setTimestamp(new Date());
    StackEventEntityManager.addStackEvent(stackEvent, accountId);

    LOG.debug("Finished creating resource " + resourceId);
    return "";
  }

  @Override
  public String finalizeDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId) {
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId);
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    resourceInfo.setEffectiveUserId(effectiveUserId);
    stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
    stackResourceEntity.setResourceStatus(StackResourceEntity.Status.DELETE_COMPLETE);
    stackResourceEntity.setResourceStatusReason("");
    StackResourceEntityManager.updateStackResource(stackResourceEntity);

    StackEvent stackEvent = new StackEvent();
    stackEvent.setStackId(stackId);
    stackEvent.setStackName(stackEntity.getStackName());
    stackEvent.setLogicalResourceId(resourceInfo.getLogicalResourceId());
    stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
    stackEvent.setEventId(resourceInfo.getLogicalResourceId() + "-" + StackResourceEntity.Status.DELETE_COMPLETE.toString() + "-" + System.currentTimeMillis());
    stackEvent.setResourceProperties(resourceInfo.getPropertiesJson());
    stackEvent.setResourceType(resourceInfo.getType());
    stackEvent.setResourceStatus(StackResourceEntity.Status.DELETE_COMPLETE.toString());
    stackEvent.setResourceStatusReason("");
    stackEvent.setTimestamp(new Date());
    StackEventEntityManager.addStackEvent(stackEvent, accountId);

    LOG.debug("Finished deleting resource " + resourceId);
    return "SUCCESS";
  }

  @Override
  public String failDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage) {
    LOG.info("Error deleting resource " + resourceId);
    LOG.error(errorMessage);
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId);
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    resourceInfo.setEffectiveUserId(effectiveUserId);
    stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
    stackResourceEntity.setResourceStatus(StackResourceEntity.Status.DELETE_FAILED);
    stackResourceEntity.setResourceStatusReason(errorMessage);
    StackResourceEntityManager.updateStackResource(stackResourceEntity);
    StackEvent stackEvent = new StackEvent();
    stackEvent.setStackId(stackId);
    stackEvent.setStackName(stackEntity.getStackName());
    stackEvent.setLogicalResourceId(resourceInfo.getLogicalResourceId());
    stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
    stackEvent.setEventId(resourceInfo.getLogicalResourceId() + "-" + StackResourceEntity.Status.DELETE_FAILED.toString() + "-" + System.currentTimeMillis());
    stackEvent.setResourceProperties(resourceInfo.getPropertiesJson());
    stackEvent.setResourceType(resourceInfo.getType());
    stackEvent.setResourceStatus(StackResourceEntity.Status.DELETE_FAILED.toString());
    stackEvent.setResourceStatusReason(errorMessage);
    stackEvent.setTimestamp(new Date());
    StackEventEntityManager.addStackEvent(stackEvent, accountId);
    return "FAILURE";
  }

  @Override
  public String deleteAllStackRecords(String stackId, String accountId) {
    LOG.info("Deleting all stack records");
    StackResourceEntityManager.deleteStackResources(stackId, accountId);
    StackEventEntityManager.deleteStackEvents(stackId, accountId);
    StackEntityManager.deleteStack(stackId, accountId);
    LOG.info("Finished deleting all stack records");
    return ""; // promiseFor() doesn't work on void return types
  }

  public String getWorkflowExecutionCloseStatus( final String stackId ) {
    final AmazonSimpleWorkflow simpleWorkflowClient = WorkflowClientManager.simpleWorkflowClient
    final List<StackWorkflowEntity> createStackWorkflowEntities =
        StackWorkflowEntityManager.getStackWorkflowEntities( stackId, CREATE_STACK_WORKFLOW );
    // TODO: is it really appropriate to fail if no workflows exist
    if ( createStackWorkflowEntities == null || createStackWorkflowEntities.empty ) {
      throw new InternalFailureException( "There is no create stack workflow for stack id ${stackId}" );
    }
    if ( createStackWorkflowEntities.size( ) > 1 ) {
      throw new InternalFailureException( "More than one create stack workflow was found for stack id ${stackId}" );
    }
    createStackWorkflowEntities.get( 0 ).with{
      simpleWorkflowClient.describeWorkflowExecution(
          new DescribeWorkflowExecutionRequest(
              domain: domain,
              execution: new WorkflowExecution(
                  runId: runId,
                  workflowId: workflowId
              )
          )
      ).with{
        executionInfo.closeStatus
      }
    }
  }

  @Override
  public String getStackStatus(String stackId, String accountId) {
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    if (stackEntity == null) {
      throw new ValidationErrorException("No stack found with id " + stackId);
    }
    return stackEntity.getStackStatus().toString();
  }

  public String setStackStatus(String stackId, String accountId, String status, String statusReason) {
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    if (stackEntity == null) {
      throw new ValidationErrorException("No stack found with id " + stackId);
    }
    stackEntity.setStackStatus(StackEntity.Status.valueOf(status));
    stackEntity.setStackStatusReason(statusReason);
    StackEntityManager.updateStack(stackEntity);
    return "";
  }

  @Override
  public String cancelCreateAndMonitorWorkflows(String stackId) {
    AmazonSimpleWorkflow simpleWorkflowClient = WorkflowClientManager.simpleWorkflowClient
    cancelOpenWorkflows(simpleWorkflowClient, StackWorkflowEntityManager.getStackWorkflowEntities(stackId, StackWorkflowEntity.WorkflowType.MONITOR_CREATE_STACK_WORKFLOW));
    cancelOpenWorkflows(simpleWorkflowClient, StackWorkflowEntityManager.getStackWorkflowEntities(stackId, CREATE_STACK_WORKFLOW));
    return "";
  }

  private boolean isWorkflowOpen(AmazonSimpleWorkflow simpleWorkflowClient, StackWorkflowEntity stackWorkflowEntity) {
    boolean isOpen = false;
    try {
      DescribeWorkflowExecutionRequest describeWorkflowExecutionRequest = new DescribeWorkflowExecutionRequest();
      describeWorkflowExecutionRequest.setDomain(stackWorkflowEntity.getDomain());
      WorkflowExecution execution = new WorkflowExecution();
      execution.setRunId(stackWorkflowEntity.getRunId());
      execution.setWorkflowId(stackWorkflowEntity.getWorkflowId());
      describeWorkflowExecutionRequest.setExecution(execution);
      WorkflowExecutionDetail workflowExecutionDetail = simpleWorkflowClient.describeWorkflowExecution(describeWorkflowExecutionRequest);
      if ("OPEN".equals(workflowExecutionDetail.getExecutionInfo().getExecutionStatus())) {
        isOpen = true;
      }
    } catch (Exception ex) {
      // not open
    }
    return isOpen;
  }
  private void cancelOpenWorkflows(AmazonSimpleWorkflow simpleWorkflowClient, List<StackWorkflowEntity> workflows) {
    if (workflows != null) {
      // Should only be one here, but that is checked for elsewhere, cancel everything
      for (StackWorkflowEntity workflow : workflows) {
        if (isWorkflowOpen(simpleWorkflowClient, workflow)) {
          RequestCancelWorkflowExecutionRequest requestCancelWorkflowRequest = new RequestCancelWorkflowExecutionRequest();
          requestCancelWorkflowRequest.setDomain(workflow.getDomain());
          requestCancelWorkflowRequest.setWorkflowId(workflow.getWorkflowId());
          requestCancelWorkflowRequest.setRunId(workflow.getRunId());
          simpleWorkflowClient.requestCancelWorkflowExecution(requestCancelWorkflowRequest);
        }
      }
    }
  }
  @Override
  public String verifyCreateAndMonitorWorkflowsClosed(String stackId) {
    AmazonSimpleWorkflow simpleWorkflowClient = WorkflowClientManager.simpleWorkflowClient
    List<StackWorkflowEntity> monitorWorkflows = StackWorkflowEntityManager.getStackWorkflowEntities(stackId, StackWorkflowEntity.WorkflowType.MONITOR_CREATE_STACK_WORKFLOW);
    if (monitorWorkflows != null) {
      for (StackWorkflowEntity workflow : monitorWorkflows) {
        if (isWorkflowOpen(simpleWorkflowClient, workflow)) {
          throw new ValidationFailedException("A monitoring workflow for " + stackId + " has not yet been canceled");
        }
      }
    }
    List<StackWorkflowEntity> createWorkflows = StackWorkflowEntityManager.getStackWorkflowEntities(stackId, CREATE_STACK_WORKFLOW);
    if (createWorkflows != null) {
      for (StackWorkflowEntity workflow : createWorkflows) {
        if (isWorkflowOpen(simpleWorkflowClient, workflow)) {
          throw new ValidationFailedException("A create workflow for " + stackId + " has not yet been canceled");
        }
      }
    }
    return "";
  }

  @Override
  public Integer getAWSCloudFormationWaitConditionTimeout(String resourceId, String stackId, String accountId, String effectiveUserId) {
    try {
      StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
      StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId);
      ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
      resourceAction.setStackEntity(stackEntity);
      resourceInfo.setEffectiveUserId(effectiveUserId);
      resourceAction.setResourceInfo(resourceInfo);
      ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()));
      Integer timeout = ((AWSCloudFormationWaitConditionProperties) resourceAction.getResourceProperties()).getTimeout(); // not proud of this hard coding.
      return timeout
    } catch (Exception ex) {
        Throwable rootCause = Throwables.getRootCause(ex);
        LOG.debug("Error getting timeout for resource " + resourceId);
        LOG.error(ex, ex);
        throw new ResourceFailureException(rootCause.getClass().getName() + ":" + rootCause.getMessage());
    }
  }
}
