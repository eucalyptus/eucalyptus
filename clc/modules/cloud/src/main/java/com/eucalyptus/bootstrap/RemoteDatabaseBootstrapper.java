package com.eucalyptus.bootstrap;

import org.apache.log4j.Logger;

@Provides( resource = Resource.Database )
@Depends( resources = Resource.SystemCredentials, remote = Component.eucalyptus )
public class RemoteDatabaseBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( RemoteDatabaseBootstrapper.class );
  @Override
  public boolean load( Resource current ) throws Exception {
    LOG.info( "-> database host: " + System.getProperty("euca.db.host") );
    LOG.info( "-> database port: " + System.getProperty("euca.db.port") );
    if( System.getProperty("euca.db.password")  == null ) {
      System.setProperty("euca.db.password", "");
    }
    LOG.info( "-> database password: " + System.getProperty("euca.db.password") );
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    return true;
  }

}
