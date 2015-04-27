/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.api.AccountProvider;
import com.eucalyptus.auth.api.IdentityProvider;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.ern.EuareResourceName;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.BaseInstanceProfile;
import com.eucalyptus.auth.principal.BaseRole;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.EuareRole;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.InstanceProfile;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.EuareUser;
import com.eucalyptus.auth.principal.SecurityTokenContent;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.auth.principal.UserPrincipalImpl;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * <h2>Eucalyptus/AWS IDs & Access Keys:</h2>
 * <p>
 * <strong>NOTE:IMPORTANT: It SHOULD NOT be the @Id of the underlying entity as this value is not
 * guaranteed to be fixed in the future (e.g., disrupted by upgrade, version changes,
 * etc.).</strong>
 * </p>
 * <ol>
 * <li>- AWS Account Number: Public ID for an ACCOUNT.</li>
 * <ul>
 * <li>- "globally" unique 12-digit number associated with the Eucalyptus account.</li>
 * <li>- is a shared value; other users may need it or discover it during normal operation of the
 * system</li>
 * <li>- _MUST_ be a 12-digit number. User commands require this value as input in certain cases and
 * enforce the length of the ID.</li>
 * </ul>
 * </li>
 * <li>AWS Access Key: Identifier value corresponding to the AWS Secret Access Key used to sign
 * requests.</li>
 * <ul>
 * <li>- "globally" unique 20 alpha-numeric characters
 * <li>
 * <li>- is a shared value; other users may need it or discover it during normal operation of the
 * system
 * <li>
 * <li>- _MUST_ be 20-alphanum characters; per the specification (e.g.,
 * s3.amazonaws.com/awsdocs/ImportExport/latest/AWSImportExport-dg.pdf). User commands require this
 * value as input in certain cases and enforce the length of the ID.
 * <li>
 * </ul>
 * </ol>
 */
public class Accounts {
  private static final Logger LOG = Logger.getLogger( Accounts.class );
  
  private static Supplier<AccountProvider> accounts = serviceLoaderSupplier( AccountProvider.class );

  private static Supplier<IdentityProvider> identities = serviceLoaderSupplier( IdentityProvider.class );

  private static <T> Supplier<T> serviceLoaderSupplier( final Class<T> serviceClass ) {
    return Suppliers.memoize( new Supplier<T>() {
      @Override
      public T get( ) {
        return ServiceLoader.load( serviceClass ).iterator( ).next( );
      }
    } );
  }

  public static void setAccountProvider( AccountProvider provider ) {
    synchronized ( Accounts.class ) {
      LOG.info( "Setting the account provider to: " + provider.getClass( ) );
      accounts = Suppliers.ofInstance( provider );
    }
  }

  public static void setIdentityProvider( IdentityProvider provider ) {
    synchronized ( Accounts.class ) {
      LOG.info( "Setting the identity provider to: " + provider.getClass( ) );
      identities = Suppliers.ofInstance( provider );
    }
  }

  protected static AccountProvider getAccountProvider( ) {
    return accounts.get( );
  }

  protected static IdentityProvider getIdentityProvider( ) {
    return identities.get( );
  }

  public static String lookupAccountIdByAlias( String alias ) throws AuthException {
    return getIdentityProvider( ).lookupAccountIdentifiersByAlias( alias ).getAccountNumber();
  }

  public static String lookupAccountIdByCanonicalId( String canonicalId ) throws AuthException {
    return getIdentityProvider( ).lookupAccountIdentifiersByCanonicalId( canonicalId ).getAccountNumber( );
  }

  public static String lookupCanonicalIdByAccountId( String accountId ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByAccountNumber( accountId ).getCanonicalId();
  }

  public static String lookupCanonicalIdByEmail( String email ) throws AuthException {
    return getIdentityProvider( ).lookupAccountIdentifiersByEmail( email ).getCanonicalId( );
  }

  public static String lookupAccountAliasById( String accountId ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByAccountNumber( accountId ).getAccountAlias( );
  }

