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


import com.eucalyptus.auth.euare.CreateRoleResponseType;
import com.eucalyptus.auth.euare.CreateRoleType;
import com.eucalyptus.auth.euare.DeleteRolePolicyResponseType;
import com.eucalyptus.auth.euare.DeleteRolePolicyType;
import com.eucalyptus.auth.euare.DeleteRoleResponseType;
import com.eucalyptus.auth.euare.DeleteRoleType;
import com.eucalyptus.auth.euare.GroupType;
import com.eucalyptus.auth.euare.ListGroupsResponseType;
import com.eucalyptus.auth.euare.ListGroupsType;
import com.eucalyptus.auth.euare.ListRolesResponseType;
import com.eucalyptus.auth.euare.ListRolesType;
import com.eucalyptus.auth.euare.PutRolePolicyResponseType;
import com.eucalyptus.auth.euare.PutRolePolicyType;
import com.eucalyptus.auth.euare.RoleType;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSIAMRoleResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSIAMRoleProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EmbeddedIAMPolicy;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSIAMRoleResourceAction extends ResourceAction {

  private AWSIAMRoleProperties properties = new AWSIAMRoleProperties();
  private AWSIAMRoleResourceInfo info = new AWSIAMRoleResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSIAMRoleProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSIAMRoleResourceInfo) resourceInfo;
  }

  @Override
  public int getNumCreateSteps() {
    return 2;
  }

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Euare.class);
    switch (stepNum) {
      case 0: // create role
        String roleName = getDefaultPhysicalResourceId();
        CreateRoleType createRoleType = new CreateRoleType();
        createRoleType.setEffectiveUserId(info.getEffectiveUserId());
        createRoleType.setRoleName(roleName);
        createRoleType.setPath(properties.getPath());
        createRoleType.setAssumeRolePolicyDocument(properties.getAssumeRolePolicyDocument().toString());
        CreateRoleResponseType createRoleResponseType = AsyncRequests.<CreateRoleType,CreateRoleResponseType> sendSync(configuration, createRoleType);
        String arn = createRoleResponseType.getCreateRoleResult().getRole().getArn();
        info.setPhysicalResourceId(roleName);
        info.setArn(JsonHelper.getStringFromJsonNode(new TextNode(arn)));
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      case 1: // add policies
        if (properties.getPolicies() != null) {
          for (EmbeddedIAMPolicy policy: properties.getPolicies()) {
            PutRolePolicyType putRolePolicyType = new PutRolePolicyType();
            putRolePolicyType.setRoleName(info.getPhysicalResourceId());
            putRolePolicyType.setPolicyName(policy.getPolicyName());
            putRolePolicyType.setPolicyDocument(policy.getPolicyDocument().toString());
            putRolePolicyType.setEffectiveUserId(info.getEffectiveUserId());
            AsyncRequests.<PutRolePolicyType,PutRolePolicyResponseType> sendSync(configuration, putRolePolicyType);
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
    ServiceConfiguration configuration = Topology.lookup(Euare.class);
    // if no role, bye...
    boolean seenAllRoles = false;
    boolean foundRole = false;
    String RoleMarker = null;
    while (!seenAllRoles && !foundRole) {
      ListRolesType listRolesType = new ListRolesType();
      listRolesType.setEffectiveUserId(info.getEffectiveUserId());
      if (RoleMarker != null) {
        listRolesType.setMarker(RoleMarker);
      }
      ListRolesResponseType listRolesResponseType = AsyncRequests.<ListRolesType,ListRolesResponseType> sendSync(configuration, listRolesType);
      if (listRolesResponseType.getListRolesResult().getIsTruncated() == Boolean.TRUE) {
        RoleMarker = listRolesResponseType.getListRolesResult().getMarker();
      } else {
        seenAllRoles = true;
      }
      if (listRolesResponseType.getListRolesResult().getRoles() != null && listRolesResponseType.getListRolesResult().getRoles().getMember() != null) {
        for (RoleType roleType: listRolesResponseType.getListRolesResult().getRoles().getMember()) {
          if (roleType.getRoleName().equals(info.getPhysicalResourceId())) {
            foundRole = true;
            break;
          }
        }
      }
    }
    if (!foundRole) return;
    // remove all policies added by us.  (Note: this could cause issues if an admin added some, but we delete what we create)
    // Note: deleting a non-existing policy doesn't do anything so we just delete them all...
    if (properties.getPolicies() != null) {
      for (EmbeddedIAMPolicy policy: properties.getPolicies()) {
        DeleteRolePolicyType deleteRolePolicyType = new DeleteRolePolicyType();
        deleteRolePolicyType.setRoleName(info.getPhysicalResourceId());
        deleteRolePolicyType.setPolicyName(policy.getPolicyName());
        deleteRolePolicyType.setEffectiveUserId(info.getEffectiveUserId());
        AsyncRequests.<DeleteRolePolicyType,DeleteRolePolicyResponseType> sendSync(configuration, deleteRolePolicyType);
      }
    }
    DeleteRoleType deleteRoleType = new DeleteRoleType();
    deleteRoleType.setRoleName(info.getPhysicalResourceId());
    deleteRoleType.setEffectiveUserId(info.getEffectiveUserId());
    AsyncRequests.<DeleteRoleType,DeleteRoleResponseType> sendSync(configuration, deleteRoleType);
  }


  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}


