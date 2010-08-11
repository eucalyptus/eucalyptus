/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.auth;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.List;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import com.eucalyptus.auth.api.GroupProvider;
import com.eucalyptus.auth.api.NoSuchCertificateException;
import com.eucalyptus.auth.api.UserInfoProvider;
import com.eucalyptus.auth.api.UserProvider;
import com.eucalyptus.auth.crypto.Crypto;
import com.eucalyptus.auth.crypto.Hmacs;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.B64;
import com.eucalyptus.auth.util.PEMFiles;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Lists;

public class DatabaseAuthProvider implements UserProvider, GroupProvider, UserInfoProvider {
  private static Logger LOG = Logger.getLogger( DatabaseAuthProvider.class );
  
  public DatabaseAuthProvider( ) {}
  
  @Override
  public User addUser( String userName, Boolean isAdmin, Boolean isEnabled ) throws UserExistsException {
    UserEntity newUser = new UserEntity( userName );
    newUser.setQueryId( Hmacs.generateQueryId( userName ) );
    newUser.setSecretKey( Hmacs.generateSecretKey( userName ) );
    newUser.setAdministrator( isAdmin );
    newUser.setEnabled( isEnabled );
    newUser.setPassword( Crypto.generateHashedPassword( userName ) );
    newUser.setToken( Crypto.generateSessionToken( userName ) );
    EntityWrapper<UserEntity> db = Authentication.getEntityWrapper( );
    try {
      db.add( newUser );
      db.commit( );
    } catch ( Throwable t ) {
      db.rollback( );
      throw new UserExistsException( t );
    }
    EntityWrapper<UserInfo> dbU = EntityWrapper.get( UserInfo.class );
    try {
      String confirmCode = Crypto.generateSessionToken( userName );
      UserInfo newUserInfo = new UserInfo( userName, confirmCode );
      dbU.add( newUserInfo );
      dbU.commit( );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      dbU.rollback( );
    }    
    User proxy = new DatabaseWrappedUser( newUser );
    Groups.DEFAULT.addMember( proxy );
    return proxy;
  }
  
  @Override
  public void deleteUser( String userName ) throws NoSuchUserException {
    UserEntity user = new UserEntity( userName );
    EntityWrapper<UserInfo> dbU = EntityWrapper.get( UserInfo.class );
    try {
      UserInfo newUserInfo = dbU.getUnique( new UserInfo( userName ) );
      dbU.delete( newUserInfo );
      dbU.commit( );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      dbU.rollback( );
    }    
    EntityWrapper<User> db = Authentication.getEntityWrapper( );
    try {
      User foundUser = db.getUnique( user );
      for( Group g : Groups.lookupUserGroups( foundUser ) ) {
        g.removeMember( foundUser );
      }
      db.delete( foundUser );
      db.commit( );
    } catch ( Exception e ) {
      db.rollback( );
      throw new NoSuchUserException( e );
    }
  }
  
  @Override
  public List<Group> lookupUserGroups( User user ) {
    List<Group> userGroups = Lists.newArrayList( );
    EntityWrapper<GroupEntity> db = Authentication.getEntityWrapper( );
    try {
      UserEntity userInfo = db.recast( UserEntity.class ).getUnique( new UserEntity( user.getName( ) ) );
      for ( GroupEntity g : db.query( new GroupEntity( ) ) ) {
        if ( g.isMember( userInfo ) ) {
          userGroups.add( DatabaseWrappedGroup.newInstance( g ) );
        }
      }
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback( );
    }
    return userGroups;
  }
  
  @Override
  public Group lookupGroup( String groupName ) throws NoSuchGroupException {
    EntityWrapper<GroupEntity> db = Authentication.getEntityWrapper( );
    try {
      GroupEntity group = db.getUnique( new GroupEntity( groupName ) );
      db.commit( );
      return DatabaseWrappedGroup.newInstance( group );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw new NoSuchGroupException( e );
    }
  }
  
  @Override
  public List<User> listAllUsers( ) {
    List<User> users = Lists.newArrayList( );
    EntityWrapper<UserEntity> db = Authentication.getEntityWrapper( );
    UserEntity searchUser = new UserEntity( );
    UserEntity user = null;
    try {
      for( UserEntity u : db.query( searchUser ) ) {
        users.add( new DatabaseWrappedUser( u ) );
      }
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
    }
    return users;
  }
  
