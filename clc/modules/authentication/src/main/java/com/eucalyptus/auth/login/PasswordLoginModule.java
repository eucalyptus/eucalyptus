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
package com.eucalyptus.auth.login;

import javax.security.auth.login.CredentialExpiredException;
import javax.security.auth.login.FailedLoginException;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PasswordAuthentication;
import com.eucalyptus.auth.api.BaseLoginModule;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.google.common.base.Objects;

/**
 *
 */
public class PasswordLoginModule extends BaseLoginModule<AccountUsernamePasswordCredentials> {

  @Override
  public boolean accepts() {
    return super.getCallbackHandler( ) instanceof AccountUsernamePasswordCredentials;
  }

  @Override
  public boolean authenticate( final AccountUsernamePasswordCredentials credentials ) throws Exception {
    final String accountName = credentials.getAccount();
    final String username = credentials.getUsername();
    final String password = credentials.getPassword();

    final Account account;
    final User user;
    try {
      account = Accounts.lookupAccountByName( accountName );
      user = account.lookupUserByName( username );
    } catch ( final AuthException e ) {
      throw new FailedLoginException();
    }

    PasswordAuthentication.authenticate( user, password );

    if ( Objects.firstNonNull( user.getPasswordExpires(), Long.MAX_VALUE )
        < System.currentTimeMillis() ) {
      throw new CredentialExpiredException();
    }

    super.setCredential( credentials.getAccountUsername() );
    super.setPrincipal( user );

    return true;
  }
}
