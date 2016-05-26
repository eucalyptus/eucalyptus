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
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.EC2Helper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSAutoScalingScalingPolicyResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSAutoScalingScalingPolicyProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AddressInfoType;
import com.eucalyptus.compute.common.AssociateAddressResponseType;
import com.eucalyptus.compute.common.AssociateAddressType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.DisassociateAddressResponseType;
import com.eucalyptus.compute.common.DisassociateAddressType;
import com.eucalyptus.compute.common.Filter;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSAutoScalingScalingPolicyResourceAction extends StepBasedResourceAction {

  private AWSAutoScalingScalingPolicyProperties properties = new AWSAutoScalingScalingPolicyProperties();
  private AWSAutoScalingScalingPolicyResourceInfo info = new AWSAutoScalingScalingPolicyResourceInfo();

  public AWSAutoScalingScalingPolicyResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSAutoScalingScalingPolicyResourceAction otherAction = (AWSAutoScalingScalingPolicyResourceAction) resourceAction;
    if (!Objects.equals(properties.getAdjustmentType(), otherAction.properties.getAdjustmentType())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getAutoScalingGroupName(), otherAction.properties.getAutoScalingGroupName())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getCooldown(), otherAction.properties.getCooldown())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getScalingAdjustment(), otherAction.properties.getScalingAdjustment())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_SCALING_POLICY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSAutoScalingScalingPolicyResourceAction action = (AWSAutoScalingScalingPolicyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        DescribeAutoScalingGroupsType describeAutoScalingGroupsType = MessageHelper.createMessage(DescribeAutoScalingGroupsType.class, action.info.getEffectiveUserId());
        AutoScalingGroupNames autoScalingGroupNames = new AutoScalingGroupNames();
        ArrayList<String> member = Lists.newArrayList(action.properties.getAutoScalingGroupName());
        autoScalingGroupNames.setMember(member);
        describeAutoScalingGroupsType.setAutoScalingGroupNames(autoScalingGroupNames);
        DescribeAutoScalingGroupsResponseType describeAutoScalingGroupsResponseType = AsyncRequests.<DescribeAutoScalingGroupsType,DescribeAutoScalingGroupsResponseType> sendSync(configuration, describeAutoScalingGroupsType);
        if (action.doesGroupNotExist(describeAutoScalingGroupsResponseType)) {
          throw new Exception("Autoscaling group " + action.properties.getAutoScalingGroupName() + " does not exist");
        }
        String scalingPolicyName = action.getDefaultPhysicalResourceId();
        PutScalingPolicyType putScalingPolicyType = MessageHelper.createMessage(PutScalingPolicyType.class, action.info.getEffectiveUserId());
        putScalingPolicyType.setAutoScalingGroupName(action.properties.getAutoScalingGroupName());
        putScalingPolicyType.setAdjustmentType(action.properties.getAdjustmentType());
        putScalingPolicyType.setCooldown(action.properties.getCooldown());
        putScalingPolicyType.setPolicyName(scalingPolicyName);
        putScalingPolicyType.setScalingAdjustment(action.properties.getScalingAdjustment());
        PutScalingPolicyResponseType putScalingPolicyResponseType = AsyncRequests.<PutScalingPolicyType,PutScalingPolicyResponseType> sendSync(configuration, putScalingPolicyType);
        action.info.setPhysicalResourceId(putScalingPolicyResponseType.getPutScalingPolicyResult().getPolicyARN()); // Docs are wrong, need ARN for alarms (and it is what AWS does
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
    DELETE_SCALING_POLICY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSAutoScalingScalingPolicyResourceAction action = (AWSAutoScalingScalingPolicyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        // no group, bye...
        DescribeAutoScalingGroupsType describeAutoScalingGroupsType = MessageHelper.createMessage(DescribeAutoScalingGroupsType.class, action.info.getEffectiveUserId());
        AutoScalingGroupNames autoScalingGroupNames = new AutoScalingGroupNames();
        ArrayList<String> member = Lists.newArrayList(action.properties.getAutoScalingGroupName());
        autoScalingGroupNames.setMember(member);
        describeAutoScalingGroupsType.setAutoScalingGroupNames(autoScalingGroupNames);
        DescribeAutoScalingGroupsResponseType describeAutoScalingGroupsResponseType = AsyncRequests.<DescribeAutoScalingGroupsType,DescribeAutoScalingGroupsResponseType> sendSync(configuration, describeAutoScalingGroupsType);
        if (action.doesGroupNotExist(describeAutoScalingGroupsResponseType)) {
          return action;
        }
        // Can delete with no consequence if not gone
        DeletePolicyType deletePolicyType = MessageHelper.createMessage(DeletePolicyType.class, action.info.getEffectiveUserId());
        deletePolicyType.setPolicyName(action.info.getPhysicalResourceId());
        deletePolicyType.setAutoScalingGroupName(action.properties.getAutoScalingGroupName());
        AsyncRequests.<DeletePolicyType,DeletePolicyResponseType> sendSync(configuration, deletePolicyType);
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
    UPDATE_SCALING_POLICY {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSAutoScalingScalingPolicyResourceAction oldAction = (AWSAutoScalingScalingPolicyResourceAction) oldResourceAction;
        AWSAutoScalingScalingPolicyResourceAction newAction = (AWSAutoScalingScalingPolicyResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        if (!Objects.equals(oldAction.properties.getAutoScalingGroupName(), newAction.properties.getAutoScalingGroupName())) {
          DeletePolicyType deletePolicyType = MessageHelper.createMessage(DeletePolicyType.class, newAction.info.getEffectiveUserId());
          deletePolicyType.setPolicyName(oldAction.info.getPhysicalResourceId());
          deletePolicyType.setAutoScalingGroupName(oldAction.properties.getAutoScalingGroupName());
          AsyncRequests.<DeletePolicyType, DeletePolicyResponseType>sendSync(configuration, deletePolicyType);
        }
        String policyName = getPolicyNameFromArn(oldAction.info.getPhysicalResourceId());
        PutScalingPolicyType putScalingPolicyType = MessageHelper.createMessage(PutScalingPolicyType.class, newAction.info.getEffectiveUserId());
        putScalingPolicyType.setAutoScalingGroupName(newAction.properties.getAutoScalingGroupName());
        putScalingPolicyType.setAdjustmentType(newAction.properties.getAdjustmentType());
        putScalingPolicyType.setCooldown(newAction.properties.getCooldown());
        putScalingPolicyType.setPolicyName(policyName);
        putScalingPolicyType.setScalingAdjustment(newAction.properties.getScalingAdjustment());
        PutScalingPolicyResponseType putScalingPolicyResponseType = AsyncRequests.<PutScalingPolicyType,PutScalingPolicyResponseType> sendSync(configuration, putScalingPolicyType);
        newAction.info.setPhysicalResourceId(putScalingPolicyResponseType.getPutScalingPolicyResult().getPolicyARN()); // Docs are wrong, need ARN for alarms (and it is what AWS does
        newAction.info.setCreatedEnoughToDelete(true);
        newAction.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(newAction.info.getPhysicalResourceId())));
        return newAction;
      }
      @Nullable
      @Override
      public Integer getTimeout() {
        return null;
      }
    };

  }

  private static String getPolicyNameFromArn(String arn) throws ValidationErrorException {
    if (arn == null) return null;
    // Hack.  Policy ARN looks like:
    //arn:aws:autoscaling:<region>:<account-id>:scalingPolicy:<some-uuid>:autoScalingGroupName/<groupName>:policyName/<policyName>
   if (!arn.contains("policyName/")) {
     throw new ValidationErrorException("Invalid policy arn " + arn);
   } else {
     return arn.substring(arn.lastIndexOf("policyName/") + "policyName/".length());
   }
  }

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



}