  @Override
  public List<User> listEnabledUsers( ) {
    List<User> users = Lists.newArrayList( );
    EntityWrapper<UserEntity> db = Authentication.getEntityWrapper( );
    UserEntity searchUser = new UserEntity( );
    searchUser.setEnabled( true );
    UserEntity user = null;
    try {
      for( UserEntity u : db.query( searchUser ) ) {
        users.add( new DatabaseWrappedUser( u ) );
      }
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
    }
    return users;
  }
  
  @Override
  public User lookupCertificate( X509Certificate cert ) throws NoSuchUserException {
    String certPem = B64.url.encString( PEMFiles.getBytes( cert ) );
    UserEntity searchUser = new UserEntity( );
    searchUser.setEnabled( true );
    X509Cert searchCert = new X509Cert( );
    searchCert.setPemCertificate( certPem );
    searchCert.setRevoked( null );
    EntityWrapper<UserEntity> db = EntityWrapper.get( searchUser );
    Session session = db.getSession( );
    try {
      Example qbeUser = Example.create( searchUser ).enableLike( MatchMode.EXACT );
      Example qbeCert = Example.create( searchCert ).enableLike( MatchMode.EXACT );
      List<UserEntity> users = ( List<UserEntity> ) session.createCriteria( UserEntity.class ).setCacheable( true ).add( qbeUser ).createCriteria( "certificates" )
                                               .setCacheable( true ).add( qbeCert ).list( );
      UserEntity ret = users.size( ) == 1 ? users.get( 0 ) : null;
      int size = users.size( );
      if ( ret != null ) {
        return new DatabaseWrappedUser( ret );
      } else {
        throw new GeneralSecurityException( ( size == 0 ) ? "No user with the specified certificate." : "Multiple users with the same certificate." );
      }
    } catch ( Throwable t ) {
      throw new NoSuchUserException( t );
    } finally {
      db.rollback( );
    }
  }
  
  @Override
  public boolean checkRevokedCertificate( X509Certificate cert ) throws NoSuchCertificateException {
    String certPem = B64.url.encString( PEMFiles.getBytes( cert ) );
    UserEntity searchUser = new UserEntity( );
    searchUser.setEnabled( true );
    X509Cert searchCert = new X509Cert( );
    searchCert.setPemCertificate( certPem );
    searchCert.setRevoked( true );
    EntityWrapper<UserEntity> db = EntityWrapper.get( searchUser );
    Session session = db.getSession( );
    try {
      Example qbeUser = Example.create( searchUser ).enableLike( MatchMode.EXACT );
      Example qbeCert = Example.create( searchCert ).enableLike( MatchMode.EXACT );
      List<User> users = ( List<User> ) session.createCriteria( User.class ).setCacheable( true ).add( qbeUser ).createCriteria( "certificates" )
                                               .setCacheable( true ).add( qbeCert ).list( );
      if( users.isEmpty( ) || users.size( ) > 1 ) {
        throw new NoSuchCertificateException( "Failed to identify user (found " + users.size() + ") from certificate information: " + cert.getSubjectX500Principal( ).toString( ) );
      } else {
        return true;
      }
    } finally {
      db.rollback( );
    }
  }
  
  
  @Override
  public User lookupQueryId( String queryId ) throws NoSuchUserException {
    String userName = null;
    EntityWrapper<UserEntity> db = Authentication.getEntityWrapper( );
    UserEntity searchUser = new UserEntity( );
    searchUser.setQueryId( queryId );
    UserEntity user = null;
    try {
      user = db.getUnique( searchUser );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      throw new NoSuchUserException( e );
    }
    return new DatabaseWrappedUser( user );
  }
  
  @Override
  public User lookupUser( String userName ) throws NoSuchUserException {
    EntityWrapper<UserEntity> db = Authentication.getEntityWrapper( );
    UserEntity searchUser = new UserEntity( userName );
    UserEntity user = null;
    try {
      user = db.getUnique( searchUser );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      throw new NoSuchUserException( e );
    }
    return new DatabaseWrappedUser( user );
  }
  
