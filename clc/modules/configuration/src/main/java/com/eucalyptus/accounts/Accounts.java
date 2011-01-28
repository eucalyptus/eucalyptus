package com.eucalyptus.accounts;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.GroupExistsException;
import com.eucalyptus.auth.Groups;
import com.eucalyptus.auth.NoSuchGroupException;
import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.UserExistsException;
import com.eucalyptus.auth.UserInfo;
import com.eucalyptus.auth.UserInfoStore;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.AvailabilityZonePermission;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.id.Cluster;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.eucalyptus.accounts.AddGroupMemberResponseType;
import com.eucalyptus.accounts.AddGroupMemberType;
import com.eucalyptus.accounts.AddGroupResponseType;
import com.eucalyptus.accounts.AddGroupType;
import com.eucalyptus.accounts.AddUserResponseType;
import com.eucalyptus.accounts.AddUserType;
import com.eucalyptus.accounts.DeleteGroupResponseType;
import com.eucalyptus.accounts.DeleteGroupType;
import com.eucalyptus.accounts.DeleteUserResponseType;
import com.eucalyptus.accounts.DeleteUserType;
import com.eucalyptus.accounts.DescribeGroupsResponseType;
import com.eucalyptus.accounts.DescribeGroupsType;
import com.eucalyptus.accounts.DescribeUsersResponseType;
import com.eucalyptus.accounts.DescribeUsersType;
import com.eucalyptus.accounts.GrantGroupAuthorizationResponseType;
import com.eucalyptus.accounts.GrantGroupAuthorizationType;
import com.eucalyptus.accounts.GroupInfoType;
import com.eucalyptus.accounts.RemoveGroupMemberResponseType;
import com.eucalyptus.accounts.RemoveGroupMemberType;
import com.eucalyptus.accounts.RevokeGroupAuthorizationResponseType;
import com.eucalyptus.accounts.RevokeGroupAuthorizationType;
import com.eucalyptus.accounts.UserInfoType;

public class Accounts {
  private static Logger LOG = Logger.getLogger( Accounts.class );
  
  public DescribeUsersResponseType describeUsers( DescribeUsersType request ) {
    DescribeUsersResponseType reply = request.getReply( );
    
    Function<User, UserInfoType> mapUser = new Function<User, UserInfoType>( ) {
      @Override
      public UserInfoType apply( User u ) {
        UserInfo otherInfo;
        try {
          otherInfo = UserInfoStore.getUserInfo( new UserInfo( u.getName( ) ) );
          return new UserInfoType( u, otherInfo.getEmail( ), otherInfo.getConfirmationCode( ) );
        } catch ( NoSuchUserException e ) {
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
      if ( email == null || UserInfo.BOGUS_ENTRY.equals( email ) ) {
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
    List<Group> groups = null;
    if( request.getGroupNames( ).isEmpty( ) ) {
      groups = Groups.listAllGroups( );
    } else {
      for( String name : request.getGroupNames( ) ) {
        groups = Lists.newArrayList( );
        try {
          groups.add( Groups.lookupGroup( name ) );
        } catch ( NoSuchGroupException e ) {
          LOG.debug( e );
        }
      }
    }
    if ( groups != null ) {
      for ( Group g : groups ) {
        GroupInfoType groupinfo = new GroupInfoType( g.getName( ) );
        for ( User u : g.getMembers( ) ) {
          groupinfo.getUsers( ).add( u.getName( ) );
        }
        for ( Authorization a : g.getAuthorizations( ) ) {
          groupinfo.getAuthorizations( ).add( a.getName( ) + ":" + a.getValue( ) );
        }
        reply.getGroups( ).add( groupinfo );
      }
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
  
  public AddGroupMemberResponseType addMember( AddGroupMemberType request ) throws EucalyptusCloudException {
    AddGroupMemberResponseType reply = request.getReply( );
    try {
      Groups.lookupGroup( request.getGroupName( ) ).addMember( Users.lookupUser( request.getUserName( ) ) );
      reply.set_return( true );
    } catch ( NoSuchGroupException e ) {
      throw new EucalyptusCloudException( "Failed to add user to group: " + request.getUserName( ) + " to group " + request.getGroupName( ), e );
    } catch ( NoSuchUserException e ) {
      throw new EucalyptusCloudException( "Failed to add user to group: " + request.getUserName( ) + " to group " + request.getGroupName( ), e );
    }
    return reply;    
  }
  public RemoveGroupMemberResponseType removeMember( RemoveGroupMemberType request ) throws EucalyptusCloudException {
    RemoveGroupMemberResponseType reply = request.getReply( );
    try {
      Groups.lookupGroup( request.getGroupName( ) ).removeMember( Users.lookupUser( request.getUserName( ) ) );
      reply.set_return( true );
    } catch ( NoSuchGroupException e ) {
      throw new EucalyptusCloudException( "Failed to remove user to group: " + request.getUserName( ) + " to group " + request.getGroupName( ), e );
    } catch ( NoSuchUserException e ) {
      throw new EucalyptusCloudException( "Failed to remove user to group: " + request.getUserName( ) + " to group " + request.getGroupName( ), e );
    }
    return reply;    
  }

  public GrantGroupAuthorizationResponseType authorize( final GrantGroupAuthorizationType request ) throws EucalyptusCloudException {
    GrantGroupAuthorizationResponseType reply = request.getReply( );
    if(!Iterables.any( Components.lookup( Cluster.class ).list( ), new Predicate<ServiceConfiguration>() {
      @Override
      public boolean apply( ServiceConfiguration arg0 ) {
        return arg0.getName( ).equals( request.getZoneName( ) );
      }} )) {
      throw new EucalyptusCloudException( "No such cluster to add authorization for: " + request.getZoneName( ) + " for group " + request.getGroupName( ) );      
    }
    try {
      Groups.lookupGroup( request.getGroupName( ) ).addAuthorization( new AvailabilityZonePermission( request.getZoneName( ) ) );
      reply.set_return( true );
    } catch ( NoSuchGroupException e ) {
      throw new EucalyptusCloudException( "Failed to add group authorization: " + request.getZoneName( ) + " to group " + request.getGroupName( ), e );
    }
    return reply;    
  }

  public RevokeGroupAuthorizationResponseType authorize( RevokeGroupAuthorizationType request ) throws EucalyptusCloudException {
    RevokeGroupAuthorizationResponseType reply = request.getReply( );
    try {
      Group g = Groups.lookupGroup( request.getGroupName( ) );
      for( Authorization a : g.getAuthorizations( ) ) {
        if( a instanceof AvailabilityZonePermission && ((AvailabilityZonePermission)a).getValue( ).equals( request.getZoneName( ) ) ) {
          g.removeAuthorization( a );
        }
      }
      reply.set_return( true );
    } catch ( NoSuchGroupException e ) {
      throw new EucalyptusCloudException( "Failed to add group authorization: " + request.getZoneName( ) + " to group " + request.getGroupName( ), e );
    }
    return reply;    
  }

}
