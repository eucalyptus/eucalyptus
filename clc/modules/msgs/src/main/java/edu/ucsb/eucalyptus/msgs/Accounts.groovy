package edu.ucsb.eucalyptus.msgs

import com.google.common.collect.Lists;

class AccountsManagementMessage extends ManagementMessage { }

class ListUsersType extends AccountsManagementMessage {
  String marker;
  Integer maxItems;
  String pathPrefix;
}

class UserType extends EucalyptusData {
  String arn;
  String path;
  String userId;
  String userName;
}

class ListUsersResponseType extends AccountsManagementMessage {
  ArrayList<UserType> users = Lists.newArrayList( );
  Boolean isTruncated;
  String marker;
}