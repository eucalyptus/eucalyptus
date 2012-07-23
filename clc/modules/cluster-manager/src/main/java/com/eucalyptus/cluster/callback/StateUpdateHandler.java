/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.cluster.callback;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.async.MessageCallback;
import com.eucalyptus.util.async.RemoteCallback;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

public class StateUpdateHandler implements EventListener {
  private static Logger LOG = Logger.getLogger( StateUpdateHandler.class );
  private static final ConcurrentMap<String,StateUpdateHandler> clusterMap = Maps.newConcurrentMap( );
  private final ConcurrentMap<Class,AtomicBoolean> inflightMap = Maps.newConcurrentMap( );
  private final ConcurrentMap<Class,MessageCallback> callbackMap = Maps.newConcurrentMap( );
  private final Cluster cluster;
  
  public static void create( Cluster cluster, RemoteCallback callback ) {
    StateUpdateHandler handler = new StateUpdateHandler( cluster );
    StateUpdateHandler.clusterMap.putIfAbsent( cluster.getName( ), handler );
    handler = StateUpdateHandler.clusterMap.get( cluster.getName( ) );
    EventRecord.here( StateUpdateHandler.class, EventType.CLUSTER_STATE_HANDLER_REGISTERED, cluster.getName( ), callback.getClass( ).getCanonicalName( ) );
  }
  
  private StateUpdateHandler( Cluster cluster ) {
    this.cluster = cluster;
  }

  public boolean timedTrigger( Event c ) {
    if ( c instanceof ClockTick ) {
      return ( ( ClockTick ) c ).isBackEdge( );
    } else {
      return false;
    }
  }

  public void addCallback( MessageCallback cb ) {
    if( this.callbackMap.putIfAbsent( cb.getClass( ), cb ) == null ) {
      this.inflightMap.put( cb.getClass( ), new AtomicBoolean( false ) );
    } else {
      LOG.debug( "Ignoring addition of timed callback for cluster "+ this.cluster.getName( )+ " which is already configured: " + cb.getClass( ) );
    }
  }

  @Override
  public void fireEvent( Event event ) {
    if( this.timedTrigger( event ) ) {
      Iterables.all( callbackMap.keySet( ), new Predicate<Class>() {

        @Override
        public boolean apply( Class arg0 ) {
          if( StateUpdateHandler.this.inflightMap.get( arg0 ).compareAndSet( false, true ) ) {
            //TODO: RELEASE: wrap this up here.
          }
          return false;
        }} );
    }
  }

}
