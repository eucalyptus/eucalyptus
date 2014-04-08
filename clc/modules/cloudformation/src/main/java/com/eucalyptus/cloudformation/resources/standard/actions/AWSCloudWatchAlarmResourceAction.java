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


import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSCloudWatchAlarmResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSCloudWatchAlarmProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.CloudWatchMetricDimension;
import com.eucalyptus.cloudformation.template.JsonHelper;
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
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

import java.util.ArrayList;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSCloudWatchAlarmResourceAction extends ResourceAction {

  private AWSCloudWatchAlarmProperties properties = new AWSCloudWatchAlarmProperties();
  private AWSCloudWatchAlarmResourceInfo info = new AWSCloudWatchAlarmResourceInfo();
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
  public void create(int stepNum) throws Exception {
    switch (stepNum) {
      case 0:
        if (properties.getAlarmName() == null) {
          properties.setAlarmName(getDefaultPhysicalResourceId());
        }
        // check alarm exists (TODO: check aws does this...)
        ServiceConfiguration configuration = Topology.lookup(CloudWatch.class);
        DescribeAlarmsType describeAlarmsType = new DescribeAlarmsType();
        AlarmNames alarmNames = new AlarmNames();
        alarmNames.setMember(Lists.newArrayList(properties.getAlarmName()));
        describeAlarmsType.setAlarmNames(alarmNames);
        describeAlarmsType.setEffectiveUserId(info.getEffectiveUserId());
        DescribeAlarmsResponseType describeAlarmsResponseType = AsyncRequests.<DescribeAlarmsType,DescribeAlarmsResponseType> sendSync(configuration, describeAlarmsType);
        if (describeAlarmsResponseType.getDescribeAlarmsResult() != null && describeAlarmsResponseType.getDescribeAlarmsResult().getMetricAlarms() != null &&
          describeAlarmsResponseType.getDescribeAlarmsResult().getMetricAlarms().getMember() != null &&
          describeAlarmsResponseType.getDescribeAlarmsResult().getMetricAlarms().getMember().size() > 0) {
          throw new ValidationErrorException("Alarm " + properties.getAlarmName() + " already exists");
        }
        PutMetricAlarmType putMetricAlarmType = new PutMetricAlarmType();
        putMetricAlarmType.setEffectiveUserId(info.getEffectiveUserId());
        putMetricAlarmType.setActionsEnabled(properties.getActionsEnabled() == null ? Boolean.TRUE : properties.getActionsEnabled());
        if (properties.getAlarmActions() != null) {
          ResourceList alarmActions = new ResourceList();
          ArrayList<String> alarmActionsMember = Lists.newArrayList(properties.getAlarmActions());
          alarmActions.setMember(alarmActionsMember);
          putMetricAlarmType.setAlarmActions(alarmActions);
        }
        putMetricAlarmType.setAlarmDescription(properties.getAlarmDescription());
        putMetricAlarmType.setAlarmName(properties.getAlarmName());
        putMetricAlarmType.setComparisonOperator(properties.getComparisonOperator());
        if (properties.getDimensions() != null) {
          Dimensions dimensions = new Dimensions();
          ArrayList<Dimension> dimensionsMember = Lists.newArrayList();
          for (CloudWatchMetricDimension cloudWatchMetricDimension: properties.getDimensions()) {
            Dimension dimension = new Dimension();
            dimension.setName(cloudWatchMetricDimension.getName());
            dimension.setValue(cloudWatchMetricDimension.getValue());
            dimensionsMember.add(dimension);
          }
          dimensions.setMember(dimensionsMember);
          putMetricAlarmType.setDimensions(dimensions);
        }
        putMetricAlarmType.setEvaluationPeriods(properties.getEvaluationPeriods());
        if (properties.getInsufficientDataActions() != null) {
          ResourceList insufficientDataActions = new ResourceList();
          ArrayList<String> insufficientDataActionsMember = Lists.newArrayList(properties.getInsufficientDataActions());
          insufficientDataActions.setMember(insufficientDataActionsMember);
          putMetricAlarmType.setInsufficientDataActions(insufficientDataActions);
        }
        putMetricAlarmType.setMetricName(properties.getMetricName());
        putMetricAlarmType.setNamespace(properties.getNamespace());
        if (properties.getOkActions() != null) {
          ResourceList okActions = new ResourceList();
          ArrayList<String> okActionsMember = Lists.newArrayList(properties.getOkActions());
          okActions.setMember(okActionsMember);
          putMetricAlarmType.setOkActions(okActions);
        }
        putMetricAlarmType.setPeriod(properties.getPeriod());
        putMetricAlarmType.setStatistic(properties.getStatistic());
        putMetricAlarmType.setThreshold(properties.getThreshold());
        putMetricAlarmType.setUnit(properties.getUnit());
        AsyncRequests.<PutMetricAlarmType, PutMetricAlarmResponseType> sendSync(configuration, putMetricAlarmType);
        info.setPhysicalResourceId(properties.getAlarmName());
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      default:
        throw new IllegalStateException("Invalid step " + stepNum);
    }
  }


  @Override
  public void update(int stepNum) throws Exception {
    throw new UnsupportedOperationException();
  }

  public void rollbackUpdate() throws Exception {
    // can't update so rollbackUpdate should be a NOOP
  }

  @Override
  public void delete() throws Exception {
    if (info.getPhysicalResourceId() == null) return;
    ServiceConfiguration configuration = Topology.lookup(CloudWatch.class);
    DescribeAlarmsType describeAlarmsType = new DescribeAlarmsType();
    AlarmNames alarmNames = new AlarmNames();
    alarmNames.setMember(Lists.newArrayList(info.getPhysicalResourceId()));
    describeAlarmsType.setAlarmNames(alarmNames);
    describeAlarmsType.setEffectiveUserId(info.getEffectiveUserId());
    DescribeAlarmsResponseType describeAlarmsResponseType = AsyncRequests.<DescribeAlarmsType,DescribeAlarmsResponseType> sendSync(configuration, describeAlarmsType);
    if (describeAlarmsResponseType.getDescribeAlarmsResult() != null && describeAlarmsResponseType.getDescribeAlarmsResult().getMetricAlarms() != null &&
      describeAlarmsResponseType.getDescribeAlarmsResult().getMetricAlarms().getMember() != null &&
      describeAlarmsResponseType.getDescribeAlarmsResult().getMetricAlarms().getMember().size() > 0) {
      DeleteAlarmsType deleteAlarmsType = new DeleteAlarmsType();
      deleteAlarmsType.setAlarmNames(alarmNames);
      deleteAlarmsType.setEffectiveUserId(info.getEffectiveUserId());
      AsyncRequests.<DeleteAlarmsType, DeleteAlarmsResponseType> sendSync(configuration, deleteAlarmsType);
    }
  }


  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}


