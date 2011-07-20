package com.eucalyptus.util.fsm;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.Logs;
import com.eucalyptus.util.async.Callback;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.eucalyptus.util.fsm.Automata.State;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

public class AtomicMarkedState<P extends HasName<P>, S extends Automata.State, T extends Automata.Transition> implements StateMachine<P, S, T> {
  private static Logger                                      LOG                  = Logger.getLogger( AtomicMarkedState.class );
  private final P                                            parent;
  private final String                                       name;
  
  private final S                                            startState;
  private final ImmutableList<S>                             immutableStates;
  private final Multimap<S, Callback<P>>                     inStateListeners     = ArrayListMultimap.create( );
  private final Multimap<S, Callback<P>>                     outStateListeners    = ArrayListMultimap.create( );
  
  private volatile ImmutableList<TransitionHandler<P, S, T>> immutableTransitions = null;
  private final Multimap<T, TransitionHandler<P, S, T>>      transitions          = ArrayListMultimap.create( );
  private final Map<S, Map<S, TransitionHandler<P, S, T>>>   stateTransitions;
  
  private final AtomicMarkableReference<S>                   state;
  private final AtomicLong                                   id                   = new AtomicLong( 0l );
  private final AtomicReference<ActiveTransition>            currentTransition    = new AtomicReference<ActiveTransition>( null );
  
  public AtomicMarkedState( S startState, P parent, Set<TransitionHandler<P, S, T>> transitions, //
                            Multimap<S, Callback<P>> inStateListeners, Multimap<S, Callback<P>> outStateListeners ) {
    this.startState = startState;
    this.name = String.format( "State-%s-%s", parent.getClass( ).getSimpleName( ), parent.getName( ) );
    this.parent = parent;
    final S[] states = State.asEnum.getEnumConstants( startState );
    this.stateTransitions = new HashMap<S, Map<S, TransitionHandler<P, S, T>>>( ) {
      {
        for ( S s : states ) {
          this.put( s, new HashMap<S, TransitionHandler<P, S, T>>( ) );
        }
      }
    };
    this.immutableStates = ImmutableList.of( states );
    this.state = new AtomicMarkableReference<S>( this.startState, false );
    this.immutableTransitions = ImmutableList.copyOf( transitions );
    for ( TransitionHandler<P, S, T> t : transitions ) {
      this.transitions.put( t.getName( ), t );
      this.stateTransitions.get( t.getRule( ).getFromState( ) ).put( t.getRule( ).getToState( ), t );
    }
    this.inStateListeners.putAll( inStateListeners );
    this.outStateListeners.putAll( outStateListeners );
  }
  
  @Override
  public boolean isLegalTransition( T transitionName ) {
    try {
      this.lookupTransition( transitionName );
      return true;
    } catch ( NoSuchElementException ex ) {
      return false;
    }
  }
  
  @Override
  public CheckedListenableFuture<P> transitionByName( T transitionName ) throws IllegalStateException, ExistingTransitionException {
    if ( this.state.isMarked( ) ) {
      throw new ExistingTransitionException( "Transition request transition=" + transitionName + " rejected because of an ongoing transition: "
                                             + this.toString( ) );
    } else if ( !this.transitions.containsKey( transitionName ) ) {
      throw new NoSuchElementException( "No such transition named: " + transitionName.toString( ) + ". Known transitions: " + this.getTransitions( ) );
    } else {
      this.checkTransition( transitionName );
      final ActiveTransition tid = this.beforeLeave( transitionName );
      CheckedListenableFuture<P> future = this.afterLeave( transitionName, tid );
      return future;
    }
  }
  
  @Override
  public CheckedListenableFuture<P> transition( S nextState ) throws IllegalStateException, ExistingTransitionException {
    if ( this.state.isMarked( ) ) {
      throw new ExistingTransitionException( "Transition request state=" + nextState + " rejected because of an ongoing transition: "
                                             + this.toString( ) );
    } else if ( !this.stateTransitions.get( this.state.getReference( ) ).containsKey( nextState ) ) {
      throw new NoSuchElementException( "No transition to " + nextState.toString( ) + " from current state " + this.toString( ) + ". Known transitions: "
                                        + this.getTransitions( ) );
    } else {
      T transitionName = this.stateTransitions.get( this.state.getReference( ) ).get( nextState ).getName( );
      this.checkTransition( transitionName );
      final ActiveTransition tid = this.beforeLeave( transitionName );
      CheckedListenableFuture<P> future = this.afterLeave( transitionName, tid );
      return future;
    }
  }
  
