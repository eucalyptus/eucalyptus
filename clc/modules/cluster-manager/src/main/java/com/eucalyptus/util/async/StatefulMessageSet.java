/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.util.async;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.EucalyptusClusterException;
import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class StatefulMessageSet<E extends Enum<E>> {
  private static Logger                        LOG           = Logger.getLogger( StatefulMessageSet.class );
  private final Multimap<E, Request>           messages      = ArrayListMultimap.create( );
  private final List<Runnable>                 cleanupTasks  = Lists.newArrayList( );
  private final ConcurrentLinkedQueue<Request> pendingEvents = new ConcurrentLinkedQueue<Request>( );
  private final E[]                            states;
  private E                                    state;
  private final E                              endState;
  private final E                              failState;
  private final Cluster                        cluster;
  private final Long                           startTime;
  
  /**
   * Collection of messages which need to honor a certain ordering. The state
   * array is a list of enum values where:
   * <ul>
   * <li>Index 0: is the start state</li>
   * <li>Index length-2: is the end state</li>
   * <li>Index length-1: is the rollback state</li>
   * </ul>
   * A transition will increase the currentState's ordinal by 1, drain the
   * messages to a pending queue and wait for all messages to be serviced before
   * proceeding.
   * 
   * @param cluster
   * @param states
   */
  public StatefulMessageSet( final Cluster cluster, final E[] states ) {
    this.cluster = cluster;
    this.states = states;
    this.state = states[0];
    this.endState = states[states.length - 2];
    this.failState = states[states.length - 1];
    this.startTime = System.currentTimeMillis( );
  }
  
  private E rollback( ) {
    return ( this.state = this.failState );
  }
  
  public void addRequest( final E state, final Request asyncRequest ) {
    EventRecord.caller( StatefulMessageSet.class, EventType.VM_PREPARE, state.name( ), asyncRequest.getCallback( ).getClass( ).getSimpleName( ) ).debug( );
    this.messages.put( state, asyncRequest );
  }

  public void addCleanup( final Runnable task ) {
    this.cleanupTasks.add( task );
  }
  
  @SuppressWarnings( "unchecked" )
  private void queueEvents( final E state ) {
    for ( final Request event : this.messages.get( state ) ) {
      try {
        EventRecord.caller( StatefulMessageSet.class, EventType.VM_STARTING, state.name( ), event.getCallback( ).toString( ) ).debug( );
        if ( event.getCallback( ) instanceof BroadcastCallback ) {
          final BroadcastCallback callback = ( BroadcastCallback ) event.getCallback( );
          this.pendingEvents.addAll( Clusters.stream( ).map( new CompatFunction<Cluster, Request>( ) {
            @Override
            public Request apply( final Cluster c ) {
              LOG.debug( "VM_STARTING: " + state.name( ) + " " + c.getName( ) + " " + event.getClass( ).getSimpleName( ) + " " + event.getCallback( ) );
              final Request request = AsyncRequests.newRequest( callback.newInstance( ) );
              request.getRequest( ).regardingUserRequest( callback.getRequest( ) );
              request.dispatch( c.getConfiguration( ) );
              return request;
            }
          } ).toJavaList( ) );
        } else {
          LOG.debug( "VM_STARTING: " + state.name( ) + " " + this.cluster.getName( ) + " " + event.getClass( ).getSimpleName( ) + " " + event.getCallback( ) );
          event.dispatch( this.cluster.getConfiguration( ) );
          this.pendingEvents.add( event );
        }
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
      }
    }
  }
  
  private E transition( final E currentState ) {
    Request request = null;
    E nextState = this.states[currentState.ordinal( ) + 1];
    while ( ( request = this.pendingEvents.poll( ) ) != null ) {
      try {
        try {
          Object o = request.getResponse( ).get( 240, TimeUnit.SECONDS );
          if ( o != null ) {
            EventRecord.here( StatefulMessageSet.class, EventType.VM_STARTING, currentState.name( ), this.cluster.getName( ), o.getClass( ).getSimpleName( ) ).info( );
            EventRecord.here( StatefulMessageSet.class, EventType.VM_STARTING, currentState.name( ), this.cluster.getName( ), o.toString( ) ).debug( );
          }
        } catch ( TimeoutException ex1 ) {
          request.getCallback( ).fireException( ex1 );
        }
      } catch ( final InterruptedException t ) {
        Thread.currentThread( ).interrupt( );
        EventRecord.here(
          StatefulMessageSet.class,
          EventType.VM_STARTING,
          "FAILED",
          currentState.name( ),
          this.cluster.getName( ),
          t.getClass( ).getSimpleName( ) ).info( );
        LOG.error( t, t );
        nextState = this.rollback( );
        break;
      } catch ( final Exception t ) {
        EventRecord.here(
          StatefulMessageSet.class,
          EventType.VM_STARTING,
          "FAILED",
          currentState.name( ),
          this.cluster.getName( ),
          t.getClass( ).getSimpleName( ) ).info( );
        if ( Throwables.getRootCause( t ) instanceof EucalyptusClusterException ) {
          LOG.warn( t );
        } else {
          LOG.error( t, t );
        }
        nextState = this.rollback( );
        break;
      }
    }
    EventRecord.here( StatefulMessageSet.class, EventType.VM_STARTING, currentState.name( ), EventType.TRANSITION.name( ), nextState.name( ) ).info( );
    return nextState;
  }
  
  private boolean isSuccessful( ) {
    return this.state.equals( this.endState );
  }
  
  private boolean isFinished( ) {
    return this.state.equals( this.failState ) || this.state.equals( this.endState );
  }
  
  public void run( ) {
    try {
      do {
        LOG.info( EventRecord.here( StatefulMessageSet.class, EventType.VM_STARTING, this.state.name( ), ( System.currentTimeMillis( ) - this.startTime )
                                                                                                         / 1000.0d
                                                                                                         + "s" ) );
        try {
          this.queueEvents( this.state );
          this.state = this.transition( this.state );
        } catch ( final Exception ex ) {
          LOG.error( ex, ex );
        }
      } while ( !this.isFinished( ) );
    } finally {
      LOG.info( EventRecord.here( StatefulMessageSet.class, this.isSuccessful( )
          ? EventType.VM_START_COMPLETED
          : EventType.VM_START_ABORTED,
          ( System.currentTimeMillis( ) - this.startTime ) / 1000.0d + "s" ) );
      if ( !isSuccessful( ) ) {
        for ( final Runnable cleanupTask : cleanupTasks ) try {
          cleanupTask.run( );
        } catch ( final RuntimeException e ) {
          LOG.error( "Error in cleanup task: " + e.getMessage( ), e );
        }
      }
    }
  }
}
