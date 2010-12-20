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

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.auth.api.AccountProvider;
import com.eucalyptus.auth.api.GroupProvider;
import com.eucalyptus.auth.api.PolicyProvider;
import com.eucalyptus.auth.api.UserProvider;
import com.eucalyptus.auth.crypto.Crypto;
import com.eucalyptus.auth.crypto.Hmacs;
import com.eucalyptus.auth.entities.AccountEntity;
import com.eucalyptus.auth.entities.AuthorizationEntity;
import com.eucalyptus.auth.entities.CertificateEntity;
import com.eucalyptus.auth.entities.ConditionEntity;
import com.eucalyptus.auth.entities.GroupEntity;
import com.eucalyptus.auth.entities.PolicyEntity;
import com.eucalyptus.auth.entities.StatementEntity;
import com.eucalyptus.auth.entities.UserEntity;
import com.eucalyptus.auth.policy.PolicyParser;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Authorization.EffectType;
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
public class DatabaseAuthProvider implements UserProvider, GroupProvider, AccountProvider, PolicyProvider {
  
  private static Logger LOG = Logger.getLogger( DatabaseAuthProvider.class );
  
  public DatabaseAuthProvider( ) {
  }
  
  @Override
  public User addUser( String userName, String path, boolean skipRegistration, boolean enabled, Map<String, String> info,
                       boolean createKey, boolean createPassword, String accountName ) throws AuthException {
    checkUserName( userName );
    checkPath( path );
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    if ( checkUserExists( userName, accountName ) ) {
      throw new AuthException( AuthException.USER_ALREADY_EXISTS );
    }
    
    UserEntity newUser = new UserEntity( userName );
    newUser.setPath( path );
    newUser.setEnabled( enabled );
    if ( skipRegistration ) {
      newUser.setRegistrationStatus( User.RegistrationStatus.CONFIRMED );
    } else {
      newUser.setRegistrationStatus( User.RegistrationStatus.REGISTERED );
    }
    if ( info != null ) {
      newUser.getInfoMap( ).putAll( info );
    }
    newUser.setToken( Crypto.generateSessionToken( userName ) );
    newUser.setConfirmationCode( Crypto.generateSessionToken( userName ) );
    try {
      if ( createPassword ) {
        newUser.setPassword( Crypto.generateHashedPassword( userName ) );
      }
      if ( createKey ) {
        newUser.addSecretKey( Hmacs.generateSecretKey( userName ) );
      }
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to generate credentials for user: " + userName );
      throw new AuthException( "Failed to generate user credentials", e );
    }
    
    GroupEntity newGroup = new GroupEntity( getUserGroupName( userName ) );
    newGroup.setUserGroup( true );
    
    EntityWrapper<AccountEntity> db = EntityWrapper.get( AccountEntity.class );
    try {
      AccountEntity account = db.getUnique( new AccountEntity( accountName ) );
      db.recast( GroupEntity.class ).add( newGroup );
      db.recast( UserEntity.class ).add( newUser );
      newGroup.setAccount( account );
      newGroup.addMember( newUser );
      newUser.addGroup( newGroup );
      db.commit( );
      return new DatabaseUserProxy( newUser );
    } catch ( Throwable e ) {
      Debugging.logError( LOG, e, "Failed to add user: " + userName + " in " + accountName );
      db.rollback( );
      throw new AuthException( AuthException.USER_CREATE_FAILURE, e );
    }
  }
  
