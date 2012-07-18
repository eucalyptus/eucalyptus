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

package com.eucalyptus.webui.client.service;

import java.io.Serializable;

import com.eucalyptus.auth.principal.Account;

public class LoginUserProfile implements Serializable {

  private static final long serialVersionUID = 1L;
  
  public static enum LoginAction {
    FIRSTTIME, // Needs first time information filling
    EXPIRATION // Needs password updating
  }
  
  private String userId;
  private String userName;
  private String accountName;
  private String accountId;
  private String userProfileSearch;
  private String userKeySearch;
  private LoginAction loginAction;

  public LoginUserProfile( ) {
  }
  
  public LoginUserProfile( String userId, String userName, String accountId, String accountName, String userProfileSearch, String userKeySearch, LoginAction action ) {
    this.setUserId( userId );
    this.setUserName( userName );
    this.setAccountId( accountId );
    this.setAccountName( accountName );
    this.setUserProfileSearch( userProfileSearch );
    this.setUserKeySearch( userKeySearch );
    this.setLoginAction( action );
  }
  
  public boolean isSystemAdmin( ) {
    return "eucalyptus".equals( accountName );
  }

  public void setUserName( String userName ) {
    this.userName = userName;
  }

  public String getUserName( ) {
    return userName;
  }

  public void setAccountName( String accountName ) {
    this.accountName = accountName;
  }

  public String getAccountName( ) {
    return accountName;
  }
  
  public String toString( ) {
    return userName + "@" + accountName;
  }

  public void setUserProfileSearch( String userProfileSearch ) {
    this.userProfileSearch = userProfileSearch;
  }

  public String getUserProfileSearch( ) {
    return userProfileSearch;
  }

  public void setUserId( String userId ) {
    this.userId = userId;
  }

  public String getUserId( ) {
    return userId;
  }

  public void setLoginAction( LoginAction loginAction ) {
    this.loginAction = loginAction;
  }

  public LoginAction getLoginAction( ) {
    return loginAction;
  }

  public String getUserKeySearch( ) {
    return this.userKeySearch;
  }

  public void setUserKeySearch( String userKeySearch ) {
    this.userKeySearch = userKeySearch;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }
  
}
