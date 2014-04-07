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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2SecurityGroupResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2SecurityGroupProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2SecurityGroupRule;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AllocateAddressResponseType;
import com.eucalyptus.compute.common.AllocateAddressType;
import com.eucalyptus.compute.common.AssociateAddressResponseType;
import com.eucalyptus.compute.common.AssociateAddressType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.eucalyptus.compute.common.AuthorizeSecurityGroupIngressResponseType;
import com.eucalyptus.compute.common.AuthorizeSecurityGroupIngressType;
import com.eucalyptus.compute.common.CreateSecurityGroupResponseType;
import com.eucalyptus.compute.common.CreateSecurityGroupType;
import com.eucalyptus.compute.common.DeleteSecurityGroupResponseType;
import com.eucalyptus.compute.common.DeleteSecurityGroupType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsResponseType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsType;
import com.eucalyptus.compute.common.IpPermissionType;
import com.eucalyptus.compute.common.RevokeSecurityGroupIngressResponseType;
import com.eucalyptus.compute.common.RevokeSecurityGroupIngressType;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.UserIdGroupPairType;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2SecurityGroupResourceAction extends ResourceAction {
  private static final Logger LOG = Logger.getLogger(AWSEC2SecurityGroupResourceAction.class);
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
  public int getNumCreateSteps() {
    return 2;
  }

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    switch (stepNum) {
      case 0: // create security group
        CreateSecurityGroupType createSecurityGroupType = new CreateSecurityGroupType();
        if (properties.getGroupDescription() != null && !properties.getGroupDescription().isEmpty()) {
          createSecurityGroupType.setGroupDescription(properties.getGroupDescription());
        }
        String groupName = getDefaultPhysicalResourceId();
        createSecurityGroupType.setGroupName(groupName);
        createSecurityGroupType.setEffectiveUserId(getResourceInfo().getEffectiveUserId());
        CreateSecurityGroupResponseType createSecurityGroupResponseType = AsyncRequests.<CreateSecurityGroupType,CreateSecurityGroupResponseType> sendSync(configuration, createSecurityGroupType);
        String groupId = createSecurityGroupResponseType.getGroupId();
        info.setGroupId(groupId);
        info.setPhysicalResourceId(groupName);
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      case 1: // create ingress rules
        if (properties.getSecurityGroupIngress() != null) {
          for (EC2SecurityGroupRule ec2SecurityGroupRule: properties.getSecurityGroupIngress()) {
            AuthorizeSecurityGroupIngressType authorizeSecurityGroupIngressType = new AuthorizeSecurityGroupIngressType();
            authorizeSecurityGroupIngressType.setEffectiveUserId(getResourceInfo().getEffectiveUserId());
            if (info.getPhysicalResourceId() != null && !info.getPhysicalResourceId().isEmpty()) {
              authorizeSecurityGroupIngressType.setGroupName(info.getPhysicalResourceId());
            }
            if (info.getGroupId() != null && !info.getGroupId().isEmpty()) {
              authorizeSecurityGroupIngressType.setGroupId(info.getGroupId());
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
    Exception finalException = null;
    // Need to retry this as we may have instances terminating from an autoscaling group...
    for (int i=0;i<60;i++) { // sleeping for 5 seconds 60 times... (5 minutes)
      Thread.sleep(5000L);
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

      DeleteSecurityGroupType deleteSecurityGroupType = new DeleteSecurityGroupType();
      deleteSecurityGroupType.setGroupName(groupName);
      deleteSecurityGroupType.setEffectiveUserId(getResourceInfo().getEffectiveUserId());
      try {
        DeleteSecurityGroupResponseType deleteSecurityGroupResponseType = AsyncRequests.<DeleteSecurityGroupType,DeleteSecurityGroupResponseType> sendSync(configuration, deleteSecurityGroupType);
        return;
      } catch (Exception ex) {
        LOG.debug("Error deleting security group.  Will retry");
        finalException = ex;
      }
    }
    throw finalException;
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}


