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
  private String userProfileSearch;
  private String userKeySearch;
  private LoginAction loginAction;

  public LoginUserProfile( ) {
  }
  
  public LoginUserProfile( String userId, String userName, String accountName, String userProfileSearch, String userKeySearch, LoginAction action ) {
    this.setUserId( userId );
    this.setUserName( userName );
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
  
}
