package com.eucalyptus.util.fsm;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.async.Callback;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class AtomicMarkedState<P extends HasName<P>, S extends Enum<S>, T extends Enum<T>> {
  private static Logger                               LOG                  = Logger.getLogger( AtomicMarkedState.class );
  private final P                                     parent;
  private final String                                name;
  
  private final S                                     startState;
  private final ImmutableList<S>                      immutableStates;
  private final Multimap<S, Callback<S>>              inStateListeners     = Multimaps.newArrayListMultimap( );
  private final Multimap<S, Callback<S>>              outStateListeners    = Multimaps.newArrayListMultimap( );
  
  private volatile ImmutableList<Transition<P, S, T>> immutableTransitions = null;
  private final Multimap<T, Transition<P, S, T>>      transitions          = Multimaps.newArrayListMultimap( );
  
  private final AtomicMarkableReference<S>            state;
  private final AtomicLong                            id                   = new AtomicLong( 0l );
  private final AtomicReference<ActiveTransition>     currentTransition    = new AtomicReference<ActiveTransition>( null );
  
  public AtomicMarkedState( S startState, P parent, Set<Transition<P, S, T>> transitions, //
                            Multimap<S, Callback<S>> inStateListeners, Multimap<S, Callback<S>> outStateListeners ) {
    this.startState = startState;
    this.name = String.format( "State-%s-%s", parent.getClass( ).getSimpleName( ), parent.getName( ) );
    this.parent = parent;
    S[] states = this.startState.getDeclaringClass( ).getEnumConstants( );
    this.immutableStates = ImmutableList.of( states );
    this.state = new AtomicMarkableReference<S>( this.startState, false );
    this.immutableTransitions = ImmutableList.copyOf( transitions );
    for ( Transition<P, S, T> t : transitions ) {
      this.transitions.put( t.getName( ), t );
    }
    this.inStateListeners.putAll( inStateListeners );
    this.outStateListeners.putAll( outStateListeners );
  }
  
  public ActiveTransition startTransition( T transitionName ) throws IllegalStateException {
    if ( !this.transitions.containsKey( transitionName ) ) {
      throw new NoSuchElementException( transitionName.toString( ) + " known transitions: " + this.getTransitions( ) );
    } else {
      this.checkTransition( transitionName );
      final ActiveTransition tid = this.beforeLeave( transitionName );
      this.afterLeave( transitionName, tid );
      return tid;
    }
  }
  
  public void transition( T transition ) throws IllegalStateException {
    this.startTransition( transition ).fire( );
  }
  
  /**
   * @see com.eucalyptus.util.fsm.State#request(com.eucalyptus.util.fsm.TransitionRule)
   * @param rule
   * @return
   */
  protected ActiveTransition request( T transitionName ) {
    Transition<P, S, T> transition = lookupTransition( transitionName );
    TransitionRule<S, T> r = transition.getRule( );
    if ( !this.currentTransition.compareAndSet( null, new ActiveTransition( this.id.incrementAndGet( ), transition ) ) ) {
      throw new IllegalStateException( "There is a currently pending transition: " + this.toString( ) );
    } else if ( !this.state.compareAndSet( r.getFromState( ), r.getToState( ), r.getFromStateMark( ), true ) ) {
      this.id.decrementAndGet( );
      this.currentTransition.set( null );
      throw new IllegalStateException( "Failed to validate expected preconditions for transition: " + transition.getRule( ).toString( )
                                       + " for current state: " + this.toString( ) );
    } else {
      return this.currentTransition.get( );
    }
  }
  
  private Transition<P, S, T> lookupTransition( T transitionName ) {
    if ( !this.transitions.containsKey( transitionName ) ) {
      throw new NoSuchElementException( "No such transition: " + transitionName );
    }
    S fromState = null;
    boolean[] mark = new boolean[1];
    for ( Transition<P, S, T> transition : this.transitions.get( transitionName ) ) {
      if ( transition.getFromState( ).equals( fromState = this.state.get( mark ) ) && transition.getFromStateMark( ) == mark[0] ) {
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
    if ( this.currentTransition.get( ) == null ) {
      Exceptions.trace( new IllegalStateException( "There is no currently pending transition: " + this.toString( ) ) );
    } else {
      ActiveTransition tr = this.currentTransition.get( );
      if ( !this.state.compareAndSet( tr.getToState( ), tr.getToState( ), true, tr.getToStateMark( ) ) ) {
        this.state.set( this.state.getReference( ), false );
        Exceptions.trace( new IllegalStateException( "Failed to apply toState for the transition: " + tr.toString( ) + " for current state: "
                                                              + this.toString( ) ) );
      }
      this.currentTransition.set( null );
      this.fireInListeners( tr.getToState( ) );
    }
  }
  
  private void error( ) {
    if ( this.currentTransition.get( ) == null ) {
      Exceptions.trace( new IllegalStateException( "There is no currently pending transition: " + this.toString( ) ) );
    } else {
      ActiveTransition tr = this.currentTransition.get( );
      if ( !this.state.compareAndSet( tr.getToState( ), tr.getErrorState( ), true, tr.getErrorStateMark( ) ) ) {
        this.state.set( this.state.getReference( ), false );
        Exceptions.trace( new IllegalStateException( "Failed to apply toState for the transition: " + tr.toString( ) + " for current state: "
                                                              + this.toString( ) ) );
      }
      this.currentTransition.set( null );
      this.fireInListeners( tr.getToState( ) );
    }
  }
  
  private void rollback( ) {
    if ( this.currentTransition.get( ) == null ) {
      Exceptions.trace( new IllegalStateException( "There is no currently pending transition: " + this.toString( ) ) );
    } else {
      ActiveTransition tr = this.currentTransition.get( );
      if ( !this.state.compareAndSet( tr.getToState( ), tr.getFromState( ), true, tr.getFromStateMark( ) ) ) {
        Exceptions.trace( new IllegalStateException( "Failed to apply toState for the transition: " + tr.toString( ) + " for current state: "
                                                              + this.toString( ) ) );
      }
      this.state.set( tr.getErrorState( ), false );
      this.currentTransition.set( null );
    }
  }
  
  protected void fireInListeners( S state ) {
    for ( Callback<S> cb : AtomicMarkedState.this.inStateListeners.get( state ) ) {
      try {
        cb.fire( state );
      } catch ( Throwable t ) {
        Exceptions.trace( "Firing state-in listeners failed for :" + cb.getClass( ).getCanonicalName( ), t );
      }
    }
  }
  
  protected void fireOutListeners( S state ) {
    for ( Callback<S> cb : AtomicMarkedState.this.outStateListeners.get( state ) ) {
      try {
        cb.fire( state );
      } catch ( Throwable t ) {
        Exceptions.trace( "Firing state-out listeners failed for :" + cb.getClass( ).getCanonicalName( ), t );
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
  
  private final void afterLeave( final T transitionName, final ActiveTransition tid ) throws IllegalStateException {
    try {
      this.fireOutListeners( tid.getFromState( ) );
      tid.leave( );
    } catch ( Throwable t ) {
      this.rollback( );
      throw Exceptions.trace( new IllegalStateException( String.format( "Failed to apply transition %s because leave() threw an exception: %s",
                                                                        transitionName.toString( ), t.getMessage( ) ), t ) );
    }
  }
  
  private final ActiveTransition beforeLeave( final T transitionName ) throws IllegalStateException {
    final ActiveTransition tid;
    try {
      tid = this.request( transitionName );
    } catch ( Throwable t ) {
      this.rollback( );
      throw Exceptions.trace( new IllegalStateException( String.format( "Failed to apply transition %s because request() threw an exception.",
                                                                        transitionName.toString( ) ), t ) );
    }
    return tid;
  }
  
  /**
   * @see com.eucalyptus.util.fsm.State#getState()
   * @return
   */
  public S getState( ) {
    return this.state.getReference( );
  }
  
  /**
   * @see com.eucalyptus.util.fsm.State#getStates()
   * @return
   */
  public ImmutableList<S> getStates( ) {
    return this.immutableStates;
  }
  
  public ImmutableList<Transition<P, S, T>> getTransitions( ) {
    return immutableTransitions;
  }
  
  /**
   * @see java.lang.Object#toString()
   * @return
   */
  public String toString( ) {
    return String.format( "State:name=%s:state=%s:mark=%s", this.name, this.state.getReference( ), this.state.isMarked( ) );
  }
  
  /**
   * @return the name
   */
  public String getName( ) {
    return this.name;
  }
  
  public class ActiveTransition extends Callback.Completion implements HasName<ActiveTransition> {
    private final Long                id;
    private final String              name;
    private final Long                startTime;
    private final Transition<P, S, T> transition;
    
    public void fire( ) {
      try {
        this.transition.enter( AtomicMarkedState.this.parent );
        try {
          AtomicMarkedState.this.commit( );
        } catch ( IllegalStateException t ) {
          LOG.trace( t, t );
        }
      } catch ( Throwable t ) {
        LOG.error( t, t );
        AtomicMarkedState.this.rollback( );
      }
      try {
        this.transition.after( AtomicMarkedState.this.parent );
      } catch ( Throwable t ) {
        LOG.error( t, t );
      }
    }
    
    public void fireException( Throwable t ) {
      AtomicMarkedState.this.error( );
    }
    
    public ActiveTransition( Long id, Transition<P, S, T> transition ) {
      this.id = id;
      this.startTime = System.nanoTime( );
      this.transition = transition;
      this.name = AtomicMarkedState.this.getName( ) + "-" + this.transition.getName( ) + "-" + id;
    }
    
    public final Long getId( ) {
      return this.id;
    }
    
    public void leave( ) {
      this.transition.leave( AtomicMarkedState.this.parent, this );
    }
    
    public Boolean getFromStateMark( ) {
      return this.transition.getFromStateMark( );
    }
    
    public S getFromState( ) {
      return this.transition.getFromState( );
    }
    
    public Boolean getToStateMark( ) {
      return this.transition.getToStateMark( );
    }
    
    public S getToState( ) {
      return this.transition.getToState( );
    }
    
    public Boolean getErrorStateMark( ) {
      return this.transition.getErrorStateMark( );
    }
    
    public S getErrorState( ) {
      return this.transition.getErrorState( );
    }
    
    public String getName( ) {
      return this.name;
    }
    
    public int compareTo( ActiveTransition that ) {
      return this.id.compareTo( that.id );
    }
    
    public String toString( ) {
      return String.format( "ActiveTransition:name=%s:id=%s:startTime=%s:transition=%s", this.name, this.id, this.startTime, this.transition );
    }
  }
}
