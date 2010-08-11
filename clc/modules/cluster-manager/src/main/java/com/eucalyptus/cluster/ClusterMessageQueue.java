/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.cluster;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.callback.QueuedEventCallback;
import com.eucalyptus.cluster.callback.StopNetworkCallback;
import com.eucalyptus.cluster.callback.TerminateCallback;
import com.eucalyptus.cluster.callback.UnassignAddressCallback;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.records.EventType;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import com.eucalyptus.records.EventRecord;

@ConfigurableClass( root = "cluster", description = "Parameters controlling communication with cluster controllers." )
public class ClusterMessageQueue implements Runnable {
  
  private static Logger                    LOG           = Logger.getLogger( ClusterMessageQueue.class );
  private final BlockingQueue<QueuedEvent> msgQueue;
  private final int                        offerInterval = 500;
  private final int                        pollInterval  = 500;
  private final AtomicBoolean              finished;
  private final String                     clusterName;
  @ConfigurableField( initial = "8", description = "Maximum number of concurrent messages sent to a single CC at a time." )
  public static Integer                    NUM_WORKERS   = 8;
  private final ThreadFactory              threadFactory;
  private final ExecutorService            workers;
  
  public ClusterMessageQueue( final String clusterName ) {
    this.finished = new AtomicBoolean( false );
    this.msgQueue = new LinkedBlockingQueue<QueuedEvent>( );
    this.clusterName = clusterName;
    this.threadFactory = ClusterThreadFactory.getThreadFactory( clusterName );
    this.workers = Executors.newFixedThreadPool( NUM_WORKERS, this.threadFactory );
  }
  
  public void start( ) {
    for ( int i = 0; i < ClusterMessageQueue.NUM_WORKERS; i++ ) {
      this.workers.execute( this );
    }
  }
  
  public void enqueue( final QueuedEventCallback callback ) {
    QueuedEvent event = QueuedEvent.make( callback );
    EventRecord.caller( ClusterMessageQueue.class, EventType.MSG_PENDING, this.clusterName, event.getCallback( ).getClass( ).getSimpleName( ) ).info( );
    if ( !this.checkDuplicates( event ) ) {
      try {
        while ( !this.msgQueue.offer( event, this.offerInterval, TimeUnit.MILLISECONDS ) ) {
          LOG.trace(new RuntimeException(),new RuntimeException());
        }
      } catch ( final InterruptedException e ) {
        LOG.debug( e, e );
        Thread.currentThread( ).interrupt( );
      }
    }
  }
  
  private boolean checkDuplicates( final QueuedEvent event ) {
    if ( event.getCallback( ) instanceof QueuedEventCallback.NOOP ) {
      RuntimeException ex = new RuntimeException( "Operation returning a NOOP." );
      LOG.debug( ex, ex );
      return true;
    }
    for ( final QueuedEvent e : this.msgQueue ) {
      if ( ( event.getCallback( ) instanceof StopNetworkCallback ) && ( e.getCallback( ) instanceof StopNetworkCallback ) ) {
        final StopNetworkCallback incoming = ( StopNetworkCallback ) event.getCallback( );
        final StopNetworkCallback existing = ( StopNetworkCallback ) e.getCallback( );
        if ( incoming.getRequest( ).getNetName( ).equals( existing.getRequest( ).getNetName( ) ) ) {
          EventRecord.caller( event.getCallback( ).getClass( ), EventType.QUEUE, this.clusterName, EventType.MSG_REJECTED.toString( ),
                              EventType.QUEUE_LENGTH.name( ), Long.toString( this.msgQueue.size( ) ) ).debug( );
          return true;
        }
      } else if ( ( event.getCallback( ) instanceof TerminateCallback ) && ( e.getCallback( ) instanceof TerminateCallback ) ) {
        final TerminateCallback incoming = ( TerminateCallback ) event.getCallback( );
        final TerminateCallback existing = ( TerminateCallback ) e.getCallback( );
        if ( existing.getRequest( ).getInstancesSet( ).containsAll( incoming.getRequest( ).getInstancesSet( ) ) ) {
          EventRecord.caller( event.getCallback( ).getClass( ), EventType.QUEUE, this.clusterName, EventType.MSG_REJECTED.toString( ),
                              EventType.QUEUE_LENGTH.name( ), Long.toString( this.msgQueue.size( ) ) ).debug( );
          return true;
        }
      } else if ( ( event.getCallback( ) instanceof UnassignAddressCallback ) && ( e.getCallback( ) instanceof UnassignAddressCallback ) ) {
        final UnassignAddressCallback incoming = ( UnassignAddressCallback ) event.getCallback( );
        final UnassignAddressCallback existing = ( UnassignAddressCallback ) e.getCallback( );
        if ( incoming.getRequest( ).getSource( ).equals( existing.getRequest( ).getSource( ) )
             && incoming.getRequest( ).getDestination( ).equals( existing.getRequest( ).getDestination( ) ) ) {
          EventRecord.caller( event.getCallback( ).getClass( ), EventType.QUEUE, this.clusterName, EventType.MSG_REJECTED.toString( ),
                              EventType.QUEUE_LENGTH.name( ), Long.toString( this.msgQueue.size( ) ) ).debug( );
          return true;
        }
      }
    }
    return false;
  }
  
