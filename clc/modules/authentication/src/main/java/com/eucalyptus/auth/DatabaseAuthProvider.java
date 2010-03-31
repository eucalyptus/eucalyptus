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
import org.bouncycastle.util.encoders.UrlBase64;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import com.eucalyptus.auth.crypto.Hmacs;
import com.eucalyptus.auth.group.Group;
import com.eucalyptus.auth.group.GroupProvider;
import com.eucalyptus.auth.group.Groups;
import com.eucalyptus.auth.group.NoSuchGroupException;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Depends;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.Resource;
import com.eucalyptus.entities.DatabaseUtil;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;

@Provides( resource = Resource.UserCredentials )
@Depends( resources = { Resource.Database } )
public class DatabaseAuthProvider implements UserProvider, GroupProvider {
  private static DatabaseAuthProvider singleton = new DatabaseAuthProvider( );
  private static Logger LOG = Logger.getLogger( DatabaseAuthProvider.class );
  static {
    //TODO FIXME TODO BROKEN FAIL: discover this at bootstrap time.
    Users.setUserProvider( singleton );
    Groups.setGroupProvider( singleton );
    //TODO FIXME TODO BROKEN FAIL: discover this at bootstrap time.
  }
  
  DatabaseAuthProvider( ) {}
  
  @Override
  public User addUser( String userName, Boolean isAdmin, Boolean isEnabled, String secretKey, String queryId ) throws UserExistsException {
    UserEntity newUser = new UserEntity( userName );
    newUser.setQueryId( queryId );
    newUser.setSecretKey( secretKey );
    newUser.setIsAdministrator( isAdmin );
    newUser.setIsEnabled( isEnabled );
    EntityWrapper<UserEntity> db = Credentials.getEntityWrapper( );
    try {
      db.add( newUser );
      db.commit( );
    } catch ( Throwable t ) {
      db.rollback( );
      throw new UserExistsException( t );
    }
    return newUser;
  }
  
  @Override
  public User deleteUser( String userName ) throws NoSuchUserException {
    UserEntity user = new UserEntity( userName );
    EntityWrapper<User> db = Credentials.getEntityWrapper( );
    try {
      User foundUser = db.getUnique( user );
      db.delete( foundUser );
      db.commit( );
      return foundUser;
    } catch ( Exception e ) {
      db.rollback( );
      throw new NoSuchUserException( e );
    }
  }
  
  @Override
  public User addUser( String userName, Boolean admin, Boolean enabled ) throws UserExistsException, UnsupportedOperationException {
    return this.addUser( userName, admin, enabled, Hmacs.generateQueryId( userName ), Hmacs.generateSecretKey( userName ) );
  }
  
  @Override
  public List<Group> lookupUserGroups( User user ) {
    List<Group> userGroups = Lists.newArrayList( );
    EntityWrapper<UserGroupEntity> db = new EntityWrapper<UserGroupEntity>( "eucalyptus_general" );
    try {
      UserInfo userInfo = db.recast( UserInfo.class ).getUnique( UserInfo.named( user.getName( ) ) );
      for ( UserGroupEntity g : db.query( new UserGroupEntity( ) ) ) {
        if ( g.belongs( userInfo ) ) {
          userGroups.add( new DatabaseWrappedGroup( g ) );
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
    EntityWrapper<UserGroupEntity> db = new EntityWrapper<UserGroupEntity>( "eucalyptus_general" );
    try {
      UserGroupEntity group = db.getUnique( new UserGroupEntity( groupName ) );
      db.commit( );
      return new DatabaseWrappedGroup( group );
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback( );
      throw new NoSuchGroupException( e );
    }
  }
  
  @Override
  public List<User> listAllUsers( ) {
    List<User> users = Lists.newArrayList( );
    EntityWrapper<UserEntity> db = Credentials.getEntityWrapper( );
    UserEntity searchUser = new UserEntity( );
    searchUser.setIsEnabled( true );
    UserEntity user = null;
    try {
      users.addAll( db.query( searchUser ) );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
    }
    return users;
  }
  
  @Override
  public List<User> listEnabledUsers( ) {
    List<User> users = Lists.newArrayList( );
    EntityWrapper<UserEntity> db = Credentials.getEntityWrapper( );
    UserEntity searchUser = new UserEntity( );
    searchUser.setIsEnabled( true );
    UserEntity user = null;
    try {
      users.addAll( db.query( searchUser ) );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
    }
    return users;
  }
  
  @Override
  public User lookupCertificate( X509Certificate cert ) throws NoSuchUserException {
    String certPem = new String( UrlBase64.encode( Hashes.getPemBytes( cert ) ) );
    UserEntity searchUser = new UserEntity( );
    X509Cert searchCert = new X509Cert( );
    searchCert.setPemCertificate( certPem );
    searchUser.setIsEnabled( true );
    Session session = DatabaseUtil.getEntityManagerFactory( Credentials.DB_NAME ).getSessionFactory( ).openSession( );
    try {
      Example qbeUser = Example.create( searchUser ).enableLike( MatchMode.EXACT );
      Example qbeCert = Example.create( searchCert ).enableLike( MatchMode.EXACT );
      List<User> users = ( List<User> ) session.createCriteria( User.class ).setCacheable( true ).add( qbeUser ).createCriteria( "certificates" )
                                               .setCacheable( true ).add( qbeCert ).list( );
      User ret = users.size( ) == 1 ? users.get( 0 ) : null;
      int size = users.size( );
      if ( ret != null ) {
        return ret;
      } else {
        throw new GeneralSecurityException( ( size == 0 ) ? "No user with the specified certificate." : "Multiple users with the same certificate." );
      }
    } catch ( Throwable t ) {
      throw new NoSuchUserException( t );
    } finally {
      try {
        session.close( );
      } catch ( Throwable t ) {}
    }
  }
  
  @Override
  public User lookupQueryId( String queryId ) throws NoSuchUserException {
    String userName = null;
    EntityWrapper<UserEntity> db = Credentials.getEntityWrapper( );
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
    return user;
  }
  
  @Override
  public User lookupUser( String userName ) throws NoSuchUserException {
    EntityWrapper<UserEntity> db = Credentials.getEntityWrapper( );
    UserEntity searchUser = new UserEntity( userName );
    UserEntity user = null;
    try {
      user = db.getUnique( searchUser );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      throw new NoSuchUserException( e );
    }
    return user;
  }

  @Override
  public Group addGroup( String groupName ) throws GroupExistsException {
    EntityWrapper<UserGroupEntity> db = Credentials.getEntityWrapper( );
    UserGroupEntity newGroup = new UserGroupEntity( groupName );
    try {
      db.add( newGroup );
      db.commit( );
    } catch ( Throwable t ) {
      db.rollback( );
      throw new GroupExistsException( t );
    }
    return new DatabaseWrappedGroup( newGroup );
  }
  
  

}
