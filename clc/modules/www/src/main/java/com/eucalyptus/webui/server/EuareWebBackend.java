package com.eucalyptus.webui.server;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.client.service.Session;

public class EuareWebBackend {

  private static final Logger LOG = Logger.getLogger( EuareWebBackend.class );
  
  public static User getUser( String userName, String accountName ) throws EucalyptusServiceException {
    if ( userName == null || accountName == null ) {
      throw new EucalyptusServiceException( "Empty user name or account name" );
    }
    try {
      Account account = Accounts.lookupAccountByName( accountName );
      User user = account.lookupUserByName( userName );
      return user;
    } catch ( Exception e ) {
      LOG.error( "Failed to verify user " + userName + "@" + accountName );
      throw new EucalyptusServiceException( "Failed to verify user " + userName + "@" + accountName );
    }
  }
  
  public static void checkPassword( User user, String password ) throws EucalyptusServiceException {
    if ( !user.getPassword( ).equals( Crypto.generateHashedPassword( password ) ) ) {
      throw new EucalyptusServiceException( "Incorrect password" );
    }
  }
  
}
