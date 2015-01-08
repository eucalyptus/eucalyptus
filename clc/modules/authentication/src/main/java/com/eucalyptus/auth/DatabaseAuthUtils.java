/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.hibernate.criterion.Restrictions;
import com.eucalyptus.auth.entities.AccountEntity;
import com.eucalyptus.auth.entities.GroupEntity;
import com.eucalyptus.auth.entities.InstanceProfileEntity;
import com.eucalyptus.auth.entities.PolicyEntity;
import com.eucalyptus.auth.entities.RoleEntity;
import com.eucalyptus.auth.entities.UserEntity;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.AccountScopedPrincipal;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionCallbackException;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public class DatabaseAuthUtils {

  public static boolean isAccountAdmin( String userName ) {
    return User.ACCOUNT_ADMIN.equals( userName );
  }
  
  public static String getUserGroupName( String userName ) {
    return User.USER_GROUP_PREFIX + userName;
  }
  
  public static boolean isUserGroupName( String groupName ) {
    return groupName.startsWith( User.USER_GROUP_PREFIX );
  }

  /**
   * Must call within a transaction.
   * 
   * @param userName
   * @param accountName
   * @return
   */
  public static UserEntity getUniqueUser( String userName, String accountName ) throws Exception {
    @SuppressWarnings( "unchecked" )
    UserEntity result = ( UserEntity ) Entities
        .createCriteria( UserEntity.class ).setCacheable( true ).add( Restrictions.eq( "name", userName ) )
        .createCriteria( "groups" ).setCacheable( true ).add( Restrictions.eq( "userGroup", true ) )
        .createCriteria( "account" ).setCacheable( true ).add( Restrictions.eq( "name", accountName ) )
        .uniqueResult( );
    if ( result == null ) {
      throw new NoSuchElementException( "Can not find user " + userName + " in " + accountName );
    }
    return result;
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
    @SuppressWarnings( "unchecked" )
    GroupEntity result = ( GroupEntity ) Entities
        .createCriteria( GroupEntity.class ).setCacheable( true ).add( Restrictions.eq( "name", groupName ) )
        .createCriteria( "account" ).setCacheable( true ).add( Restrictions.eq( "name", accountName ) )
        .uniqueResult( );
    if ( result == null ) {
      throw new NoSuchElementException( "Can not find group " + groupName + " in " + accountName );
    }
    return result;
  }

  /**
   * Must call within a transaction.
   */
  public static InstanceProfileEntity getUniqueInstanceProfile( String instanceProfileName, String accountName ) throws Exception {
    @SuppressWarnings( "unchecked" )
    InstanceProfileEntity result = ( InstanceProfileEntity ) Entities
        .createCriteria( InstanceProfileEntity.class ).add( Restrictions.eq( "name", instanceProfileName ) )
        .createCriteria( "account" ).add( Restrictions.eq( "name", accountName ) )
        .setCacheable( true )
        .uniqueResult( );
    if ( result == null ) {
      throw new NoSuchElementException( "Can not find instance profile " + instanceProfileName + " in " + accountName );
    }
    return result;
  }

  /**
   * Must call within a transaction.
   */
  public static RoleEntity getUniqueRole( String roleName, String accountName ) throws Exception {
    @SuppressWarnings( "unchecked" )
    final RoleEntity result = ( RoleEntity ) Entities
        .createCriteria( RoleEntity.class ).add( Restrictions.eq( "name", roleName ) )
        .createCriteria( "account" ).add( Restrictions.eq( "name", accountName ) )
        .setCacheable( true )
        .uniqueResult();
    if ( result == null ) {
      throw new NoSuchElementException( "Can not find role " + roleName + " in " + accountName );
    }
    return result;
  }

  /**
   * Must call within a transaction.
   * 
   * @param accountName
   * @return
   * @throws Exception
   */
  public static AccountEntity getUniqueAccount( String accountName ) throws Exception {
    return getUnique( AccountEntity.class, "name", accountName );
  }
  
  /**
   * Must call within a transacton.
   */
  public static PolicyEntity getUniquePolicy(  String policyName, String groupId ) throws Exception {
    @SuppressWarnings( "unchecked" )
    PolicyEntity result = ( PolicyEntity ) Entities
        .createCriteria( PolicyEntity.class ).setCacheable( true ).add( Restrictions.eq( "name", policyName ) )
        .createCriteria( "group" ).setCacheable( true ).add( Restrictions.eq( "groupId", groupId ) )
        .uniqueResult( );
    if ( result == null ) {
      throw new NoSuchElementException( "Can not find policy " + policyName + " for group " + groupId );
    }
    return result;
  }
  
  public static PolicyEntity removeGroupPolicy( GroupEntity group, String name ) throws Exception {
    return removeNamedPolicy( group.getPolicies(), name );
  }

  public static PolicyEntity removeNamedPolicy( List<PolicyEntity> policyEntities, String name ) throws Exception {
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
   * Check if the user name follows the IAM spec.
   * http://docs.amazonwebservices.com/IAM/latest/UserGuide/index.html?Using_Identifiers.html
   * 
   * @param userName
   * @throws AuthException
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
      @SuppressWarnings( "unchecked" )
      UserEntity result = ( UserEntity ) Entities
          .createCriteria( UserEntity.class ).setCacheable( true ).add( Restrictions.eq( "name", userName ) )
          .createCriteria( "groups" ).setCacheable( true ).add( Restrictions.eq( "userGroup", true ) )
          .createCriteria( "account" ).setCacheable( true ).add( Restrictions.eq( "name", accountName ) )
          .uniqueResult( );
      db.commit( );
      return result != null;
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
      @SuppressWarnings( "unchecked" )
      RoleEntity result = ( RoleEntity ) Entities
          .createCriteria( RoleEntity.class ).add( Restrictions.eq( "name", roleName ) )
          .createCriteria( "account" ).add( Restrictions.eq( "name", accountName ) )
          .setCacheable( true )
          .uniqueResult();
      return result != null;
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
      @SuppressWarnings( "unchecked" )
      AccountEntity result = ( AccountEntity ) Entities
          .createCriteria( AccountEntity.class ).setCacheable( true ).add( Restrictions.eq( "name", accountName ) )
          .uniqueResult( );
      db.commit( );
      return result != null;
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
      @SuppressWarnings( "unchecked" )
      GroupEntity result = ( GroupEntity ) Entities
          .createCriteria( GroupEntity.class ).setCacheable( true ).add( Restrictions.eq( "name", groupName ) )
          .createCriteria( "account" ).setCacheable( true ).add( Restrictions.eq( "name", accountName ) )
          .uniqueResult( );
      db.commit( );
      return result != null;
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
      @SuppressWarnings( "unchecked" )
      InstanceProfileEntity result = ( InstanceProfileEntity ) Entities
          .createCriteria( InstanceProfileEntity.class ).add( Restrictions.eq( "name", instanceProfileName ) )
          .createCriteria( "account" ).add( Restrictions.eq( "name", accountName ) )
          .setCacheable( true )
          .uniqueResult();
      return result != null;
    } catch ( Exception e ) {
      throw new AuthException( "Failed to find instance profile", e );
    }
  }



  /**
   * Check if the account is empty (no roles, no groups, no users).
   */
  public static boolean isAccountEmpty( String accountName ) throws AuthException {
    try ( final TransactionResource db = Entities.transactionFor( GroupEntity.class ) ) {
      final long groups = Entities.count(
          new GroupEntity( ),
          Restrictions.eq( "account.name", accountName ),
          Collections.singletonMap( "account", "account" ) );

      final long roles = Entities.count(
          new RoleEntity( ),
          Restrictions.eq( "account.name", accountName ),
          Collections.singletonMap( "account", "account" ) );

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

  @SuppressWarnings("unchecked")
  public static <T> T getUnique( Class<T> entityClass, String property, Object value ) throws Exception {
    T result = ( T ) Entities.createCriteria( entityClass ).setCacheable( true ).add( Restrictions.eq( property, value ) ).uniqueResult( );
    if ( result == null ) {
      throw new NoSuchElementException( "No " + entityClass.getCanonicalName( ) + " with " + property + "=" + value ); 
    }
    return result;
  }
  
  public static <T> void invokeUnique( Class<T> entityClass, String property, Object value, final Callback<T> c ) throws TransactionException {
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

  static Supplier<String> getAccountNumberSupplier( final AccountScopedPrincipal principal ){
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
