package com.eucalyptus.auth;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.AuthBootstrapHelper;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.ldap.LdapConfiguration;

@Provides( Empyrean.class )
@RunDuring( Bootstrap.Stage.UserCredentialsInit )
@DependsLocal( Eucalyptus.class )
public class LdapAuthBootstrapper extends Bootstrapper {
  private static Logger       LOG    = Logger.getLogger( LdapAuthBootstrapper.class );
  
  public static final boolean ENABLE = LdapConfiguration.ENABLE_LDAP;
  
  @Override
  public boolean load( ) throws Exception {
    if ( ENABLE ) {
      LdapAuthProvider ldapAuth = new LdapAuthProvider( );
      Users.setUserProvider( ldapAuth );
      Groups.setGroupProvider( ldapAuth );
      UserInfoStore.setUserInfoProvider( ldapAuth );
    }
    return true;
  }
  
  @Override
  public boolean start( ) throws Exception {
    if ( ENABLE ) {  
      if ( !ensureLdapTree( ) ) {
        return false;
      }
      AuthBootstrapHelper.ensureStandardGroupsExists( );
      AuthBootstrapHelper.ensureAdminExists( );
      loadCache( );
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

  
  private boolean ensureLdapTree( ) {
    boolean result = EucaLdapHelper.createRoot( );
    result = result && EucaLdapHelper.createGroupRoot( );
    result = result && EucaLdapHelper.createUserRoot( );
    LOG.debug( "Built or found LDAP tree" );
    return result;
  }
  
  private void loadCache( ) {
    LdapCache.getInstance( ).reloadGroups( Groups.listAllGroups( ) );
    for ( User user : Users.listAllUsers( ) ) {
      LdapCache.getInstance( ).addUser( user );
    }
  }
}
