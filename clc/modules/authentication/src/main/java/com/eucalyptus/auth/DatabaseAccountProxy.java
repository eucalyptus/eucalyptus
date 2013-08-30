/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth;

import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.auth.checker.InvalidValueException;
import com.eucalyptus.auth.checker.ValueChecker;
import com.eucalyptus.auth.checker.ValueCheckerFactory;
import com.eucalyptus.auth.entities.AccountEntity;
import com.eucalyptus.auth.entities.AuthorizationEntity;
import com.eucalyptus.auth.entities.CertificateEntity;
import com.eucalyptus.auth.entities.GroupEntity;
import com.eucalyptus.auth.entities.InstanceProfileEntity;
import com.eucalyptus.auth.entities.PolicyEntity;
import com.eucalyptus.auth.entities.RoleEntity;
import com.eucalyptus.auth.entities.UserEntity;
import com.eucalyptus.auth.policy.PolicyParser;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.InstanceProfile;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.Authorization.EffectType;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Lists;

public class DatabaseAccountProxy implements Account {

  private static final long serialVersionUID = 1L;

  private static Logger LOG = Logger.getLogger( DatabaseAccountProxy.class );

  private static final ValueChecker ACCOUNT_NAME_CHECKER = ValueCheckerFactory.createAccountNameChecker( );
  private static final ValueChecker USER_GROUP_NAME_CHECKER = ValueCheckerFactory.createUserAndGroupNameChecker( );
  private static final ValueChecker PATH_CHECKER = ValueCheckerFactory.createPathChecker( );

  private AccountEntity delegate;
  
  public DatabaseAccountProxy( AccountEntity delegate ) {
    this.delegate = delegate;
  }

  @Override
  public String getName( ) {
    return this.delegate.getName( );
  }
  
  @Override
  public String toString( ) {
    return this.delegate.toString( );
  }

  @Override
  public String getAccountNumber( ) {
    return this.delegate.getAccountNumber( );
  }

  @Override
  public String getCanonicalId() {
    return this.delegate.getCanonicalId();
  }

