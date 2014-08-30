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


import com.amazonaws.services.ec2.model.IpPermission;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2SecurityGroupEgressResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2SecurityGroupEgressProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AuthorizeSecurityGroupEgressResponseType;
import com.eucalyptus.compute.common.AuthorizeSecurityGroupEgressType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeSecurityGroupsResponseType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsType;
import com.eucalyptus.compute.common.IpPermissionType;
import com.eucalyptus.compute.common.RevokeSecurityGroupEgressResponseType;
import com.eucalyptus.compute.common.RevokeSecurityGroupEgressType;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.UserIdGroupPairType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.util.ArrayList;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2SecurityGroupEgressResourceAction extends ResourceAction {

  private AWSEC2SecurityGroupEgressProperties properties = new AWSEC2SecurityGroupEgressProperties();
  private AWSEC2SecurityGroupEgressResourceInfo info = new AWSEC2SecurityGroupEgressResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSEC2SecurityGroupEgressProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2SecurityGroupEgressResourceInfo) resourceInfo;
  }

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    switch (stepNum) {
      case 0:
        // property validation
        validateProperties();
        // Make sure security group exists.
        DescribeSecurityGroupsType describeSecurityGroupsType = new DescribeSecurityGroupsType();
        describeSecurityGroupsType.setEffectiveUserId(info.getEffectiveUserId());
        describeSecurityGroupsType.setSecurityGroupIdSet(Lists.newArrayList(properties.getGroupId()));
        DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType =
          AsyncRequests.<DescribeSecurityGroupsType, DescribeSecurityGroupsResponseType>sendSync(configuration, describeSecurityGroupsType);
        ArrayList<SecurityGroupItemType> securityGroupItemTypeArrayList = describeSecurityGroupsResponseType.getSecurityGroupInfo();
        boolean hasDefaultEgressRule = false;
        if (securityGroupItemTypeArrayList == null || securityGroupItemTypeArrayList.isEmpty()) {
          throw new ValidationErrorException("No such group with id '" + properties.getGroupId()+"'");
        } else {
          // should only be one, so take the first one
          SecurityGroupItemType securityGroupItemType = securityGroupItemTypeArrayList.get(0);
          if (securityGroupItemType.getIpPermissionsEgress() != null) {
            for (IpPermissionType ipPermissionType : securityGroupItemType.getIpPermissionsEgress()) {
              if (isDefaultEgressRule(ipPermissionType)) {
                hasDefaultEgressRule = true;
                break;
              }
            }
          }
        }

        AuthorizeSecurityGroupEgressType authorizeSecurityGroupEgressType = new AuthorizeSecurityGroupEgressType();
        authorizeSecurityGroupEgressType.setEffectiveUserId(getResourceInfo().getEffectiveUserId());
        authorizeSecurityGroupEgressType.setGroupId(properties.getGroupId());
        IpPermissionType ipPermissionType = new IpPermissionType(
          properties.getIpProtocol(),
          properties.getFromPort(),
          properties.getToPort()
        );
        if (!Strings.isNullOrEmpty(properties.getCidrIp())) {
          ipPermissionType.setCidrIpRanges(Lists.newArrayList(properties.getCidrIp()));
        }
        if (!Strings.isNullOrEmpty(properties.getDestinationSecurityGroupId())) {
          ipPermissionType.setGroups(Lists.newArrayList(new UserIdGroupPairType(null, null, properties.getDestinationSecurityGroupId())));
        }
        authorizeSecurityGroupEgressType.setIpPermissions(Lists.newArrayList(ipPermissionType));
        AuthorizeSecurityGroupEgressResponseType authorizeSecurityGroupIngressResponseType = AsyncRequests.<AuthorizeSecurityGroupEgressType, AuthorizeSecurityGroupEgressResponseType> sendSync(configuration, authorizeSecurityGroupEgressType);

        // remove default (if there)
        if (hasDefaultEgressRule) {
          RevokeSecurityGroupEgressType revokeSecurityGroupEgressType = new RevokeSecurityGroupEgressType();
          revokeSecurityGroupEgressType.setEffectiveUserId(getResourceInfo().getEffectiveUserId());
          revokeSecurityGroupEgressType.setGroupId(properties.getGroupId());
          revokeSecurityGroupEgressType.setIpPermissions(Lists.newArrayList(DEFAULT_EGRESS_RULE()));
          RevokeSecurityGroupEgressResponseType revokeSecurityGroupEgressResponseType = AsyncRequests.<RevokeSecurityGroupEgressType, RevokeSecurityGroupEgressResponseType> sendSync(configuration, revokeSecurityGroupEgressType);
        }

        info.setPhysicalResourceId(getDefaultPhysicalResourceId());
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      default:
        throw new IllegalStateException("Invalid step " + stepNum);
    }
  }

  private static final IpPermissionType DEFAULT_EGRESS_RULE() {
    IpPermissionType ipPermissionType = new IpPermissionType();
    ipPermissionType.setIpProtocol("-1");
    ipPermissionType.setCidrIpRanges(Lists.newArrayList("0.0.0.0/0"));
    return ipPermissionType;
  }

  private boolean isDefaultEgressRule(IpPermissionType ipPermissionType) {
    return ipPermissionType.getIpProtocol().equals("-1") && ipPermissionType.getFromPort() == null
      && ipPermissionType.getToPort() == null && ipPermissionType.getCidrIpRanges() != null &&
      ipPermissionType.getCidrIpRanges().size() == 1 && ipPermissionType.getCidrIpRanges().get(0).equals("0.0.0.0/0");
  }

  private void validateProperties() throws ValidationErrorException {
    // Can't specify cidr and destination security group
    if (!Strings.isNullOrEmpty(properties.getCidrIp()) && !Strings.isNullOrEmpty(properties.getDestinationSecurityGroupId())) {
      throw new ValidationErrorException("Both CidrIp and DestinationSecurityGroup cannot be specified");
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
    DescribeSecurityGroupsType describeSecurityGroupsType = new DescribeSecurityGroupsType();
    describeSecurityGroupsType.setEffectiveUserId(info.getEffectiveUserId());
    describeSecurityGroupsType.setSecurityGroupIdSet(Lists.newArrayList(properties.getGroupId()));
    DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType =
      AsyncRequests.<DescribeSecurityGroupsType, DescribeSecurityGroupsResponseType>sendSync(configuration, describeSecurityGroupsType);
    ArrayList<SecurityGroupItemType> securityGroupItemTypeArrayList = describeSecurityGroupsResponseType.getSecurityGroupInfo();
    if (securityGroupItemTypeArrayList == null || securityGroupItemTypeArrayList.isEmpty()) {
      return; // no group
    }

    RevokeSecurityGroupEgressType revokeSecurityGroupEgressType = new RevokeSecurityGroupEgressType();
    revokeSecurityGroupEgressType.setEffectiveUserId(getResourceInfo().getEffectiveUserId());
    revokeSecurityGroupEgressType.setGroupId(properties.getGroupId());
    IpPermissionType ipPermissionType = new IpPermissionType(
      properties.getIpProtocol(),
      properties.getFromPort(),
      properties.getToPort()
    );
    if (!Strings.isNullOrEmpty(properties.getCidrIp())) {
      ipPermissionType.setCidrIpRanges(Lists.newArrayList(properties.getCidrIp()));
    }
    if (!Strings.isNullOrEmpty(properties.getDestinationSecurityGroupId())) {
      // Generally no need for DestinationSecurityGroupOwnerId if DestinationSecurityGroupId is set, but pass it along if set
      ipPermissionType.setGroups(Lists.newArrayList(new UserIdGroupPairType(null, null, properties.getDestinationSecurityGroupId())));
    }
    revokeSecurityGroupEgressType.setIpPermissions(Lists.newArrayList(ipPermissionType));
    RevokeSecurityGroupEgressResponseType revokeSecurityGroupEgressResponseType = AsyncRequests.<RevokeSecurityGroupEgressType, RevokeSecurityGroupEgressResponseType> sendSync(configuration, revokeSecurityGroupEgressType);
    // do one last check, if there and no egress rules, re-add default rule
    DescribeSecurityGroupsType describeSecurityGroupsType2 = new DescribeSecurityGroupsType();
    describeSecurityGroupsType2.setEffectiveUserId(info.getEffectiveUserId());
    describeSecurityGroupsType2.setSecurityGroupIdSet(Lists.newArrayList(properties.getGroupId()));
    DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType2 =
      AsyncRequests.<DescribeSecurityGroupsType, DescribeSecurityGroupsResponseType>sendSync(configuration, describeSecurityGroupsType);
    ArrayList<SecurityGroupItemType> securityGroupItemTypeArrayList2 = describeSecurityGroupsResponseType2.getSecurityGroupInfo();
    if (securityGroupItemTypeArrayList2 == null || securityGroupItemTypeArrayList2.isEmpty()) {
      return; // no group
    }
    if (securityGroupItemTypeArrayList2.get(0).getIpPermissionsEgress() == null || securityGroupItemTypeArrayList2.get(0).getIpPermissionsEgress().isEmpty()) {
      AuthorizeSecurityGroupEgressType authorizeSecurityGroupEgressType = new AuthorizeSecurityGroupEgressType();
      authorizeSecurityGroupEgressType.setEffectiveUserId(getResourceInfo().getEffectiveUserId());
      authorizeSecurityGroupEgressType.setGroupId(properties.getGroupId());
      authorizeSecurityGroupEgressType.setIpPermissions(Lists.newArrayList(DEFAULT_EGRESS_RULE()));
      AuthorizeSecurityGroupEgressResponseType authorizeSecurityGroupIngressResponseType = AsyncRequests.<AuthorizeSecurityGroupEgressType, AuthorizeSecurityGroupEgressResponseType> sendSync(configuration, authorizeSecurityGroupEgressType);
    }

  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}


