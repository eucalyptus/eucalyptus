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
package com.eucalyptus.auth;

import javax.security.auth.login.CredentialExpiredException;
import com.eucalyptus.auth.ldap.LdapSync;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.EuareUser;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.crypto.Crypto;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

/**
 *
 */
public class PasswordAuthentication {
  public static final String INVALID_USERNAME_OR_PASSWORD = "Invalid username or password";
  public static final String PASSWORD_CHANGE_NOT_SUPPORTED = "Changing password is not supported for this user";

  public static void authenticate( final EuareUser user,
                                   final String password ) throws AuthException, CredentialExpiredException {
    boolean checkExpiration = true;
    if ( authenticateWithLdap( user ) ) try {
      LdapSync.authenticate(user, password);
      checkExpiration = false;
    } catch ( LdapException e ) {
      throw new AuthException(INVALID_USERNAME_OR_PASSWORD);
    } else if ( !Crypto.verifyPassword(password, user.getPassword()) ) {
      throw new AuthException(INVALID_USERNAME_OR_PASSWORD);
    }
    if ( checkExpiration ) {
      checkPasswordExpiration( user );
    }
  }

  public static boolean authenticateWithLdap( final EuareUser user ) {
    return LdapSync.enabled( ) && !user.isSystemAdmin( ) && !user.isAccountAdmin( );
  }

  /**
   * Authenticates a user by {@code user}/{@code password} and if provided updates the user's
   * password to be the given {@code newPassword}
   * 
   * @param accountAlias - identified user account alias
   * @param username - identified username
   * @param password - user provided password
   * @param newPassword - user provided new password, may be null, cannot be given w/ LDAP
   * @throws AuthException
   * @throws CredentialExpiredException 
   * @see {@link PasswordAuthentication#authenticate(EuareUser, String)}
   */
  public static UserPrincipal authenticate(
      final String accountAlias,
      final String username,
      final String password,
      final String newPassword
  ) throws AuthException, CredentialExpiredException {
    //TODO:STEVE: password auth for federated users
    final Account account = Accounts.lookupAccountByName( accountAlias );
    final EuareUser user = account.lookupUserByName( username );

    if ( newPassword == null ) {
      authenticate( user, password );
    } else if ( authenticateWithLdap( user ) ) {
      throw new AuthException( PASSWORD_CHANGE_NOT_SUPPORTED );
    } else try {
      authenticate( user, password );
      updatePassword( user, newPassword );//Authentication suceeded, update password.
    } catch ( CredentialExpiredException ex ) {
      updatePassword( user, newPassword );//Password is expired but user is authenticated, allow this update.
    }

    return Accounts.lookupPrincipalByUserId( user.getUserId( ), null );
  }

  private static void updatePassword( EuareUser user, String newPassword ) throws AuthException {
    if ( Strings.isNullOrEmpty( newPassword ) || user.getName( ).equals( newPassword ) ) {
      throw new AuthException( AuthException.INVALID_PASSWORD );
    }
    String newEncrypted = Crypto.generateEncryptedPassword( newPassword );
    user.setPassword( newEncrypted );
    user.setPasswordExpires( System.currentTimeMillis( ) + AuthenticationLimitProvider.Values.getDefaultPasswordExpiry( ) );
  }

  private static void checkPasswordExpiration( final EuareUser user ) throws CredentialExpiredException {
    if ( Objects.firstNonNull( user.getPasswordExpires(), Long.MAX_VALUE )
        < System.currentTimeMillis() ) {
      throw new CredentialExpiredException();
    }
  }

}