  /**
   * @see com.eucalyptus.util.fsm.State#request(com.eucalyptus.util.fsm.TransitionRule)
   * @param rule
   * @return
   * @throws ExistingTransitionException
   */
  protected ActiveTransition request( T transitionName ) throws ExistingTransitionException {
    TransitionHandler<P, S, T> transition = lookupTransition( transitionName );
    TransitionRule<S, T> rule = transition.getRule( );
    if ( !this.state.compareAndSet( rule.getFromState( ), rule.getFromState( ), rule.getFromStateMark( ), true ) ) {
      throw new ExistingTransitionException( "Transition request " + transitionName + " rejected because of an ongoing transition: "
                                             + this.toString( ) );
    } else {
      this.currentTransition.set( new ActiveTransition( this.id.incrementAndGet( ), rule, transition ) );
      return this.currentTransition.get( );
    }
  }
  
  private TransitionHandler<P, S, T> lookupTransition( T transitionName ) {
    if ( !this.transitions.containsKey( transitionName ) ) {
      throw new NoSuchElementException( "No such transition: " + transitionName );
    }
    S fromState = null;
    boolean[] mark = new boolean[1];
    for ( TransitionHandler<P, S, T> transition : this.transitions.get( transitionName ) ) {
      if ( transition.getRule( ).getFromState( ).equals( fromState = this.state.get( mark ) ) && transition.getRule( ).getFromStateMark( ) == mark[0] ) {
        return transition;
      }
    }
    throw new NoSuchElementException( "No such transition: " + transitionName + " for the current state " + fromState + " and mark " + mark[0] + " for parent "
                                      + this.parent.toString( ) );
  }
  
  /**
   * @see com.eucalyptus.util.fsm.State#commit()
   */
  private void commit( ) {
    LOG.debug( "Transition commit(): " + this.currentTransition.get( ) );
    if ( !this.state.isMarked( ) ) {
      IllegalStateException ex = Exceptions.trace( new IllegalStateException( "commit() called when there is no currently pending transition: "
                                                                              + this.toString( ) ) );
      LOG.error( ex, ex );
      throw ex;
    } else {
      ActiveTransition tr = this.currentTransition.getAndSet( null );
      this.state.set( tr.getTransitionRule( ).getToState( ), tr.getTransitionRule( ).getToStateMark( ) );
      if ( !tr.getTransitionRule( ).getFromState( ).equals( tr.getTransitionRule( ).getToState( ) ) ) {
        this.state.set( tr.getTransitionRule( ).getToState( ), false );
        this.fireInListeners( tr.getTransitionRule( ).getToState( ) );
      } else {
        this.state.set( tr.getTransitionRule( ).getToState( ), false );
      }
      EventRecord.caller( this.getClass( ), EventType.TRANSITION_FUTURE,
                          "set(" + this.parent.toString( ) + ":" + this.parent.getClass( ).getCanonicalName( ) + ")" ).trace( );
      tr.getTransitionFuture( ).set( this.parent );
    }
  }
  
