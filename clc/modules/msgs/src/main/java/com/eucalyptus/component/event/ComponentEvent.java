package com.eucalyptus.component.event;

import com.eucalyptus.component.Component;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.event.Event;

public class ComponentEvent extends Event {
  private ServiceConfiguration configuration;
  private String                 name;
  private boolean                local;
  
  public ComponentEvent( ServiceConfiguration configuration, String name, boolean local ) {
    super( );
    this.configuration = configuration;
    this.name = name;
    this.local = local;
  }
  
  public ServiceConfiguration getConfiguration( ) {
    return configuration;
  }
  
  public com.eucalyptus.bootstrap.Component getComponent( ) {
    return this.getConfiguration( ).getComponent( );
  }

  public boolean isLocal( ) {
    return local;
  }
  
  @Override
  public String toString( ) {
    return String.format( "ComponentEvent [name=%s, configuration=%s, local=%s, getCause()=%s, getFail()=%s, isVetoed()=%s]", name, configuration, local,
                          getCause( ), getFail( ), isVetoed( ) );
  }
  
}
