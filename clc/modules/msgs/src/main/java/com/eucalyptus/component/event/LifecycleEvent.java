package com.eucalyptus.component.event;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.event.Event;

public class LifecycleEvent implements Event {
  private ServiceConfiguration configuration;
  private ComponentId            identity;
  private boolean                local;
  public LifecycleEvent( ServiceConfiguration configuration, ComponentId compId, boolean local ) {
    this.configuration = configuration;
    this.identity = compId;
    this.local = local;
  }

  public ServiceConfiguration getConfiguration( ) {
    return configuration;
  }
  
  public ComponentId getIdentity( ) {
    return this.identity;
  }

  public boolean isLocal( ) {
    return local;
  }
  
  @Override
  public String toString( ) {
    return String.format( "LifecycleEvent [componentId=%s, configuration=%s, local=%s]", identity, configuration, local );
  }

}
