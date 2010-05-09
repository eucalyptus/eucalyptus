package com.eucalyptus.auth;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.Bootstrap.Stage;

@Provides( Component.bootstrap )
@RunDuring( Bootstrap.Stage.UserCredentialsInit )
@DependsLocal( Component.eucalyptus )
public class LdapAuthBootstrapper extends Bootstrapper {
  
  @Override
  public boolean load( Stage current ) throws Exception {
    // TODO Auto-generated method stub
    return true;
  }
  
  @Override
  public boolean start( ) throws Exception {
    // TODO Auto-generated method stub
    return true;
  }
  
}
