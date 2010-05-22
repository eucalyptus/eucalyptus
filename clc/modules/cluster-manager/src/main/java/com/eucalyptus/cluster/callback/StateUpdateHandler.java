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
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

public class StateUpdateHandler implements EventListener {
  private static Logger LOG = Logger.getLogger( StateUpdateHandler.class );
  private static final ConcurrentMap<String,StateUpdateHandler> clusterMap = Maps.newConcurrentHashMap( );
  private final ConcurrentMap<Class,AtomicBoolean> inflightMap = Maps.newConcurrentHashMap( );
  private final ConcurrentMap<Class,QueuedEventCallback> callbackMap = Maps.newConcurrentHashMap( );
  private final Cluster cluster;
  
  public static void create( Cluster cluster, QueuedEventCallback callback ) {
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

  public void addCallback( QueuedEventCallback cb ) {
    if( this.callbackMap.putIfAbsent( cb.getClass( ), cb ) == null ) {
      this.inflightMap.put( cb.getClass( ), new AtomicBoolean( false ) );
    } else {
      LOG.debug( "Ignoring addition of timed callback for cluster "+ this.cluster.getName( )+ " which is already configured: " + cb.getClass( ) );
    }
  }
  
  @Override
  public void advertiseEvent( Event event ) {}

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