  @Override
  public Group addGroup( String groupName ) throws GroupExistsException {
    EntityWrapper<GroupEntity> db = Authentication.getEntityWrapper( );
    GroupEntity newGroup = new GroupEntity( groupName );
    try {
      db.add( newGroup );
      db.commit( );
    } catch ( Throwable t ) {
      db.rollback( );
      throw new GroupExistsException( t );
    }
    return DatabaseWrappedGroup.newInstance( newGroup );
  }
  
  @Override
  public List<Group> listAllGroups( ) {
    List<Group> ret = Lists.newArrayList( );
    GroupEntity search = new GroupEntity( );
    EntityWrapper<GroupEntity> db = EntityWrapper.get( search );
    try {
      List<GroupEntity> groupList = db.query( search );
      for ( GroupEntity g : groupList ) {
        ret.add( DatabaseWrappedGroup.newInstance( g ) );
      }
    } finally {
      db.commit( );
    }
    return ret;
  }
  @Override
  public void deleteGroup( String groupName ) throws NoSuchGroupException {
    Groups.checkNotRestricted( groupName );
    EntityWrapper<GroupEntity> db = Authentication.getEntityWrapper( );
    GroupEntity delGroup = new GroupEntity( groupName );
    try {
      GroupEntity g = db.getUnique( delGroup );
      db.delete( g );
      db.commit( );
    } catch ( Throwable t ) {
      db.rollback( );
      throw new NoSuchGroupException( t );
    }    
  }

  @Override
  public void updateUser( String name, Tx<User> userTx ) throws NoSuchUserException {
    UserEntity search = new UserEntity(name);
    EntityWrapper<UserEntity> db = EntityWrapper.get( search );
    try {
      UserEntity entity = db.getUnique( search );
      userTx.fire( entity );
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw new NoSuchUserException( e.getMessage( ), e );
    } catch ( Throwable e ) {
      db.rollback( );
      LOG.error( e, e );
      throw new NoSuchUserException( e.getMessage( ), e );
    }    
  }

  @Override
  public void addUserInfo( UserInfo user ) throws UserExistsException {
    EntityWrapper<UserInfo> dbWrapper = EntityWrapper.get( UserInfo.class );
    try {
      dbWrapper.add( user );
      dbWrapper.commit();
    } catch ( Exception e1 ) {
      dbWrapper.rollback();
      LOG.error( e1, e1 );
      throw new UserExistsException( "User info exists", e1 );
    }    
  }

  @Override
  public void deleteUserInfo( String userName ) throws NoSuchUserException {
    EntityWrapper<UserInfo> dbWrapper = new EntityWrapper<UserInfo>( );
    try {
      UserInfo userInfo = dbWrapper.getUnique( new UserInfo(userName) );
      dbWrapper.delete( userInfo );
      dbWrapper.commit( );
    } catch ( EucalyptusCloudException e1 ) {
      dbWrapper.rollback( );
      LOG.error( e1, e1 );
      throw new NoSuchUserException( "User info does not exist", e1 );  
    }    
  }

  @Override
  public UserInfo getUserInfo( UserInfo search ) throws NoSuchUserException {
    EntityWrapper<UserInfo> dbWrapper = new EntityWrapper<UserInfo>( );
    try {
      UserInfo userInfo = dbWrapper.getUnique( search );
      dbWrapper.commit( );
      return userInfo;
    } catch ( EucalyptusCloudException e ) {
      dbWrapper.rollback( );
      throw new NoSuchUserException( "User info does not exist", e );
    }
  }

  @Override
  public void updateUserInfo( String name, Tx<UserInfo> infoTx ) throws NoSuchUserException {
    EntityWrapper<UserInfo> dbWrapper = new EntityWrapper<UserInfo>( );
    try {
      UserInfo userInfo = dbWrapper.getUnique( new UserInfo(name) );
      infoTx.fire( userInfo );
      dbWrapper.commit( );
    } catch ( EucalyptusCloudException e ) {
      dbWrapper.rollback( );
      LOG.error( e, e );
      throw new NoSuchUserException( "User info does not exist", e );
    } catch ( Throwable t ) {
      dbWrapper.rollback( );
      LOG.error( t, t );
      throw new NoSuchUserException( "Error in updating user info", t );
    }
  }
}
