package com.eucalyptus.component;

import com.eucalyptus.util.Transition;

public enum DefaultTransitionList {
  PREFLIGHT( Lifecycles.State.DISABLED, Lifecycles.State.PRIMORDIAL ),
  INITIALIZING( Lifecycles.State.PRIMORDIAL, Lifecycles.State.INITIALIZED ),
  LOADING( Lifecycles.State.INITIALIZED, Lifecycles.State.LOADED ),
  STARTING( Lifecycles.State.LOADED, Lifecycles.State.STARTED ),
  STOPPING( Lifecycles.State.STARTED, Lifecycles.State.STOPPED ),
  PAUSING( Lifecycles.State.STARTED, Lifecycles.State.PAUSED ),
  RESTARTING( Lifecycles.State.STOPPED, Lifecycles.State.STARTED ),
  RESUMING( Lifecycles.State.STOPPED, Lifecycles.State.PAUSED );
  private Lifecycles.State oldState;
  private Lifecycles.State newState;
  
  DefaultTransitionList( Lifecycles.State o, Lifecycles.State n ) {
    this.oldState = o;
    this.newState = n;
  }
  
  public Transition newInstance( ) {
    return new DefaultTransition( this.name( ), oldState, newState );
  }
}
