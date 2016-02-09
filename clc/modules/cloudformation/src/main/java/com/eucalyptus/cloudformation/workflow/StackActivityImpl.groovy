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
import com.eucalyptus.cloudformation.entity.StackResourceEntityManager
import com.eucalyptus.cloudformation.entity.StackWorkflowEntity
import com.eucalyptus.cloudformation.entity.StackWorkflowEntityManager
import com.eucalyptus.cloudformation.entity.Status
import com.eucalyptus.cloudformation.entity.VersionedStackEntity
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
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateTypeAndDirection
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
  public String createGlobalStackEvent(String stackId, String accountId, String resourceStatus, String resourceStatusReason, int stackVersion) {
    LOG.info("Creating global stack event: " + resourceStatus);
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, stackVersion);
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
  public String initCreateResource(String resourceId, String stackId, String accountId, String effectiveUserId, String reverseDependentResourcesJson, int stackVersion) {
    LOG.info("Creating resource " + resourceId);
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, stackVersion);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, stackVersion);
    ArrayList<String> reverseDependentResourceIds = (reverseDependentResourcesJson == null) ? new ArrayList<String>()
      : (ArrayList<String>) new ObjectMapper().readValue(reverseDependentResourcesJson, new TypeReference<ArrayList<String>>() {
    })
    Map<String, ResourceInfo> resourceInfoMap = Maps.newLinkedHashMap();
    for (String reverseDependentResourceId : reverseDependentResourceIds) {
      resourceInfoMap.put(reverseDependentResourceId, StackResourceEntityManager.getResourceInfo(stackId, accountId, reverseDependentResourceId, stackVersion));
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
    // internal physical resource uuid is set during resource creation or 'update with replacement'.  It is used whenever a new physical resource id
    // is created.  New physical resource ids are not necessarily unique, so this is used as an extra check.
    stackResourceEntity.setInternalPhysicalResourceUuid(UUID.randomUUID( ).toString( ));
    stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
    StackResourceEntityManager.updateStackResource(stackResourceEntity);
    StackEventEntityManager.addStackEvent(stackResourceEntity);
    return "";
  }

  private void updateResourceInfoFields(ResourceInfo resourceInfo, VersionedStackEntity stackEntity, LinkedHashMap<String, ResourceInfo> resourceInfoMap, String effectiveUserId) {
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
  public String getResourceType(String stackId, String accountId, String resourceId, int resourceVersion) {
    return StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, resourceVersion).getResourceType();
  }

  @Override
  public String initDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId, int deletedResourceVersion) {
    LOG.info("Deleting resource " + resourceId);
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, deletedResourceVersion);
    String stackName = stackEntity.getStackName();
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, deletedResourceVersion);
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
  public String determineCreateResourceFailures(String stackId, String accountId, int createdResourceVersion) {
    Collection<String> failedResources = Lists.newArrayList();
    Collection<StackResourceEntity> cancelledResources = Lists.newArrayList();
    for (StackResourceEntity stackResourceEntity : StackResourceEntityManager.getStackResources(stackId, accountId, createdResourceVersion)) {
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
    return failedResources.isEmpty() ? "" : "The following resource(s) failed to create: " + failedResources + ".";
  }

  @Override
  public String determineUpdateResourceFailures(String stackId, String accountId, int updatedResourceVersion) {
    Collection<String> failedResources = Lists.newArrayList();
    Collection<StackResourceEntity> cancelledResources = Lists.newArrayList();
    for (StackResourceEntity stackResourceEntity : StackResourceEntityManager.getStackResources(stackId, accountId, updatedResourceVersion)) {
      if (stackResourceEntity.getResourceStatus() == Status.UPDATE_FAILED) {
        failedResources.add(stackResourceEntity.getLogicalResourceId());
      } else if (stackResourceEntity.getResourceStatus() == Status.UPDATE_IN_PROGRESS) {
        cancelledResources.add(stackResourceEntity);
        failedResources.add(stackResourceEntity.getLogicalResourceId());
      }
    }
    for (StackResourceEntity stackResourceEntity : cancelledResources) {
      stackResourceEntity.setResourceStatus(Status.UPDATE_FAILED);
      stackResourceEntity.setResourceStatusReason("Resource update cancelled");
      StackResourceEntityManager.updateStackResource(stackResourceEntity);
    }
    return failedResources.isEmpty() ? "" : "The following resource(s) failed to update: " + failedResources + ".";
  }


  @Override
  public String determineDeleteResourceFailures(String stackId, String accountId, int deletedResourceVersion) {
    Collection<String> failedResources = Lists.newArrayList();
    Collection<StackResourceEntity> cancelledResources = Lists.newArrayList();
    for (StackResourceEntity stackResourceEntity : StackResourceEntityManager.getStackResources(stackId, accountId, deletedResourceVersion)) {
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
    return failedResources.isEmpty() ? "" : "The following resource(s) failed to delete: " + failedResources + ".";
  }

  @Override
  public String finalizeCreateStack(String stackId, String accountId, String effectiveUserId, int createdStackVersion) {
    LOG.info("Finalizing create stack");
    try {
      setOutputs(stackId, accountId, effectiveUserId, Status.CREATE_COMPLETE, createdStackVersion)
    } catch (Exception e) {
      LOG.error(e, e);
      throw e;
    }
    LOG.info("Done finalizing create stack");
    return ""; // promiseFor() doesn't work on void return types
  }

  private void setOutputs(String stackId, String accountId, String effectiveUserId, Status status, int stackVersion) {
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, stackVersion);
    Map<String, ResourceInfo> resourceInfoMap = Maps.newLinkedHashMap();
    for (StackResourceEntity stackResourceEntity : StackResourceEntityManager.getStackResources(stackId, accountId, stackVersion)) {
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
    stackEntity.setStackStatus(status);
    StackEntityManager.updateStack(stackEntity);
  }

  @Override
  public String finalizeUpdateStack(String stackId, String accountId, String effectiveUserId, int updatedStackVersion) {
    LOG.info("Finalizing update stack");
    try {
      setOutputs(stackId, accountId, effectiveUserId, Status.UPDATE_COMPLETE_CLEANUP_IN_PROGRESS, updatedStackVersion)
    } catch (Exception e) {
      LOG.error(e, e);
      throw e;
    }
    LOG.info("Done finalizing update stack");
    return ""; // promiseFor() doesn't work on void return types
  }

  @Override
  public Boolean performCreateStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int createdResourceVersion) {
    LOG.info("Performing creation step " + stepId + " on resource " + resourceId);
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, createdResourceVersion);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, createdResourceVersion);
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
  public Boolean performDeleteStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int deletedResourceVersion) {
    LOG.info("Performing delete step " + stepId + " on resource " + resourceId);
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, deletedResourceVersion);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, deletedResourceVersion);
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
  public String finalizeCreateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int createdResourceVersion) {
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, createdResourceVersion);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, createdResourceVersion);
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
  public String finalizeDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion) {
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, updatedResourceVersion);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedResourceVersion);
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
  public String failDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage, int deletedResourceVersion) {
    LOG.info("Error deleting resource " + resourceId);
    LOG.error(errorMessage);
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, deletedResourceVersion);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, deletedResourceVersion);
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
    StackResourceEntityManager.deleteStackResources(stackId, accountId);
    StackEventEntityManager.deleteStackEvents(stackId, accountId);
    StackEntityManager.deleteStack(stackId, accountId);
    LOG.info("Finished deleting all stack records");
    return ""; // promiseFor() doesn't work on void return types
  }

  @Override
  public String getCreateWorkflowExecutionCloseStatus(final String stackId) {
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
  public String getUpdateWorkflowExecutionCloseStatus(final String stackId) {
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
    LOG.info("Update stack status = " + status);
    return status;
  }

  @Override
  public String getStackStatus(String stackId, String accountId, int stackVersion) {
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, stackVersion);
    if (stackEntity == null) {
      throw new ValidationErrorException("No stack found with id " + stackId);
    }
    String status = stackEntity.getStackStatus().toString();
    LOG.info("status = " + status);
    return status;
  }

  public String setStackStatus(String stackId, String accountId, String status, String statusReason, int stackVersion) {
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, stackVersion);
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
  public Integer getAWSCloudFormationWaitConditionTimeout(String resourceId, String stackId, String accountId, String effectiveUserId, int resourceVersion) {
    try {
      VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, resourceVersion);
      StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, resourceVersion);
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
  public String validateAWSParameterTypes(String stackId, String accountId, String effectiveUserId, int stackVersion) {
    try {
      VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, stackVersion);
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
  public String cancelOutstandingCreateResources(String stackId, String accountId, String cancelMessage, int createdResourceVersion) {
    List<StackResourceEntity> stackResourceEntityList = StackResourceEntityManager.getStackResources(stackId, accountId, createdResourceVersion);
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
  public String cancelOutstandingUpdateResources(String stackId, String accountId, String cancelMessage, int updatedResourceVersion) {
    List<StackResourceEntity> stackResourceEntityList = StackResourceEntityManager.getStackResources(stackId, accountId, updatedResourceVersion);
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
  public String initUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, String reverseDependentResourcesJson, int updatedResourceVersion) {
    LOG.info("Determining if resource " + resourceId + " needs update");
    VersionedStackEntity nextStackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, updatedResourceVersion);
    VersionedStackEntity previousStackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, updatedResourceVersion - 1);
    StackResourceEntity previousStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedResourceVersion - 1);

    StackResourceEntity nextStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedResourceVersion);
    ArrayList<String> reverseDependentResourceIds = (reverseDependentResourcesJson == null) ? new ArrayList<String>()
      : (ArrayList<String>) new ObjectMapper().readValue(reverseDependentResourcesJson, new TypeReference<ArrayList<String>>() {
    })
    Map<String, ResourceInfo> resourceInfoMap = Maps.newLinkedHashMap();
    for (String reverseDependentResourceId : reverseDependentResourceIds) {
      resourceInfoMap.put(reverseDependentResourceId, StackResourceEntityManager.getResourceInfo(stackId, accountId, reverseDependentResourceId, updatedResourceVersion));
    }
    ResourceInfo nextResourceInfo = StackResourceEntityManager.getResourceInfo(nextStackResourceEntity);
    if (!nextResourceInfo.getAllowedByCondition()) {
      LOG.info("Resource " + resourceId + " not allowed by condition, skipping");
      return "SKIP"; //TODO: codify this somewhere...
    };
    updateResourceInfoFields(nextResourceInfo, nextStackEntity, resourceInfoMap, effectiveUserId);
    nextStackResourceEntity = StackResourceEntityManager.updateResourceInfo(nextStackResourceEntity, nextResourceInfo);
    StackResourceEntityManager.updateStackResource(nextStackResourceEntity);
    if (previousStackResourceEntity == null || previousStackResourceEntity.getResourceStatus() == Status.NOT_STARTED) {
      return "CREATE";
    }
    boolean nothingOutsidePropertiesChanged = true;
    if (!Objects.equals(previousStackResourceEntity.getDeletionPolicy(), nextStackResourceEntity.getDeletionPolicy())) {
      nothingOutsidePropertiesChanged = false;
    }
    if (!Objects.equals(previousStackResourceEntity.getDescription(), nextStackResourceEntity.getDescription())) {
      nothingOutsidePropertiesChanged = false;
    }
    JsonNode previousMetadata = JsonHelper.getJsonNodeFromString(previousStackResourceEntity.getPropertiesJson());
    JsonNode nextMetadata = JsonHelper.getJsonNodeFromString(nextStackResourceEntity.getPropertiesJson());
    if (!Objects.equals(previousMetadata, nextMetadata)) {
      nothingOutsidePropertiesChanged = false;
    }
    JsonNode previousUpdatePolicy = JsonHelper.getJsonNodeFromString(previousStackResourceEntity.getUpdatePolicyJson());
    JsonNode nextUpdatePolicy = JsonHelper.getJsonNodeFromString(nextStackResourceEntity.getUpdatePolicyJson());
    if (!Objects.equals(previousUpdatePolicy, nextUpdatePolicy)) {
      nothingOutsidePropertiesChanged = false;
    }
    JsonNode previousProperties = JsonHelper.getJsonNodeFromString(previousStackResourceEntity.getPropertiesJson());
    JsonNode nextProperties = JsonHelper.getJsonNodeFromString(nextStackResourceEntity.getPropertiesJson());
    boolean propertiesChanged = !Objects.equals(previousProperties, nextProperties);
    if (nothingOutsidePropertiesChanged && !propertiesChanged) {
      // nothing has changed, so copy the old values to the new one.
      StackResourceEntityManager.copyStackResourceEntityData(previousStackResourceEntity, nextStackResourceEntity);
      nextStackResourceEntity.setResourceVersion(updatedResourceVersion);
      StackResourceEntityManager.updateStackResource(nextStackResourceEntity);
      return "NONE";
    }

    // at this point something has changed -- copy the values from the old stack resource that are determined from
    // the previous run, rather than the parsed template
    nextStackResourceEntity.setPhysicalResourceId(previousStackResourceEntity.getPhysicalResourceId());
    nextStackResourceEntity.setReferenceValueJson(previousStackResourceEntity.getReferenceValueJson());
    nextStackResourceEntity.setResourceAttributesJson(previousStackResourceEntity.getResourceAttributesJson());
    nextStackResourceEntity.setResourceStatus(previousStackResourceEntity.getResourceStatus());
    nextStackResourceEntity.setResourceStatusReason(previousStackResourceEntity.getResourceStatusReason());
    // until we hear otherwise this is the same resource as the previous one... use the same unique id
    nextStackResourceEntity.setInternalPhysicalResourceUuid(previousStackResourceEntity.getInternalPhysicalResourceUuid());
    StackResourceEntityManager.updateStackResource(nextStackResourceEntity);

    if (!propertiesChanged) {
      return "NO_PROPERTIES";
    }
    ResourceInfo previousResourceInfo = StackResourceEntityManager.getResourceInfo(previousStackResourceEntity);
    ResourceAction previousResourceAction = new ResourceResolverManager().resolveResourceAction(previousResourceInfo.getType());
    previousResourceAction.setStackEntity(previousStackEntity); // NOTE: stack entity has been changed with new values but nothing (yet) is used from it
    previousResourceInfo.setEffectiveUserId(effectiveUserId);
    previousResourceAction.setResourceInfo(previousResourceInfo);
    ResourcePropertyResolver.populateResourceProperties(previousResourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(previousResourceInfo.getPropertiesJson()));
    ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(nextResourceInfo.getType());
    resourceAction.setStackEntity(nextStackEntity);
    nextResourceInfo.setEffectiveUserId(effectiveUserId);
    resourceAction.setResourceInfo(nextResourceInfo);
    ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(nextResourceInfo.getPropertiesJson()));
    UpdateType updateType = previousResourceAction.getUpdateType(resourceAction);
    if (updateType == UpdateType.NO_INTERRUPTION || updateType == UpdateType.SOME_INTERRUPTION) {
      nextStackResourceEntity.setResourceStatus(Status.UPDATE_IN_PROGRESS);
      nextStackResourceEntity.setResourceStatusReason(null);
      StackResourceEntityManager.updateStackResource(nextStackResourceEntity);
      StackEventEntityManager.addStackEvent(nextStackResourceEntity);
    } else if (updateType == UpdateType.NEEDS_REPLACEMENT) {
      nextStackResourceEntity.setResourceStatus(Status.UPDATE_IN_PROGRESS);
      nextStackResourceEntity.setResourceStatusReason("Requested update requires the creation of a new physical resource; hence creating one.");
      // internal physical resource uuid is set any time a new physical resource id would be created.  (During create and update with replacement).  This is done
      // as physical resource ids do not always change (i.e. AWS::EC2::SecurityGroupIngress)
      nextStackResourceEntity.setInternalPhysicalResourceUuid(UUID.randomUUID( ).toString( ));
      StackEventEntityManager.addStackEvent(nextStackResourceEntity);
      StackResourceEntityManager.updateStackResource(nextStackResourceEntity);
    } else if (updateType == UpdateType.NONE) { // This really shouldn't happen, as we have already shown properties have
      // changed.  copy everything and log it for now.
      // nothing has changed, so copy the old values to the new one.
      LOG.warn("Resource " + nextStackResourceEntity.getLogicalResourceId() + " on stack " + nextStackResourceEntity.getStackId() + " has changed properties, but the resource update type is NONE.  Copying the previous value");
      StackResourceEntityManager.copyStackResourceEntityData(previousStackResourceEntity, nextStackResourceEntity);
      nextStackResourceEntity.setResourceVersion(updatedResourceVersion);
      StackResourceEntityManager.updateStackResource(nextStackResourceEntity);
      return "NONE";

    }
    return updateType.toString();
  }

  @Override
  public String finalizeUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion) {
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, updatedResourceVersion);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedResourceVersion);
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
  public Boolean performUpdateStep(String updateTypeAndDirectionStr, String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion) {
    UpdateTypeAndDirection updateTypeAndDirection = UpdateTypeAndDirection.valueOf(updateTypeAndDirectionStr);
    LOG.info("Performing " + updateTypeAndDirection + " step " + stepId + " on resource " + resourceId);
    VersionedStackEntity nextStackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, updatedResourceVersion);
    StackResourceEntity nextStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedResourceVersion);
    ResourceInfo nextResourceInfo = StackResourceEntityManager.getResourceInfo(nextStackResourceEntity);

    VersionedStackEntity previousStackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, updatedResourceVersion - 1);
    StackResourceEntity previousStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedResourceVersion - 1);
    ResourceInfo previousResourceInfo = StackResourceEntityManager.getResourceInfo(previousStackResourceEntity);
    try {
      ResourceAction nextResourceAction = new ResourceResolverManager().resolveResourceAction(nextResourceInfo.getType());
      nextResourceAction.setStackEntity(nextStackEntity);
      nextResourceInfo.setEffectiveUserId(effectiveUserId);
      nextResourceAction.setResourceInfo(nextResourceInfo);
      ResourcePropertyResolver.populateResourceProperties(nextResourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(nextResourceInfo.getPropertiesJson()));
      if (!(nextResourceAction instanceof StepBasedResourceAction)) {
        throw new ClassCastException("Calling performUpdateStep against a resource action that does not extend StepBasedResourceAction: " + nextResourceAction.getClass().getName());
      }
      ResourceAction previousResourceAction = new ResourceResolverManager().resolveResourceAction(previousResourceInfo.getType());
      previousResourceAction.setStackEntity(previousStackEntity);
      previousResourceInfo.setEffectiveUserId(effectiveUserId);
      previousResourceAction.setResourceInfo(previousResourceInfo);
      ResourcePropertyResolver.populateResourceProperties(previousResourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(previousResourceInfo.getPropertiesJson()));
      if (!(previousResourceAction instanceof StepBasedResourceAction)) {
        throw new ClassCastException("Calling performUpdateStep against a resource action that does not extend StepBasedResourceAction: " + previousResourceAction.getClass().getName());
      }

      UpdateStep updateStep = ((StepBasedResourceAction) previousResourceAction).getUpdateStep(updateTypeAndDirection, stepId);
      nextResourceAction = updateStep.perform(previousResourceAction, nextResourceAction);
      nextResourceInfo = nextResourceAction.getResourceInfo();
      nextStackResourceEntity.setDescription(""); // deal later
      nextStackResourceEntity = StackResourceEntityManager.updateResourceInfo(nextStackResourceEntity, nextResourceInfo);
      StackResourceEntityManager.updateStackResource(nextStackResourceEntity);
    } catch (NotAResourceFailureException ex) {
      LOG.info( "Update step not yet complete: ${ex.message}" );
      LOG.debug(ex, ex);
      return false;
    } catch (Exception ex) {
      LOG.info("Error updating resource " + resourceId);
      LOG.error(ex, ex);
      nextStackResourceEntity = StackResourceEntityManager.updateResourceInfo(nextStackResourceEntity, nextResourceInfo);
      nextStackResourceEntity.setResourceStatus(Status.UPDATE_FAILED);
      Throwable rootCause = Throwables.getRootCause(ex);
      nextStackResourceEntity.setResourceStatusReason("" + rootCause.getMessage());
      StackResourceEntityManager.updateStackResource(nextStackResourceEntity);
      StackEventEntityManager.addStackEvent(nextStackResourceEntity);
      throw new ResourceFailureException(rootCause.getClass().getName() + ":" + rootCause.getMessage());
    }
    return true;
  }

  @Override
  public String finalizeUpdateCleanupStack(String stackId, String accountId, String statusMessage, int updatedStackVersion) {
    // get rid of all non-current stack versions and resources
    StackEntityManager.reallyDeleteAllStackVersionsExcept(stackId, accountId, updatedStackVersion)
    StackResourceEntityManager.reallyDeleteAllVersionsExcept(stackId, accountId, updatedStackVersion)
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, updatedStackVersion);
    stackEntity.setStackStatus(Status.UPDATE_COMPLETE);
    stackEntity.setStackStatusReason(statusMessage);
    StackEntityManager.updateStack(stackEntity);
    return "";
  }

  @Override
  public String initUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion) {
    LOG.info("Determining if resource " + resourceId + " needs deleting during cleanup");
    StackResourceEntity nextStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedResourceVersion);
    StackResourceEntity previousStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedResourceVersion - 1);
    // Delete if:
    // previous entity exists, in something other than a DELETE_COMPLETE or NOT_STARTED state, and has a different
    // physical resource id than the next one (next physical resource id = null if it doesn't exist).
    // There are actually some instances where the physical resource id is the same, so we have created an
    // internal physical resource uuid that is different in the needs update case...
    boolean previousEntityExists = previousStackResourceEntity != null;
    boolean previousEntityState = previousStackResourceEntity == null ? null : previousStackResourceEntity.getResourceStatus();
    boolean previousEntityInAppropriateState = (previousEntityState != Status.DELETE_COMPLETE) && (previousEntityState != Status.NOT_STARTED);
    String nextPhysicalResourceId = (nextStackResourceEntity == null || nextStackResourceEntity.getResourceStatus() == Status.NOT_STARTED) ? null : nextStackResourceEntity.getPhysicalResourceId();
    String nextInternalPhysicalResourceUuid = (nextStackResourceEntity == null || nextStackResourceEntity.getResourceStatus() == Status.NOT_STARTED) ? null : nextStackResourceEntity.getInternalPhysicalResourceUuid();
    if (previousEntityExists && previousEntityInAppropriateState &&
      (!Objects.equals(previousStackResourceEntity.getPhysicalResourceId(), nextPhysicalResourceId) ||
       !Objects.equals(previousStackResourceEntity.getInternalPhysicalResourceUuid(), nextInternalPhysicalResourceUuid)
      )
    ) {
      previousStackResourceEntity.setResourceStatus(Status.DELETE_IN_PROGRESS);
      previousStackResourceEntity.setResourceStatusReason(null);
      StackResourceEntityManager.updateStackResource(previousStackResourceEntity);
      StackEventEntityManager.addStackEvent(previousStackResourceEntity);
      return "";
    }
    return "SKIP";
  }

  @Override
  public String failUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage, int updatedResourceVersion) {
    LOG.info("Error deleting resource " + resourceId);
    LOG.error(errorMessage);
    StackResourceEntity previousStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedResourceVersion - 1);
    ResourceInfo previousResourceInfo = StackResourceEntityManager.getResourceInfo(previousStackResourceEntity);
    previousResourceInfo.setEffectiveUserId(effectiveUserId);
    previousStackResourceEntity = StackResourceEntityManager.updateResourceInfo(previousStackResourceEntity, previousResourceInfo);
    previousStackResourceEntity.setResourceStatus(Status.DELETE_FAILED);
    previousStackResourceEntity.setResourceStatusReason(errorMessage);
    StackResourceEntityManager.updateStackResource(previousStackResourceEntity);
    StackEventEntityManager.addStackEvent(previousStackResourceEntity);
    return "FAILURE";
  }

  @Override
  public String finalizeUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion) {
    StackResourceEntity previousStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedResourceVersion - 1);
    ResourceInfo previousResourceInfo = StackResourceEntityManager.getResourceInfo(previousStackResourceEntity);
    previousResourceInfo.setEffectiveUserId(effectiveUserId);
    previousStackResourceEntity = StackResourceEntityManager.updateResourceInfo(previousStackResourceEntity, previousResourceInfo);
    previousStackResourceEntity.setResourceStatus(Status.DELETE_COMPLETE);
    previousStackResourceEntity.setResourceStatusReason("");
    StackResourceEntityManager.updateStackResource(previousStackResourceEntity);
    StackEventEntityManager.addStackEvent(previousStackResourceEntity);
    LOG.info("Finished deleting resource " + resourceId);
    return "SUCCESS";
  }

  @Override
  public String initUpdateRollbackResource(String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion) {
    int updatedStackVersion = rolledBackResourceVersion - 1;
    LOG.info("Determining if resource " + resourceId + " needs update rollback");
    VersionedStackEntity updatedStackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, updatedStackVersion);
    StackResourceEntity updatedStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedStackVersion);

    VersionedStackEntity rolledbackStackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, rolledBackResourceVersion);
    // stack resource entity for rollback is still "behind" the updated version
    StackResourceEntity rolledbackStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedStackVersion - 1);
    if (updatedStackResourceEntity != null && updatedStackResourceEntity.getResourceStatus() != Status.NOT_STARTED) {
      rolledbackStackResourceEntity.setResourceStatus(updatedStackResourceEntity.getResourceStatus());
      rolledbackStackResourceEntity.setResourceStatusReason(updatedStackResourceEntity.getResourceStatusReason());
    }
    rolledbackStackResourceEntity.setResourceVersion(rolledBackResourceVersion);
    StackResourceEntityManager.updateStackResource(rolledbackStackResourceEntity);

    ResourceInfo rolledbackResourceInfo = StackResourceEntityManager.getResourceInfo(rolledbackStackResourceEntity);
    if (!rolledbackResourceInfo.getAllowedByCondition()) {
      LOG.info("Resource " + resourceId + " not allowed by condition, skipping");
      return "SKIP"; //TODO: codify this somewhere...
    };
    if (updatedStackResourceEntity == null || updatedStackResourceEntity.getResourceStatus() == Status.NOT_STARTED) {
      return "CREATE";
    }
    boolean nothingOutsidePropertiesChanged = true;
    if (!Objects.equals(rolledbackStackResourceEntity.getDeletionPolicy(), updatedStackResourceEntity.getDeletionPolicy())) {
      nothingOutsidePropertiesChanged = false;
    }
    if (!Objects.equals(rolledbackStackResourceEntity.getDescription(), updatedStackResourceEntity.getDescription())) {
      nothingOutsidePropertiesChanged = false;
    }
    JsonNode previousMetadata = JsonHelper.getJsonNodeFromString(rolledbackStackResourceEntity.getPropertiesJson());
    JsonNode nextMetadata = JsonHelper.getJsonNodeFromString(updatedStackResourceEntity.getPropertiesJson());
    if (!Objects.equals(previousMetadata, nextMetadata)) {
      nothingOutsidePropertiesChanged = false;
    }
    JsonNode previousUpdatePolicy = JsonHelper.getJsonNodeFromString(rolledbackStackResourceEntity.getUpdatePolicyJson());
    JsonNode nextUpdatePolicy = JsonHelper.getJsonNodeFromString(updatedStackResourceEntity.getUpdatePolicyJson());
    if (!Objects.equals(previousUpdatePolicy, nextUpdatePolicy)) {
      nothingOutsidePropertiesChanged = false;
    }
    JsonNode previousProperties = JsonHelper.getJsonNodeFromString(rolledbackStackResourceEntity.getPropertiesJson());
    JsonNode nextProperties = JsonHelper.getJsonNodeFromString(updatedStackResourceEntity.getPropertiesJson());

    boolean propertiesChanged = !Objects.equals(previousProperties, nextProperties);
    if (nothingOutsidePropertiesChanged && !propertiesChanged) {
      // nothing has changed, values should be the same (or correct)
      return "NONE";
    }

    if (!propertiesChanged) {
      return "NO_PROPERTIES";
    }

    // Update the resource info.
    rolledbackResourceInfo = StackResourceEntityManager.getResourceInfo(rolledbackStackResourceEntity);
    ResourceAction rolledbackResourceAction = new ResourceResolverManager().resolveResourceAction(rolledbackResourceInfo.getType());
    rolledbackResourceAction.setStackEntity(rolledbackStackEntity);
    rolledbackResourceInfo.setEffectiveUserId(effectiveUserId);
    rolledbackResourceAction.setResourceInfo(rolledbackResourceInfo);
    boolean errorWithProperties = false;
    try {
      ResourcePropertyResolver.populateResourceProperties(rolledbackResourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(rolledbackResourceInfo.getPropertiesJson()));
    } catch (Exception ex) {
      errorWithProperties = true;
    }
    ResourceInfo updatedResourceInfo = StackResourceEntityManager.getResourceInfo(updatedStackResourceEntity);
    ResourceAction updatedResourceAction = new ResourceResolverManager().resolveResourceAction(updatedResourceInfo.getType());
    updatedResourceAction.setStackEntity(updatedStackEntity);
    updatedResourceInfo.setEffectiveUserId(effectiveUserId);
    updatedResourceAction.setResourceInfo(updatedResourceInfo);
    try {
      ResourcePropertyResolver.populateResourceProperties(updatedResourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(updatedResourceInfo.getPropertiesJson()));
    } catch (Exception ex) {
      errorWithProperties = true;
    }
    if (errorWithProperties) {
      return UpdateType.UNSUPPORTED; // (We can't check the update type, so there was obviously an error on the way "in", so treat it as if it were unsupported (same logic)
    }
    // We check the update type going the original way (sometimes going backwards has a different value, such as IAM Access key with serial, needing the new number to be higher.)
    UpdateType updateType = rolledbackResourceAction.getUpdateType(updatedResourceAction);
    if (updateType == UpdateType.NO_INTERRUPTION || updateType == UpdateType.SOME_INTERRUPTION) {
      updatedStackResourceEntity.setResourceStatus(Status.UPDATE_IN_PROGRESS);
      updatedStackResourceEntity.setResourceStatusReason(null);
      StackResourceEntityManager.updateStackResource(updatedStackResourceEntity);
      StackEventEntityManager.addStackEvent(updatedStackResourceEntity);
    }
    // Unsupported update types or "needs replacement" ones don't need status updates
    return updateType.toString();
  }

  public String finalizeUpdateRollbackResource(String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion) {
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, rolledBackResourceVersion);
    stackResourceEntity.setResourceStatus(Status.UPDATE_COMPLETE);
    stackResourceEntity.setResourceStatusReason("");
    StackResourceEntityManager.updateStackResource(stackResourceEntity);
    StackEventEntityManager.addStackEvent(stackResourceEntity);
    LOG.info("Finished updating resource " + resourceId);
    return "";
  }


  public String finalizeUpdateRollbackStack(String stackId, String accountId, int rolledBackStackVersion) {
    createGlobalStackEvent(stackId, accountId, Status.UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS.toString(), "", rolledBackStackVersion);
    return "SUCCESS";
  }

  public String failUpdateRollbackStack(String stackId, String accountId, int rolledBackStackVersion, String errorMessage) {
    StackResourceEntityManager.flattenResources(stackId, accountId, rolledBackStackVersion);
    StackEntityManager.reallyDeleteAllStackVersionsExcept(stackId, accountId, rolledBackStackVersion);
    createGlobalStackEvent(stackId, accountId, Status.UPDATE_ROLLBACK_FAILED.toString(), "", rolledBackStackVersion);
    return "FAILURE";
  }

  public String finalizeUpdateRollbackCleanupStack(String stackId, String accountId, String statusMessage, int rolledBackStackVersion) {
    // get rid of all non-current stack versions and resources
    StackEntityManager.reallyDeleteAllStackVersionsExcept(stackId, accountId, rolledBackStackVersion)
    StackResourceEntityManager.reallyDeleteAllVersionsExcept(stackId, accountId, rolledBackStackVersion)
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, rolledBackStackVersion);
    stackEntity.setStackStatus(Status.UPDATE_ROLLBACK_COMPLETE);
    stackEntity.setStackStatusReason(statusMessage);
    StackEntityManager.updateStack(stackEntity);
    return "";
  }
  public String initUpdateRollbackCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion) {
    LOG.info("Determining if resource " + resourceId + " needs deleting during cleanup");
    StackResourceEntity nextStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, rolledBackResourceVersion);
    StackResourceEntity previousStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, rolledBackResourceVersion - 1);
    // Delete if:
    // previous entity exists, in something other than a DELETE_COMPLETE or NOT_STARTED state, and has a different
    // physical resource id than the next one (next physical resource id = null if it doesn't exist)
    // There are actually some instances where the physical resource id is the same, so we have created an
    // internal physical resource uuid that is different in the needs update case...
    boolean previousEntityExists = previousStackResourceEntity != null;
    boolean previousEntityState = previousStackResourceEntity == null ? null : previousStackResourceEntity.getResourceStatus();
    boolean previousEntityInAppropriateState = (previousEntityState != Status.DELETE_COMPLETE) && (previousEntityState != Status.NOT_STARTED);
    String nextPhysicalResourceId = (nextStackResourceEntity == null || nextStackResourceEntity.getResourceStatus() == Status.NOT_STARTED) ? null : nextStackResourceEntity.getPhysicalResourceId();
    String nextInternalPhysicalResourceUuid = (nextStackResourceEntity == null || nextStackResourceEntity.getResourceStatus() == Status.NOT_STARTED) ? null : nextStackResourceEntity.getInternalPhysicalResourceUuid();
    if (previousEntityExists && previousEntityInAppropriateState &&
      (!Objects.equals(previousStackResourceEntity.getPhysicalResourceId(), nextPhysicalResourceId) ||
        !Objects.equals(previousStackResourceEntity.getInternalPhysicalResourceUuid(), nextInternalPhysicalResourceUuid)
      )
    ) {
      previousStackResourceEntity.setResourceStatus(Status.DELETE_IN_PROGRESS);
      previousStackResourceEntity.setResourceStatusReason(null);
      StackResourceEntityManager.updateStackResource(previousStackResourceEntity);
      StackEventEntityManager.addStackEvent(previousStackResourceEntity);
      return "";
    }
    return "SKIP";
  }
  public String failUpdateRollbackCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage, int rolledBackResourceVersion){
    LOG.info("Error deleting resource " + resourceId);
    LOG.error(errorMessage);
    StackResourceEntity previousStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, rolledBackResourceVersion - 1);
    ResourceInfo previousResourceInfo = StackResourceEntityManager.getResourceInfo(previousStackResourceEntity);
    previousResourceInfo.setEffectiveUserId(effectiveUserId);
    previousStackResourceEntity = StackResourceEntityManager.updateResourceInfo(previousStackResourceEntity, previousResourceInfo);
    previousStackResourceEntity.setResourceStatus(Status.DELETE_FAILED);
    previousStackResourceEntity.setResourceStatusReason(errorMessage);
    StackResourceEntityManager.updateStackResource(previousStackResourceEntity);
    StackEventEntityManager.addStackEvent(previousStackResourceEntity);
    return "FAILURE";
  }

  public String finalizeUpdateRollbackCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion){
    StackResourceEntity previousStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, rolledBackResourceVersion - 1);
    ResourceInfo previousResourceInfo = StackResourceEntityManager.getResourceInfo(previousStackResourceEntity);
    previousResourceInfo.setEffectiveUserId(effectiveUserId);
    previousStackResourceEntity = StackResourceEntityManager.updateResourceInfo(previousStackResourceEntity, previousResourceInfo);
    previousStackResourceEntity.setResourceStatus(Status.DELETE_COMPLETE);
    previousStackResourceEntity.setResourceStatusReason("");
    StackResourceEntityManager.updateStackResource(previousStackResourceEntity);
    StackEventEntityManager.addStackEvent(previousStackResourceEntity);
    LOG.info("Finished deleting resource " + resourceId);
    return "SUCCESS";
  }

  public String rollbackStackState(String stackId, String accountId, int rolledBackStackVersion) {
    StackEntityManager.rollbackUpdateStack(stackId, accountId, rolledBackStackVersion);
    return "";
  }

}
