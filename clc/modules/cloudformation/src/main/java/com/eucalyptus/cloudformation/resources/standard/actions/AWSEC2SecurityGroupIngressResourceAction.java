/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2SecurityGroupIngressResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2SecurityGroupIngressProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AuthorizeSecurityGroupIngressResponseType;
import com.eucalyptus.compute.common.AuthorizeSecurityGroupIngressType;
import com.eucalyptus.compute.common.CloudFilters;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Objects;


/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2SecurityGroupIngressResourceAction extends StepBasedResourceAction {
  private static final Logger LOG = Logger.getLogger(AWSEC2SecurityGroupIngressResourceAction.class);
  private AWSEC2SecurityGroupIngressProperties properties = new AWSEC2SecurityGroupIngressProperties();
  private AWSEC2SecurityGroupIngressResourceInfo info = new AWSEC2SecurityGroupIngressResourceInfo();

  public AWSEC2SecurityGroupIngressResourceAction() {
    // only replacement update options
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), null, null);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSEC2SecurityGroupIngressResourceAction otherAction = (AWSEC2SecurityGroupIngressResourceAction) resourceAction;
    if (!Objects.equals(properties.getGroupName(), otherAction.properties.getGroupName())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }

    if (!Objects.equals(properties.getGroupId(), otherAction.properties.getGroupId())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }

    if (!Objects.equals(properties.getIpProtocol(), otherAction.properties.getIpProtocol())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }

    if (!Objects.equals(properties.getCidrIp(), otherAction.properties.getCidrIp())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }

    if (!Objects.equals(properties.getSourceSecurityGroupName(), otherAction.properties.getSourceSecurityGroupName())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }

    if (!Objects.equals(properties.getSourceSecurityGroupId(), otherAction.properties.getSourceSecurityGroupId())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }

    if (!Objects.equals(properties.getSourceSecurityGroupOwnerId(), otherAction.properties.getSourceSecurityGroupOwnerId())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }

    if (!Objects.equals(properties.getFromPort(), otherAction.properties.getFromPort())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }

    if (!Objects.equals(properties.getToPort(), otherAction.properties.getToPort())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    return updateType;
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
          describeSecurityGroupsType.setFilterSet( Lists.newArrayList( CloudFilters.filter( "group-id", action.properties.getGroupId( ) ) ) );
          DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType = AsyncRequests.sendSync(configuration, describeSecurityGroupsType);
          ArrayList<SecurityGroupItemType> securityGroupItemTypeArrayList = describeSecurityGroupsResponseType.getSecurityGroupInfo();
          if (securityGroupItemTypeArrayList == null || securityGroupItemTypeArrayList.isEmpty()) {
            throw new ValidationErrorException("No such group with id '" + action.properties.getGroupId()+"'");
          }
        }
        if (!Strings.isNullOrEmpty(action.properties.getGroupName())) {
          DescribeSecurityGroupsType describeSecurityGroupsType = MessageHelper.createMessage(DescribeSecurityGroupsType.class, action.info.getEffectiveUserId());
          describeSecurityGroupsType.setSecurityGroupSet(Lists.newArrayList(action.properties.getGroupName()));
          describeSecurityGroupsType.setFilterSet( Lists.newArrayList( CloudFilters.filter( "group-name", action.properties.getGroupName( ) ) ) );
          DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType = AsyncRequests.sendSync(configuration, describeSecurityGroupsType);
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
    DELETE_INGRESS_RULE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2SecurityGroupIngressResourceAction action = (AWSEC2SecurityGroupIngressResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;

        // property validation
        action.validateProperties();
        // Make sure security group exists.
        if (!Strings.isNullOrEmpty(action.properties.getGroupId())) {
          DescribeSecurityGroupsType describeSecurityGroupsType = MessageHelper.createMessage(DescribeSecurityGroupsType.class, action.info.getEffectiveUserId());
          describeSecurityGroupsType.setFilterSet( Lists.newArrayList( CloudFilters.filter( "group-id", action.properties.getGroupId( ) ) ) );
          DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType = AsyncRequests.sendSync(configuration, describeSecurityGroupsType);
          ArrayList<SecurityGroupItemType> securityGroupItemTypeArrayList = describeSecurityGroupsResponseType.getSecurityGroupInfo();
          if (securityGroupItemTypeArrayList == null || securityGroupItemTypeArrayList.isEmpty()) {
            return action; // no group
          }
        }
        if (!Strings.isNullOrEmpty(action.properties.getGroupName())) {
          DescribeSecurityGroupsType describeSecurityGroupsType = MessageHelper.createMessage(DescribeSecurityGroupsType.class, action.info.getEffectiveUserId());
          describeSecurityGroupsType.setFilterSet( Lists.newArrayList( CloudFilters.filter( "group-name", action.properties.getGroupName( ) ) ) );
          DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType = AsyncRequests.sendSync(configuration, describeSecurityGroupsType);
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



}


