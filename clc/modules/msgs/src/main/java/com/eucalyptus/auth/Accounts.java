package com.eucalyptus.auth;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.api.AccountProvider;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;

public class Accounts {
  private static final Logger LOG = Logger.getLogger( Accounts.class );
  
  private static AccountProvider accounts;
  
  public static void setAccountProvider( AccountProvider provider ) {
    synchronized ( Accounts.class ) {
      LOG.info( "Setting the account provider to: " + provider.getClass( ) );
      accounts = provider;
    }
  }
  
  public static AccountProvider getAccountProvider( ) {
    return accounts;
  }
  
  public static Account addAccount( String accountName ) throws AuthException {
    return Accounts.getAccountProvider( ).addAccount( accountName );
  }
  
  public static void deleteAccount( String accountName, boolean forceDeleteSystem, boolean recursive ) throws AuthException {
    Accounts.getAccountProvider( ).deleteAccount( accountName, forceDeleteSystem, recursive );
  }
  
  public static List<Group> listAllGroups( String accountName ) throws AuthException {
    return Accounts.getAccountProvider( ).listAllGroups( accountName );
  }
  
  public static List<User> listAllUsers( String accountName ) throws AuthException {
    return Accounts.getAccountProvider( ).listAllUsers( accountName );
  }
  
  public static List<Account> listAllAccounts( ) throws AuthException {
    return Accounts.getAccountProvider( ).listAllAccounts( );
  }
  
  public static Account addSystemAccount( ) throws AuthException {
    return addAccount( Account.SYSTEM_ACCOUNT );
  }
  
}
