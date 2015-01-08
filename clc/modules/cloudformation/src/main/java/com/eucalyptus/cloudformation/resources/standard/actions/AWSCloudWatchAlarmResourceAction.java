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


import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSCloudWatchAlarmResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSCloudWatchAlarmProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.CloudWatchMetricDimension;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.StackActivity;
import com.eucalyptus.cloudformation.workflow.steps.CreateMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.DeleteMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.eucalyptus.cloudwatch.common.CloudWatch;
import com.eucalyptus.cloudwatch.common.msgs.AlarmNames;
import com.eucalyptus.cloudwatch.common.msgs.DeleteAlarmsResponseType;
import com.eucalyptus.cloudwatch.common.msgs.DeleteAlarmsType;
import com.eucalyptus.cloudwatch.common.msgs.DescribeAlarmsResponseType;
import com.eucalyptus.cloudwatch.common.msgs.DescribeAlarmsType;
import com.eucalyptus.cloudwatch.common.msgs.Dimension;
import com.eucalyptus.cloudwatch.common.msgs.Dimensions;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricAlarmResponseType;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricAlarmType;
import com.eucalyptus.cloudwatch.common.msgs.ResourceList;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.netflix.glisten.WorkflowOperations;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSCloudWatchAlarmResourceAction extends ResourceAction {

  private AWSCloudWatchAlarmProperties properties = new AWSCloudWatchAlarmProperties();
  private AWSCloudWatchAlarmResourceInfo info = new AWSCloudWatchAlarmResourceInfo();

  public AWSCloudWatchAlarmResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

  }
  private enum CreateSteps implements Step {
    CREATE_ALARM {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSCloudWatchAlarmResourceAction action = (AWSCloudWatchAlarmResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(CloudWatch.class);
        if (action.properties.getAlarmName() == null) {
          action.properties.setAlarmName(action.getDefaultPhysicalResourceId());
        }
        // check alarm exists (TODO: check aws does this...)
        DescribeAlarmsType describeAlarmsType = MessageHelper.createMessage(DescribeAlarmsType.class, action.info.getEffectiveUserId());
        AlarmNames alarmNames = new AlarmNames();
        alarmNames.setMember(Lists.newArrayList(action.properties.getAlarmName()));
        describeAlarmsType.setAlarmNames(alarmNames);
        DescribeAlarmsResponseType describeAlarmsResponseType = AsyncRequests.<DescribeAlarmsType,DescribeAlarmsResponseType> sendSync(configuration, describeAlarmsType);
        if (describeAlarmsResponseType.getDescribeAlarmsResult() != null && describeAlarmsResponseType.getDescribeAlarmsResult().getMetricAlarms() != null &&
          describeAlarmsResponseType.getDescribeAlarmsResult().getMetricAlarms().getMember() != null &&
          describeAlarmsResponseType.getDescribeAlarmsResult().getMetricAlarms().getMember().size() > 0) {
          throw new ValidationErrorException("Alarm " + action.properties.getAlarmName() + " already exists");
        }
        PutMetricAlarmType putMetricAlarmType = MessageHelper.createMessage(PutMetricAlarmType.class, action.info.getEffectiveUserId());
        putMetricAlarmType.setActionsEnabled(action.properties.getActionsEnabled() == null ? Boolean.TRUE : action.properties.getActionsEnabled());
        if (action.properties.getAlarmActions() != null) {
          ResourceList alarmActions = new ResourceList();
          ArrayList<String> alarmActionsMember = Lists.newArrayList(action.properties.getAlarmActions());
          alarmActions.setMember(alarmActionsMember);
          putMetricAlarmType.setAlarmActions(alarmActions);
        }
        putMetricAlarmType.setAlarmDescription(action.properties.getAlarmDescription());
        putMetricAlarmType.setAlarmName(action.properties.getAlarmName());
        putMetricAlarmType.setComparisonOperator(action.properties.getComparisonOperator());
        if (action.properties.getDimensions() != null) {
          Dimensions dimensions = new Dimensions();
          ArrayList<Dimension> dimensionsMember = Lists.newArrayList();
          for (CloudWatchMetricDimension cloudWatchMetricDimension: action.properties.getDimensions()) {
            Dimension dimension = new Dimension();
            dimension.setName(cloudWatchMetricDimension.getName());
            dimension.setValue(cloudWatchMetricDimension.getValue());
            dimensionsMember.add(dimension);
          }
          dimensions.setMember(dimensionsMember);
          putMetricAlarmType.setDimensions(dimensions);
        }
        putMetricAlarmType.setEvaluationPeriods(action.properties.getEvaluationPeriods());
        if (action.properties.getInsufficientDataActions() != null) {
          ResourceList insufficientDataActions = new ResourceList();
          ArrayList<String> insufficientDataActionsMember = Lists.newArrayList(action.properties.getInsufficientDataActions());
          insufficientDataActions.setMember(insufficientDataActionsMember);
          putMetricAlarmType.setInsufficientDataActions(insufficientDataActions);
        }
        putMetricAlarmType.setMetricName(action.properties.getMetricName());
        putMetricAlarmType.setNamespace(action.properties.getNamespace());
        if (action.properties.getOkActions() != null) {
          ResourceList okActions = new ResourceList();
          ArrayList<String> okActionsMember = Lists.newArrayList(action.properties.getOkActions());
          okActions.setMember(okActionsMember);
          putMetricAlarmType.setOkActions(okActions);
        }
        putMetricAlarmType.setPeriod(action.properties.getPeriod());
        putMetricAlarmType.setStatistic(action.properties.getStatistic());
        putMetricAlarmType.setThreshold(action.properties.getThreshold());
        putMetricAlarmType.setUnit(action.properties.getUnit());
        AsyncRequests.<PutMetricAlarmType, PutMetricAlarmResponseType> sendSync(configuration, putMetricAlarmType);
        action.info.setPhysicalResourceId(action.properties.getAlarmName());
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
    DELETE_ALARM {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSCloudWatchAlarmResourceAction action = (AWSCloudWatchAlarmResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(CloudWatch.class);
        if (action.info.getPhysicalResourceId() == null) return action;
        DescribeAlarmsType describeAlarmsType = MessageHelper.createMessage(DescribeAlarmsType.class, action.info.getEffectiveUserId());
        AlarmNames alarmNames = new AlarmNames();
        alarmNames.setMember(Lists.newArrayList(action.info.getPhysicalResourceId()));
        describeAlarmsType.setAlarmNames(alarmNames);
        DescribeAlarmsResponseType describeAlarmsResponseType = AsyncRequests.<DescribeAlarmsType,DescribeAlarmsResponseType> sendSync(configuration, describeAlarmsType);
        if (describeAlarmsResponseType.getDescribeAlarmsResult() != null && describeAlarmsResponseType.getDescribeAlarmsResult().getMetricAlarms() != null &&
          describeAlarmsResponseType.getDescribeAlarmsResult().getMetricAlarms().getMember() != null &&
          describeAlarmsResponseType.getDescribeAlarmsResult().getMetricAlarms().getMember().size() > 0) {
          DeleteAlarmsType deleteAlarmsType = MessageHelper.createMessage(DeleteAlarmsType.class, action.info.getEffectiveUserId());
          deleteAlarmsType.setAlarmNames(alarmNames);
          AsyncRequests.<DeleteAlarmsType, DeleteAlarmsResponseType> sendSync(configuration, deleteAlarmsType);
        }
        return action;
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
    properties = (AWSCloudWatchAlarmProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSCloudWatchAlarmResourceInfo) resourceInfo;
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


