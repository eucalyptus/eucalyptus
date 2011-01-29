package com.eucalyptus.accounts
;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.Groups;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.component.ComponentMessage;
import com.eucalyptus.empyrean.Empyrean;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


@ComponentMessage(AccountsService.class)
public class ManagementMessage extends BaseMessage {}
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
  ArrayList<String> revoked = new ArrayList<String>();
    
  public UserInfoType( User u, String email, String confirmationCode ) {
    this.userName = u.getName();
    this.accessKey = u.getQueryId();
    this.secretKey = u.getSecretKey();
    this.distinguishedName = u.getX509Certificate( )?.getSubjectX500Principal( )?.toString();
    this.certificateSerial = u.getX509Certificate( )?.getSerialNumber( );
    for( X509Certificate x : u.getAllX509Certificates() ) {
      if( !this.certificateSerial.equals(x.getSerialNumber().toString())) {
        this.revoked.add( x.getSerialNumber().toString() );
      }
    }
    for( Group g : Groups.lookupUserGroups( u ) ) {
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
  ArrayList<String> authorizations = new ArrayList<String>();
  public GroupInfoType( String name ) {
    this.groupName = name;
  }
}
public class UserManagementMessage extends ManagementMessage {}

public class DescribeUsersType extends UserManagementMessage {
  ArrayList<String> userNames = new ArrayList<String>();  
}
public class DescribeUsersResponseType extends UserManagementMessage {
  ArrayList<UserInfoType> users = new ArrayList<UserInfoType>();
}
public class AddUserType extends UserManagementMessage {
  String userName;
  Boolean admin;
  String email;
}

public class AddUserResponseType extends UserManagementMessage {}
public class DeleteUserType extends UserManagementMessage {
  String userName;
}
public class DeleteUserResponseType extends UserManagementMessage {}


public class GroupManagementMessage extends ManagementMessage  {}
public class DescribeGroupsType extends GroupManagementMessage {
  ArrayList<String> groupNames = new ArrayList<String>();  
}
public class DescribeGroupsResponseType extends GroupManagementMessage {
  ArrayList<GroupInfoType> groups = new ArrayList<GroupInfoType>();
}
public class AddGroupType extends GroupManagementMessage {
  String groupName;
}
public class AddGroupResponseType extends GroupManagementMessage {}
public class DeleteGroupType extends GroupManagementMessage {
  String groupName;
}
public class DeleteGroupResponseType extends GroupManagementMessage {}
public class AddGroupMemberType extends GroupManagementMessage {
  String groupName;
  String userName;
  Boolean admin;
}
public class AddGroupMemberResponseType extends GroupManagementMessage {}
public class DeleteGroupMemberType extends GroupManagementMessage {
  String groupName;
  String userName;
}
public class DeleteGroupMemberResponseType extends GroupManagementMessage {}

public class RemoveGroupMemberType extends GroupManagementMessage {
  String groupName;
  String userName;
}
public class RemoveGroupMemberResponseType extends GroupManagementMessage {}

public class GrantGroupAuthorizationType extends GroupManagementMessage {
  String groupName;
  String zoneName;
}
public class GrantGroupAuthorizationResponseType extends GroupManagementMessage {}

public class RevokeGroupAuthorizationType extends GroupManagementMessage {
  String groupName;
  String zoneName;
}
public class RevokeGroupAuthorizationResponseType extends GroupManagementMessage {}


public class GrantGroupAdminType extends GroupManagementMessage {
  String groupName;
  String userName;
}
public class GrantGroupAdminResponseType extends GroupManagementMessage {}
public class RevokeGroupAdminType extends GroupManagementMessage {
  String groupName;
  String userName;
}
public class RevokeGroupAdminResponseType extends GroupManagementMessage {}