  public static Account lookupAccountByName( String accountName ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupAccountByName( accountName );
  }
  
  public static Account lookupAccountById( String accountId ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupAccountById( accountId );
  }

  public static Account lookupAccountByCanonicalId(String canonicalId) throws AuthException {
    return Accounts.getAccountProvider( ).lookupAccountByCanonicalId( canonicalId );
  }

  public static Account addAccount( String accountName ) throws AuthException {
    return Accounts.getAccountProvider( ).addAccount( accountName );
  }

  public static void deleteAccount( String accountName, boolean forceDeleteSystem, boolean recursive ) throws AuthException {
    Accounts.getAccountProvider( ).deleteAccount( accountName, forceDeleteSystem, recursive );
  }

  public static List<Account> listAllAccounts( ) throws AuthException {
    return Accounts.getAccountProvider( ).listAllAccounts( );
  }

  public static boolean isSystemAccount( String accountName ) {
    return
        Account.SYSTEM_ACCOUNT.equals( accountName ) ||
        Objects.toString( accountName, "" ).startsWith( Account.SYSTEM_ACCOUNT_PREFIX );
  }

  public static boolean isSystemAccount( Account account ) {
    return isSystemAccount( account == null ? null : account.getName( ) );
  }

  public static Account addSystemAccount( ) throws AuthException {
    return Accounts.getAccountProvider( ).addAccount( Account.SYSTEM_ACCOUNT );
  }

  /**
   * Add a system account.
   *
   * @return The new account or an existing account with the specified name.
   */
  public static Account addSystemAccount( final String accountName ) throws AuthException {
    return Accounts.getAccountProvider( ).addSystemAccount( accountName );
  }

  /**
   * Add a system account with an admin user.
   *
   * @return The new account or an existing account with the specified name.
   */
  public static Account addSystemAccountWithAdmin( final String accountName ) throws AuthException {
    final Account account = addSystemAccount( accountName );
    try {
      account.lookupUserByName( EuareUser.ACCOUNT_ADMIN );
    } catch ( final AuthException e ) {
      account.addUser( EuareUser.ACCOUNT_ADMIN, "/", true, null );
    }
    return account;
  }

  @Nonnull
  public static List<String> listAccountNumbersForName( final String accountAliasExpression ) throws AuthException {
    return Lists.newArrayList( Iterables.transform(
        listAccountIdentifiersForName( accountAliasExpression ),
        AccountIdentifiers.Properties.accountNumber( ) ) );
  }

  @Nonnull
  public static List<AccountIdentifiers> listAccountIdentifiersForName( final String accountAliasExpression ) throws AuthException {
    return getIdentityProvider( ).listAccountIdentifiersByAliasMatch( accountAliasExpression );
  }

  @Nonnull
  public static UserPrincipal lookupPrincipalByAccountNumber( String accountNumber ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByAccountNumber( accountNumber );
  }

  @Nonnull
  public static UserPrincipal lookupPrincipalByAccountNumberAndUsername( String accountNumber, String username ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByAccountNumberAndUsername( accountNumber, username );
  }

  @Nonnull
  public static UserPrincipal lookupPrincipalByCanonicalId( String canonicalId ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByCanonicalId( canonicalId );
  }

  @Nonnull
  public static UserPrincipal lookupPrincipalByAccessKeyId( String accessKeyId, String nonce ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByAccessKeyId( accessKeyId, nonce );
  }

  @Nonnull
  public static UserPrincipal lookupPrincipalByUserId( String userId, String nonce ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByUserId( userId, nonce );
  }

  @Nonnull
  public static UserPrincipal lookupPrincipalByRoleId( String roleId, String nonce ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByRoleId( roleId, nonce );
  }

  @Nonnull
  public static UserPrincipal lookupPrincipalByCertificateId( String certificateId ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByCertificateId( certificateId );
  }

  @Nonnull
  public static InstanceProfile lookupInstanceProfileByName( String accountNumber, String name ) throws AuthException {
    return getIdentityProvider( ).lookupInstanceProfileByName( accountNumber, name );
  }

