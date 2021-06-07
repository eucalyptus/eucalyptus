/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
