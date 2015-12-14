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
import com.eucalyptus.cloudformation.ValidationErrorException
import com.eucalyptus.cloudformation.entity.StackEntity
import com.eucalyptus.cloudformation.entity.StackEntityHelper
import com.eucalyptus.cloudformation.entity.StackEntityManager
import com.eucalyptus.cloudformation.entity.StackEventEntityManager
import com.eucalyptus.cloudformation.entity.StackResourceEntity
import com.eucalyptus.cloudformation.entity.StackResourceEntityForCleanup
import com.eucalyptus.cloudformation.entity.StackResourceEntityInUse
import com.eucalyptus.cloudformation.entity.StackResourceEntityManager
import com.eucalyptus.cloudformation.entity.StackUpdateInfoEntityManager
import com.eucalyptus.cloudformation.entity.StackWorkflowEntity
import com.eucalyptus.cloudformation.entity.StackWorkflowEntityManager
import com.eucalyptus.cloudformation.entity.Status
import com.eucalyptus.cloudformation.resources.ResourceAction
import com.eucalyptus.cloudformation.resources.ResourceInfo
import com.eucalyptus.cloudformation.resources.ResourcePropertyResolver
import com.eucalyptus.cloudformation.resources.ResourceResolverManager
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSCloudFormationWaitConditionProperties
import com.eucalyptus.cloudformation.template.AWSParameterTypeValidationHelper
import com.eucalyptus.cloudformation.template.FunctionEvaluation
import com.eucalyptus.cloudformation.template.IntrinsicFunctions
import com.eucalyptus.cloudformation.template.JsonHelper
import com.eucalyptus.cloudformation.template.ParameterType
import com.eucalyptus.cloudformation.template.TemplateParser
import com.eucalyptus.cloudformation.workflow.steps.Step
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep
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
import static com.eucalyptus.cloudformation.entity.StackWorkflowEntity.WorkflowType.UPDATE_STACK_WORKFLOW

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
    String eventId = UUID.randomUUID().toString(); //TODO: AWS has a value related to stack id. (I think)
    String logicalResourceId = stackName;
    String physicalResourceId = stackId;
    ObjectNode properties = new ObjectMapper().createObjectNode();
    for (StackEntity.Parameter parameter : StackEntityHelper.jsonToParameters(stackEntity.getParametersJson())) {
      properties.put(parameter.getKey(), parameter.getStringValue());
    }
    Status status = Status.valueOf(resourceStatus);
    String resourceProperties = JsonHelper.getStringFromJsonNode(properties);
    String resourceType = "AWS::CloudFormation::Stack";
    Date timestamp = new Date();
    StackEventEntityManager.addStackEvent(accountId, eventId, logicalResourceId, physicalResourceId,
      resourceProperties, status, resourceStatusReason, resourceType, stackId, stackName, timestamp);

    // Good to update the global stack too
    stackEntity.setStackStatus(status);
    stackEntity.setStackStatusReason(resourceStatusReason);
    if ((status == Status.DELETE_IN_PROGRESS) && (stackEntity.getDeleteOperationTimestamp() == null)) {
      stackEntity.setDeleteOperationTimestamp(new Date()); // AWS only records the first delete attempt timestamp
    }
    StackEntityManager.updateStack(stackEntity);
    LOG.info("Done creating global stack event: " + resourceStatus);
    return ""; // promiseFor() doesn't work on void return types
  }

  @Override
  public String initCreateResource(String resourceId, String stackId, String accountId, String effectiveUserId, String reverseDependentResourcesJson) {
    LOG.info("Creating resource " + resourceId);
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResourceInUse(stackId, accountId, resourceId);
    ArrayList<String> reverseDependentResourceIds = (reverseDependentResourcesJson == null) ? new ArrayList<String>()
      : (ArrayList<String>) new ObjectMapper().readValue(reverseDependentResourcesJson, new TypeReference<ArrayList<String>>() {
    })
    Map<String, ResourceInfo> resourceInfoMap = Maps.newLinkedHashMap();
    for (String reverseDependentResourceId : reverseDependentResourceIds) {
      resourceInfoMap.put(reverseDependentResourceId, StackResourceEntityManager.getResourceInfo(stackId, accountId, reverseDependentResourceId));
    }
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    if (!resourceInfo.getAllowedByCondition()) {
      LOG.info("Resource " + resourceId + " not allowed by condition, skipping");
      return "SKIP"; //TODO: codify this somewhere...
    };

    updateResourceInfoFields(resourceInfo, stackEntity, resourceInfoMap, effectiveUserId)

    ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
    resourceAction.setStackEntity(stackEntity);
    resourceInfo.setEffectiveUserId(effectiveUserId);
    resourceAction.setResourceInfo(resourceInfo);
    stackResourceEntity.setResourceStatus(Status.CREATE_IN_PROGRESS);
    stackResourceEntity.setResourceStatusReason(null);
    stackResourceEntity.setDescription(""); // deal later
    stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
    StackResourceEntityManager.updateStackResource(stackResourceEntity);
    StackEventEntityManager.addStackEvent(stackResourceEntity);
    return "";
  }

  private void updateResourceInfoFields(ResourceInfo resourceInfo, StackEntity stackEntity, LinkedHashMap<String, ResourceInfo> resourceInfoMap, String effectiveUserId) {
    // Evaluate all properties
    if (resourceInfo.getPropertiesJson() != null) {
      JsonNode propertiesJsonNode = JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson());
      List<String> propertyKeys = Lists.newArrayList(propertiesJsonNode.fieldNames());
      for (String propertyKey : propertyKeys) {
        JsonNode evaluatedPropertyNode = FunctionEvaluation.evaluateFunctions(propertiesJsonNode.get(propertyKey), stackEntity, resourceInfoMap, effectiveUserId);
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
        JsonNode evaluatedMetadataNode = FunctionEvaluation.evaluateFunctions(metadataJsonNode.get(metadataKey), stackEntity, resourceInfoMap, effectiveUserId);
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
        JsonNode evaluatedUpdatePolicyNode = FunctionEvaluation.evaluateFunctions(updatePolicyJsonNode.get(updatePolicyKey), stackEntity, resourceInfoMap, effectiveUserId);
        if (IntrinsicFunctions.NO_VALUE.evaluateMatch(evaluatedUpdatePolicyNode).isMatch()) {
          ((ObjectNode) updatePolicyJsonNode).remove(updatePolicyKey);
        } else {
          ((ObjectNode) updatePolicyJsonNode).put(updatePolicyKey, evaluatedUpdatePolicyNode);
        }
      }
      resourceInfo.setUpdatePolicyJson(JsonHelper.getStringFromJsonNode(updatePolicyJsonNode));
    }
  }

  @Override
  public String getResourceType(String stackId, String accountId, String resourceId) {
    return StackResourceEntityManager.getStackResourceInUse(stackId, accountId, resourceId).getResourceType();
  }

  @Override
  public String getResourceTypeForUpdate(String stackId, String accountId, String resourceId) {
    return StackResourceEntityManager.getStackResourceForUpdate(stackId, accountId, resourceId).getResourceType();
  }

  @Override
  public String initDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId) {
    LOG.info("Deleting resource " + resourceId);
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    String stackName = stackEntity.getStackName();
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResourceInUse(stackId, accountId, resourceId);
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    resourceInfo.setEffectiveUserId(effectiveUserId);
    if (stackResourceEntity != null && stackResourceEntity.getResourceStatus() != Status.DELETE_COMPLETE
      && stackResourceEntity.getResourceStatus() != Status.NOT_STARTED) {
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
      resourceAction.setStackEntity(stackEntity);
      resourceAction.setResourceInfo(resourceInfo);
      if ("Retain".equals(resourceInfo.getDeletionPolicy())) {
        LOG.info("Resource " + resourceId + " has a 'Retain' DeletionPolicy, skipping.");
        stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
        stackResourceEntity.setResourceStatus(Status.DELETE_SKIPPED);
        stackResourceEntity.setResourceStatusReason(null);
        StackResourceEntityManager.updateStackResource(stackResourceEntity);
        StackEventEntityManager.addStackEvent(stackResourceEntity);
        return "SKIP";
      } else {
        stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
        stackResourceEntity.setResourceStatus(Status.DELETE_IN_PROGRESS);
        stackResourceEntity.setResourceStatusReason(null);
        StackResourceEntityManager.updateStackResource(stackResourceEntity);
        StackEventEntityManager.addStackEvent(stackResourceEntity);
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
      if (stackResourceEntity.getResourceStatus() == Status.CREATE_FAILED) {
        failedResources.add(stackResourceEntity.getLogicalResourceId());
      } else if (stackResourceEntity.getResourceStatus() == Status.CREATE_IN_PROGRESS) {
        cancelledResources.add(stackResourceEntity);
        failedResources.add(stackResourceEntity.getLogicalResourceId());
      }
    }
    for (StackResourceEntity stackResourceEntity : cancelledResources) {
      stackResourceEntity.setResourceStatus(Status.CREATE_FAILED);
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
      if (stackResourceEntity.getResourceStatus() == Status.DELETE_FAILED) {
        failedResources.add(stackResourceEntity.getLogicalResourceId());
      } else if (stackResourceEntity.getResourceStatus() == Status.DELETE_IN_PROGRESS) {
        cancelledResources.add(stackResourceEntity);
        failedResources.add(stackResourceEntity.getLogicalResourceId());
      }
    }
    for (StackResourceEntity stackResourceEntity : cancelledResources) {
      stackResourceEntity.setResourceStatus(Status.DELETE_FAILED);
      stackResourceEntity.setResourceStatusReason("Resource deletion cancelled");
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
    }
    return "The following resource(s) failed to delete: " + failedResources + ".";
  }

  @Override
  public String finalizeCreateStack(String stackId, String accountId, String effectiveUserId) {
    LOG.info("Finalizing create stack");
    try {
      StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
      Map<String, ResourceInfo> resourceInfoMap = Maps.newLinkedHashMap();
      for (StackResourceEntity stackResourceEntity : StackResourceEntityManager.getStackResources(stackId, accountId)) {
        resourceInfoMap.put(stackResourceEntity.getLogicalResourceId(), StackResourceEntityManager.getResourceInfo(stackResourceEntity));
      }
      List<StackEntity.Output> outputs = StackEntityHelper.jsonToOutputs(stackEntity.getWorkingOutputsJson());

      for (StackEntity.Output output : outputs) {
        output.setReady(true);
        if (!output.isAllowedByCondition()) continue; // don't evaluate outputs that won't show up anyway.
        JsonNode outputValue = FunctionEvaluation.evaluateFunctions(JsonHelper.getJsonNodeFromString(output.getJsonValue()), stackEntity, resourceInfoMap, effectiveUserId);
        if (outputValue == null || !outputValue.isValueNode()) {
          throw new ValidationErrorException("Cannot create outputs: All outputs must be strings.")
        }
        output.setStringValue(outputValue.asText());
        output.setJsonValue(JsonHelper.getStringFromJsonNode(outputValue));
      }
      stackEntity.setWorkingOutputsJson(StackEntityHelper.outputsToJson(outputs));
      // now finalize the outputs
      stackEntity.setOutputsJson(StackEntityHelper.outputsToJson(outputs));
      stackEntity.setStackStatus(Status.CREATE_COMPLETE);
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
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResourceInUse(stackId, accountId, resourceId);
    String oldPhysicalResourceId = stackResourceEntity.getPhysicalResourceId();
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    try {
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
      resourceAction.setStackEntity(stackEntity);
      resourceInfo.setEffectiveUserId(effectiveUserId);
      resourceAction.setResourceInfo(resourceInfo);
      ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()));
      if (!(resourceAction instanceof StepBasedResourceAction)) {
        throw new ClassCastException("Calling performCreateStep against a resource action that does not extend StepBasedResourceAction: " + resourceAction.getClass().getName());
      }
      Step createStep = ((StepBasedResourceAction) resourceAction).getCreateStep(stepId);
      resourceAction = createStep.perform(resourceAction);
      resourceInfo = resourceAction.getResourceInfo();
      stackResourceEntity.setDescription(""); // deal later
      stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
      if (!Objects.equals(stackResourceEntity.getPhysicalResourceId(), oldPhysicalResourceId)) {
        stackResourceEntity.setResourceStatus(Status.CREATE_IN_PROGRESS);
        stackResourceEntity.setResourceStatusReason("Resource creation Initiated");
        StackEventEntityManager.addStackEvent(stackResourceEntity);
      }
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
    } catch (NotAResourceFailureException ex) {
      LOG.info( "Create step not yet complete: ${ex.message}" );
      LOG.debug(ex, ex);
      return false;
    } catch (Exception ex) {
      LOG.info("Error creating resource " + resourceId);
      LOG.error(ex, ex);
      stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
      stackResourceEntity.setResourceStatus(Status.CREATE_FAILED);
      Throwable rootCause = Throwables.getRootCause(ex);
      stackResourceEntity.setResourceStatusReason("" + rootCause.getMessage());
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
      StackEventEntityManager.addStackEvent(stackResourceEntity);
      throw new ResourceFailureException(rootCause.getClass().getName() + ":" + rootCause.getMessage());
    }
    return true;
  }

  @Override
  public Boolean performDeleteStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId) {
    LOG.info("Performing delete step " + stepId + " on resource " + resourceId);
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResourceInUse(stackId, accountId, resourceId);
    return performDeleteStepOnStackResourceEntity(stackResourceEntity, stackEntity, effectiveUserId, stepId, resourceId)
  }

  @Override
  public Boolean performUpdateCleanupStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId) {
    LOG.info("Performing delete step " + stepId + " on resource " + resourceId);
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResourceForCleanup(stackId, accountId, resourceId);
    return performDeleteStepOnStackResourceEntity(stackResourceEntity, stackEntity, effectiveUserId, stepId, resourceId)
  }

  private boolean performDeleteStepOnStackResourceEntity(StackResourceEntity stackResourceEntity, StackEntity stackEntity, String effectiveUserId, String stepId, String resourceId) {
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
        if (!(resourceAction instanceof StepBasedResourceAction)) {
          throw new ClassCastException("Calling performDeleteStep against a resource action that does not extend StepBasedResourceAction: " + resourceAction.getClass().getName());
        }
        Step deleteStep = ((StepBasedResourceAction) resourceAction).getDeleteStep(stepId);
        resourceAction = deleteStep.perform(resourceAction);
        resourceInfo = resourceAction.getResourceInfo();
        stackResourceEntity.setResourceStatus(Status.DELETE_IN_PROGRESS);
        stackResourceEntity.setResourceStatusReason(null);
        stackResourceEntity.setDescription(""); // deal later
        stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
        StackResourceEntityManager.updateStackResource(stackResourceEntity);
      }
    } catch (NotAResourceFailureException ex) {
      LOG.info("Delete step not yet complete: ${ex.message}");
      LOG.debug(ex, ex);
      return false;
    } catch (Exception ex) {
      LOG.info("Error deleting resource " + resourceId);
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
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResourceInUse(stackId, accountId, resourceId);
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
    ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()));
    resourceAction.setStackEntity(stackEntity);
    resourceInfo.setEffectiveUserId(effectiveUserId);
    resourceAction.setResourceInfo(resourceInfo);
    resourceInfo.setReady(Boolean.TRUE);
    stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
    stackResourceEntity.setResourceStatus(Status.CREATE_COMPLETE);
    stackResourceEntity.setResourceStatusReason("");
    StackResourceEntityManager.updateStackResource(stackResourceEntity);
    StackEventEntityManager.addStackEvent(stackResourceEntity);

    LOG.info("Finished creating resource " + resourceId);
    return "";
  }

  @Override
  public String finalizeDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId) {
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResourceInUse(stackId, accountId, resourceId);
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    resourceInfo.setEffectiveUserId(effectiveUserId);
    stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
    stackResourceEntity.setResourceStatus(Status.DELETE_COMPLETE);
    stackResourceEntity.setResourceStatusReason("");
    StackResourceEntityManager.updateStackResource(stackResourceEntity);
    StackEventEntityManager.addStackEvent(stackResourceEntity);
    LOG.info("Finished deleting resource " + resourceId);
    return "SUCCESS";
  }

  @Override
  public String failDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage) {
    LOG.info("Error deleting resource " + resourceId);
    LOG.error(errorMessage);
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResourceInUse(stackId, accountId, resourceId);
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    resourceInfo.setEffectiveUserId(effectiveUserId);
    stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
    stackResourceEntity.setResourceStatus(Status.DELETE_FAILED);
    stackResourceEntity.setResourceStatusReason(errorMessage);
    StackResourceEntityManager.updateStackResource(stackResourceEntity);
    StackEventEntityManager.addStackEvent(stackResourceEntity);
    return "FAILURE";
  }

  @Override
  public String deleteAllStackRecords(String stackId, String accountId) {
    LOG.info("Deleting all stack records");
    StackResourceEntityManager.deleteStackResourcesInUse(stackId, accountId);
    StackResourceEntityManager.deleteStackResourcesForUpdate(stackId, accountId);
    StackResourceEntityManager.deleteStackResourcesForCleanup(stackId, accountId);
    StackEventEntityManager.deleteStackEvents(stackId, accountId);
    StackUpdateInfoEntityManager.deleteStackUpdateInfo(stackId, accountId);
    StackEntityManager.deleteStack(stackId, accountId);
    LOG.info("Finished deleting all stack records");
    return ""; // promiseFor() doesn't work on void return types
  }

  @Override
  public String getCreateWorkflowExecutionCloseStatus( final String stackId ) {
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
  public String getUpdateWorkflowExecutionCloseStatus( final String stackId ) {
    LOG.debug("Getting update execution close status for stack " + stackId);
    final AmazonSimpleWorkflow simpleWorkflowClient = WorkflowClientManager.simpleWorkflowClient
    final List<StackWorkflowEntity> updateStackWorkflowEntities =
      StackWorkflowEntityManager.getStackWorkflowEntities( stackId, UPDATE_STACK_WORKFLOW );
    // TODO: is it really appropriate to fail if no workflows exist
    if ( updateStackWorkflowEntities == null || updateStackWorkflowEntities.empty ) {
      throw new InternalFailureException( "There is no update stack workflow for stack id ${stackId}" );
    }
    if ( updateStackWorkflowEntities.size( ) > 1 ) {
      throw new InternalFailureException( "More than one update stack workflow was found for stack id ${stackId}" );
    }
    String status = updateStackWorkflowEntities.get( 0 ).with{
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
    LOG.debug("Update stack status = " + status);
    return status;
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
    stackEntity.setStackStatus(Status.valueOf(status));
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
          throw new RetryAfterConditionCheckFailedException("A monitoring workflow for " + stackId + " has not yet been canceled");
        }
      }
    }
    List<StackWorkflowEntity> createWorkflows = StackWorkflowEntityManager.getStackWorkflowEntities(stackId, CREATE_STACK_WORKFLOW);
    if (createWorkflows != null) {
      for (StackWorkflowEntity workflow : createWorkflows) {
        if (isWorkflowOpen(simpleWorkflowClient, workflow)) {
          throw new RetryAfterConditionCheckFailedException("A create workflow for " + stackId + " has not yet been canceled");
        }
      }
    }
    return "";
  }

  @Override
  public Integer getAWSCloudFormationWaitConditionTimeout(String resourceId, String stackId, String accountId, String effectiveUserId) {
    try {
      StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
      StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResourceInUse(stackId, accountId, resourceId);
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
        LOG.info("Error getting timeout for resource " + resourceId);
        LOG.error(ex, ex);
        throw new ResourceFailureException(rootCause.getClass().getName() + ":" + rootCause.getMessage());
    }
  }
  @Override
  public String validateAWSParameterTypes(String stackId, String accountId, String effectiveUserId) {
    try {
      StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
      Map<String, ParameterType> parameterTypeMap = new TemplateParser().getParameterTypeMap(stackEntity.getTemplateBody());
      for (StackEntity.Parameter parameter: StackEntityHelper.jsonToParameters(stackEntity.getParametersJson())) {
        AWSParameterTypeValidationHelper.validateParameter(parameter, parameterTypeMap.get(parameter.getKey()), effectiveUserId);
      }
    } catch (Exception e) {
      LOG.error(e, e);
      throw e;
    }
    return "";
  }

  @Override
  public String cancelOutstandingCreateResources(String stackId, String accountId, String cancelMessage) {
    List<StackResourceEntity> stackResourceEntityList = StackResourceEntityManager.getStackResources(stackId, accountId);
    for (StackResourceEntity stackResourceEntity: stackResourceEntityList) {
      if (stackResourceEntity.getResourceStatus() == Status.CREATE_IN_PROGRESS) {
        stackResourceEntity.setResourceStatus(Status.CREATE_FAILED);
        stackResourceEntity.setResourceStatusReason(cancelMessage);
        StackResourceEntityManager.updateStackResource(stackResourceEntity);
        StackEventEntityManager.addStackEvent(stackResourceEntity);
      }
    }
    return "";
  }

  @Override
  public String cancelOutstandingUpdateResources(String stackId, String accountId, String cancelMessage) {
    List<StackResourceEntity> stackResourceEntityList = StackResourceEntityManager.getStackResources(stackId, accountId);
    for (StackResourceEntity stackResourceEntity: stackResourceEntityList) {
      if (stackResourceEntity.getResourceStatus() == Status.UPDATE_IN_PROGRESS) {
        stackResourceEntity.setResourceStatus(Status.UPDATE_FAILED);
        stackResourceEntity.setResourceStatusReason(cancelMessage);
        StackResourceEntityManager.updateStackResource(stackResourceEntity);
        StackEventEntityManager.addStackEvent(stackResourceEntity);
      }
    }
    return "";
  }

  @Override
  public String initUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, String reverseDependentResourcesJson) {
    LOG.info("Updating resource " + resourceId);
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    StackResourceEntity oldStackResourceEntity = StackResourceEntityManager.getStackResourceInUse(stackId, accountId, resourceId);

    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResourceForUpdate(stackId, accountId, resourceId);
    ArrayList<String> reverseDependentResourceIds = (reverseDependentResourcesJson == null) ? new ArrayList<String>()
      : (ArrayList<String>) new ObjectMapper().readValue(reverseDependentResourcesJson, new TypeReference<ArrayList<String>>() {
    })
    Map<String, ResourceInfo> resourceInfoMap = Maps.newLinkedHashMap();
    for (String reverseDependentResourceId : reverseDependentResourceIds) {
      resourceInfoMap.put(reverseDependentResourceId, StackResourceEntityManager.getResourceInfo(stackId, accountId, reverseDependentResourceId));
    }
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    if (!resourceInfo.getAllowedByCondition()) {
      LOG.info("Resource " + resourceId + " not allowed by condition, skipping");
      return "SKIP"; //TODO: codify this somewhere...
    };
    updateResourceInfoFields(resourceInfo, stackEntity, resourceInfoMap, effectiveUserId);
    stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
    StackResourceEntityManager.updateStackResource(stackResourceEntity);
    if (oldStackResourceEntity == null) {
      // copy to the other table
      StackResourceEntity stackResourceEntityInUse = new StackResourceEntityInUse();
      StackResourceEntityManager.copyStackResourceEntityData(stackResourceEntity, stackResourceEntityInUse);
      StackResourceEntityManager.addStackResource(stackResourceEntityInUse);
      // delete from the update table
      stackResourceEntity.setRecordDeleted(Boolean.TRUE);
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
      return "CREATE";
    }
    if (oldStackResourceEntity.getResourceStatus() == Status.NOT_STARTED) {
      // also copy to the other table
      StackResourceEntityManager.copyStackResourceEntityData(stackResourceEntity, oldStackResourceEntity);
      StackResourceEntityManager.updateStackResource(oldStackResourceEntity);
      // delete from the update table
      stackResourceEntity.setRecordDeleted(Boolean.TRUE);
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
      return "CREATE";
    }
    boolean nothingOutsidePropertiesChanged = true;
    if (Objects.equals(oldStackResourceEntity.getDeletionPolicy(), stackResourceEntity.getDeletionPolicy())) {
      nothingOutsidePropertiesChanged = false;
    }
    if (Objects.equals(oldStackResourceEntity.getDescription(), stackResourceEntity.getDescription())) {
      nothingOutsidePropertiesChanged = false;
    }
    JsonNode oldMetadata = JsonHelper.getJsonNodeFromString(oldStackResourceEntity.getPropertiesJson());
    JsonNode metadata = JsonHelper.getJsonNodeFromString(stackResourceEntity.getPropertiesJson());
    if (Objects.equals(oldMetadata, metadata)) {
      nothingOutsidePropertiesChanged = false;
    }
    JsonNode oldUpdatePolicy = JsonHelper.getJsonNodeFromString(oldStackResourceEntity.getUpdatePolicyJson());
    JsonNode updatePolicy = JsonHelper.getJsonNodeFromString(stackResourceEntity.getUpdatePolicyJson());
    if (Objects.equals(oldUpdatePolicy, updatePolicy)) {
      nothingOutsidePropertiesChanged = false;
    }
    JsonNode oldProperties = JsonHelper.getJsonNodeFromString(oldStackResourceEntity.getPropertiesJson());
    JsonNode properties = JsonHelper.getJsonNodeFromString(stackResourceEntity.getPropertiesJson());
    boolean propertiesChanged = !Objects.equals(oldProperties, properties);
    if (nothingOutsidePropertiesChanged && !propertiesChanged) {
      // nothing changed, just remove the new one
      stackResourceEntity.setRecordDeleted(Boolean.TRUE);
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
      return "NONE";
    }
    if (!nothingOutsidePropertiesChanged) {
      // change all the non-properties items in the old stack.  (We can always change them back later)
      oldStackResourceEntity.setDeletionPolicy(stackResourceEntity.getDeletionPolicy());
      oldStackResourceEntity.setDescription(stackResourceEntity.getDescription());
      oldStackResourceEntity.setMetadataJson(stackResourceEntity.getMetadataJson());
      oldStackResourceEntity.setUpdatePolicyJson(stackResourceEntity.getUpdatePolicyJson());
      StackResourceEntityManager.updateStackResource(oldStackResourceEntity);
    }
    if (!propertiesChanged) {
      // we are essentially done in this case, remove the new one
      // nothing changed, just remove the new one
      stackResourceEntity.setRecordDeleted(Boolean.TRUE);
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
      return "NO_PROPERTIES";
    }
    ResourceInfo oldResourceInfo = StackResourceEntityManager.getResourceInfo(oldStackResourceEntity);
    ResourceAction oldResourceAction = new ResourceResolverManager().resolveResourceAction(oldResourceInfo.getType());
    oldResourceAction.setStackEntity(stackEntity); // NOTE: stack entity has been changed with new values but nothing (yet) is used from it
    oldResourceInfo.setEffectiveUserId(effectiveUserId);
    oldResourceAction.setResourceInfo(resourceInfo);
    ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
    resourceAction.setStackEntity(stackEntity);
    resourceInfo.setEffectiveUserId(effectiveUserId);
    resourceAction.setResourceInfo(resourceInfo);
    ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()));
    UpdateType updateType = oldResourceAction.getUpdateType(resourceAction);
    if (updateType == UpdateType.NO_INTERRUPTION || updateType == UpdateType.SOME_INTERRUPTION) {
      oldStackResourceEntity.setResourceStatus(Status.UPDATE_IN_PROGRESS);
      oldStackResourceEntity.setResourceStatusReason(null);
      stackResourceEntity.setResourceStatus(Status.UPDATE_IN_PROGRESS);
      stackResourceEntity.setResourceStatusReason(null);
      StackResourceEntityManager.updateStackResource(oldStackResourceEntity);
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
    } else if (updateType == UpdateType.NEEDS_REPLACEMENT) {
      // put the old one in the cleanup pile
      StackResourceEntity cleanupEntity = new StackResourceEntityForCleanup();
      StackResourceEntityManager.copyStackResourceEntityData(oldStackResourceEntity, cleanupEntity);
      StackResourceEntityManager.addStackResource(cleanupEntity);
      // move the new one to the old one and update the status (and physical resource id for now)
      stackResourceEntity.setPhysicalResourceId(oldStackResourceEntity.getPhysicalResourceId());
      stackResourceEntity.setResourceStatus(Status.UPDATE_IN_PROGRESS);
      stackResourceEntity.setResourceStatusReason("Requested update requires the creation of a new physical resource; hence creating one.");
      StackEventEntityManager.addStackEvent(stackResourceEntity);
      StackResourceEntityManager.copyStackResourceEntityData(stackResourceEntity, oldStackResourceEntity);
      StackResourceEntityManager.updateStackResource(oldStackResourceEntity);
      // remove the new one from the update table
      stackResourceEntity.setRecordDeleted(Boolean.TRUE);
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
    }
    return updateType.toString();
  }

  @Override
  public String finalizeUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId) {
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResourceInUse(stackId, accountId, resourceId);
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
    ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()));
    resourceAction.setStackEntity(stackEntity);
    resourceInfo.setEffectiveUserId(effectiveUserId);
    resourceAction.setResourceInfo(resourceInfo);
    resourceInfo.setReady(Boolean.TRUE);
    stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
    stackResourceEntity.setResourceStatus(Status.UPDATE_COMPLETE);
    stackResourceEntity.setResourceStatusReason("");
    StackResourceEntityManager.updateStackResource(stackResourceEntity);
    StackEventEntityManager.addStackEvent(stackResourceEntity);
    LOG.info("Finished updating resource " + resourceId);
    return "";
  }

  @Override
  public Boolean performUpdateNoInterruptionStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId) {
    LOG.info("Performing update with no interruption step " + stepId + " on resource " + resourceId);
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    // This time we still modify the stack entity that is in use but our other one is in the update table.  We will call it new.
    StackResourceEntity newResourceEntity = StackResourceEntityManager.getStackResourceForUpdate(stackId, accountId, resourceId);
    ResourceInfo newResourceInfo = StackResourceEntityManager.getResourceInfo(newResourceEntity);

    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResourceInUse(stackId, accountId, resourceId);
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    try {
      ResourceAction newResourceAction = new ResourceResolverManager().resolveResourceAction(newResourceInfo.getType());
      newResourceAction.setStackEntity(stackEntity);
      newResourceInfo.setEffectiveUserId(effectiveUserId);
      newResourceAction.setResourceInfo(newResourceInfo);
      ResourcePropertyResolver.populateResourceProperties(newResourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(newResourceInfo.getPropertiesJson()));
      if (!(newResourceAction instanceof StepBasedResourceAction)) {
        throw new ClassCastException("Calling performUpdateNoInterruptionStep against a resource action that does not extend StepBasedResourceAction: " + newResourceAction.getClass().getName());
      }
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
      resourceAction.setStackEntity(stackEntity);
      resourceInfo.setEffectiveUserId(effectiveUserId);
      resourceAction.setResourceInfo(resourceInfo);
      ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()));
      if (!(resourceAction instanceof StepBasedResourceAction)) {
        throw new ClassCastException("Calling performUpdateNoInterruptionStep against a resource action that does not extend StepBasedResourceAction: " + resourceAction.getClass().getName());
      }

      UpdateStep updateStep = ((StepBasedResourceAction) resourceAction).getUpdateNoInterruptionStep(stepId);
      resourceAction = updateStep.perform(resourceAction, newResourceAction);
      resourceInfo = resourceAction.getResourceInfo();
      stackResourceEntity.setDescription(""); // deal later
      stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
    } catch (NotAResourceFailureException ex) {
      LOG.info( "Update step not yet complete: ${ex.message}" );
      LOG.debug(ex, ex);
      return false;
    } catch (Exception ex) {
      LOG.info("Error creating resource " + resourceId);
      LOG.error(ex, ex);
      stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
      stackResourceEntity.setResourceStatus(Status.UPDATE_FAILED);
      Throwable rootCause = Throwables.getRootCause(ex);
      stackResourceEntity.setResourceStatusReason("" + rootCause.getMessage());
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
      StackEventEntityManager.addStackEvent(stackResourceEntity);
      throw new ResourceFailureException(rootCause.getClass().getName() + ":" + rootCause.getMessage());
    }
    return true;
  }

  @Override
  public Boolean performUpdateSomeInterruptionStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId) {
    LOG.info("Performing update with some interruption step " + stepId + " on resource " + resourceId);
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    // This time we still modify the stack entity that is in use but our other one is in the update table.  We will call it new.
    StackResourceEntity newResourceEntity = StackResourceEntityManager.getStackResourceForUpdate(stackId, accountId, resourceId);
    ResourceInfo newResourceInfo = StackResourceEntityManager.getResourceInfo(newResourceEntity);

    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResourceInUse(stackId, accountId, resourceId);
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    try {
      ResourceAction newResourceAction = new ResourceResolverManager().resolveResourceAction(newResourceInfo.getType());
      newResourceAction.setStackEntity(stackEntity);
      newResourceInfo.setEffectiveUserId(effectiveUserId);
      newResourceAction.setResourceInfo(newResourceInfo);
      ResourcePropertyResolver.populateResourceProperties(newResourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(newResourceInfo.getPropertiesJson()));
      if (!(newResourceAction instanceof StepBasedResourceAction)) {
        throw new ClassCastException("Calling performUpdateSomeInterruptionStep against a resource action that does not extend StepBasedResourceAction: " + newResourceAction.getClass().getName());
      }
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
      resourceAction.setStackEntity(stackEntity);
      resourceInfo.setEffectiveUserId(effectiveUserId);
      resourceAction.setResourceInfo(resourceInfo);
      ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()));
      if (!(resourceAction instanceof StepBasedResourceAction)) {
        throw new ClassCastException("Calling performUpdateSomeInterruptionStep against a resource action that does not extend StepBasedResourceAction: " + resourceAction.getClass().getName());
      }

      UpdateStep updateStep = ((StepBasedResourceAction) resourceAction).getUpdateSomeInterruptionStep(stepId);
      resourceAction = updateStep.perform(resourceAction, newResourceAction);
      resourceInfo = resourceAction.getResourceInfo();
      stackResourceEntity.setDescription(""); // deal later
      stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
    } catch (NotAResourceFailureException ex) {
      LOG.info( "Update step not yet complete: ${ex.message}" );
      LOG.debug(ex, ex);
      return false;
    } catch (Exception ex) {
      LOG.info("Error creating resource " + resourceId);
      LOG.error(ex, ex);
      stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
      stackResourceEntity.setResourceStatus(Status.UPDATE_FAILED);
      Throwable rootCause = Throwables.getRootCause(ex);
      stackResourceEntity.setResourceStatusReason("" + rootCause.getMessage());
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
      StackEventEntityManager.addStackEvent(stackResourceEntity);
      throw new ResourceFailureException(rootCause.getClass().getName() + ":" + rootCause.getMessage());
    }
    return true;
  }

  @Override
  public Boolean performUpdateWithReplacementStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId) {
    LOG.info("Performing update with replacement step " + stepId + " on resource " + resourceId);
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    // This time we still modify the stack entity that is in use but our other one is in the cleanup table.  We will call it old.
    StackResourceEntity oldResourceEntity = StackResourceEntityManager.getStackResourceForCleanup(stackId, accountId, resourceId);
    ResourceInfo oldResourceInfo = StackResourceEntityManager.getResourceInfo(oldResourceEntity);

    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResourceInUse(stackId, accountId, resourceId);
    String oldPhysicalResourceId = stackResourceEntity.getPhysicalResourceId();
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    try {
      ResourceAction oldResourceAction = new ResourceResolverManager().resolveResourceAction(oldResourceInfo.getType());
      oldResourceAction.setStackEntity(stackEntity);
      oldResourceInfo.setEffectiveUserId(effectiveUserId);
      oldResourceAction.setResourceInfo(oldResourceInfo);
      ResourcePropertyResolver.populateResourceProperties(oldResourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(oldResourceInfo.getPropertiesJson()));
      if (!(oldResourceAction instanceof StepBasedResourceAction)) {
        throw new ClassCastException("Calling performUpdateWithReplacementStep against a resource action that does not extend StepBasedResourceAction: " + oldResourceAction.getClass().getName());
      }
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
      resourceAction.setStackEntity(stackEntity);
      resourceInfo.setEffectiveUserId(effectiveUserId);
      resourceAction.setResourceInfo(resourceInfo);
      ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()));
      if (!(resourceAction instanceof StepBasedResourceAction)) {
        throw new ClassCastException("Calling performUpdateWithReplacementStep against a resource action that does not extend StepBasedResourceAction: " + resourceAction.getClass().getName());
      }

      UpdateStep updateStep = ((StepBasedResourceAction) resourceAction).getUpdateWithReplacementStep(stepId);
      resourceAction = updateStep.perform(oldResourceAction, resourceAction);
      resourceInfo = resourceAction.getResourceInfo();
      stackResourceEntity.setDescription(""); // deal later
      stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
      if (!Objects.equals(stackResourceEntity.getPhysicalResourceId(), oldPhysicalResourceId)) {
        stackResourceEntity.setResourceStatus(Status.UPDATE_IN_PROGRESS);
        stackResourceEntity.setResourceStatusReason("Resource creation Initiated");
        StackEventEntityManager.addStackEvent(stackResourceEntity);
      }
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
    } catch (NotAResourceFailureException ex) {
      LOG.info( "Update step not yet complete: ${ex.message}" );
      LOG.debug(ex, ex);
      return false;
    } catch (Exception ex) {
      LOG.info("Error creating resource " + resourceId);
      LOG.error(ex, ex);
      stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
      stackResourceEntity.setResourceStatus(Status.UPDATE_FAILED);
      Throwable rootCause = Throwables.getRootCause(ex);
      stackResourceEntity.setResourceStatusReason("" + rootCause.getMessage());
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
      StackEventEntityManager.addStackEvent(stackResourceEntity);
      throw new ResourceFailureException(rootCause.getClass().getName() + ":" + rootCause.getMessage());
    }
    return true;

  }

  @Override
  public String finalizeUpdateCleanupStack(String stackId, String accountId, String statusMessage) {
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    // remove all items from non-in-use stacks
    StackResourceEntityManager.deleteStackResourcesForCleanup(stackId, accountId);
    StackResourceEntityManager.deleteStackResourcesForUpdate(stackId, accountId);

    StackUpdateInfoEntityManager.deleteStackUpdateInfo(stackId, accountId);
    stackEntity.setStackStatus(Status.UPDATE_COMPLETE);
    stackEntity.setStackStatusReason(statusMessage);
    StackEntityManager.updateStack(stackEntity);
    return "";
  }

  @Override
  public String initUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId) {
    LOG.info("Deleting resource " + resourceId);
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    String stackName = stackEntity.getStackName();
    // check that it is in the cleanup pile
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResourceForCleanup(stackId, accountId, resourceId);
    if (stackResourceEntity != null && stackResourceEntity.getResourceStatus() != Status.DELETE_COMPLETE
      && stackResourceEntity.getResourceStatus() != Status.NOT_STARTED) {
      stackResourceEntity.setResourceStatus(Status.DELETE_IN_PROGRESS);
      stackResourceEntity.setResourceStatusReason(null);
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
      StackEventEntityManager.addStackEvent(stackResourceEntity);
      return "";
    }
    return "SKIP";
  }

  @Override
  public String failUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage) {
    LOG.info("Error deleting resource " + resourceId);
    LOG.error(errorMessage);
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResourceForCleanup(stackId, accountId, resourceId);
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    resourceInfo.setEffectiveUserId(effectiveUserId);
    stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
    stackResourceEntity.setResourceStatus(Status.DELETE_FAILED);
    stackResourceEntity.setResourceStatusReason(errorMessage);
    StackResourceEntityManager.updateStackResource(stackResourceEntity);
    StackEventEntityManager.addStackEvent(stackResourceEntity);
    return "FAILURE";
  }

  @Override
  public String finalizeUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId) {
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResourceForCleanup(stackId, accountId, resourceId);
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    resourceInfo.setEffectiveUserId(effectiveUserId);
    stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
    stackResourceEntity.setResourceStatus(Status.DELETE_COMPLETE);
    stackResourceEntity.setResourceStatusReason("");
    StackResourceEntityManager.updateStackResource(stackResourceEntity);
    StackEventEntityManager.addStackEvent(stackResourceEntity);
    LOG.info("Finished deleting resource " + resourceId);
    return "SUCCESS";
  }

  @Override
  public String removeUpdateCleanupResourceIfAppropriateFromStack(String resourceId, String stackId, String accountId) {
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId, accountId);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResourceInUse(stackId, accountId, resourceId);
    StackResourceEntity stackResourceEntityCleanup = StackResourceEntityManager.getStackResourceForCleanup(stackId, accountId, resourceId);
    // remove the cleanup one regardless
    if (stackResourceEntityCleanup != null) {
      stackResourceEntityCleanup.setRecordDeleted(Boolean.TRUE);
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
    }
    // remove the original one if it is the same resource (i.e. same physical resource id, i.e. with replacement)
    if (stackResourceEntity != null && stackResourceEntityCleanup != null
      && Objects.equals(stackResourceEntity.getPhysicalResourceId(), stackResourceEntityCleanup.getPhysicalResourceId())) {
      stackResourceEntity.setRecordDeleted(Boolean.TRUE);
      StackResourceEntityManager.updateStackResource(stackResourceEntityCleanup);
    }
  }

}
