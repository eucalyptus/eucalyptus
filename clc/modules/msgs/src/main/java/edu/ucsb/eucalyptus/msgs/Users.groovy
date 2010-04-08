package edu.ucsb.eucalyptus.msgs;

import java.util.ArrayList;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.Groups;

public class UserInfoType extends EucalyptusData {
  String userName;
  String email;
  String accessKey;
  String secretKey;
  Boolean admin = Boolean.FALSE;
  Boolean enabled = Boolean.FALSE;
  String distinguishedName;
  String certificateSerial;
  String certificateCode;
  String confirmationCode;
  ArrayList<String> groups = new ArrayList<String>();
  
  public UserInfoType( User u, String email, String confirmationCode ) {
    this.userName = u.getName();
    this.accessKey = u.getQueryId();
    this.secretKey = u.getSecretKey();
    this.distinguishedName = u.getX509Certificate( )?.getSubjectX500Principal( )?.toString();
    this.certificateSerial = u.getX509Certificate( )?.getSerialNumber( );
    for( Group g : Groups.lookupGroups( u ) ) {
      this.groups.add( g.getName() );
    }
    this.enabled = u.isEnabled( );
    this.admin = u.isAdministrator( );
    this.email = email;
    this.certificateCode = u.getToken();
    this.confirmationCode = confirmationCode;
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
