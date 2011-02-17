package com.eucalyptus.auth;

import java.security.cert.X509Certificate;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.api.AccountProvider;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.util.FullName;

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
  
  public static Account lookupAccountByName( String accountName ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupAccountByName( accountName );
  }
  
  public static Account lookupAccountById( String accountId ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupAccountById( accountId );
  }
  
  public static Account addAccount( String accountName ) throws AuthException {
    return Accounts.getAccountProvider( ).addAccount( accountName );
  }
  
  public static void deleteAccount( String accountName, boolean forceDeleteSystem, boolean recursive ) throws AuthException {
    Accounts.getAccountProvider( ).deleteAccount( accountName, forceDeleteSystem, recursive );
  }
  
  public static List<Account> listAllAccounts( ) throws AuthException {
    return Accounts.getAccountProvider( ).listAllAccounts( );
  }
  
  public static Account addSystemAccount( ) throws AuthException {
    return Accounts.getAccountProvider( ).addAccount( Account.SYSTEM_ACCOUNT );
  }
  
  public static List<User> listAllUsers( ) throws AuthException {
    return Accounts.getAccountProvider( ).listAllUsers( );
  }
  
  public static boolean shareSameAccount( String userId1, String userId2 ) {
    return Accounts.getAccountProvider( ).shareSameAccount( userId1, userId2 );
  }
  
  @Deprecated
  public static User lookupUserByName( String userName ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupUserByName( userName );
  }

  public static User lookupUserById( String userId ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupUserById( userId );
  }
  
  public static User lookupUserByAccessKeyId( String keyId ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupUserByAccessKeyId( keyId );
  }
  
  public static User lookupUserByCertificate( X509Certificate cert ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupUserByCertificate( cert );
  }
  
  public static Group lookupGroupById( String groupId ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupGroupById( groupId );
  }
  
  public static Certificate lookupCertificate( X509Certificate cert ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupCertificate( cert );
  }
  
  public static AccessKey lookupAccessKeyById( String keyId ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupAccessKeyById( keyId );
  }
  
  public static User lookupSystemAdmin( ) throws AuthException {
    Account system = Accounts.getAccountProvider( ).lookupAccountByName( Account.SYSTEM_ACCOUNT );
    return system.lookupUserByName( User.ACCOUNT_ADMIN );
  }
  
  public static String getFirstActiveAccessKeyId( User user ) throws AuthException {
    for ( AccessKey k : user.getKeys( ) ) {
      if ( k.isActive( ) ) {
        return k.getId( );
      }
    }
    throw new AuthException( "No active access key for " + user );
  }

  /**
   * @deprecated TEMPORARY
   */
  @Deprecated
  public static FullName lookupUserFullNameById( String userId ) {
    try {
      return UserFullName.get( Accounts.lookupUserById( userId ) );
    } catch ( AuthException ex ) {
      throw new RuntimeException( "Failed to identify user with id " + userId + " something has gone seriously wrong.", ex );
    }
  }
  
}
