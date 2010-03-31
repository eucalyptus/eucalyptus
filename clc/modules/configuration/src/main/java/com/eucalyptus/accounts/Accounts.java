package com.eucalyptus.accounts;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections.EnumerationUtils;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.GroupExistsException;
import com.eucalyptus.auth.Groups;
import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.UserExistsException;
import com.eucalyptus.auth.UserGroupEntity;
import com.eucalyptus.auth.UserInfo;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.X509Cert;
import com.eucalyptus.auth.crypto.Crypto;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.AddGroupResponseType;
import edu.ucsb.eucalyptus.msgs.AddGroupType;
import edu.ucsb.eucalyptus.msgs.AddUserResponseType;
import edu.ucsb.eucalyptus.msgs.AddUserType;
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
    Function<User,UserInfoType> mapUser = new Function<User,UserInfoType>() {
      @Override
      public UserInfoType apply( User u ) {
        UserInfo otherInfo;
        try {
          otherInfo = db.getUnique( UserInfo.named( u.getName( ) ) );
        } catch ( EucalyptusCloudException e ) {
          otherInfo = new UserInfo(  );
        }
        return new UserInfoType( u, otherInfo.getEmail( ), otherInfo.getCertificateCode( ), otherInfo.getConfirmationCode( ) );
      }
    };
    List<UserInfoType> userList = Lists.newArrayList( );
    if( request.getUserNames( ).isEmpty( ) ) {
      userList = Lists.transform( Users.listAllUsers( ), mapUser );
    } else {
      for( String name : request.getUserNames( ) ) { try {
          userList.add( mapUser.apply( Users.lookupUser( name ) ) );
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
    String certCode = Crypto.generateSessionToken( userName );
    String confirmCode = Crypto.generateSessionToken( userName );
    String oneTimePass = Crypto.generateSessionToken( userName );
    boolean admin = request.getAdmin( );
    try {
      User u = null;
      if ( email == null ) {
        u = Users.addUser( userName, admin, true );
      } else {
        u = Users.addUser( userName, admin, false );
      }
      EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>( );
      try {
        UserInfo newUser = null;
        if ( email == null ) {
          newUser = new UserInfo( userName, admin, confirmCode, certCode, oneTimePass );
        } else {
          //TODO: handle the email dispatch case here.
          newUser = new UserInfo( userName, email, admin, confirmCode, certCode, oneTimePass );
          u.setEnabled( Boolean.FALSE );
        }
        db.add( newUser );
        db.commit( );
        reply.set_return( true );
      } catch ( Throwable t ) {
        db.rollback( );
        try {
          Users.deleteUser( userName );
        } catch ( NoSuchUserException e ) {}
        throw new EucalyptusCloudException( "Error creating user: " + userName + ": " + t.getMessage( ) );
      }
    } catch ( UserExistsException e1 ) {
      throw new EucalyptusCloudException( "User already exists: " + userName );
    }
    return reply;
  }
  
  public DeleteUserResponseType deleteUser( DeleteUserType request ) throws EucalyptusCloudException {
    DeleteUserResponseType reply = request.getReply( );
    reply.set_return( false );
    UserInfo userInfo = UserInfo.named( request.getUserName( ) );
    EntityWrapper<UserInfo> db = EntityWrapper.get( userInfo );
    try {
      UserInfo deleteUserInfo = db.getUnique( userInfo );
      db.delete( deleteUserInfo );
      try {
        Users.deleteUser( request.getUserName( ) );
      } catch ( NoSuchUserException e ) {
        db.rollback( );
        throw new EucalyptusCloudException( "No such user exists: " + request.getUserName( ), e );
      } catch ( UnsupportedOperationException e ) {
        db.rollback( );
        throw new EucalyptusCloudException( "System is configured to be read only.", e );
      }
      db.commit( );
      reply.set_return( true );
    } catch ( Exception e1 ) {
      throw new EucalyptusCloudException( "System is configured to be read only." );      
    }
    return reply;
  }
  
  public DescribeGroupsResponseType describeGroups( DescribeGroupsType request ) {
    DescribeGroupsResponseType reply = request.getReply( );
    List<Group> groups = Groups.listAllGroups( );
    for ( Group g : groups ) {
      GroupInfoType groupinfo = new GroupInfoType( g.getName( ) );
      for ( User u : (List<User>) EnumerationUtils.toList( g.members( ) ) ) {
        groupinfo.getUsers( ).add( u.getName( ) );
      }
    }
    return reply;
  }
  
  public AddGroupResponseType addGroup( AddGroupType request ) throws EucalyptusCloudException {
    AddGroupResponseType reply = request.getReply( );
    try {
      Groups.addGroup( request.getGroupName( ) );
    } catch ( GroupExistsException e ) {
      throw new EucalyptusCloudException( "Group already exists: " + request.getGroupName( ), e );
    }
    return reply;
  }
}
