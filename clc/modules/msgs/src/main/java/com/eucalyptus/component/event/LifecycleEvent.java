package com.eucalyptus.component.event;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.event.Event;

public class LifecycleEvent implements Event {
  private static final long serialVersionUID = 1L;
  private final ServiceConfiguration configuration;
  
  public LifecycleEvent( ServiceConfiguration configuration ) {
    this.configuration = configuration;
  }
  
  public ServiceConfiguration getConfiguration( ) {
    return this.configuration;
  }
  
  public ComponentId getIdentity( ) {
    return this.configuration.getComponentId( );
  }
  
  public boolean isLocal( ) {
    return this.configuration.isLocal( );
  }
  
  @Override
  public String toString( ) {
    return String.format( "%s:%s", this.getClass( ).getSimpleName( ), configuration );
  }
  
}
