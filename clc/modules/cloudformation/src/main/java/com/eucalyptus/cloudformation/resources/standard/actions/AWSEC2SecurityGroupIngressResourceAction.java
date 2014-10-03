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
import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2SecurityGroupIngressResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2SecurityGroupIngressProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.CreateStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.steps.MultiStepWithRetryCreatePromise;
import com.eucalyptus.cloudformation.workflow.steps.MultiStepWithRetryDeletePromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AuthorizeSecurityGroupIngressResponseType;
import com.eucalyptus.compute.common.AuthorizeSecurityGroupIngressType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeSecurityGroupsResponseType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsType;
import com.eucalyptus.compute.common.IpPermissionType;
import com.eucalyptus.compute.common.RevokeSecurityGroupIngressResponseType;
import com.eucalyptus.compute.common.RevokeSecurityGroupIngressType;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.UserIdGroupPairType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2SecurityGroupIngressResourceAction extends ResourceAction {
  private static final Logger LOG = Logger.getLogger(AWSEC2SecurityGroupIngressResourceAction.class);
  private AWSEC2SecurityGroupIngressProperties properties = new AWSEC2SecurityGroupIngressProperties();
  private AWSEC2SecurityGroupIngressResourceInfo info = new AWSEC2SecurityGroupIngressResourceInfo();

  public AWSEC2SecurityGroupIngressResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

  }
  private enum CreateSteps implements Step {
    CREATE_INGRESS_RULE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2SecurityGroupIngressResourceAction action = (AWSEC2SecurityGroupIngressResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // property validation
        action.validateProperties();
        // Make sure security group exists.
        if (!Strings.isNullOrEmpty(action.properties.getGroupId())) {
          DescribeSecurityGroupsType describeSecurityGroupsType = MessageHelper.createMessage(DescribeSecurityGroupsType.class, action.info.getEffectiveUserId());
          describeSecurityGroupsType.setSecurityGroupIdSet(Lists.newArrayList(action.properties.getGroupId()));
          DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType =
            AsyncRequests.<DescribeSecurityGroupsType, DescribeSecurityGroupsResponseType>sendSync(configuration, describeSecurityGroupsType);
          ArrayList<SecurityGroupItemType> securityGroupItemTypeArrayList = describeSecurityGroupsResponseType.getSecurityGroupInfo();
          if (securityGroupItemTypeArrayList == null || securityGroupItemTypeArrayList.isEmpty()) {
            throw new ValidationErrorException("No such group with id '" + action.properties.getGroupId()+"'");
          }
        }
        if (!Strings.isNullOrEmpty(action.properties.getGroupName())) {
          DescribeSecurityGroupsType describeSecurityGroupsType = MessageHelper.createMessage(DescribeSecurityGroupsType.class, action.info.getEffectiveUserId());
          describeSecurityGroupsType.setSecurityGroupSet(Lists.newArrayList(action.properties.getGroupName()));
          DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType =
            AsyncRequests.<DescribeSecurityGroupsType, DescribeSecurityGroupsResponseType>sendSync(configuration, describeSecurityGroupsType);
          ArrayList<SecurityGroupItemType> securityGroupItemTypeArrayList = describeSecurityGroupsResponseType.getSecurityGroupInfo();
          if (securityGroupItemTypeArrayList == null || securityGroupItemTypeArrayList.isEmpty()) {
            throw new ValidationErrorException("No such group with name '" + action.properties.getGroupName() + "'");
          }
          // may be multiple return values, make sure at least one with non-vpc
          boolean foundNonVpc = false;
          for (SecurityGroupItemType securityGroupItemType : securityGroupItemTypeArrayList) {
            if (Strings.isNullOrEmpty(securityGroupItemType.getVpcId())) {
              foundNonVpc = true;
              break;
            }
          }
          if (!foundNonVpc) {
            throw new ValidationErrorException("Invalid value '" + action.properties.getGroupName() + "' for groupName. " +
              "You may not reference VPC security groups by name. Please use the corresponding id for this operation.");
          }
        }

        AuthorizeSecurityGroupIngressType authorizeSecurityGroupIngressType = MessageHelper.createMessage(AuthorizeSecurityGroupIngressType.class, action.info.getEffectiveUserId());
        if (!Strings.isNullOrEmpty(action.properties.getGroupId())) {
          authorizeSecurityGroupIngressType.setGroupId(action.properties.getGroupId());
        }
        if (!Strings.isNullOrEmpty(action.properties.getGroupName())) {
          authorizeSecurityGroupIngressType.setGroupName(action.properties.getGroupName());
        }
        IpPermissionType ipPermissionType = new IpPermissionType(
          action.properties.getIpProtocol(),
          action.properties.getFromPort(),
          action.properties.getToPort()
        );
        if (!Strings.isNullOrEmpty(action.properties.getCidrIp())) {
          ipPermissionType.setCidrIpRanges(Lists.newArrayList(action.properties.getCidrIp()));
        }
        if (!Strings.isNullOrEmpty(action.properties.getSourceSecurityGroupId())) {
          // Generally no need for SourceSecurityGroupOwnerId if SourceSecurityGroupId is set, but pass it along if set
          ipPermissionType.setGroups(Lists.newArrayList(new UserIdGroupPairType(action.properties.getSourceSecurityGroupOwnerId(), null, action.properties.getSourceSecurityGroupId())));
        }
        if (!Strings.isNullOrEmpty(action.properties.getSourceSecurityGroupName())) {
          // I think SourceSecurityGroupOwnerId is needed here.  If not provided, use the local account id
          String sourceSecurityGroupOwnerId = action.properties.getSourceSecurityGroupOwnerId();
          if (Strings.isNullOrEmpty(sourceSecurityGroupOwnerId)) {
            sourceSecurityGroupOwnerId = action.stackEntity.getAccountId();
          }
          ipPermissionType.setGroups(Lists.newArrayList(new UserIdGroupPairType(sourceSecurityGroupOwnerId, action.properties.getSourceSecurityGroupName(), null)));
        }
        authorizeSecurityGroupIngressType.setIpPermissions(Lists.newArrayList(ipPermissionType));
        AuthorizeSecurityGroupIngressResponseType authorizeSecurityGroupIngressResponseType = AsyncRequests.<AuthorizeSecurityGroupIngressType, AuthorizeSecurityGroupIngressResponseType> sendSync(configuration, authorizeSecurityGroupIngressType);
        action.info.setPhysicalResourceId(action.info.getLogicalResourceId()); // Strange, but this is what AWS does in this particular case...
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }

      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
      }
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_INGRESS_RULE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2SecurityGroupIngressResourceAction action = (AWSEC2SecurityGroupIngressResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.info.getPhysicalResourceId() == null) return action;

        // property validation
        action.validateProperties();
        // Make sure security group exists.
        if (!Strings.isNullOrEmpty(action.properties.getGroupId())) {
          DescribeSecurityGroupsType describeSecurityGroupsType = MessageHelper.createMessage(DescribeSecurityGroupsType.class, action.info.getEffectiveUserId());
          describeSecurityGroupsType.setSecurityGroupIdSet(Lists.newArrayList(action.properties.getGroupId()));
          DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType =
            AsyncRequests.<DescribeSecurityGroupsType, DescribeSecurityGroupsResponseType>sendSync(configuration, describeSecurityGroupsType);
          ArrayList<SecurityGroupItemType> securityGroupItemTypeArrayList = describeSecurityGroupsResponseType.getSecurityGroupInfo();
          if (securityGroupItemTypeArrayList == null || securityGroupItemTypeArrayList.isEmpty()) {
            return action; // no group
          }
        }
        if (!Strings.isNullOrEmpty(action.properties.getGroupName())) {
          DescribeSecurityGroupsType describeSecurityGroupsType = MessageHelper.createMessage(DescribeSecurityGroupsType.class, action.info.getEffectiveUserId());
          describeSecurityGroupsType.setSecurityGroupSet(Lists.newArrayList(action.properties.getGroupName()));
          DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType =
            AsyncRequests.<DescribeSecurityGroupsType, DescribeSecurityGroupsResponseType>sendSync(configuration, describeSecurityGroupsType);
          ArrayList<SecurityGroupItemType> securityGroupItemTypeArrayList = describeSecurityGroupsResponseType.getSecurityGroupInfo();
          if (securityGroupItemTypeArrayList == null || securityGroupItemTypeArrayList.isEmpty()) {
            return action; // no group
          }
          // may be multiple return values, make sure at least one with non-vpc
          boolean foundNonVpc = false;
          for (SecurityGroupItemType securityGroupItemType : securityGroupItemTypeArrayList) {
            if (Strings.isNullOrEmpty(securityGroupItemType.getVpcId())) {
              foundNonVpc = true;
              break;
            }
          }
          if (!foundNonVpc) {
            return action; // no (non-vpc) group
          }
        }

        RevokeSecurityGroupIngressType revokeSecurityGroupIngressType = MessageHelper.createMessage(RevokeSecurityGroupIngressType.class, action.info.getEffectiveUserId());
        if (!Strings.isNullOrEmpty(action.properties.getGroupId())) {
          revokeSecurityGroupIngressType.setGroupId(action.properties.getGroupId());
        }
        if (!Strings.isNullOrEmpty(action.properties.getGroupName())) {
          revokeSecurityGroupIngressType.setGroupName(action.properties.getGroupName());
        }
        IpPermissionType ipPermissionType = new IpPermissionType(
          action.properties.getIpProtocol(),
          action.properties.getFromPort(),
          action.properties.getToPort()
        );
        if (!Strings.isNullOrEmpty(action.properties.getCidrIp())) {
          ipPermissionType.setCidrIpRanges(Lists.newArrayList(action.properties.getCidrIp()));
        }
        if (!Strings.isNullOrEmpty(action.properties.getSourceSecurityGroupId())) {
          // Generally no need for SourceSecurityGroupOwnerId if SourceSecurityGroupId is set, but pass it along if set
          ipPermissionType.setGroups(Lists.newArrayList(new UserIdGroupPairType(action.properties.getSourceSecurityGroupOwnerId(), null, action.properties.getSourceSecurityGroupId())));
        }
        if (!Strings.isNullOrEmpty(action.properties.getSourceSecurityGroupName())) {
          // I think SourceSecurityGroupOwnerId is needed here.  If not provided, use the local account id
          String sourceSecurityGroupOwnerId = action.properties.getSourceSecurityGroupOwnerId();
          if (Strings.isNullOrEmpty(sourceSecurityGroupOwnerId)) {
            sourceSecurityGroupOwnerId = action.stackEntity.getAccountId();
          }
          ipPermissionType.setGroups(Lists.newArrayList(new UserIdGroupPairType(sourceSecurityGroupOwnerId, action.properties.getSourceSecurityGroupName(), null)));
        }
        revokeSecurityGroupIngressType.setIpPermissions(Lists.newArrayList(ipPermissionType));
        RevokeSecurityGroupIngressResponseType revokeSecurityGroupIngressResponseType = AsyncRequests.<RevokeSecurityGroupIngressType, RevokeSecurityGroupIngressResponseType> sendSync(configuration, revokeSecurityGroupIngressType);
        return action;
      }

      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
      }
    }
  }

  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSEC2SecurityGroupIngressProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2SecurityGroupIngressResourceInfo) resourceInfo;
  }

  private void validateProperties() throws ValidationErrorException {
    // group id or group name must be set
    if (Strings.isNullOrEmpty(properties.getGroupId()) && Strings.isNullOrEmpty(properties.getGroupName())) {
      throw new ValidationErrorException("Exactly one of GroupName and GroupId must be specified");
    }
    // but not both
    if (!Strings.isNullOrEmpty(properties.getGroupId()) && !Strings.isNullOrEmpty(properties.getGroupName())) {
      throw new ValidationErrorException("Exactly one of GroupName and GroupId must be specified");
    }
    // Can't specify cidr and source security group
    if (!Strings.isNullOrEmpty(properties.getCidrIp()) &&
      (!Strings.isNullOrEmpty(properties.getSourceSecurityGroupId())
        || !Strings.isNullOrEmpty(properties.getSourceSecurityGroupName())
        || !Strings.isNullOrEmpty(properties.getSourceSecurityGroupOwnerId()))) {
      throw new ValidationErrorException("Both CidrIp and SourceSecurityGroup cannot be specified");
    }
    // Can't specify both source security group name and id
    if (!Strings.isNullOrEmpty(properties.getSourceSecurityGroupId()) && !Strings.isNullOrEmpty(properties.getSourceSecurityGroupName())) {
      throw new ValidationErrorException("Both SourceSecurityGroupName and SourceSecurityGroupId cannot be specified");
    }
  }

  @Override
  public Promise<String> getCreatePromise(CreateStackWorkflowImpl createStackWorkflow, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(CreateSteps.values()), StepTransform.INSTANCE);
    return new MultiStepWithRetryCreatePromise(createStackWorkflow, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId);
  }

  @Override
  public Promise<String> getDeletePromise(DeleteStackWorkflowImpl deleteStackWorkflow, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(DeleteSteps.values()), StepTransform.INSTANCE);
    return new MultiStepWithRetryDeletePromise(deleteStackWorkflow, stepIds, this).getDeletePromise(resourceId, stackId, accountId, effectiveUserId);
  }

}


