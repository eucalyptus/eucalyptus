package com.eucalyptus.auth;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.ldap.LdapSync;
import com.eucalyptus.auth.policy.PolicyEngineImpl;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.system.Threads;

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
    if(ComponentIds.lookup( Eucalyptus.class ).isAvailableLocally()) {
      this.ensureSystemAdminExists( );
      // User info map key is case insensitive.
      // Older code may produce non-lowercase keys.
      // Normalize them if there is any.
      this.ensureUserInfoNormalized( );
      LdapSync.start( );
    }
    return true;
  }
  
  private void ensureUserInfoNormalized() {
    try {
      Account account = Accounts.lookupAccountByName( Account.SYSTEM_ACCOUNT );
      User sysadmin = account.lookupUserByName( User.ACCOUNT_ADMIN );
      if ( sysadmin.getInfo( ).containsKey( "Email" ) ) {
        Threads.newThread( new Runnable( ) {

          @Override
          public void run() {
            try {
              LOG.debug( "Starting to normalize user info for all users" );
              Accounts.normalizeUserInfo( );
            } catch ( Exception e ) {
              LOG.error( e, e );
            }
          }
          
        } ).start( ); 
      }
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
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
  public void destroy( ) throws Exception {
  }

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
  
  private void ensureSystemAdminExists( ) throws Exception {
    try {
      Account account = Accounts.lookupAccountByName( Account.SYSTEM_ACCOUNT );
      account.lookupUserByName( User.ACCOUNT_ADMIN );
    } catch ( Exception e ) {
      LOG.debug( "System admin does not exist. Adding it now." );
      // Order matters.
      try {
        Account system = Accounts.addSystemAccount( );
        User admin = system.addUser( User.ACCOUNT_ADMIN, "/", true, true, null );
        admin.createKey( );
        admin.createPassword( );
      } catch ( Exception ex ) {
        LOG.error( ex , ex );
      }
    }
  }
}
