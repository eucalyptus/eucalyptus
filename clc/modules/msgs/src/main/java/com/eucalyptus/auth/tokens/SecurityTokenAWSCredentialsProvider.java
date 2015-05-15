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
package com.eucalyptus.auth.tokens;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 *
 */
public class SecurityTokenAWSCredentialsProvider implements AWSCredentialsProvider {

  private static final int EXPIRATION_SECS = 900;
  private static final int PRE_EXPIRY = 60;

  private final AtomicReference<Supplier<AWSCredentials>> credentialsSupplier = new AtomicReference<>( );
  private final Supplier<User> user;

  public SecurityTokenAWSCredentialsProvider( final AccountFullName accountFullName ) {
    this( new Supplier<User>() {
      @Override
      public User get() {
        try {
          return Accounts.lookupPrincipalByAccountNumber( accountFullName.getAccountNumber( ) );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  public SecurityTokenAWSCredentialsProvider( final User user ) {
    this( Suppliers.ofInstance( user ) );
  }

  public SecurityTokenAWSCredentialsProvider( final Supplier<User> user ) {
    this.user = user;
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
    return Suppliers.memoizeWithExpiration( new Supplier<AWSCredentials>( ) {
      @Override
      public AWSCredentials get() {
        try {
          final SecurityToken securityToken = SecurityTokenManager.issueSecurityToken( user.get( ), EXPIRATION_SECS );
          return new BasicSessionCredentials(
              securityToken.getAccessKeyId( ),
              securityToken.getSecretKey( ),
              securityToken.getToken( ) );
        } catch ( final AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    }, EXPIRATION_SECS - PRE_EXPIRY, TimeUnit.SECONDS );
  }
}
