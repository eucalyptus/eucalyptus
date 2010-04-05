package com.eucalyptus.ws;

import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Dispatcher;
import com.eucalyptus.event.SystemClock;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.client.ServiceDispatcher;

@Provides(Component.any)
@RunDuring(Bootstrap.Stage.Verification)
public class ServiceVerifyBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( ServiceVerifyBootstrapper.class );

  @Override
  public boolean start( ) throws Exception {
    for ( Map.Entry<String, Dispatcher> e : ServiceDispatcher.getEntries( ) ) {
      LOG.info( "-> key=" + e.getKey( ) + " entry=" + e.getValue( ) );
    }
    for ( Component c : Component.values( ) ) {
      LOG.debug( "-> Verifying access by component type: " + c );
      for ( Dispatcher s : ServiceDispatcher.lookupMany( c ) ) {
        LOG.debug( "--> Found: " + s );
      }
    }
    if( !Components.lookup( Component.walrus ).getLifecycle( ).isInitialized( ) ) {
      LOG.warn( LogUtil.header( "Failed to find a Walrus configuration.  You must register one in order to use Eucalyptus." ) );      
    }
    if( !Components.lookup( Component.storage ).getLifecycle( ).isInitialized( ) ) {
      LOG.warn( LogUtil.header( "Failed to find a Storage Controller configuration.  You must register one in order to use Eucalyptus." ) );
    }
    SystemClock.setupTimer();
    // TODO: handle clusters here too.
    return true;
  }

  @Override
  public boolean load( Stage current ) throws Exception {
    return true;
  }
}
