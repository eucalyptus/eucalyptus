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


import com.eucalyptus.auth.euare.AccessKeyMetadataType;
import com.eucalyptus.auth.euare.CreateAccessKeyResponseType;
import com.eucalyptus.auth.euare.CreateAccessKeyType;
import com.eucalyptus.auth.euare.DeleteAccessKeyResponseType;
import com.eucalyptus.auth.euare.DeleteAccessKeyType;
import com.eucalyptus.auth.euare.ListAccessKeysResponseType;
import com.eucalyptus.auth.euare.ListAccessKeysType;
import com.eucalyptus.auth.euare.ListUsersResponseType;
import com.eucalyptus.auth.euare.ListUsersType;
import com.eucalyptus.auth.euare.UpdateAccessKeyResponseType;
import com.eucalyptus.auth.euare.UpdateAccessKeyType;
import com.eucalyptus.auth.euare.UserType;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSIAMAccessKeyResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSIAMAccessKeyProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.compute.common.AllocateAddressResponseType;
import com.eucalyptus.compute.common.AllocateAddressType;
import com.eucalyptus.compute.common.AssociateAddressResponseType;
import com.eucalyptus.compute.common.AssociateAddressType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSIAMAccessKeyResourceAction extends ResourceAction {

  private AWSIAMAccessKeyProperties properties = new AWSIAMAccessKeyProperties();
  private AWSIAMAccessKeyResourceInfo info = new AWSIAMAccessKeyResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSIAMAccessKeyProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSIAMAccessKeyResourceInfo) resourceInfo;
  }

  @Override
  public int getNumCreateSteps() {
    return 2;
  }

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Euare.class);
    if (properties.getStatus() == null) properties.setStatus("Active");
    switch (stepNum) {
      case 0: // create key
        if (!"Active".equals(properties.getStatus()) && !"Inactive".equals(properties.getStatus())) {
          throw new ValidationErrorException("Invalid status " + properties.getStatus());
        }
        CreateAccessKeyType createAccessKeyType = new CreateAccessKeyType();
        createAccessKeyType.setEffectiveUserId(info.getEffectiveUserId());
        createAccessKeyType.setUserName(properties.getUserName());
        CreateAccessKeyResponseType createAccessKeyResponseType = AsyncRequests.<CreateAccessKeyType,CreateAccessKeyResponseType> sendSync(configuration, createAccessKeyType);
        // access key id = physical resource id
        info.setPhysicalResourceId(createAccessKeyResponseType.getCreateAccessKeyResult().getAccessKey().getAccessKeyId());
        info.setSecretAccessKey(JsonHelper.getStringFromJsonNode(new TextNode(createAccessKeyResponseType.getCreateAccessKeyResult().getAccessKey().getSecretAccessKey())));
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      case 1: // set status
        UpdateAccessKeyType updateAccessKeyType = new UpdateAccessKeyType();
        updateAccessKeyType.setUserName(properties.getUserName());
        updateAccessKeyType.setAccessKeyId(info.getPhysicalResourceId());
        updateAccessKeyType.setStatus(properties.getStatus());
        updateAccessKeyType.setEffectiveUserId(info.getEffectiveUserId());
        AsyncRequests.<UpdateAccessKeyType,UpdateAccessKeyResponseType> sendSync(configuration, updateAccessKeyType);
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
    if (properties.getStatus() == null) properties.setStatus("Active");
    // if no user, return
    boolean seenAllUsers = false;
    boolean foundUser = false;
    String userMarker = null;
    while (!seenAllUsers && !foundUser) {
      ListUsersType listUsersType = new ListUsersType();
      listUsersType.setEffectiveUserId(info.getEffectiveUserId());
      if (userMarker != null) {
        listUsersType.setMarker(userMarker);
      }
      ListUsersResponseType listUsersResponseType = AsyncRequests.<ListUsersType,ListUsersResponseType> sendSync(configuration, listUsersType);
      if (listUsersResponseType.getListUsersResult().getIsTruncated() == Boolean.TRUE) {
        userMarker = listUsersResponseType.getListUsersResult().getMarker();
      } else {
        seenAllUsers = true;
      }
      if (listUsersResponseType.getListUsersResult().getUsers() != null && listUsersResponseType.getListUsersResult().getUsers().getMemberList() != null) {
      for (UserType userType: listUsersResponseType.getListUsersResult().getUsers().getMemberList()) {
        if (userType.getUserName().equals(properties.getUserName())) {
          foundUser = true;
          break;
        }
      }
      }
    }
    if (!foundUser) return;

    boolean seenAllAccessKeys = false;
    boolean foundAccessKey = false;
    String accessKeyMarker = null;
    while (!seenAllAccessKeys && !foundAccessKey) {
      ListAccessKeysType listAccessKeysType = new ListAccessKeysType();
      listAccessKeysType.setUserName(properties.getUserName());
      listAccessKeysType.setEffectiveUserId(info.getEffectiveUserId());
      if (accessKeyMarker != null) {
        listAccessKeysType.setMarker(accessKeyMarker);
      }
      ListAccessKeysResponseType listAccessKeysResponseType = AsyncRequests.<ListAccessKeysType,ListAccessKeysResponseType> sendSync(configuration, listAccessKeysType);
      if (listAccessKeysResponseType.getListAccessKeysResult().getIsTruncated() == Boolean.TRUE) {
        accessKeyMarker = listAccessKeysResponseType.getListAccessKeysResult().getMarker();
      } else {
        seenAllAccessKeys = true;
      }
      if (listAccessKeysResponseType.getListAccessKeysResult().getAccessKeyMetadata() != null && listAccessKeysResponseType.getListAccessKeysResult().getAccessKeyMetadata().getMemberList() != null) {
        for (AccessKeyMetadataType accessKeyMetadataType: listAccessKeysResponseType.getListAccessKeysResult().getAccessKeyMetadata().getMemberList()) {
          if (accessKeyMetadataType.getAccessKeyId().equals(info.getPhysicalResourceId())) {
            foundAccessKey = true;
            break;
          }
        }
      }
    }
    if (!foundAccessKey) return;
    DeleteAccessKeyType deleteAccessKeyType = new DeleteAccessKeyType();
    deleteAccessKeyType.setUserName(properties.getUserName());
    deleteAccessKeyType.setAccessKeyId(info.getPhysicalResourceId());
    deleteAccessKeyType.setEffectiveUserId(info.getEffectiveUserId());
    AsyncRequests.<DeleteAccessKeyType,DeleteAccessKeyResponseType> sendSync(configuration, deleteAccessKeyType);
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}


