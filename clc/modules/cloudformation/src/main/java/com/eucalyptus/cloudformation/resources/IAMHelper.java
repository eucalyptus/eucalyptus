/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudformation.resources;

import com.eucalyptus.auth.euare.common.msgs.AccessKeyMetadataType;
import com.eucalyptus.auth.euare.common.msgs.GetGroupResponseType;
import com.eucalyptus.auth.euare.common.msgs.GetGroupType;
import com.eucalyptus.auth.euare.common.msgs.GroupType;
import com.eucalyptus.auth.euare.common.msgs.InstanceProfileType;
import com.eucalyptus.auth.euare.common.msgs.ListAccessKeysResponseType;
import com.eucalyptus.auth.euare.common.msgs.ListAccessKeysType;
import com.eucalyptus.auth.euare.common.msgs.ListGroupsForUserResponseType;
import com.eucalyptus.auth.euare.common.msgs.ListGroupsForUserType;
import com.eucalyptus.auth.euare.common.msgs.ListGroupsResponseType;
import com.eucalyptus.auth.euare.common.msgs.ListGroupsType;
import com.eucalyptus.auth.euare.common.msgs.ListInstanceProfilesResponseType;
import com.eucalyptus.auth.euare.common.msgs.ListInstanceProfilesType;
import com.eucalyptus.auth.euare.common.msgs.ListRolesResponseType;
import com.eucalyptus.auth.euare.common.msgs.ListRolesType;
import com.eucalyptus.auth.euare.common.msgs.ListUsersResponseType;
import com.eucalyptus.auth.euare.common.msgs.ListUsersType;
import com.eucalyptus.auth.euare.common.msgs.RemoveUserFromGroupResponseType;
import com.eucalyptus.auth.euare.common.msgs.RemoveUserFromGroupType;
import com.eucalyptus.auth.euare.common.msgs.RoleType;
import com.eucalyptus.auth.euare.common.msgs.UserType;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EmbeddedIAMPolicy;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.util.async.AsyncRequests;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.HashSet;
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
      if (Boolean.TRUE.equals(listUsersResponseType.getListUsersResult().getIsTruncated())) {
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
      if (Boolean.TRUE.equals(listAccessKeysResponseType.getListAccessKeysResult().getIsTruncated())) {
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
      if (Boolean.TRUE.equals(listGroupsResponseType.getListGroupsResult().getIsTruncated())) {
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

  public static boolean instanceProfileExists(ServiceConfiguration configuration, String instanceProfileName, String effectiveUserId) throws Exception {
    return getInstanceProfile(configuration, instanceProfileName, effectiveUserId) != null;
  }

  public static InstanceProfileType getInstanceProfile(ServiceConfiguration configuration, String instanceProfileName, String effectiveUserId) throws Exception {
    InstanceProfileType retVal = null;
    boolean seenAllInstanceProfiles = false;
    String instanceProfileMarker = null;
    while (!seenAllInstanceProfiles && retVal == null) {
      ListInstanceProfilesType listInstanceProfilesType = MessageHelper.createMessage(ListInstanceProfilesType.class, effectiveUserId);
      if (instanceProfileMarker != null) {
        listInstanceProfilesType.setMarker(instanceProfileMarker);
      }
      ListInstanceProfilesResponseType listInstanceProfilesResponseType = AsyncRequests.<ListInstanceProfilesType,ListInstanceProfilesResponseType> sendSync(configuration, listInstanceProfilesType);
      if (Boolean.TRUE.equals(listInstanceProfilesResponseType.getListInstanceProfilesResult().getIsTruncated())) {
        instanceProfileMarker = listInstanceProfilesResponseType.getListInstanceProfilesResult().getMarker();
      } else {
        seenAllInstanceProfiles = true;
      }
      if (listInstanceProfilesResponseType.getListInstanceProfilesResult().getInstanceProfiles() != null && listInstanceProfilesResponseType.getListInstanceProfilesResult().getInstanceProfiles().getMember() != null) {
        for (InstanceProfileType instanceProfileType: listInstanceProfilesResponseType.getListInstanceProfilesResult().getInstanceProfiles().getMember()) {
          if (instanceProfileType.getInstanceProfileName().equals(instanceProfileName)) {
            retVal = instanceProfileType;
            break;
          }
        }
      }
    }
    return retVal;
  }

  public static List<String> getExistingGroups(ServiceConfiguration configuration, Set<String> passedInGroups, String effectiveUserId) throws Exception {
    List<String> realGroups = Lists.newArrayList();
    boolean seenAllGroups = false;
    String groupMarker = null;
    while (!seenAllGroups) {
      ListGroupsType listGroupsType = MessageHelper.createMessage(ListGroupsType.class, effectiveUserId);
      if (groupMarker != null) {
        listGroupsType.setMarker(groupMarker);
      }
      ListGroupsResponseType listGroupsResponseType = AsyncRequests.<ListGroupsType,ListGroupsResponseType> sendSync(configuration, listGroupsType);
      if (Boolean.TRUE.equals(listGroupsResponseType.getListGroupsResult().getIsTruncated())) {
        groupMarker = listGroupsResponseType.getListGroupsResult().getMarker();
      } else {
        seenAllGroups = true;
      }
      if (listGroupsResponseType.getListGroupsResult().getGroups() != null && listGroupsResponseType.getListGroupsResult().getGroups().getMemberList() != null) {
        for (GroupType groupType: listGroupsResponseType.getListGroupsResult().getGroups().getMemberList()) {
          if (passedInGroups.contains(groupType.getGroupName())) {
            realGroups.add(groupType.getGroupName());
          }
        }
      }
    }
    return realGroups;
  }

  public static List<String> getExistingUsers(ServiceConfiguration configuration, Set<String> passedInUsers, String effectiveUserId) throws Exception {
    List<String> realUsers = Lists.newArrayList();
    boolean seenAllUsers = false;
    String userMarker = null;
    while (!seenAllUsers) {
      ListUsersType listUsersType = MessageHelper.createMessage(ListUsersType.class, effectiveUserId);
      if (userMarker != null) {
        listUsersType.setMarker(userMarker);
      }
      ListUsersResponseType listUsersResponseType = AsyncRequests.<ListUsersType,ListUsersResponseType> sendSync(configuration, listUsersType);
      if (Boolean.TRUE.equals(listUsersResponseType.getListUsersResult().getIsTruncated())) {
        userMarker = listUsersResponseType.getListUsersResult().getMarker();
      } else {
        seenAllUsers = true;
      }
      if (listUsersResponseType.getListUsersResult().getUsers() != null && listUsersResponseType.getListUsersResult().getUsers().getMemberList() != null) {
        for (UserType userType: listUsersResponseType.getListUsersResult().getUsers().getMemberList()) {
          if (passedInUsers.contains(userType.getUserName())) {
            realUsers.add(userType.getUserName());
          }
        }
      }
    }
    return realUsers;
  }

  public static List<String> getExistingRoles(ServiceConfiguration configuration, Set<String> passedInRoles, String effectiveUserId) throws Exception {
    List<String> realRoles = Lists.newArrayList();
    boolean seenAllRoles = false;
    String roleMarker = null;
    while (!seenAllRoles) {
      ListRolesType listRolesType = MessageHelper.createMessage(ListRolesType.class, effectiveUserId);
      if (roleMarker != null) {
        listRolesType.setMarker(roleMarker);
      }
      ListRolesResponseType listRolesResponseType = AsyncRequests.<ListRolesType,ListRolesResponseType> sendSync(configuration, listRolesType);
      if (Boolean.TRUE.equals(listRolesResponseType.getListRolesResult().getIsTruncated())) {
        roleMarker = listRolesResponseType.getListRolesResult().getMarker();
      } else {
        seenAllRoles = true;
      }
      if (listRolesResponseType.getListRolesResult().getRoles() != null && listRolesResponseType.getListRolesResult().getRoles().getMember() != null) {
        for (RoleType roleType: listRolesResponseType.getListRolesResult().getRoles().getMember()) {
          if (passedInRoles.contains(roleType.getRoleName())) {
            realRoles.add(roleType.getRoleName());
          }
        }
      }
    }
    return realRoles;
  }

  public static boolean roleExists(ServiceConfiguration configuration, String roleName, String effectiveUserId) throws Exception {
    return getRole(configuration, roleName, effectiveUserId) != null;
  }

  private static RoleType getRole(ServiceConfiguration configuration, String roleName, String effectiveUserId) throws Exception {
    RoleType retVal = null;
    boolean seenAllRoles = false;
    String RoleMarker = null;
    while (!seenAllRoles && retVal == null) {
      ListRolesType listRolesType = MessageHelper.createMessage(ListRolesType.class, effectiveUserId);
      if (RoleMarker != null) {
        listRolesType.setMarker(RoleMarker);
      }
      ListRolesResponseType listRolesResponseType = AsyncRequests.<ListRolesType,ListRolesResponseType> sendSync(configuration, listRolesType);
      if (Boolean.TRUE.equals(listRolesResponseType.getListRolesResult().getIsTruncated())) {
        RoleMarker = listRolesResponseType.getListRolesResult().getMarker();
      } else {
        seenAllRoles = true;
      }
      if (listRolesResponseType.getListRolesResult().getRoles() != null && listRolesResponseType.getListRolesResult().getRoles().getMember() != null) {
        for (RoleType roleType: listRolesResponseType.getListRolesResult().getRoles().getMember()) {
          if (roleType.getRoleName().equals(roleName)) {
            retVal = roleType;
            break;
          }
        }
      }
    }
    return retVal;
  }

  public static <T> Set<T> collectionToSetAndNullToEmpty(Collection<T> c) {
    HashSet<T> set = Sets.newLinkedHashSet();
    if (c != null) {
      set.addAll(c);
    }
    return set;
  }

  public static Set<String> getGroupNamesForUser(ServiceConfiguration configuration, String userName, String effectiveUserId) throws Exception {
    Set<String> groupSet = Sets.newLinkedHashSet();
    boolean seenAllGroups = false;
    String groupMarker = null;
    while (!seenAllGroups) {
      ListGroupsForUserType listGroupsForUserType = MessageHelper.createMessage(ListGroupsForUserType.class, effectiveUserId);
      listGroupsForUserType.setUserName(userName);
      if (groupMarker != null) {
        listGroupsForUserType.setMarker(groupMarker);
      }
      ListGroupsForUserResponseType listGroupsForUserResponseType = AsyncRequests.<ListGroupsForUserType,ListGroupsForUserResponseType> sendSync(configuration, listGroupsForUserType);
      if (Boolean.TRUE.equals(listGroupsForUserResponseType.getListGroupsForUserResult().getIsTruncated())) {
        groupMarker = listGroupsForUserResponseType.getListGroupsForUserResult().getMarker();
      } else {
        seenAllGroups = true;
      }
      if (listGroupsForUserResponseType.getListGroupsForUserResult().getGroups() != null && listGroupsForUserResponseType.getListGroupsForUserResult().getGroups().getMemberList() != null) {
        for (GroupType groupType: listGroupsForUserResponseType.getListGroupsForUserResult().getGroups().getMemberList()) {
          groupSet.add(groupType.getGroupName());
        }
      }
    }
    return groupSet;
  }

  public static Collection<String> nonexistantUsers(ServiceConfiguration configuration, Collection<String> userNames, String effectiveUserId) throws Exception {
    boolean seenAllUsers = false;
    List<String> currentUsers = Lists.newArrayList();
    String userMarker = null;
    while (!seenAllUsers) {
      ListUsersType listUsersType = MessageHelper.createMessage(ListUsersType.class, effectiveUserId);
      if (userMarker != null) {
        listUsersType.setMarker(userMarker);
      }
      ListUsersResponseType listUsersResponseType = AsyncRequests.<ListUsersType,ListUsersResponseType> sendSync(configuration, listUsersType);
      if (Boolean.TRUE.equals(listUsersResponseType.getListUsersResult().getIsTruncated())) {
        userMarker = listUsersResponseType.getListUsersResult().getMarker();
      } else {
        seenAllUsers = true;
      }
      if (listUsersResponseType.getListUsersResult().getUsers() != null && listUsersResponseType.getListUsersResult().getUsers().getMemberList() != null) {
        for (UserType userType: listUsersResponseType.getListUsersResult().getUsers().getMemberList()) {
          currentUsers.add(userType.getUserName());
        }
      }
    }
    List<String> nonexistantUsers = Lists.newArrayList();
    for (String user: userNames) {
      if (!currentUsers.contains(user)) {
        nonexistantUsers.add(user);
      }
    }
    return nonexistantUsers;
  }

  public static void removeUsersFromGroup(ServiceConfiguration configuration, Collection<String> userNames, String groupName, String effectiveUserId) throws Exception {
    // if no group, bye...
    if (!groupExists(configuration, groupName, effectiveUserId)) return;
    Set<String> passedInUsers = userNames == null ? new HashSet<String>() : Sets.newHashSet(userNames);
    Set<String> actualUsers = getUserNamesForGroup(configuration, groupName, effectiveUserId);
    for (String user: Sets.intersection(passedInUsers, actualUsers)) {
      RemoveUserFromGroupType removeUserFromGroupType = MessageHelper.createMessage(RemoveUserFromGroupType.class, effectiveUserId);
      removeUserFromGroupType.setGroupName(groupName);
      removeUserFromGroupType.setUserName(user);
      AsyncRequests.<RemoveUserFromGroupType,RemoveUserFromGroupResponseType> sendSync(configuration, removeUserFromGroupType);
    }

  }

  public static Set<String> getUserNamesForGroup(ServiceConfiguration configuration, String groupName, String effectiveUserId) throws Exception {
    Set<String> users = Sets.newLinkedHashSet();
    boolean seenAllUsers = false;
    String userMarker = null;
    while (!seenAllUsers) {
      GetGroupType getGroupType = MessageHelper.createMessage(GetGroupType.class, effectiveUserId);
      getGroupType.setGroupName(groupName);
      if (userMarker != null) {
        getGroupType.setMarker(userMarker);
      }
      GetGroupResponseType getGroupResponseType = AsyncRequests.<GetGroupType,GetGroupResponseType> sendSync(configuration, getGroupType);
      if (Boolean.TRUE.equals(getGroupResponseType.getGetGroupResult().getIsTruncated())) {
        userMarker = getGroupResponseType.getGetGroupResult().getMarker();
      } else {
        seenAllUsers = true;
      }
      if (getGroupResponseType.getGetGroupResult() != null && getGroupResponseType.getGetGroupResult().getUsers().getMemberList() != null) {
        for (UserType userType: getGroupResponseType.getGetGroupResult().getUsers().getMemberList()) {
          users.add(userType.getUserName());
        }
      }
    }
    return users;
  }

}