  @Nonnull
  public static Role lookupRoleByName( String accountNumber, String name ) throws AuthException {
    return getIdentityProvider( ).lookupRoleByName( accountNumber, name );
  }

  @Nonnull
  public static SecurityTokenContent decodeSecurityToken( String accessKeyIdentifier, String securityToken ) throws AuthException {
    return getIdentityProvider( ).decodeSecurityToken( accessKeyIdentifier, securityToken );
  }

  public static EuareUser lookupUserById( String userId ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupUserById( userId );
  }

  public static EuareUser lookupUserByAccessKeyId( String keyId ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupUserByAccessKeyId( keyId );
  }
  
  public static EuareUser lookupUserByCertificate( X509Certificate cert ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupUserByCertificate( cert );
  }

  public static Group lookupGroupById( String groupId ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupGroupById( groupId );
  }

  public static EuareRole lookupRoleById( String roleId ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupRoleById( roleId );
  }

  public static Certificate lookupCertificate( X509Certificate cert ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupCertificate( cert );
  }

  public static Certificate lookupCertificateById( String certificateId ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupCertificateById( certificateId );
  }

  public static AccessKey lookupAccessKeyById( String keyId ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupAccessKeyById( keyId );
  }
  
  public static EuareUser lookupSystemAdmin( ) throws AuthException {
    Account system = Accounts.getAccountProvider( ).lookupAccountByName( Account.SYSTEM_ACCOUNT );
    return system.lookupAdmin();
  }

  public static UserPrincipal lookupSystemAdminAsPrincipal( ) throws AuthException {
    Account system = Accounts.getAccountProvider( ).lookupAccountByName( Account.SYSTEM_ACCOUNT );
    return userAsPrincipal( system.lookupAdmin() );
  }

  public static EuareUser lookupAwsExecReadAdmin(boolean ensureActiveKey) throws AuthException {
	Account system = Accounts.getAccountProvider( ).lookupAccountByName( AccountIdentifiers.AWS_EXEC_READ_SYSTEM_ACCOUNT );
	EuareUser user = system.lookupAdmin();
	if (ensureActiveKey) {
      boolean hasActiveKey = false;
      for (AccessKey k:user.getKeys()) {
	    if ( k.isActive() ) {
          hasActiveKey = true;
          break;
	    }
      }
      if (!hasActiveKey) {
        user.createKey();
	    LOG.debug("Created new user key for " + user.getName());
      }
	}
	return user;
  }

  public static EuareUser lookupObjectStorageWalrusAccount(boolean ensureActiveKey) throws AuthException {
    Account system = Accounts.getAccountProvider( ).lookupAccountByName( AccountIdentifiers.OBJECT_STORAGE_WALRUS_ACCOUNT );
    EuareUser user = system.lookupAdmin();
    if (ensureActiveKey) {
      boolean hasActiveKey = false;
      for (AccessKey k:user.getKeys()) {
        if ( k.isActive() ) {
          hasActiveKey = true;
          break;
        }
      }
      if (!hasActiveKey) {
        user.createKey();
        LOG.debug("Created new user key for " + user.getName());
      }
    }
    return user;
  }

  public static UserPrincipal userAsPrincipal( final EuareUser user  ) throws AuthException {
    return new UserPrincipalImpl( user );
  }

  public static UserPrincipal roleAsPrincipal( final EuareRole role ) throws AuthException {
    return new UserPrincipalImpl( role );
  }

  public static String getAccountFullName( Account account ) {
    return "/" + account.getName( );
  }

  public static String getUserFullName( User user ) {
    if ( user.getPath( ).endsWith( "/" ) ) {
      return user.getPath( ) + user.getName( );
    } else {
      return user.getPath( ) + "/" + user.getName( );
    }
  }
  
  public static String getGroupFullName( Group group ) {
    if ( group.getPath( ).endsWith( "/" ) ) {
      return group.getPath( ) + group.getName( );
    } else {
      return group.getPath( ) + "/" + group.getName( );
    }
  }

