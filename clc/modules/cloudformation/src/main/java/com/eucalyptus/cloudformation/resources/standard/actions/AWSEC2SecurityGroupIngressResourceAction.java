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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2SecurityGroupIngressResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2SecurityGroupIngressProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
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
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Map;


/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2SecurityGroupIngressResourceAction extends ResourceAction {
  private static final Logger LOG = Logger.getLogger(AWSEC2SecurityGroupIngressResourceAction.class);
  private AWSEC2SecurityGroupIngressProperties properties = new AWSEC2SecurityGroupIngressProperties();
  private AWSEC2SecurityGroupIngressResourceInfo info = new AWSEC2SecurityGroupIngressResourceInfo();
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

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    switch (stepNum) {
      case 0:
        // property validation
        validateProperties();
        // Make sure security group exists.
        if (!Strings.isNullOrEmpty(properties.getGroupId())) {
          DescribeSecurityGroupsType describeSecurityGroupsType = new DescribeSecurityGroupsType();
          describeSecurityGroupsType.setEffectiveUserId(info.getEffectiveUserId());
          describeSecurityGroupsType.setSecurityGroupIdSet(Lists.newArrayList(properties.getGroupId()));
          DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType =
            AsyncRequests.<DescribeSecurityGroupsType, DescribeSecurityGroupsResponseType>sendSync(configuration, describeSecurityGroupsType);
          ArrayList<SecurityGroupItemType> securityGroupItemTypeArrayList = describeSecurityGroupsResponseType.getSecurityGroupInfo();
          if (securityGroupItemTypeArrayList == null || securityGroupItemTypeArrayList.isEmpty()) {
            throw new ValidationErrorException("No such group with id '" + properties.getGroupId()+"'");
          }
        }
        if (!Strings.isNullOrEmpty(properties.getGroupName())) {
          DescribeSecurityGroupsType describeSecurityGroupsType = new DescribeSecurityGroupsType();
          describeSecurityGroupsType.setEffectiveUserId(info.getEffectiveUserId());
          describeSecurityGroupsType.setSecurityGroupSet(Lists.newArrayList(properties.getGroupName()));
          DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType =
            AsyncRequests.<DescribeSecurityGroupsType, DescribeSecurityGroupsResponseType>sendSync(configuration, describeSecurityGroupsType);
          ArrayList<SecurityGroupItemType> securityGroupItemTypeArrayList = describeSecurityGroupsResponseType.getSecurityGroupInfo();
          if (securityGroupItemTypeArrayList == null || securityGroupItemTypeArrayList.isEmpty()) {
            throw new ValidationErrorException("No such group with name '" + properties.getGroupName() + "'");
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
            throw new ValidationErrorException("Invalid value '" + properties.getGroupName() + "' for groupName. " +
              "You may not reference VPC security groups by name. Please use the corresponding id for this operation.");
          }
        }

        AuthorizeSecurityGroupIngressType authorizeSecurityGroupIngressType = new AuthorizeSecurityGroupIngressType();
        authorizeSecurityGroupIngressType.setEffectiveUserId(getResourceInfo().getEffectiveUserId());
        if (!Strings.isNullOrEmpty(properties.getGroupId())) {
          authorizeSecurityGroupIngressType.setGroupId(properties.getGroupId());
        }
        if (!Strings.isNullOrEmpty(properties.getGroupName())) {
          authorizeSecurityGroupIngressType.setGroupName(properties.getGroupName());
        }
        IpPermissionType ipPermissionType = new IpPermissionType(
          properties.getIpProtocol(),
          properties.getFromPort(),
          properties.getToPort()
        );
        if (!Strings.isNullOrEmpty(properties.getCidrIp())) {
          ipPermissionType.setCidrIpRanges(Lists.newArrayList(properties.getCidrIp()));
        }
        if (!Strings.isNullOrEmpty(properties.getSourceSecurityGroupId())) {
          // Generally no need for SourceSecurityGroupOwnerId if SourceSecurityGroupId is set, but pass it along if set
          ipPermissionType.setGroups(Lists.newArrayList(new UserIdGroupPairType(properties.getSourceSecurityGroupOwnerId(), null, properties.getSourceSecurityGroupId())));
        }
        if (!Strings.isNullOrEmpty(properties.getSourceSecurityGroupName())) {
          // I think SourceSecurityGroupOwnerId is needed here.  If not provided, use the local account id
          String sourceSecurityGroupOwnerId = properties.getSourceSecurityGroupOwnerId();
          if (Strings.isNullOrEmpty(sourceSecurityGroupOwnerId)) {
            sourceSecurityGroupOwnerId = stackEntity.getAccountId();
          }
          ipPermissionType.setGroups(Lists.newArrayList(new UserIdGroupPairType(sourceSecurityGroupOwnerId, properties.getSourceSecurityGroupName(), null)));
        }
        authorizeSecurityGroupIngressType.setIpPermissions(Lists.newArrayList(ipPermissionType));
        AuthorizeSecurityGroupIngressResponseType authorizeSecurityGroupIngressResponseType = AsyncRequests.<AuthorizeSecurityGroupIngressType, AuthorizeSecurityGroupIngressResponseType> sendSync(configuration, authorizeSecurityGroupIngressType);
        info.setPhysicalResourceId(info.getLogicalResourceId()); // Strange, but this is what AWS does in this particular case...
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      default:
        throw new IllegalStateException("Invalid step " + stepNum);
    }
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
  public void update(int stepNum) throws Exception {
    throw new UnsupportedOperationException();
  }

  public void rollbackUpdate() throws Exception {
    // can't update so rollbackUpdate should be a NOOP
  }

  @Override
  public void delete() throws Exception {
    if (info.getPhysicalResourceId() == null) return;

    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    // property validation
    validateProperties();
    // Make sure security group exists.
    if (!Strings.isNullOrEmpty(properties.getGroupId())) {
      DescribeSecurityGroupsType describeSecurityGroupsType = new DescribeSecurityGroupsType();
      describeSecurityGroupsType.setEffectiveUserId(info.getEffectiveUserId());
      describeSecurityGroupsType.setSecurityGroupIdSet(Lists.newArrayList(properties.getGroupId()));
      DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType =
        AsyncRequests.<DescribeSecurityGroupsType, DescribeSecurityGroupsResponseType>sendSync(configuration, describeSecurityGroupsType);
      ArrayList<SecurityGroupItemType> securityGroupItemTypeArrayList = describeSecurityGroupsResponseType.getSecurityGroupInfo();
      if (securityGroupItemTypeArrayList == null || securityGroupItemTypeArrayList.isEmpty()) {
        return; // no group
      }
    }
    if (!Strings.isNullOrEmpty(properties.getGroupName())) {
      DescribeSecurityGroupsType describeSecurityGroupsType = new DescribeSecurityGroupsType();
      describeSecurityGroupsType.setEffectiveUserId(info.getEffectiveUserId());
      describeSecurityGroupsType.setSecurityGroupSet(Lists.newArrayList(properties.getGroupName()));
      DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType =
        AsyncRequests.<DescribeSecurityGroupsType, DescribeSecurityGroupsResponseType>sendSync(configuration, describeSecurityGroupsType);
      ArrayList<SecurityGroupItemType> securityGroupItemTypeArrayList = describeSecurityGroupsResponseType.getSecurityGroupInfo();
      if (securityGroupItemTypeArrayList == null || securityGroupItemTypeArrayList.isEmpty()) {
        return; // no group
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
        return; // no (non-vpc) group
      }
    }

    RevokeSecurityGroupIngressType revokeSecurityGroupIngressType = new RevokeSecurityGroupIngressType();
    revokeSecurityGroupIngressType.setEffectiveUserId(getResourceInfo().getEffectiveUserId());
    if (!Strings.isNullOrEmpty(properties.getGroupId())) {
      revokeSecurityGroupIngressType.setGroupId(properties.getGroupId());
    }
    if (!Strings.isNullOrEmpty(properties.getGroupName())) {
      revokeSecurityGroupIngressType.setGroupName(properties.getGroupName());
    }
    IpPermissionType ipPermissionType = new IpPermissionType(
      properties.getIpProtocol(),
      properties.getFromPort(),
      properties.getToPort()
    );
    if (!Strings.isNullOrEmpty(properties.getCidrIp())) {
      ipPermissionType.setCidrIpRanges(Lists.newArrayList(properties.getCidrIp()));
    }
    if (!Strings.isNullOrEmpty(properties.getSourceSecurityGroupId())) {
      // Generally no need for SourceSecurityGroupOwnerId if SourceSecurityGroupId is set, but pass it along if set
      ipPermissionType.setGroups(Lists.newArrayList(new UserIdGroupPairType(properties.getSourceSecurityGroupOwnerId(), null, properties.getSourceSecurityGroupId())));
    }
    if (!Strings.isNullOrEmpty(properties.getSourceSecurityGroupName())) {
      // I think SourceSecurityGroupOwnerId is needed here.  If not provided, use the local account id
      String sourceSecurityGroupOwnerId = properties.getSourceSecurityGroupOwnerId();
      if (Strings.isNullOrEmpty(sourceSecurityGroupOwnerId)) {
        sourceSecurityGroupOwnerId = stackEntity.getAccountId();
      }
      ipPermissionType.setGroups(Lists.newArrayList(new UserIdGroupPairType(sourceSecurityGroupOwnerId, properties.getSourceSecurityGroupName(), null)));
    }
    revokeSecurityGroupIngressType.setIpPermissions(Lists.newArrayList(ipPermissionType));
    RevokeSecurityGroupIngressResponseType revokeSecurityGroupIngressResponseType = AsyncRequests.<RevokeSecurityGroupIngressType, RevokeSecurityGroupIngressResponseType> sendSync(configuration, revokeSecurityGroupIngressType);
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}