    @Override
  public void setName( final String name ) throws AuthException {
    try {
      ACCOUNT_NAME_CHECKER.check( name );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, "Invalid account name " + name );
      throw new AuthException( AuthException.INVALID_NAME, e );
    }
    try {
      // try finding the account with the same name to change to
      ( new DatabaseAuthProvider( ) ).lookupAccountByName( name );
    } catch ( AuthException ae ) {
      try {
        // not found
        DatabaseAuthUtils.invokeUnique( AccountEntity.class, "accountNumber", this.delegate.getAccountNumber( ), new Tx<AccountEntity>( ) {
          public void fire( AccountEntity t ) {
            t.setName( name );
          }
        } );
      } catch ( Exception e ) {
        Debugging.logError( LOG, e, "Failed to setName for " + this.delegate );
        throw new AuthException( e );
      }
      return;
    }
    // found
    throw new AuthException( AuthException.ACCOUNT_ALREADY_EXISTS );
  }

  @Override
  public List<User> getUsers( ) throws AuthException {
    List<User> results = Lists.newArrayList( );
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      @SuppressWarnings( "unchecked" )
      List<UserEntity> users = ( List<UserEntity> ) db
          .createCriteria( UserEntity.class ).setCacheable( true )
          .createCriteria( "groups" ).setCacheable( true ).add( Restrictions.eq( "userGroup", true ) )
          .createCriteria( "account" ).setCacheable( true ).add( Restrictions.eq( "name", this.delegate.getName( ) ) )
          .list( );
      db.commit( );
      for ( UserEntity u : users ) {
        results.add( new DatabaseUserProxy( u ) );
      }
      return results;
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to get users for " + this.delegate.getName( ) );
      throw new AuthException( "Failed to get users for account", e );
    }
  }

  @Override
  public List<Group> getGroups( ) throws AuthException {
    List<Group> results = Lists.newArrayList( );
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      @SuppressWarnings( "unchecked" )
      List<GroupEntity> groups = ( List<GroupEntity> ) db
          .createCriteria( GroupEntity.class ).setCacheable( true ).add( Restrictions.eq( "userGroup", false ) )
          .createCriteria( "account" ).setCacheable( true ).add( Restrictions.eq( "name", this.delegate.getName( ) ) )
          .list( );
      db.commit( );
      for ( GroupEntity g : groups ) {
        results.add( new DatabaseGroupProxy( g ) );
      }
      return results;
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to get groups for " + this.delegate.getName( ) );
      throw new AuthException( "Failed to get groups", e );
    }
  }

  @Override
  public List<Role> getRoles( ) throws AuthException {
    final List<Role> results = Lists.newArrayList( );
    final EntityWrapper<RoleEntity> db = EntityWrapper.get( RoleEntity.class );
    try {
      @SuppressWarnings( "unchecked" )
      List<RoleEntity> roles = ( List<RoleEntity> ) db
          .createCriteria( RoleEntity.class )
          .createCriteria( "account" ).add( Restrictions.eq( "name", this.delegate.getName( ) ) )
          .setCacheable( true )
          .list( );
      for ( final RoleEntity role : roles ) {
        results.add( new DatabaseRoleProxy( role ) );
      }
      return results;
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get roles for " + this.delegate.getName( ) );
      throw new AuthException( "Failed to get roles", e );
    } finally {
      if ( db.isActive() ) db.rollback();
    }
  }

  @Override
  public List<InstanceProfile> getInstanceProfiles() throws AuthException {
    final List<InstanceProfile> results = Lists.newArrayList( );
    final EntityWrapper<InstanceProfileEntity> db = EntityWrapper.get( InstanceProfileEntity.class );
    try {
      @SuppressWarnings( "unchecked" )
      List<InstanceProfileEntity> instanceProfiles = ( List<InstanceProfileEntity> ) db
          .createCriteria( InstanceProfileEntity.class )
          .createCriteria( "account" ).add( Restrictions.eq( "name", this.delegate.getName( ) ) )
          .setCacheable( true )
          .list( );
      for ( final InstanceProfileEntity instanceProfile : instanceProfiles ) {
        results.add( new DatabaseInstanceProfileProxy( instanceProfile  ) );
      }
      return results;
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get instance profiles for " + this.delegate.getName( ) );
      throw new AuthException( "Failed to get instance profiles", e );
    } finally {
      if ( db.isActive() ) db.rollback();
    }
  }

  @Override
  public User addUser( String userName, String path, boolean skipRegistration, boolean enabled, Map<String, String> info ) throws AuthException {
    try {
      USER_GROUP_NAME_CHECKER.check( userName );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, "Invalid user name " + userName );
      throw new AuthException( AuthException.INVALID_NAME, e );
    }
    try {
      PATH_CHECKER.check( path );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, "Invalid path " + path );
      throw new AuthException( AuthException.INVALID_PATH, e );
    }
    if ( DatabaseAuthUtils.checkUserExists( userName, this.delegate.getName( ) ) ) {
      throw new AuthException( AuthException.USER_ALREADY_EXISTS );
    }
    UserEntity newUser = new UserEntity( userName );
    newUser.setPath( path );
    newUser.setEnabled( enabled );
    newUser.setPasswordExpires( System.currentTimeMillis( ) + User.PASSWORD_LIFETIME );
    if ( skipRegistration ) {
      newUser.setRegistrationStatus( User.RegistrationStatus.CONFIRMED );
    } else {
      newUser.setRegistrationStatus( User.RegistrationStatus.REGISTERED );
    }
    if ( info != null ) {
      newUser.getInfo( ).putAll( info );
    }
    newUser.setToken( Crypto.generateSessionToken() );
    //newUser.setConfirmationCode( Crypto.generateSessionToken( userName ) );
    GroupEntity newGroup = new GroupEntity( DatabaseAuthUtils.getUserGroupName( userName ) );
    newGroup.setUserGroup( true );
    EntityWrapper<AccountEntity> db = EntityWrapper.get( AccountEntity.class );
    try {
      AccountEntity account = DatabaseAuthUtils.getUnique( db, AccountEntity.class, "name", this.delegate.getName( ) );
      newGroup = db.recast( GroupEntity.class ).merge( newGroup );
      newUser = db.recast( UserEntity.class ).merge( newUser );
      newGroup.setAccount( account );
      newGroup.getUsers( ).add( newUser );
      newUser.getGroups( ).add( newGroup );
      db.commit( );
      return new DatabaseUserProxy( newUser );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to add user: " + userName + " in " + this.delegate.getName( ) );
      db.rollback( );
      throw new AuthException( AuthException.USER_CREATE_FAILURE, e );
    }
  }
  
  private boolean userHasResourceAttached( String userName, String accountName ) throws AuthException {
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      UserEntity user = DatabaseAuthUtils.getUniqueUser( db, userName, accountName );
      GroupEntity userGroup = DatabaseAuthUtils.getUniqueGroup( db.recast( GroupEntity.class ), DatabaseAuthUtils.getUserGroupName( userName ), accountName );
      boolean result = ( user.getGroups( ).size( ) > 1
          || user.getKeys( ).size( ) > 0
          || getCurrentCertificateNumber( user.getCertificates( ) ) > 0
          || userGroup.getPolicies( ).size( ) > 0 );
      db.commit( );
      return result;
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to check user " + userName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_USER, e );
    }
  }

  private boolean roleHasResourceAttached( String roleName, String accountName ) throws AuthException {
    final EntityWrapper<RoleEntity> db = EntityWrapper.get( RoleEntity.class );
    try {
      final RoleEntity roleEntity = DatabaseAuthUtils.getUniqueRole( db, roleName, accountName );
      return roleEntity.getPolicies( ).size( ) > 0;
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to check role " + roleName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_ROLE, e );
    } finally {
      if ( db.isActive() ) db.rollback();
    }
  }

  private static int getCurrentCertificateNumber( List<CertificateEntity> certs ) {
    int num = 0;
    for ( CertificateEntity cert : certs ) {
      if ( !cert.isRevoked( ) ) {
        num++;
      }
    }
    return num;
  }
  
  @Override
  public void deleteUser( String userName, boolean forceDeleteAdmin, boolean recursive ) throws AuthException {
    String accountName = this.delegate.getName( );
    if ( userName == null ) {
      throw new AuthException( AuthException.EMPTY_USER_NAME );
    }
    if ( !forceDeleteAdmin && DatabaseAuthUtils.isAccountAdmin( userName ) ) {
      throw new AuthException( AuthException.DELETE_ACCOUNT_ADMIN );
    }
    if ( !recursive && userHasResourceAttached( userName, accountName ) ) {
      throw new AuthException( AuthException.USER_DELETE_CONFLICT );
    }
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      UserEntity user = DatabaseAuthUtils.getUniqueUser( db, userName, accountName );
      for ( GroupEntity ge : user.getGroups( ) ) {
        if ( ge.isUserGroup( ) ) {
          db.recast( GroupEntity.class ).delete( ge );
        } else {
          ge.getUsers( ).remove( user );
        }
      }
      db.delete( user );
      db.commit( );
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to delete user: " + userName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_USER, e );
    }
  }

  @Override
  public Role addRole( final String roleName, final String path, final String assumeRolePolicy ) throws AuthException, PolicyParseException {
    try {
      USER_GROUP_NAME_CHECKER.check( roleName );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, "Invalid role name " + roleName );
      throw new AuthException( AuthException.INVALID_NAME, e );
    }
    try {
      PATH_CHECKER.check( path );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, "Invalid path " + path );
      throw new AuthException( AuthException.INVALID_PATH, e );
    }
    if ( DatabaseAuthUtils.checkRoleExists( roleName, this.delegate.getName() ) ) {
      throw new AuthException( AuthException.ROLE_ALREADY_EXISTS );
    }
    final PolicyEntity parsedPolicy = PolicyParser.getResourceInstance().parse( assumeRolePolicy );
    final EntityWrapper<AccountEntity> db = EntityWrapper.get( AccountEntity.class );
    try {
      final AccountEntity account = DatabaseAuthUtils.getUnique( db, AccountEntity.class, "name", this.delegate.getName( ) );
      final RoleEntity newRole = new RoleEntity( roleName );
      newRole.setRoleId( Crypto.generateAlphanumericId( 21, "ARO" ) );
      newRole.setPath( path );
      newRole.setAccount( account );
      newRole.setAssumeRolePolicy( parsedPolicy );
      parsedPolicy.setName( "assume-role-policy-for-" + newRole.getRoleId() );
      final RoleEntity persistedRole = db.recast( RoleEntity.class ).persist( newRole );
      db.commit( );
      return new DatabaseRoleProxy( persistedRole );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to add role: " + roleName + " in " + this.delegate.getName() );
      throw new AuthException( AuthException.ROLE_CREATE_FAILURE, e );
    } finally {
      if ( db.isActive() ) db.rollback();
    }
  }

  @Override
  public void deleteRole( final String roleName ) throws AuthException {
    final String accountName = this.delegate.getName( );
    if ( roleName == null ) {
      throw new AuthException( AuthException.EMPTY_ROLE_NAME );
    }
    if ( roleHasResourceAttached( roleName, accountName ) ) {
      throw new AuthException( AuthException.ROLE_DELETE_CONFLICT );
    }
    final EntityWrapper<RoleEntity> db = EntityWrapper.get( RoleEntity.class );
    try {
      final RoleEntity role = DatabaseAuthUtils.getUniqueRole( db, roleName, accountName );
      db.delete( role );
      db.commit( );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to delete role: " + roleName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_ROLE, e );
    } finally {
      if ( db.isActive() ) db.rollback();
    }
  }

  @Override
  public Group addGroup( String groupName, String path ) throws AuthException {
    try {
      USER_GROUP_NAME_CHECKER.check( groupName );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, "Invalid group name " + groupName );
      throw new AuthException( AuthException.INVALID_NAME, e );
    }
    try {
      PATH_CHECKER.check( path );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, "Invalid path " + path );
      throw new AuthException( AuthException.INVALID_PATH, e );
    }
    if ( DatabaseAuthUtils.checkGroupExists( groupName, this.delegate.getName( ) ) ) {
      throw new AuthException( AuthException.GROUP_ALREADY_EXISTS );
    }
    EntityWrapper<AccountEntity> db = EntityWrapper.get( AccountEntity.class );
    try {
      AccountEntity account = DatabaseAuthUtils.getUnique( db, AccountEntity.class, "name", this.delegate.getName( ) );
      GroupEntity group = new GroupEntity( groupName );
      group.setPath( path );
      group.setUserGroup( false );
      group.setAccount( account );
      db.recast( GroupEntity.class ).add( group );
      db.commit( );
      return new DatabaseGroupProxy( group );
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to add group " + groupName + " in " + this.delegate.getName( ) );
      throw new AuthException( AuthException.GROUP_CREATE_FAILURE, e );
    }
  }
  
  private boolean groupHasResourceAttached( String groupName, String accountName ) throws AuthException {
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      GroupEntity group = DatabaseAuthUtils.getUniqueGroup( db, groupName, accountName );
      boolean hasResAttached = group.getUsers( ).size( ) > 0 || group.getPolicies( ).size( ) > 0;
      db.commit( );
      return hasResAttached;
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to check group " + groupName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_GROUP, e );
    }
  }
  
  @Override
  public void deleteGroup( String groupName, boolean recursive ) throws AuthException {
    String accountName = this.delegate.getName( );
    if ( groupName == null ) {
      throw new AuthException( AuthException.EMPTY_GROUP_NAME );
    }
    if ( DatabaseAuthUtils.isUserGroupName( groupName ) ) {
      throw new AuthException( AuthException.USER_GROUP_DELETE );
    }
    if ( !recursive && groupHasResourceAttached( groupName, accountName ) ) {
      throw new AuthException( AuthException.GROUP_DELETE_CONFLICT );
    }
    
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      GroupEntity group = DatabaseAuthUtils.getUniqueGroup( db, groupName, accountName );
      db.delete( group );
      db.commit( );
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to delete group " + groupName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_GROUP, e );
    }
  }

  @Override
  public InstanceProfile addInstanceProfile( final String instanceProfileName, final String path ) throws AuthException {
    try {
      USER_GROUP_NAME_CHECKER.check( instanceProfileName );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, "Invalid instance profile name " + instanceProfileName );
      throw new AuthException( AuthException.INVALID_NAME, e );
    }
    try {
      PATH_CHECKER.check( path );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, "Invalid path " + path );
      throw new AuthException( AuthException.INVALID_PATH, e );
    }
    if ( DatabaseAuthUtils.checkInstanceProfileExists( instanceProfileName, this.delegate.getName() ) ) {
      throw new AuthException( AuthException.INSTANCE_PROFILE_ALREADY_EXISTS );
    }
    final EntityWrapper<AccountEntity> db = EntityWrapper.get( AccountEntity.class );
    try {
      final AccountEntity account = DatabaseAuthUtils.getUnique( db, AccountEntity.class, "name", this.delegate.getName( ) );
      final InstanceProfileEntity newInstanceProfile = new InstanceProfileEntity( instanceProfileName );
      newInstanceProfile.setPath( path );
      newInstanceProfile.setAccount( account );
      final InstanceProfileEntity persistedInstanceProfile = db.recast( InstanceProfileEntity.class ).persist( newInstanceProfile );
      db.commit( );
      return new DatabaseInstanceProfileProxy( persistedInstanceProfile );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to add instance profile: " + instanceProfileName + " in " + this.delegate.getName() );
      throw new AuthException( AuthException.INSTANCE_PROFILE_CREATE_FAILURE, e );
    } finally {
      if ( db.isActive() ) db.rollback();
    }
  }

  @Override
  public void deleteInstanceProfile( final String instanceProfileName ) throws AuthException {
    final String accountName = this.delegate.getName( );
    if ( instanceProfileName == null ) {
      throw new AuthException( AuthException.EMPTY_INSTANCE_PROFILE_NAME );
    }
    final EntityWrapper<InstanceProfileEntity> db = EntityWrapper.get( InstanceProfileEntity.class );
    try {
      final InstanceProfileEntity instanceProfileEntity = DatabaseAuthUtils.getUniqueInstanceProfile( db, instanceProfileName, accountName );
      db.delete( instanceProfileEntity );
      db.commit( );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to delete instance profile: " + instanceProfileName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_INSTANCE_PROFILE, e );
    } finally {
      if ( db.isActive() ) db.rollback();
    }
  }

  @Override
  public Group lookupGroupByName( String groupName ) throws AuthException {
    String accountName = this.delegate.getName( );
    if ( groupName == null ) {
      throw new AuthException( AuthException.EMPTY_GROUP_NAME );
    }
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      GroupEntity group = DatabaseAuthUtils.getUniqueGroup( db, groupName, accountName );
      db.commit( );
      return new DatabaseGroupProxy( group );
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to get group " + groupName + " for " + accountName );
      throw new AuthException( AuthException.NO_SUCH_GROUP, e );
    }
  }

  @Override
  public InstanceProfile lookupInstanceProfileByName( final String instanceProfileName ) throws AuthException {
    final String accountName = this.delegate.getName( );
    if ( instanceProfileName == null ) {
      throw new AuthException( AuthException.EMPTY_INSTANCE_PROFILE_NAME );
    }
    final EntityWrapper<InstanceProfileEntity> db = EntityWrapper.get( InstanceProfileEntity.class );
    try {
      final InstanceProfileEntity instanceProfileEntity =
          DatabaseAuthUtils.getUniqueInstanceProfile( db, instanceProfileName, accountName );
      return new DatabaseInstanceProfileProxy( instanceProfileEntity );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get instance profile " + instanceProfileName + " for " + accountName );
      throw new AuthException( AuthException.NO_SUCH_INSTANCE_PROFILE, e );
    } finally {
      if ( db.isActive() ) db.rollback();
    }
  }

  @Override
  public Role lookupRoleByName( String roleName ) throws AuthException {
    final String accountName = this.delegate.getName( );
    if ( roleName == null ) {
      throw new AuthException( AuthException.EMPTY_ROLE_NAME );
    }
    final EntityWrapper<RoleEntity> db = EntityWrapper.get( RoleEntity.class );
    try {
      final RoleEntity roleEntity = DatabaseAuthUtils.getUniqueRole( db, roleName, accountName );
      return new DatabaseRoleProxy( roleEntity );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get role " + roleName + " for " + accountName );
      throw new AuthException( AuthException.NO_SUCH_ROLE, e );
    } finally {
      if ( db.isActive() ) db.rollback();
    }
  }

  @Override
  public User lookupUserByName( String userName ) throws AuthException {
    String accountName = this.delegate.getName( );
    if ( userName == null ) {
      throw new AuthException( AuthException.EMPTY_USER_NAME );
    }
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      UserEntity user = DatabaseAuthUtils.getUniqueUser( db, userName, accountName );
      db.commit( );
      return new DatabaseUserProxy( user );
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to find user: " + userName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_USER, e );
    }
  }

  @Override
  public User lookupAdmin() throws AuthException {
    return lookupUserByName( User.ACCOUNT_ADMIN );
  }

  @Override
  public List<Authorization> lookupAccountGlobalAuthorizations( String resourceType ) throws AuthException {
    String accountId = this.delegate.getAccountNumber( );
    if ( resourceType == null ) {
      throw new AuthException( "Empty resource type" );
    }
    EntityWrapper<AuthorizationEntity> db = EntityWrapper.get( AuthorizationEntity.class );
    try {
      @SuppressWarnings( "unchecked" )
      List<AuthorizationEntity> authorizations = ( List<AuthorizationEntity> ) db
          .createCriteria( AuthorizationEntity.class ).setCacheable( true ).add(
              Restrictions.and(
                  Restrictions.eq( "type", resourceType ),
                  Restrictions.or( 
                      Restrictions.eq( "effect", EffectType.Allow ),
                      Restrictions.eq( "effect", EffectType.Deny ) ) ) )
          .createCriteria( "statement" ).setCacheable( true )
          .createCriteria( "policy" ).setCacheable( true )
          .createCriteria( "group" ).setCacheable( true ).add( Restrictions.eq( "name", DatabaseAuthUtils.getUserGroupName( User.ACCOUNT_ADMIN ) ) )
          .createCriteria( "account" ).setCacheable( true ).add( Restrictions.eq( "accountNumber", accountId ) )
          .list( );
      db.commit( );
      List<Authorization> results = Lists.newArrayList( );
      for ( AuthorizationEntity auth : authorizations ) {
        results.add( new DatabaseAuthorizationProxy( auth ) );
      }
      return results;
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to lookup global authorization for account " + accountId + ", type=" + resourceType);
      throw new AuthException( "Failed to lookup account global auth", e );
    }
  }
  
  @Override
  public List<Authorization> lookupAccountGlobalQuotas( String resourceType ) throws AuthException {
    String accountId = this.delegate.getAccountNumber( );
    if ( resourceType == null ) {
      throw new AuthException( "Empty resource type" );
    }
    EntityWrapper<AuthorizationEntity> db = EntityWrapper.get( AuthorizationEntity.class );
    try {
      @SuppressWarnings( "unchecked" )
      List<AuthorizationEntity> authorizations = ( List<AuthorizationEntity> ) db
          .createCriteria( AuthorizationEntity.class ).setCacheable( true ).add(
              Restrictions.and(
                  Restrictions.eq( "type", resourceType ),
                  Restrictions.eq( "effect", EffectType.Limit ) ) )
          .createCriteria( "statement" ).setCacheable( true )
          .createCriteria( "policy" ).setCacheable( true )
          .createCriteria( "group" ).setCacheable( true ).add( Restrictions.eq( "name", DatabaseAuthUtils.getUserGroupName( User.ACCOUNT_ADMIN ) ) )
          .createCriteria( "account" ).setCacheable( true ).add( Restrictions.eq( "accountNumber", accountId ) )
          .list( );
      db.commit( );
      List<Authorization> results = Lists.newArrayList( );
      for ( AuthorizationEntity auth : authorizations ) {
        results.add( new DatabaseAuthorizationProxy( auth ) );
      }
      return results;
    } catch ( Exception e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to lookup global quota for account " + accountId + ", type=" + resourceType);
      throw new AuthException( "Failed to lookup account global quota", e );
    }
  }
  
}
