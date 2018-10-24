/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.Callback.Completion;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class StateMachineBuilder<P extends HasName<P>, S extends Automata.State, T extends Automata.Transition> {
  private static Logger                         LOG               = Logger.getLogger( StateMachineBuilder.class );
  
  protected P                                   parent;
  protected S                                   startState;
  private ImmutableList<S>                      immutableStates;
  private final Set<TransitionHandler<P, S, T>> transitions       = Sets.newHashSet( );
  private final Multimap<S, Callback<P>>        inStateListeners  = ArrayListMultimap.create( );
  private final Multimap<S, Callback<P>>        outStateListeners = ArrayListMultimap.create( );
  
  protected class InStateBuilder {
    S state;
    
    public InStateBuilder run( Callback<P> callback ) {
      inStateListeners.put( state, callback );
      return this;
    }
    
    public InStateBuilder run( final Predicate<P> predicate ) {
      inStateListeners.put( state, new Callback<P>( ) {
        {}
        
        @Override
        public void fire( P p ) {
          predicate.apply( p );
        }
      } );
      return this;
    }
  }
  
  protected class OutStateBuilder {
    S state;
    
    public OutStateBuilder run( Callback<P> callback ) {
      outStateListeners.put( state, callback );
      return this;
    }
    
    public OutStateBuilder run( final Predicate<P> predicate ) {
      outStateListeners.put( state, new Callback<P>( ) {
        {}
        
        @Override
        public void fire( P p ) {
          predicate.apply( p );
        }
      } );
      return this;
    }
  }
  
  protected class TransitionBuilder {
    TransitionHandler<P, S, T>  transition;
    T                           name;
    S                           fromState;
    S                           toState;
    S                           errorState;
    TransitionAction<P>         action;
    List<TransitionListener<P>> listeners = Lists.newArrayList( );
    
    TransitionBuilder commit( TransitionAction<P> action ) {
      this.action = action;
      this.errorState = ( this.errorState == null )
                                                   ? this.fromState
                                                   : this.errorState;
      TransitionRule<S, T> rule = new BasicTransitionRule<S, T>( name, fromState, toState, errorState );
      this.transition = new TransitionImpl<P, S, T>( rule, this.action, this.listeners.toArray( new TransitionListener[] {} ) );
      this.listeners = null;
      StateMachineBuilder.this.addTransition( transition );
      return this;
    }
    
    private void commit( ) {}
    
    public TransitionBuilder on( T transitionName ) {
      this.name = transitionName;
      return this;
    }
    
    public TransitionBuilder from( S fromState ) {
      this.fromState = fromState;
      return this;
    }
    
    public TransitionBuilder to( S toState ) {
      this.toState = toState;
      return this;
    }
    
    public TransitionBuilder error( S errorState ) {
      this.errorState = errorState;
      return this;
    }
    
    public void run( final Callback<P> callable ) {
      TransitionAction<P> action = Transitions.callbackAsAction( callable );
      this.commit( action );
    }
    
    public void run( Runnable function ) {
      TransitionAction<P> action = Transitions.runnableAsAction( function );
      this.commit( action );
    }
    
    public void run( Function<P, TransitionAction<P>> function ) {
      this.commit( function.apply( parent ) );
    }
    
    public void run( TransitionAction<P> action ) {
      this.commit( action );
    }
    
    public void run( final Predicate<P> predicate ) {
      TransitionAction<P> action = Transitions.predicateAsAction( predicate );
      this.commit( action );
    }
    
    public TransitionBuilder addListener( Callback<P> callback ) {
      TransitionListener<P> listener = Transitions.callbackAsListener( callback );
      if ( this.listeners == null ) {
        this.transition.addListener( listener );
      } else {
        this.listeners.add( listener );
      }
      return this;
    }
    
    public TransitionBuilder addListener( TransitionListener<P>... listeners ) {
      if ( this.listeners == null ) {
        for ( TransitionListener<P> l : listeners ) {
          transition.addListener( l );
        }
      } else {
        for ( TransitionListener<P> l : listeners ) {
          this.listeners.add( l );
        }
      }
      return this;
    }
    
  }
  
  protected InStateBuilder in( final S input ) {
    return new InStateBuilder( ) {
      {
        state = input;
      }
    };
  }
  
  protected OutStateBuilder out( final S input ) {
    return new OutStateBuilder( ) {
      {
        state = input;
      }
    };
  }
  
  protected TransitionBuilder on( final T input ) {
    return new TransitionBuilder( ) {
      {
        name = input;
      }
    };
  }
  
  protected TransitionBuilder from( final S input ) {
    return new TransitionBuilder( ) {
      {
        fromState = input;
      }
    };
  }
  
  protected StateMachineBuilder<P, S, T> addTransition( TransitionHandler<P, S, T> transition ) {
    if ( this.transitions.contains( transition ) ) {
      throw new IllegalArgumentException( "Duplicate transition named: " + transition.getName( ) );
    } else {
      this.transitions.add( transition );
    }
    return this;
  }
  
  public StateMachine<P, S, T> newAtomicMarkedState( ) {
    if ( startState == null || parent == null || transitions == null || transitions.isEmpty( ) ) {
      throw new IllegalStateException( "Call to build() for an ill-formed state machine -- did you finish adding all the transition rules?" );
    }
    this.doChecks( );
    return new AtomicMarkedState<P, S, T>( startState, parent, transitions, inStateListeners, outStateListeners );
  }
  
  private void doChecks( ) {
    this.immutableStates = ImmutableList.copyOf( this.startState.asEnum.getEnumConstants( this.startState ) );
    if ( this.transitions.isEmpty( ) ) {
      throw new IllegalStateException( "Started state machine with no registered transitions." );
    }
    T transitionName = this.transitions.iterator( ).next( ).getName( );
    T[] trans = transitionName.asEnum.getEnumConstants( transitionName );
    Map<String, TransitionHandler<P, S, T>> alltransitions = Maps.newHashMap( );
    for ( S s1 : this.immutableStates ) {
      for ( S s2 : this.immutableStates ) {
        alltransitions.put( String.format( "%s.%s->%s.%s", s1, false, s2, false ), null );
        alltransitions.put( String.format( "%s.%s->%s.%s", s1, false, s2, true ), null );
        alltransitions.put( String.format( "%s.%s->%s.%s", s1, true, s2, false ), null );
        alltransitions.put( String.format( "%s.%s->%s.%s", s1, true, s2, true ), null );
      }
    }
    LOG.debug( "Starting state machine: " + this.parent.getClass( ).getSimpleName( ) + " " + this.parent.getName( ) );
//    for ( S s : this.immutableStates ) {
//      LOG.debug( "fsm " + this.parent.getName( ) + "       state:" + s.name( ) );
//    }
    Multimap<T, TransitionHandler<P, S, T>> transNames = ArrayListMultimap.create( );
    for ( TransitionHandler<P, S, T> t : this.transitions ) {
      transNames.put( t.getName( ), t );
    }
    for ( T t : trans ) {
//      LOG.debug( "fsm " + this.parent.getName( ) + " transitions:" + ( transNames.containsKey( t )
//        ? transNames.get( t )
//        : t.name( ) + ":NONE" ) );
      for ( TransitionHandler<P, S, T> tr : transNames.get( t ) ) {
        String trKey = String.format( "%s.%s->%s.%s (err=%s.%s)", tr.getFromState( ), tr.getFromStateMark( ), tr.getToState( ), tr.getToStateMark( ),
                                      tr.getErrorState( ), tr.getErrorStateMark( ) );
        if ( alltransitions.get( trKey ) != null ) {
          LOG.error( "Duplicate transition: " + tr + " AND " + alltransitions.get( trKey ) );
          throw new IllegalStateException( "Duplicate transition: " + tr + " AND " + alltransitions.get( trKey ) );
        } else {
          alltransitions.put( trKey, tr );
        }
      }
    }
    for ( String trKey : alltransitions.keySet( ) ) {
      if ( alltransitions.get( trKey ) != null ) {
        LOG.debug( String.format( "fsm %s %-60.60s %s", this.parent.getName( ), trKey, alltransitions.get( trKey ) ) );
      }
    }
  }
  
  public StateMachineBuilder( P parent, S startState ) {
    super( );
    this.parent = parent;
    this.startState = startState;
  }
  
  static class BasicTransitionRule<S extends Automata.State, T extends Automata.Transition> implements TransitionRule<S, T> {
    private T             name;
    private S             fromState;
    private S             toState;
    private S             errorState;
    private final Boolean fromStateMark;
    private final Boolean toStateMark;
    private final Boolean errorStateMark;
    
    protected BasicTransitionRule( T name, S fromState, S toState ) {
      this.name = name;
      this.fromState = fromState;
      this.toState = toState;
      this.errorState = fromState;
      this.fromStateMark = false;
      this.toStateMark = false;
      this.errorStateMark = false;
    }
    
    protected BasicTransitionRule( T name, S fromState, S toState, S errorState ) {
      this.name = name;
      this.fromState = fromState;
      this.toState = toState;
      this.errorState = errorState;
      this.fromStateMark = false;
      this.toStateMark = false;
      this.errorStateMark = false;
    }
    
    protected BasicTransitionRule( T name, S fromState, Boolean fromStateMark, S toState, Boolean toStateMark, S errorState, Boolean errorStateMark ) {
      this.name = name;
      this.fromState = fromState;
      this.toState = toState;
      this.errorState = errorState;
      this.fromStateMark = fromStateMark;
      this.toStateMark = toStateMark;
      this.errorStateMark = errorStateMark;
    }
    
    @Override
    public final T getName( ) {
      return this.name;
    }
    
    @Override
    public final S getFromState( ) {
      return this.fromState;
    }
    
    @Override
    public final S getToState( ) {
      return this.toState;
    }
    
    @Override
    public final S getErrorState( ) {
      return this.errorState;
    }
    
    @Override
    public final Boolean getFromStateMark( ) {
      return this.fromStateMark;
    }
    
    @Override
    public final Boolean getToStateMark( ) {
      return this.toStateMark;
    }
    
    @Override
    public final Boolean getErrorStateMark( ) {
      return this.errorStateMark;
    }
    
    @Override
    public int compareTo( TransitionRule<S, T> that ) {
      return this.getName( ).compareTo( that.getName( ) );
    }
    
    @Override
    public String toString( ) {
      return String.format(
        "Transition name=%s from=%s/%s to=%s/%s error=%s",
        this.getName( ),
        this.getFromState( ),
        this.getFromStateMark( ),
        this.getToState( ),
        this.getToStateMark( ),
        this.getErrorState( ) );
    }
    
  }
  
}
