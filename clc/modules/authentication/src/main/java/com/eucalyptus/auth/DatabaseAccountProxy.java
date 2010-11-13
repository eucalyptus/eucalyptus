package com.eucalyptus.auth;

import java.security.Principal;
import java.util.Enumeration;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.entities.AccountEntity;
import com.eucalyptus.auth.principal.Account;

public class DatabaseAccountProxy implements Account {

  private static final long serialVersionUID = 1L;

  private static Logger LOG = Logger.getLogger( DatabaseAccountProxy.class );
  
  private AccountEntity delegate;
  
  public DatabaseAccountProxy( AccountEntity delegate ) {
    this.delegate = delegate;
  }

  @Override
  public String getName( ) {
    return this.delegate.getName( );
  }

  @Override
  public String getAccountId( ) {
    return this.delegate.getAccountId( );
  }
  
  @Override
  public String toString( ) {
    return this.delegate.toString( );
  }
  
}
