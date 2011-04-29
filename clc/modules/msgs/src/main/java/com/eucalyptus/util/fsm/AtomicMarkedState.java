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
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.Logs;
import com.eucalyptus.util.async.Callback;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.fsm.Automata.State;
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
                                             + this.currentTransition.get( ) );
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
                                             + this.currentTransition.get( ) );
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
    if ( !this.currentTransition.compareAndSet( null, new ActiveTransition( this.id.incrementAndGet( ), rule, transition ) ) ) {
      throw new ExistingTransitionException( "Transition request " + transitionName + " rejected because of an ongoing transition: "
                                             + this.currentTransition.get( ) );
    } else if ( !this.state.compareAndSet( rule.getFromState( ), rule.getToState( ), rule.getFromStateMark( ), true ) ) {
      this.id.decrementAndGet( );
      this.currentTransition.set( null );
      throw new IllegalStateException( "Failed to validate expected preconditions for transition: " + transition.getRule( ).toString( )
                                       + " for current state: " + this.toString( ) );
    } else {
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
    if ( this.currentTransition.get( ) == null ) {
      Exceptions.trace( new IllegalStateException( "commit() called when there is no currently pending transition: " + this.toString( ) ) );
    } else {
      ActiveTransition tr = this.currentTransition.get( );
      boolean doFireInListeners = !this.state.getReference( ).equals( tr.getTransitionRule( ).getFromState( ) );
      if ( !this.state.compareAndSet( tr.getTransitionRule( ).getToState( ), tr.getTransitionRule( ).getToState( ), true,
                                      tr.getTransitionRule( ).getToStateMark( ) ) ) {
        this.state.set( this.state.getReference( ), false );
        Exceptions.trace( new IllegalStateException( "Failed to apply toState for the transition: " + tr.toString( ) + " for current state: "
                                                              + this.toString( ) ) );
      }
      this.currentTransition.set( null );
      if ( doFireInListeners ) {
        this.fireInListeners( tr.getTransitionRule( ).getToState( ) );
      }
    }
  }
  
  private void error( ) {
    LOG.debug( "Transition error(): " + this.currentTransition.get( ) );
    if ( this.currentTransition.get( ) == null ) {
      Exceptions.trace( new IllegalStateException( "error() called when there is no currently pending transition: " + this.toString( ) ) );
    } else {
      ActiveTransition tr = this.currentTransition.get( );
      if ( !this.state.compareAndSet( tr.getTransitionRule( ).getToState( ), tr.getTransitionRule( ).getErrorState( ), true,
                                      tr.getTransitionRule( ).getErrorStateMark( ) ) ) {
        this.state.set( this.state.getReference( ), false );
        Exceptions.trace( new IllegalStateException( "Failed to apply toState for the transition: " + tr.toString( ) + " for current state: "
                                                              + this.toString( ) ) );
      }
      if ( !this.state.getReference( ).equals( tr.getTransitionRule( ).getErrorState( ) ) ) {
        this.currentTransition.set( null );
        this.state.set( tr.getTransitionRule( ).getErrorState( ), false );
        this.fireInListeners( tr.getTransitionRule( ).getErrorState( ) );
      } else {
        this.currentTransition.set( null );
        this.state.set( tr.getTransitionRule( ).getErrorState( ), false );
      }
    }
  }
  
  private void rollback( ) {
    LOG.debug( "Transition debug(): " + this.currentTransition.get( ) );
    if ( this.currentTransition.get( ) == null ) {
      if ( this.state.isMarked( ) ) {
        this.state.set( this.state.getReference( ), false );
      }
      Exceptions.trace( new IllegalStateException( "rollback() called when there is no currently pending transition: " + this.toString( ) ) );
    } else {
      ActiveTransition tr = this.currentTransition.get( );
      if ( !this.state.compareAndSet( tr.getTransitionRule( ).getToState( ), tr.getTransitionRule( ).getFromState( ), true,
                                      tr.getTransitionRule( ).getFromStateMark( ) ) ) {
        Exceptions.trace( new IllegalStateException( "Failed to apply toState for the transition: " + tr.toString( ) + " for current state: "
                                                              + this.toString( ) ) );
      }
      if ( !this.state.getReference( ).equals( tr.getTransitionRule( ).getFromState( ) ) ) {
        this.state.set( tr.getTransitionRule( ).getFromState( ), false );
        this.currentTransition.set( null );
        this.fireInListeners( tr.getTransitionRule( ).getFromState( ) );
      } else {
        this.state.set( tr.getTransitionRule( ).getFromState( ), false );
        this.currentTransition.set( null );
      }
    }
  }
  
  protected void fireInListeners( S state ) {
    for ( Callback<P> cb : AtomicMarkedState.this.inStateListeners.get( state ) ) {
      try {
        cb.fire( this.parent );
      } catch ( Throwable t ) {
        Exceptions.trace( "Firing state-in listeners failed for :" + cb.getClass( ).getCanonicalName( ), Exceptions.filterStackTrace( t ) );
      }
    }
  }
  
  protected void fireOutListeners( S state ) {
    for ( Callback<P> cb : AtomicMarkedState.this.outStateListeners.get( state ) ) {
      try {
        cb.fire( this.parent );
      } catch ( Throwable t ) {
        Exceptions.trace( "Firing state-out listeners failed for :" + cb.getClass( ).getCanonicalName( ), Exceptions.filterStackTrace( t ) );
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
      this.fireOutListeners( tid.getTransitionRule( ).getFromState( ) );
      return tid.leave( );
    } catch ( Throwable t ) {
      this.rollback( );
      throw Exceptions.trace( new IllegalStateException( String.format( "Failed to apply transition %s because leave() threw an exception: %s",
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
      this.rollback( );
      throw Exceptions.trace( new IllegalStateException( String.format( "Failed to apply transition %s because request() threw an exception.",
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
    return String.format( "State:name=%s:state=%s:mark=%s:transition=%s", this.name, this.state.getReference( ), this.state.isMarked( ), ( Logs.EXTREME
      ? ( t != null
        ? t.toString( )
        : "idle" )
      : "" ) );
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
    private final Throwable                  endStackTrace    = new RuntimeException( );
    private final CheckedListenableFuture<P> transitionFuture = new TransitionFuture<P>( );
    private TransitionRule<S, T>             rule;
    
    public ActiveTransition( Long id, TransitionRule<S, T> rule, TransitionAction<P> transition ) {
      this.id = id;
      this.startTime = System.nanoTime( );
      this.endTime = 0l;
      this.rule = rule;
      this.transition = transition;
      this.name = AtomicMarkedState.this.getName( ) + "-" + this.rule.getName( ) + "-" + id;
      if ( Logs.DEBUG ) {
        this.startStackTrace = Exceptions.filterStackTrace( new RuntimeException( ) );
      } else {
        this.startStackTrace = null;
      }
    }
    
    public void fire( ) {
      try {
        this.transition.enter( AtomicMarkedState.this.parent );
        try {
          this.teardown( );
          AtomicMarkedState.this.commit( );
        } catch ( IllegalStateException t ) {
          LOG.trace( t, t );
        }
      } catch ( Throwable t ) {
        LOG.error( t, t );
        this.teardown( );
        AtomicMarkedState.this.rollback( );
        this.transitionFuture.setException( t );
      }
      try {
        this.transition.after( AtomicMarkedState.this.parent );
        this.transitionFuture.set( AtomicMarkedState.this.parent );
      } catch ( Throwable t ) {
        this.transitionFuture.setException( t );
        LOG.error( t, t );
      }
    }
    
    private void teardown( ) {
      if ( Logs.TRACE ) {
        RuntimeException ex = new RuntimeException( );
        if ( this.endTime != 0l ) {
          LOG.error( "Transition being committed for a second time!" );
          LOG.error( "FIRST: " + Exceptions.filterStackTraceElements( this.endStackTrace ), this.endStackTrace );
          LOG.error( "SECOND: " + Exceptions.filterStackTraceElements( ex ), ex );
        } else {
          this.endTime = System.nanoTime( );
          this.endStackTrace.setStackTrace( Exceptions.filterStackTraceElements( new RuntimeException( ) ).toArray( new StackTraceElement[] {} ) );
          LOG.trace( this );
        }
      } else if ( Logs.EXTREME ) {
        LOG.error( this.toString( ) );
      }
    }
    
    public void fireException( Throwable t ) {
      this.teardown( );
      AtomicMarkedState.this.error( );
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
      sb.append( EventType.TRANSITION ).append( this.name ).append( " Active" ).append( this.transition != null
        ? this.transition.toString( )
        : "null" ).append( " id=" ).append( this.id ).append( " startTime=" ).append( new Date( this.startTime ) );
      Logs.exhaust( ).info( sb.toString( ) );
      Logs.exhaust( ).info( Exceptions.string( this.startStackTrace ) );
      return sb.toString( );
    }
  }
}
