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
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;

import javax.annotation.Nullable;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSIAMAccessKeyResourceAction extends StepBasedResourceAction {

  private AWSIAMAccessKeyProperties properties = new AWSIAMAccessKeyProperties();
  private AWSIAMAccessKeyResourceInfo info = new AWSIAMAccessKeyResourceInfo();

  public AWSIAMAccessKeyResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), null, null, null);
  }

  private enum CreateSteps implements Step {
    CREATE_KEY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMAccessKeyResourceAction action = (AWSIAMAccessKeyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.properties.getStatus() == null) action.properties.setStatus("Active");
        if (!"Active".equals(action.properties.getStatus()) && !"Inactive".equals(action.properties.getStatus())) {
          throw new ValidationErrorException("Invalid status " + action.properties.getStatus());
        }
        CreateAccessKeyType createAccessKeyType = MessageHelper.createMessage(CreateAccessKeyType.class, action.info.getEffectiveUserId());
        createAccessKeyType.setUserName(action.properties.getUserName());
        CreateAccessKeyResponseType createAccessKeyResponseType = AsyncRequests.<CreateAccessKeyType,CreateAccessKeyResponseType> sendSync(configuration, createAccessKeyType);
        // access key id = physical resource id
        action.info.setPhysicalResourceId(createAccessKeyResponseType.getCreateAccessKeyResult().getAccessKey().getAccessKeyId());
        action.info.setSecretAccessKey(JsonHelper.getStringFromJsonNode(new TextNode(createAccessKeyResponseType.getCreateAccessKeyResult().getAccessKey().getSecretAccessKey())));
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    SET_STATUS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMAccessKeyResourceAction action = (AWSIAMAccessKeyResourceAction) resourceAction;
        if (action.properties.getStatus() == null) action.properties.setStatus("Active");
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        UpdateAccessKeyType updateAccessKeyType = MessageHelper.createMessage(UpdateAccessKeyType.class, action.info.getEffectiveUserId());
        updateAccessKeyType.setUserName(action.properties.getUserName());
        updateAccessKeyType.setAccessKeyId(action.info.getPhysicalResourceId());
        updateAccessKeyType.setStatus(action.properties.getStatus());
        AsyncRequests.<UpdateAccessKeyType,UpdateAccessKeyResponseType> sendSync(configuration, updateAccessKeyType);
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
    DELETE_KEY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMAccessKeyResourceAction action = (AWSIAMAccessKeyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.info.getPhysicalResourceId() == null) return action;

        if (action.properties.getStatus() == null) action.properties.setStatus("Active");
        // if no user, return
        boolean seenAllUsers = false;
        boolean foundUser = false;
        String userMarker = null;
        while (!seenAllUsers && !foundUser) {
          ListUsersType listUsersType = MessageHelper.createMessage(ListUsersType.class, action.info.getEffectiveUserId());
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
              if (userType.getUserName().equals(action.properties.getUserName())) {
                foundUser = true;
                break;
              }
            }
          }
        }
        if (!foundUser) return action;

        boolean seenAllAccessKeys = false;
        boolean foundAccessKey = false;
        String accessKeyMarker = null;
        while (!seenAllAccessKeys && !foundAccessKey) {
          ListAccessKeysType listAccessKeysType = MessageHelper.createMessage(ListAccessKeysType.class, action.info.getEffectiveUserId());
          listAccessKeysType.setUserName(action.properties.getUserName());
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
              if (accessKeyMetadataType.getAccessKeyId().equals(action.info.getPhysicalResourceId())) {
                foundAccessKey = true;
                break;
              }
            }
          }
        }
        if (!foundAccessKey) return action;
        DeleteAccessKeyType deleteAccessKeyType = MessageHelper.createMessage(DeleteAccessKeyType.class, action.info.getEffectiveUserId());
        deleteAccessKeyType.setUserName(action.properties.getUserName());
        deleteAccessKeyType.setAccessKeyId(action.info.getPhysicalResourceId());
        AsyncRequests.<DeleteAccessKeyType,DeleteAccessKeyResponseType> sendSync(configuration, deleteAccessKeyType);
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



}


