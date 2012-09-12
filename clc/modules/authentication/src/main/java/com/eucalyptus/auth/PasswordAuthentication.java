/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

import com.eucalyptus.auth.ldap.LdapSync;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.crypto.Crypto;

/**
 *
 */
public class PasswordAuthentication {
  public static final String INVALID_USERNAME_OR_PASSWORD = "Invalid username or password";

  public static void authenticate( final User user,
                                   final String password ) throws AuthException {
    if ( authenticateWithLdap( user ) ) try {
      LdapSync.authenticate(user, password);
    } catch ( LdapException e ) {
      throw new AuthException(INVALID_USERNAME_OR_PASSWORD);
    } else if ( !Crypto.verifyPassword(password, user.getPassword()) ) {
      throw new AuthException(INVALID_USERNAME_OR_PASSWORD);
    }
  }

  public static boolean authenticateWithLdap( final User user ) {
    return LdapSync.enabled( ) && !user.isSystemAdmin( ) && !user.isAccountAdmin( );
  }
}
