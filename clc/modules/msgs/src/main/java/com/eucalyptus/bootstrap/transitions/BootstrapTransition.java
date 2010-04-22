package com.eucalyptus.bootstrap.transitions;

import com.eucalyptus.component.Lifecycles;
import com.eucalyptus.component.Lifecycles.State;
import com.eucalyptus.util.SimpleTransition;

public class BootstrapTransition<O> extends SimpleTransition<O, Lifecycles.State> {

  public BootstrapTransition( String name, State oldState, State newState ) {
    super( name, oldState, newState );
  }

  @Override
  protected void commit( O component ) throws Exception {}

}
