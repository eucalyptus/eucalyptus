/**
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cluster.proxy;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.cluster.service.ClusterServiceEnv;
import com.eucalyptus.cluster.service.ClusterServiceId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceDependencyException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.ThrowingFunction;

/**
 * Bootstrapper that only allows the Cluster component to enable when the proxy is up (if in use)
 */
@RunDuring( Bootstrap.Stage.RemoteServicesInit )
@Provides( ClusterServiceId.class )
public class ProxyClusterBootstrapper extends Bootstrapper {

  @Override
  public boolean check( ) throws Exception {
    ensureProxyIfRequired( true, p -> {p.check(); return p;} );
    return true;
  }

  @Override
  public void destroy( ) throws Exception {
    ensureProxyIfRequired( false );
  }

  @Override
  public boolean enable( ) throws Exception {
    ensureProxyIfRequired( false, p -> {p.enable(); return p;} );
    return true;
  }

  @Override
  public boolean disable( ) throws Exception {
    ensureProxyIfRequired( false, p -> {p.disable(); return p;} );
    return true;
  }

  @Override
  public boolean load( ) throws Exception {
    ensureProxyIfRequired( false );
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    ensureProxyIfRequired( false, p -> {p.start(); return p;} );
    return true;
  }

  @Override
  public boolean stop( ) throws Exception {
    ensureProxyIfRequired( false, p -> {p.stop(); return p;} );
    return true;
  }

  private void ensureProxyIfRequired( final boolean requireEnabled ) throws Exception {
    ensureProxyIfRequired( requireEnabled, p -> p );
  }

  private void ensureProxyIfRequired(
      final boolean requireEnabled,
      final ThrowingFunction<ProxyClusterManager,ProxyClusterManager,Exception> action
  ) throws Exception {
    if ( ClusterServiceEnv.requireProxy( ) ) {
      ensureProxy( requireEnabled, action );
    }
  }

  private void ensureProxy(
      final boolean requireEnabled,
      final ThrowingFunction<ProxyClusterManager,ProxyClusterManager,Exception> action
  ) throws Exception {
    ProxyClusterManager.checkLocal( );
    try {
      ProxyClusterManager.local( ).map( action.asUndeclaredFunction( ) );
    } catch ( final RuntimeException ex ) {
      throw Exceptions.rethrow( ex, Exception.class );
    }
    if ( requireEnabled &&
        !ProxyClusterManager.local( ).map( ProxyClusterManager::isEnabled ).getOrElse( false ) ) {
      throw new ServiceDependencyException( "The "
          + ComponentIds.lookup( ClusterServiceId.class ).name( )
          + " service depends upon a locally ENABLED "
          + ComponentIds.lookup( ProxyClusterController.class ).name( ) );
    }
  }
}
