package com.eucalyptus.auth;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.api.UserInfoProvider;

/**
 * Unified APIs to access UserInfo. This is backed up by various data source, including database and LDAP. It is an auxiliary class to the Users class for
 * getting complete user information in the system.
 */
public class UserInfoStore {
  private static Logger           LOG = Logger.getLogger( Users.class );
  private static UserInfoProvider infoStore;
  
  public static void setUserInfoProvider( UserInfoProvider provider ) {
    synchronized ( Users.class ) {
      LOG.info( "Setting the user provider to: " + provider.getClass( ) );
      infoStore = provider;
    }
  }
  
  public static UserInfoProvider getUserInfoProvider( ) {
    return infoStore;
  }
  
  public static UserInfo getUserInfo( String name ) {
    return null;
  }
  
  public static void addUserInfo( UserInfo info ) {

  }
}
