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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.auth;

import java.security.cert.X509Certificate;
import java.util.List;
import javax.persistence.EntityManager;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.auth.api.AccountProvider;
import com.eucalyptus.auth.entities.AccessKeyEntity;
import com.eucalyptus.auth.entities.AccountEntity;
import com.eucalyptus.auth.entities.CertificateEntity;
import com.eucalyptus.auth.entities.GroupEntity;
import com.eucalyptus.auth.entities.UserEntity;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.X509CertHelper;
import com.eucalyptus.entities.EntityWrapper;
import com.google.common.collect.Lists;

/**
 * The authorization provider based on database storage. This class includes all the APIs to
 * create/delete/query Eucalyptus authorization entities.
 * 
 * @author wenye
 *
 */
public class DatabaseAuthProvider implements AccountProvider {
  
  private static Logger LOG = Logger.getLogger( DatabaseAuthProvider.class );
  
  public DatabaseAuthProvider( ) {
  }

  @Override
  public User lookupUserById( final String userId ) throws AuthException {
    if ( userId == null ) {
      throw new AuthException( AuthException.EMPTY_USER_ID );
    }
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      UserEntity user = db.getUnique( UserEntity.newInstanceWithId( userId ) );
      db.commit( );
      return new DatabaseUserProxy( user );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to find user by ID " + userId );
      throw new AuthException( AuthException.NO_SUCH_USER, e );
    }
  }
  
  /**
   * Lookup enabled user by its access key ID. Only return the user if the key is active.
   * 
   * @param keyId
   * @return
   * @throws AuthException
   */
  @Override
  public User lookupUserByAccessKeyId( String keyId ) throws AuthException {
    if ( keyId == null || "".equals( keyId) ) {
      throw new AuthException( "Empty key ID" );
    }
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      Example userExample = Example.create( new UserEntity( true ) ).enableLike( MatchMode.EXACT );
      @SuppressWarnings( "unchecked" )
      List<UserEntity> users = ( List<UserEntity> ) db
          .createCriteria( UserEntity.class ).setCacheable( true ).add( userExample )
          .createCriteria( "keys" ).setCacheable( true ).add( 
              Restrictions.and( Restrictions.idEq( keyId ), Restrictions.eq( "active", true ) ) )
          .list( );
      if ( users.size( ) != 1 ) {
        throw new AuthException( "Found " + users.size( ) + " user(s)" );
      }
      db.commit( );
      return new DatabaseUserProxy( users.get( 0 ) );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to find user with access key ID : " + keyId );
      throw new AuthException( AuthException.NO_SUCH_USER, e );
    }
  }
  
  /**
   * Lookup enabled user by its certificate. Only return the user if the certificate is active and not revoked.
   * 
   * @param cert
   * @return
   * @throws AuthException
   */
  @Override
  public User lookupUserByCertificate( X509Certificate cert ) throws AuthException {
    if ( cert == null ) {
      throw new AuthException( "Empty input cert" );
    }
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      Example userExample = Example.create( new UserEntity( true ) ).enableLike( MatchMode.EXACT );
      CertificateEntity searchCert = new CertificateEntity( X509CertHelper.fromCertificate( cert ) );
      searchCert.setActive( true );
      searchCert.setRevoked( false );
      Example certExample = Example.create( searchCert ).enableLike( MatchMode.EXACT );
      @SuppressWarnings( "unchecked" )
      List<UserEntity> users = ( List<UserEntity> ) db
          .createCriteria( UserEntity.class ).setCacheable( true ).add( userExample )
          .createCriteria( "certificates" ).setCacheable( true ).add( certExample )
          .list( );
      if ( users.size( ) != 1 ) {
        throw new AuthException( "Found " + users.size( ) + " user(s)" );
      }
      db.commit( );
      return new DatabaseUserProxy( users.get( 0 ) );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to find user with certificate : " + cert );
      throw new AuthException( AuthException.NO_SUCH_USER, e );
    }
  }
  
  @Override
  public Group lookupGroupById( final String groupId ) throws AuthException {
    if ( groupId == null ) {
      throw new AuthException( AuthException.EMPTY_GROUP_ID );
    }
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      GroupEntity group = db.getUnique( GroupEntity.newInstanceWithId( groupId ) );
      db.commit( );
      return new DatabaseGroupProxy( group );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to find group by ID " + groupId );
      throw new AuthException( AuthException.NO_SUCH_GROUP, e );
    }
  }
  
  /**
   * Add account admin user separately.
   * 
   * @param accountName
   * @return
   * @throws AuthException
   */
  @Override
  public Account addAccount( String accountName ) throws AuthException {
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    if ( DatabaseAuthUtils.checkAccountExists( accountName ) ) {
      throw new AuthException( AuthException.ACCOUNT_ALREADY_EXISTS );
    }
    AccountEntity account = new AccountEntity( accountName );
    EntityWrapper<AccountEntity> db = EntityWrapper.get( AccountEntity.class );
    try {
      db.add( account );
      db.commit( );
      return new DatabaseAccountProxy( account );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to add account " + accountName );
      throw new AuthException( AuthException.ACCOUNT_CREATE_FAILURE, e );
    }
  }
  
  @Override
  @SuppressWarnings( "unchecked" )
  public void deleteAccount( String accountName, boolean forceDeleteSystem, boolean recursive ) throws AuthException {
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    if ( !forceDeleteSystem && DatabaseAuthUtils.isSystemAccount( accountName ) ) {
      throw new AuthException( AuthException.DELETE_SYSTEM_ACCOUNT );
    }
    if ( !recursive && !DatabaseAuthUtils.isAccountEmpty( accountName ) ) {
      throw new AuthException( AuthException.ACCOUNT_DELETE_CONFLICT );
    }
    Example accountExample = Example.create( new AccountEntity( accountName ) ).enableLike( MatchMode.EXACT );
    Example groupExample = Example.create( new GroupEntity( true ) ).enableLike( MatchMode.EXACT );
    EntityWrapper<AccountEntity> db = EntityWrapper.get( AccountEntity.class );
    try {
      if ( recursive ) {
        List<GroupEntity> groups = ( List<GroupEntity> ) db
            .createCriteria( GroupEntity.class ).setCacheable( true )
            .createCriteria( "account" ).setCacheable( true ).add( accountExample )
            .list( );
        List<UserEntity> users = ( List<UserEntity> ) db
            .createCriteria( UserEntity.class ).setCacheable( true )
            .createCriteria( "groups" ).setCacheable( true ).add( groupExample )
            .createCriteria( "account" ).setCacheable( true ).add( accountExample )
            .list( );
        for ( GroupEntity g : groups ) {
          db.recast( GroupEntity.class ).delete( g );
        }
        for ( UserEntity u : users ) {
          db.recast( UserEntity.class ).delete( u );
        }
      }
      List<AccountEntity> accounts = ( List<AccountEntity> ) db
          .createCriteria( AccountEntity.class ).setCacheable( true ).add( accountExample )
          .list( );
      if ( accounts.size( ) != 1 ) {
        throw new AuthException( "Found " + accounts.size( ) + " account(s)" );
      }
      db.delete( accounts.get( 0 ) );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to delete account " + accountName );
      throw new AuthException( AuthException.NO_SUCH_ACCOUNT, e );
    }
  }  
  
  @Override
  public List<User> listAllUsers( ) throws AuthException {
    List<User> results = Lists.newArrayList( );
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      List<UserEntity> users = db.query( new UserEntity( ) );
      db.commit( );
      for ( UserEntity u : users ) {
        results.add( new DatabaseUserProxy( u ) );
      }
      return results;
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to get all users" );
      throw new AuthException( "Failed to get all users", e );
    }
  }
  
  @Override
  public List<Account> listAllAccounts( ) throws AuthException {
    List<Account> results = Lists.newArrayList( );
    EntityWrapper<AccountEntity> db = EntityWrapper.get( AccountEntity.class );
    try {
      for ( AccountEntity account : db.query( new AccountEntity( ) ) ) {
        results.add( new DatabaseAccountProxy( account ) );
      }
      db.commit( );
      return results;
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to get accounts" );
      throw new AuthException( "Failed to accounts", e );
    }
  }
  
  @Override
  public boolean shareSameAccount( String userId1, String userId2 ) {
    if ( userId1 == userId2 ) {
      return true;
    }
    if ( userId1 == null || userId2 == null ) {
      return false;
    }
    try {
      User user1 = lookupUserById( userId1 );
      User user2 = lookupUserById( userId2 );
      if ( user1.getAccount( ).getId( ).equals( user2.getAccount( ).getId( ) ) ) {
        return true;
      }
    } catch ( AuthException e ) {
      LOG.warn( "User(s) can not be found", e );
    }
    return false;
  }

  @Override
  public Certificate lookupCertificate( X509Certificate cert ) throws AuthException {
    if ( cert == null ) {
      throw new AuthException( "Empty input cert" );
    }
    EntityWrapper<CertificateEntity> db = EntityWrapper.get( CertificateEntity.class );
    try {
      CertificateEntity certEntity = db.getUnique( new CertificateEntity( X509CertHelper.fromCertificate( cert ) ) );
      db.commit( );
      return new DatabaseCertificateProxy( certEntity );
    }  catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to lookup cert " + cert );
      throw new AuthException( AuthException.NO_SUCH_CERTIFICATE, e );
    }
  }

  @Override
  public Account lookupAccountByName( String accountName ) throws AuthException {
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    EntityWrapper<AccountEntity> db = EntityWrapper.get( AccountEntity.class );
    try {
      Example accountExample = Example.create( new AccountEntity( accountName ) ).enableLike( MatchMode.EXACT );
      @SuppressWarnings( "unchecked" )
      List<AccountEntity> accounts = ( List<AccountEntity> ) db
          .createCriteria( AccountEntity.class ).setCacheable( true ).add( accountExample )
          .list( );
      if ( accounts.size( ) < 1 ) {
        throw new AuthException( "No matching account by name " + accountName );
      }
      db.commit( );
      return new DatabaseAccountProxy( accounts.get( 0 ) );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to find account " + accountName );
      throw new AuthException( "Failed to find account", e );
    }
  }

  @Override
  public Account lookupAccountById( final String accountId ) throws AuthException {
    if ( accountId == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_ID );
    }
    EntityWrapper<AccountEntity> db = EntityWrapper.get( AccountEntity.class );
    try {
      AccountEntity account = db.getUnique( AccountEntity.newInstanceWithId( accountId ) );
      db.commit( );
      return new DatabaseAccountProxy( account );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to find account " + accountId );
      throw new AuthException( "Failed to find account", e );
    }
  }

  @Override
  public AccessKey lookupAccessKeyById( final String keyId ) throws AuthException {
    if ( keyId == null ) {
      throw new AuthException( "Empty access key ID" );
    }
    EntityWrapper<AccessKeyEntity> db = EntityWrapper.get( AccessKeyEntity.class );
    try {
      AccessKeyEntity keyEntity = db.getUnique( AccessKeyEntity.newInstanceWithId( keyId ) );
      db.commit( );
      return new DatabaseAccessKeyProxy( keyEntity );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to find access key with ID " + keyId );
      throw new AuthException( "Failed to find access key", e );      
    }
  }
  
}
