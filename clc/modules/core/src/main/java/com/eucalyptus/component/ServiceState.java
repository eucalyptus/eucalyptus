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

package com.eucalyptus.component;

import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListSet;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.Component.Transition;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.eucalyptus.util.fsm.ExistingTransitionException;
import com.eucalyptus.util.fsm.StateMachine;
import com.eucalyptus.util.fsm.StateMachineBuilder;
import com.eucalyptus.util.fsm.TransitionAction;
import com.eucalyptus.util.fsm.TransitionHandler;
import com.eucalyptus.util.fsm.TransitionRecord;
import com.eucalyptus.util.fsm.Transitions;
import com.google.common.collect.ImmutableList;

public class ServiceState implements StateMachine<ServiceConfiguration, Component.State, Component.Transition> {
  private final StateMachine<ServiceConfiguration, Component.State, Component.Transition> stateMachine;
  private final ServiceConfiguration                                                      parent;

  public ServiceState( final ServiceConfiguration parent ) {
    this.parent = parent;
    this.stateMachine = this.buildStateMachine( );
  }
  
  @Override
  public Component.State getState( ) {
    return this.stateMachine.getState( );
  }
  
  private StateMachine<ServiceConfiguration, Component.State, Component.Transition> buildStateMachine( ) {
    final TransitionAction<ServiceConfiguration> noop = Transitions.noop( );
    return new StateMachineBuilder<ServiceConfiguration, State, Transition>( this.parent, State.PRIMORDIAL ) {
      {
        /**
         * GRZE:TODO: there seems to be a missing case for cleaning up deregistered components:
         * e.g., in( State.STOPPED ).run( ServiceTransitions.StateCallbacks.PROPERTIES_REMOVE ) )
         * ?!?!? but not that.
         **/
        in( State.NOTREADY ).run( ServiceTransitions.StateCallbacks.ENSURE_DISABLED ).run( ServiceTransitions.StateCallbacks.FIRE_STATE_EVENT );
        from( State.PRIMORDIAL ).to( State.INITIALIZED ).error( State.BROKEN ).on( Transition.INITIALIZING ).run( noop );
        from( State.PRIMORDIAL ).to( State.BROKEN ).error( State.BROKEN ).on( Transition.FAILED_TO_PREPARE ).run( noop );
        from( State.INITIALIZED ).to( State.LOADED ).error( State.BROKEN ).on( Transition.LOAD ).addListener( ServiceTransitions.StateCallbacks.STATIC_PROPERTIES_ADD ).addListener( ServiceTransitions.StateCallbacks.PROPERTIES_ADD ).run( ServiceTransitions.TransitionActions.LOAD );
        from( State.LOADED ).to( State.NOTREADY ).error( State.BROKEN ).on( Transition.START ).addListener( ServiceTransitions.StateCallbacks.FIRE_STATE_EVENT ).addListener( ServiceTransitions.StateCallbacks.STATIC_PROPERTIES_ADD ).addListener( ServiceTransitions.StateCallbacks.PROPERTIES_ADD ).run( ServiceTransitions.TransitionActions.START );
        from( State.NOTREADY ).to( State.DISABLED ).error( State.NOTREADY ).on( Transition.READY_CHECK ).addListener( ServiceTransitions.StateCallbacks.STATIC_PROPERTIES_ADD ).addListener( ServiceTransitions.StateCallbacks.PROPERTIES_ADD ).run( ServiceTransitions.TransitionActions.CHECK );
        from( State.DISABLED ).to( State.ENABLED ).error( State.NOTREADY ).on( Transition.ENABLE ).addListener( ServiceTransitions.StateCallbacks.FIRE_STATE_EVENT ).run( ServiceTransitions.TransitionActions.ENABLE );
        from( State.DISABLED ).to( State.STOPPED ).error( State.NOTREADY ).on( Transition.STOP ).addListener( ServiceTransitions.StateCallbacks.FIRE_STATE_EVENT ).run( ServiceTransitions.TransitionActions.STOP );
        from( State.NOTREADY ).to( State.STOPPED ).error( State.NOTREADY ).on( Transition.STOPPING_NOTREADY ).addListener( ServiceTransitions.StateCallbacks.FIRE_STATE_EVENT ).run( ServiceTransitions.TransitionActions.STOP );
        from( State.DISABLED ).to( State.DISABLED ).error( State.NOTREADY ).on( Transition.DISABLED_CHECK ).addListener( ServiceTransitions.StateCallbacks.STATIC_PROPERTIES_ADD ).run( ServiceTransitions.TransitionActions.CHECK );
        from( State.ENABLED ).to( State.DISABLED ).error( State.NOTREADY ).on( Transition.DISABLE ).addListener( ServiceTransitions.StateCallbacks.FIRE_STATE_EVENT ).run( ServiceTransitions.TransitionActions.DISABLE );
        from( State.ENABLED ).to( State.ENABLED ).error( State.NOTREADY ).on( Transition.ENABLED_CHECK ).addListener( ServiceTransitions.StateCallbacks.STATIC_PROPERTIES_ADD ).run( ServiceTransitions.TransitionActions.CHECK );
        from( State.STOPPED ).to( State.INITIALIZED ).error( State.BROKEN ).on( Transition.DESTROY ).run( /*ServiceTransitions.TransitionActions.DESTROY*/noop );//GRZE: DESTROY removes the service configuration so leaving INITIALIZED becomes impossible 
        from( State.BROKEN ).to( State.STOPPED ).error( State.BROKEN ).on( Transition.STOPPING_BROKEN ).run( noop );
        from( State.BROKEN ).to( State.INITIALIZED ).error( State.BROKEN ).on( Transition.RELOAD ).run( noop );
        from( State.STOPPED ).to( State.PRIMORDIAL ).error( State.BROKEN ).on( Transition.REMOVING ).run( ServiceTransitions.TransitionActions.DESTROY );//TODO:GRZE: handle deregistering transition here.
      }
    }.newAtomicMarkedState( );
  }
  
