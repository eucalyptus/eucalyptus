package com.eucalyptus.auth;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.api.UserInfoProvider;
import com.eucalyptus.util.Tx;

/**
 * Unified APIs to access UserInfo. This is backed up by various data source, including database and LDAP. It is an auxiliary class to the Users class for
 * getting complete user information in the system.
 */
public class UserInfoStore {
  private static Logger           LOG = Logger.getLogger( Users.class );
  private static UserInfoProvider infoStore;
  
  public static void setUserInfoProvider( UserInfoProvider provider ) {
    synchronized ( Users.class ) {
      LOG.info( "Setting the user info provider to: " + provider.getClass( ) );
      infoStore = provider;
    }
  }
  
  public static UserInfoProvider getUserInfoProvider( ) {
    return infoStore;
  }
  
  public static UserInfo getUserInfo( UserInfo search ) throws NoSuchUserException {
    return UserInfoStore.getUserInfoProvider( ).getUserInfo( search );
  }
  
  public static void addUserInfo( UserInfo info ) throws UserExistsException {
    UserInfoStore.getUserInfoProvider( ).addUserInfo( info );
  }
  
  public static void updateUserInfo( String userName, Tx<UserInfo> infoTx ) throws NoSuchUserException {
    UserInfoStore.getUserInfoProvider( ).updateUserInfo( userName, infoTx );
  }
  
  public static void deleteUserInfo( String userName ) throws NoSuchUserException {
    UserInfoStore.getUserInfoProvider( ).deleteUserInfo( userName );
  }
}
