package com.eucalyptus.bootstrap;


import org.apache.log4j.Logger;

import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.cloud.ws.DNSControl;

@Provides(resource=Resource.PrivilegedContext,component=Component.dns)
@Depends(local=Component.dns)
public class DNSBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( DNSBootstrapper.class );
  private static DNSBootstrapper singleton;

  public static Bootstrapper getInstance( ) {
    synchronized ( DNSBootstrapper.class ) {
      if ( singleton == null ) {
        singleton = new DNSBootstrapper( );
        LOG.info( "Creating DNS Bootstrapper instance." );
      } else {
        LOG.info( "Returning DNS Bootstrapper instance." );
      }
    }
    return singleton;
  }

  @Override
  public boolean check( ) throws Exception {
    return true;
  }

  @Override
  public boolean destroy( ) throws Exception {
    return true;
  }

  @Override
  public boolean load(Resource current ) throws Exception {
	  LOG.info("Initializing DNS");
	  DNSControl.initialize();
	  return true;
  }

  @Override
  public boolean start( ) throws Exception {
    return true;
  }

  @Override
  public boolean stop( ) throws Exception {
    return true;
  }

}
