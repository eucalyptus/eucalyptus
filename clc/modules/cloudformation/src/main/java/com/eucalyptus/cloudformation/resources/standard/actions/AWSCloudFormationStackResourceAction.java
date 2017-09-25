/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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


import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.eucalyptus.cloudformation.common.CloudFormation;
import com.eucalyptus.cloudformation.CloudFormationService;
import com.eucalyptus.cloudformation.common.msgs.CreateStackResponseType;
import com.eucalyptus.cloudformation.common.msgs.CreateStackType;
import com.eucalyptus.cloudformation.common.msgs.DeleteStackResponseType;
import com.eucalyptus.cloudformation.common.msgs.DeleteStackType;
import com.eucalyptus.cloudformation.common.msgs.DescribeStacksResponseType;
import com.eucalyptus.cloudformation.common.msgs.DescribeStacksType;
import com.eucalyptus.cloudformation.common.msgs.Output;
import com.eucalyptus.cloudformation.common.msgs.Outputs;
import com.eucalyptus.cloudformation.common.msgs.Parameter;
import com.eucalyptus.cloudformation.common.msgs.Parameters;
import com.eucalyptus.cloudformation.common.msgs.ResourceList;
import com.eucalyptus.cloudformation.common.msgs.Tag;
import com.eucalyptus.cloudformation.common.msgs.Tags;
import com.eucalyptus.cloudformation.common.msgs.UpdateStackResponseType;
import com.eucalyptus.cloudformation.common.msgs.UpdateStackType;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.entity.StackEntityHelper;
import com.eucalyptus.cloudformation.entity.StackUpdateInfoEntityManager;
import com.eucalyptus.cloudformation.entity.StacksWithNoUpdateToPerformEntityManager;
import com.eucalyptus.cloudformation.entity.Status;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSCloudFormationStackResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSCloudFormationStackProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.CloudFormationResourceTag;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.ResourceFailureException;
import com.eucalyptus.cloudformation.workflow.RetryAfterConditionCheckFailedException;
import com.eucalyptus.cloudformation.workflow.StackActivityClient;
import com.eucalyptus.cloudformation.workflow.UpdateStackPartsWorkflowKickOff;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateCleanupUpdateMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.UpdateRollbackCleanupUpdateMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateTypeAndDirection;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.glisten.WorkflowOperations;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSCloudFormationStackResourceAction extends StepBasedResourceAction {
  private static final Logger LOG = Logger.getLogger(AWSCloudFormationStackResourceAction.class);
  private AWSCloudFormationStackProperties properties = new AWSCloudFormationStackProperties();
  private AWSCloudFormationStackResourceInfo info = new AWSCloudFormationStackResourceInfo();

  public AWSCloudFormationStackResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
    setUpdateSteps(UpdateTypeAndDirection.UPDATE_ROLLBACK_NO_INTERRUPTION, fromUpdateEnum(UpdateRollbackNoInterruptionSteps.class));
    clearAndPutIfNotNull(updateCleanupUpdateSteps, fromEnum(UpdateCleanupUpdateSteps.class));
    clearAndPutIfNotNull(updateRollbackCleanupUpdateSteps, fromEnum(UpdateRollbackCleanupUpdateSteps.class));
  }

  protected Map<String, Step> updateCleanupUpdateSteps = Maps.newLinkedHashMap();
  public final Step getUpdateCleanupUpdateStep(String stepId) {
    return updateCleanupUpdateSteps.get(stepId);
  }

  protected Map<String, Step> updateRollbackCleanupUpdateSteps = Maps.newLinkedHashMap();
  public final Step getUpdateRollbackCleanupUpdateStep(String stepId) {
    return updateRollbackCleanupUpdateSteps.get(stepId);
  }

  @Override
  public boolean mustCheckUpdateTypeEvenIfNoPropertiesChanged() {
    return true;
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) throws Exception {
    AWSCloudFormationStackResourceAction otherAction = (AWSCloudFormationStackResourceAction) resourceAction;
    // always no interruption
    return UpdateType.NO_INTERRUPTION;
  }

  private enum CreateSteps implements Step {
    CREATE_STACK {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSCloudFormationStackResourceAction action = (AWSCloudFormationStackResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(CloudFormation.class);
        CreateStackType createStackType = MessageHelper.createMessage(CreateStackType.class, action.info.getEffectiveUserId());
        String stackName = action.getDefaultPhysicalResourceId();
        createStackType.setStackName(stackName);
        if (action.properties.getTimeoutInMinutes() != null) {
          createStackType.setTimeoutInMinutes(action.properties.getTimeoutInMinutes());
        }
        if (action.properties.getNotificationARNs() != null) {
          ResourceList notificationARNs = new ResourceList();
          notificationARNs.getMember().addAll(action.properties.getNotificationARNs());
          createStackType.setNotificationARNs(notificationARNs);
        }
        if (action.properties.getTags() != null) {
          Tags tags = new Tags();
          for (CloudFormationResourceTag cloudFormationResourceTag: action.properties.getTags()) {
            Tag tag = new Tag();
            tag.setKey(cloudFormationResourceTag.getKey());
            tag.setValue(cloudFormationResourceTag.getValue());
            tags.getMember().add(tag);
          }
          ResourceList notificationARNs = new ResourceList();
          notificationARNs.getMember().addAll(action.properties.getNotificationARNs());
          createStackType.setTags(tags);
        }
        createStackType.setDisableRollback(true); // Rollback will be handled by outer stack
        if (action.properties.getParameters() != null) {
          Parameters parameters = new Parameters();
          createStackType.setParameters(parameters);
          if (!action.properties.getParameters().isObject()) {
            throw new ValidationErrorException("Invalid Parameters value " + action.properties.getParameters());
          }
          for (String paramName : Lists.newArrayList(action.properties.getParameters().fieldNames())) {
            JsonNode paramValue = action.properties.getParameters().get(paramName);
            if (!paramValue.isValueNode()) {
              throw new ValidationErrorException("All Parameters must have String values for nested stacks");
            } else {
              Parameter parameter = new Parameter();
              parameter.setParameterKey(paramName);
              parameter.setParameterValue(paramValue.asText());
              parameters.getMember().add(parameter);
            }
          }
        }
        createStackType.setTemplateURL(action.properties.getTemplateURL());
        // inherit outer stack capabilities
        ResourceList capabilities = new ResourceList();
        List<String> stackCapabilities = StackEntityHelper.jsonToCapabilities(action.getStackEntity().getCapabilitiesJson());
        if (stackCapabilities != null) {
          capabilities.getMember().addAll(stackCapabilities);
        }
        createStackType.setCapabilities(capabilities);
        CreateStackResponseType createStackResponseType = AsyncRequests.<CreateStackType, CreateStackResponseType>sendSync(configuration, createStackType);
        action.info.setPhysicalResourceId(createStackResponseType.getCreateStackResult().getStackId());
        action.info.setCreatedEnoughToDelete(true);

        return action;
      }
    },
    WAIT_UNTIL_CREATE_COMPLETE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSCloudFormationStackResourceAction action = (AWSCloudFormationStackResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(CloudFormation.class);
        DescribeStacksType describeStacksType = MessageHelper.createMessage(DescribeStacksType.class, action.info.getEffectiveUserId());
        describeStacksType.setStackName(action.info.getPhysicalResourceId()); // actually the stack id...
        DescribeStacksResponseType describeStacksResponseType = AsyncRequests.<DescribeStacksType, DescribeStacksResponseType>sendSync(configuration, describeStacksType);
        if (describeStacksResponseType.getDescribeStacksResult() == null ||
          describeStacksResponseType.getDescribeStacksResult().getStacks() == null ||
          describeStacksResponseType.getDescribeStacksResult().getStacks().getMember() == null ||
          describeStacksResponseType.getDescribeStacksResult().getStacks().getMember().size() != 1) {
          throw new ResourceFailureException("Not exactly one stack returned for stack " + action.info.getPhysicalResourceId());
        }
        String status = describeStacksResponseType.getDescribeStacksResult().getStacks().getMember().get(0).getStackStatus();
        String statusReason = describeStacksResponseType.getDescribeStacksResult().getStacks().getMember().get(0).getStackStatusReason();
        if (status == null) {
          throw new ResourceFailureException("Null status for stack " + action.info.getPhysicalResourceId());
        }
        if (!status.startsWith("CREATE")) {
          throw new ResourceFailureException("Stack " + action.info.getPhysicalResourceId() + " is no longer being created.");
        }
        if (status.equals(Status.CREATE_FAILED.toString())) {
          throw new ResourceFailureException("Failed to create stack " + action.info.getPhysicalResourceId() + "."  + statusReason);
        }
        if (status.equals(Status.CREATE_IN_PROGRESS.toString())) {
          throw new RetryAfterConditionCheckFailedException("Stack " + action.info.getPhysicalResourceId() + " is still being created.");
        }
        return action;
      }

      @Override
      public Integer getTimeout( ) {
        // Wait as long as necessary for stacks
        return MAX_TIMEOUT;
      }
    },
    POPULATE_OUTPUTS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSCloudFormationStackResourceAction action = (AWSCloudFormationStackResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(CloudFormation.class);
        DescribeStacksType describeStacksType = MessageHelper.createMessage(DescribeStacksType.class, action.info.getEffectiveUserId());
        describeStacksType.setStackName(action.info.getPhysicalResourceId()); // actually the stack id...
        DescribeStacksResponseType describeStacksResponseType = AsyncRequests.<DescribeStacksType, DescribeStacksResponseType>sendSync(configuration, describeStacksType);
        if (describeStacksResponseType.getDescribeStacksResult() == null ||
          describeStacksResponseType.getDescribeStacksResult().getStacks() == null ||
          describeStacksResponseType.getDescribeStacksResult().getStacks().getMember() == null ||
          describeStacksResponseType.getDescribeStacksResult().getStacks().getMember().size() != 1) {
          throw new ResourceFailureException("Not exactly one stack returned for stack " + action.info.getPhysicalResourceId());
        }

        Outputs outputs = describeStacksResponseType.getDescribeStacksResult().getStacks().getMember().get(0).getOutputs();
        if (outputs != null && outputs.getMember() != null && !outputs.getMember().isEmpty()) {
          for (Output output: outputs.getMember()) {
            action.info.getOutputAttributes().put("Outputs." + output.getOutputKey(), JsonHelper.getStringFromJsonNode(new TextNode(output.getOutputValue())));
          }
        }
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

  private enum DeleteSteps implements Step {

    DEAL_WITH_UPDATE_COMPLETE_CLEANUP_IN_PROGRESS_ON_DELETE {
    @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
      AWSCloudFormationStackResourceAction action = (AWSCloudFormationStackResourceAction) resourceAction;
      ServiceConfiguration configuration = Topology.lookup(CloudFormation.class);
      if (alreadyDeletedOrNeverCreated(action, configuration)) return action;
        StatusAndReason statusAndReason = getStackStatusAndReason(action, configuration);
        String status = statusAndReason.getStatus();
        if (Status.UPDATE_COMPLETE_CLEANUP_IN_PROGRESS.toString().equals(status) || Status.UPDATE_ROLLBACK_IN_PROGRESS.toString().equals(status)) {
          action.info.getEucaAttributes().put(AWSCloudFormationStackResourceInfo.EUCA_DELETE_STATUS_UPDATE_COMPLETE_CLEANUP_IN_PROGRESS,
            JsonHelper.getStringFromJsonNode(new TextNode("true")));
          UpdateStackPartsWorkflowKickOff.kickOffUpdateRollbackStackWorkflow(action.info.getPhysicalResourceId(), action.info.getAccountId(),
            action.getStackEntity().getStackId(), action.info.getEffectiveUserId());
        } else {
          action.info.getEucaAttributes().remove(AWSCloudFormationStackResourceInfo.EUCA_DELETE_STATUS_UPDATE_COMPLETE_CLEANUP_IN_PROGRESS);
        }
        return action;
      }
    },
    WAIT_UNTIL_NOT_UPDATE_ROLLBACK_IN_PROGRESS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSCloudFormationStackResourceAction action = (AWSCloudFormationStackResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(CloudFormation.class);
        if (alreadyDeletedOrNeverCreated(action, configuration)) return action;
        if (action.info.getEucaAttributes().containsKey(AWSCloudFormationStackResourceInfo.EUCA_DELETE_STATUS_UPDATE_COMPLETE_CLEANUP_IN_PROGRESS)) {
          boolean deleteStatusUpdateCompleteCleanupInProgress = Boolean.valueOf(JsonHelper.getJsonNodeFromString(
            action.info.getEucaAttributes().get(AWSCloudFormationStackResourceInfo.EUCA_DELETE_STATUS_UPDATE_COMPLETE_CLEANUP_IN_PROGRESS)).asText());
          if (deleteStatusUpdateCompleteCleanupInProgress) {
            StatusAndReason statusAndReason = getStackStatusAndReason(action, configuration);
            String status = statusAndReason.getStatus();
            if (Status.UPDATE_COMPLETE_CLEANUP_IN_PROGRESS.toString().equals(status) || Status.UPDATE_ROLLBACK_IN_PROGRESS.toString().equals(status)) {
              throw new RetryAfterConditionCheckFailedException("Stack " + action.info.getPhysicalResourceId() + " is still being rolled back.");
            }
          }
          action.info.getEucaAttributes().remove(AWSCloudFormationStackResourceInfo.EUCA_DELETE_STATUS_UPDATE_COMPLETE_CLEANUP_IN_PROGRESS);
        }
        return action;
      }

      @Override
      public Integer getTimeout() {
        // Wait as long as necessary for stacks
        return MAX_TIMEOUT;
      }
    },
    DEAL_WITH_UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS_ON_DELETE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSCloudFormationStackResourceAction action = (AWSCloudFormationStackResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(CloudFormation.class);
        if (alreadyDeletedOrNeverCreated(action, configuration)) return action;
        StatusAndReason statusAndReason = getStackStatusAndReason(action, configuration);
        String status = statusAndReason.getStatus();
        if (Status.UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS.toString().equals(status)) {
          action.info.getEucaAttributes().put(AWSCloudFormationStackResourceInfo.EUCA_DELETE_STATUS_UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS,
            JsonHelper.getStringFromJsonNode(new TextNode("true")));
          UpdateStackPartsWorkflowKickOff.kickOffUpdateRollbackCleanupStackWorkflow(action.info.getPhysicalResourceId(),
            action.info.getAccountId(), action.info.getEffectiveUserId());
        } else {
          action.info.getEucaAttributes().remove(AWSCloudFormationStackResourceInfo.EUCA_DELETE_STATUS_UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS);
        }
        return action;
      }
    },
    WAIT_UNTIL_NOT_UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSCloudFormationStackResourceAction action = (AWSCloudFormationStackResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(CloudFormation.class);
        if (alreadyDeletedOrNeverCreated(action, configuration)) return action;
        if (action.info.getEucaAttributes().containsKey(AWSCloudFormationStackResourceInfo.EUCA_DELETE_STATUS_UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS)) {
          boolean deleteStatusUpdateCompleteCleanupInProgress = Boolean.valueOf(JsonHelper.getJsonNodeFromString(
            action.info.getEucaAttributes().get(AWSCloudFormationStackResourceInfo.EUCA_DELETE_STATUS_UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS)).asText());
          if (deleteStatusUpdateCompleteCleanupInProgress) {
            StatusAndReason statusAndReason = getStackStatusAndReason(action, configuration);
            String status = statusAndReason.getStatus();
            if (Status.UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS.toString().equals(status)) {
              throw new RetryAfterConditionCheckFailedException("Stack " + action.info.getPhysicalResourceId() + " is still being rolled back clean up.");
            }
          }
          action.info.getEucaAttributes().remove(AWSCloudFormationStackResourceInfo.EUCA_DELETE_STATUS_UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS);
        }
        return action;
      }
      @Override
      public Integer getTimeout() {
        // Wait as long as necessary for stacks
        return MAX_TIMEOUT;
      }
    },
    DELETE_STACK {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSCloudFormationStackResourceAction action = (AWSCloudFormationStackResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(CloudFormation.class);
        if (alreadyDeletedOrNeverCreated(action, configuration)) return action;
        DeleteStackType deleteStackType = MessageHelper.createMessage(DeleteStackType.class, action.info.getEffectiveUserId());
        deleteStackType.setStackName(action.info.getPhysicalResourceId()); // actually stack id
        AsyncRequests.<DeleteStackType, DeleteStackResponseType>sendSync(configuration, deleteStackType);
        return action;
      }

    },
    WAIT_UNTIL_DELETE_COMPLETE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSCloudFormationStackResourceAction action = (AWSCloudFormationStackResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(CloudFormation.class);
        if (alreadyDeletedOrNeverCreated(action, configuration)) return action;
        // First see if stack exists or has been deleted
        DescribeStacksType describeStacksType = MessageHelper.createMessage(DescribeStacksType.class, action.info.getEffectiveUserId());
        describeStacksType.setStackName(action.info.getPhysicalResourceId()); // actually the stack id...
        DescribeStacksResponseType describeStacksResponseType = AsyncRequests.<DescribeStacksType, DescribeStacksResponseType>sendSync(configuration, describeStacksType);
        if (describeStacksResponseType.getDescribeStacksResult() == null ||
          describeStacksResponseType.getDescribeStacksResult().getStacks() == null ||
          describeStacksResponseType.getDescribeStacksResult().getStacks().getMember() == null ||
          describeStacksResponseType.getDescribeStacksResult().getStacks().getMember().isEmpty()) {
          return action;
        }
        if (describeStacksResponseType.getDescribeStacksResult().getStacks().getMember().size() > 1) {
          throw new ResourceFailureException("More than one stack returned for stack " + action.info.getPhysicalResourceId());
        }
        String status = describeStacksResponseType.getDescribeStacksResult().getStacks().getMember().get(0).getStackStatus();
        if (status == null) {
          throw new ResourceFailureException("Null status for stack " + action.info.getPhysicalResourceId());
        }
        if (status.equals(Status.DELETE_IN_PROGRESS.toString())) {
          throw new RetryAfterConditionCheckFailedException("Stack " + action.info.getPhysicalResourceId() + " is still being deleted.");
        }
        // TODO: consider this logic
        if (status.endsWith("IN_PROGRESS")) {
          throw new ResourceFailureException("Stack " + action.info.getPhysicalResourceId() + " is in the middle of " + status + ", not deleting");
        }
        if (status.equals(Status.DELETE_COMPLETE.toString())) {
          return action;
        }
        if (status.equals(Status.DELETE_FAILED.toString())) {
          throw new ResourceFailureException("Deleting stack " + action.info.getPhysicalResourceId() + " failed");
        }
        throw new RetryAfterConditionCheckFailedException("Stack " + action.info.getPhysicalResourceId() + " current status is " + status + ", maybe not yet started deleting?");
      }

      @Override
      public Integer getTimeout( ) {
        // Wait as long as necessary for stacks
        return MAX_TIMEOUT;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }

    private static boolean alreadyDeletedOrNeverCreated(AWSCloudFormationStackResourceAction action, ServiceConfiguration configuration) throws Exception {
      if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return true;
      // First see if stack exists or has been deleted
      DescribeStacksType describeStacksType = MessageHelper.createMessage(DescribeStacksType.class, action.info.getEffectiveUserId());
      describeStacksType.setStackName(action.info.getPhysicalResourceId()); // actually the stack id...
      DescribeStacksResponseType describeStacksResponseType = AsyncRequests.<DescribeStacksType, DescribeStacksResponseType>sendSync(configuration, describeStacksType);
      if (describeStacksResponseType.getDescribeStacksResult() == null ||
        describeStacksResponseType.getDescribeStacksResult().getStacks() == null ||
        describeStacksResponseType.getDescribeStacksResult().getStacks().getMember() == null ||
        describeStacksResponseType.getDescribeStacksResult().getStacks().getMember().isEmpty()) {
        return true;
      }
      if (describeStacksResponseType.getDescribeStacksResult().getStacks().getMember().size() > 1) {
        throw new ResourceFailureException("More than one stack returned for stack " + action.info.getPhysicalResourceId());
      }
      String status = describeStacksResponseType.getDescribeStacksResult().getStacks().getMember().get(0).getStackStatus();
      if (status == null) {
        throw new ResourceFailureException("Null status for stack " + action.info.getPhysicalResourceId());
      }
      if (status.equals(Status.DELETE_COMPLETE.toString())) {
        return true;
      }
      return false;
    }
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_STACK {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSCloudFormationStackResourceAction newAction = (AWSCloudFormationStackResourceAction) newResourceAction;
        AWSCloudFormationStackResourceAction oldAction = (AWSCloudFormationStackResourceAction) oldResourceAction;

        StacksWithNoUpdateToPerformEntityManager.deleteStackWithNoUpdateToPerform(newAction.info.getPhysicalResourceId(), newAction.info.getAccountId());


        ServiceConfiguration configuration = Topology.lookup(CloudFormation.class);
        UpdateStackType updateStackType = MessageHelper.createMessage(UpdateStackType.class, newAction.info.getEffectiveUserId());
        String stackName = newAction.info.getPhysicalResourceId();
        updateStackType.setStackName(stackName);
        if (newAction.properties.getNotificationARNs() != null) {
          ResourceList notificationARNs = new ResourceList();
          notificationARNs.getMember().addAll(newAction.properties.getNotificationARNs());
          updateStackType.setNotificationARNs(notificationARNs);
        }
        if (newAction.properties.getTags() != null) {
          Tags tags = new Tags();
          for (CloudFormationResourceTag cloudFormationResourceTag: newAction.properties.getTags()) {
            Tag tag = new Tag();
            tag.setKey(cloudFormationResourceTag.getKey());
            tag.setValue(cloudFormationResourceTag.getValue());
            tags.getMember().add(tag);
          }
          ResourceList notificationARNs = new ResourceList();
          notificationARNs.getMember().addAll(newAction.properties.getNotificationARNs());
          updateStackType.setTags(tags);
        }
        if (newAction.properties.getParameters() != null) {
          Parameters parameters = new Parameters();
          updateStackType.setParameters(parameters);
          if (!newAction.properties.getParameters().isObject()) {
            throw new ValidationErrorException("Invalid Parameters value " + newAction.properties.getParameters());
          }
          for (String paramName : Lists.newArrayList(newAction.properties.getParameters().fieldNames())) {
            JsonNode paramValue = newAction.properties.getParameters().get(paramName);
            if (!paramValue.isValueNode()) {
              throw new ValidationErrorException("All Parameters must have String values for nested stacks");
            } else {
              Parameter parameter = new Parameter();
              parameter.setParameterKey(paramName);
              parameter.setParameterValue(paramValue.asText());
              parameters.getMember().add(parameter);
            }
          }
        }
        updateStackType.setTemplateURL(newAction.properties.getTemplateURL());
        // inherit outer stack capabilities
        ResourceList capabilities = new ResourceList();
        List<String> stackCapabilities = StackEntityHelper.jsonToCapabilities(newAction.getStackEntity().getCapabilitiesJson());
        if (stackCapabilities != null) {
          capabilities.getMember().addAll(stackCapabilities);
        }
        updateStackType.setCapabilities(capabilities);
        try {
          final UpdateStackResponseType updateStackResponseType = AsyncRequests.<UpdateStackType, UpdateStackResponseType>sendSync(configuration, updateStackType);
          newAction.info.setPhysicalResourceId(updateStackResponseType.getUpdateStackResult().getStackId());
          newAction.info.setCreatedEnoughToDelete(true);
          newAction.info.setReferenceValueJson( JsonHelper.getStringFromJsonNode(new TextNode(newAction.info.getPhysicalResourceId())) );
        } catch (final Exception e) {
          final Optional<AsyncExceptions.AsyncWebServiceError> error = AsyncExceptions.asWebServiceError(e);
          if (error.isPresent() && Strings.nullToEmpty(error.get().getMessage()).equals(CloudFormationService.NO_UPDATES_ARE_TO_BE_PERFORMED)) {
            StacksWithNoUpdateToPerformEntityManager.addStackWithNoUpdateToPerform(newAction.info.getPhysicalResourceId(), newAction.info.getAccountId());
          } else {
            throw e;
          }
        }
        return newAction;
      }
    },
    WAIT_UNTIL_UPDATE_COMPLETE {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSCloudFormationStackResourceAction newAction = (AWSCloudFormationStackResourceAction) newResourceAction;
        AWSCloudFormationStackResourceAction oldAction = (AWSCloudFormationStackResourceAction) oldResourceAction;
        ServiceConfiguration configuration = Topology.lookup(CloudFormation.class);
        if (StacksWithNoUpdateToPerformEntityManager.isStackWithNoUpdateToPerform(newAction.info.getPhysicalResourceId(), newAction.info.getAccountId())) {
          return newAction;
        }
        if (StackUpdateInfoEntityManager.hasNoUpdateInfoRecord(newAction.info.getPhysicalResourceId(), newAction.info.getAccountId())) {
          return newAction;
        }
        DescribeStacksType describeStacksType = MessageHelper.createMessage(DescribeStacksType.class, newAction.info.getEffectiveUserId());
        describeStacksType.setStackName(newAction.info.getPhysicalResourceId()); // actually the stack id...
        DescribeStacksResponseType describeStacksResponseType = AsyncRequests.<DescribeStacksType, DescribeStacksResponseType>sendSync(configuration, describeStacksType);
        if (describeStacksResponseType.getDescribeStacksResult() == null ||
          describeStacksResponseType.getDescribeStacksResult().getStacks() == null ||
          describeStacksResponseType.getDescribeStacksResult().getStacks().getMember() == null ||
          describeStacksResponseType.getDescribeStacksResult().getStacks().getMember().size() != 1) {
          throw new ResourceFailureException("Not exactly one stack returned for stack " + newAction.info.getPhysicalResourceId());
        }
        String status = describeStacksResponseType.getDescribeStacksResult().getStacks().getMember().get(0).getStackStatus();
        String statusReason = describeStacksResponseType.getDescribeStacksResult().getStacks().getMember().get(0).getStackStatusReason();
        if (status == null) {
          throw new ResourceFailureException("Null status for stack " + newAction.info.getPhysicalResourceId());
        }
        if (!status.startsWith("UPDATE")) {
          throw new ResourceFailureException("Stack " + newAction.info.getPhysicalResourceId() + " is no longer being updated.");
        }
        if (status.startsWith("UPDATE_ROLLBACK") || status.startsWith("UPDATE_FAILED")) {
          throw new ResourceFailureException("Failed to update stack " + newAction.info.getPhysicalResourceId() + "."  + statusReason);
        }
        if (status.equals(Status.UPDATE_IN_PROGRESS.toString())) {
          throw new RetryAfterConditionCheckFailedException("Stack " + newAction.info.getPhysicalResourceId() + " is still being updated.");
        }
        return newAction;
      }

      @Override
      public Integer getTimeout( ) {
        // Wait as long as necessary for stacks
        return MAX_TIMEOUT;
      }
    },
    POPULATE_OUTPUTS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSCloudFormationStackResourceAction newAction = (AWSCloudFormationStackResourceAction) newResourceAction;
        AWSCloudFormationStackResourceAction oldAction = (AWSCloudFormationStackResourceAction) oldResourceAction;
        if (StacksWithNoUpdateToPerformEntityManager.isStackWithNoUpdateToPerform(newAction.info.getPhysicalResourceId(), newAction.info.getAccountId())) {
          return newAction;
        }
        if (StackUpdateInfoEntityManager.hasNoUpdateInfoRecord(newAction.info.getPhysicalResourceId(), newAction.info.getAccountId())) {
          return newAction;
        }
        ServiceConfiguration configuration = Topology.lookup(CloudFormation.class);
        DescribeStacksType describeStacksType = MessageHelper.createMessage(DescribeStacksType.class, newAction.info.getEffectiveUserId());
        describeStacksType.setStackName(newAction.info.getPhysicalResourceId()); // actually the stack id...
        DescribeStacksResponseType describeStacksResponseType = AsyncRequests.<DescribeStacksType, DescribeStacksResponseType>sendSync(configuration, describeStacksType);
        if (describeStacksResponseType.getDescribeStacksResult() == null ||
          describeStacksResponseType.getDescribeStacksResult().getStacks() == null ||
          describeStacksResponseType.getDescribeStacksResult().getStacks().getMember() == null ||
          describeStacksResponseType.getDescribeStacksResult().getStacks().getMember().size() != 1) {
          throw new ResourceFailureException("Not exactly one stack returned for stack " + newAction.info.getPhysicalResourceId());
        }

        Outputs outputs = describeStacksResponseType.getDescribeStacksResult().getStacks().getMember().get(0).getOutputs();
        if (outputs != null && outputs.getMember() != null && !outputs.getMember().isEmpty()) {
          for (Output output: outputs.getMember()) {
            newAction.info.getOutputAttributes().put("Outputs." + output.getOutputKey(), JsonHelper.getStringFromJsonNode(new TextNode(output.getOutputValue())));
          }
        }
        newAction.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(newAction.info.getPhysicalResourceId())));
        return newAction;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

  private enum UpdateRollbackNoInterruptionSteps implements UpdateStep {
    UPDATE_ROLLBACK_STACK {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSCloudFormationStackResourceAction newAction = (AWSCloudFormationStackResourceAction) newResourceAction;
        AWSCloudFormationStackResourceAction oldAction = (AWSCloudFormationStackResourceAction) oldResourceAction;
        if (StacksWithNoUpdateToPerformEntityManager.isStackWithNoUpdateToPerform(newAction.info.getPhysicalResourceId(), newAction.info.getAccountId())) {
          return newAction;
        }
        if (StackUpdateInfoEntityManager.hasNoUpdateInfoRecord(newAction.info.getPhysicalResourceId(), newAction.info.getAccountId())) {
          return newAction;
        }
        UpdateStackPartsWorkflowKickOff.kickOffUpdateRollbackStackWorkflow(newAction.info.getPhysicalResourceId(), newAction.info.getAccountId(),
          newAction.getStackEntity().getStackId(), newAction.info.getEffectiveUserId());
        return newAction;
      }
    },
    WAIT_UNTIL_UPDATE_ROLLBACK_COMPLETE {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSCloudFormationStackResourceAction newAction = (AWSCloudFormationStackResourceAction) newResourceAction;
        AWSCloudFormationStackResourceAction oldAction = (AWSCloudFormationStackResourceAction) oldResourceAction;
        if (StacksWithNoUpdateToPerformEntityManager.isStackWithNoUpdateToPerform(newAction.info.getPhysicalResourceId(), newAction.info.getAccountId())) {
          return newAction;
        }
        if (StackUpdateInfoEntityManager.hasNoUpdateInfoRecord(newAction.info.getPhysicalResourceId(), newAction.info.getAccountId())) {
          return newAction;
        }
        ServiceConfiguration configuration = Topology.lookup(CloudFormation.class);
        StatusAndReason statusAndReason = getStackStatusAndReason(newAction, configuration);
        String status = statusAndReason.getStatus();
        String statusReason = statusAndReason.getReason();
        if (status == null) {
          throw new ResourceFailureException("Null status for stack " + newAction.info.getPhysicalResourceId());
        }
        if (!status.startsWith("UPDATE")) {
          throw new ResourceFailureException("Stack " + newAction.info.getPhysicalResourceId() + " is no longer being updated.");
        }
        if (status.equals(Status.UPDATE_ROLLBACK_FAILED.toString())) {
          throw new ResourceFailureException("Failed to update rollback " + newAction.info.getPhysicalResourceId() + "."  + statusReason);
        }
        if (status.equals(Status.UPDATE_ROLLBACK_IN_PROGRESS.toString())) {
          throw new RetryAfterConditionCheckFailedException("Stack " + newAction.info.getPhysicalResourceId() + " is still being rolled back.");
        }
        return newAction;
      }

      @Override
      public Integer getTimeout( ) {
        // Wait as long as necessary for stacks
        return MAX_TIMEOUT;
      }
    };
    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }
  private static StatusAndReason getStackStatusAndReason(AWSCloudFormationStackResourceAction action, ServiceConfiguration configuration) throws Exception {
    DescribeStacksType describeStacksType = MessageHelper.createMessage(DescribeStacksType.class, action.info.getEffectiveUserId());
    describeStacksType.setStackName(action.info.getPhysicalResourceId()); // actually the stack id...
    DescribeStacksResponseType describeStacksResponseType = AsyncRequests.<DescribeStacksType, DescribeStacksResponseType>sendSync(configuration, describeStacksType);
    if (describeStacksResponseType.getDescribeStacksResult() == null ||
      describeStacksResponseType.getDescribeStacksResult().getStacks() == null ||
      describeStacksResponseType.getDescribeStacksResult().getStacks().getMember() == null ||
      describeStacksResponseType.getDescribeStacksResult().getStacks().getMember().size() != 1) {
      throw new ResourceFailureException("Not exactly one stack returned for stack " + action.info.getPhysicalResourceId());
    }
    return new StatusAndReason(describeStacksResponseType.getDescribeStacksResult().getStacks().getMember().get(0).getStackStatus(),
      describeStacksResponseType.getDescribeStacksResult().getStacks().getMember().get(0).getStackStatusReason());
  }

  private static class StatusAndReason {
    private String status;
    private String reason;

    private StatusAndReason(String status, String reason) {
      this.status = status;
      this.reason = reason;
    }

    public String getStatus() {
      return status;
    }

    public String getReason() {
      return reason;
    }
  }


  private enum UpdateCleanupUpdateSteps implements Step {
    UPDATE_CLEANUP_STACK {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSCloudFormationStackResourceAction action = (AWSCloudFormationStackResourceAction) resourceAction;
        if (StacksWithNoUpdateToPerformEntityManager.isStackWithNoUpdateToPerform(action.info.getPhysicalResourceId(), action.info.getAccountId())) {
          return action;
        }
        if (StackUpdateInfoEntityManager.hasNoUpdateInfoRecord(action.info.getPhysicalResourceId(), action.info.getAccountId())) {
          return action;
        }
        ServiceConfiguration configuration = Topology.lookup(CloudFormation.class);
        String status = getStackStatusAndReason(action, configuration).getStatus();
        if (!Status.UPDATE_COMPLETE_CLEANUP_IN_PROGRESS.toString().equals(status)) {
          throw new ResourceFailureException("Update cleanup stack called when status is " + status + " for stack " + action.info.getPhysicalResourceId());
        }
        UpdateStackPartsWorkflowKickOff.kickOffUpdateCleanupStackWorkflow(action.info.getPhysicalResourceId(), action.info.getAccountId(), action.info.getEffectiveUserId());
        return action;
      }
    },
    WAIT_UNTIL_UPDATE_CLEANUP_COMPLETE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSCloudFormationStackResourceAction action = (AWSCloudFormationStackResourceAction) resourceAction;
        if (StacksWithNoUpdateToPerformEntityManager.isStackWithNoUpdateToPerform(action.info.getPhysicalResourceId(), action.info.getAccountId())) {
          return action;
        }
        if (StackUpdateInfoEntityManager.hasNoUpdateInfoRecord(action.info.getPhysicalResourceId(), action.info.getAccountId())) {
          return action;
        }
        ServiceConfiguration configuration = Topology.lookup(CloudFormation.class);
        String status = getStackStatusAndReason(action, configuration).getStatus();
        if (status == null) {
          throw new ResourceFailureException("Null status for stack " + action.info.getPhysicalResourceId());
        }
        if (!status.startsWith("UPDATE")) {
          throw new ResourceFailureException("Stack " + action.info.getPhysicalResourceId() + " is no longer being updated.");
        }
        if (status.equals(Status.UPDATE_COMPLETE_CLEANUP_IN_PROGRESS.toString())) {
          throw new RetryAfterConditionCheckFailedException("Stack " + action.info.getPhysicalResourceId() + " is still being cleaned up.");
        }
        return action;
      }

      @Override
      public Integer getTimeout( ) {
        // Wait as long as necessary for stacks
        return MAX_TIMEOUT;
      }
    };
    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

  private enum UpdateRollbackCleanupUpdateSteps implements Step {
    UPDATE_ROLLBACK_CLEANUP_STACK {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSCloudFormationStackResourceAction action = (AWSCloudFormationStackResourceAction) resourceAction;
        if (StacksWithNoUpdateToPerformEntityManager.isStackWithNoUpdateToPerform(action.info.getPhysicalResourceId(), action.info.getAccountId())) {
          return action;
        }
        if (StackUpdateInfoEntityManager.hasNoUpdateInfoRecord(action.info.getPhysicalResourceId(), action.info.getAccountId())) {
          return action;
        }
        ServiceConfiguration configuration = Topology.lookup(CloudFormation.class);
        String status = getStackStatusAndReason(action, configuration).getStatus();
        if (!Status.UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS.toString().equals(status)) {
          throw new ResourceFailureException("Update rollback cleanup stack called when status is " + status + " for stack " + action.info.getPhysicalResourceId());
        }
        UpdateStackPartsWorkflowKickOff.kickOffUpdateRollbackCleanupStackWorkflow(action.info.getPhysicalResourceId(), action.info.getAccountId(), action.info.getEffectiveUserId());
        return action;
      }
    },
    WAIT_UNTIL_UPDATE_ROLLBACK_CLEANUP_COMPLETE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSCloudFormationStackResourceAction action = (AWSCloudFormationStackResourceAction) resourceAction;
        if (StacksWithNoUpdateToPerformEntityManager.isStackWithNoUpdateToPerform(action.info.getPhysicalResourceId(), action.info.getAccountId())) {
          return action;
        }
        if (StackUpdateInfoEntityManager.hasNoUpdateInfoRecord(action.info.getPhysicalResourceId(), action.info.getAccountId())) {
          return action;
        }
        ServiceConfiguration configuration = Topology.lookup(CloudFormation.class);
        String status = getStackStatusAndReason(action, configuration).getStatus();
        if (status == null) {
          throw new ResourceFailureException("Null status for stack " + action.info.getPhysicalResourceId());
        }
        if (!status.startsWith("UPDATE")) {
          throw new ResourceFailureException("Stack " + action.info.getPhysicalResourceId() + " is no longer being updated.");
        }
        if (status.equals(Status.UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS.toString())) {
          throw new RetryAfterConditionCheckFailedException("Stack " + action.info.getPhysicalResourceId() + " is still being rolled back clean up.");
        }
        return action;
       }

      @Override
      public Integer getTimeout( ) {
        // Wait as long as necessary for stacks
        return MAX_TIMEOUT;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }


  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSCloudFormationStackProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSCloudFormationStackResourceInfo) resourceInfo;
  }

  public Promise<String> getUpdateCleanupUpdatePromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion) {
    List<String> stepIds = Lists.newArrayList(updateCleanupUpdateSteps.keySet());
    return new UpdateCleanupUpdateMultiStepPromise(workflowOperations, stepIds, this).getUpdateCleanupUpdatePromise(resourceId, stackId, accountId, effectiveUserId, updatedResourceVersion);
  }

  public Promise<String> getUpdateRollbackCleanupUpdatePromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion) {
    List<String> stepIds = Lists.newArrayList(updateRollbackCleanupUpdateSteps.keySet());
    return new UpdateRollbackCleanupUpdateMultiStepPromise(workflowOperations, stepIds, this).getUpdateRollbackCleanupUpdatePromise(resourceId, stackId, accountId, effectiveUserId, updatedResourceVersion);
  }

}


