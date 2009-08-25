package com.eucalyptus.auth;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Resource;

public class RemoteComponentCredentialBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( RemoteComponentCredentialBootstrapper.class );
  @Override
  public boolean load( Resource current ) throws Exception {
    while ( true ) {
      try {
        Credentials.init( );
        for ( Component c : Component.values( ) ) {
          try {
            if ( !SystemCredentialProvider.check( c ) ) SystemCredentialProvider.init( c );
          } catch ( Exception e ) {
            LOG.error( e );
          }
        }
        break;
      } catch ( Exception e ) {
        LOG.error( e );
        LOG.fatal( "Waiting for system credentials before proceeding with startup..." );
      }
      try {
        Thread.sleep( 2000 );
      } catch ( Exception e ) {}
    }
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    return true;
  }
  
  

}
