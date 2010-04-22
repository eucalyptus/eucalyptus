package com.eucalyptus.cluster;

import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.callback.BroadcastCallback;
import com.eucalyptus.cluster.callback.QueuedEventCallback;
import com.eucalyptus.records.EventType;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import edu.ucsb.eucalyptus.msgs.EventRecord;

public class StatefulMessageSet<E extends Enum<E>> {
  private static Logger                              LOG           = Logger.getLogger( StatefulMessageSet.class );
  private Multimap<E, QueuedEventCallback>           messages      = Multimaps.newHashMultimap( );
  private ConcurrentLinkedQueue<QueuedEventCallback> pendingEvents = new ConcurrentLinkedQueue<QueuedEventCallback>( );
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
  
  private E rollback( ) {
    return ( this.state = failState );
  }
  
  public void addRequest( E state, QueuedEventCallback callback ) {
    LOG.debug( EventRecord.caller( StatefulMessageSet.class, EventType.VM_PREPARE, state.name( ), callback.getClass( ).getSimpleName( ) ) );
    this.messages.put( state, callback );
  }
  
  @SuppressWarnings( "unchecked" )
  private void queueEvents( final E state ) {
    for ( final QueuedEventCallback event : this.messages.get( state ) ) {
      if ( event instanceof BroadcastCallback ) {
        final BroadcastCallback callback = ( BroadcastCallback ) event;
        this.pendingEvents.addAll( Lists.transform( Clusters.getInstance( ).listValues( ), new Function<Cluster, QueuedEventCallback>( ) {
          public QueuedEventCallback apply( Cluster c ) {
            LOG.info( EventRecord.caller( StatefulMessageSet.class, EventType.VM_STARTING, state.name( ), c.getName( ), event.getClass( ).getSimpleName( ) ) );
            return callback.newInstance( ).regardingUserRequest( callback.getRequest( ) ).dispatch( c );
          }
        } ) );
      } else {
        this.pendingEvents.add( event.dispatch( cluster ) );
        LOG.info( EventRecord.caller( StatefulMessageSet.class, EventType.VM_STARTING, state.name( ), cluster.getName( ), event.getClass( ).getSimpleName( ) ) );
      }
    }
  }
  
  private E transition( final E currentState ) {
    QueuedEventCallback event = null;
    E nextState = this.states[currentState.ordinal( ) + 1];
    while ( ( event = this.pendingEvents.poll( ) ) != null ) {
      while ( !event.pollForResponse( 1000l ) );
      try {
        Object o = event.pollResponse( 1000l );
        LOG.info( EventRecord.here( StatefulMessageSet.class, EventType.VM_STARTING, currentState.name( ), cluster.getName( ), o.getClass( ).getSimpleName( ) ) );
      } catch ( Throwable t ) {
        LOG.info( EventRecord.here( StatefulMessageSet.class, EventType.VM_STARTING, currentState.name( ), cluster.getName( ), t.getClass( ).getSimpleName( ) ) );
        LOG.debug( t, t );
        nextState = this.rollback( );
      }
    }
    LOG.info( EventRecord.here( StatefulMessageSet.class, EventType.VM_STARTING, currentState.name( ), EventType.TRANSITION.name( ), nextState.name( ) ) );
    return nextState;
  }
  
  private boolean isSuccessful( ) {
    return this.state.equals( endState );
  }
  
  private boolean isFinished( ) {
    return this.state.equals( this.failState ) || this.state.equals( endState );
  }
  
  public void run( ) {
    do {
      this.queueEvents( this.state );
      this.state = this.transition( this.state );
    } while ( !this.isFinished( ) );
    LOG.info( EventRecord.here( StatefulMessageSet.class, 
                                this.isSuccessful( )?EventType.VM_START_COMPLETED:EventType.VM_START_ABORTED, 
                                                    ( System.currentTimeMillis( ) - this.startTime ) / 1000.0d + "s" ) );
  }
}
