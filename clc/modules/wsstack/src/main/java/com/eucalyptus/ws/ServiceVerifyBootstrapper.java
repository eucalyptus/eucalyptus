package com.eucalyptus.ws;

import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.Resource;
import com.eucalyptus.event.SystemClock;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.client.ServiceDispatcher;

@Provides( resource = Resource.Verification )
public class ServiceVerifyBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( ServiceVerifyBootstrapper.class );

  @Override
  public boolean start( ) throws Exception {
    for ( Map.Entry<String, ServiceDispatcher> e : ServiceDispatcher.getEntries( ) ) {
      LOG.info( "-> key=" + e.getKey( ) + " entry=" + e.getValue( ) );
    }
    for ( Component c : Component.values( ) ) {
      LOG.debug( "-> Verifying access by component type: " + c );
      for ( ServiceDispatcher s : ServiceDispatcher.lookupMany( c ) ) {
        LOG.debug( "--> Found: " + s );
      }
    }
    if ( ServiceDispatcher.lookupMany( Component.walrus ).size( ) < 1 && !Component.walrus.isLocal( ) ) {
      Component.walrus.setInitialized( false );
      LOG.warn( LogUtil.header( "Failed to find a Walrus configuration.  You must register one in order to use Eucalyptus." ) );
    }
    if ( ServiceDispatcher.lookupMany( Component.storage ).size( ) < 1 && !Component.storage.isLocal( ) ) {
      Component.storage.setInitialized( false );
      LOG.warn( LogUtil.header( "Failed to find a Storage Controller configuration.  You must register one in order to use Eucalyptus." ) );
    }
    SystemClock.setupTimer();
    // TODO: handle clusters here too.
    return true;
  }

  @Override
  public boolean load( Resource current ) throws Exception {
    return true;
  }
}
