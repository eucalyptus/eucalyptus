package com.eucalyptus.component.event;

import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.event.Event;

public class LifecycleEvent extends Event {
  private ServiceConfiguration configuration;
  private String                 name;
  private boolean                local;
  public LifecycleEvent( ServiceConfiguration configuration, String name, boolean local ) {
    this.configuration = configuration;
    this.name = name;
    this.local = local;
  }

  public ServiceConfiguration getConfiguration( ) {
    return configuration;
  }
  
  public com.eucalyptus.bootstrap.Component getPeer( ) {
    return this.getConfiguration( ).getComponent( );
  }

  public boolean isLocal( ) {
    return local;
  }
  
  @Override
  public String toString( ) {
    return String.format( "LifecycleEvent [name=%s, configuration=%s, local=%s, getCause()=%s, getFail()=%s, isVetoed()=%s]", name, configuration, local,
                          getCause( ), getFail( ), isVetoed( ) );
  }

}
