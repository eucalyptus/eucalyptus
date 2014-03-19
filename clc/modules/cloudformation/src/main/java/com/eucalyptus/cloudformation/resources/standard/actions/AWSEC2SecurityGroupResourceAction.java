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


import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2SecurityGroupResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2SecurityGroupProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2SecurityGroupRule;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.AuthorizeSecurityGroupIngressResponseType;
import edu.ucsb.eucalyptus.msgs.AuthorizeSecurityGroupIngressType;
import edu.ucsb.eucalyptus.msgs.CreateSecurityGroupResponseType;
import edu.ucsb.eucalyptus.msgs.CreateSecurityGroupType;
import edu.ucsb.eucalyptus.msgs.DeleteSecurityGroupResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteSecurityGroupType;
import edu.ucsb.eucalyptus.msgs.DescribeSecurityGroupsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeSecurityGroupsType;
import edu.ucsb.eucalyptus.msgs.IpPermissionType;
import edu.ucsb.eucalyptus.msgs.RevokeSecurityGroupIngressResponseType;
import edu.ucsb.eucalyptus.msgs.RevokeSecurityGroupIngressType;
import edu.ucsb.eucalyptus.msgs.SecurityGroupItemType;
import edu.ucsb.eucalyptus.msgs.UserIdGroupPairType;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2SecurityGroupResourceAction extends ResourceAction {

  private AWSEC2SecurityGroupProperties properties = new AWSEC2SecurityGroupProperties();
  private AWSEC2SecurityGroupResourceInfo info = new AWSEC2SecurityGroupResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSEC2SecurityGroupProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2SecurityGroupResourceInfo) resourceInfo;
  }

  @Override
  public void create() throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Eucalyptus.class);
    CreateSecurityGroupType createSecurityGroupType = new CreateSecurityGroupType();
    if (properties.getGroupDescription() != null && !properties.getGroupDescription().isEmpty()) {
      createSecurityGroupType.setGroupDescription(properties.getGroupDescription());
    }
    String groupName = getStackEntity().getStackName() + "-" + getResourceInfo().getLogicalResourceId() + "-" +
      Crypto.generateAlphanumericId(13,""); // seems to be what AWS does
    createSecurityGroupType.setGroupName(groupName);
    createSecurityGroupType.setEffectiveUserId(getResourceInfo().getEffectiveUserId());
    CreateSecurityGroupResponseType createSecurityGroupResponseType = AsyncRequests.<CreateSecurityGroupType,CreateSecurityGroupResponseType> sendSync(configuration, createSecurityGroupType);
    String groupId = createSecurityGroupResponseType.getGroupId();
    if (properties.getSecurityGroupIngress() != null) {
      for (EC2SecurityGroupRule ec2SecurityGroupRule: properties.getSecurityGroupIngress()) {
        AuthorizeSecurityGroupIngressType authorizeSecurityGroupIngressType = new AuthorizeSecurityGroupIngressType();
        authorizeSecurityGroupIngressType.setEffectiveUserId(getResourceInfo().getEffectiveUserId());
        if (groupName != null && !groupName.isEmpty()) {
          authorizeSecurityGroupIngressType.setGroupName(groupName);
        }
        if (groupId != null && !groupId.isEmpty()) {
          authorizeSecurityGroupIngressType.setGroupId(groupId);
        }
        int fromPort = -1;
        String ipProtocol = ec2SecurityGroupRule.getIpProtocol();
        try {
          fromPort = Integer.parseInt(ec2SecurityGroupRule.getFromPort());
        } catch (Exception ignore) {}
        int toPort = -1;
        try {
          toPort = Integer.parseInt(ec2SecurityGroupRule.getToPort());
        } catch (Exception ignore) {}
        String sourceSecurityGroupName = ec2SecurityGroupRule.getSourceSecurityGroupName();
        String sourceSecurityGroupOwnerId = ec2SecurityGroupRule.getSourceSecurityGroupOwnerId();
        if (sourceSecurityGroupOwnerId == null && sourceSecurityGroupName != null) {
          sourceSecurityGroupOwnerId = stackEntity.getAccountId();
        }
        String cidrIp = ec2SecurityGroupRule.getCidrIp();
        IpPermissionType ipPermissionType = new IpPermissionType(ipProtocol, fromPort, toPort);
        if (sourceSecurityGroupName != null) {
          ipPermissionType.setGroups(Lists.newArrayList(new UserIdGroupPairType(sourceSecurityGroupOwnerId, sourceSecurityGroupName, null)));
        }
        if (cidrIp != null) {
          ipPermissionType.setCidrIpRanges(Lists.newArrayList(cidrIp));
        }
        authorizeSecurityGroupIngressType.setIpPermissions(Lists.newArrayList(ipPermissionType));
        AuthorizeSecurityGroupIngressResponseType authorizeSecurityGroupIngressResponseType = AsyncRequests.<AuthorizeSecurityGroupIngressType, AuthorizeSecurityGroupIngressResponseType> sendSync(configuration, authorizeSecurityGroupIngressType);
      }
    }
    info.setGroupId(groupId);
    info.setPhysicalResourceId(groupName);
    info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
  }
  @Override
  public void delete() throws Exception {
    if (info.getPhysicalResourceId() == null) return;
    ServiceConfiguration configuration = Topology.lookup(Eucalyptus.class);

    String groupName = info.getPhysicalResourceId();
    String groupId = info.getGroupId();
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
    if (!nameToIdMap.containsKey(groupName) || !nameToIdMap.get(groupName).equals(groupId)) return; // different (or nonexistant) group

    if (properties.getSecurityGroupIngress() != null) {
      for (EC2SecurityGroupRule ec2SecurityGroupRule: properties.getSecurityGroupIngress()) {
        RevokeSecurityGroupIngressType revokeSecurityGroupIngressType = new RevokeSecurityGroupIngressType();
        revokeSecurityGroupIngressType.setEffectiveUserId(getResourceInfo().getEffectiveUserId());
        if (groupName != null && !groupName.isEmpty()) {
          revokeSecurityGroupIngressType.setGroupName(groupName);
        }
        if (groupId != null && !groupId.isEmpty()) {
          revokeSecurityGroupIngressType.setGroupId(groupId);
        }
        int fromPort = -1;
        String ipProtocol = ec2SecurityGroupRule.getIpProtocol();
        try {
          fromPort = Integer.parseInt(ec2SecurityGroupRule.getFromPort());
        } catch (Exception ignore) {}
        int toPort = -1;
        try {
          toPort = Integer.parseInt(ec2SecurityGroupRule.getToPort());
        } catch (Exception ignore) {}
        String sourceSecurityGroupName = ec2SecurityGroupRule.getSourceSecurityGroupName();
        String sourceSecurityGroupOwnerId = ec2SecurityGroupRule.getSourceSecurityGroupOwnerId();
        if (sourceSecurityGroupOwnerId == null && sourceSecurityGroupName != null) {
          sourceSecurityGroupOwnerId = stackEntity.getAccountId();
        }
        String cidrIp = ec2SecurityGroupRule.getCidrIp();
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
    }
    DeleteSecurityGroupType deleteSecurityGroupType = new DeleteSecurityGroupType();
    deleteSecurityGroupType.setGroupName(groupName);
    deleteSecurityGroupType.setEffectiveUserId(getResourceInfo().getEffectiveUserId());
    DeleteSecurityGroupResponseType deleteSecurityGroupResponseType = AsyncRequests.<DeleteSecurityGroupType,DeleteSecurityGroupResponseType> sendSync(configuration, deleteSecurityGroupType);
  }

  @Override
  public void rollback() throws Exception {
    delete();
  }

}


