package com.eucalyptus.component.event;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.event.Event;

public class AbstractLifecycleEvent implements LifecycleEvent {
  private final ServiceConfiguration configuration;
  
  public AbstractLifecycleEvent( ServiceConfiguration configuration ) {
    this.configuration = configuration;
  }
  
  public ServiceConfiguration getReference( ) {
    return this.configuration;
  }
  
  @Override
  public String toString( ) {
    return String.format( "%s:%s", this.getClass( ).getSimpleName( ), configuration );
  }
  
}
