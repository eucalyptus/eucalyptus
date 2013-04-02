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

/**
 *
 */
public class AccountUsernamePasswordCredentials extends WrappedCredentials<AccountUsernamePasswordCredentials.AccountCredentials> {

  public AccountUsernamePasswordCredentials( final String correlationId,
                                             final String account,
                                             final String username,
                                             final String password,
                                             final String newPassword ) {
    super( correlationId, new AccountCredentials( account, username, password, newPassword) );
  }

  public AccountUsername getAccountUsername() {
    return getLoginData().getAccountUsername();
  }

  public String getAccount() {
    return getLoginData().getAccount();
  }

  public String getUsername() {
    return getLoginData().getUsername();
  }

  public String getPassword() {
    return getLoginData().getPassword();
  }

  public String getNewPassword() {
    return getLoginData().getNewPassword();
  }

  /**
   * Public credentials
   */
  public static final class AccountUsername {
    private final String account;
    private final String username;

    public AccountUsername( final String account,
                            final String username ) {
      this.account = account;
      this.username = username;
    }

    public String getAccount() {
      return account;
    }

    public String getUsername() {
      return username;
    }

    public String toString() {
      return String.format( "%s@%s", getUsername(), getAccount() );
    }
  }

  /**
   * Public/private credentials
   */
  public static final class AccountCredentials {
    private final String password;
    private final String newPassword;
    private final AccountUsername accountUsername;

    public AccountCredentials( final String account,
                               final String username,
                               final String password,
                               final String newPassword ) {
      this.accountUsername = new AccountUsername( account, username );
      this.password = password;
      this.newPassword = newPassword;
    }

    public String getNewPassword( ) {
      return this.newPassword;
    }

    public AccountUsername getAccountUsername() {
      return accountUsername;
    }

    public String getAccount() {
      return accountUsername.getAccount();
    }

    public String getUsername() {
      return accountUsername.getUsername();
    }

    public String getPassword() {
      return password;
    }
  }
}
