package com.eucalyptus.event;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.config.LocalConfiguration;

public class StartComponentEvent extends ComponentEvent {

  public static StartComponentEvent getLocal( Component c ) {
    return new StartComponentEvent( new LocalConfiguration(c, c.name( )), c, true );
  }
  public static StartComponentEvent getRemote( ComponentConfiguration config ) {
    return new StartComponentEvent( config, config.getComponent( ), false );
  }
  
  private StartComponentEvent( ComponentConfiguration configuration, Component component, boolean local ) {
    super( configuration, component, local );
  }
  @Override
  public String toString( ) {
    return String.format( "StartComponentEvent [component=%s, configuration=%s, local=%s]", component, configuration, local );
  }
  
  

  
}
