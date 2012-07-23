/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
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
  static Logger                                                                           LOG     = Logger.getLogger( ServiceState.class );
  private final StateMachine<ServiceConfiguration, Component.State, Component.Transition> stateMachine;
  private final ServiceConfiguration                                                      parent;
  private Component.State                                                                 goal    = Component.State.DISABLED;              //TODO:GRZE:OMGFIXME
  private final NavigableSet<String>                                                      details = new ConcurrentSkipListSet<String>( );
  
  public ServiceState( final ServiceConfiguration parent ) {
    this.parent = parent;
    this.stateMachine = this.buildStateMachine( );
  }
  
  @Override
  public Component.State getState( ) {
    return this.stateMachine.getState( );
  }
  
  public String getDetails( ) {
    return this.details.toString( );
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
        from( State.STOPPED ).to( State.INITIALIZED ).error( State.BROKEN ).on( Transition.DESTROY ).run( ServiceTransitions.TransitionActions.DESTROY );
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
  
  /**
   * @return the goal
   */
  public Component.State getGoal( ) {
    return this.goal;
  }
  
  void setGoal( final Component.State goal ) {
    this.goal = goal;
  }
  
  @Override
  public boolean isBusy( ) {
    return this.stateMachine.isBusy( );
  }
  
  protected boolean checkTransition( Transition transition ) {
    return this.parent.getComponentId( ).isAvailableLocally( ) && this.stateMachine.isLegalTransition( transition );
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
