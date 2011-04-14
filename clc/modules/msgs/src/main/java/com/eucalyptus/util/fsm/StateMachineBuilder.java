package com.eucalyptus.util.fsm;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.async.Callback;
import com.eucalyptus.util.async.Callback.Completion;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class StateMachineBuilder<P extends HasName<P>, S extends Enum<S>, T extends Enum<T>> {
  private static Logger                  LOG               = Logger.getLogger( StateMachineBuilder.class );
  
  protected P                            parent;
  protected S                            startState;
  private ImmutableList<S>               immutableStates;
  private final Set<Transition<P, S, T>> transitions       = Sets.newHashSet( );
  private final Multimap<S, Callback<S>> inStateListeners  = ArrayListMultimap.create( );
  private final Multimap<S, Callback<S>> outStateListeners = ArrayListMultimap.create( );
  
  protected class InStateBuilder {
    S state;
    
    public void run( Callback<S> callback ) {
      inStateListeners.put( state, callback );
    }
    public void run( final Predicate<S> predicate ) {
      inStateListeners.put( state, new Callback<S>() {{ }
      @Override
      public void fire( S s ) {
        predicate.apply( s );
      }} );
    }
  }
  
  protected class OutStateBuilder {
    S state;
    
    public void run( Callback<S> callback ) {
      outStateListeners.put( state, callback );
    }
  }
  
  protected class TransitionBuilder {
    Transition<P, S, T>          transition;
    T                            name;
    S                            fromState;
    S                            toState;
    S                            errorState;
    TransitionAction<P>          action;
    List<TransitionListener<P>> listeners = Lists.newArrayList( );
    
    TransitionBuilder init( TransitionAction<P> action ) {
      this.action = action;
      this.errorState = ( this.errorState == null )
        ? this.fromState
        : this.errorState;
      this.transition = Transitions.create( this.name, this.fromState, this.toState, this.errorState, this.action, this.listeners.toArray( new TransitionListener[] {} ) );
      this.listeners = null;;
      return this;
    }
    
    private void commit( ) {
      StateMachineBuilder.this.addTransition( transition );
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
    
    public void outOfBand( ) {
      this.init( new TransitionAction<P>( ) {
        public boolean before( P parent ) {
          return true;
        }
        
        public void leave( P parent, Completion transitionCallback ) {}
        
        public void enter( P parent ) {}
        
        public void after( P parent ) {}
        
        public String toString( ) {
          return "TransitionAction.outOfBand";
        }
      } );
      this.commit( );
    }

    public void oob( ) {
      this.init( TransitionAction.OUTOFBAND );
      this.commit( );
    }
    
    public void noop( ) {
      this.init( TransitionAction.NOOP );
      this.commit( );
    }
    
    public void run( TransitionAction<P> action ) {
      this.init( action );
      this.commit( );
    }
    
    public TransitionBuilder add( TransitionListener<P> listener ) {
      if( this.listeners == null ) {
        this.transition.addListener( listener );
      } else {
        this.listeners.add( listener );
      }
      return this;
    }
    
    public TransitionBuilder add( TransitionListener<P>... listeners ) {
      if( this.listeners == null ) {
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
  
  protected StateMachineBuilder<P, S, T> addTransition( Transition<P, S, T> transition ) {
    if ( this.transitions.contains( transition ) ) {
      throw new IllegalArgumentException( "Duplicate transition named: " + transition.getName( ) );
    } else {
      this.transitions.add( transition );
    }
    return this;
  }
  
  public AtomicMarkedState<P, S, T> newAtomicState( ) {
    if ( startState == null || parent == null || transitions == null || transitions.isEmpty( ) ) {
      throw new IllegalStateException( "Call to build() for an ill-formed state machine -- did you finish adding all the transition rules?" );
    }
    this.doChecks( );
    return new AtomicMarkedState<P, S, T>( startState, parent, transitions, inStateListeners, outStateListeners );
  }
  
  private void doChecks( ) {
    this.immutableStates = ImmutableList.of( this.startState.getDeclaringClass( ).getEnumConstants( ) );
    if ( this.transitions.isEmpty( ) ) {
      throw new IllegalStateException( "Started state machine with no registered transitions." );
    }
    T[] trans = this.transitions.iterator( ).next( ).getName( ).getDeclaringClass( ).getEnumConstants( );
    Map<String, Transition<P, S, T>> alltransitions = Maps.newHashMap( );
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
    Multimap<T, Transition<P, S, T>> transNames = ArrayListMultimap.create( );
    for ( Transition<P, S, T> t : this.transitions ) {
      transNames.put( t.getName( ), t );
    }
    for ( T t : trans ) {
//      LOG.debug( "fsm " + this.parent.getName( ) + " transitions:" + ( transNames.containsKey( t )
//        ? transNames.get( t )
//        : t.name( ) + ":NONE" ) );
      for ( Transition<P, S, T> tr : transNames.get( t ) ) {
        String trKey = String.format( "%s.%s->%s.%s (err=%s.%s)", tr.getFromState( ), tr.getFromStateMark( ), tr.getToState( ), tr.getToStateMark( ), tr.getErrorState( ), tr.getErrorStateMark( ) );
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
  
}
