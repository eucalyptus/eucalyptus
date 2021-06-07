/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.auth.tokens;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.HasRole;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Pair;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 *
 */
@SuppressWarnings( { "Guava", "WeakerAccess" } )
public class SecurityTokenAWSCredentialsProvider implements AWSCredentialsProvider {

  private static final int DEFAULT_EXPIRATION_SECS = 900;
  private static final int DEFAULT_PRE_EXPIRY_SECS = 60;

  private final AtomicReference<Supplier<AWSCredentials>> credentialsSupplier = new AtomicReference<>( );
  private final Supplier<User> user;
  private final Supplier<Pair<Role,RoleSecurityTokenAttributes>> role;
  private final int expirationSecs;
  private final int preExpirySecs;

  public static SecurityTokenAWSCredentialsProvider forUserOrRole( final User user ) {
    if ( user instanceof HasRole && ((HasRole) user).getRole( ) != null ) {
      final Role role = ((HasRole) user).getRole( );
      final RoleSecurityTokenAttributes attributes =
          RoleSecurityTokenAttributes.forUser( user )
              .or( RoleSecurityTokenAttributes.basic( "eucalyptus" ) );
      return new SecurityTokenAWSCredentialsProvider( role, attributes );
    } else {
      return new SecurityTokenAWSCredentialsProvider( user );
    }
  }

  public SecurityTokenAWSCredentialsProvider( final AccountFullName accountFullName ) {
    this( () -> {
      try {
        return Accounts.lookupPrincipalByAccountNumber( accountFullName.getAccountNumber( ) );
      } catch ( AuthException e ) {
        throw Exceptions.toUndeclared( e );
      }
    } );
  }

  public SecurityTokenAWSCredentialsProvider( final User user ) {
    this( Suppliers.ofInstance( user ) );
  }

  public SecurityTokenAWSCredentialsProvider( final Supplier<User> user ) {
    this( user, DEFAULT_EXPIRATION_SECS );
  }

  public SecurityTokenAWSCredentialsProvider( final Supplier<User> user, final int expirationSecs ) {
    this( user, Math.max( expirationSecs, DEFAULT_PRE_EXPIRY_SECS * 2 ), DEFAULT_PRE_EXPIRY_SECS );
  }

  public SecurityTokenAWSCredentialsProvider( final User user, final int expirationSecs, final int preExpirySecs ) {
    this( Suppliers.ofInstance( user ), expirationSecs, preExpirySecs );
  }

  public SecurityTokenAWSCredentialsProvider( final Supplier<User> user, final int expirationSecs, final int preExpirySecs ) {
    this( user, null, expirationSecs, preExpirySecs );
  }

  public SecurityTokenAWSCredentialsProvider( final Role role, final RoleSecurityTokenAttributes attributes ) {
    this( null, Suppliers.ofInstance( Pair.pair( role, attributes ) ), DEFAULT_EXPIRATION_SECS, DEFAULT_PRE_EXPIRY_SECS );
  }

  private SecurityTokenAWSCredentialsProvider(
      final Supplier<User> user,
      final Supplier<Pair<Role,RoleSecurityTokenAttributes>> role,
      final int expirationSecs,
      final int preExpirySecs
  ) {
    this.user = user;
    this.role = role;
    this.expirationSecs = Math.max( expirationSecs, DEFAULT_PRE_EXPIRY_SECS * 2 );
    this.preExpirySecs = preExpirySecs;
    refresh( );
  }

  @Override
  public AWSCredentials getCredentials( ) {
    return credentialsSupplier.get( ).get( );
  }

  @Override
  public void refresh( ) {
    credentialsSupplier.set( refreshCredentialsSupplier( ) );
  }

  private Supplier<AWSCredentials> refreshCredentialsSupplier( ) {
    return Suppliers.memoizeWithExpiration( () -> {
      try {
        final SecurityToken securityToken = user != null ?
            SecurityTokenManager.issueSecurityToken( user.get( ), expirationSecs ) :
            SecurityTokenManager.issueSecurityToken( role.get( ).getLeft( ), role.get( ).getRight( ), expirationSecs );
        return new BasicSessionCredentials(
            securityToken.getAccessKeyId( ),
            securityToken.getSecretKey( ),
            securityToken.getToken( ) );
      } catch ( final AuthException e ) {
        throw Exceptions.toUndeclared( e );
      }
    }, expirationSecs - preExpirySecs, TimeUnit.SECONDS );
  }
}
