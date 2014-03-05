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

import com.eucalyptus.address.AddressingDispatcher
import com.eucalyptus.util.async.Request
import edu.ucsb.eucalyptus.msgs.BaseMessage
import edu.ucsb.eucalyptus.msgs.UnassignAddressType
import groovy.transform.CompileStatic
import org.apache.log4j.Logger

import java.util.concurrent.atomic.AtomicBoolean

/**
 *
 */
@CompileStatic
class EdgeNetworking {
  private static final Logger logger = Logger.getLogger( EdgeNetworking )

  private static final AtomicBoolean configured = new AtomicBoolean( false )
  private static final AtomicBoolean reported = new AtomicBoolean( false )
  private static final AtomicBoolean enabled = new AtomicBoolean( false )
  private static final AtomicBoolean initialized = new AtomicBoolean( false )

  static void setConfigured( boolean value ) {
    configured.set( value )
    checkEnabled( )
  }

  static void setReported( boolean value ) {
    reported.set( value )
    checkEnabled( )
  }

  static boolean isEnabled( ) {
    enabled.get( )
  }

  private static boolean shouldEnable( ) {
    configured.get( ) || reported.get( )
  }

  private static void checkEnabled( ) {
    if ( shouldEnable( ) && !isEnabled( ) ) {
      enable( )
    } else if ( ( !shouldEnable() && isEnabled( ) ) || initialized.compareAndSet( false, true ) ) {
      disable( )
    }
  }

  private static void enable() {
    enabled.set( true );
    configureNetworking( 'EDGE', AddressingDispatcher.Dispatcher.SHORTCUT, new EdgeAddressingInterceptor( ) )
    logger.info( "EDGE networking enabled" )
  }

  private static void disable() {
    enabled.set( false );
    configureNetworking( 'Generic', AddressingDispatcher.Dispatcher.STANDARD, null )
    logger.info( "Generic networking enabled" )
  }

  private static final void configureNetworking( final String serviceName,
                                                 final AddressingDispatcher.Dispatcher dispatcher,
                                                 final AddressingDispatcher.AddressingInterceptor interceptor ) {
    DispatchingNetworkingService.updateNetworkService( serviceName );
    AddressingDispatcher.configure( dispatcher, interceptor )
  }

  private static final class EdgeAddressingInterceptor extends AddressingDispatcher.AddressingInterceptorSupport {
    @Override
    protected void onMessage(
        final Request<? extends BaseMessage, ? extends BaseMessage> request,
        final String partition
    ) {
      NetworkInfoBroadcaster.requestNetworkInfoBroadcast( )
      if ( request.getRequest( ) instanceof UnassignAddressType ) {
        UnassignAddressType unassign = (UnassignAddressType) request.getRequest( )
        PublicAddresses.markDirty( unassign.getSource( ), partition )
      }
    }
  }
}
