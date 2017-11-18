/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
