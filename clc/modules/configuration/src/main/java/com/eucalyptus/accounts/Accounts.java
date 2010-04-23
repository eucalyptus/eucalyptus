package com.eucalyptus.accounts;

import java.util.List;
import org.apache.commons.collections.EnumerationUtils;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.GroupExistsException;
import com.eucalyptus.auth.Groups;
import com.eucalyptus.auth.NoSuchGroupException;
import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.UserExistsException;
import com.eucalyptus.auth.UserInfo;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.AddGroupResponseType;
import edu.ucsb.eucalyptus.msgs.AddGroupType;
import edu.ucsb.eucalyptus.msgs.AddUserResponseType;
import edu.ucsb.eucalyptus.msgs.AddUserType;
import edu.ucsb.eucalyptus.msgs.DeleteGroupResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteGroupType;
import edu.ucsb.eucalyptus.msgs.DeleteUserResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteUserType;
import edu.ucsb.eucalyptus.msgs.DescribeGroupsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeGroupsType;
import edu.ucsb.eucalyptus.msgs.DescribeUsersResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeUsersType;
import edu.ucsb.eucalyptus.msgs.GroupInfoType;
import edu.ucsb.eucalyptus.msgs.UserInfoType;

public class Accounts {
  private static Logger LOG = Logger.getLogger( Accounts.class );
  
  public DescribeUsersResponseType describeUsers( DescribeUsersType request ) {
    DescribeUsersResponseType reply = request.getReply( );
    
    final EntityWrapper<UserInfo> db = EntityWrapper.get( new UserInfo( ) );
    Function<User, UserInfoType> mapUser = new Function<User, UserInfoType>( ) {
      @Override
      public UserInfoType apply( User u ) {
        UserInfo otherInfo;
        try {
          otherInfo = db.getUnique( new UserInfo( u.getName( ) ) );
          return new UserInfoType( u, otherInfo.getEmail( ), otherInfo.getConfirmationCode( ) );
        } catch ( EucalyptusCloudException e ) {
          return new UserInfoType( u, null, null );
        }
      }
    };
    
    List<UserInfoType> userList = reply.getUsers( );
    if ( request.getUserNames( ).isEmpty( ) ) {
      List<User> allUsers = Users.listAllUsers( );
      List<UserInfoType> allUserInfo = Lists.transform( allUsers, mapUser );
      userList.addAll( allUserInfo );
    } else {
      for ( String name : request.getUserNames( ) ) {
        try {
          User user = Users.lookupUser( name );
          UserInfoType userInfo = mapUser.apply( user );
          userList.add( userInfo );
        } catch ( NoSuchUserException e ) {}
      }
    }
    db.commit( );
    return reply;
  }
  
  public AddUserResponseType addUser( AddUserType request ) throws EucalyptusCloudException {
    AddUserResponseType reply = request.getReply( );
    reply.set_return( false );
    String userName = request.getUserName( );
    String email = request.getEmail( );
    boolean admin = request.getAdmin( );
    try {
      User u = null;
      if ( email == null ) {
        u = Users.addUser( userName, admin, true );
      } else {
        u = Users.addUser( userName, admin, false );
      }
    } catch ( UserExistsException e1 ) {
      throw new EucalyptusCloudException( "User already exists: " + userName );
    }
    reply.set_return( true );
    return reply;
  }
  
  public DeleteUserResponseType deleteUser( DeleteUserType request ) throws EucalyptusCloudException {
    DeleteUserResponseType reply = request.getReply( );
    reply.set_return( false );
    try {
      Users.deleteUser( request.getUserName( ) );
    } catch ( NoSuchUserException e ) {
      throw new EucalyptusCloudException( "No such user exists: " + request.getUserName( ), e );
    } catch ( UnsupportedOperationException e ) {
      throw new EucalyptusCloudException( "System is configured to be read only.", e );
    }
    reply.set_return( true );
    return reply;
  }
  public DeleteGroupResponseType deleteUser( DeleteGroupType request ) throws EucalyptusCloudException {
    DeleteGroupResponseType reply = request.getReply( );
    reply.set_return( false );
    try {
      Groups.deleteGroup( request.getGroupName( ) );
    } catch ( NoSuchGroupException e ) {
      throw new EucalyptusCloudException( "No such group exists: " + request.getGroupName( ), e );
    } catch ( UnsupportedOperationException e ) {
      throw new EucalyptusCloudException( "System is configured to be read only.", e );
    }
    reply.set_return( true );
    return reply;
  }
  
  public DescribeGroupsResponseType describeGroups( DescribeGroupsType request ) {
    DescribeGroupsResponseType reply = request.getReply( );
    List<Group> groups = Groups.listAllGroups( );
    for ( Group g : groups ) {
      GroupInfoType groupinfo = new GroupInfoType( g.getName( ) );
      for ( User u : ( List<User> ) EnumerationUtils.toList( g.members( ) ) ) {
        groupinfo.getUsers( ).add( u.getName( ) );
      }
      reply.getGroups( ).add( groupinfo );
    }
    return reply;
  }
  
  public AddGroupResponseType addGroup( AddGroupType request ) throws EucalyptusCloudException {
    AddGroupResponseType reply = request.getReply( );
    try {
      Groups.addGroup( request.getGroupName( ) );
      reply.set_return( true );
    } catch ( GroupExistsException e ) {
      throw new EucalyptusCloudException( "Group already exists: " + request.getGroupName( ), e );
    }
    return reply;
  }
}
