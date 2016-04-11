/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.network.applicator;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.callback.BroadcastNetworkInfoCallback;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.UnconditionalCallback;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BroadcastNetworkInfoResponseType;

/**
 *
 */
public class BroadcastingApplicator implements Applicator {

  private static final Logger logger = Logger.getLogger( BroadcastingApplicator.class );

  private static final ConcurrentMap<String,Long> activeBroadcastMap = Maps.newConcurrentMap( );

  @Override
  public void apply( final ApplicatorContext context, final ApplicatorChain chain ) throws ApplicatorException {
    final String networkInfo = MarshallingApplicatorHelper.getMarshalledNetworkInfo( context );
    final String encodedNetworkInfo =
        new String( B64.standard.enc( networkInfo.getBytes( Charsets.UTF_8 ) ), Charsets.UTF_8 );

    final BroadcastNetworkInfoCallback callback = new BroadcastNetworkInfoCallback( encodedNetworkInfo );
    for ( final com.eucalyptus.cluster.Cluster cluster : context.getClusters( ) ) {
      final Long broadcastTime = System.currentTimeMillis( );
      if ( null == activeBroadcastMap.putIfAbsent( cluster.getPartition( ), broadcastTime ) ) {
        try {
          AsyncRequests.newRequest( callback.newInstance( ) ).then( new UnconditionalCallback<BroadcastNetworkInfoResponseType>() {
            @Override
            public void fire( ) {
              activeBroadcastMap.remove( cluster.getPartition( ), broadcastTime );
            }
          } ).dispatch( cluster.getConfiguration( ) );
        } catch ( Exception e ) {
          activeBroadcastMap.remove( cluster.getPartition( ), broadcastTime );
          logger.error( "Error broadcasting network information to cluster (" + cluster.getPartition() + ") ("+cluster.getName()+")", e );
        }
      } else {
        logger.warn( "Skipping network information broadcast for active partition " + cluster.getPartition( ) );
      }
    }

    chain.applyNext( context );
  }

  public static class BroadcastingApplicatorEventListener implements EventListener<ClockTick> {
    private final int activeBroadcastTimeoutMins = 3;

    public static void register( ) {
      Listeners.register( ClockTick.class, new BroadcastingApplicatorEventListener( ) );
    }

    @SuppressWarnings("UnnecessaryQualifiedReference")
    @Override
    public void fireEvent( final ClockTick event ) {
      for ( final Map.Entry<String,Long> entry : BroadcastingApplicator.activeBroadcastMap.entrySet( ) ) {
        if ( entry.getValue() + TimeUnit.MINUTES.toMillis( activeBroadcastTimeoutMins ) < System.currentTimeMillis( ) &&
            BroadcastingApplicator.activeBroadcastMap.remove( entry.getKey( ), entry.getValue( ) ) ) {
          logger.warn( "Timed out active network information broadcast for partition " + entry.getKey( ) );
        }
      }
    }
  }
}
