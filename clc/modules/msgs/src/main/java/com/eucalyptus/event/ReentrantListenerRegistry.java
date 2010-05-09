package com.eucalyptus.event;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Record;
import com.eucalyptus.util.LogUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.eucalyptus.records.EventRecord;

public class ReentrantListenerRegistry<T> {
  private static Logger              LOG = Logger.getLogger( ReentrantListenerRegistry.class );
  private Multimap<T, EventListener> listenerMap;
  private Lock                       modificationLock;
  
  public ReentrantListenerRegistry( ) {
    super( );
    this.listenerMap = Multimaps.newArrayListMultimap( );
    this.modificationLock = new ReentrantLock( );
  }
  
  public void register( T type, EventListener listener ) {
    EventRecord.caller( ReentrantListenerRegistry.class, EventType.LISTENER_REGISTERED, type.getClass( ).getSimpleName( ), listener.getClass( ).getCanonicalName( ) ).info( );
    this.modificationLock.lock( );
    try {
      if ( !this.listenerMap.containsEntry( type, listener ) ) {
        this.listenerMap.put( type, listener );
      }
    } finally {
      this.modificationLock.unlock( );
    }
  }
  
  public void deregister( T type, EventListener listener ) {
    EventRecord.caller( ReentrantListenerRegistry.class, EventType.LISTENER_DEREGISTERED, type.getClass( ).getSimpleName( ), listener.getClass( ).getCanonicalName( ) ).info( );
    this.modificationLock.lock( );
    try {
      this.listenerMap.remove( type, listener );
    } finally {
      this.modificationLock.unlock( );
    }
  }
  
  public void destroy( T type ) {
    this.modificationLock.lock( );
    for( EventListener e : this.listenerMap.get( type ) ) {
      EventRecord.caller( ReentrantListenerRegistry.class, EventType.LISTENER_DESTROY_ALL, type.getClass( ).getSimpleName( ), e.getClass( ).getCanonicalName( ) ).info( );
    }
    try {
      this.listenerMap.removeAll( type );
    } finally {
      this.modificationLock.unlock( );
    }
  }
  
  public void fireEvent( T type, Event e ) throws EventVetoedException {
    List<EventListener> listeners;
    this.modificationLock.lock( );
    try {
      listeners = Lists.newArrayList( this.listenerMap.get( type ) );
    } finally {
      this.modificationLock.unlock( );
    }
    this.fireEvent( e, listeners );
  }
  
  private void fireEvent( Event e, List<EventListener> listeners ) throws EventVetoedException {
    for ( EventListener ce : listeners ) {
      ce.advertiseEvent( e );
      if ( e.isVetoed( ) ) {
        String cause = e.getCause( ) != null ? e.getCause( ) : "no cause given";
        EventRecord.here( ReentrantListenerRegistry.class, EventType.LISTENER_EVENT_VETOD, ce.getClass( ).getSimpleName( ), e.toString( ), cause ).info( );
        throw new EventVetoedException( String.format( "Event %s was vetoed by listener %s: %s", LogUtil.dumpObject( e ), LogUtil.dumpObject( ce ), cause ) );
      }
    }
    for ( EventListener ce : listeners ) {
      Record record = EventRecord.here( ReentrantListenerRegistry.class, EventType.LISTENER_EVENT_FIRED, ce.getClass( ).getSimpleName( ), e.toString( ));
      if ( e instanceof ClockTick ) {
        record.trace( );
      } else {
        record.debug( );
      }
      ce.fireEvent( e );
      if ( e.getFail( ) != null ) {
        LOG.info( e.getFail( ) );
        LOG.debug( e.getFail( ), e.getFail( ) );
        throw new EventVetoedException( e.getFail( ) );
      }
    }
  }
  
}
