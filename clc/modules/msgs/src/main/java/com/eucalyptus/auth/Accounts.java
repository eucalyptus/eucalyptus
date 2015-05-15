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
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.api.IdentityProvider;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.ern.EuareResourceName;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.AccountIdentifiersImpl;
import com.eucalyptus.auth.principal.BaseInstanceProfile;
import com.eucalyptus.auth.principal.BaseRole;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.InstanceProfile;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.SecurityTokenContent;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserPrincipal;
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

  private static Supplier<IdentityProvider> identities = serviceLoaderSupplier( IdentityProvider.class );

  protected static <T> Supplier<T> serviceLoaderSupplier( final Class<T> serviceClass ) {
    return Suppliers.memoize( new Supplier<T>() {
      @Override
      public T get( ) {
        return ServiceLoader.load( serviceClass ).iterator( ).next( );
      }
    } );
  }

  public static void setIdentityProvider( IdentityProvider provider ) {
    synchronized ( Accounts.class ) {
      LOG.info( "Setting the identity provider to: " + provider.getClass( ) );
      identities = Suppliers.ofInstance( provider );
    }
  }

  protected static IdentityProvider getIdentityProvider( ) {
    return identities.get();
  }

  public static X509Certificate getEuareCertificate( final String accountNumber ) throws AuthException {
    return getIdentityProvider( ).getCertificateByAccountNumber( accountNumber );
  }

  public static X509Certificate signCertificate(
      final String accountNumber,
      final RSAPublicKey publicKey,
      final String principal,
      final int expiryInDays
  ) throws AuthException {
    return getIdentityProvider().signCertificate( accountNumber, publicKey, principal, expiryInDays );
  }

  public static String lookupAccountIdByAlias( String alias ) throws AuthException {
    if ( isAccountNumber( alias ) ) {
      return alias;
    } else {
      return getIdentityProvider( ).lookupAccountIdentifiersByAlias( alias ).getAccountNumber( );
    }
  }

  public static String lookupAccountIdByCanonicalId( String canonicalId ) throws AuthException {
    return getIdentityProvider( ).lookupAccountIdentifiersByCanonicalId( canonicalId ).getAccountNumber();
  }

  public static String lookupCanonicalIdByAccountId( String accountId ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByAccountNumber( accountId ).getCanonicalId();
  }

  public static String lookupCanonicalIdByEmail( String email ) throws AuthException {
    return getIdentityProvider( ).lookupAccountIdentifiersByEmail( email ).getCanonicalId();
  }

  public static String lookupAccountAliasById( String accountId ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByAccountNumber( accountId ).getAccountAlias();
  }

  public static AccountIdentifiers lookupAccountIdentifiersByAlias( final String alias ) throws AuthException {
    return Accounts.getIdentityProvider( ).lookupAccountIdentifiersByAlias( alias );
  }

  public static AccountIdentifiers lookupAccountIdentifiersByCanonicalId( final String canonicalId ) throws AuthException {
    return Accounts.getIdentityProvider( ).lookupAccountIdentifiersByCanonicalId( canonicalId );
  }

  public static AccountIdentifiers lookupAccountIdentifiersById( final String accountId ) throws AuthException {
    final UserPrincipal user = Accounts.getIdentityProvider( ).lookupPrincipalByAccountNumber( accountId );
    return new AccountIdentifiersImpl(
        user.getAccountNumber( ),
        user.getAccountAlias(),
        user.getCanonicalId( )
    );
  }

  public static boolean isSystemAccount( String accountName ) {
    return
        AccountIdentifiers.SYSTEM_ACCOUNT.equals( accountName ) ||
        Objects.toString( accountName, "" ).startsWith( AccountIdentifiers.SYSTEM_ACCOUNT_PREFIX );
  }

  @Nonnull
  public static List<String> listAccountNumbersForName( final String accountAliasExpression ) throws AuthException {
    return Lists.newArrayList( Iterables.transform(
        listAccountIdentifiersForName( accountAliasExpression ),
        AccountIdentifiers.Properties.accountNumber() ) );
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
  public static UserPrincipal lookupPrincipalByUserId( String userId ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByUserId( userId, null );
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
  public static List<X509Certificate> lookupAccountCertificatesByAccountNumber( String accountNumber ) throws AuthException {
    return getIdentityProvider( ).lookupAccountCertificatesByAccountNumber( accountNumber );
  }

  @Nonnull
  public static SecurityTokenContent decodeSecurityToken( String accessKeyIdentifier, String securityToken ) throws AuthException {
    return getIdentityProvider().decodeSecurityToken( accessKeyIdentifier, securityToken );
  }

  public static UserPrincipal lookupSystemAccountByAlias( final String alias ) throws AuthException {
    if ( !isSystemAccount( alias ) ) {
      throw new AuthException( "Not a system account: " + alias );
    }
    final String accountNumber = lookupAccountIdentifiersByAlias( alias ).getAccountNumber( );
    return lookupPrincipalByAccountNumber( accountNumber );
  }

  public static UserPrincipal lookupSystemAdmin( ) throws AuthException {
    return lookupSystemAccountByAlias( AccountIdentifiers.SYSTEM_ACCOUNT );
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

  public static Function<User,String> toUserId() {
    return UserStringProperties.USER_ID;
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

  private enum GroupFilters implements Predicate<Group> {
    USER_GROUP {
      @Override
      public boolean apply( @Nullable final Group group ) {
        return group != null && group.isUserGroup( );
      }
    }
  }
}
