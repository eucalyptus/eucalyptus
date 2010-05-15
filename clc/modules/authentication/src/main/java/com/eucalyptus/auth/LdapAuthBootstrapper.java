package com.eucalyptus.auth;

import com.eucalyptus.auth.ldap.EntryExistsException;
import com.eucalyptus.auth.EucaLdapHelper;
import com.eucalyptus.auth.ldap.LdapAttributes;
import com.eucalyptus.auth.ldap.LdapConfiguration;
import com.eucalyptus.auth.ldap.LdapContextManager;
import com.eucalyptus.auth.ldap.LdapException;
import com.eucalyptus.auth.util.AuthBootstrapHelper;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import org.apache.log4j.Logger;

@Provides( Component.bootstrap )
@RunDuring( Bootstrap.Stage.UserCredentialsInit )
@DependsLocal( Component.eucalyptus )
public class LdapAuthBootstrapper extends Bootstrapper {
  private static Logger       LOG    = Logger.getLogger( LdapAuthBootstrapper.class );
  public static final boolean ENABLE = !DatabaseAuthBootstrapper.ENABLE;
  
  @Override
  public boolean load( Stage current ) throws Exception {
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
    }
    return true;
  }
  
  private boolean ensureLdapTree( ) {
    boolean result = EucaLdapHelper.createRoot( );
    result = result && EucaLdapHelper.createGroupRoot( );
    result = result && EucaLdapHelper.createUserRoot( );
    LOG.debug( "Built or found LDAP tree" );
    return result;
  }
}
