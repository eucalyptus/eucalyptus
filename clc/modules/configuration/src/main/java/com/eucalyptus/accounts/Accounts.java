package com.eucalyptus.accounts;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.CredentialProvider;
import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.User;
import com.eucalyptus.auth.UserExistsException;
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
      List<UserGroupInfo> groupList = db.recast( UserGroupInfo.class ).query( new UserGroupInfo() );
      for ( User u : CredentialProvider.getAllUsers( ) ) {
        try {
          UserInfo otherInfo = db.getUnique( UserInfo.named( u.getUserName( ) ) );
          String accessKey = u.getQueryId( );
          String secretKey = u.getSecretKey( );
          ArrayList<String> certs = Lists.newArrayList( );
          ArrayList<String> groups = Lists.newArrayList( );
          for ( X509Cert cert : u.getCertificates( ) ) {
            certs.add( cert.getAlias( ) );
          }
          db.recast( UserGroupInfo.class ).query( new UserGroupInfo() );
          for( UserGroupInfo g : groupList ) {
            if( g.getUsers( ).contains( otherInfo ) ) {
              groups.add( g.getName( ) );
            }
          }
          UserInfoType userInfo = new UserInfoType( u.getUserName( ), otherInfo.getEmail( ), otherInfo.getCertificateCode( ), otherInfo.getConfirmationCode( ),
                                                    accessKey, secretKey, certs, groups, u.getIsAdministrator( ), u.getIsEnabled( ), otherInfo.isConfirmed( ) );
          reply.getUsers( ).add( userInfo );
        } catch ( EucalyptusCloudException e ) {
        }
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
    String certCode = Hashes.getDigestBase64( userName, Hashes.Digest.SHA512, true ).replaceAll("\\p{Punct}", "" );
    String confirmCode = Hashes.getDigestBase64( userName, Hashes.Digest.SHA512, true ).replaceAll("\\p{Punct}", "" );
    String oneTimePass = Hashes.getDigestBase64( userName, Hashes.Digest.MD2, true ).replaceAll("\\p{Punct}", "" );
    boolean admin = request.getAdmin( );
    try {
      User u = null;
      if( email == null ) {
        u = CredentialProvider.addUser( userName, admin );
      } else {
        u = CredentialProvider.addDisabledUser( userName, admin );        
      }
      EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
      try {
        UserInfo newUser = null;
        if( email == null ) {
          newUser = new UserInfo( userName, admin, confirmCode, certCode, oneTimePass );
        } else {
          //TODO: handle the email dispatch case here.
          newUser = new UserInfo( userName, email, admin, confirmCode, certCode, oneTimePass );        
          u.setIsEnabled( false );
        }
        db.add( newUser );
        db.commit( );
        reply.set_return( true );
      } catch ( Throwable t ) {
        db.rollback( );
        try {
          CredentialProvider.deleteUser( userName );
        } catch ( NoSuchUserException e ) {
        }
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
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    try {
      UserInfo userInfo = db.getUnique( UserInfo.named( userName ) );
      db.delete( userInfo );
      db.commit( );
      try {
        CredentialProvider.deleteUser( userName );
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
    EntityWrapper<UserGroupInfo> db = new EntityWrapper<UserGroupInfo>();
    try {
      List<UserGroupInfo> groupList = db.query( new UserGroupInfo() );
      for( UserGroupInfo g : groupList ) {
        try {
          GroupInfoType groupinfo = new GroupInfoType( g.getName( ) );
          for( UserInfo u : g.getUsers( ) ) {
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
    EntityWrapper<UserGroupInfo> db = new EntityWrapper<UserGroupInfo>();
    UserGroupInfo group = new UserGroupInfo( request.getGroupName( ) );
    try {
      db.getUnique( group );
      throw new EucalyptusCloudException("Group already exists: " + group.getName( ) );
    } catch ( Exception e ) {
      try {
        db.add( new UserGroupInfo( request.getGroupName( ) ) );
        db.commit( );
      } catch ( Exception e1 ) {
        db.rollback( );
        throw new EucalyptusCloudException("Error adding group: " + group.getName( ) );
      }
    }
    return reply;
  }
}