  private void error( Throwable t ) {
    LOG.debug( "Transition error(): " + this.toString( ) );
    if ( !this.state.isMarked( ) ) {
      IllegalStateException ex = Exceptions.debug( new IllegalStateException( "error() called when there is no currently pending transition: "
                                                                              + this.toString( ), t ) );
      Logs.exhaust( ).error( ex, ex );
      throw ex;
    } else {
      ActiveTransition tr = this.currentTransition.getAndSet( null );
      this.state.set( tr.getTransitionRule( ).getErrorState( ), tr.getTransitionRule( ).getErrorStateMark( ) );
      if ( !tr.getTransitionRule( ).getFromState( ).equals( tr.getTransitionRule( ).getErrorState( ) ) ) {
        this.state.set( tr.getTransitionRule( ).getErrorState( ), false );
        this.fireInListeners( tr.getTransitionRule( ).getErrorState( ) );
      } else {
        this.state.set( tr.getTransitionRule( ).getErrorState( ), false );
      }
      EventRecord.caller( this.getClass( ), EventType.TRANSITION_FUTURE, "setException(" + t.getClass( ).getCanonicalName( ) + "): " + t.getMessage( ) ).trace( );
      tr.getTransitionFuture( ).setException( t );
    }
  }
  
  private void rollback( Throwable t ) {
    LOG.debug( "Transition rollback(): " + this.toString( ) );
    if ( !this.state.isMarked( ) ) {
      Exceptions.debug( new IllegalStateException( "rollback() called when there is no currently pending transition: " + this.toString( ) ) );
    } else {
      ActiveTransition tr = this.currentTransition.getAndSet( null );
      this.state.set( tr.getTransitionRule( ).getFromState( ), false );
    }
  }
  
  protected void fireInListeners( S state ) {
    for ( Callback<P> cb : AtomicMarkedState.this.inStateListeners.get( state ) ) {
      try {
        Logs.exhaust( ).debug( "Firing state-in listener: " + cb.getClass( ) + " for " + this.toString( ) );
        cb.fire( this.parent );
      } catch ( Throwable t ) {
        Exceptions.debug( "Firing state-in listeners failed for :" + cb.getClass( ).getCanonicalName( ), Exceptions.filterStackTrace( t ) );
      }
    }
  }
  
  protected void fireOutListeners( S state ) {
    for ( Callback<P> cb : AtomicMarkedState.this.outStateListeners.get( state ) ) {
      try {
        Logs.exhaust( ).debug( "Firing state-in listener: " + cb.getClass( ) + " for " + this.toString( ) );
        cb.fire( this.parent );
      } catch ( Throwable t ) {
        Exceptions.debug( "Firing state-out listeners failed for :" + cb.getClass( ).getCanonicalName( ), Exceptions.filterStackTrace( t ) );
      }
    }
  }
  
  private final void checkTransition( final T transitionName ) {
    try {
      if ( !this.lookupTransition( transitionName ).before( this.parent ) ) {
        throw Exceptions.trace( new IllegalStateException( String.format( "Failed to apply transition %s because before() returned false.",
                                                                          transitionName.toString( ) ) ) );
      }
    } catch ( Throwable t ) {
      throw Exceptions.trace( new IllegalStateException( String.format( "Failed to apply transition %s because before() threw an exception: %s",
                                                                        transitionName.toString( ), t.getMessage( ) ), t ) );
    }
  }
  
  private final CheckedListenableFuture<P> afterLeave( final T transitionName, final ActiveTransition tid ) throws IllegalStateException {
    try {
      CheckedListenableFuture<P> result = tid.leave( );
      this.fireOutListeners( tid.getTransitionRule( ).getFromState( ) );
      return result;
    } catch ( Throwable t ) {
      this.error( t );
      throw Exceptions.debug( new IllegalStateException( String.format( "Failed to apply transition %s because leave() threw an exception: %s",
                                                                        transitionName.toString( ), t.getMessage( ) ), t ) );
    }
  }
  
  private final ActiveTransition beforeLeave( final T transitionName ) throws IllegalStateException, ExistingTransitionException {
    final ActiveTransition tid;
    try {
      tid = this.request( transitionName );
    } catch ( ExistingTransitionException t ) {
      throw t;
    } catch ( Throwable t ) {
      this.rollback( t );
      throw Exceptions.debug( new IllegalStateException( String.format( "Failed to apply transition %s because request() threw an exception.",
                                                                        transitionName.toString( ) ), t ) );
    }
    return tid;
  }
  
  /**
   * TODO: DOCUMENT
   * 
   * @see com.eucalyptus.util.fsm.StateMachine#getState()
   * @return
   */
  @Override
  public S getState( ) {
    return this.state.getReference( );
  }
  
