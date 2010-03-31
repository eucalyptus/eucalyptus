package com.eucalyptus.configurable;

import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Depends;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.Resource;

@Provides(resource=Resource.CredentialsConfiguration)
@Depends( resources = { Resource.DatabaseInit } )
public class PropertiesBootstrapper extends Bootstrapper {

  @Override
  public boolean load( Resource current ) throws Exception {
    ConfigurationProperties.doConfiguration( );
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    return false;
  }

}
