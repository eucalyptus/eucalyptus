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
class NetworkingDriver {
  private static final Logger logger = Logger.getLogger( NetworkingDriver )

  protected static final AtomicBoolean configured = new AtomicBoolean( false )
  protected static final AtomicBoolean reported = new AtomicBoolean( false )
  protected static final AtomicBoolean enabled = new AtomicBoolean( false )
  protected static final AtomicBoolean initialized = new AtomicBoolean( false )
  protected static String networkingMode = new String("");

  /**
   * Informs the class wether or not the networking configuration is coming from the property setting or the
   * environment.
   *
   * @param value set to true to indicate the network configuration is coming from the network property or set to
   *        false to indicate the network configuration is coming from the environment.
   */
  static void setConfigured( boolean value ) {
    configured.set( value )
    checkEnabled( )
  }

  /**
   * Indicates that a given networking mode has been reported by a Clusters. If the reported mode is set to
   * either EDGE, MANAGED or MANAGED-NOVLAN, we will considered that we have a reported valid mode. Otherwise,
   * we will considered the mode as "unreported".
   *
   * @param mode the networking mode as reported (e.g. "EDGE", "MANAGED", "MANAGED-NOVLAN", "")
   */
  static void setReported( String mode ) {
    // We only support EDGE, MANAGED or MANAGED-NOVLAN anything else is considered unreported
    if ("EDGE".equals(mode) || "MANAGED".equals(mode) || "MANAGED-NOVLAN".equals(mode)) {
      reported.set( true )
    } else {
      reported.set( false )
    }

    // Set the mode for later use
    networkingMode = mode;
    checkEnabled( )
  }

  /**
   * An indicator as wehter we have a valid networking mode reported and/or a valid configuration from the
   * networking configuration property.
   *
   * @return true if we are configured true the property and/or we have a known networking mode reported. Otherwise
   *         false is returned.
   */
  static boolean isEnabled( ) {
    enabled.get( )
  }

  /**
   * Helper API. We should be considered enabled if we have a valid networking mode reported or if we have
   * a valid configuration provided through the networking configuration property.
   *
   * @return true if we are configured true the property and/or we have a known networking mode reported. Otherwise
   *         false is returned.
   */
  private static boolean shouldEnable( ) {
    configured.get( ) || reported.get( )
  }

  /**
   * Helper API to check wether or not we have an "enabled" state transition. If we have a transition,
   * the proper actions will be taken
   */
  private static void checkEnabled( ) {
    if ( shouldEnable( ) && !isEnabled( ) ) {
      enable( )
    } else if ( ( !shouldEnable() && isEnabled( ) ) || initialized.compareAndSet( false, true ) ) {
      disable( )
    }
  }

  /**
   * A state transition has been detected and we are enabling this support. We will set our state to "enabled" and
   * then call configureNetworking with our known networking mode to properly setup what we need for this mode.
   * Essentially we need to bypass the address dispatcher. At this point, the mode should have been validated to be one
   * of which we support.
   */
  private static void enable() {
    enabled.set( true );
    configureNetworking( networkingMode,
                         AddressingDispatcher.Dispatcher.SHORTCUT,
                         new BroadcastAddressingInterceptor( ) )
    logger.info( networkingMode + " networking enabled" )
  }

  /**
   * A state transition has been detected and we are disabling this support. We will set our state to "disabled" and
   * then call configureNetworking with the 'EDGE' service name to reset our setup. At this point we want to
   * re-enable the standard address dispatcher.
   */
  private static void disable() {
    enabled.set( false );
    configureNetworking( 'EDGE', AddressingDispatcher.Dispatcher.STANDARD, null )
    logger.info( "EDGE networking enabled" )
  }

  /**
   * Sets up the proper networking service dispatcher as well as configure the address dispatcher interceptor.
   *
   * @param serviceName The service name for the proper network service dispatcher. This should be either ("Generic",
   *                    "EDGE", "MANAGED" or "MANAGED-NOVLAN")
   * @param dispatcher The addressing dispatcher type (SHORTCUT, STANDARD, etc.)
   * @param interceptor The instance of the addressing dispatcher class or null
   */
  private static final void configureNetworking( final String serviceName,
                                                 final AddressingDispatcher.Dispatcher dispatcher,
                                                 final AddressingDispatcher.AddressingInterceptor interceptor ) {
    DispatchingNetworkingService.updateNetworkService( serviceName );
    AddressingDispatcher.configure( dispatcher, interceptor )
  }

  /**
   * Network Driver addressing interceptor class.
   */
  private static final class BroadcastAddressingInterceptor extends AddressingDispatcher.AddressingInterceptorSupport {
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
