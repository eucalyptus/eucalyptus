package com.eucalyptus.ws;

import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Dispatcher;
import com.eucalyptus.event.SystemClock;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.client.ServiceDispatcher;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

@Provides( com.eucalyptus.bootstrap.Component.any )
@RunDuring(Bootstrap.Stage.Verification)
public class ServiceVerifyBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( ServiceVerifyBootstrapper.class );

  @Override
  public boolean start( ) throws Exception {
    LOG.info( LogUtil.header( "Components" ) );
    requireComponent( Components.delegate.cluster, "Cluster Controller" );
    requireComponent( Components.delegate.walrus, "Walrus" );
    requireComponent( Components.delegate.storage, "Storage Controller" );

    LOG.info( LogUtil.header( "Component Configurations" ) );
    Iterables.all( Components.list( ), Components.componentPrinter( ) );
    
    LOG.info( LogUtil.subheader( "Service Dispatchers" ) );
    Iterables.all( ServiceDispatcher.values( ), Components.dispatcherPrinter( ) );

    SystemClock.setupTimer( );
    return true;
  }
  
  private static void requireComponent( com.eucalyptus.bootstrap.Component comp, String prettyName ) {
    try {
      Component component = Components.lookup( comp );
      if ( !component.isInitialized( ) || !component.isRunning( ) ) {
        LOG.warn( LogUtil.header( "Failed to find a " + prettyName + " configuration.  You must register one in order to use Eucalyptus." ) );
      }
    } catch ( NoSuchElementException ex ) {
      LOG.warn( LogUtil.header( "Failed to find a " + prettyName + " configuration.  You must register one in order to use Eucalyptus." ) );
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
    }
  }
  
  @Override
  public boolean load( Bootstrap.Stage stage ) throws Exception {
    try {
      LOG.info( LogUtil.header( "Component Configurations" ) );
      Iterables.all( Components.list( ), Components.componentPrinter( ) );
    } catch ( Throwable ex ) {
      LOG.error( ex , ex );
    }
    return true;
  }
}
