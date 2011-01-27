package com.eucalyptus.accounts;

import com.eucalyptus.component.ComponentId;

public class AccountsService extends ComponentId {
  
  public AccountsService( ) {
    super( "Accounts" );
  }

  
  @Override
  public String getLocalEndpointName( ) {
    return "vm://AccountsInternal";
  }

  @Override
  public Boolean isCloudLocal( ) {
    return true;
  }
  
  @Override
  public Boolean hasDispatcher( ) {
    return true;
  }
  
  @Override
  public Boolean isAlwaysLocal( ) {
    return false;
  }
  
}
