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
package com.eucalyptus.auth.euare.login;

import javax.security.auth.login.CredentialExpiredException;
import com.eucalyptus.auth.euare.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.AuthenticationLimitProvider;
import com.eucalyptus.auth.LdapException;
import com.eucalyptus.auth.euare.ldap.LdapSync;
import com.eucalyptus.auth.euare.principal.EuareAccount;
import com.eucalyptus.auth.euare.principal.EuareUser;
import com.eucalyptus.auth.principal.User;
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

  private static boolean authenticateWithLdap( final User user ) {
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
   */
  public static UserPrincipal authenticate(
      final String accountAlias,
      final String username,
      final String password,
      final String newPassword
  ) throws AuthException, CredentialExpiredException {
    if ( newPassword != null ) { // password change is for local region
      final EuareAccount account = Accounts.lookupAccountByName( accountAlias );
      final EuareUser user = account.lookupUserByName( username );
      if ( authenticateWithLdap( user ) ) {
        throw new AuthException( PASSWORD_CHANGE_NOT_SUPPORTED );
      }
      if ( !Crypto.verifyPassword(password, user.getPassword()) ) {
        throw new AuthException(INVALID_USERNAME_OR_PASSWORD);
      } else {
        updatePassword( user, newPassword );
        checkPasswordExpiration( user );
      }
      return Accounts.lookupPrincipalByUserId( user.getUserId( ) );
    } else { // can be remote region if not LDAP
      final String accountNumber = Accounts.lookupAccountIdByAlias( accountAlias );
      final UserPrincipal user  = Accounts.lookupPrincipalByAccountNumberAndUsername( accountNumber, username );
      if ( authenticateWithLdap( user ) ) {
        try {
          LdapSync.authenticate( Accounts.lookupUserById( user.getUserId( ) ), password );
        } catch ( LdapException e ) {
          throw new AuthException(INVALID_USERNAME_OR_PASSWORD);
        }
      } else if ( !Crypto.verifyPassword( password, user.getPassword( ) ) ) {
        throw new AuthException(INVALID_USERNAME_OR_PASSWORD);
      } else {
        checkPasswordExpiration( user );
      }
      return user;
    }
  }

  private static void updatePassword( EuareUser user, String newPassword ) throws AuthException {
    if ( Strings.isNullOrEmpty( newPassword ) || user.getName( ).equals( newPassword ) ) {
      throw new AuthException( AuthException.INVALID_PASSWORD );
    }
    String newEncrypted = Crypto.generateEncryptedPassword( newPassword );
    user.setPassword( newEncrypted );
    user.setPasswordExpires( System.currentTimeMillis( ) + AuthenticationLimitProvider.Values.getDefaultPasswordExpiry( ) );
  }

  private static void checkPasswordExpiration( final User user ) throws CredentialExpiredException {
    if ( Objects.firstNonNull( user.getPasswordExpires(), Long.MAX_VALUE )
        < System.currentTimeMillis() ) {
      throw new CredentialExpiredException();
    }
  }

}
