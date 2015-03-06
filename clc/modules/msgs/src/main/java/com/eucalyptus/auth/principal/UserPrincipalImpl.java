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
 ************************************************************************/
package com.eucalyptus.auth.principal;

import static com.eucalyptus.auth.principal.Certificate.Util.revoked;
import static com.eucalyptus.util.CollectionUtils.propertyPredicate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.util.NonNullFunction;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 *
 */
public class UserPrincipalImpl implements UserPrincipal {

  private static final long serialVersionUID = 1L;

  @Nonnull
  private final String name;

  @Nonnull
  private final String path;

  @Nonnull
  private final String userId;

  @Nonnull
  private final String authenticatedId;

  @Nullable
  private final String token;

  @Nonnull
  private final String accountAlias;

  @Nonnull
  private final String accountNumber;

  @Nonnull
  private final String canonicalId;

  private final boolean enabled;

  private final boolean accountAdmin;

  private final boolean systemAdmin;

  private final boolean systemUser;

  @Nonnull
  private final ImmutableList<AccessKey> keys;

  @Nonnull
  private final ImmutableList<Certificate> certificates;

  @Nonnull
  private final ImmutableList<PolicyVersion> principalPolicies;

  public UserPrincipalImpl( final EuareUser user ) throws AuthException {
    final Account account = user.getAccount( );

    final List<PolicyVersion> policies = Lists.newArrayList( );
    if ( user.isEnabled( ) ) {
      if ( user.isAccountAdmin() ) {
        policies.add( PolicyVersions.getAdministratorPolicy() );
      } else {
        Iterables.addAll(
            policies,
            Iterables.transform(
                user.getPolicies( ),
                PolicyVersions.policyVersion( PolicyScope.User, Accounts.getUserArn( user ) ) ) );
        for ( final Group group : Iterables.filter( user.getGroups(), Predicates.not( Accounts.isUserGroup() ) ) ) {
          Iterables.addAll(
              policies,
              Iterables.transform(
                  group.getPolicies( ),
                  PolicyVersions.policyVersion( PolicyScope.Group, Accounts.getGroupArn( group ) ) ) );
        }
      }
      Iterables.addAll(
          policies,
          Iterables.transform(
              account.lookupAdmin( ).getPolicies( ),
              PolicyVersions.policyVersion( PolicyScope.Account, user.getAccountNumber( ) ) ) ); //TODO:STEVE: ARN for account?
    }

    this.name = user.getName( );
    this.path = user.getPath();
    this.userId = user.getUserId();
    this.authenticatedId = user.getUserId( );
    this.canonicalId = account.getCanonicalId();
    this.token = user.getToken();
    this.accountAlias = account.getName();
    this.accountNumber = account.getAccountNumber();
    this.enabled = user.isEnabled();
    this.accountAdmin = user.isAccountAdmin( );
    this.systemAdmin = user.isSystemAdmin( );
    this.systemUser = user.isSystemUser( );
    this.keys = ImmutableList.copyOf( Iterables.transform( user.getKeys( ), keyWrapper( this )) );
    this.certificates = ImmutableList.copyOf(
        Iterables.filter( user.getCertificates( ), propertyPredicate( false, revoked( ) ) ) );
    this.principalPolicies = ImmutableList.copyOf( policies );
  }

  public UserPrincipalImpl( final Role role ) throws AuthException {
    final Account account = role.getAccount( );
    final EuareUser user = account.lookupAdmin( );
    final List<PolicyVersion> policies = Lists.newArrayList( );
    Iterables.addAll(
        policies,
        Iterables.transform(
            role.getPolicies( ),
            PolicyVersions.policyVersion( PolicyScope.Role, Accounts.getRoleArn( role ) ) ) );
    Iterables.addAll(
        policies,
        Iterables.transform(
            user.getPolicies(),
            PolicyVersions.policyVersion( PolicyScope.Account, user.getAccountNumber( ) ) ) ); //TODO:STEVE: ARN for account?

    this.name = user.getName( );
    this.path = user.getPath();
    this.userId = user.getUserId();
    this.authenticatedId = role.getRoleId();
    this.canonicalId = account.getCanonicalId();
    this.token = null;
    this.accountAlias = account.getName( );
    this.accountNumber = account.getAccountNumber();
    this.enabled = true;
    this.accountAdmin = false;
    this.systemAdmin = false;
    this.systemUser = user.isSystemUser( );
    this.keys = ImmutableList.copyOf( Collections.<AccessKey>emptyIterator( ) );
    this.certificates = ImmutableList.copyOf( Collections.<Certificate>emptyIterator() );
    this.principalPolicies = ImmutableList.copyOf( policies );
  }

  public UserPrincipalImpl(
      final UserPrincipal principal,
      final Iterable<AccessKey> keys
  ) throws AuthException {
    this.name = principal.getName( );
    this.path = principal.getPath();
    this.userId = principal.getUserId();
    this.authenticatedId = principal.getAuthenticatedId();
    this.canonicalId = principal.getCanonicalId();
    this.token = principal.getToken();
    this.accountAlias = principal.getAccountAlias();
    this.accountNumber = principal.getAccountNumber();
    this.enabled = principal.isEnabled();
    this.accountAdmin = principal.isAccountAdmin( );
    this.systemAdmin = principal.isSystemAdmin( );
    this.systemUser = principal.isSystemUser( );
    this.keys = ImmutableList.copyOf( keys );
    this.certificates = ImmutableList.copyOf( principal.getCertificates() );
    this.principalPolicies = ImmutableList.copyOf( principal.getPrincipalPolicies() );
  }

  @Nonnull
  public String getName( ) {
    return name;
  }

  @Nonnull
  public String getPath( ) {
    return path;
  }

  @Nonnull
  public String getUserId( ) {
    return userId;
  }

  @Nonnull
  public String getAuthenticatedId( ) {
    return authenticatedId;
  }

  @Nullable
  public String getToken( ) {
    return token;
  }

  @Nonnull
  public String getAccountAlias( ) {
    return accountAlias;
  }

  @Nonnull
  public String getAccountNumber( ) {
    return accountNumber;
  }

  @Nonnull
  public String getCanonicalId( ) {
    return canonicalId;
  }

  public boolean isEnabled( ) {
    return enabled;
  }

  @Override
  public boolean isAccountAdmin() {
    return accountAdmin;
  }

  public boolean isSystemAdmin( ) {
    return systemAdmin;
  }

  public boolean isSystemUser( ) {
    return systemUser;
  }

  @Nonnull
  public ImmutableList<AccessKey> getKeys( ) {
    return keys;
  }

  @Nonnull
  public ImmutableList<Certificate> getCertificates( ) {
    return certificates;
  }

  @Nonnull
  public ImmutableList<PolicyVersion> getPrincipalPolicies( ) {
    return principalPolicies;
  }

  private static NonNullFunction<AccessKey,AccessKey> keyWrapper( final UserPrincipal userPrincipal ) {
    return new NonNullFunction<AccessKey, AccessKey>() {
      @Nonnull
      @Override
      public AccessKey apply( final AccessKey accessKey ) {
        return new AccessKey( ){
          @Override public Boolean isActive( ) { return accessKey.isActive( ); }
          @Override public void setActive( final Boolean active ) throws AuthException { }
          @Override public String getAccessKey() { return accessKey.getAccessKey( ); }
          @Override public String getSecretKey( ) { return accessKey.getSecretKey( ); }
          @Override public Date getCreateDate( ) { return accessKey.getCreateDate( ); }
          @Override public UserPrincipal getPrincipal( ) { return userPrincipal; }
        };
      }
    };
  }
}
