package com.eucalyptus.configurable;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.Bootstrap.Stage;

@Provides(Component.configuration)
@RunDuring(Bootstrap.Stage.SystemCredentialsInit)
public class PropertiesBootstrapper extends Bootstrapper {

  @Override
  public boolean load( Stage current ) throws Exception {
    ConfigurationProperties.doConfiguration( );
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    return true;
  }

}
