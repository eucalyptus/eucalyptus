package com.eucalyptus.auth;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.util.AuthBootstrapHelper;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.ldap.LdapConfiguration;

@Provides( Empyrean.class )
@RunDuring( Bootstrap.Stage.UserCredentialsInit )
public class DatabaseAuthBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( DatabaseAuthBootstrapper.class );
  
  public static boolean ENABLE = !LdapConfiguration.ENABLE_LDAP;
  
  public boolean load( ) throws Exception {
    if (ENABLE) {
      DatabaseAuthProvider dbAuth = new DatabaseAuthProvider( );
      Users.setUserProvider( dbAuth );
      Groups.setGroupProvider( dbAuth );
      UserInfoStore.setUserInfoProvider( dbAuth );
    }
    return true;
  }
  
  public boolean start( ) throws Exception {
    if(Components.lookup( Eucalyptus.class ).isAvailableLocally( )) {
      if (ENABLE) {
        this.checkUserEnabled( );
        AuthBootstrapHelper.ensureStandardGroupsExists( );
        AuthBootstrapHelper.ensureAdminExists( );
      }
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
    return true;
  }

  
  private void checkUserEnabled( ) {
    EntityWrapper<UserEntity> db = Authentication.getEntityWrapper( );
    for ( UserEntity u : db.query( new UserEntity( ) ) ) {
      if ( u.isEnabled( ) != Boolean.FALSE ) {
        u.setEnabled( Boolean.TRUE );
      }
    }
  }
}
