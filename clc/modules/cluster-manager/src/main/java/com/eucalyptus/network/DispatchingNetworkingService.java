/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.network;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.network.DescribeNetworkingFeaturesResponseType;
import com.eucalyptus.compute.common.network.DescribeNetworkingFeaturesType;
import com.eucalyptus.compute.common.network.NetworkingService;
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesResponseType;
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesType;
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesResponseType;
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesType;
import com.eucalyptus.compute.common.network.UpdateInstanceResourcesResponseType;
import com.eucalyptus.compute.common.network.UpdateInstanceResourcesType;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.LockResource;

/**
 * NetworkingService implementation that delegates to underlying service.
 */
public class DispatchingNetworkingService implements NetworkingService {

  private static ReadWriteLock delegateLock = new ReentrantReadWriteLock( );
  private static NetworkMode networkingServiceDelegateMode;
  private static NetworkingService networkingServiceDelegate = networkingServiceProxy( ( proxy, method, args ) -> {
    if ( !method.getName( ).equals( "update" ) ) {
      throw new IllegalStateException( "Networking not initialized" );
    }
    return null;
  } );
  private static NetworkingService lockingDelegate = networkingServiceProxy( ( proxy, method, args ) ->
      method.invoke( LockResource.withLock( delegateLock.readLock( ), () -> networkingServiceDelegate ), args )
  );

  public static void updateNetworkService( @Nonnull final NetworkMode networkMode ) {
    if ( delegateNeedsUpdatingFor( networkMode ) ) {
      try ( final LockResource ignored = LockResource.lock( delegateLock.writeLock( ) ) ) {
        if ( delegateNeedsUpdatingFor( networkMode ) ) {
          networkingServiceDelegateMode = networkMode;
          networkingServiceDelegate = poolInvoked( new EdgeNetworkingService( ) );
        }
      }
    }
  }

  private static NetworkingService networkingServiceProxy( final InvocationHandler handler ) {
    return (NetworkingService) Proxy.newProxyInstance(
        DispatchingNetworkingService.class.getClassLoader( ),
        new Class[]{ NetworkingService.class },
        handler );
  }

  private static boolean delegateNeedsUpdatingFor( final NetworkMode networkMode ) {
    try ( LockResource ignored = LockResource.lock( delegateLock.readLock( ) ) ) {
      return networkingServiceDelegateMode == null ||
          !networkingServiceDelegateMode.equals( networkMode );
    }
  }

  private static NetworkingService poolInvoked( final NetworkingService service ) {
    return networkingServiceProxy( ( proxy, method, args ) ->
        Threads.enqueue(
            Eucalyptus.class,
            DispatchingNetworkingService.class,
            () -> method.invoke( service, args )
        ).get( ) );
  }

  private static NetworkingService getDelegate( ) {
    return lockingDelegate;
  }

  @Override
  public PrepareNetworkResourcesResponseType prepare( final PrepareNetworkResourcesType request ) {
    return getDelegate( ).prepare( request );
  }

  @Override
  public ReleaseNetworkResourcesResponseType release( final ReleaseNetworkResourcesType request ) {
    return getDelegate( ).release( request );
  }

  @Override
  public DescribeNetworkingFeaturesResponseType describeFeatures( final DescribeNetworkingFeaturesType request ) {
    return getDelegate( ).describeFeatures( request );
  }

  @Override
  public UpdateInstanceResourcesResponseType update( final UpdateInstanceResourcesType request ) {
    return getDelegate( ).update( request );
  }
}
