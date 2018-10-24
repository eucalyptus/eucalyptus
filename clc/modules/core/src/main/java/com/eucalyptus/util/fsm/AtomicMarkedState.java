/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.eucalyptus.util.fsm.Automata.State;
import com.google.common.base.Supplier;
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
  private static final AtomicLong                            id                   = new AtomicLong( 0l );
  private final AtomicReference<ActiveTransition>            currentTransition    = new AtomicReference<ActiveTransition>( null );
  
  public AtomicMarkedState( S startState, P parent, Set<TransitionHandler<P, S, T>> transitions, //
                            Multimap<S, Callback<P>> inStateListeners, Multimap<S, Callback<P>> outStateListeners ) {
    this.startState = startState;
    String tempName = null;
    if ( parent instanceof HasFullName ) {
      try {
        tempName = ( ( HasFullName ) parent ).getFullName( ).toString( );
      } catch ( Exception ex ) {}
    }
    this.name = ( tempName != null ? tempName : String.format( "%s-%s", parent.getClass( ).getSimpleName( ), parent.getName( ) ) );
    this.parent = parent;
    final S[] states = State.asEnum.getEnumConstants( startState );
    this.stateTransitions = new HashMap<S, Map<S, TransitionHandler<P, S, T>>>( ) {
      {
        for ( S s : states ) {
          this.put( s, new HashMap<S, TransitionHandler<P, S, T>>( ) );
        }
      }
    };
    this.immutableStates = ImmutableList.copyOf( states );
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
  
  /**
   * TODO:GRZE: remove this in the future.
   */
  @Deprecated
  public CheckedListenableFuture<P> transitionByName( T transitionName ) throws IllegalStateException, ExistingTransitionException {
    if ( this.state.isMarked( ) ) {
      throw new ExistingTransitionException( "Transition request transition=" + transitionName
                                             + " rejected because of an ongoing transition: "
                                             + this.toString( ) );
    } else if ( !this.transitions.containsKey( transitionName ) ) {
      throw new NoSuchElementException( "No such transition named: " + transitionName.toString( )
                                        + ". Known transitions: "
                                        + this.getTransitions( ) );
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
      throw new ExistingTransitionException( "Transition request state=" + nextState
                                             + " rejected because of an ongoing transition: "
                                             + this.toString( ) );
    } else if ( !this.stateTransitions.get( this.state.getReference( ) ).containsKey( nextState ) ) {
      throw new NoSuchElementException( "No transition to " + nextState.toString( )
                                        + " from current state "
                                        + this.toString( )
                                        + ". Known transitions: "
                                        + this.getTransitions( ) );
    } else {
      T transitionName = this.stateTransitions.get( this.state.getReference( ) ).get( nextState ).getName( );
      this.checkTransition( transitionName );
      final ActiveTransition tid = this.beforeLeave( transitionName );
      return this.afterLeave( transitionName, tid );
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
      throw new ExistingTransitionException( "Transition request " + transitionName
                                             + " rejected because of an ongoing transition: "
                                             + this.toString( ) );
    } else {
      this.currentTransition.set( this.create( rule, transition ) );
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
    throw new NoSuchElementException( "No such transition: " + transitionName
                                      + " for the current state "
                                      + fromState
                                      + " and mark "
                                      + mark[0]
                                      + " for parent "
                                      + this.parent.toString( ) );
  }
  
  /**
   * @see com.eucalyptus.util.fsm.State#commit()
   */
  private void commit( ) {
    Logs.exhaust( ).trace( "Transition commit(): " + this.currentTransition.get( ) );
    if ( !this.state.isMarked( ) ) {
      IllegalStateException ex = new IllegalStateException( "commit() called when there is no currently pending transition: " + this.toString( ) );
      Logs.exhaust( ).error( ex, ex );
    } else {
      ActiveTransition tr = this.currentTransition.getAndSet( null );
      this.state.set( tr.getTransitionRule( ).getToState( ), tr.getTransitionRule( ).getToStateMark( ) );
      if ( !tr.getTransitionRule( ).getFromState( ).equals( tr.getTransitionRule( ).getToState( ) ) ) {
        this.state.set( tr.getTransitionRule( ).getToState( ), false );
        this.fireInListeners( tr.getTransitionRule( ).getToState( ) );
      } else {
        this.state.set( tr.getTransitionRule( ).getToState( ), false );
      }
      tr.getTransitionFuture( ).set( this.parent );
      try {
        EventRecord.caller( this.getClass( ), EventType.TRANSITION_FUTURE,
                            "set(" + this.parent.toString( )
                                + ":"
                                + this.parent.getClass( ).getCanonicalName( )
                                + ")" ).trace( );
      } catch ( Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
      }
    }
  }
  
  private void error( Throwable t ) {
    Logs.extreme( ).error( "Transition error(): " + this.toString( ), t );
    if ( !this.state.isMarked( ) ) {
      IllegalStateException ex = new IllegalStateException( "error() called when there is no currently pending transition: "
                                                            + this.toString( ), t );
      Logs.exhaust( ).error( ex );
      ActiveTransition tr = this.currentTransition.getAndSet( null );
      if ( tr != null ) {
        tr.getTransitionFuture( ).setException( t );
        this.state.set( tr.getTransitionRule( ).getErrorState( ), false );
      }
    } else {
      ActiveTransition tr = this.currentTransition.getAndSet( null );
      if ( tr != null ) {
        Logs.extreme( ).error( "Transition error(): " + this.toString( ) + "Transition error(): START STACK\n" + tr.startStackTrace );
        Logs.extreme( ).error( "Transition error(): " + this.toString( ) + "Transition error(): END STACK" + tr.endStackTrace.get( ) );
        this.state.set( tr.getTransitionRule( ).getErrorState( ), tr.getTransitionRule( ).getErrorStateMark( ) );
        if ( !tr.getTransitionRule( ).getFromState( ).equals( tr.getTransitionRule( ).getErrorState( ) ) ) {
          this.state.set( tr.getTransitionRule( ).getErrorState( ), false );
          this.fireInListeners( tr.getTransitionRule( ).getErrorState( ) );
        } else {
          this.state.set( tr.getTransitionRule( ).getErrorState( ), false );
        }
        EventRecord.caller( this.getClass( ), EventType.TRANSITION_FUTURE, "setException(" + t.getClass( ).getCanonicalName( )
                                                                           + "): "
                                                                           + t.getMessage( ) ).trace( );
        tr.getTransitionFuture( ).setException( t );
      }
    }
  }
  
  private void rollback( Throwable t ) {
    LOG.debug( "Transition rollback(): " + this.toString( ) );
    if ( !this.state.isMarked( ) ) {
      Exceptions.trace( new IllegalStateException( "rollback() called when there is no currently pending transition: " + this.toString( ) ) );
    } else {
      ActiveTransition tr = this.currentTransition.getAndSet( null );
      if ( tr != null ) {
        this.state.set( tr.getTransitionRule( ).getFromState( ), false );
      }
    }
  }
  
  protected void fireInListeners( S state ) {
    for ( Callback<P> cb : AtomicMarkedState.this.inStateListeners.get( state ) ) {
      try {
        Logs.exhaust( ).debug( "Firing state-in listener: " + cb.getClass( )
                               + " for "
                               + this.toString( ) );
        cb.fire( this.parent );
      } catch ( Exception t ) {
        Exceptions.trace( "Firing state-in listeners failed for :" + cb.getClass( ).getCanonicalName( ), Exceptions.filterStackTrace( t ) );
      }
    }
  }
  
  protected void fireOutListeners( S state ) {
    for ( Callback<P> cb : AtomicMarkedState.this.outStateListeners.get( state ) ) {
      try {
        Logs.exhaust( ).debug( "Firing state-in listener: " + cb.getClass( )
                               + " for "
                               + this.toString( ) );
        cb.fire( this.parent );
      } catch ( Exception t ) {
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
    } catch ( Exception t ) {
      throw Exceptions.trace( new IllegalStateException( String.format( "Failed to apply transition %s because before() threw an exception: %s",
                                                                        transitionName.toString( ), t.getMessage( ) ), t ) );
    }
  }
  
  private final CheckedListenableFuture<P> afterLeave( final T transitionName, final ActiveTransition tid ) throws IllegalStateException {
    try {
      CheckedListenableFuture<P> result = tid.leave( );
      try {
        this.fireOutListeners( tid.getTransitionRule( ).getFromState( ) );
      } catch ( Exception ex ) {
        Logs.extreme( ).error( ex, ex );
      }
      return result;
    } catch ( Exception t ) {
      this.error( t );
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
    } catch ( Exception t ) {
      this.error( t );
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
    return String.format(
      "State:name=%s:state=%s:mark=%s:transition=%s",
      this.name,
      this.state.getReference( ),
      this.state.isMarked( ),
      ( t != null
                 ? t.toString( )
                 : "idle" ) );
  }
  
  /**
   * @return the name
   */
  public String getName( ) {
    return this.name;
  }
  
  private ActiveTransition create( TransitionRule<S, T> rule, TransitionAction<P> transition ) {
    return new ActiveTransition( AtomicMarkedState.id.incrementAndGet( ), rule, transition );
  }
  
  private class ActiveTransition extends Callback.Completion implements HasName<ActiveTransition>, TransitionRecord {
    private final Long                       txId;
    private final String                     txName;
    private final Long                       startTime;
    private Long                             endTime          = 0l;
    private final TransitionAction<P>        transition;
    private final String                     startStackTrace;
    private final Supplier<String>           endStackTrace;
    private final CheckedListenableFuture<P> transitionFuture = Futures.newGenericeFuture( );
    private final TransitionRule<S, T>       rule;
    
    private ActiveTransition( Long id, TransitionRule<S, T> rule, TransitionAction<P> transition ) {
      this.txId = id;
      this.startTime = System.currentTimeMillis( );
      this.endTime = 0l;
      this.rule = rule;
      this.transition = transition;
      this.txName = AtomicMarkedState.this.getName( ) + " #" + this.txId + " " + this.rule.getName( );
      if ( Logs.isExtrrreeeme( ) ) {
        this.startStackTrace = Threads.currentStackRange( 0, 32 );
      } else {
        this.startStackTrace = Threads.currentStackFrame( 0 ).toString( );
      }
      this.endStackTrace = new Supplier<String>( ) {
        
        @Override
        public String get( ) {
          if ( Logs.isExtrrreeeme( ) ) {
            return Threads.currentStackRange( 0, 32 );
          } else {
            return Threads.currentStackFrame( 3 ).toString( );
          }
        }
      };
    }
    
    CheckedListenableFuture<P> getTransitionFuture( ) {
      return this.transitionFuture;
    }
    
    @Override
    public boolean isDone( ) {
      return this.transitionFuture.isDone( );
    }
    
    public void fire( ) {
      try {
        if ( !this.isDone( ) ) {
          this.transition.enter( AtomicMarkedState.this.parent );
          this.transition.after( AtomicMarkedState.this.parent );
          try {
            AtomicMarkedState.this.commit( );
          } catch ( Exception ex ) {}
        }
      } catch ( Exception t ) {
        this.fireException( t );
//      } finally {
//        this.transitionFuture.set( AtomicMarkedState.this.parent );
      }
    }
    
    public void fireException( Throwable t ) {
      try {
        if ( Logs.isExtrrreeeme( ) ) {
          Logs.extreme( ).error( this.startStackTrace );
          Logs.extreme( ).error( this.endStackTrace.get( ) );
        }
        AtomicMarkedState.this.error( t );
      } finally {
        this.transitionFuture.setException( t );
      }
    }
    
    public final Long getId( ) {
      return this.txId;
    }
    
    public TransitionRule<S, T> getTransitionRule( ) {
      return this.rule;
    }
    
    public CheckedListenableFuture<P> leave( ) {
      try {
        this.transition.leave( AtomicMarkedState.this.parent, this );
        return this.transitionFuture;
      } catch ( Exception ex ) {
        this.transitionFuture.setException( ex );
        return this.transitionFuture;
      }
    }
    
    public String getName( ) {
      return this.txName;
    }
    
    public int compareTo( ActiveTransition that ) {
      return this.txId.compareTo( that.txId );
    }
    
    public String toString( ) {
      StringBuilder sb = new StringBuilder( );
      sb.append( EventType.TRANSITION ).append( " " ).append( this.txName ).append( " Active" ).append( this.transition != null
                                                                                                                               ? this.transition.toString( )
                                                                                                                               : "null" ).append( " id=" ).append(
        this.txId ).append( " startTime=" ).append( new Date( this.startTime ) );
      Logs.exhaust( ).trace( sb.toString( ) );
      return sb.toString( );
    }

    @Override
    public Long getTxId( ) {
      return this.txId;
    }

    @Override
    public String getTxName( ) {
      return this.txName;
    }

    public Long getStartTime( ) {
      return this.startTime;
    }

    public Long getEndTime( ) {
      return this.endTime;
    }

    @Override
    public TransitionAction<P> getTransition( ) {
      return this.transition;
    }

    public String getStartStackTrace( ) {
      return this.startStackTrace;
    }

    public Supplier<String> getEndStackTrace( ) {
      return this.endStackTrace;
    }

    @Override
    public TransitionRule<S, T> getRule( ) {
      return this.rule;
    }
  }
  
  public P getParent( ) {
    return this.parent;
  }

  public TransitionRecord getTransitionRecord( ) {
    return this.currentTransition.get( );
  }
}
