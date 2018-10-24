/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.cloudformation.workflow

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow
import com.amazonaws.services.simpleworkflow.model.DescribeWorkflowExecutionRequest
import com.amazonaws.services.simpleworkflow.model.RequestCancelWorkflowExecutionRequest
import com.amazonaws.services.simpleworkflow.model.UnknownResourceException
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionDetail
import com.eucalyptus.cloudformation.common.CloudFormation
import com.eucalyptus.cloudformation.InternalFailureException
import com.eucalyptus.cloudformation.ValidationErrorException
import com.eucalyptus.cloudformation.config.CloudFormationProperties
import com.eucalyptus.cloudformation.entity.DeleteStackWorkflowExtraInfoEntityManager
import com.eucalyptus.cloudformation.entity.SignalEntityManager
import com.eucalyptus.cloudformation.entity.StackEntity
import com.eucalyptus.cloudformation.entity.StackEntityHelper
import com.eucalyptus.cloudformation.entity.StackEntityManager
import com.eucalyptus.cloudformation.entity.StackEventEntityManager
import com.eucalyptus.cloudformation.entity.StackEventHelper
import com.eucalyptus.cloudformation.entity.StackResourceEntity
import com.eucalyptus.cloudformation.entity.StackResourceEntityManager
import com.eucalyptus.cloudformation.entity.StackUpdateInfoEntity
import com.eucalyptus.cloudformation.entity.StackUpdateInfoEntityManager
import com.eucalyptus.cloudformation.entity.StackWorkflowEntity
import com.eucalyptus.cloudformation.entity.StackWorkflowEntityManager
import com.eucalyptus.cloudformation.entity.StacksWithNoUpdateToPerformEntityManager
import com.eucalyptus.cloudformation.entity.Status
import com.eucalyptus.cloudformation.entity.VersionedStackEntity
import com.eucalyptus.cloudformation.resources.ResourceAction
import com.eucalyptus.cloudformation.resources.ResourceInfo
import com.eucalyptus.cloudformation.resources.ResourcePropertyResolver
import com.eucalyptus.cloudformation.resources.ResourceResolverManager
import com.eucalyptus.cloudformation.resources.standard.TagHelper
import com.eucalyptus.cloudformation.resources.standard.actions.AWSCloudFormationStackResourceAction
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
import com.google.common.base.Splitter
import com.google.common.base.Strings
import com.google.common.base.Throwables
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.netflix.glisten.ActivityOperations
import com.netflix.glisten.impl.swf.SwfActivityOperations
import groovy.transform.CompileStatic
import org.apache.log4j.Level
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
    StackEventHelper.createGlobalStackEvent(stackId, accountId, resourceStatus, resourceStatusReason, stackVersion);
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
    stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
    StackResourceEntityManager.updateStackResource(stackResourceEntity);
    StackEventEntityManager.addStackEvent(stackResourceEntity);
    return "";
  }

  private void updateResourceInfoFields(ResourceInfo resourceInfo, VersionedStackEntity stackEntity, Map<String, ResourceInfo> resourceInfoMap, String effectiveUserId) {
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
    // Update creation policy: (should we?)
    if (resourceInfo.getCreationPolicyJson() != null) {
      JsonNode creationPolicyJsonNode = JsonHelper.getJsonNodeFromString(resourceInfo.getCreationPolicyJson());
      List<String> creationPolicyKeys = Lists.newArrayList(creationPolicyJsonNode.fieldNames());
      for (String creationPolicyKey : creationPolicyKeys) {
        JsonNode evaluatedCreationPolicyNode = FunctionEvaluation.evaluateFunctions(creationPolicyJsonNode.get(creationPolicyKey), stackEntity, resourceInfoMap, effectiveUserId);
        if (IntrinsicFunctions.NO_VALUE.evaluateMatch(evaluatedCreationPolicyNode).isMatch()) {
          ((ObjectNode) creationPolicyJsonNode).remove(creationPolicyKey);
        } else {
          ((ObjectNode) creationPolicyJsonNode).put(creationPolicyKey, evaluatedCreationPolicyNode);
        }
      }
      resourceInfo.setCreationPolicyJson(JsonHelper.getStringFromJsonNode(creationPolicyJsonNode));
    }
  }

  @Override
  public String getResourceType(String stackId, String accountId, String resourceId, int resourceVersion) {
    return StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, resourceVersion).getResourceType();
  }

  @Override
  public String initDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId, int deletedResourceVersion, String retainedResourcesStr) {
    LOG.info("Deleting resource " + resourceId);
    List<String> retainedResources = Lists.newArrayList();
    if (!Strings.isNullOrEmpty(retainedResourcesStr)) {
      retainedResources.addAll(Splitter.on(",").omitEmptyStrings().splitToList(retainedResourcesStr));
    }
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
      if ("Retain".equals(resourceInfo.getDeletionPolicy()) || retainedResources.contains(resourceId) || stackResourceEntity.getResourceStatus() == Status.DELETE_SKIPPED) {
        LOG.info("Resource " + resourceId + " has a 'Retain' DeletionPolicy, already DELETE_SKIPPED, or is explicity retained in a delete-stack command, skipping.");
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
      StackEventEntityManager.addStackEvent(stackResourceEntity);
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
      StackEventEntityManager.addStackEvent(stackResourceEntity);
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
    setOutputs(stackId, accountId, effectiveUserId, Status.CREATE_COMPLETE, createdStackVersion)
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
    setOutputs(stackId, accountId, effectiveUserId, Status.UPDATE_COMPLETE_CLEANUP_IN_PROGRESS, updatedStackVersion)
    LOG.info("Done finalizing update stack");
    return ""; // promiseFor() doesn't work on void return types
  }

  @Override
  public Boolean performCreateStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int createdResourceVersion) {
    LOG.info("Performing creation step " + stepId + " on resource " + resourceId);
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, createdResourceVersion);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, createdResourceVersion);
    Boolean oldValueForCreatedEnoughToDelete = stackResourceEntity.getCreatedEnoughToDelete();
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    try {
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
      resourceAction.setStackEntity(stackEntity);
      resourceInfo.setEffectiveUserId(effectiveUserId);
      resourceAction.setResourceInfo(resourceInfo);
      // Note we do strict checking for properties here as it is our first look during create
      ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()), CloudFormationProperties.ENFORCE_STRICT_RESOURCE_PROPERTIES);
      if (!(resourceAction instanceof StepBasedResourceAction)) {
        throw new ClassCastException("Calling performCreateStep against a resource action that does not extend StepBasedResourceAction: " + resourceAction.getClass().getName());
      }
      Step createStep = ((StepBasedResourceAction) resourceAction).getCreateStep(stepId);
      resourceAction = createStep.perform(resourceAction);
      resourceInfo = resourceAction.getResourceInfo();
      stackResourceEntity.setDescription(""); // deal later
      stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
      if (!Objects.equals(stackResourceEntity.getCreatedEnoughToDelete(), oldValueForCreatedEnoughToDelete)) {
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
      LOG.error( "Error creating resource ${resourceId}: ${ex}", LOG.debugEnabled ? ex : null );
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
        // TODO: consider the strict property case.  It seems unnecessary here.
        ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()), false);
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
      LOG.error( "Error deleting resource ${resourceId}: ${ex}", LOG.debugEnabled ? ex : null );
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
    // TODO: consider the strict property case.  It seems unnecessary here. (done in perform create steps)
    ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()), false);
    resourceAction.setStackEntity(stackEntity);
    resourceInfo.setEffectiveUserId(effectiveUserId);
    resourceAction.setResourceInfo(resourceInfo);
    resourceInfo.setReady(Boolean.TRUE);
    stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
    stackResourceEntity.setResourceStatus(Status.CREATE_COMPLETE);
    stackResourceEntity.setResourceStatusReason("");
    StackResourceEntityManager.updateStackResource(stackResourceEntity);
    StackEventEntityManager.addStackEvent(stackResourceEntity);
    SignalEntityManager.deleteSignals(stackId, accountId, resourceId, createdResourceVersion); // no need anymore

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
    DeleteStackWorkflowExtraInfoEntityManager.deleteExtraInfoEntities(stackId);
    StackWorkflowEntityManager.deleteStackWorkflowEntities(stackId);
    StackUpdateInfoEntityManager.deleteStackUpdateInfo(stackId, accountId);
    StacksWithNoUpdateToPerformEntityManager.deleteStackWithNoUpdateToPerform(stackId, accountId);
    SignalEntityManager.deleteSignals(stackId, accountId);
    LOG.info("Finished deleting all stack records");
    return ""; // promiseFor() doesn't work on void return types
  }

  @Override
  public String getCreateWorkflowExecutionCloseStatus(final String stackId) {
    return getWorkflowExecutionCloseStatus(stackId, CREATE_STACK_WORKFLOW.toString());
  }

  @Override
  public String getUpdateWorkflowExecutionCloseStatus(final String stackId) {
    return getWorkflowExecutionCloseStatus(stackId, UPDATE_STACK_WORKFLOW.toString());
  }

  @Override
  public String getStackStatus(String stackId, String accountId, int stackVersion) {
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, stackVersion);
    if (stackEntity == null) {
      LOG.warn("No stack found with id " + stackId + " possibly due to a quick update after create.  Returning 'null' for status");
      return null;
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
        LOG.info("Cancelling open workflow " + workflow.getWorkflowType() + " for stack " + workflow.getStackId());
        try {
          RequestCancelWorkflowExecutionRequest requestCancelWorkflowRequest = new RequestCancelWorkflowExecutionRequest();
          requestCancelWorkflowRequest.setDomain(workflow.getDomain());
          requestCancelWorkflowRequest.setWorkflowId(workflow.getWorkflowId());
          requestCancelWorkflowRequest.setRunId(workflow.getRunId());
          simpleWorkflowClient.requestCancelWorkflowExecution(requestCancelWorkflowRequest);
        } catch (UnknownResourceException ex) {
          LOG.info("UnknownResourceFault found when trying to cancel open workflow " + workflow.getWorkflowType() + " for stack " + workflow.getStackId() + ", but that means the workflow no longer exists, so we don't care. ");
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
  public String validateAWSParameterTypes(String stackId, String accountId, String effectiveUserId, int stackVersion) {
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, stackVersion);
    Map<String, ParameterType> parameterTypeMap = new TemplateParser().getParameterTypeMap(stackEntity.getTemplateBody());
    for (StackEntity.Parameter parameter: StackEntityHelper.jsonToParameters(stackEntity.getParametersJson())) {
      AWSParameterTypeValidationHelper.validateParameter(parameter, parameterTypeMap.get(parameter.getKey()), effectiveUserId);
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
    JsonNode previousCreationPolicy = JsonHelper.getJsonNodeFromString(previousStackResourceEntity.getCreationPolicyJson());
    JsonNode nextCreationPolicy = JsonHelper.getJsonNodeFromString(nextStackResourceEntity.getCreationPolicyJson());
    if (!Objects.equals(previousCreationPolicy, nextCreationPolicy)) {
      nothingOutsidePropertiesChanged = false;
    }
    JsonNode previousProperties = JsonHelper.getJsonNodeFromString(previousStackResourceEntity.getPropertiesJson());
    JsonNode nextProperties = JsonHelper.getJsonNodeFromString(nextStackResourceEntity.getPropertiesJson());
    boolean propertiesChanged = !Objects.equals(previousProperties, nextProperties);


    ResourceInfo previousResourceInfo = StackResourceEntityManager.getResourceInfo(previousStackResourceEntity);
    ResourceAction previousResourceAction = new ResourceResolverManager().resolveResourceAction(previousResourceInfo.getType());

    boolean stackTagsChanged = !TagHelper.stackTagsEquals(previousStackEntity, nextStackEntity);
    boolean shouldCheckUpdateTypeForTags = previousResourceInfo.supportsTags() && stackTagsChanged;

    if (nothingOutsidePropertiesChanged && !propertiesChanged && !previousResourceAction.mustCheckUpdateTypeEvenIfNoPropertiesChanged() && !shouldCheckUpdateTypeForTags) {
      // nothing has changed, so copy the old values to the new one.
      StackResourceEntityManager.copyStackResourceEntityData(previousStackResourceEntity, nextStackResourceEntity);
      nextStackResourceEntity.setUpdateType("NONE"); // no update with replacement this time.
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
    // presumably we won't need to create something unless we are in the update with replacement case...
    nextStackResourceEntity.setCreatedEnoughToDelete(previousStackResourceEntity.getCreatedEnoughToDelete());
    StackResourceEntityManager.updateStackResource(nextStackResourceEntity);

    if (!propertiesChanged && !previousResourceAction.mustCheckUpdateTypeEvenIfNoPropertiesChanged() && !shouldCheckUpdateTypeForTags) {
      nextStackResourceEntity.setUpdateType("NO_PROPERTIES");
      StackResourceEntityManager.updateStackResource(nextStackResourceEntity);
      return "NO_PROPERTIES";
    }
    previousResourceAction.setStackEntity(previousStackEntity); // NOTE: stack entity has been changed with new values but nothing (yet) is used from it
    previousResourceInfo.setEffectiveUserId(effectiveUserId);
    previousResourceAction.setResourceInfo(previousResourceInfo);
    // TODO: consider the strict property case.  Previous stack may be non-strict, so no.
    ResourcePropertyResolver.populateResourceProperties(previousResourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(previousResourceInfo.getPropertiesJson()), false);
    ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(nextResourceInfo.getType());
    resourceAction.setStackEntity(nextStackEntity);
    nextResourceInfo.setEffectiveUserId(effectiveUserId);
    resourceAction.setResourceInfo(nextResourceInfo);
    // Note: here we do strict properties, as it is our first look at the new resource.
    ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(nextResourceInfo.getPropertiesJson()), CloudFormationProperties.ENFORCE_STRICT_RESOURCE_PROPERTIES);
    UpdateType updateType = previousResourceAction.getUpdateType(resourceAction, stackTagsChanged);
    if (updateType == UpdateType.NO_INTERRUPTION || updateType == UpdateType.SOME_INTERRUPTION) {
      nextStackResourceEntity.setResourceStatus(Status.UPDATE_IN_PROGRESS);
      nextStackResourceEntity.setResourceStatusReason(null);
      nextStackResourceEntity.setUpdateType(updateType.toString());
      StackResourceEntityManager.updateStackResource(nextStackResourceEntity);
      StackEventEntityManager.addStackEvent(nextStackResourceEntity);
    } else if (updateType == UpdateType.NEEDS_REPLACEMENT) {
      nextStackResourceEntity.setResourceStatus(Status.UPDATE_IN_PROGRESS);
      nextStackResourceEntity.setResourceStatusReason("Requested update requires the creation of a new physical resource; hence creating one.");
      nextStackResourceEntity.setUpdateType(updateType.toString());
      nextStackResourceEntity.setCreatedEnoughToDelete(Boolean.FALSE); // just in case it was set before...
      StackEventEntityManager.addStackEvent(nextStackResourceEntity);
      StackResourceEntityManager.updateStackResource(nextStackResourceEntity);
    } else if (updateType == UpdateType.NONE) { // This really shouldn't happen, as we have already shown properties have
      // changed.  copy everything and log it for now.
      // nothing has changed, so copy the old values to the new one.
      LOG.warn("Resource " + nextStackResourceEntity.getLogicalResourceId() + " on stack " + nextStackResourceEntity.getStackId() + " has changed properties, but the resource update type is NONE.  Copying the previous value");
      StackResourceEntityManager.copyStackResourceEntityData(previousStackResourceEntity, nextStackResourceEntity);
      nextStackResourceEntity.setUpdateType(updateType.toString());
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
    // TODO: consider the strict property case.  We checked it during init
    ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()), false);
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
      // TODO: consider the strict property case.  We already did it during initUpdateResource
      ResourcePropertyResolver.populateResourceProperties(nextResourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(nextResourceInfo.getPropertiesJson()), false);
      if (!(nextResourceAction instanceof StepBasedResourceAction)) {
        throw new ClassCastException("Calling performUpdateStep against a resource action that does not extend StepBasedResourceAction: " + nextResourceAction.getClass().getName());
      }
      ResourceAction previousResourceAction = new ResourceResolverManager().resolveResourceAction(previousResourceInfo.getType());
      previousResourceAction.setStackEntity(previousStackEntity);
      previousResourceInfo.setEffectiveUserId(effectiveUserId);
      previousResourceAction.setResourceInfo(previousResourceInfo);
      // TODO: consider the strict property case.  We don't check on previous stacks, as they are grandfathered.
      ResourcePropertyResolver.populateResourceProperties(previousResourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(previousResourceInfo.getPropertiesJson()), false);
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
      LOG.error( "Error updating resource ${resourceId}: ${ex}", LOG.debugEnabled ? ex : null );
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
  public String failUpdateUnsupportedResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage, int updatedResourceVersion) {
    VersionedStackEntity nextStackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, updatedResourceVersion);
    StackResourceEntity nextStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedResourceVersion);
    ResourceInfo nextResourceInfo = StackResourceEntityManager.getResourceInfo(nextStackResourceEntity);

    nextStackResourceEntity = StackResourceEntityManager.updateResourceInfo(nextStackResourceEntity, nextResourceInfo);
    nextStackResourceEntity.setResourceStatus(Status.UPDATE_FAILED);
    nextStackResourceEntity.setResourceStatusReason(errorMessage);
    StackResourceEntityManager.updateStackResource(nextStackResourceEntity);
    StackEventEntityManager.addStackEvent(nextStackResourceEntity);
    throw new ValidationErrorException(errorMessage);
  }

  @Override
  public String finalizeUpdateCleanupStack(String stackId, String accountId, String statusMessage, int updatedStackVersion) {
    // get rid of all non-current stack versions and resources
    StackEntityManager.reallyDeleteAllStackVersionsExcept(stackId, accountId, updatedStackVersion)
    StackResourceEntityManager.reallyDeleteAllVersionsExcept(stackId, accountId, updatedStackVersion)
    StackUpdateInfoEntityManager.deleteStackUpdateInfo(stackId, accountId);
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
    // previous entity exists, in something other than a DELETE_COMPLETE or NOT_STARTED state, and
    // the next entity either does not exist, is in state NOT_STARTED, or was updated via replacement.
    boolean previousEntityExists = previousStackResourceEntity != null;
    boolean previousEntityState = previousStackResourceEntity == null ? null : previousStackResourceEntity.getResourceStatus();
    boolean previousEntityInAppropriateState = (previousEntityState != Status.DELETE_COMPLETE) && (previousEntityState != Status.NOT_STARTED);
    boolean nextEntityExists = nextStackResourceEntity != null;
    boolean nextEntityState = nextStackResourceEntity == null ? null : nextStackResourceEntity.getResourceStatus();
    boolean nextEntityNotStarted = (nextEntityState == Status.NOT_STARTED);
    boolean nextEntityFromResourceReplacement = (nextEntityExists  && UpdateType.NEEDS_REPLACEMENT.toString().equals(nextStackResourceEntity.getUpdateType()));

    if (previousEntityExists && previousEntityInAppropriateState &&
      (!nextEntityExists || nextEntityNotStarted || nextEntityFromResourceReplacement)
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
    StackResourceEntity rolledbackStackResourceEntity;

    if (StackUpdateInfoEntityManager.isRollbackStartedResource(stackId, accountId, resourceId)) {
      // we've already moved the resource version
      rolledbackStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, rolledBackResourceVersion);
    } else {
      rolledbackStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedStackVersion - 1);
      if (updatedStackResourceEntity != null && updatedStackResourceEntity.getResourceStatus() != Status.NOT_STARTED) {
        rolledbackStackResourceEntity.setResourceStatus(updatedStackResourceEntity.getResourceStatus());
        rolledbackStackResourceEntity.setResourceStatusReason(updatedStackResourceEntity.getResourceStatusReason());
      }
      rolledbackStackResourceEntity.setResourceVersion(rolledBackResourceVersion);
      StackResourceEntityManager.updateStackResource(rolledbackStackResourceEntity);
      StackUpdateInfoEntityManager.addRollbackStartedResource(stackId, accountId, resourceId);
    }

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
    JsonNode previousCreationPolicy = JsonHelper.getJsonNodeFromString(rolledbackStackResourceEntity.getCreationPolicyJson());
    JsonNode nextCreationPolicy = JsonHelper.getJsonNodeFromString(updatedStackResourceEntity.getCreationPolicyJson());
    if (!Objects.equals(previousCreationPolicy, nextCreationPolicy)) {
      nothingOutsidePropertiesChanged = false;
    }
    JsonNode previousProperties = JsonHelper.getJsonNodeFromString(rolledbackStackResourceEntity.getPropertiesJson());
    JsonNode nextProperties = JsonHelper.getJsonNodeFromString(updatedStackResourceEntity.getPropertiesJson());

    boolean propertiesChanged = !Objects.equals(previousProperties, nextProperties);

    boolean stackTagsChanged = !TagHelper.stackTagsEquals(rolledbackStackEntity, updatedStackEntity);
    boolean shouldCheckUpdateTypeForTags = rolledbackResourceInfo.supportsTags() && stackTagsChanged;

    // Update the resource info.
    rolledbackResourceInfo = StackResourceEntityManager.getResourceInfo(rolledbackStackResourceEntity);
    ResourceAction rolledbackResourceAction = new ResourceResolverManager().resolveResourceAction(rolledbackResourceInfo.getType());
    rolledbackResourceAction.setStackEntity(rolledbackStackEntity);
    rolledbackResourceInfo.setEffectiveUserId(effectiveUserId);
    rolledbackResourceAction.setResourceInfo(rolledbackResourceInfo);

    if (nothingOutsidePropertiesChanged && !propertiesChanged && !rolledbackResourceAction.mustCheckUpdateTypeEvenIfNoPropertiesChanged() && !shouldCheckUpdateTypeForTags) {
      // nothing has changed, values should be the same (or correct)
      return "NONE";
    }

    if (!propertiesChanged && !rolledbackResourceAction.mustCheckUpdateTypeEvenIfNoPropertiesChanged() && !shouldCheckUpdateTypeForTags) {
      return "NO_PROPERTIES";
    }

    boolean errorWithProperties = false;
    try {
      // TODO: consider the strict property case.  This is kind of like delete, we've already validated strict or we are grandfathered
      ResourcePropertyResolver.populateResourceProperties(rolledbackResourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(rolledbackResourceInfo.getPropertiesJson()), false);
    } catch (Exception ex) {
      errorWithProperties = true;
    }
    ResourceInfo updatedResourceInfo = StackResourceEntityManager.getResourceInfo(updatedStackResourceEntity);
    ResourceAction updatedResourceAction = new ResourceResolverManager().resolveResourceAction(updatedResourceInfo.getType());
    updatedResourceAction.setStackEntity(updatedStackEntity);
    updatedResourceInfo.setEffectiveUserId(effectiveUserId);
    updatedResourceAction.setResourceInfo(updatedResourceInfo);
    try {
      // TODO: consider the strict property case.  This is kind of like delete, we've already validated strict
      ResourcePropertyResolver.populateResourceProperties(updatedResourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(updatedResourceInfo.getPropertiesJson()), false);
    } catch (Exception ex) {
      errorWithProperties = true;
    }
    if (errorWithProperties) {
      return UpdateType.UNSUPPORTED; // (We can't check the update type, so there was obviously an error on the way "in", so treat it as if it were unsupported (same logic)
    }
    // only put update in progress if we actually did an update and it was of no_interruption or some_interruption
    if (UpdateType.NO_INTERRUPTION.toString().equals(updatedStackResourceEntity.getUpdateType()) ||
      UpdateType.SOME_INTERRUPTION.toString().equals(updatedStackResourceEntity.getUpdateType())) {
      rolledbackStackResourceEntity.setResourceStatus(Status.UPDATE_IN_PROGRESS);
      rolledbackStackResourceEntity.setResourceStatusReason(null);
      StackResourceEntityManager.updateStackResource(rolledbackStackResourceEntity);
      StackEventEntityManager.addStackEvent(rolledbackStackResourceEntity);
    }
    // Unsupported update types or "needs replacement" ones don't need status updates
    // At this point just return the previous value
    return updatedStackResourceEntity.getUpdateType();
  }

  @Override
  public String finalizeUpdateRollbackResource(String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion) {
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, rolledBackResourceVersion);
    stackResourceEntity.setResourceStatus(Status.UPDATE_COMPLETE);
    stackResourceEntity.setResourceStatusReason("");
    StackResourceEntityManager.updateStackResource(stackResourceEntity);
    StackEventEntityManager.addStackEvent(stackResourceEntity);
    LOG.info("Finished updating resource " + resourceId);
    return "";
  }


  @Override
  public String finalizeUpdateRollbackStack(String stackId, String accountId, int rolledBackStackVersion) {
    createGlobalStackEvent(stackId, accountId, Status.UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS.toString(), "", rolledBackStackVersion);
    return "SUCCESS";
  }

  @Override
  public String failUpdateRollbackStack(String stackId, String accountId, int rolledBackStackVersion, String errorMessage) {
    createGlobalStackEvent(stackId, accountId, Status.UPDATE_ROLLBACK_FAILED.toString(), errorMessage, rolledBackStackVersion);
    return "FAILURE";
  }

  @Override
  public String finalizeUpdateRollbackCleanupStack(String stackId, String accountId, String statusMessage, int rolledBackStackVersion) {
    // get rid of all non-current stack versions and resources
    StackEntityManager.reallyDeleteAllStackVersionsExcept(stackId, accountId, rolledBackStackVersion)
    StackResourceEntityManager.reallyDeleteAllVersionsExcept(stackId, accountId, rolledBackStackVersion)
    StackUpdateInfoEntityManager.deleteStackUpdateInfo(stackId, accountId);
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, rolledBackStackVersion);
    stackEntity.setStackStatus(Status.UPDATE_ROLLBACK_COMPLETE);
    stackEntity.setStackStatusReason(statusMessage);
    StackEntityManager.updateStack(stackEntity);
    return "";
  }
  @Override
  public String initUpdateRollbackCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion) {
    LOG.info("Determining if resource " + resourceId + " needs deleting during cleanup");
    StackResourceEntity nextStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, rolledBackResourceVersion);
    StackResourceEntity previousStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, rolledBackResourceVersion - 1);
    // Delete cases:
    // 1) Previous entity was Updated With Replacement:  Both entities exist, not in DELETE_COMPLETE or NOT_STARTED.  Previous entity is 'updateWithReplacement'
    // 2) Previous entity was created.  Previous entity exists, is not in DELETE_COMPLETE or NOT_STARTED and not 'updateWithReplacement'.  Next entity does not exist or is in
    //    NOT_STARTED or DELETE_COMPLETE state.
    boolean updateWithReplacementCase = (previousStackResourceEntity != null) && (nextStackResourceEntity != null) &&
      (previousStackResourceEntity.getResourceStatus() != Status.DELETE_COMPLETE) &&
      (previousStackResourceEntity.getResourceStatus() != Status.NOT_STARTED) &&
      (nextStackResourceEntity.getResourceStatus() != Status.DELETE_COMPLETE) &&
      (nextStackResourceEntity.getResourceStatus() != Status.NOT_STARTED) &&
      (UpdateType.NEEDS_REPLACEMENT.toString().equals(previousStackResourceEntity.getUpdateType()));

    boolean createdCase = (previousStackResourceEntity != null) &&
      (previousStackResourceEntity.getResourceStatus() != Status.DELETE_COMPLETE) &&
      (previousStackResourceEntity.getResourceStatus() != Status.NOT_STARTED) &&
      (!UpdateType.NEEDS_REPLACEMENT.toString().equals(previousStackResourceEntity.getUpdateType())) &&
      (nextStackResourceEntity == null ||
        nextStackResourceEntity.getResourceStatus() == Status.NOT_STARTED ||
        nextStackResourceEntity.getResourceStatus() == Status.DELETE_COMPLETE);

    if (updateWithReplacementCase || createdCase) {
      previousStackResourceEntity.setResourceStatus(Status.DELETE_IN_PROGRESS);
      previousStackResourceEntity.setResourceStatusReason(null);
      StackResourceEntityManager.updateStackResource(previousStackResourceEntity);
      StackEventEntityManager.addStackEvent(previousStackResourceEntity);
      return "";
    }
    return "SKIP";
  }
  @Override
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

  @Override
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

  @Override
  public String initUpdateRollbackStack(String stackId, String accountId, int rolledBackStackVersion) {
    StackEntityManager.rollbackUpdateStack(stackId, accountId, rolledBackStackVersion);
    VersionedStackEntity nextStackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, rolledBackStackVersion);
    if (nextStackEntity.getStackStatus() != Status.UPDATE_ROLLBACK_IN_PROGRESS) {
      createGlobalStackEvent(stackId, accountId, Status.UPDATE_ROLLBACK_IN_PROGRESS.toString(), "", rolledBackStackVersion);
    }
    return "";
  }

  @Override
  public String flattenStackForDelete(String stackId, String accountId) {
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackByNameOrId(stackId, accountId);
    if (stackEntity == null) {
      throw new ValidationErrorException("No such stack " + stackId);
    }
    if (stackEntity.getStackStatus() == Status.UPDATE_ROLLBACK_FAILED) {
      StackUpdateInfoEntity stackUpdateRollbackInfoEntity = StackUpdateInfoEntityManager.getStackUpdateInfoEntity(stackId, accountId);
      if (stackUpdateRollbackInfoEntity == null) {
        throw new ValidationErrorException("No stack update rollback record for stack " + stackId);
      } else {
        StackUpdateInfoEntityManager.deleteStackUpdateInfo(stackId, accountId);
      }
      StackResourceEntityManager.flattenResources(stackId, accountId, stackUpdateRollbackInfoEntity.getUpdatedStackVersion() + 1);
      StackEntityManager.reallyDeleteAllStackVersionsExcept(stackId, accountId, stackUpdateRollbackInfoEntity.getUpdatedStackVersion() + 1);
    }
    return "";
  }

  @Override
  public String checkResourceAlreadyRolledBackOrStartedRollback(String stackId, String accountId, String resourceId) {
    if (StackUpdateInfoEntityManager.isRollbackCompletedResource(stackId, accountId, resourceId)) {
      return StackUpdateInfoEntity.RolledBackResource.RollbackStatus.COMPLETED.toString();
    } else if (StackUpdateInfoEntityManager.isRollbackStartedResource(stackId, accountId, resourceId)) {
      return StackUpdateInfoEntity.RolledBackResource.RollbackStatus.STARTED.toString();
    } else {
      return "";
    }
  }

  @Override
  public String addCompletedUpdateRollbackResource(String stackId, String accountId, String resourceId) {
    StackUpdateInfoEntityManager.addRollbackCompletedResource(stackId, accountId, resourceId);
    return "";
  }
  @Override
  public Boolean checkInnerStackUpdate(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion) {
    StackResourceEntity nextStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedResourceVersion);
    StackResourceEntity previousStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedResourceVersion - 1);
    if (previousStackResourceEntity != null && nextStackResourceEntity != null &&
        previousStackResourceEntity.getResourceStatus() != Status.DELETE_COMPLETE &&
        previousStackResourceEntity.getResourceStatus() != Status.NOT_STARTED &&
        nextStackResourceEntity.getResourceStatus() != Status.DELETE_COMPLETE &&
        nextStackResourceEntity.getResourceStatus() != Status.NOT_STARTED &&
        previousStackResourceEntity.getResourceType().equals("AWS::CloudFormation::Stack")) {
      LOG.info("Resource " + resourceId + " is a stack that needs updating during cleanup");
      return Boolean.TRUE;
    } else {
      return Boolean.FALSE;
    }
  }
  @Override
  public String initUpdateCleanupInnerStackUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion) {
    LOG.info("Resource " + resourceId + " is a stack that needs updating during cleanup");
    StackResourceEntity previousStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedResourceVersion - 1);
    previousStackResourceEntity.setResourceStatus(Status.UPDATE_IN_PROGRESS);
    previousStackResourceEntity.setResourceStatusReason(null);
    StackResourceEntityManager.updateStackResource(previousStackResourceEntity);
    StackEventEntityManager.addStackEvent(previousStackResourceEntity);
  }
  @Override
  public String finalizeUpdateCleanupInnerStackUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion) {
    StackResourceEntity previousStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedResourceVersion - 1);
    ResourceInfo previousResourceInfo = StackResourceEntityManager.getResourceInfo(previousStackResourceEntity);
    previousResourceInfo.setEffectiveUserId(effectiveUserId);
    previousStackResourceEntity = StackResourceEntityManager.updateResourceInfo(previousStackResourceEntity, previousResourceInfo);
    previousStackResourceEntity.setResourceStatus(Status.UPDATE_COMPLETE);
    previousStackResourceEntity.setResourceStatusReason("");
    StackResourceEntityManager.updateStackResource(previousStackResourceEntity);
    StackEventEntityManager.addStackEvent(previousStackResourceEntity);
    LOG.info("Finished updating resource " + resourceId);
    return "SUCCESS";
  }

  public Boolean performUpdateCleanupInnerStackUpdateStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion) {
    LOG.info("Performing update cleanup inner stack update step " + stepId + " on resource " + resourceId);
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, updatedResourceVersion);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedResourceVersion);
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    try {
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
      resourceAction.setStackEntity(stackEntity);
      resourceInfo.setEffectiveUserId(effectiveUserId);
      resourceAction.setResourceInfo(resourceInfo);
      boolean errorWithProperties = false;
      try {
        // TODO: consider the strict property case.  This is kind of like delete, I think, so I think we've already validated strict
        ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()), false);
      } catch (Exception ex) {
        errorWithProperties = true;
      }
      if (!errorWithProperties) {
        // if we have errors with properties we had them on create too, so we didn't start (really)
        if (!(resourceAction instanceof AWSCloudFormationStackResourceAction)) {
          throw new ClassCastException("Calling performUpdateCleanupInnerStackUpdateStep against a resource action that does not extend AWSCloudFormationStackResourceAction: " + resourceAction.getClass().getName());
        }
        Step updateCleanupInnerStackUpdateStep = ((AWSCloudFormationStackResourceAction) resourceAction).getUpdateCleanupUpdateStep(stepId);
        resourceAction = updateCleanupInnerStackUpdateStep.perform(resourceAction);
        resourceInfo = resourceAction.getResourceInfo();
        stackResourceEntity.setResourceStatus(Status.UPDATE_IN_PROGRESS);
        stackResourceEntity.setResourceStatusReason(null);
        stackResourceEntity.setDescription(""); // deal later
        stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
        StackResourceEntityManager.updateStackResource(stackResourceEntity);
      }
    } catch (NotAResourceFailureException ex) {
      LOG.info("Update cleanup Inner Stack Update step not yet complete: ${ex.message}");
      LOG.debug(ex, ex);
      return false;
    } catch (Exception ex) {
      LOG.error( "Error updating resource ${resourceId}: ${ex}", LOG.debugEnabled ? ex : null );
      Throwable rootCause = Throwables.getRootCause(ex);
      throw new ResourceFailureException(rootCause.getMessage());
      // Don't put the update failed step here as we need to return "failure" but this must be done in the caller
    }
    return true;
  }

  @Override
  public String initUpdateRollbackCleanupInnerStackUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion) {
    LOG.info("Resource " + resourceId + " is a stack that needs updating during rollback cleanup");
    StackResourceEntity previousStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedResourceVersion - 1);
    previousStackResourceEntity.setResourceStatus(Status.UPDATE_IN_PROGRESS);
    previousStackResourceEntity.setResourceStatusReason(null);
    StackResourceEntityManager.updateStackResource(previousStackResourceEntity);
    StackEventEntityManager.addStackEvent(previousStackResourceEntity);
  }
  @Override
  public String finalizeUpdateRollbackCleanupInnerStackUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion) {
    StackResourceEntity previousStackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedResourceVersion - 1);
    ResourceInfo previousResourceInfo = StackResourceEntityManager.getResourceInfo(previousStackResourceEntity);
    previousResourceInfo.setEffectiveUserId(effectiveUserId);
    previousStackResourceEntity = StackResourceEntityManager.updateResourceInfo(previousStackResourceEntity, previousResourceInfo);
    previousStackResourceEntity.setResourceStatus(Status.UPDATE_COMPLETE);
    previousStackResourceEntity.setResourceStatusReason("");
    StackResourceEntityManager.updateStackResource(previousStackResourceEntity);
    StackEventEntityManager.addStackEvent(previousStackResourceEntity);
    LOG.info("Finished updating resource " + resourceId);
    return "SUCCESS";
  }

  public Boolean performUpdateRollbackCleanupInnerStackUpdateStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion) {
    LOG.info("Performing update Rollback Cleanup inner stack update step " + stepId + " on resource " + resourceId);
    VersionedStackEntity stackEntity = StackEntityManager.getNonDeletedVersionedStackById(stackId, accountId, updatedResourceVersion);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, accountId, resourceId, updatedResourceVersion);
    ResourceInfo resourceInfo = StackResourceEntityManager.getResourceInfo(stackResourceEntity);
    try {
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
      resourceAction.setStackEntity(stackEntity);
      resourceInfo.setEffectiveUserId(effectiveUserId);
      resourceAction.setResourceInfo(resourceInfo);
      boolean errorWithProperties = false;
      try {
        // TODO: consider the strict property case.  This is kind of like delete, we've already validated strict, I think.
        ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()), false);
      } catch (Exception ex) {
        errorWithProperties = true;
      }
      if (!errorWithProperties) {
        // if we have errors with properties we had them on create too, so we didn't start (really)
        if (!(resourceAction instanceof AWSCloudFormationStackResourceAction)) {
          throw new ClassCastException("Calling performUpdateRollbackCleanupInnerStackUpdateStep against a resource action that does not extend AWSCloudFormationStackResourceAction: " + resourceAction.getClass().getName());
        }
        Step updateRollbackCleanupInnerStackUpdateStep = ((AWSCloudFormationStackResourceAction) resourceAction).getUpdateRollbackCleanupUpdateStep(stepId);
        resourceAction = updateRollbackCleanupInnerStackUpdateStep.perform(resourceAction);
        resourceInfo = resourceAction.getResourceInfo();
        stackResourceEntity.setResourceStatus(Status.UPDATE_IN_PROGRESS);
        stackResourceEntity.setResourceStatusReason(null);
        stackResourceEntity.setDescription(""); // deal later
        stackResourceEntity = StackResourceEntityManager.updateResourceInfo(stackResourceEntity, resourceInfo);
        StackResourceEntityManager.updateStackResource(stackResourceEntity);
      }
    } catch (NotAResourceFailureException ex) {
      LOG.info("Update RollbackCleanup Inner Stack Update step not yet complete: ${ex.message}");
      LOG.debug(ex, ex);
      return false;
    } catch (Exception ex) {
      LOG.error( "Error updating resource ${resourceId}: ${ex}", LOG.debugEnabled ? ex : null );
      Throwable rootCause = Throwables.getRootCause(ex);
      throw new ResourceFailureException(rootCause.getMessage());
      // Don't put the update failed step here as we need to return "failure" but this must be done in the caller
    }
    return true;
  }
  @Override
  public String kickOffUpdateRollbackCleanupStackWorkflow(String stackId, String accountId, String effectiveUserId) {
    UpdateStackPartsWorkflowKickOff.kickOffUpdateRollbackCleanupStackWorkflow(stackId, accountId, effectiveUserId);
    return "";
  }

  @Override
  public String kickOffUpdateRollbackStackWorkflow(String stackId, String accountId, String outerStackArn,String effectiveUserId) {
    UpdateStackPartsWorkflowKickOff.kickOffUpdateRollbackStackWorkflow(stackId, accountId, outerStackArn, effectiveUserId);
    return "";
  }

  @Override
  public String kickOffUpdateCleanupStackWorkflow(String stackId, String accountId, String effectiveUserId) {
    UpdateStackPartsWorkflowKickOff.kickOffUpdateCleanupStackWorkflow(stackId, accountId, effectiveUserId);
    return "";
  }


  @Override
  public String kickOffDeleteStackWorkflow(String effectiveUserId, String stackId, String stackName, String stackAccountId, String stackAccountAlias, String resourceDependencyManagerJson, int deletedStackVersion, String retainedResourcesStr) {
    CommonDeleteRollbackKickoff.kickOffDeleteStackWorkflow(effectiveUserId, stackId, stackName, stackAccountId, stackAccountAlias, resourceDependencyManagerJson, deletedStackVersion, retainedResourcesStr);
    return "";
  }

  @Override
  public String kickOffRollbackStackWorkflow(String effectiveUserId, String stackId, String stackName, String accountId, String accountAlias, String resourceDependencyManagerJson, int rolledBackStackVersion) {
    CommonDeleteRollbackKickoff.kickOffRollbackStackWorkflow(effectiveUserId, stackId, stackName, accountId, accountAlias, resourceDependencyManagerJson, rolledBackStackVersion);
    return "";
  }

  @Override
  public String logMessage(String level, String message) {
    LOG.log(Level.toLevel(level), message);
    return "";
  }

  @Override
  public String cancelOutstandingDeleteResources(String stackId, String accountId, String cancelMessage, int deletedResourceVersion) {
    List<StackResourceEntity> stackResourceEntityList = StackResourceEntityManager.getStackResources(stackId, accountId, deletedResourceVersion);
    for (StackResourceEntity stackResourceEntity : stackResourceEntityList) {
      if (stackResourceEntity.getResourceStatus() == Status.DELETE_IN_PROGRESS) {
        stackResourceEntity.setResourceStatus(Status.DELETE_FAILED);
        stackResourceEntity.setResourceStatusReason(cancelMessage);
        StackResourceEntityManager.updateStackResource(stackResourceEntity);
        StackEventEntityManager.addStackEvent(stackResourceEntity);
      }
    }
    return "";
  }

  @Override
  public String getWorkflowExecutionCloseStatus(String stackId, String workflowType) {
    LOG.info("Getting ${workflowType} execution close status for stack " + stackId);
    final AmazonSimpleWorkflow simpleWorkflowClient = WorkflowClientManager.simpleWorkflowClient
    final List<StackWorkflowEntity> stackWorkflowEntities =
      StackWorkflowEntityManager.getStackWorkflowEntities(stackId, StackWorkflowEntity.WorkflowType.valueOf(workflowType));
    if (stackWorkflowEntities == null || stackWorkflowEntities.empty) {
      throw new InternalFailureException( "There is no ${workflowType} for stack id ${stackId}" );
    }
    if (stackWorkflowEntities.size() > 1) {
      throw new InternalFailureException("More than one ${workflowType} was found for stack id ${stackId}");
    }
    String status = stackWorkflowEntities.get(0).with {
      simpleWorkflowClient.describeWorkflowExecution(
        new DescribeWorkflowExecutionRequest(
          domain: domain,
          execution: new WorkflowExecution(
            runId: runId,
            workflowId: workflowId
          )
        )
      ).with {
        executionInfo.closeStatus
      }
    }
    LOG.info("${workflowType} stack status = " + status);
    return status;
  }

  @Override
  public String getStackStatusIfLatest(String stackId, String accountId, int stackVersion) {
    LOG.info("Getting stack status for stack " + stackId + " if version " + stackVersion + " is the latest");
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId);
    if (stackEntity == null) {
      LOG.info("No current version found for " + stackId + ", returning null status");
      return null;
    } else if (stackEntity.getStackVersion() != null && stackEntity.getStackVersion() == stackVersion) {
      String status = stackEntity.getStackStatus().toString();
      LOG.info("status = " + status);
      return status;
    } else {
      LOG.info("Version " + stackVersion + " not current for stack " + stackId + " so returning null status");
      return null;
    }
  }

  @Override
  public String setStackStatusIfLatest(String stackId, String accountId, String status, String statusReason, int stackVersion) {
    LOG.info("Setting stack status for stack " + stackId + " if version " + stackVersion + " is the latest");
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId);
    if (stackEntity == null) {
     LOG.info(stackId + " does not exist, so can not set status.");
    } else if (stackEntity.getStackVersion() != null && stackEntity.getStackVersion() == stackVersion) {
      stackEntity.setStackStatus(Status.valueOf(status));
      stackEntity.setStackStatusReason(statusReason);
      StackEntityManager.updateStack(stackEntity);
    } else {
      LOG.info("Version " + stackVersion + " not current for stack " + stackId + " so not updating status");
    }
    return "";
  }
}
