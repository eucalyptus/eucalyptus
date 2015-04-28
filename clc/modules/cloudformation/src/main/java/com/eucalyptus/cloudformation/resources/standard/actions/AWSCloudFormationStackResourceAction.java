/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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


import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.eucalyptus.cloudformation.CloudFormation;
import com.eucalyptus.cloudformation.CreateStackResponseType;
import com.eucalyptus.cloudformation.CreateStackType;
import com.eucalyptus.cloudformation.DeleteStackResponseType;
import com.eucalyptus.cloudformation.DeleteStackType;
import com.eucalyptus.cloudformation.DescribeStacksResponseType;
import com.eucalyptus.cloudformation.DescribeStacksType;
import com.eucalyptus.cloudformation.Output;
import com.eucalyptus.cloudformation.Outputs;
import com.eucalyptus.cloudformation.Parameter;
import com.eucalyptus.cloudformation.Parameters;
import com.eucalyptus.cloudformation.ResourceList;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.entity.StackEntity;
import com.eucalyptus.cloudformation.entity.StackEntityHelper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSCloudFormationStackResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSCloudFormationStackProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.ResourceFailureException;
import com.eucalyptus.cloudformation.workflow.StackActivity;
import com.eucalyptus.cloudformation.workflow.ValidationFailedException;
import com.eucalyptus.cloudformation.workflow.steps.CreateMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.DeleteMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.netflix.glisten.WorkflowOperations;
import org.apache.log4j.Logger;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSCloudFormationStackResourceAction extends ResourceAction {
  private static final Logger LOG = Logger.getLogger(AWSCloudFormationStackResourceAction.class);
  private AWSCloudFormationStackProperties properties = new AWSCloudFormationStackProperties();
  private AWSCloudFormationStackResourceInfo info = new AWSCloudFormationStackResourceInfo();

  public AWSCloudFormationStackResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

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
        if (status.equals(StackEntity.Status.CREATE_FAILED.toString())) {
          throw new ResourceFailureException("Failed to create stack " + action.info.getPhysicalResourceId() + "."  + statusReason);
        }
        if (status.equals(StackEntity.Status.CREATE_IN_PROGRESS.toString())) {
          throw new ValidationFailedException("Stack " + action.info.getPhysicalResourceId() + " is still being created.");
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
    DELETE_STACK {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSCloudFormationStackResourceAction action = (AWSCloudFormationStackResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(CloudFormation.class);
        if (action.info.getPhysicalResourceId() == null) return action;
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
        if (status.equals(StackEntity.Status.DELETE_COMPLETE.toString())) {
          return action;
        }
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
        if (action.info.getPhysicalResourceId() == null) return action;
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
        if (status.equals(StackEntity.Status.DELETE_IN_PROGRESS.toString())) {
          throw new ValidationFailedException("Stack " + action.info.getPhysicalResourceId() + " is still being deleted.");
        }
        // TODO: consider this logic
        if (status.endsWith("IN_PROGRESS")) {
          throw new ResourceFailureException("Stack " + action.info.getPhysicalResourceId() + " is in the middle of " + status + ", not deleting");
        }
        if (status.equals(StackEntity.Status.DELETE_COMPLETE.toString())) {
          return action;
        }
        if (status.equals(StackEntity.Status.DELETE_FAILED.toString())) {
          throw new ResourceFailureException("Deleting stack " + action.info.getPhysicalResourceId() + " failed");
        }
        throw new ValidationFailedException("Stack " + action.info.getPhysicalResourceId() + " current status is " + status + ", maybe not yet started deleting?");
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

  @Override
  public Promise<String> getCreatePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(CreateSteps.values()), StepTransform.INSTANCE);
    return new CreateMultiStepPromise(workflowOperations, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId);
  }

  @Override
  public Promise<String> getDeletePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(DeleteSteps.values()), StepTransform.INSTANCE);
    return new DeleteMultiStepPromise(workflowOperations, stepIds, this).getDeletePromise(resourceId, stackId, accountId, effectiveUserId);
  }

}


