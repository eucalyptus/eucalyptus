package com.eucalyptus.auth;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Depends;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.Resource;

@Provides( resource = Resource.SystemCredentials )
@Depends( remote = Component.eucalyptus )
public class RemoteComponentCredentialBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( RemoteComponentCredentialBootstrapper.class );

  @Override
  public boolean load( Resource current ) throws Exception {
    Credentials.init( );
    while ( true ) {
      if ( this.checkAllKeys( ) ) {
        for ( Component c : Component.values( ) ) {
          LOG.info( "Initializing system credentials for " + c.name( ) );
          SystemCredentialProvider.init( c );
          c.markHasKeys( );
        }
        break;
      } else {
        LOG.fatal( "Waiting for system credentials before proceeding with startup..." );
        try {
          Thread.sleep( 2000 );
        } catch ( Exception e ) {
        }
      }
    }
    return true;
  }

  private boolean checkAllKeys( ) {
    for ( Component c : Component.values( ) ) {
      if ( c.isEnabled( ) ) {
        try {
          if( !EucaKeyStore.getCleanInstance( ).containsEntry( c.name( ) ) ) {
            return false;
          }
        } catch ( Exception e ) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    return true;
  }

}