  public void deleteUser( String userName, String accountName, boolean forceDeleteAdmin, boolean recursive ) throws AuthException {
    if ( userName == null ) {
      throw new AuthException( AuthException.EMPTY_USER_NAME );
    }
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    if ( !forceDeleteAdmin && isAccountAdmin( userName ) ) {
      throw new AuthException( AuthException.DELETE_ACCOUNT_ADMIN );
    }
    if ( !recursive && userHasResourceAttached( userName, accountName ) ) {
      throw new AuthException( AuthException.USER_DELETE_CONFLICT );
    }
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      UserEntity user = getUniqueUser( db.getSession( ), userName, accountName );
      for ( Group g : user.getGroups( ) ) {
        GroupEntity ge = ( GroupEntity ) g;
        if ( ge.isUserGroup( ) ) {
          db.recast( GroupEntity.class ).delete( ge );
        } else {
          g.removeMember( user );
        }
      }
      db.delete( user );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to delete user: " + userName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_USER, e );
    }
  }
  
  @Override
  public User lookupUserByName( String userName, String accountName ) throws AuthException {
    if ( userName == null ) {
      throw new AuthException( AuthException.EMPTY_USER_NAME );
    }
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      UserEntity user = getUniqueUser( db.getSession( ), userName, accountName );
      db.commit( );
      return new DatabaseUserProxy( user );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to find user: " + userName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_USER, e );
    }
  }
  
  @Override
  public User lookupUserById( String userId ) throws AuthException {
    if ( userId == null ) {
      throw new AuthException( AuthException.EMPTY_USER_ID );
    }
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    EntityManager em = db.getEntityManager( );
    try {
      UserEntity user = em.find( UserEntity.class, userId );
      db.commit( );
      return new DatabaseUserProxy( user );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to find user by ID " + userId );
      throw new AuthException( AuthException.NO_SUCH_USER, e );
    }
  }
  
  @Override
  public User lookupSystemAdmin( ) throws AuthException {
    return lookupUserByName( User.ACCOUNT_ADMIN_USER_NAME, User.SYSTEM_ADMIN_ACCOUNT_NAME );
  }
  
  @Override
  public User lookupAccountAdmin( String accountName ) throws AuthException {
    return lookupUserByName( User.ACCOUNT_ADMIN_USER_NAME, accountName );
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
    Session session = db.getSession( );
    try {
      Example userExample = Example.create( new UserEntity( true ) ).enableLike( MatchMode.EXACT );
      @SuppressWarnings( "unchecked" )
      List<UserEntity> users = ( List<UserEntity> ) session
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
    Session session = db.getSession( );
    try {
      Example userExample = Example.create( new UserEntity( true ) ).enableLike( MatchMode.EXACT );
      CertificateEntity searchCert = new CertificateEntity( X509CertHelper.fromCertificate( cert ) );
      searchCert.setActive( true );
      searchCert.setRevoked( false );
      Example certExample = Example.create( searchCert ).enableLike( MatchMode.EXACT );
      @SuppressWarnings( "unchecked" )
      List<UserEntity> users = ( List<UserEntity> ) session
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
  public Group addGroup( String groupName, String path, String accountName ) throws AuthException {
    if ( groupName == null ) {
      throw new AuthException( AuthException.EMPTY_GROUP_NAME );
    }
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    checkPath( path );
    if ( checkGroupExists( groupName, accountName ) ) {
      throw new AuthException( AuthException.GROUP_ALREADY_EXISTS );
    }
    
    EntityWrapper<AccountEntity> db = EntityWrapper.get( AccountEntity.class );
    try {
      AccountEntity account = db.getUnique( new AccountEntity( accountName ) );
      GroupEntity group = new GroupEntity( groupName );
      group.setPath( path );
      group.setUserGroup( false );
      group.setAccount( account );
      db.recast( GroupEntity.class ).add( group );
      db.commit( );
      return new DatabaseGroupProxy( group );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to add group " + groupName + " in " + accountName );
      throw new AuthException( AuthException.GROUP_CREATE_FAILURE, e );
    }
  }
  
  @Override
  public void deleteGroup( String groupName, String accountName, boolean recursive ) throws AuthException {
    if ( groupName == null ) {
      throw new AuthException( AuthException.EMPTY_GROUP_NAME );
    }
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    if ( isUserGroupName( groupName ) ) {
      throw new AuthException( AuthException.USER_GROUP_DELETE );
    }
    if ( !recursive && groupHasResourceAttached( groupName, accountName ) ) {
      throw new AuthException( AuthException.GROUP_DELETE_CONFLICT );
    }
    
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      GroupEntity group = getUniqueGroup( db.getSession( ), groupName, accountName );
      db.delete( group );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to delete group " + groupName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_GROUP, e );
    }
  }
  
  @Override
  public Group lookupGroupByName( String groupName, String accountName ) throws AuthException {
    if ( groupName == null ) {
      throw new AuthException( AuthException.EMPTY_GROUP_NAME );
    }
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    Session session = db.getSession( );
    try {
      GroupEntity group = getUniqueGroup( session, groupName, accountName );
      db.commit( );
      return new DatabaseGroupProxy( group );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to get group " + groupName + " for " + accountName );
      throw new AuthException( "Failed to get group", e );
    }
  }
  
  @Override
  public Group lookupGroupById( String groupId ) throws AuthException {
    if ( groupId == null ) {
      throw new AuthException( AuthException.EMPTY_GROUP_ID );
    }
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    EntityManager em = db.getEntityManager( );
    try {
      GroupEntity group = em.find( GroupEntity.class, groupId );
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
    if ( checkAccountExists( accountName ) ) {
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
  public void deleteAccount( String accountName ) throws AuthException {
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    if ( !isAccountEmpty( accountName ) ) {
      throw new AuthException( AuthException.ACCOUNT_DELETE_CONFLICT );
    }
    EntityWrapper<AccountEntity> db = EntityWrapper.get( AccountEntity.class );
    try {
      AccountEntity account = getUniqueAccount( db.getSession( ), accountName );
      db.delete( account );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to delete account " + accountName );
      throw new AuthException( AuthException.NO_SUCH_ACCOUNT, e );
    }
  }
  
  @Override
  public List<Group> listAllGroups( String accountName ) throws AuthException {
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    if ( !checkAccountExists( accountName ) ) {
      throw new AuthException( AuthException.NO_SUCH_ACCOUNT );
    }
    List<Group> results = Lists.newArrayList( );
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    Session session = db.getSession( );
    try {
      Example accountExample = Example.create( new AccountEntity( accountName ) );
      Example groupExample = Example.create( new GroupEntity( false ) );
      @SuppressWarnings( "unchecked" )
      List<GroupEntity> groups = ( List<GroupEntity> ) session
          .createCriteria( GroupEntity.class ).setCacheable( true ).add( groupExample )
          .createCriteria( "account" ).setCacheable( true ).add( accountExample )
          .list( );
      db.commit( );
      for ( GroupEntity g : groups ) {
        results.add( new DatabaseGroupProxy( g ) );
      }
      return results;
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to get groups for " + accountName );
      throw new AuthException( "Failed to get groups", e );
    }
  }
  
  @Override
  public List<User> listAllUsers( String accountName ) throws AuthException {
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    if ( !checkAccountExists( accountName ) ) {
      throw new AuthException( AuthException.NO_SUCH_ACCOUNT );
    }
    List<User> results = Lists.newArrayList( );
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    Session session = db.getSession( );
    try {
      Example accountExample = Example.create( new AccountEntity( accountName ) );
      Example groupExample = Example.create( new GroupEntity( true ) );
      @SuppressWarnings( "unchecked" )
      List<UserEntity> users = ( List<UserEntity> ) session
          .createCriteria( UserEntity.class ).setCacheable( true )
          .createCriteria( "groups" ).setCacheable( true ).add( groupExample )
          .createCriteria( "account" ).setCacheable( true ).add( accountExample )
          .list( );
      db.commit( );
      for ( UserEntity u : users ) {
        results.add( new DatabaseUserProxy( u ) );
      }
      return results;
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to get users for " + accountName );
      throw new AuthException( "Failed to get users for account", e );
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
  public String attachGroupPolicy( String policy, String groupName, String accountName ) throws AuthException, PolicyException {
    if ( groupName == null ) {
      throw new AuthException( AuthException.EMPTY_GROUP_NAME );
    }
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    PolicyEntity parsedPolicy = PolicyParser.getInstance( ).parse( policy );
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      GroupEntity group = getUniqueGroup( db.getSession( ), groupName, accountName );
      db.recast( PolicyEntity.class ).add( parsedPolicy );
      parsedPolicy.setGroup( group );
      for ( StatementEntity statement : parsedPolicy.getStatements( ) ) {
        db.recast( StatementEntity.class ).add( statement );
        statement.setPolicy( parsedPolicy );
        for ( AuthorizationEntity auth : statement.getAuthorizations( ) ) {
          db.recast( AuthorizationEntity.class ).add( auth );
          auth.setStatement( statement );
        }
        for ( ConditionEntity cond : statement.getConditions( ) ) {
          db.recast( ConditionEntity.class ).add( cond );
          cond.setStatement( statement );
        }
      }
      db.commit( );
      return parsedPolicy.getId( );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to attach policy for " + groupName + " in " + accountName);
      throw new AuthException( "Failed to attach policy", e );
    }
  }
  
  @Override
  public String attachUserPolicy( String policy, String userName, String accountName ) throws AuthException, PolicyException {
    return this.attachGroupPolicy( policy, getUserGroupName( userName ), accountName );
  }
  
  @Override
  public void removeGroupPolicy( String policyId, String groupName, String accountName ) throws AuthException {
    if ( policyId == null ) {
      throw new AuthException( "Empty policy ID" );
    }
    if ( groupName == null ) {
      throw new AuthException( AuthException.EMPTY_GROUP_NAME );
    }
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    EntityManager em = db.getEntityManager( );
    try {
      GroupEntity group = getUniqueGroup( db.getSession( ), groupName, accountName );
      PolicyEntity policy = em.find( PolicyEntity.class, policyId );
      group.getPolicies( ).remove( policy );
      db.recast( PolicyEntity.class ).delete( policy );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to remove policy for " + groupName + " in " + accountName);
      throw new AuthException( "Failed to remove policy", e );
    }
  }
  
  @Override
  public List<? extends Authorization> lookupAuthorizations( String resourceType, String userId ) throws AuthException {
    if ( resourceType == null ) {
      throw new AuthException( "Empty resource type" );
    }
    if ( userId == null ) {
      throw new AuthException( AuthException.EMPTY_USER_ID );
    }
    EntityWrapper<AuthorizationEntity> db = EntityWrapper.get( AuthorizationEntity.class );
    Session session = db.getSession( );
    try {
      @SuppressWarnings( "unchecked" )
      List<AuthorizationEntity> authorizations = ( List<AuthorizationEntity> ) session
          .createCriteria( AuthorizationEntity.class ).setCacheable( true ).add(
              Restrictions.and(
                  Restrictions.eq( "type", resourceType ),
                  Restrictions.or( 
                      Restrictions.eq( "effect", EffectType.Allow ),
                      Restrictions.eq( "effect", EffectType.Deny ) ) ) )
          .createCriteria( "statement" ).setCacheable( true )
          .createCriteria( "policy" ).setCacheable( true )
          .createCriteria( "group" ).setCacheable( true )
          .createCriteria( "users" ).setCacheable( true ).add(Restrictions.idEq( userId ) )
          .list( );
      db.commit( );
      List<DatabaseAuthorizationProxy> results = Lists.newArrayList( );
      for ( AuthorizationEntity auth : authorizations ) {
        results.add( new DatabaseAuthorizationProxy( auth ) );
      }
      return results;
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to lookup authorization for user with ID " + userId + ", type=" + resourceType);
      throw new AuthException( "Failed to lookup auth", e );
    }
  }
  
  @Override
  public List<? extends Authorization> lookupAccountGlobalAuthorizations( String resourceType, String accountId ) throws AuthException {
    if ( resourceType == null ) {
      throw new AuthException( "Empty resource type" );
    }
    if ( accountId == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_ID );
    }
    GroupEntity searchGroup = new GroupEntity( getUserGroupName( User.ACCOUNT_ADMIN_USER_NAME ) );
    EntityWrapper<AuthorizationEntity> db = EntityWrapper.get( AuthorizationEntity.class );
    Session session = db.getSession( );
    try {
      Example groupExample = Example.create( searchGroup ).enableLike( MatchMode.EXACT );
      @SuppressWarnings( "unchecked" )
      List<AuthorizationEntity> authorizations = ( List<AuthorizationEntity> ) session
          .createCriteria( AuthorizationEntity.class ).setCacheable( true ).add(
              Restrictions.and(
                  Restrictions.eq( "type", resourceType ),
                  Restrictions.or( 
                      Restrictions.eq( "effect", EffectType.Allow ),
                      Restrictions.eq( "effect", EffectType.Deny ) ) ) )
          .createCriteria( "statement" ).setCacheable( true )
          .createCriteria( "policy" ).setCacheable( true )
          .createCriteria( "group" ).setCacheable( true ).add( groupExample )
          .createCriteria( "account" ).setCacheable( true ).add( Restrictions.idEq( accountId ) )
          .list( );
      db.commit( );
      List<DatabaseAuthorizationProxy> results = Lists.newArrayList( );
      for ( AuthorizationEntity auth : authorizations ) {
        results.add( new DatabaseAuthorizationProxy( auth ) );
      }
      return results;
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to lookup global authorization for account " + accountId + ", type=" + resourceType);
      throw new AuthException( "Failed to lookup account global auth", e );
    }
  }
  
  @Override
  public List<? extends Authorization> lookupQuotas( String resourceType, String userId ) throws AuthException {
    EntityWrapper<AuthorizationEntity> db = EntityWrapper.get( AuthorizationEntity.class );
    Session session = db.getSession( );
    try {
      @SuppressWarnings( "unchecked" )
      List<AuthorizationEntity> authorizations = ( List<AuthorizationEntity> ) session
          .createCriteria( AuthorizationEntity.class ).setCacheable( true ).add(
              Restrictions.and(
                  Restrictions.eq( "type", resourceType ),
                  Restrictions.eq( "effect", EffectType.Limit ) ) )
          .createCriteria( "statement" ).setCacheable( true )
          .createCriteria( "policy" ).setCacheable( true )
          .createCriteria( "group" ).setCacheable( true )
          .createCriteria( "users" ).add(Restrictions.idEq( userId ) )
          .list( );
      db.commit( );
      List<DatabaseAuthorizationProxy> results = Lists.newArrayList( );
      for ( AuthorizationEntity auth : authorizations ) {
        results.add( new DatabaseAuthorizationProxy( auth ) );
      }
      return results;
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to lookup quotas for user with ID " + userId + ", type=" + resourceType);
      throw new AuthException( "Failed to lookup quota", e );
    }
  }
  
  @Override
  public List<? extends Authorization> lookupAccountGlobalQuotas( String resourceType, String accountId ) throws AuthException {
    if ( resourceType == null ) {
      throw new AuthException( "Empty resource type" );
    }
    if ( accountId == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    GroupEntity searchGroup = new GroupEntity( getUserGroupName( User.ACCOUNT_ADMIN_USER_NAME ) );
    EntityWrapper<AuthorizationEntity> db = EntityWrapper.get( AuthorizationEntity.class );
    Session session = db.getSession( );
    try {
      Example groupExample = Example.create( searchGroup ).enableLike( MatchMode.EXACT );
      @SuppressWarnings( "unchecked" )
      List<AuthorizationEntity> authorizations = ( List<AuthorizationEntity> ) session
          .createCriteria( AuthorizationEntity.class ).setCacheable( true ).add(
              Restrictions.and(
                  Restrictions.eq( "type", resourceType ),
                  Restrictions.eq( "effect", EffectType.Limit ) ) )
          .createCriteria( "statement" ).setCacheable( true )
          .createCriteria( "policy" ).setCacheable( true )
          .createCriteria( "group" ).setCacheable( true ).add( groupExample )
          .createCriteria( "account" ).setCacheable( true ).add( Restrictions.idEq( accountId ) )
          .list( );
      db.commit( );
      List<DatabaseAuthorizationProxy> results = Lists.newArrayList( );
      for ( AuthorizationEntity auth : authorizations ) {
        results.add( new DatabaseAuthorizationProxy( auth ) );
      }
      return results;
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to lookup global quota for account " + accountId + ", type=" + resourceType);
      throw new AuthException( "Failed to lookup account global quota", e );
    }
  }
  
  @Override
  public boolean isCertificateActive( X509Certificate cert ) throws AuthException {
    if ( cert == null ) {
      throw new AuthException( "Empty input cert" );
    }
    EntityWrapper<CertificateEntity> db = EntityWrapper.get( CertificateEntity.class );
    try {
      CertificateEntity certEntity = db.getUnique( new CertificateEntity( X509CertHelper.fromCertificate( cert ) ) );
      db.commit( );
      return certEntity.isActive( ) && !certEntity.isRevoked( );
    }  catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to lookup cert " + cert );
      throw new AuthException( AuthException.NO_SUCH_CERTIFICATE, e );
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
      if ( user1.getAccount( ).getAccountId( ).equals( user2.getAccount( ).getAccountId( ) ) ) {
        return true;
      }
    } catch ( AuthException e ) {
      LOG.warn( "User(s) can not be found", e );
    }
    return false;
  }

  @Override
  public void addSystemAccount( ) throws AuthException {
    this.addAccount( User.SYSTEM_ADMIN_ACCOUNT_NAME );
  }
  
  @Override
  public void addSystemAdmin( ) throws AuthException {
    this.addUser( User.ACCOUNT_ADMIN_USER_NAME, "/", true, true, null, true, true, User.SYSTEM_ADMIN_ACCOUNT_NAME );
  }
  
  @Override
  public void addAccountAdmin( String accountName, String password ) throws AuthException {
    User admin = this.addUser( User.ACCOUNT_ADMIN_USER_NAME, "/", true, true, null, true, true, accountName );
    admin.setPassword( password );
  }
  
  /**
   * Must call within a transaction.
   * 
   * @param session
   * @param userName
   * @param accountName
   * @return
   */
  public static UserEntity getUniqueUser( Session session, String userName, String accountName ) throws Exception {
    Example accountExample = Example.create( new AccountEntity( accountName ) ).enableLike( MatchMode.EXACT );
    Example groupExample = Example.create( new GroupEntity( true ) ).enableLike( MatchMode.EXACT );
    Example userExample = Example.create( new UserEntity( userName ) ).enableLike( MatchMode.EXACT );
    @SuppressWarnings( "unchecked" )
    List<UserEntity> users = ( List<UserEntity> ) session
        .createCriteria( UserEntity.class ).setCacheable( true ).add( userExample )
        .createCriteria( "groups" ).setCacheable( true ).add( groupExample )
        .createCriteria( "account" ).setCacheable( true ).add( accountExample )
        .list( );
    if ( users.size( ) != 1 ) {
      throw new AuthException( "Found " + users.size( ) + " user(s)" );
    }
    return users.get( 0 );
  }
  
  /**
   * Must call within a transaction.
   * 
   * @param session
   * @param groupName
   * @param accountName
   * @return
   * @throws Exception
   */
  public static GroupEntity getUniqueGroup( Session session, String groupName, String accountName ) throws Exception {
    Example accountExample = Example.create( new AccountEntity( accountName ) ).enableLike( MatchMode.EXACT );
    Example groupExample = Example.create( new GroupEntity( groupName ) ).enableLike( MatchMode.EXACT );
    @SuppressWarnings( "unchecked" )
    List<GroupEntity> groups = ( List<GroupEntity> ) session
        .createCriteria( GroupEntity.class ).setCacheable( true ).add( groupExample )
        .createCriteria( "account" ).setCacheable( true ).add( accountExample )
        .list( );
    if ( groups.size( ) != 1 ) {
      throw new AuthException( "Found " + groups.size( ) + " group(s)" );
    }
    return groups.get( 0 );
  }
  
  /**
   * Must call within a transaction.
   * 
   * @param session
   * @param accountName
   * @return
   * @throws Exception
   */
  public static AccountEntity getUniqueAccount( Session session, String accountName ) throws Exception {
    Example accountExample = Example.create( new AccountEntity( accountName ) ).enableLike( MatchMode.EXACT );
    @SuppressWarnings( "unchecked" )
    List<AccountEntity> accounts = ( List<AccountEntity> ) session
        .createCriteria( AccountEntity.class ).setCacheable( true ).add( accountExample )
        .list( );
    if ( accounts.size( ) != 1 ) {
      throw new AuthException( "Found " + accounts.size( ) + " account(s)" );
    }
    return accounts.get( 0 );
  }
  
  /**
   * Check if user still has resource, e.g. groups, keys, certs and policies, attached.
   * 
   * @param userName
   * @param accountName
   * @return
   * @throws AuthException
   */
  private boolean userHasResourceAttached( String userName, String accountName ) throws AuthException {
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      UserEntity user = getUniqueUser( db.getSession( ), userName, accountName );
      GroupEntity userGroup = getUniqueGroup( db.getSession( ), getUserGroupName( userName ), accountName );
      db.commit( );
      return ( user.getGroups( ).size( ) > 1
          || user.getAccessKeys( ).size( ) > 0
          || user.getCertificates( ).size( ) > 0
          || userGroup.getPolicies( ).size( ) > 0 );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to check user " + userName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_USER, e );
    }
  }
  
  /**
   * Check if group still has resource, e.g. users and policies, attached.
   * 
   * @param groupName
   * @param accountName
   * @return
   * @throws AuthException
   */
  private boolean groupHasResourceAttached( String groupName, String accountName ) throws AuthException {
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      GroupEntity group = getUniqueGroup( db.getSession( ), groupName, accountName );
      db.commit( );
      return ( group.getUsers( ).size( ) > 0 || group.getPolicies( ).size( ) > 0 );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to check group " + groupName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_GROUP, e );
    }
  }
  
  /**
   * Check if the acount is empty (no groups, no users).
   * 
   * @param accountName
   * @return
   * @throws AuthException
   */
  private boolean isAccountEmpty( String accountName ) throws AuthException {
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    Session session = db.getSession( );
    try {
      Example accountExample = Example.create( new AccountEntity( accountName ) ).enableLike( MatchMode.EXACT );
      @SuppressWarnings( "unchecked" )
      List<GroupEntity> groups = ( List<GroupEntity> ) session
          .createCriteria( GroupEntity.class ).setCacheable( true )
          .createCriteria( "account" ).setCacheable( true ).add( accountExample )
          .list( );
      db.commit( );
      return groups.size( ) == 0;
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to check groups for account " + accountName );
      throw new AuthException( "Failed to check groups for account", e );
    }
  }
  
  /**
   * Check if an account exists.
   * 
   * @param accountName
   * @return
   * @throws AuthException
   */
  private boolean checkAccountExists( String accountName ) throws AuthException {
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    EntityWrapper<AccountEntity> db = EntityWrapper.get( AccountEntity.class );
    Session session = db.getSession( );
    try {
      Example accountExample = Example.create( new AccountEntity( accountName ) ).enableLike( MatchMode.EXACT );
      @SuppressWarnings( "unchecked" )
      List<AccountEntity> accounts = ( List<AccountEntity> ) session
          .createCriteria( AccountEntity.class ).setCacheable( true ).add( accountExample )
          .list( );
      db.commit( );
      return accounts.size( ) > 0;
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to check account " + accountName );
      throw new AuthException( "Failed to check account", e );
    }
  }
  
  /**
   * Check if a group exists.
   * 
   * @param groupName
   * @param accountName
   * @return
   * @throws AuthException
   */
  private boolean checkGroupExists( String groupName, String accountName ) throws AuthException {
    if ( groupName == null) {
      throw new AuthException( AuthException.EMPTY_GROUP_NAME );
    }  
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    Session session = db.getSession( );
    try {
      Example accountExample = Example.create( new AccountEntity( accountName ) ).enableLike( MatchMode.EXACT );
      Example groupExample = Example.create( new GroupEntity( groupName ) ).enableLike( MatchMode.EXACT );
      @SuppressWarnings( "unchecked" )
      List<GroupEntity> groups = ( List<GroupEntity> ) session
          .createCriteria( GroupEntity.class ).setCacheable( true ).add( groupExample )
          .createCriteria( "account" ).setCacheable( true ).add( accountExample )
          .list( );
      db.commit( );
      return groups.size( ) > 0;
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to check group " + groupName + " in " + accountName );
      throw new AuthException( "Failed to check group", e );
    }
  }
  
  /**
   * Check if a user exists.
   * 
   * @param userName
   * @param accountName
   * @return
   * @throws AuthException
   */
  private boolean checkUserExists( String userName, String accountName ) throws AuthException {
    if ( userName == null || accountName == null ) {
      throw new AuthException( "Empty user name or account name" );
    }
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    Session session = db.getSession( );
    try {
      Example accountExample = Example.create( new AccountEntity( accountName ) ).enableLike( MatchMode.EXACT );
      Example groupExample = Example.create( new GroupEntity( true ) ).enableLike( MatchMode.EXACT );
      Example userExample = Example.create( new UserEntity( userName ) ).enableLike( MatchMode.EXACT );
      @SuppressWarnings( "unchecked" )
      List<UserEntity> users = ( List<UserEntity> ) session
          .createCriteria( UserEntity.class ).setCacheable( true ).add( userExample )
          .createCriteria( "groups" ).setCacheable( true ).add( groupExample )
          .createCriteria( "account" ).setCacheable( true ).add( accountExample )
          .list( );
      db.commit( );
      return users.size( ) > 0;
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to check user " + userName + " in " + accountName );
      throw new AuthException( "Failed to check user", e );
    }
  }
  
  private static boolean isSystemAdmin( String accountName ) {
    return User.SYSTEM_ADMIN_ACCOUNT_NAME.equals( accountName );
  }
  
  private static boolean isAccountAdmin( String userName ) {
    return User.ACCOUNT_ADMIN_USER_NAME.equals( userName );
  }
  
  public static String getUserGroupName( String userName ) {
    return User.USER_GROUP_PREFIX + userName;
  }
  
  private static boolean isUserGroupName( String groupName ) {
    return groupName.startsWith( User.USER_GROUP_PREFIX );
  }
  
  /**
   * Check if the user name follows the IAM spec.
   * http://docs.amazonwebservices.com/IAM/latest/UserGuide/index.html?Using_Identifiers.html
   * 
   * @param userName
   * @throws AuthException
   */
  private static void checkUserName( String userName ) throws AuthException {
    if ( userName == null || "".equals( userName ) ) {
      throw new AuthException( "Empty user name" );
    }
    for ( int i = 0; i < userName.length( ); i++ ) {
      char c = userName.charAt( i );
      if ( !Character.isLetterOrDigit( c ) 
          && c != '+' && c != '=' && c != ',' && c != '.' && c != '@' && c != '-' ) {
        throw new AuthException( "Invalid character in user name: " + c );
      }
    }
  }
  
  /**
   * Check if the path follows the IAM spec.
   * http://docs.amazonwebservices.com/IAM/latest/UserGuide/index.html?Using_Identifiers.html
   * 
   * @param path
   * @throws AuthException
   */
  private static void checkPath( String path ) throws AuthException {
    if ( path != null && !path.startsWith( "/" ) ) {
      throw new AuthException( "Invalid path: " + path );
    }
  }
  
}
