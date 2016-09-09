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

import static com.eucalyptus.entities.Entities.criteriaQuery;
import static com.eucalyptus.entities.Entities.restriction;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import javax.persistence.metamodel.SingularAttribute;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.euare.persist.entities.AccountEntity;
import com.eucalyptus.auth.euare.persist.entities.AccountEntity_;
import com.eucalyptus.auth.euare.persist.entities.GroupEntity;
import com.eucalyptus.auth.euare.persist.entities.GroupEntity_;
import com.eucalyptus.auth.euare.persist.entities.InstanceProfileEntity;
import com.eucalyptus.auth.euare.persist.entities.InstanceProfileEntity_;
import com.eucalyptus.auth.euare.persist.entities.OpenIdProviderEntity;
import com.eucalyptus.auth.euare.persist.entities.OpenIdProviderEntity_;
import com.eucalyptus.auth.euare.persist.entities.PolicyEntity;
import com.eucalyptus.auth.euare.persist.entities.PolicyEntity_;
import com.eucalyptus.auth.euare.persist.entities.RoleEntity;
import com.eucalyptus.auth.euare.persist.entities.RoleEntity_;
import com.eucalyptus.auth.euare.persist.entities.UserEntity;
import com.eucalyptus.auth.euare.persist.entities.UserEntity_;
import com.eucalyptus.auth.euare.principal.EuareAccountScopedPrincipal;
import com.eucalyptus.auth.euare.principal.EuareUser;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionCallbackException;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public class DatabaseAuthUtils {

  public static boolean isAccountAdmin( String userName ) {
    return User.ACCOUNT_ADMIN.equals( userName );
  }
  
  public static String getUserGroupName( String userName ) {
    return EuareUser.USER_GROUP_PREFIX + userName;
  }
  
  public static boolean isUserGroupName( String groupName ) {
    return groupName.startsWith( EuareUser.USER_GROUP_PREFIX );
  }

  /**
   * Must call within a transaction.
   * 
   * @param userName
   * @param accountName
   * @return
   */
  public static UserEntity getUniqueUser( String userName, String accountName ) throws Exception {
    try {
      return Entities.criteriaQuery( UserEntity.class ).whereEqual( UserEntity_.name, userName )
          .join( UserEntity_.groups ).whereEqual( GroupEntity_.userGroup, Boolean.TRUE )
          .join( GroupEntity_.account ).whereEqual( AccountEntity_.name, accountName )
          .uniqueResult( );
    } catch ( final NoSuchElementException e ) {
      throw new NoSuchElementException( "Can not find user " + userName + " in " + accountName );
    }
  }

  /**
   * Must call within a transaction.
   */
  public static long countUsersInGroup( String groupName, String accountName ) throws Exception {
    return Entities.count( UserEntity.class )
        .join( UserEntity_.groups ).whereEqual( GroupEntity_.name, groupName )
        .join( GroupEntity_.account ).whereEqual( AccountEntity_.name, accountName )
        .uniqueResult( );
  }

  /**
   * Must call within a transaction.
   */
  public static long countPoliciesInGroup( String groupName, String accountName ) throws Exception {
    return Entities.count( PolicyEntity.class )
        .join( PolicyEntity_.group ).whereEqual( GroupEntity_.name, groupName )
        .join( GroupEntity_.account ).whereEqual( AccountEntity_.name, accountName )
        .uniqueResult( );
  }

  /**
   * Must call within a transaction.
   * 
   * @param groupName
   * @param accountName
   * @return
   * @throws Exception
   */
  public static GroupEntity getUniqueGroup( String groupName, String accountName ) throws Exception {
    try {
      return Entities.criteriaQuery( GroupEntity.class ).whereEqual( GroupEntity_.name, groupName )
          .join( GroupEntity_.account ).whereEqual( AccountEntity_.name, accountName )
          .uniqueResult( );
    } catch ( final NoSuchElementException e ) {
      throw new NoSuchElementException( "Can not find group " + groupName + " in " + accountName );
    }
  }

  /**
   * Must call within a transaction.
   */
  public static InstanceProfileEntity getUniqueInstanceProfile( String instanceProfileName, String accountName ) throws Exception {
    try {
      return Entities.criteriaQuery( InstanceProfileEntity.class ).whereEqual( InstanceProfileEntity_.name, instanceProfileName )
        .join( InstanceProfileEntity_.account ).whereEqual( AccountEntity_.name, accountName )
        .uniqueResult( );
    } catch ( final NoSuchElementException e ) {
      throw new NoSuchElementException( "Can not find instance profile " + instanceProfileName + " in " + accountName );
    }
  }

  /**
   * Must call within a transaction.
   */
  public static RoleEntity getUniqueRole( String roleName, String accountName ) throws Exception {
    try {
      return Entities.criteriaQuery( RoleEntity.class ).whereEqual( RoleEntity_.name, roleName )
        .join( RoleEntity_.account ).whereEqual( AccountEntity_.name, accountName )
        .uniqueResult( );
    } catch ( final NoSuchElementException e ) {
      throw new NoSuchElementException( "Can not find role " + roleName + " in " + accountName );
    }
  }

  /**
   * Must call within a transaction.
   * 
   * @param accountName
   * @return
   * @throws Exception
   */
  public static AccountEntity getUniqueAccount( String accountName ) throws Exception {
    return getUnique( AccountEntity.class, AccountEntity_.name, accountName );
  }
  
  /**
   * Must call within a transacton.
   */
  public static PolicyEntity getUniquePolicy(  String policyName, String groupId ) throws Exception {
    try {
      return Entities.criteriaQuery( PolicyEntity.class ).whereEqual( PolicyEntity_.name, policyName )
        .join( PolicyEntity_.group ).whereEqual( GroupEntity_.id, groupId )
        .uniqueResult( );
    } catch ( final NoSuchElementException e ) {
      throw new NoSuchElementException( "Can not find policy " + policyName + " for group " + groupId );
    }
  }
  
  public static PolicyEntity removeGroupPolicy( GroupEntity group, String name ) throws Exception {
    return removeNamedPolicy( group.getPolicies(), name );
  }

  public static PolicyEntity removeNamedPolicy( Collection<PolicyEntity> policyEntities, String name ) throws Exception {
    PolicyEntity policy = null;
    for ( PolicyEntity p : policyEntities ) {
      if ( name.equals( p.getName( ) ) ) {
        policy = p;
      }
    }
    if ( policy != null ) {
      policyEntities.remove( policy );
    }
    return policy;
  }

  /**
   * Must call within a transaction.
   */
  public static OpenIdProviderEntity getUniqueOpenIdConnectProvider( String url, String accountName ) throws Exception {
    try {
      return Entities.criteriaQuery( OpenIdProviderEntity.class ).whereEqual( OpenIdProviderEntity_.url, url )
        .join( OpenIdProviderEntity_.account ).whereEqual( AccountEntity_.name, accountName )
        .uniqueResult( );
    } catch ( final NoSuchElementException e ) {
      throw new NoSuchElementException( "Can not find openid connect provider " + url + " in " + accountName );
    }
  }

  /**
   * Check if the user name follows the IAM spec.
   * http://docs.amazonwebservices.com/IAM/latest/UserGuide/index.html?Using_Identifiers.html
   * 
   * @param userName
   * @throws com.eucalyptus.auth.AuthException
   */
  public static void checkUserName( String userName ) throws AuthException {
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
  public static void checkPath( String path ) throws AuthException {
    if ( path != null && !path.startsWith( "/" ) ) {
      throw new AuthException( "Invalid path: " + path );
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
  public static boolean checkUserExists( String userName, String accountName ) throws AuthException {
    if ( userName == null || accountName == null ) {
      throw new AuthException( "Empty user name or account name" );
    }
    try ( final TransactionResource db = Entities.transactionFor( UserEntity.class ) ) {
      final Optional<UserEntity> userOptional = Entities
          .criteriaQuery( UserEntity.class ).whereEqual( UserEntity_.name, userName )
          .join( UserEntity_.groups ).whereEqual( GroupEntity_.userGroup, Boolean.TRUE )
          .join( GroupEntity_.account ).whereEqual( AccountEntity_.name, accountName )
          .uniqueResultOption( );
      db.commit( );
      return userOptional.isPresent( );
    } catch ( Exception e ) {
      throw new AuthException( "Failed to find user", e );
    }
  }

  /**
   * Check if a role exists.
   */
  public static boolean checkRoleExists( String roleName, String accountName ) throws AuthException {
    if ( roleName == null || accountName == null ) {
      throw new AuthException( "Empty user name or account name" );
    }
    try ( final TransactionResource db = Entities.transactionFor( RoleEntity.class ) ) {
      final Optional<RoleEntity> roleOptional = Entities
          .criteriaQuery( RoleEntity.class ).whereEqual( RoleEntity_.name, roleName )
          .join( RoleEntity_.account ).whereEqual( AccountEntity_.name, accountName )
          .uniqueResultOption( );
      return roleOptional.isPresent( );
    } catch ( Exception e ) {
      throw new AuthException( "Failed to find role", e );
    }
  }

  /**
   * Check if an account exists.
   * 
   * @param accountName
   * @return
   * @throws AuthException
   */
  public static boolean checkAccountExists( String accountName ) throws AuthException {
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    try ( final TransactionResource db = Entities.transactionFor( AccountEntity.class ) ) {
      final Optional<AccountEntity> accountOptional = Entities
          .criteriaQuery( AccountEntity.class ).whereEqual( AccountEntity_.name, accountName )
          .uniqueResultOption( );
      db.commit( );
      return accountOptional.isPresent( );
    } catch ( Exception e ) {
      throw new AuthException( "Failed to find account", e );
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
  public static boolean checkGroupExists( String groupName, String accountName ) throws AuthException {
    if ( groupName == null) {
      throw new AuthException( AuthException.EMPTY_GROUP_NAME );
    }  
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    try ( final TransactionResource db = Entities.transactionFor( GroupEntity.class ) ) {
      final Optional<GroupEntity> groupOptional = Entities
          .criteriaQuery( GroupEntity.class ).whereEqual( GroupEntity_.name, groupName )
          .join( GroupEntity_.account ).whereEqual( AccountEntity_.name, accountName )
          .uniqueResultOption( );
      db.commit( );
      return groupOptional.isPresent( );
    } catch ( Exception e ) {
      throw new AuthException( "Failed to find group", e );
    }
  }

  /**
   * Check if an instance profile exists.
   */
  public static boolean checkInstanceProfileExists( String instanceProfileName, String accountName ) throws AuthException {
    if ( instanceProfileName == null || accountName == null ) {
      throw new AuthException( "Empty instance profile name or account name" );
    }
    try ( final TransactionResource db = Entities.transactionFor( InstanceProfileEntity.class ) ) {
      final Optional<InstanceProfileEntity> profileOptional = Entities
          .criteriaQuery( InstanceProfileEntity.class ).whereEqual( InstanceProfileEntity_.name, instanceProfileName )
          .join( InstanceProfileEntity_.account ).whereEqual( AccountEntity_.name, accountName )
          .uniqueResultOption( );
      return profileOptional.isPresent( );
    } catch ( Exception e ) {
      throw new AuthException( "Failed to find instance profile", e );
    }
  }

  /**
   * Check if the account is empty (no roles, no groups, no users).
   */
  public static boolean isAccountEmpty( String accountName ) throws AuthException {
    try ( final TransactionResource db = Entities.transactionFor( GroupEntity.class ) ) {
      final long groups = Entities.count( GroupEntity.class )
          .join( GroupEntity_.account )
          .whereEqual( AccountEntity_.name, accountName )
          .uniqueResult( );

      final long roles = Entities.count( RoleEntity.class )
          .join( RoleEntity_.account )
          .whereEqual( AccountEntity_.name, accountName )
          .uniqueResult( );

      return roles + groups == 0;
    } catch ( Exception e ) {
      throw new AuthException( "Error checking if account is empty", e );
    }
  }
  
  public static boolean policyNameinList( String name, List<Policy> policies ) {
    if ( policies != null ) {
      for ( Policy p : policies ) {
        if ( p.getName( ).equals( name ) ) {
          return true;
        }
      }
    }
    return false;
  }

  public static <T,PT> T getUnique( Class<T> entityClass, SingularAttribute<? super T, PT> property, PT value ) throws Exception {
    try {
      return criteriaQuery( restriction( entityClass ).equal( property, value ) ).uniqueResult( );
    } catch ( NoSuchElementException e ) {
      throw new NoSuchElementException( "No " + entityClass.getCanonicalName( ) + " with " + property.getName( ) + "=" + value );
    }
  }

  public static <T,PT> void invokeUnique( Class<T> entityClass, SingularAttribute<? super T, PT> property, PT value, final Callback<T> c ) throws TransactionException {
    try ( final TransactionResource db = Entities.transactionFor( entityClass ) ) {
      T result = getUnique( entityClass, property, value );
      if ( c != null ) {
        c.fire( result );
      }
      db.commit( );
    } catch ( Exception e ) {
      throw new TransactionCallbackException( e );
    }
  }

  static Supplier<String> getAccountNumberSupplier( final EuareAccountScopedPrincipal principal ){
    return Suppliers.memoize( new Supplier<String>() {
      @Override
      public String get() {
        try {
          return principal.getAccount().getAccountNumber();
        } catch ( final AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  public static <T> T extract( final Supplier<T> supplier ) throws AuthException {
    try {
      return supplier.get( );
    } catch ( final RuntimeException e ) {
      throw Exceptions.rethrow( e, AuthException.class );
    }
  }
}
