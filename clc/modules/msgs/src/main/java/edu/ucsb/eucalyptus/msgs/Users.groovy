package edu.ucsb.eucalyptus.msgs;

import java.util.ArrayList;

public class UserInfoType extends EucalyptusData {
  String userName;
  String email;
  String certificateCode;
  String confirmationCode;
  String accessKey;
  String secretKey;
  Boolean admin = Boolean.FALSE;
  Boolean confirmed = Boolean.FALSE;
  Boolean enabled = Boolean.FALSE;
  ArrayList<String> activeCertificates = new ArrayList<String>();
  ArrayList<String> groups = new ArrayList<String>();
  public UserInfoType( String userName, String email, String certificateCode, String confirmationCode, String accessKey, String secretKey, ArrayList<String> activeCertificates, ArrayList<String> groups, Boolean admin, Boolean enabled, Boolean confirmed ) {
    this.userName = userName;
    this.email = email;
    this.certificateCode = certificateCode;
    this.confirmationCode = confirmationCode;
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.activeCertificates = activeCertificates;
    this.groups = groups;
    this.enabled = enabled;
    this.confirmed = confirmed;
    this.admin = admin;
  }
}  
public class GroupInfoType extends EucalyptusData {
  String groupName;
  ArrayList<String> users = new ArrayList<String>();
  public GroupInfoType( String name ) {
    this.groupName = name;
  }
}

public class DescribeUsersType extends EucalyptusMessage {
  ArrayList<String> userNames = new ArrayList<String>();  
}
public class DescribeUsersResponseType extends EucalyptusMessage {
  ArrayList<UserInfoType> users = new ArrayList<UserInfoType>();
}
public class AddUserType extends EucalyptusMessage {
  String userName;
  Boolean admin;
  String email;
}

public class AddUserResponseType extends EucalyptusMessage {
}
public class DeleteUserType extends EucalyptusMessage {
  String userName;
}
public class DeleteUserResponseType extends EucalyptusMessage {
}

public class DescribeGroupsType extends EucalyptusMessage {
  ArrayList<String> groupNames = new ArrayList<String>();  
}
public class DescribeGroupsResponseType extends EucalyptusMessage {
  ArrayList<GroupInfoType> groups = new ArrayList<GroupInfoType>();
}
public class AddGroupType extends EucalyptusMessage {
  String groupName;
}
public class AddGroupResponseType extends EucalyptusMessage {
}
public class DeleteGroupType extends EucalyptusMessage {
  String groupName;
}
public class DeleteGroupResponseType extends EucalyptusMessage {
}
