/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.network

import com.eucalyptus.component.id.Eucalyptus
import com.eucalyptus.compute.common.network.NetworkingService
import com.eucalyptus.system.Threads
import com.eucalyptus.util.LockResource
import groovy.transform.CompileStatic

import javax.annotation.Nonnull
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * NetworkingService implementation that delegates to underlying service.
 */
@CompileStatic
class DispatchingNetworkingService {

  private static ReadWriteLock delegateLock = new ReentrantReadWriteLock( )
  private static NetworkMode networkingServiceDelegateMode
  private static NetworkingService networkingServiceDelegate =
      networkingServiceProxy{ Object proxy, Method method, Object[] args ->
        if ( !method.name.equals( 'update' ) ) throw new IllegalStateException( "Networking not initialized" ); null }

  @Delegate
  private static NetworkingService lockingDelegate =
      networkingServiceProxy{ Object proxy, Method method, Object[] args ->
        method.invoke( LockResource.withLock( delegateLock.readLock( ) ){ networkingServiceDelegate }, args ) }

  static void updateNetworkService( @Nonnull final NetworkMode networkMode ) {
    if ( delegateNeedsUpdatingFor( networkMode ) ) {
      LockResource.withLock( delegateLock.writeLock( ) ) {
        if ( delegateNeedsUpdatingFor( networkMode ) ) {
          networkingServiceDelegateMode = networkMode
          networkingServiceDelegate = poolInvoked(new EdgeNetworkingService())
        }
        void
      }
    }
  }

  private static NetworkingService networkingServiceProxy( final Closure closure ) {
    (NetworkingService) Proxy.newProxyInstance(
        DispatchingNetworkingService.getClassLoader( ),
        [ NetworkingService ] as Class<?>[ ],
        closure as InvocationHandler )
  }

  private static boolean delegateNeedsUpdatingFor( final NetworkMode networkMode ) {
    LockResource.withLock( delegateLock.readLock( ) ) {
      networkingServiceDelegateMode == null || networkingServiceDelegateMode != networkMode
    }
  }

  private static NetworkingService poolInvoked( final NetworkingService service ) {
    networkingServiceProxy{ Object proxy, Method method, Object[] args ->
          Threads.enqueue( Eucalyptus, DispatchingNetworkingService ){ method.invoke( service, args ) }.get( ) }
  }

}
