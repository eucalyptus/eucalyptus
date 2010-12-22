package edu.ucsb.eucalyptus.msgs

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
  ArrayList<UserType> users;
  Boolean isTruncated;
  String marker;
}