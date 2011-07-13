package com.eucalyptus.event;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Record;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class ReentrantListenerRegistry<T> {
  private static Logger              LOG = Logger.getLogger( ReentrantListenerRegistry.class );
  private Multimap<T, EventListener> listenerMap;
  private Lock                       modificationLock;
  
  public ReentrantListenerRegistry( ) {
    super( );
    this.listenerMap = ArrayListMultimap.create( );
    this.modificationLock = new ReentrantLock( );
  }
  
  public void register( T type, EventListener listener ) {
    if ( type instanceof Enum ) {
      EventRecord.caller( ReentrantListenerRegistry.class, EventType.LISTENER_REGISTERED, type.getClass( ).getSimpleName( ), ( ( Enum ) type ).name( ),
                          listener.getClass( ).getSimpleName( ) ).trace( );
    } else {
      EventRecord.caller( ReentrantListenerRegistry.class, EventType.LISTENER_REGISTERED, type.getClass( ).getSimpleName( ),
                          listener.getClass( ).getSimpleName( ) ).trace( );
    }
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
    if ( type instanceof Enum ) {
      EventRecord.caller( ReentrantListenerRegistry.class, EventType.LISTENER_DEREGISTERED, type.getClass( ).getSimpleName( ), ( ( Enum ) type ).name( ),
                          listener.getClass( ).getSimpleName( ) ).trace( );
    } else {
      EventRecord.caller( ReentrantListenerRegistry.class, EventType.LISTENER_DEREGISTERED, type.getClass( ).getSimpleName( ),
                          listener.getClass( ).getSimpleName( ) ).trace( );
    }
    this.modificationLock.lock( );
    try {
      this.listenerMap.remove( type, listener );
    } finally {
      this.modificationLock.unlock( );
    }
  }
  
  public void destroy( T type ) {
    this.modificationLock.lock( );
    for ( EventListener e : this.listenerMap.get( type ) ) {
      EventRecord.caller( ReentrantListenerRegistry.class, EventType.LISTENER_DESTROY_ALL, type.getClass( ).getSimpleName( ), e.getClass( ).getCanonicalName( ) ).trace( );
    }
    try {
      this.listenerMap.removeAll( type );
    } finally {
      this.modificationLock.unlock( );
    }
  }
  
  public void fireEvent( T type, Event e ) throws EventFailedException {
    List<EventListener> listeners;
    this.modificationLock.lock( );
    try {
      listeners = Lists.newArrayList( this.listenerMap.get( type ) );
    } finally {
      this.modificationLock.unlock( );
    }
    this.fireEvent( e, listeners );
  }
  
  private void fireEvent( Event e, List<EventListener> listeners ) throws EventFailedException {
    List<Throwable> errors = Lists.newArrayList( );
    for ( EventListener ce : listeners ) {
      Record record = EventRecord.here( ReentrantListenerRegistry.class, EventType.LISTENER_EVENT_FIRED, ce.getClass( ).getSimpleName( ), e.toString( ) );
      if ( e instanceof ClockTick || e instanceof Hertz ) {
//        record.trace( );
      } else {
        record.debug( );
      }
      try {
        ce.fireEvent( e );
      } catch ( Throwable ex ) {
        ex = new EventFailedException( "Failed to fire event: listener=" + ce.getClass( ).getCanonicalName( ) + " event=" + e.toString( ) + " because of: "
                                       + ex.getMessage( ), Exceptions.filterStackTrace( ex ) );
        errors.add( ex );
      }
    }
    for ( Throwable ex : errors ) {
      LOG.error( ex.getMessage( ), ex );
    }
  }
  
}
