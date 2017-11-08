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

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.Callback.Completion;
import com.eucalyptus.util.fsm.TransitionListener.Phases;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

public class TransitionImpl<P extends HasName<P>, S extends Automata.State, T extends Automata.Transition> implements TransitionAction<P>, TransitionHandler<P, S, T>, TransitionRule<S, T> {
  private static Logger                                       LOG       = Logger.getLogger( TransitionImpl.class );
  private final AtomicInteger                                 index     = new AtomicInteger( 0 );
  private final TransitionRule<S, T>                          rule;
  private final ConcurrentMap<Integer, TransitionListener<P>> listeners = Maps.newConcurrentMap( );
  private final TransitionAction<P>                           action;
  
  TransitionImpl( final TransitionRule<S, T> transitionRule, final TransitionAction<P> action, TransitionListener<P>... listeners ) {
    this.rule = transitionRule;
    this.action = action;
    for ( TransitionListener<P> listener : listeners ) {
      this.addListener( listener );
    }
  }
  
  /**
   * Add a transition listener. The stages of the transition will execute for
   * each listener in the order it was added:
   * <ol>
   * <li>{@link TransitionListener#before()}</li>
   * <li>{@link TransitionListener#leave()}</li>
   * <li>{@link TransitionListener#enter()}</li>
   * <li>{@link TransitionListener#after()}</li>
   * </ol>
   * 
   * @param transitionListener
   */
  @Override
  public TransitionHandler<P, S, T> addListener( TransitionListener<P> listener ) {
    Logs.extreme( ).debug( EventRecord.here( TransitionImpl.class, EventType.TRANSITION, this.toString( ), "addListener", listener.getClass( ).getSimpleName( ) ) );
    this.listeners.put( index.incrementAndGet( ), listener );
    return this;
  }
  
  private void removeListener( Integer key ) {
    TransitionListener<P> listener = this.listeners.remove( key );
    Logs.extreme( ).debug( EventRecord.here( TransitionImpl.class, EventType.TRANSITION, this.toString( ), "removeListener", listener.getClass( ).getSimpleName( ) ) );
  }
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionRule#getName()
   */
  @Override
  public T getName( ) {
    return this.rule.getName( );
  }
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionRule#getFromStateMark()
   */
  public Boolean getFromStateMark( ) {
    return this.rule.getFromStateMark( );
  }
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionRule#getFromState()
   */
  public S getFromState( ) {
    return this.rule.getFromState( );
  }
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionRule#getToStateMark()
   */
  public Boolean getToStateMark( ) {
    return this.rule.getToStateMark( );
  }
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionRule#getToState()
   */
  public S getToState( ) {
    return this.rule.getToState( );
  }
  
  /**
   * @return
   * @see com.eucalyptus.util.fsm.TransitionRule#getErrorState()
   */
  public S getErrorState( ) {
    return this.rule.getErrorState( );
  }
  
  /**
   * @return
   * @see com.eucalyptus.util.fsm.TransitionRule#getErrorStateMark()
   */
  public Boolean getErrorStateMark( ) {
    return this.rule.getErrorStateMark( );
  }
  
  private boolean fireListeners( final TransitionListener.Phases phase, final Predicate<TransitionListener<P>> pred, P parent ) {
    for ( Entry<Integer, TransitionListener<P>> entry : this.listeners.entrySet( ) ) {
      final TransitionListener<P> tl = entry.getValue( );
      Logs.extreme( ).trace( EventRecord.here( TransitionImpl.class, EventType.TRANSITION_LISTENER, "" + parent.getName( ), this.toString( ),
                                               phase.toString( ),//
                                               entry.getKey( ).toString( ), tl.getClass( ).toString( ) ) );
      try {
        if ( !pred.apply( entry.getValue( ) ) ) {
          throw new TransitionListenerException( entry.getValue( ).getClass( ).getSimpleName( ) + "." + phase + "( ) returned false." );
        }
      } catch ( Exception t ) {
        Logs.extreme( ).error( t, t );
        return false;
      }
    }
    return true;
  }
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionListener#before()
   */
  public boolean before( final P parent ) {
    if ( this.action == null ) {
      throw new IllegalStateException( "Attempt to apply delegated transition before it is defined." );
    } else {
      this.fireListeners( Phases.before, new Predicate<TransitionListener<P>>( ) {
        @Override
        public boolean apply( TransitionListener<P> listener ) {
          return listener.before( parent );
        }
      }, parent );
      try {
        return this.action.before( parent );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        return false;
      }
    }
  }
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionListener#leave()
   */
  @Override
  public void leave( final P parent, final Completion transitionCallback ) {
    if ( this.action == null ) {
      throw new IllegalStateException( "Attempt to apply delegated transition before it is defined." );
    } else {
      try {
        this.action.leave( parent, transitionCallback );
        this.fireListeners( Phases.leave, new Predicate<TransitionListener<P>>( ) {
          @Override
          public boolean apply( TransitionListener<P> listener ) {
            listener.leave( parent );
            return true;
          }
        }, parent );
      } catch ( Exception ex ) {
        Logs.extreme( ).error( ex , ex );
        transitionCallback.fireException( new TransitionException( ex ) );
      }
    }
  }
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionListener#enter()
   */
  public void enter( final P parent ) {
    if ( this.action == null ) {
      throw new IllegalStateException( "Attempt to apply delegated transition before it is defined." );
    } else {
      this.fireListeners( Phases.enter, new Predicate<TransitionListener<P>>( ) {
        @Override
        public boolean apply( TransitionListener<P> listener ) {
          listener.enter( parent );
          return true;
        }
      }, parent );
      this.action.enter( parent );
    }
  }
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionListener#after()
   */
  public void after( final P parent ) {
    if ( this.action == null ) {
      throw new IllegalStateException( "Attempt to apply delegated transition before it is defined." );
    } else {
      this.fireListeners( Phases.after, new Predicate<TransitionListener<P>>( ) {
        @Override
        public boolean apply( TransitionListener<P> listener ) {
          listener.after( parent );
          return true;
        }
      }, parent );
      this.action.after( parent );
    }
  }
  
  @Override
  public String toString( ) {
    String actionName = "null";
    if( this.action != null ) {
      actionName = this.action.getClass( ).getName( ).replaceAll( "^(\\w.)*", "" );
    }
//    Iterable<String> listenerNames = Iterables.transform( this.listeners.values( ), new Function<TransitionListener<P>, String>( ) {
//      public String apply( TransitionListener<P> arg0 ) {
//        return arg0.getClass( ).getName( ).replaceAll( "^(\\w.)*", "" );
//      }
//    } );
    return String.format( "Transition name=%s from=%s/%s to=%s/%s error=%s action=%s", this.getName( ), this.getFromState( ), this.getFromStateMark( ),
                          this.getToState( ), this.getToStateMark( ), this.getErrorState( ), "" + this.action );
  }
  
  /**
   * @return the rule
   */
  @Override
  public TransitionRule<S, T> getRule( ) {
    return this.rule;
  }
  
  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.rule == null )
      ? 0
      : this.rule.hashCode( ) );
    return result;
  }
  
  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    TransitionImpl other = ( TransitionImpl ) obj;
    return this.rule.equals( other.rule );
  }
  
  /**
   * @return the action
   */
  @Override
  public TransitionAction<P> getAction( ) {
    return this.action;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo( TransitionRule<S, T> that ) {
    return this.getName( ).compareTo( that.getName( ) );
  }
  
}