  @Override
  public CheckedListenableFuture<ServiceConfiguration> transition( Component.State state ) throws IllegalStateException, NoSuchElementException, ExistingTransitionException {
    try {
      return this.stateMachine.transition( state );
    } catch ( IllegalStateException ex ) {
      throw Exceptions.trace( ex );
    } catch ( NoSuchElementException ex ) {
      throw Exceptions.trace( ex );
    } catch ( ExistingTransitionException ex ) {
      throw ex;
    } catch ( Exception ex ) {
      throw Exceptions.trace( new RuntimeException( "Failed to perform transition from " + this.getState( ) + " to " + state + " for " + this.parent.getName( )
                                                    + ".\nCAUSE: " + ex.getMessage( ) + "\nSTATE: " + this.stateMachine.toString( ), ex ) );
    }
  }
  
  @Override
  public boolean isBusy( ) {
    return this.stateMachine.isBusy( );
  }
  
  @Override
  public ImmutableList<State> getStates( ) {
    return this.stateMachine.getStates( );
  }
  
  @Override
  public ImmutableList<TransitionHandler<ServiceConfiguration, State, Transition>> getTransitions( ) {
    return this.stateMachine.getTransitions( );
  }
  
  @Override
  public boolean isLegalTransition( Transition transitionName ) {
    return this.stateMachine.isLegalTransition( transitionName );
  }
  
  @Override
  public ServiceConfiguration getParent( ) {
    return this.stateMachine.getParent( );
  }

  @Override
  public String toString( ) {
    return this.parent.getFullName( ).toString( );
  }

  @Override
  public TransitionRecord<ServiceConfiguration, State, Transition> getTransitionRecord( ) {
    return this.stateMachine.getTransitionRecord( );
  }
  
}
