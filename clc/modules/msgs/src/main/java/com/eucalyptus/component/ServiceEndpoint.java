/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.component;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import org.jgroups.util.UUID;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Threads;
import com.eucalyptus.system.Threads.ThreadPool;
import com.eucalyptus.util.Assertions;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Expendable;
import com.eucalyptus.util.HasParent;
import com.eucalyptus.util.Logs;
import com.eucalyptus.util.async.NOOP;
import com.eucalyptus.util.async.Request;
import com.eucalyptus.util.fsm.TransitionException;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class ServiceEndpoint extends AtomicReference<URI> implements HasParent<MessagableService> {
  private static Logger                      LOG           = Logger.getLogger( ServiceEndpoint.class );
  private static final int                   offerInterval = 2000;                                      //@Configurable
  private static final int                   pollInterval  = 2000;                                      //@Configurable
  private final MessagableService            parent;
  private final Boolean                      local;
  private final BlockingQueue<QueuedRequest> msgQueue;
  private final AtomicBoolean                running;
  @ConfigurableField( initial = "8", description = "Maximum number of concurrent messages sent to a single CC at a time." )
  public static Integer                      NUM_WORKERS   = 8;                                        //ASAP: restore configurability
  private ThreadPool                         workers;
  
  public ServiceEndpoint( MessagableService parent, Boolean local, URI uri ) {
    super( uri );
    this.parent = parent;
    this.local = local;
    Assertions.assertNotNull( uri );
    try {
      uri.parseServerAuthority( );
    } catch ( URISyntaxException e ) {
      LOG.error( e, e );
      throw new TransitionException( "Failed to initalize service: " + parent + " because of: " + e.getMessage( ), e );
    }
    this.running = new AtomicBoolean( false );
    this.msgQueue = new LinkedBlockingQueue<QueuedRequest>( );
    this.workers = this.createPool( );
  }
  
  private ThreadPool createPool( ) {
    return Threads.lookup( this.parent.getComponentId( ).getClass( ), ServiceEndpoint.class,
                           this.parent.getServiceConfiguration( ) + "-queue-" + UUID.randomUUID( ).toString( ) ).limitTo( NUM_WORKERS );
  }
  
  public Boolean isRunning( ) {
    return this.running.get( );
  }
  
  public void start( ) {
    if ( this.running.compareAndSet( false, true ) ) {
      if ( this.workers == null || this.workers.isShutdown( ) || this.workers.isTerminated( ) ) {
        this.workers = this.createPool( );
      }
      for ( int i = 0; i < NUM_WORKERS; i++ ) {
        this.workers.execute( new ServiceEndpointWorker( ) );
      }
    }
  }
  
  public void enqueue( final Request request ) {//FIXME: for now request is already wrapped in messaging state.
    if ( !this.running.get( ) ) {
      throw new RejectedExecutionException( "Endpoint is currently not running: " + this.parent.getServiceConfiguration( ).getFullName( ) );
    } else {
      QueuedRequest event = new QueuedRequest( request );
      EventRecord.caller( ServiceEndpoint.class, EventType.MSG_PENDING, this.parent.getName( ), event.getCallback( ).getClass( ).getSimpleName( ) ).info( );
      if ( !this.filter( event ) ) {
        try {
          while ( !this.msgQueue.offer( event, this.offerInterval, TimeUnit.MILLISECONDS ) );
          Logs.extreme( ).trace( event.getRequest( ).getRequest( ).toSimpleString( ) );
        } catch ( final InterruptedException e ) {
          LOG.debug( e, e );
          Thread.currentThread( ).interrupt( );
        }
      }
    }
  }
  
  class ServiceEndpointWorker implements Runnable {
    @SuppressWarnings( "unchecked" )
    public void run( ) {
      LOG.info( "Starting message queue: " + Thread.currentThread( ).getName( ) + " for endpoint: " + ServiceEndpoint.this.get( ) );
      while ( ServiceEndpoint.this.running.get( ) ) {
        try {
          QueuedRequest event;
          if ( ( event = ServiceEndpoint.this.msgQueue.poll( ServiceEndpoint.this.pollInterval, TimeUnit.MILLISECONDS ) ) != null ) {
            EventRecord.here( ServiceEndpointWorker.class, EventType.DEQUEUE, event.getCallback( ).getClass( ).getSimpleName( ),
                              event.getRequest( ).getRequest( ).toSimpleString( ) ).debug( );
            final long start = System.nanoTime( );
            event.getRequest( ).sendSync( ServiceEndpoint.this.getParent( ).getServiceConfiguration( ) );
            EventRecord.here( ServiceEndpointWorker.class, EventType.QUEUE, ServiceEndpoint.this.getParent( ).getName( ) )//
            .append( event.getCallback( ).getClass( ).getSimpleName( ) )//
            .append( EventType.QUEUE_TIME.name( ), Long.toString( start - event.getStartTime( ) ) )//
            .append( EventType.SERVICE_TIME.name( ), Long.toString( System.currentTimeMillis( ) - start ) )//
            .append( EventType.QUEUE_LENGTH.name( ), Long.toString( ServiceEndpoint.this.msgQueue.size( ) ) )//
            .append( EventType.MSG_SENT.name( ), event.getRequest( ).getRequest( ).toSimpleString( ) )//
            .info( );
          }
        } catch ( InterruptedException e1 ) {
          Thread.currentThread( ).interrupt( );
          return;
        } catch ( final ExecutionException e ) {
          LOG.error( e.getCause( ), e.getCause( ) );
        } catch ( final Throwable e ) {
          LOG.error( e, e );
        }
      }
      LOG.debug( "Shutting down cluster message queue: " + Thread.currentThread( ).getName( ) );
    }
  }
  
  public void stop( ) {
    this.running.set( false );
    this.workers.shutdownNow( ); //TODO:GRZE:FIXME there is a potential conflict here between releasing the threads and the incorrect state of the threadpool in the case where it has been shut down by a previou s deregister operation.
  }
  
  public MessagableService getParent( ) {
    return this.parent;
  }
  
  public Boolean isLocal( ) {
    return this.local;
  }
  
  public URI getUri( ) {
    return this.get( );
  }
  
  public String getHost( ) {
    return this.get( ).getHost( );
  }
  
  public Integer getPort( ) {
    return this.get( ).getPort( );
  }
  
  public String getServicePath( ) {
    return this.get( ).getPath( );
  }
  
  public InetSocketAddress getSocketAddress( ) {
    return new InetSocketAddress( this.get( ).getHost( ), this.get( ).getPort( ) );
  }
  
  @Override
  public String toString( ) {
    return String.format( "ServiceEndpoint %s %s %s mq:%d url:%s", this.parent.getName( ), this.local
      ? "local"
      : "remote", this.running.get( )
      ? "running"
      : "stopped",
                          this.msgQueue.size( ), this.get( ) );
  }
  
  static class QueuedRequest {
    private Request request;
    private Long    startTime; //ASAP: make it a timer.
                               
    private QueuedRequest( Request request ) {
      super( );
      this.request = request;
      this.startTime = System.currentTimeMillis( );
    }
    
    public Request getRequest( ) {
      return request;
    }
    
    public Long getStartTime( ) {
      return this.startTime;
    }
    
    public Callback getCallback( ) {
      return request.getCallback( );
    }
    
    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( !( o instanceof QueuedRequest ) ) return false;
      
      QueuedRequest that = ( QueuedRequest ) o;
      
      if ( !request.equals( that.request ) ) return false;
      
      return true;
    }
    
    @Override
    public int hashCode( ) {
      int result = 31 * request.hashCode( );
      return result;
    }
    
  }
  
  /**
   * @see com.eucalyptus.util.Expendable#duplicateOf(Object)
   * @param event
   * @return
   */
  private boolean filter( final QueuedRequest event ) {
    if ( !( event.getCallback( ) instanceof Expendable ) ) {
      return false;
    } else if ( event.getCallback( ) instanceof NOOP ) {
      RuntimeException ex = new RuntimeException( "Operation returning a NOOP." );
      LOG.debug( ex, ex );
      return true;
    } else {
      final Expendable cb = ( Expendable ) event.getCallback( );
      Iterables.filter( Lists.newArrayList( this.msgQueue ), new Predicate<QueuedRequest>( ) {
        @Override
        public boolean apply( QueuedRequest arg0 ) {
          if ( arg0.getClass( ).isAssignableFrom( event.getClass( ) ) && arg0 instanceof Expendable && cb.duplicateOf( arg0.getCallback( ) ) ) {
            EventRecord.caller( event.getCallback( ).getClass( ), EventType.QUEUE, ServiceEndpoint.this.parent.getName( ) )//
            .append( EventType.MSG_REJECTED.toString( ), EventType.QUEUE_LENGTH.name( ), Long.toString( ServiceEndpoint.this.msgQueue.size( ) ) )//
            .info( );
            return true;
          } else {
            return false;
          }
        }
      } );
    }
    return false;
  }
  
}
