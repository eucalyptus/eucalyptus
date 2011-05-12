package com.eucalyptus.component;

import com.eucalyptus.event.Event;

public class AbstractLifecycleEvent implements LifecycleEvent {
  private final ServiceConfiguration configuration;
  private final LifecycleEvent.Type  lifecycleEventType;
  
  public AbstractLifecycleEvent( LifecycleEvent.Type lifecycleEventType, ServiceConfiguration configuration ) {
    this.configuration = configuration;
    this.lifecycleEventType = lifecycleEventType;
  }
  
  public ServiceConfiguration getReference( ) {
    return this.configuration;
  }
  
  @Override
  public String toString( ) {
    return String.format( "%s:%s", this.getClass( ).getSimpleName( ), configuration );
  }
  
  @Override
  public Type getLifecycleEventType( ) {
    return this.lifecycleEventType;
  }
  
}
