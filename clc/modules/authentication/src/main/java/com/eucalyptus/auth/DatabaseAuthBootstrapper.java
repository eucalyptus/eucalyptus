package com.eucalyptus.auth;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.ldap.LdapSync;
import com.eucalyptus.auth.policy.PolicyEngineImpl;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.entities.Counters;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.VmType;

@Provides( Component.bootstrap )
@RunDuring( Bootstrap.Stage.UserCredentialsInit )
public class DatabaseAuthBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( DatabaseAuthBootstrapper.class );
    
  public boolean load( ) throws Exception {
  	DatabaseAuthProvider dbAuth = new DatabaseAuthProvider( );
  	Users.setUserProvider( dbAuth );
  	Groups.setGroupProvider( dbAuth );
  	Accounts.setAccountProvider( dbAuth );
  	Policies.setPolicyProvider( dbAuth );
  	Authorizations.setPolicyEngine( new PolicyEngineImpl( ) );
    return true;
  }
  
  public boolean start( ) throws Exception {
    return true;
  }
  
  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#enable()
   */
  @Override
  public boolean enable( ) throws Exception {
    this.eusureSystemAdminExist( );
    this.ensureCountersExist( );
    this.ensureVmTypesExist( );
    LdapSync.start( );
    
    // Remove once done.
    //AuthTest.test( );
    
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
  
  private void ensureVmTypesExist( ) {
    EntityWrapper<VmType> db = new EntityWrapper<VmType>( "eucalyptus_general" );
    try {
      if ( db.query( new VmType( ) ).size( ) == 0 ) { //TODO: make defaults configurable?
        db.add( new VmType( "m1.small", 1, 2, 128 ) );
        db.add( new VmType( "c1.medium", 1, 5, 256 ) );
        db.add( new VmType( "m1.large", 2, 10, 512 ) );
        db.add( new VmType( "m1.xlarge", 2, 20, 1024 ) );
        db.add( new VmType( "c1.xlarge", 4, 20, 2048 ) );
      }
      db.commit( );
    } catch ( Exception e ) {
      db.rollback( );
    }
  }

  private void ensureCountersExist( ) {
    Counters.getNextId( );
  }
  
  private void eusureSystemAdminExist( ) throws Exception {
    try {
      User user = Users.lookupSystemAdmin( );
      if ( user != null ) {
        return;
      }
    } catch ( AuthException e ) {
      LOG.warn( "System admin does not exist. Adding it now.", e );
    }
    // Order matters.
    Accounts.addSystemAccount( );
    Users.addSystemAdmin( );
  }

}
