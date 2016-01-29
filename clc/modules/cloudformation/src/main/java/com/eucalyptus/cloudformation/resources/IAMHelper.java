/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudformation.resources;

import com.eucalyptus.auth.euare.AccessKeyMetadataType;
import com.eucalyptus.auth.euare.GroupType;
import com.eucalyptus.auth.euare.ListAccessKeysResponseType;
import com.eucalyptus.auth.euare.ListAccessKeysType;
import com.eucalyptus.auth.euare.ListGroupsResponseType;
import com.eucalyptus.auth.euare.ListGroupsType;
import com.eucalyptus.auth.euare.ListUsersResponseType;
import com.eucalyptus.auth.euare.ListUsersType;
import com.eucalyptus.auth.euare.UserType;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EmbeddedIAMPolicy;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.util.async.AsyncRequests;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

/**
 * Created by ethomas on 1/28/16.
 */
public class IAMHelper {
  public static UserType getUser(ServiceConfiguration configuration, String userName, String effectiveUserId) throws Exception {
    UserType retVal = null;
    // if no user, return
    boolean seenAllUsers = false;
    String userMarker = null;
    while (!seenAllUsers && retVal == null) {
      ListUsersType listUsersType = MessageHelper.createMessage(ListUsersType.class, effectiveUserId);
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
          if (userType.getUserName().equals(userName)) {
            retVal = userType;
            break;
          }
        }
      }
    }
    return retVal;
  }

  public static boolean userExists(ServiceConfiguration configuration, String userName, String effectiveUserId) throws Exception {
    return getUser(configuration, userName, effectiveUserId) != null;
  }

  public static AccessKeyMetadataType getAccessKey(ServiceConfiguration configuration, String accessKeyId, String userName, String effectiveUserId) throws Exception {
    AccessKeyMetadataType retVal = null;
    boolean seenAllAccessKeys = false;
    String accessKeyMarker = null;
    while (!seenAllAccessKeys && (retVal == null)) {
      ListAccessKeysType listAccessKeysType = MessageHelper.createMessage(ListAccessKeysType.class, effectiveUserId);
      listAccessKeysType.setUserName(userName);
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
          if (accessKeyMetadataType.getAccessKeyId().equals(accessKeyId)) {
            retVal = accessKeyMetadataType;
            break;
          }
        }
      }
    }
    return retVal;
  }

  public static boolean accessKeyExists(ServiceConfiguration configuration, String accessKeyId, String userName, String effectiveUserId) throws Exception {
    return getAccessKey(configuration, accessKeyId, userName, effectiveUserId) != null;
  }

  public static boolean groupExists(ServiceConfiguration configuration, String groupName, String effectiveUserId) throws Exception {
    return (getGroup(configuration, groupName, effectiveUserId) != null);
  }

  public static GroupType getGroup(ServiceConfiguration configuration, String groupName, String effectiveUserId) throws Exception {
    GroupType retVal = null;
    boolean seenAllGroups = false;
    String groupMarker = null;
    while (!seenAllGroups && retVal == null) {
      ListGroupsType listGroupsType = MessageHelper.createMessage(ListGroupsType.class, effectiveUserId);
      if (groupMarker != null) {
        listGroupsType.setMarker(groupMarker);
      }
      ListGroupsResponseType listGroupsResponseType = AsyncRequests.<ListGroupsType,ListGroupsResponseType> sendSync(configuration, listGroupsType);
      if (listGroupsResponseType.getListGroupsResult().getIsTruncated() == Boolean.TRUE) {
        groupMarker = listGroupsResponseType.getListGroupsResult().getMarker();
      } else {
        seenAllGroups = true;
      }
      if (listGroupsResponseType.getListGroupsResult().getGroups() != null && listGroupsResponseType.getListGroupsResult().getGroups().getMemberList() != null) {
        for (GroupType groupType: listGroupsResponseType.getListGroupsResult().getGroups().getMemberList()) {
          if (groupType.getGroupName().equals(groupName)) {
            retVal = groupType;
            break;
          }
        }
      }
    }
    return retVal;
  }

  public static Set<String> getPolicyNames(List<EmbeddedIAMPolicy> policies) {
    Set<String> policyNames = Sets.newLinkedHashSet();
    if (policies != null) {
      for (EmbeddedIAMPolicy policy : policies) {
        policyNames.add(policy.getPolicyName());
      }
    }
    return policyNames;
  }
}