  /**
   * TODO: DOCUMENT
   * 
   * @see com.eucalyptus.util.fsm.StateMachine#isBusy()
   * @return
   */
  @Override
  public boolean isBusy( ) {
    return this.state.isMarked( );
  }
  
  /**
   * TODO: DOCUMENT
   * 
   * @see com.eucalyptus.util.fsm.StateMachine#getStates()
   * @return
   */
  @Override
  public ImmutableList<S> getStates( ) {
    return this.immutableStates;
  }
  
  /**
   * TODO: DOCUMENT
   * 
   * @see com.eucalyptus.util.fsm.StateMachine#getTransitions()
   * @return
   */
  @Override
  public ImmutableList<TransitionHandler<P, S, T>> getTransitions( ) {
    return immutableTransitions;
  }
  
  /**
   * @see java.lang.Object#toString()
   * @return
   */
  public String toString( ) {
    ActiveTransition t = this.currentTransition.get( );
    return String.format( "State:name=%s:state=%s:mark=%s:transition=%s", this.name, this.state.getReference( ), this.state.isMarked( ), ( t != null
        ? t.toString( )
        : "idle" ) );
  }
  
  /**
   * @return the name
   */
  public String getName( ) {
    return this.name;
  }
  
  public class ActiveTransition extends Callback.Completion implements HasName<ActiveTransition> {
    private final Long                       id;
    private final String                     name;
    private final Long                       startTime;
    private Long                             endTime          = 0l;
    private final TransitionAction<P>        transition;
    private final Throwable                  startStackTrace;
    private Throwable                        endStackTrace;
    private final CheckedListenableFuture<P> transitionFuture = Futures.newGenericeFuture( );
    private TransitionRule<S, T>             rule;
    
    public ActiveTransition( Long id, TransitionRule<S, T> rule, TransitionAction<P> transition ) {
      this.id = id;
      this.startTime = System.nanoTime( );
      this.endTime = 0l;
      this.rule = rule;
      this.transition = transition;
      this.name = AtomicMarkedState.this.getName( ) + "-" + this.rule.getName( ) + "-" + id;
      if ( Logs.EXTREME ) {
        this.startStackTrace = Exceptions.filterStackTrace( new RuntimeException( ), 20 );
      } else {
        this.startStackTrace = null;
      }
    }
    
    CheckedListenableFuture<P> getTransitionFuture( ) {
      return this.transitionFuture;
    }
    
    public void fire( ) {
      try {
        this.transition.enter( AtomicMarkedState.this.parent );
        this.transition.after( AtomicMarkedState.this.parent );
        AtomicMarkedState.this.commit( );
      } catch ( Throwable t ) {
        this.fireException( t );
      }
    }
    
    public void fireException( Throwable t ) {
      if( Logs.EXTREME ) {
        Logs.exhaust( ).trace( Exceptions.string( this.startStackTrace ) );
        Logs.exhaust( ).trace( Exceptions.string( this.endStackTrace ) );
      }
      AtomicMarkedState.this.error( t );
    }
    
    public final Long getId( ) {
      return this.id;
    }
    
    public TransitionRule<S, T> getTransitionRule( ) {
      return this.rule;
    }
    
    public CheckedListenableFuture<P> leave( ) {
      this.transition.leave( AtomicMarkedState.this.parent, this );
      return this.transitionFuture;
    }
    
    public String getName( ) {
      return this.name;
    }
    
    public int compareTo( ActiveTransition that ) {
      return this.id.compareTo( that.id );
    }
    
    public String toString( ) {
      StringBuilder sb = new StringBuilder( );
      sb.append( EventType.TRANSITION ).append( " " ).append( this.name ).append( " Active" ).append( this.transition != null
        ? this.transition.toString( )
        : "null" ).append( " id=" ).append( this.id ).append( " startTime=" ).append( new Date( this.startTime ) );
      Logs.exhaust( ).trace( sb.toString( ) );
      return sb.toString( );
    }
  }
  
  public P getParent( ) {
    return this.parent;
  }
}
