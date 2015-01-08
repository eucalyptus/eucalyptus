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
    switch (stepNum) {
      case 0:
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // May need to pass in both groupId and groupName (VPC may be different) so trying to get both...
        // Is there a better way?
        DescribeSecurityGroupsType describeSecurityGroupsType = new DescribeSecurityGroupsType();
        describeSecurityGroupsType.setEffectiveUserId(info.getEffectiveUserId());
        DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType =
          AsyncRequests.<DescribeSecurityGroupsType,DescribeSecurityGroupsResponseType> sendSync(configuration, describeSecurityGroupsType);
        ArrayList<SecurityGroupItemType> securityGroupItemTypeArrayList = describeSecurityGroupsResponseType.getSecurityGroupInfo();
        Map<String, String> nameToIdMap = Maps.newHashMap();
        if (securityGroupItemTypeArrayList != null) {
          for (SecurityGroupItemType securityGroupItemType: securityGroupItemTypeArrayList) {
            nameToIdMap.put(securityGroupItemType.getGroupName(), securityGroupItemType.getGroupId());
          }
        }
        AuthorizeSecurityGroupIngressType authorizeSecurityGroupIngressType = new AuthorizeSecurityGroupIngressType();
        authorizeSecurityGroupIngressType.setEffectiveUserId(getResourceInfo().getEffectiveUserId());

        String groupName = null;
        if (properties.getGroupName() != null && !properties.getGroupName().isEmpty()) {
          groupName = properties.getGroupName();
        }
        String groupId = null;
        if (properties.getGroupId() != null && !properties.getGroupId().isEmpty()) {
          groupId = properties.getGroupId();
        }
        if (groupName != null && !nameToIdMap.containsKey(groupName)) {
          throw new ValidationErrorException("No such group with name " + groupName);
        }
        if (groupId != null && !nameToIdMap.containsValue(groupId)) {
          throw new ValidationErrorException("No such group with id " + groupId);
        }
        if (groupId == null && groupName == null) {
          throw new ValidationErrorException("GroupId or GroupName is required for AWS::EC2::SecurityGroupIngress");
        }
        if (groupName == null) {
          for (String key: nameToIdMap.keySet()) {
            if (nameToIdMap.get(key).equals(groupId)) {
              groupName = key;
              break;
            }
          }
        }
        if (groupId == null) {
          groupId = nameToIdMap.get(groupName);
        }
        if (!nameToIdMap.get(groupName).equals(groupId)) {
          throw new ValidationErrorException("GroupId ("+groupId+") and " +
            "GroupName ("+groupName+") do not match the same group in AWS::EC2::SecurityGroupIngress");
        }
        authorizeSecurityGroupIngressType.setGroupName(groupName);
        authorizeSecurityGroupIngressType.setGroupId(groupId);
        int fromPort = -1;
        String ipProtocol = properties.getIpProtocol();
        try {
          fromPort = Integer.parseInt(properties.getFromPort());
        } catch (Exception ignore) {}
        int toPort = -1;
        try {
          toPort = Integer.parseInt(properties.getToPort());
        } catch (Exception ignore) {}
        String sourceSecurityGroupName = properties.getSourceSecurityGroupName();
        String sourceSecurityGroupOwnerId = properties.getSourceSecurityGroupOwnerId();
        if (sourceSecurityGroupOwnerId == null && sourceSecurityGroupName != null) {
          sourceSecurityGroupOwnerId = stackEntity.getAccountId();
        }
        String cidrIp = properties.getCidrIp();
        IpPermissionType ipPermissionType = new IpPermissionType(ipProtocol, fromPort, toPort);
        if (sourceSecurityGroupName != null) {
          ipPermissionType.setGroups(Lists.newArrayList(new UserIdGroupPairType(sourceSecurityGroupOwnerId, sourceSecurityGroupName, null)));
        }
        if (cidrIp != null) {
          ipPermissionType.setCidrIpRanges(Lists.newArrayList(cidrIp));
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
    // May need to pass in both groupId and groupName (VPC may be different) so trying to get both...
    // Is there a better way?
    DescribeSecurityGroupsType describeSecurityGroupsType = new DescribeSecurityGroupsType();
    describeSecurityGroupsType.setEffectiveUserId(info.getEffectiveUserId());
    DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType =
      AsyncRequests.<DescribeSecurityGroupsType,DescribeSecurityGroupsResponseType> sendSync(configuration, describeSecurityGroupsType);
    ArrayList<SecurityGroupItemType> securityGroupItemTypeArrayList = describeSecurityGroupsResponseType.getSecurityGroupInfo();
    Map<String, String> nameToIdMap = Maps.newHashMap();
    if (securityGroupItemTypeArrayList != null) {
      for (SecurityGroupItemType securityGroupItemType: securityGroupItemTypeArrayList) {
        nameToIdMap.put(securityGroupItemType.getGroupName(), securityGroupItemType.getGroupId());
      }
    }
    RevokeSecurityGroupIngressType revokeSecurityGroupIngressType = new RevokeSecurityGroupIngressType();
    revokeSecurityGroupIngressType.setEffectiveUserId(getResourceInfo().getEffectiveUserId());

    String groupName = null;
    if (properties.getGroupName() != null && !properties.getGroupName().isEmpty()) {
      groupName = properties.getGroupName();
    }
    String groupId = null;
    if (properties.getGroupId() != null && !properties.getGroupId().isEmpty()) {
      groupId = properties.getGroupId();
    }
    if (groupName != null && !nameToIdMap.containsKey(groupName)) {
      return; // no such group
    }
    if (groupId != null && !nameToIdMap.containsValue(groupId)) {
      return; // no such group
    }
    if (groupId == null && groupName == null) {
      return; // no such group
    }
    if (groupName == null) {
      for (String key: nameToIdMap.keySet()) {
        if (nameToIdMap.get(key).equals(groupId)) {
          groupName = key;
          break;
        }
      }
    }
    if (groupId == null) {
      groupId = nameToIdMap.get(groupName);
    }
    if (!nameToIdMap.get(groupName).equals(groupId)) {
      return; // no such group
    }
    revokeSecurityGroupIngressType.setGroupName(groupName);
    revokeSecurityGroupIngressType.setGroupId(groupId);
    revokeSecurityGroupIngressType.setGroupName(groupName);
    revokeSecurityGroupIngressType.setGroupId(groupId);
    int fromPort = -1;
    String ipProtocol = properties.getIpProtocol();
    try {
      fromPort = Integer.parseInt(properties.getFromPort());
    } catch (Exception ignore) {}
    int toPort = -1;
    try {
      toPort = Integer.parseInt(properties.getToPort());
    } catch (Exception ignore) {}
    String sourceSecurityGroupName = properties.getSourceSecurityGroupName();
    String sourceSecurityGroupOwnerId = properties.getSourceSecurityGroupOwnerId();
    if (sourceSecurityGroupOwnerId == null && sourceSecurityGroupName != null) {
      sourceSecurityGroupOwnerId = stackEntity.getAccountId();
    }
    String cidrIp = properties.getCidrIp();
    IpPermissionType ipPermissionType = new IpPermissionType(ipProtocol, fromPort, toPort);
    if (sourceSecurityGroupName != null) {
      ipPermissionType.setGroups(Lists.newArrayList(new UserIdGroupPairType(sourceSecurityGroupOwnerId, sourceSecurityGroupName, null)));
    }
    if (cidrIp != null) {
      ipPermissionType.setCidrIpRanges(Lists.newArrayList(cidrIp));
    }
    revokeSecurityGroupIngressType.setIpPermissions(Lists.newArrayList(ipPermissionType));
    RevokeSecurityGroupIngressResponseType revokeSecurityGroupIngressResponseType = AsyncRequests.<RevokeSecurityGroupIngressType, RevokeSecurityGroupIngressResponseType> sendSync(configuration, revokeSecurityGroupIngressType);
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}


