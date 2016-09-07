/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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

package com.eucalyptus.auth.euare.persist;

import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.AuthenticationLimitProvider;
import com.eucalyptus.auth.Debugging;
import com.eucalyptus.auth.PolicyParseException;
import com.eucalyptus.auth.ServerCertificate;
import com.eucalyptus.auth.euare.OpenIdConnectProvider;
import com.eucalyptus.auth.euare.ServerCertificates;
import com.eucalyptus.auth.euare.checker.InvalidValueException;
import com.eucalyptus.auth.euare.checker.ValueChecker;
import com.eucalyptus.auth.euare.checker.ValueCheckerFactory;
import com.eucalyptus.auth.euare.persist.entities.AccountEntity;
import com.eucalyptus.auth.euare.persist.entities.AccountEntity_;
import com.eucalyptus.auth.euare.persist.entities.GroupEntity;
import com.eucalyptus.auth.euare.persist.entities.GroupEntity_;
import com.eucalyptus.auth.euare.persist.entities.InstanceProfileEntity;
import com.eucalyptus.auth.euare.persist.entities.InstanceProfileEntity_;
import com.eucalyptus.auth.euare.persist.entities.OpenIdProviderEntity;
import com.eucalyptus.auth.euare.persist.entities.OpenIdProviderEntity_;
import com.eucalyptus.auth.euare.persist.entities.PolicyEntity;
import com.eucalyptus.auth.euare.persist.entities.RoleEntity;
import com.eucalyptus.auth.euare.persist.entities.RoleEntity_;
import com.eucalyptus.auth.euare.persist.entities.ServerCertificateEntity;
import com.eucalyptus.auth.euare.persist.entities.UserEntity;
import com.eucalyptus.auth.euare.persist.entities.UserEntity_;
import com.eucalyptus.auth.euare.principal.EuareAccount;
import com.eucalyptus.auth.euare.principal.EuareGroup;
import com.eucalyptus.auth.euare.principal.EuareOpenIdConnectProvider;
import com.eucalyptus.auth.euare.principal.EuareRole;
import com.eucalyptus.auth.euare.principal.EuareUser;
import com.eucalyptus.auth.policy.PolicyParser;
import com.eucalyptus.auth.policy.PolicyPolicy;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.euare.principal.EuareInstanceProfile;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.auth.util.Identifiers;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.Tx;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class DatabaseAccountProxy implements EuareAccount {

  private static final long serialVersionUID = 1L;

  private static Logger LOG = Logger.getLogger( DatabaseAccountProxy.class );

  private static final ValueChecker ACCOUNT_NAME_CHECKER = ValueCheckerFactory.createAccountNameChecker( );
  private static final ValueChecker USER_NAME_CHECKER = ValueCheckerFactory.createUserNameChecker( );
  private static final ValueChecker GROUP_NAME_CHECKER = ValueCheckerFactory.createGroupNameChecker( );
  private static final ValueChecker PATH_CHECKER = ValueCheckerFactory.createPathChecker( );

  private AccountEntity delegate;
  
  public DatabaseAccountProxy( AccountEntity delegate ) {
    this.delegate = delegate;
  }

  @Override
  public String getName( ) {
    return this.delegate.getName( );
  }

  /**
   * Get the resource display name, this is the name with path.
   *
   * @return The display name.
   */
  @Override
  public String getDisplayName() {
    return Accounts.getAccountFullName( this );
  }

  @Override
  public OwnerFullName getOwner( ) {
    return AccountFullName.getInstance( getAccountNumber( ) );
  }

  @Override
  public String toString( ) {
    return this.delegate.toString( );
  }

  public boolean hasAccountAlias( ) {
    return !getAccountNumber( ).equals( getName( ) );
  }

  @Override
  public String getAccountNumber( ) {
    return this.delegate.getAccountNumber( );
  }

  @Override
  public String getAccountAlias( ) {
    return getName( );
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
    setNameUnsafe( name );
  }

  @Override
  public void setNameUnsafe( final String name ) throws AuthException {
    try {
      // try finding the account with the same name to change to
      ( new DatabaseAuthProvider( ) ).lookupAccountByName( name );
    } catch ( AuthException ae ) {
      try {
        // not found
        DatabaseAuthUtils.invokeUnique( AccountEntity.class, AccountEntity_.accountNumber, this.delegate.getAccountNumber( ), new Tx<AccountEntity>( ) {
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
  public List<EuareUser> getUsers( ) throws AuthException {
    List<EuareUser> results = Lists.newArrayList();
    try ( final TransactionResource db = Entities.transactionFor( GroupEntity.class ) ) {
      List<UserEntity> users = Entities
          .criteriaQuery( UserEntity.class )
          .join( UserEntity_.groups ).whereEqual( GroupEntity_.userGroup, Boolean.TRUE )
          .join( GroupEntity_.account ).whereEqual( AccountEntity_.name, this.delegate.getName( ) )
          .list( );
      db.commit();
      for ( UserEntity u : users ) {
        results.add( new DatabaseUserProxy( u ) );
      }
      return results;
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get users for " + this.delegate.getName( ) );
      throw new AuthException( "Failed to get users for account", e );
    }
  }

  @Override
  public List<EuareGroup> getGroups( ) throws AuthException {
    List<EuareGroup> results = Lists.newArrayList( );
    try ( final TransactionResource db = Entities.transactionFor( GroupEntity.class ) ) {
      List<GroupEntity> groups = Entities
          .criteriaQuery( GroupEntity.class ).whereEqual( GroupEntity_.userGroup, Boolean.FALSE )
          .join( GroupEntity_.account ).whereEqual( AccountEntity_.name, this.delegate.getName( ) )
          .list( );
      db.commit( );
      for ( GroupEntity g : groups ) {
        results.add( new DatabaseGroupProxy( g, Suppliers.ofInstance( getAccountNumber( ) ) ) );
      }
      return results;
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get groups for " + this.delegate.getName( ) );
      throw new AuthException( "Failed to get groups", e );
    }
  }

  @Override
  public List<EuareRole> getRoles( ) throws AuthException {
    final List<EuareRole> results = Lists.newArrayList( );
    try ( final TransactionResource db = Entities.transactionFor( RoleEntity.class ) ) {
      List<RoleEntity> roles = Entities
          .criteriaQuery( RoleEntity.class )
          .join( RoleEntity_.account ).whereEqual( AccountEntity_.name, this.delegate.getName( ) )
          .list( );
      for ( final RoleEntity role : roles ) {
        results.add( new DatabaseRoleProxy( role ) );
      }
      return results;
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get roles for " + this.delegate.getName( ) );
      throw new AuthException( "Failed to get roles", e );
    }
  }

  @Override
  public List<EuareInstanceProfile> getInstanceProfiles() throws AuthException {
    final List<EuareInstanceProfile> results = Lists.newArrayList( );
    try ( final TransactionResource db = Entities.transactionFor( InstanceProfileEntity.class ) ) {
      List<InstanceProfileEntity> instanceProfiles = Entities
          .criteriaQuery( InstanceProfileEntity.class )
          .join( InstanceProfileEntity_.account ).whereEqual( AccountEntity_.name, this.delegate.getName( ) )
          .list( );
      for ( final InstanceProfileEntity instanceProfile : instanceProfiles ) {
        results.add( new DatabaseInstanceProfileProxy( instanceProfile  ) );
      }
      return results;
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get instance profiles for " + this.delegate.getName( ) );
      throw new AuthException( "Failed to get instance profiles", e );
    }
  }

  @Override
  public EuareUser addUser( String userName, String path, boolean enabled, Map<String, String> info ) throws AuthException {
    try {
      USER_NAME_CHECKER.check( userName );
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
    UserEntity newUser = new UserEntity( this.getAccountNumber(), userName );
    newUser.setPath( path );
    newUser.setEnabled( enabled );
    newUser.setPasswordExpires( System.currentTimeMillis( ) + AuthenticationLimitProvider.Values.getDefaultPasswordExpiry( ) );
    if ( info != null ) {
      newUser.getInfo( ).putAll( info );
    }
    newUser.setToken( Crypto.generateSessionToken() );
    //newUser.setConfirmationCode( Crypto.generateSessionToken( userName ) );
    GroupEntity newGroup = new GroupEntity( this.getAccountNumber(), DatabaseAuthUtils.getUserGroupName( userName ) );
    newGroup.setUserGroup( true );
    try ( final TransactionResource db = Entities.transactionFor( AccountEntity.class ) ) {
      AccountEntity account = DatabaseAuthUtils.getUnique( AccountEntity.class, AccountEntity_.name, this.delegate.getName( ) );
      newGroup = Entities.mergeDirect( newGroup );
      newUser = Entities.mergeDirect( newUser );
      newGroup.setAccount( account );
      newGroup.getUsers( ).add( newUser );
      newUser.getGroups( ).add( newGroup );
      db.commit( );
      return new DatabaseUserProxy( newUser );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to add user: " + userName + " in " + this.delegate.getName( ) );
      throw new AuthException( AuthException.USER_CREATE_FAILURE, e );
    }
  }
  
  private boolean userHasResourceAttached( String userName, String accountName ) throws AuthException {
    try ( final TransactionResource db = Entities.transactionFor( UserEntity.class ) ) {
      UserEntity user = DatabaseAuthUtils.getUniqueUser( userName, accountName );
      GroupEntity userGroup = DatabaseAuthUtils.getUniqueGroup( DatabaseAuthUtils.getUserGroupName( userName ), accountName );
      boolean result = ( user.getGroups( ).size( ) > 1
          || user.getKeys( ).size( ) > 0
          || user.getCertificates( ).size( ) > 0
          || userGroup.getPolicies( ).size( ) > 0 );
      db.commit( );
      return result;
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to check user " + userName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_USER, e );
    }
  }

  private boolean roleHasResourceAttached( String roleName, String accountName ) throws AuthException {
    try ( final TransactionResource db = Entities.transactionFor( RoleEntity.class ) ) {
      final RoleEntity roleEntity = DatabaseAuthUtils.getUniqueRole( roleName, accountName );
      return
          !roleEntity.getPolicies( ).isEmpty( ) ||
          !roleEntity.getInstanceProfiles( ).isEmpty( );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to check role " + roleName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_ROLE, e );
    }
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
    try ( final TransactionResource db = Entities.transactionFor( UserEntity.class ) ) {
      UserEntity user = DatabaseAuthUtils.getUniqueUser( userName, accountName );
      for ( GroupEntity ge : user.getGroups( ) ) {
        if ( ge.isUserGroup( ) ) {
          Entities.delete( ge );
        } else {
          ge.getUsers( ).remove( user );
        }
      }
      Entities.delete( user );
      db.commit( );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to delete user: " + userName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_USER, e );
    }
  }

  @Override
  public EuareRole addRole( final String roleName, final String path, final String assumeRolePolicy ) throws AuthException, PolicyParseException {
    try {
      USER_NAME_CHECKER.check( roleName );
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
    final PolicyPolicy policyPolicy = PolicyParser.getResourceInstance( ).parse( assumeRolePolicy );
    final PolicyEntity parsedPolicy = PolicyEntity.create( null, policyPolicy.getPolicyVersion( ), assumeRolePolicy );
    try ( final TransactionResource db = Entities.transactionFor( AccountEntity.class ) ) {
      final AccountEntity account = DatabaseAuthUtils.getUnique( AccountEntity.class, AccountEntity_.name, this.delegate.getName( ) );
      final RoleEntity newRole = new RoleEntity( roleName );
      newRole.setRoleId( Identifiers.generateIdentifier( "ARO" ) );
      newRole.setPath( path );
      newRole.setAccount( account );
      newRole.setAssumeRolePolicy( parsedPolicy );
      parsedPolicy.setName( "assume-role-policy-for-" + newRole.getRoleId() );
      final RoleEntity persistedRole = Entities.persist( newRole );
      db.commit( );
      return new DatabaseRoleProxy( persistedRole );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to add role: " + roleName + " in " + this.delegate.getName() );
      throw new AuthException( AuthException.ROLE_CREATE_FAILURE, e );
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
    try ( final TransactionResource db = Entities.transactionFor( RoleEntity.class ) ) {
      final RoleEntity role = DatabaseAuthUtils.getUniqueRole( roleName, accountName );
      Entities.delete( role );
      db.commit( );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to delete role: " + roleName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_ROLE, e );
    }
  }

  @Override
  public EuareGroup addGroup( String groupName, String path ) throws AuthException {
    try {
      GROUP_NAME_CHECKER.check( groupName );
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
    try ( final TransactionResource db = Entities.transactionFor( AccountEntity.class ) ) {
      AccountEntity account = DatabaseAuthUtils.getUnique( AccountEntity.class, AccountEntity_.name, this.delegate.getName( ) );
      GroupEntity group = new GroupEntity( this.getAccountNumber(), groupName );
      group.setPath( path );
      group.setUserGroup( false );
      group.setAccount( account );
      Entities.persist( group );
      db.commit( );
      return new DatabaseGroupProxy( group, Suppliers.ofInstance( getAccountNumber( ) ) );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to add group " + groupName + " in " + this.delegate.getName( ) );
      throw new AuthException( AuthException.GROUP_CREATE_FAILURE, e );
    }
  }
  
  private boolean groupHasResourceAttached( String groupName, String accountName ) throws AuthException {
    try ( final TransactionResource db = Entities.transactionFor( GroupEntity.class ) ) {
      boolean hasResAttached =
          DatabaseAuthUtils.countUsersInGroup( groupName, accountName ) > 0 ||
          DatabaseAuthUtils.countPoliciesInGroup( groupName, accountName ) > 0;
      db.commit( );
      return hasResAttached;
    } catch ( Exception e ) {
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

    try ( final TransactionResource db = Entities.transactionFor( GroupEntity.class ) ) {
      GroupEntity group = DatabaseAuthUtils.getUniqueGroup( groupName, accountName );
      Entities.delete( group );
      db.commit( );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to delete group " + groupName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_GROUP, e );
    }
  }

  @Override
  public EuareInstanceProfile addInstanceProfile( final String instanceProfileName, final String path ) throws AuthException {
    try {
      GROUP_NAME_CHECKER.check( instanceProfileName );
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
    try ( final TransactionResource db = Entities.transactionFor( AccountEntity.class ) ) {
      final AccountEntity account = DatabaseAuthUtils.getUnique( AccountEntity.class, AccountEntity_.name, this.delegate.getName( ) );
      final InstanceProfileEntity newInstanceProfile = new InstanceProfileEntity( instanceProfileName );
      newInstanceProfile.setPath( path );
      newInstanceProfile.setAccount( account );
      final InstanceProfileEntity persistedInstanceProfile = Entities.persist( newInstanceProfile );
      db.commit( );
      return new DatabaseInstanceProfileProxy( persistedInstanceProfile );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to add instance profile: " + instanceProfileName + " in " + this.delegate.getName() );
      throw new AuthException( AuthException.INSTANCE_PROFILE_CREATE_FAILURE, e );
    }
  }

  @Override
  public void deleteInstanceProfile( final String instanceProfileName ) throws AuthException {
    final String accountName = this.delegate.getName( );
    if ( instanceProfileName == null ) {
      throw new AuthException( AuthException.EMPTY_INSTANCE_PROFILE_NAME );
    }
    try ( final TransactionResource db = Entities.transactionFor( InstanceProfileEntity.class ) ) {
      final InstanceProfileEntity instanceProfileEntity = DatabaseAuthUtils.getUniqueInstanceProfile( instanceProfileName, accountName );
      Entities.delete( instanceProfileEntity );
      db.commit( );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to delete instance profile: " + instanceProfileName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_INSTANCE_PROFILE, e );
    }
  }

  @Override
  public EuareGroup lookupGroupByName( String groupName ) throws AuthException {
    String accountName = this.delegate.getName( );
    if ( groupName == null ) {
      throw new AuthException( AuthException.EMPTY_GROUP_NAME );
    }
    try ( final TransactionResource db = Entities.transactionFor( GroupEntity.class ) ) {
      GroupEntity group = DatabaseAuthUtils.getUniqueGroup( groupName, accountName );
      db.commit( );
      return new DatabaseGroupProxy( group, Suppliers.ofInstance( getAccountNumber( ) ) );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get group " + groupName + " for " + accountName );
      throw new AuthException( AuthException.NO_SUCH_GROUP, e );
    }
  }

  @Override
  public EuareInstanceProfile lookupInstanceProfileByName( final String instanceProfileName ) throws AuthException {
    final String accountName = this.delegate.getName( );
    if ( instanceProfileName == null ) {
      throw new AuthException( AuthException.EMPTY_INSTANCE_PROFILE_NAME );
    }
    try ( final TransactionResource db = Entities.transactionFor( InstanceProfileEntity.class ) ) {
      final InstanceProfileEntity instanceProfileEntity =
          DatabaseAuthUtils.getUniqueInstanceProfile( instanceProfileName, accountName );
      return new DatabaseInstanceProfileProxy( instanceProfileEntity );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get instance profile " + instanceProfileName + " for " + accountName );
      throw new AuthException( AuthException.NO_SUCH_INSTANCE_PROFILE, e );
    }
  }

  @Override
  public EuareRole lookupRoleByName( String roleName ) throws AuthException {
    final String accountName = this.delegate.getName( );
    if ( roleName == null ) {
      throw new AuthException( AuthException.EMPTY_ROLE_NAME );
    }
    try ( final TransactionResource db = Entities.transactionFor( RoleEntity.class ) ) {
      final RoleEntity roleEntity = DatabaseAuthUtils.getUniqueRole( roleName, accountName );
      return new DatabaseRoleProxy( roleEntity );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get role " + roleName + " for " + accountName );
      throw new AuthException( AuthException.NO_SUCH_ROLE, e );
    }
  }

  @Override
  public EuareUser lookupUserByName( String userName ) throws AuthException {
    String accountName = this.delegate.getName( );
    if ( userName == null ) {
      throw new AuthException( AuthException.EMPTY_USER_NAME );
    }
    try ( final TransactionResource db = Entities.transactionFor( UserEntity.class ) ) {
      UserEntity user = DatabaseAuthUtils.getUniqueUser( userName, accountName );
      db.commit( );
      return new DatabaseUserProxy( user );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to find user: " + userName + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_USER, e );
    }
  }

  @Override
  public EuareUser lookupAdmin() throws AuthException {
    return lookupUserByName( User.ACCOUNT_ADMIN );
  }

  @Override
  public ServerCertificate addServerCertificate(String certName,
      String certBody, String certChain, String certPath, String pk)
          throws AuthException {
    if(! ServerCertificateEntity.isCertificateNameValid(certName))
      throw new AuthException(AuthException.INVALID_SERVER_CERT_NAME);
    if(! ServerCertificateEntity.isCertificatePathValid(certPath))
      throw new AuthException(AuthException.INVALID_SERVER_CERT_PATH);

    try{
      ServerCertificates.verifyCertificate( certBody, pk, certChain );
    }catch(final AuthException ex) {
      throw ex;
    }catch(final Exception ex) {
      throw new AuthException(AuthException.SERVER_CERT_INVALID_FORMAT);
    }

    String encPk = null;
    String sessionKey = null;
    try{
      // generate symmetric key
      final MessageDigest digest = Digest.SHA256.get();
      final byte[] salt = new byte[32];
      Crypto.getSecureRandomSupplier().get().nextBytes(salt);
      //digest.update( this.lookupAdmin().getPassword().getBytes( Charsets.UTF_8 ) );
      digest.update( salt );
      final SecretKey symmKey = new SecretKeySpec( digest.digest(), "AES" );

      // encrypt the server pk
      Cipher cipher = Ciphers.AES_GCM.get();
      final byte[] iv = new byte[32];
      Crypto.getSecureRandomSupplier().get().nextBytes(iv);
      cipher.init( Cipher.ENCRYPT_MODE, symmKey, new IvParameterSpec( iv ), Crypto.getSecureRandomSupplier( ).get( ) );
      final byte[] cipherText = cipher.doFinal(pk.getBytes());
      encPk = new String(Base64.encode(Arrays.concatenate(iv, cipherText)));

      final PublicKey euarePublicKey = SystemCredentials.lookup(Euare.class).getCertificate().getPublicKey();
      cipher = Ciphers.RSA_PKCS1.get();
      cipher.init(Cipher.WRAP_MODE, euarePublicKey, Crypto.getSecureRandomSupplier( ).get( ));
      byte[] wrappedKeyBytes = cipher.wrap(symmKey);
      sessionKey = new String(Base64.encode(wrappedKeyBytes));
    } catch ( final Exception e ) {
      LOG.error("Failed to encrypt key", e);
      throw Exceptions.toUndeclared(e);
    }

    try{
      final ServerCertificate found = lookupServerCertificate(certName);
      if(found!=null)
        throw new AuthException(AuthException.SERVER_CERT_ALREADY_EXISTS);
    }catch(final NoSuchElementException ex){
      ;
    }catch(final AuthException ex){
      if(! AuthException.SERVER_CERT_NO_SUCH_ENTITY.equals(ex.getMessage()))
        throw ex;
    }catch(final Exception ex){
      throw ex;
    }

    final String certId = Identifiers.generateIdentifier( "ASC" );
    ServerCertificateEntity entity = null;
    try ( final TransactionResource db = Entities.transactionFor( ServerCertificateEntity.class ) ) {
      final UserFullName accountAdmin = UserFullName.getInstance( this.lookupAdmin());
      entity = new ServerCertificateEntity(accountAdmin, certName);
      entity.setCertBody(certBody);
      entity.setCertChain(certChain);
      entity.setCertPath(certPath);
      entity.setPrivateKey(encPk);
      entity.setSessionKey(sessionKey);
      entity.setCertId(certId);
      Entities.persist(entity);
      db.commit();
    } catch( final Exception ex){
      LOG.error("Failed to persist server certificate entity", ex);
      throw Exceptions.toUndeclared(ex);
    }

    return ServerCertificates.ToServerCertificate.INSTANCE.apply(entity);
  }
  
  @Override
  public ServerCertificate deleteServerCertificate(String certName)
      throws AuthException {
    try ( final TransactionResource db = Entities.transactionFor( ServerCertificateEntity.class ) ) {
      final ServerCertificateEntity found = Entities.criteriaQuery(
          ServerCertificateEntity.named( UserFullName.getInstance( this.lookupAdmin( ) ), certName )
      ).uniqueResult( );
      Entities.delete( found );
      db.commit();
      return ServerCertificates.ToServerCertificate.INSTANCE.apply(found);
    } catch(final NoSuchElementException ex){
      throw new AuthException(AuthException.SERVER_CERT_NO_SUCH_ENTITY);
    } catch(final Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
  }

  @Override
  public ServerCertificate lookupServerCertificate(final String certName) throws AuthException {
    try ( final TransactionResource db = Entities.transactionFor( ServerCertificateEntity.class ) ) {
      final ServerCertificateEntity serverCertificateEntity = Entities.criteriaQuery(
          ServerCertificateEntity.named( UserFullName.getInstance( this.lookupAdmin( ) ), certName )
      ).readonly( ).uniqueResult( );
      db.rollback();
      return ServerCertificates.ToServerCertificateWithSecrets.INSTANCE.apply( serverCertificateEntity );
    } catch( final NoSuchElementException ex ){
      throw new AuthException( AuthException.SERVER_CERT_NO_SUCH_ENTITY );
    } catch( final AuthException ex ){
      throw ex;
    } catch( final Exception ex ){
      throw Exceptions.toUndeclared(ex);
    }
  }

  @Override
  public List<ServerCertificate> listServerCertificates(final String pathPrefix) throws AuthException {
    final List<ServerCertificateEntity> result;
    try ( final TransactionResource db = Entities.transactionFor( ServerCertificateEntity.class ) ) {
      result = Entities.criteriaQuery(
          ServerCertificateEntity.named( UserFullName.getInstance( this.lookupAdmin( ) ) )
      ).readonly( ).list( );
      db.rollback();
    }catch(final Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
    
    final String prefix = pathPrefix.length()>1 && pathPrefix.endsWith("/") ? pathPrefix.substring(0, pathPrefix.length()-1) : pathPrefix;
    final Iterable<ServerCertificateEntity> filtered;
    if ( prefix.equals("/") ) {
      filtered = result;
    } else {
      filtered = Iterables.filter( result, new Predicate<ServerCertificateEntity>(){
      @Override
      public boolean apply(ServerCertificateEntity entity) {
        final String path = entity.getCertPath();
        return path.startsWith(prefix) && (path.length()==prefix.length() || path.charAt(prefix.length()) == '/');
      }});
    }
    
    return Lists.newArrayList( Iterables.transform( filtered, new Function<ServerCertificateEntity, ServerCertificate>(){
      @Override
      public ServerCertificate apply(ServerCertificateEntity entity) {
        return ServerCertificates.ToServerCertificate.INSTANCE.apply(entity);
      }
    }));
  }
  
  @Override
  public void updateServerCeritificate(String certName, String newCertName, String newPath) throws AuthException    {
    try{
      ServerCertificate cert = this.lookupServerCertificate(certName);
      try{
        cert = this.lookupServerCertificate(newCertName);
        if(cert!=null)
          throw new AuthException(AuthException.SERVER_CERT_ALREADY_EXISTS);
      }catch(final AuthException ex){
        ;
      }
      ServerCertificates.updateServerCertificate(UserFullName.getInstance(this.lookupAdmin()), certName, newCertName, newPath);
    }catch(final AuthException ex){
      throw ex;
    }catch(final Exception ex){
      throw ex;
    }
  }

  @Override
  public EuareOpenIdConnectProvider createOpenIdConnectProvider(String url, List<String> clientIDList, List<String> thumbprintList) throws AuthException {
    Debugging.logError( LOG, null, "in Database layer: createOpenIdConnectProvider()");
    if(! OpenIdConnectProvider.isUrlValid(url))
      throw new AuthException(AuthException.INVALID_OPENID_PROVIDER_URL);
    try{
      final EuareOpenIdConnectProvider found = lookupOpenIdConnectProvider(url);
      if(found!=null)
        throw new AuthException(AuthException.OPENID_PROVIDER_ALREADY_EXISTS);
      return found;
    }catch(final NoSuchElementException ex){
      ;
    }catch(final AuthException ex){
      if(! AuthException.OPENID_PROVIDER_NO_SUCH_ENTITY.equals(ex.getMessage()))
        throw ex;
    }catch(final Exception ex){
      throw ex;
    }

    try ( final TransactionResource db = Entities.transactionFor( AccountEntity.class ) ) {
      final AccountEntity account = DatabaseAuthUtils.getUnique( AccountEntity.class, AccountEntity_.name, this.delegate.getName( ) );
      final OpenIdProviderEntity newOpenIdProvider = new OpenIdProviderEntity( url );
      newOpenIdProvider.getClientIDs().addAll( clientIDList );
      newOpenIdProvider.getThumbprints().addAll( thumbprintList );
      newOpenIdProvider.setAccount( account );
      final OpenIdProviderEntity persistedOpenIdProvider = Entities.persist( newOpenIdProvider );
      db.commit( );
      return new DatabaseOpenIdProviderProxy( persistedOpenIdProvider );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to add openid connect provider: " + url + " in " + this.delegate.getName() );
      throw new AuthException( AuthException.OPENID_PROVIDER_CREATE_FAILURE, e );
    }
  }

  public EuareOpenIdConnectProvider lookupOpenIdConnectProvider(final String url) throws AuthException {
    final String accountName = this.delegate.getName( );
    try ( final TransactionResource db = Entities.transactionFor( OpenIdProviderEntity.class ) ) {
      final OpenIdProviderEntity openidproviderEntity = DatabaseAuthUtils.getUniqueOpenIdConnectProvider( url, accountName );
      return new DatabaseOpenIdProviderProxy( openidproviderEntity );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get openid provider " + url + " for " + accountName );
      throw new AuthException( AuthException.OPENID_PROVIDER_NO_SUCH_ENTITY, e );
    }
  }

  @Override
  public void deleteOpenIdConnectProvider(String url) throws AuthException {
    final String accountName = this.delegate.getName( );
    if (url == null ) {
      throw new AuthException( AuthException.EMPTY_OPENID_PROVIDER_ARN );
    }
    try ( final TransactionResource db = Entities.transactionFor( OpenIdProviderEntity.class ) ) {
      final OpenIdProviderEntity provider = DatabaseAuthUtils.getUniqueOpenIdConnectProvider( url, accountName );
      Entities.delete( provider );
      db.commit( );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to delete openid connect provider: " + url + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_OPENID_CONNECT_PROVIDER, e );
    }
  }

  @Override
  public List<EuareOpenIdConnectProvider> listOpenIdConnectProviders() throws AuthException {
    final List<EuareOpenIdConnectProvider> results = Lists.newArrayList( );
    try ( final TransactionResource db = Entities.transactionFor( OpenIdProviderEntity.class ) ) {
      List<OpenIdProviderEntity> providers = Entities
          .criteriaQuery( OpenIdProviderEntity.class )
          .join( OpenIdProviderEntity_.account ).whereEqual( AccountEntity_.name, this.delegate.getName( ) )
          .list( );
      for ( final OpenIdProviderEntity provider : providers ) {
        results.add( new DatabaseOpenIdProviderProxy( provider ) );
      }
      return results;
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get openid connect providers for " + this.delegate.getName( ) );
      throw new AuthException( "Failed to get openid connect providers", e );
    }
  }

  @Override
  public EuareOpenIdConnectProvider getOpenIdConnectProvider(String url) throws AuthException {
    final String accountName = this.delegate.getName( );
    if (url == null ) {
      throw new AuthException( AuthException.EMPTY_OPENID_PROVIDER_URL );
    }
    try ( final TransactionResource db = Entities.transactionFor( OpenIdProviderEntity.class ) ) {
      final OpenIdProviderEntity provider = DatabaseAuthUtils.getUniqueOpenIdConnectProvider( url, accountName );
      return new DatabaseOpenIdProviderProxy( provider );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to delete openid connect provider: " + url + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_OPENID_CONNECT_PROVIDER, e );
    }
  }

  @Override
  public void addClientIdToOpenIdConnectProvider(String clientId, String url) throws AuthException {
    final String accountName = this.delegate.getName( );
    if (url == null ) {
      throw new AuthException( AuthException.EMPTY_OPENID_PROVIDER_URL );
    }
    try ( final TransactionResource db = Entities.transactionFor( OpenIdProviderEntity.class ) ) {
      final OpenIdProviderEntity provider = DatabaseAuthUtils.getUniqueOpenIdConnectProvider( url, accountName );
      provider.getClientIDs().add(clientId);
      db.commit( );
      return;
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to delete openid connect provider: " + url + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_OPENID_CONNECT_PROVIDER, e );
    }
  }

  @Override
  public void removeClientIdFromOpenIdConnectProvider(String clientId, String url) throws AuthException {
    final String accountName = this.delegate.getName( );
    if (url == null ) {
      throw new AuthException( AuthException.EMPTY_OPENID_PROVIDER_URL );
    }
    try ( final TransactionResource db = Entities.transactionFor( OpenIdProviderEntity.class ) ) {
      final OpenIdProviderEntity provider = DatabaseAuthUtils.getUniqueOpenIdConnectProvider( url, accountName );
      provider.getClientIDs().remove(clientId);
      db.commit( );
      return;
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to delete openid connect provider: " + url + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_OPENID_CONNECT_PROVIDER, e );
    }
  }

  @Override
  public void updateOpenIdConnectProviderThumbprint(String url, List<String> thumbprintList) throws AuthException {
    final String accountName = this.delegate.getName( );
    if (url == null ) {
      throw new AuthException( AuthException.EMPTY_OPENID_PROVIDER_URL );
    }
    try ( final TransactionResource db = Entities.transactionFor( OpenIdProviderEntity.class ) ) {
      final OpenIdProviderEntity provider = DatabaseAuthUtils.getUniqueOpenIdConnectProvider( url, accountName );
      provider.getThumbprints().clear();
      provider.getThumbprints().addAll(thumbprintList);
      db.commit( );
      return;
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to delete openid connect provider: " + url + " in " + accountName );
      throw new AuthException( AuthException.NO_SUCH_OPENID_CONNECT_PROVIDER, e );
    }
  }
}
