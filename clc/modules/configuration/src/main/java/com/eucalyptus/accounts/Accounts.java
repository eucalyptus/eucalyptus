package com.eucalyptus.accounts;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Credentials;
import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.User;
import com.eucalyptus.auth.UserExistsException;
import com.eucalyptus.auth.UserGroupEntity;
import com.eucalyptus.auth.UserInfo;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.X509Cert;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
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
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>( );
    try {
      List<UserGroupEntity> groupList = db.recast( UserGroupEntity.class ).query( new UserGroupEntity( ) );
      for ( User u : Users.listAllUsers( ) ) {
        try {
          UserInfo otherInfo = db.getUnique( UserInfo.named( u.getName( ) ) );
          String accessKey = u.getQueryId( );
          String secretKey = u.getSecretKey( );
          X509Certificate cert = u.getX509Certificate( );
          String dn = cert.getSubjectX500Principal( ).toString( );
          String certSerial = cert.getSerialNumber( ).toString( );
          ArrayList<String> groups = Lists.newArrayList( );
          db.recast( UserGroupEntity.class ).query( new UserGroupEntity( ) );
          for ( UserGroupEntity g : groupList ) {
            if ( g.getUsers( ).contains( otherInfo ) ) {
              groups.add( g.getName( ) );
            }
          }
          UserInfoType userInfo = new UserInfoType( u.getName( ), otherInfo.getEmail( ), otherInfo.getCertificateCode( ), otherInfo.getConfirmationCode( ),
                                                    accessKey, secretKey, dn, certSerial, groups, u.getIsAdministrator( ), u.getIsEnabled( ),
                                                    otherInfo.isConfirmed( ) );
          reply.getUsers( ).add( userInfo );
        } catch ( EucalyptusCloudException e ) {}
      }
      db.commit( );
    } catch ( Exception e ) {
      db.rollback( );
    }
    return reply;
  }
  
  public AddUserResponseType addUser( AddUserType request ) throws EucalyptusCloudException {
    AddUserResponseType reply = request.getReply( );
    reply.set_return( false );
    String userName = request.getUserName( );
    String email = request.getEmail( );
    String certCode = Credentials.generateSessionToken( userName );
    String confirmCode = Credentials.generateSessionToken( userName );
    String oneTimePass = Credentials.generateSessionToken( userName );
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
          u.setIsEnabled( Boolean.FALSE );
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
    String userName = request.getUserName( );
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>( );
    try {
      UserInfo userInfo = db.getUnique( UserInfo.named( userName ) );
      db.delete( userInfo );
      db.commit( );
      try {
        Users.deleteUser( userName );
      } catch ( NoSuchUserException e ) {
        LOG.trace( e, e );
      }
      reply.set_return( true );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw new EucalyptusCloudException( "No such user: " + userName );
    }
    return reply;
  }
  
  public DescribeGroupsResponseType describeGroups( DescribeGroupsType request ) {
    DescribeGroupsResponseType reply = request.getReply( );
    EntityWrapper<UserGroupEntity> db = new EntityWrapper<UserGroupEntity>( );
    try {
      List<UserGroupEntity> groupList = db.query( new UserGroupEntity( ) );
      for ( UserGroupEntity g : groupList ) {
        try {
          GroupInfoType groupinfo = new GroupInfoType( g.getName( ) );
          for ( UserInfo u : g.getUsers( ) ) {
            groupinfo.getUsers( ).add( u.getUserName( ) );
          }
        } catch ( Exception e ) {
          LOG.trace( e, e );
        }
      }
      db.commit( );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      db.rollback( );
    }
    return reply;
  }
  
  public AddGroupResponseType addGroup( AddGroupType request ) throws EucalyptusCloudException {
    AddGroupResponseType reply = request.getReply( );
    EntityWrapper<UserGroupEntity> db = new EntityWrapper<UserGroupEntity>( );
    UserGroupEntity group = new UserGroupEntity( request.getGroupName( ) );
    try {
      db.getUnique( group );
      throw new EucalyptusCloudException( "Group already exists: " + group.getName( ) );
    } catch ( Exception e ) {
      try {
        db.add( new UserGroupEntity( request.getGroupName( ) ) );
        db.commit( );
      } catch ( Exception e1 ) {
        db.rollback( );
        throw new EucalyptusCloudException( "Error adding group: " + group.getName( ) );
      }
    }
    return reply;
  }
}
