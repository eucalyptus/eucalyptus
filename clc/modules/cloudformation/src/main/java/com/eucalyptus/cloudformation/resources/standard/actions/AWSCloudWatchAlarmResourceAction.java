/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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


import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSCloudWatchAlarmResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSCloudWatchAlarmProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.CloudWatchMetricDimension;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSCloudWatchAlarmResourceAction extends StepBasedResourceAction {

  private AWSCloudWatchAlarmProperties properties = new AWSCloudWatchAlarmProperties();
  private AWSCloudWatchAlarmResourceInfo info = new AWSCloudWatchAlarmResourceInfo();

  public AWSCloudWatchAlarmResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
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
        action.info.setCreatedEnoughToDelete(true);
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
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
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

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_ALARM {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSCloudWatchAlarmResourceAction oldAction = (AWSCloudWatchAlarmResourceAction) oldResourceAction;
        AWSCloudWatchAlarmResourceAction newAction = (AWSCloudWatchAlarmResourceAction) newResourceAction;
        // just update the alarm values.
        ServiceConfiguration configuration = Topology.lookup(CloudWatch.class);
        PutMetricAlarmType putMetricAlarmType = MessageHelper.createMessage(PutMetricAlarmType.class, oldAction.info.getEffectiveUserId());
        putMetricAlarmType.setActionsEnabled(newAction.properties.getActionsEnabled() == null ? Boolean.TRUE : newAction.properties.getActionsEnabled());
        if (newAction.properties.getAlarmActions() != null) {
          ResourceList alarmActions = new ResourceList();
          ArrayList<String> alarmActionsMember = Lists.newArrayList(newAction.properties.getAlarmActions());
          alarmActions.setMember(alarmActionsMember);
          putMetricAlarmType.setAlarmActions(alarmActions);
        }
        putMetricAlarmType.setAlarmDescription(newAction.properties.getAlarmDescription());
        putMetricAlarmType.setAlarmName(oldAction.info.getPhysicalResourceId()); // alarm name is physical resource id
        putMetricAlarmType.setComparisonOperator(newAction.properties.getComparisonOperator());
        if (newAction.properties.getDimensions() != null) {
          Dimensions dimensions = new Dimensions();
          ArrayList<Dimension> dimensionsMember = Lists.newArrayList();
          for (CloudWatchMetricDimension cloudWatchMetricDimension: newAction.properties.getDimensions()) {
            Dimension dimension = new Dimension();
            dimension.setName(cloudWatchMetricDimension.getName());
            dimension.setValue(cloudWatchMetricDimension.getValue());
            dimensionsMember.add(dimension);
          }
          dimensions.setMember(dimensionsMember);
          putMetricAlarmType.setDimensions(dimensions);
        }
        putMetricAlarmType.setEvaluationPeriods(newAction.properties.getEvaluationPeriods());
        if (newAction.properties.getInsufficientDataActions() != null) {
          ResourceList insufficientDataActions = new ResourceList();
          ArrayList<String> insufficientDataActionsMember = Lists.newArrayList(newAction.properties.getInsufficientDataActions());
          insufficientDataActions.setMember(insufficientDataActionsMember);
          putMetricAlarmType.setInsufficientDataActions(insufficientDataActions);
        }
        putMetricAlarmType.setMetricName(newAction.properties.getMetricName());
        putMetricAlarmType.setNamespace(newAction.properties.getNamespace());
        if (newAction.properties.getOkActions() != null) {
          ResourceList okActions = new ResourceList();
          ArrayList<String> okActionsMember = Lists.newArrayList(newAction.properties.getOkActions());
          okActions.setMember(okActionsMember);
          putMetricAlarmType.setOkActions(okActions);
        }
        putMetricAlarmType.setPeriod(newAction.properties.getPeriod());
        putMetricAlarmType.setStatistic(newAction.properties.getStatistic());
        putMetricAlarmType.setThreshold(newAction.properties.getThreshold());
        putMetricAlarmType.setUnit(newAction.properties.getUnit());
        AsyncRequests.<PutMetricAlarmType, PutMetricAlarmResponseType> sendSync(configuration, putMetricAlarmType);
        return newAction;
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
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSCloudWatchAlarmResourceAction otherAction = (AWSCloudWatchAlarmResourceAction) resourceAction;
    if (!Objects.equals(properties.getActionsEnabled(), otherAction.properties.getActionsEnabled())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getAlarmActions(), otherAction.properties.getAlarmActions())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getAlarmDescription(), otherAction.properties.getAlarmDescription())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getAlarmName(), otherAction.properties.getAlarmName())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getComparisonOperator(), otherAction.properties.getComparisonOperator())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getDimensions(), otherAction.properties.getDimensions())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getEvaluationPeriods(), otherAction.properties.getEvaluationPeriods())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getInsufficientDataActions(), otherAction.properties.getInsufficientDataActions())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getMetricName(), otherAction.properties.getMetricName())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getNamespace(), otherAction.properties.getNamespace())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getOkActions(), otherAction.properties.getOkActions())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getPeriod(), otherAction.properties.getPeriod())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getStatistic(), otherAction.properties.getStatistic())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getThreshold(), otherAction.properties.getThreshold())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getUnit(), otherAction.properties.getUnit())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }

}


