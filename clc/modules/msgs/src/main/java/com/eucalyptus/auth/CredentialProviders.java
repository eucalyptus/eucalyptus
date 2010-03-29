package com.eucalyptus.auth;

import org.apache.log4j.Logger;

public class CredentialProviders {

  private static Logger LOG = Logger.getLogger( CredentialProviders.class );
  private static UserCredentialProvider userCredentials;

  public static void setUserProvider( UserCredentialProvider provider ) {
    synchronized( CredentialProviders.class ) {
      LOG.info( "Setting the credential provider to: " + provider.getClass( ) );
      userCredentials = provider;
    }
  }
  
  public static UserCredentialProvider getUserProvider() {
     return userCredentials;
  }

  
  
}
