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


import com.eucalyptus.autoscaling.common.AutoScaling;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupNames;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingNotificationTypes;
import com.eucalyptus.autoscaling.common.msgs.AvailabilityZones;
import com.eucalyptus.autoscaling.common.msgs.CreateAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.msgs.CreateAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.DeleteAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.msgs.DeleteAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsType;
import com.eucalyptus.autoscaling.common.msgs.LoadBalancerNames;
import com.eucalyptus.autoscaling.common.msgs.PutNotificationConfigurationResponseType;
import com.eucalyptus.autoscaling.common.msgs.PutNotificationConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.TerminationPolicies;
import com.eucalyptus.autoscaling.common.msgs.UpdateAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.msgs.UpdateAutoScalingGroupType;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSAutoScalingAutoScalingGroupResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSAutoScalingAutoScalingGroupProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import java.util.ArrayList;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSAutoScalingAutoScalingGroupResourceAction extends ResourceAction {

  private AWSAutoScalingAutoScalingGroupProperties properties = new AWSAutoScalingAutoScalingGroupProperties();
  private AWSAutoScalingAutoScalingGroupResourceInfo info = new AWSAutoScalingAutoScalingGroupResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSAutoScalingAutoScalingGroupProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSAutoScalingAutoScalingGroupResourceInfo) resourceInfo;
  }

  @Override
  public int getNumCreateSteps() {
    return 3;
  }

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
    switch (stepNum) {
      case 0:
        String autoScalingGroupName = getDefaultPhysicalResourceId();
        CreateAutoScalingGroupType createAutoScalingGroupType = new CreateAutoScalingGroupType();
        createAutoScalingGroupType.setAutoScalingGroupName(autoScalingGroupName);
        if (properties.getInstanceId() != null) {
          throw new ValidationErrorException("InstanceId not supported");
        }
        if (properties.getLaunchConfigurationName() == null) {
          throw new ValidationErrorException("LaunchConfiguration required (as InstanceId not supported)");
        }
        if (properties.getAvailabilityZones() != null) {
          createAutoScalingGroupType.setAvailabilityZones(new AvailabilityZones(properties.getAvailabilityZones()));
        }
        createAutoScalingGroupType.setDefaultCooldown(properties.getCooldown());
        createAutoScalingGroupType.setDesiredCapacity(properties.getDesiredCapacity());
        createAutoScalingGroupType.setHealthCheckGracePeriod(properties.getHealthCheckGracePeriod());
        createAutoScalingGroupType.setLaunchConfigurationName(properties.getLaunchConfigurationName());
        if (properties.getLoadBalancerNames() != null) {
          createAutoScalingGroupType.setLoadBalancerNames(new LoadBalancerNames(properties.getLoadBalancerNames()));
        }
        createAutoScalingGroupType.setMaxSize(properties.getMaxSize());
        createAutoScalingGroupType.setMinSize(properties.getMinSize());
        if (properties.getTerminationPolicies() != null) {
          createAutoScalingGroupType.setTerminationPolicies(new TerminationPolicies(properties.getTerminationPolicies()));
        }
        if (properties.getVpcZoneIdentifier() != null && properties.getVpcZoneIdentifier().size() > 1) {
//          createAutoScalingGroupType.setVpcZoneIdentifier(Joiner.on(",").join(properties.getVpcZoneIdentifier()));
          throw new Exception("Multiple values for vpc zone identifier not supported");
        } else if (properties.getVpcZoneIdentifier() != null && properties.getVpcZoneIdentifier().size() == 1) {
            createAutoScalingGroupType.setVpcZoneIdentifier(properties.getVpcZoneIdentifier().get(0));
        }
        createAutoScalingGroupType.setEffectiveUserId(info.getEffectiveUserId());
        AsyncRequests.<CreateAutoScalingGroupType,CreateAutoScalingGroupResponseType> sendSync(configuration, createAutoScalingGroupType);
        info.setPhysicalResourceId(autoScalingGroupName);
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      case 1: // add notification configurations
        if (properties.getNotificationConfiguration() != null) {
          PutNotificationConfigurationType putNotificationConfigurationType = new PutNotificationConfigurationType();
          putNotificationConfigurationType.setAutoScalingGroupName(info.getPhysicalResourceId());
          putNotificationConfigurationType.setTopicARN(properties.getNotificationConfiguration().getTopicARN());
          AutoScalingNotificationTypes autoScalingNotificationTypes = new AutoScalingNotificationTypes();
          ArrayList<String> member = Lists.newArrayList(properties.getNotificationConfiguration().getNotificationTypes());
          autoScalingNotificationTypes.setMember(member);
          putNotificationConfigurationType.setNotificationTypes(autoScalingNotificationTypes);
          putNotificationConfigurationType.setEffectiveUserId(info.getEffectiveUserId());
          AsyncRequests.<PutNotificationConfigurationType,PutNotificationConfigurationResponseType> sendSync(configuration, putNotificationConfigurationType);
        }
        break;
      case 2: // add tags (later)
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
    ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
    // first see if group exists..
    DescribeAutoScalingGroupsType describeAutoScalingGroupsType = new DescribeAutoScalingGroupsType();
    AutoScalingGroupNames autoScalingGroupNames = new AutoScalingGroupNames();
    ArrayList<String> member = Lists.newArrayList(info.getPhysicalResourceId());
    autoScalingGroupNames.setMember(member);
    describeAutoScalingGroupsType.setAutoScalingGroupNames(autoScalingGroupNames);
    describeAutoScalingGroupsType.setEffectiveUserId(info.getEffectiveUserId());
    DescribeAutoScalingGroupsResponseType describeAutoScalingGroupsResponseType = AsyncRequests.<DescribeAutoScalingGroupsType,DescribeAutoScalingGroupsResponseType> sendSync(configuration, describeAutoScalingGroupsType);
    if (doesGroupNotExist(describeAutoScalingGroupsResponseType)) {
      return; // group is gone...
    }
    // make sure capacity, etc is 0
    UpdateAutoScalingGroupType updateAutoScalingGroupType = new UpdateAutoScalingGroupType();
    updateAutoScalingGroupType.setMinSize(0);
    updateAutoScalingGroupType.setMaxSize(0);
    updateAutoScalingGroupType.setDesiredCapacity(0);
    updateAutoScalingGroupType.setAutoScalingGroupName(info.getPhysicalResourceId());
    updateAutoScalingGroupType.setEffectiveUserId(info.getEffectiveUserId());
    AsyncRequests.<UpdateAutoScalingGroupType,UpdateAutoScalingGroupResponseType> sendSync(configuration, updateAutoScalingGroupType);
    boolean zeroInstances = false;
    for (int i=0;i<60;i++) { // sleeping for 5 seconds 60 times... (5 minutes)
      Thread.sleep(5000L);
      DescribeAutoScalingGroupsType describeAutoScalingGroupsType2 = new DescribeAutoScalingGroupsType();
      AutoScalingGroupNames autoScalingGroupNames2 = new AutoScalingGroupNames();
      ArrayList<String> member2 = Lists.newArrayList(info.getPhysicalResourceId());
      autoScalingGroupNames2.setMember(member2);
      describeAutoScalingGroupsType2.setAutoScalingGroupNames(autoScalingGroupNames2);
      describeAutoScalingGroupsType2.setEffectiveUserId(info.getEffectiveUserId());
      DescribeAutoScalingGroupsResponseType describeAutoScalingGroupsResponseType2 = AsyncRequests.<DescribeAutoScalingGroupsType,DescribeAutoScalingGroupsResponseType> sendSync(configuration, describeAutoScalingGroupsType2);
      if (doesGroupNotExist(describeAutoScalingGroupsResponseType2)) {
        return; // group is gone...
      } else {
        AutoScalingGroupType firstGroup = describeAutoScalingGroupsResponseType2.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember().get(0);
        if (firstGroup.getInstances() == null || firstGroup.getInstances().getMember() == null || firstGroup.getInstances().getMember().isEmpty()) {
          zeroInstances = true;
          break;
        }
      }
    }
    if (!zeroInstances) throw new Exception("Timeout (waiting for 0 instances)");
    // Now delete it...
    DeleteAutoScalingGroupType deleteAutoScalingGroupType = new DeleteAutoScalingGroupType();
    deleteAutoScalingGroupType.setAutoScalingGroupName(info.getPhysicalResourceId());
    deleteAutoScalingGroupType.setEffectiveUserId(info.getEffectiveUserId());
    AsyncRequests.<DeleteAutoScalingGroupType,DeleteAutoScalingGroupResponseType> sendSync(configuration, deleteAutoScalingGroupType);
    for (int i=0;i<60;i++) { // sleeping for 5 seconds 60 times... (5 minutes)
      Thread.sleep(5000L);
      DescribeAutoScalingGroupsType describeAutoScalingGroupsType2 = new DescribeAutoScalingGroupsType();
      AutoScalingGroupNames autoScalingGroupNames2 = new AutoScalingGroupNames();
      ArrayList<String> member2 = Lists.newArrayList(info.getPhysicalResourceId());
      autoScalingGroupNames2.setMember(member2);
      describeAutoScalingGroupsType2.setAutoScalingGroupNames(autoScalingGroupNames2);
      describeAutoScalingGroupsType2.setEffectiveUserId(info.getEffectiveUserId());
      DescribeAutoScalingGroupsResponseType describeAutoScalingGroupsResponseType2 = AsyncRequests.<DescribeAutoScalingGroupsType,DescribeAutoScalingGroupsResponseType> sendSync(configuration, describeAutoScalingGroupsType2);
      if (doesGroupNotExist(describeAutoScalingGroupsResponseType2)) {
        return; // group is gone...
      }
    }
    throw new Exception("Timeout (waiting for group to be deleted)");
  }

  private boolean doesGroupNotExist(DescribeAutoScalingGroupsResponseType describeAutoScalingGroupsResponseType) {
    return describeAutoScalingGroupsResponseType == null || describeAutoScalingGroupsResponseType.getDescribeAutoScalingGroupsResult() == null
      || describeAutoScalingGroupsResponseType.getDescribeAutoScalingGroupsResult().getAutoScalingGroups() == null
      || describeAutoScalingGroupsResponseType.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember() == null
      || describeAutoScalingGroupsResponseType.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember().isEmpty();
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}


