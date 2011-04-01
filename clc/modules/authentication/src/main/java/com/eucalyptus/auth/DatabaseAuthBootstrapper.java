package com.eucalyptus.auth;

import java.security.AuthProvider;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.ldap.LdapSync;
import com.eucalyptus.auth.policy.PolicyEngineImpl;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.crypto.Certs;
import com.eucalyptus.crypto.CryptoProvider;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.entities.EntityWrapper;

@Provides( Empyrean.class )
@RunDuring( Bootstrap.Stage.UserCredentialsInit )
public class DatabaseAuthBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( DatabaseAuthBootstrapper.class );
    
  public boolean load( ) throws Exception {
  	DatabaseAuthProvider dbAuth = new DatabaseAuthProvider( );
  	Accounts.setAccountProvider( dbAuth );
  	Permissions.setPolicyEngine( new PolicyEngineImpl( ) );
    return true;
  }
  
  public boolean start( ) throws Exception {
  if(Components.lookup( Eucalyptus.class ).isAvailableLocally( )) {
      this.eusureSystemAdminExist( );
      LdapSync.start( );
    
      // Remove once done.
      //AuthTest.test( );
    
    }
    return true;
  }
  
  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#enable()
   */
  @Override
  public boolean enable( ) throws Exception {

    
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#stop()
   */
  @Override
  public boolean stop( ) throws Exception {
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#destroy()
   */
  @Override
  public void destroy( ) throws Exception {}

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#disable()
   */
  @Override
  public boolean disable( ) throws Exception {
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#check()
   */
  @Override
  public boolean check( ) throws Exception {
    return LdapSync.check( );
  }
  private void eusureSystemAdminExist( ) throws Exception {
    try {
      Account account = Accounts.lookupAccountByName( Account.SYSTEM_ACCOUNT );
      account.lookupUserByName( User.ACCOUNT_ADMIN );
    } catch ( AuthException e ) {
      // Order matters.
      Account system = Accounts.addSystemAccount( );
      User admin = system.addUser( User.ACCOUNT_ADMIN, "/", true, true, null );
      admin.createToken( );
      admin.createConfirmationCode( );
      admin.createPassword( );
      LOG.warn( "System admin does not exist. Adding it now.", e );
    }
  }
}