  public String getClusterName( ) {
    return this.clusterName;
  }
  
  @SuppressWarnings( "unchecked" )
  public void run( ) {
    LOG.debug( "Starting cluster message queue: " + Thread.currentThread( ).getName( ) );
    while ( !this.finished.get( ) ) {
      try {
        final QueuedEvent event = this.msgQueue.poll( this.pollInterval, TimeUnit.MILLISECONDS );
        if ( event != null ) {// msg == null if the queue was empty
          LOG.debug( "-> Dequeued message of type " + event.getCallback( ).getClass( ).getSimpleName( ) );
          final long start = System.currentTimeMillis( );
          try {
            event.getCallback( ).send( this.clusterName );
            //TODO: handle events which raised I/O exceptions to indicate the cluster state.
          } catch ( final Throwable e ) {
            LOG.debug( e, e );
          }
          LOG.info( EventRecord.here( ClusterMessageQueue.class, EventType.QUEUE, this.clusterName, event.getCallback( ).getClass( ).getSimpleName( ),
                                      EventType.QUEUE_TIME.name( ), Long.toString( start - event.getStartTime( ) ), EventType.SERVICE_TIME.name( ),
                                      Long.toString( System.currentTimeMillis( ) - start ), EventType.QUEUE_LENGTH.name( ),
                                      Long.toString( this.msgQueue.size( ) ) ) );
        }
      } catch ( final Throwable e ) {
        LOG.error( e, e );
      }
    }
    LOG.debug( "Shutting down cluster message queue: " + Thread.currentThread( ).getName( ) );
  }
  
  public void stop( ) {
    this.finished.lazySet( true );
    this.workers.shutdownNow( );
  }
  
  @Override
  public String toString( ) {
    return "ClusterMessageQueue{" + "msgQueue=" + this.msgQueue.size( ) + '}';
  }
  
  static class QueuedEvent {
    
    private QueuedEventCallback callback;
    private Long                startTime;
    
    public static QueuedEvent make( final QueuedEventCallback callback ) {
      return new QueuedEvent( callback );
    }
    
    private QueuedEvent( QueuedEventCallback callback ) {
      super( );
      this.callback = callback;
      this.startTime = System.currentTimeMillis( );
    }
    
    public Long getStartTime( ) {
      return this.startTime;
    }
    
    public QueuedEventCallback getCallback( ) {
      return callback;
    }
    
    public BaseMessage getEvent( ) {
      return callback.getRequest( );
    }
    
    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( !( o instanceof QueuedEvent ) ) return false;
      
      QueuedEvent that = ( QueuedEvent ) o;
      
      if ( !callback.equals( that.callback ) ) return false;
      
      return true;
    }
    
    @Override
    public int hashCode( ) {
      int result = 31 * callback.hashCode( );
      return result;
    }
    
  }
  
}
