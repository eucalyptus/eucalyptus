package com.eucalyptus.event;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.eucalyptus.util.LogUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

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
    LOG.info( String.format( "Registering event listener for %s: %s", type, LogUtil.dumpObject( listener ) ) );
    this.modificationLock.lock( );
    try {
      if( !this.listenerMap.containsEntry( type, listener ) ) {
        this.listenerMap.put( type, listener );
      }
    } finally {
      this.modificationLock.unlock( );
    }
  }

  public void deregister( T type, EventListener listener ) {
    LOG.info( String.format( "Deregistering event listener for %s: %s", type, LogUtil.dumpObject( listener ) ) );
    this.modificationLock.lock( );
    try {
      this.listenerMap.remove( type, listener );
    } finally {
      this.modificationLock.unlock( );
    }
  }

  public void destroy( T type ) {
    LOG.info( String.format( "Destroying event listeners for %s", LogUtil.dumpObject( type ) ) );
    this.modificationLock.lock( );
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
      if ( e.isVetoed( ) ) { throw new EventVetoedException( String.format( "Event %s was vetoed by listener %s: %s", LogUtil.dumpObject( e ), LogUtil.dumpObject( ce ), e.getCause( ) != null ? e.getCause( ) : "no cause given" ) ); }
    }
    for ( EventListener ce : listeners ) {
      String logString = String.format( "Firing event %s on listener %s", LogUtil.dumpObject( e ), LogUtil.dumpObject( ce ) );
      if( e instanceof ClockTick ) {
        LOG.trace( logString );
      } else {
        LOG.debug( logString );        
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
