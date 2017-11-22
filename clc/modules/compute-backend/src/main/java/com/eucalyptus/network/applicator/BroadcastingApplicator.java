/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.network.applicator;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.common.broadcast.BNetworkInfo;
import com.eucalyptus.cluster.callback.BroadcastNetworkInfoCallback;
import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.UnconditionalCallback;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.eucalyptus.cluster.common.msgs.BroadcastNetworkInfoResponseType;

/**
 *
 */
public class BroadcastingApplicator implements Applicator {

  private static final Logger logger = Logger.getLogger( BroadcastingApplicator.class );

  private static final ConcurrentMap<String,Long> activeBroadcastMap = Maps.newConcurrentMap( );

  @Override
  public void apply( final ApplicatorContext context, final ApplicatorChain chain ) throws ApplicatorException {
    final BNetworkInfo netInfo = context.getNetworkInfo( );
    final String networkInfo = MarshallingApplicatorHelper.getMarshalledNetworkInfo( context );
    final String encodedNetworkInfo =
        new String( B64.standard.enc( networkInfo.getBytes( Charsets.UTF_8 ) ), Charsets.UTF_8 );

    final BroadcastNetworkInfoCallback callback = new BroadcastNetworkInfoCallback(
        encodedNetworkInfo,
        netInfo.version( ).getOrNull( ),
        netInfo.appliedVersion( ).getOrNull( )
    );
    for ( final Cluster cluster : context.getClusters( ) ) {
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
