package com.eucalyptus.cluster;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.callback.BroadcastCallback;
import com.eucalyptus.cluster.callback.QueuedEventCallback;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import edu.ucsb.eucalyptus.constants.EventType;
import edu.ucsb.eucalyptus.msgs.EventRecord;

public class StatefulMessageSet<E extends Enum<E>> {
  private static Logger                              LOG              = Logger.getLogger( StatefulMessageSet.class );
  private Multimap<E, QueuedEventCallback>           messages         = Multimaps.newHashMultimap( );
  private AtomicBoolean                              rollback         = new AtomicBoolean( false );
  private List<QueuedEventCallback>                  rollbackMessages = Lists.newArrayList( );
  private ConcurrentLinkedQueue<QueuedEventCallback> pendingEvents    = new ConcurrentLinkedQueue<QueuedEventCallback>( );
  private E[]                                        states;
  private E                                          state;
  private E                                          endState;
  private E                                          failState;
  private Cluster                                    cluster;
  private Long                                       startTime;
  
  /**
   * Collection of messages which need to honor a certain ordering. The state array is a list of enum values where:
   * <ul>
   * <li>Index 0: is the start state</li>
   * <li>Index length-2: is the end state</li>
   * <li>Index length-1: is the rollback state</li>
   * </ul>
   * A transition will increase the currentState's ordinal by 1, drain the messages to a pending queue and wait for all messages to be serviced before
   * proceeding.
   * 
   * @param cluster
   * @param states
   */
  public StatefulMessageSet( Cluster cluster, E[] states ) {
    this.cluster = cluster;
    this.states = states;
    this.state = states[0];
    this.endState = states[states.length - 2];
    this.failState = states[states.length - 1];
    this.startTime = System.currentTimeMillis( );
  }
  
  public void rollback( ) {
    this.rollback.lazySet( true );
  }
  
  public void addRequest( E state, QueuedEventCallback callback ) {
    LOG.debug( EventRecord.caller( StatefulMessageSet.class, EventType.VM_PREPARE, state.name( ), callback.getClass( ).getSimpleName( ) ) );
    this.messages.put( state, callback );
  }
  
  public void addRollbackRequest( QueuedEventCallback callback ) {
    LOG.debug( EventRecord.caller( StatefulMessageSet.class, EventType.VM_PREPARE, this.failState.name( ), callback.getClass( ).getSimpleName( ) ) );
    this.rollbackMessages.add( callback );
  }
  
  @SuppressWarnings( "unchecked" )
  private void queueEvents( E state ) {
    for ( QueuedEventCallback event : this.messages.get( state ) ) {
      if ( event instanceof BroadcastCallback ) {
        BroadcastCallback callback = ( BroadcastCallback ) event;
        for ( Cluster c : Clusters.getInstance( ).listValues( ) ) {
          QueuedEventCallback subEvent = callback.newInstance( ).regardingUserRequest( callback.getRequest( ) );
          this.pendingEvents.add( subEvent );
          LOG.info( EventRecord.caller( StatefulMessageSet.class, EventType.VM_STARTING, this.state.name( ), c.getName( ), event.getClass( ).getSimpleName( ) ) );
          subEvent.dispatch( c );
        }
      } else {
        LOG.info( EventRecord.caller( StatefulMessageSet.class, EventType.VM_STARTING, this.state.name( ), this.cluster.getName( ), event.getClass( ).getSimpleName( ) ) );
        this.pendingEvents.add( event );
        event.dispatch( cluster );
      }
    }
  }
  
  public void transition( ) {
    QueuedEventCallback event = null;
    while ( ( event = this.pendingEvents.poll( ) ) != null ) {
      Object o = null;
      try {
        o = event.getResponse( );
        LOG.info( EventRecord.here( StatefulMessageSet.class, EventType.VM_STARTING, this.state.name( ), this.cluster.getName( ), o.getClass( ).getSimpleName( ) ) );
      } catch ( Throwable t ) {
        LOG.info( EventRecord.here( StatefulMessageSet.class, EventType.VM_STARTING, this.state.name( ), this.cluster.getName( ), t.getClass( ).getSimpleName( ) ) );
        LOG.debug( t, t );
        this.rollback.lazySet( true );
        this.state = failState;
        return;
      }
    }
    LOG.info( EventRecord.here( StatefulMessageSet.class, EventType.VM_STARTING, this.state.name( ), EventType.TRANSITION.name( ), this.states[this.state.ordinal( ) + 1].name( ) ) );
    this.state = this.states[this.state.ordinal( ) + 1];
  }
  
  public void run( ) {
    do {
      if ( this.state.equals( failState ) ) {
        this.pendingEvents.addAll( this.rollbackMessages );
      } else {
        this.queueEvents( this.state );
        this.transition( );
      }
    } while ( !this.state.equals( endState ) || !this.state.equals( failState ) );
    LOG.info( EventRecord.here( StatefulMessageSet.class, EventType.VM_STARTED, ( System.currentTimeMillis( ) - this.startTime ) / 1000.0d + "s" ) );
  }
}
