package com.eucalyptus.component;

import com.eucalyptus.component.Lifecycles.State;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Transition;
import com.eucalyptus.records.EventRecord;

public class DefaultTransition extends Transition<Component, Lifecycles.State> {
  
  public DefaultTransition( String name, State oldState, State newState ) {
    super( name, oldState, newState );
  }
  
  @Override
  protected void commit( Component component ) throws Exception {}
  
  @Override
  protected void post( Component component ) throws Exception {}
  
  @Override
  protected void prepare( Component component ) throws Exception {}
  
  @Override
  protected void rollback( Component component ) {}
    
}
