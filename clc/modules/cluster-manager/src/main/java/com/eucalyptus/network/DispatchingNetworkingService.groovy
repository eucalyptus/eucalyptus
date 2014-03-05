/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.network

import com.eucalyptus.component.id.Eucalyptus
import com.eucalyptus.compute.common.network.NetworkingService
import com.eucalyptus.system.Threads
import com.eucalyptus.util.LockResource
import groovy.transform.CompileStatic
import groovy.transform.PackageScope

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
  private static String networkingServiceDelegateName
  private static NetworkingService networkingServiceDelegate =
      networkingServiceProxy{ throw new IllegalStateException( "Networking not initialized" ) }
  @Delegate
  private static NetworkingService lockingDelegate =
      networkingServiceProxy{ Object proxy, Method method, Object[] args ->
        method.invoke( LockResource.withLock( delegateLock.readLock( ) ){ networkingServiceDelegate }, args ) }

  @PackageScope
  static void updateNetworkService( final String networkService ) {
    if ( delegateNeedsUpdatingFor( networkService ) ) {
      LockResource.withLock( delegateLock.writeLock( ) ) {
        if ( delegateNeedsUpdatingFor( networkService ) ) {
          networkingServiceDelegateName = networkService
          networkingServiceDelegate = poolInvoked( 'EDGE' == networkService ?
              new EdgeNetworkingService( ) :
              new GenericNetworkingService( ) )
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

  private static boolean delegateNeedsUpdatingFor( final String networkMode ) {
    LockResource.withLock( delegateLock.readLock( ) ) {
      networkingServiceDelegateName == null || networkingServiceDelegateName != networkMode
    }
  }

  private static NetworkingService poolInvoked( final NetworkingService service ) {
    networkingServiceProxy{ Object proxy, Method method, Object[] args ->
          Threads.enqueue( Eucalyptus, DispatchingNetworkingService ){ method.invoke( service, args ) }.get( ) }
  }

}
