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
import com.eucalyptus.autoscaling.common.msgs.DeletePolicyResponseType;
import com.eucalyptus.autoscaling.common.msgs.DeletePolicyType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsType;
import com.eucalyptus.autoscaling.common.msgs.PutScalingPolicyResponseType;
import com.eucalyptus.autoscaling.common.msgs.PutScalingPolicyType;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSAutoScalingScalingPolicyResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSAutoScalingScalingPolicyProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

import java.util.ArrayList;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSAutoScalingScalingPolicyResourceAction extends ResourceAction {

  private AWSAutoScalingScalingPolicyProperties properties = new AWSAutoScalingScalingPolicyProperties();
  private AWSAutoScalingScalingPolicyResourceInfo info = new AWSAutoScalingScalingPolicyResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSAutoScalingScalingPolicyProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSAutoScalingScalingPolicyResourceInfo) resourceInfo;
  }
  private boolean doesGroupNotExist(DescribeAutoScalingGroupsResponseType describeAutoScalingGroupsResponseType) {
    return describeAutoScalingGroupsResponseType == null || describeAutoScalingGroupsResponseType.getDescribeAutoScalingGroupsResult() == null
      || describeAutoScalingGroupsResponseType.getDescribeAutoScalingGroupsResult().getAutoScalingGroups() == null
      || describeAutoScalingGroupsResponseType.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember() == null
      || describeAutoScalingGroupsResponseType.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember().isEmpty();
  }

  @Override
  public void create(int stepNum) throws Exception {
    switch (stepNum) {
      case 0:
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        DescribeAutoScalingGroupsType describeAutoScalingGroupsType = new DescribeAutoScalingGroupsType();
        AutoScalingGroupNames autoScalingGroupNames = new AutoScalingGroupNames();
        ArrayList<String> member = Lists.newArrayList(properties.getAutoScalingGroupName());
        autoScalingGroupNames.setMember(member);
        describeAutoScalingGroupsType.setAutoScalingGroupNames(autoScalingGroupNames);
        describeAutoScalingGroupsType.setEffectiveUserId(info.getEffectiveUserId());
        DescribeAutoScalingGroupsResponseType describeAutoScalingGroupsResponseType2 = AsyncRequests.<DescribeAutoScalingGroupsType,DescribeAutoScalingGroupsResponseType> sendSync(configuration, describeAutoScalingGroupsType);
        if (doesGroupNotExist(describeAutoScalingGroupsResponseType2)) {
          throw new Exception("Autoscaling group " + properties.getAutoScalingGroupName() + " does not exist");
        }
        String scalingPolicyName = getDefaultPhysicalResourceId();
        PutScalingPolicyType putScalingPolicyType = new PutScalingPolicyType();
        putScalingPolicyType.setAutoScalingGroupName(properties.getAutoScalingGroupName());
        putScalingPolicyType.setAdjustmentType(properties.getAdjustmentType());
        putScalingPolicyType.setCooldown(properties.getCooldown());
        putScalingPolicyType.setPolicyName(scalingPolicyName);
        putScalingPolicyType.setScalingAdjustment(properties.getScalingAdjustment());
        putScalingPolicyType.setEffectiveUserId(info.getEffectiveUserId());
        PutScalingPolicyResponseType putScalingPolicyResponseType = AsyncRequests.<PutScalingPolicyType,PutScalingPolicyResponseType> sendSync(configuration, putScalingPolicyType);
        info.setPhysicalResourceId(putScalingPolicyResponseType.getPutScalingPolicyResult().getPolicyARN()); // Docs are wrong, need ARN for alarms (and it is what AWS does
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
  @Override
  public void delete() throws Exception {
    if (info.getPhysicalResourceId() == null) return;
    ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
    // no group, bye...
    DescribeAutoScalingGroupsType describeAutoScalingGroupsType = new DescribeAutoScalingGroupsType();
    AutoScalingGroupNames autoScalingGroupNames = new AutoScalingGroupNames();
    ArrayList<String> member = Lists.newArrayList(properties.getAutoScalingGroupName());
    autoScalingGroupNames.setMember(member);
    describeAutoScalingGroupsType.setAutoScalingGroupNames(autoScalingGroupNames);
    describeAutoScalingGroupsType.setEffectiveUserId(info.getEffectiveUserId());
    DescribeAutoScalingGroupsResponseType describeAutoScalingGroupsResponseType2 = AsyncRequests.<DescribeAutoScalingGroupsType,DescribeAutoScalingGroupsResponseType> sendSync(configuration, describeAutoScalingGroupsType);
    if (doesGroupNotExist(describeAutoScalingGroupsResponseType2)) {
      return;
    }
    // Can delete with no consequence if not gone
    DeletePolicyType deletePolicyType = new DeletePolicyType();
    deletePolicyType.setPolicyName(info.getPhysicalResourceId());
    deletePolicyType.setAutoScalingGroupName(properties.getAutoScalingGroupName());
    deletePolicyType.setEffectiveUserId(info.getEffectiveUserId());
    AsyncRequests.<DeletePolicyType,DeletePolicyResponseType> sendSync(configuration, deletePolicyType);
  }

  public void rollbackUpdate() throws Exception {
    // can't update so rollbackUpdate should be a NOOP
  }


  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}