  public static String getRoleFullName( BaseRole role ) {
    if ( role.getPath( ).endsWith( "/" ) ) {
      return role.getPath( ) + role.getName( );
    } else {
      return role.getPath( ) + "/" + role.getName( );
    }
  }

  public static String getInstanceProfileFullName( BaseInstanceProfile instanceProfile ) {
    if ( instanceProfile.getPath( ).endsWith( "/" ) ) {
      return instanceProfile.getPath( ) + instanceProfile.getName( );
    } else {
      return instanceProfile.getPath( ) + "/" + instanceProfile.getName( );
    }
  }

  public static String getUserArn( final User user ) throws AuthException {
    return buildArn( user.getAccountNumber( ), PolicySpec.IAM_RESOURCE_USER, user.getPath(), user.getName() );
  }

  public static String getUserArn( final UserPrincipal user ) throws AuthException {
    return buildArn( user.getAccountNumber( ), PolicySpec.IAM_RESOURCE_USER, user.getPath(), user.getName() );
  }

  public static String getGroupArn( final Group group ) throws AuthException {
    return buildArn( group.getAccountNumber( ), PolicySpec.IAM_RESOURCE_GROUP, group.getPath(), group.getName() );
  }

  public static String getRoleArn( final BaseRole role ) throws AuthException {
    return buildArn( role.getAccountNumber( ), PolicySpec.IAM_RESOURCE_ROLE, role.getPath(), role.getName() );
  }

  public static String getInstanceProfileArn( final BaseInstanceProfile instanceProfile ) throws AuthException {
    return buildArn( instanceProfile.getAccountNumber( ), PolicySpec.IAM_RESOURCE_INSTANCE_PROFILE, instanceProfile.getPath( ), instanceProfile.getName( ) );
  }

  private static String buildArn( final String accountNumber,
                                  final String type,
                                  final String path,
                                  final String name ) throws AuthException {
    return new EuareResourceName( accountNumber, type, path, name ).toString( );
  }

  public static boolean isRoleIdentifier( final String identifier ) {
    return identifier.startsWith( "ARO" );
  }

  public static boolean isAccountNumber( final String identifier ) {
    return identifier.matches( "[0-9]{12}" );
  }

  public static Function<Account,String> toAccountNumber() {
    return AccountStringProperties.ACCOUNT_NUMBER;
  }

  public static Function<User,String> toUserAccountNumber() {
    return UserStringProperties.ACCOUNT_NUMBER;
  }

  public static Function<User,String> toUserId() {
    return UserStringProperties.USER_ID;
  }

  public static Function<Group,String> toGroupAccountNumber() {
    return GroupStringProperties.ACCOUNT_NUMBER;
  }

  public static Function<Group,String> toGroupId() {
    return GroupStringProperties.GROUP_ID;
  }

  public static Predicate<Group> isUserGroup( ) {
    return GroupFilters.USER_GROUP;
  }

  private enum AccountStringProperties implements Function<Account,String> {
    ACCOUNT_NUMBER {
      @Override
      public String apply( final Account account ) {
        return account.getAccountNumber();
      }
    }
  }

  private enum UserStringProperties implements Function<User,String> {
    ACCOUNT_NUMBER {
      @Override
      public String apply( final User user ) {
        try {
          return user.getAccountNumber( );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    },
    USER_ID {
      @Override
      public String apply( final User user ) {
        return user.getUserId( );
      }
    }
  }

  private enum GroupStringProperties implements Function<Group,String> {
    ACCOUNT_NUMBER {
      @Override
      public String apply( final Group group ) {
        try {
          return group.getAccountNumber( );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    },
    GROUP_ID {
      @Override
      public String apply( final Group group ) {
        return group.getGroupId( );
      }
    }
  }

  private enum GroupFilters implements Predicate<Group> {
    USER_GROUP {
      @Override
      public boolean apply( @Nullable final Group group ) {
        return group != null && group.isUserGroup( );
      }
    }
  }
}
