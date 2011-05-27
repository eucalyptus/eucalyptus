package com.eucalyptus.webui.client.service;

import java.io.Serializable;

public class LoginUserProfile implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private String userName;  
  private String accountName;

  public LoginUserProfile( ) {
  }
  
  public LoginUserProfile( String userName, String accountName ) {
    this.setUserName( userName );
    this.setAccountName( accountName );
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
  
}
