package com.eucalyptus.event;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.config.LocalConfiguration;

public class StopComponentEvent extends ComponentEvent{
  public static StopComponentEvent getLocal( Component c ) {
    return new StopComponentEvent( new LocalConfiguration(c), c, true );
  }
  public static StopComponentEvent getRemote( ComponentConfiguration config ) {
    return new StopComponentEvent( config, config.getComponent( ), false );
  }

  private StopComponentEvent( ComponentConfiguration configuration, Component component, boolean local ) {
    super( configuration, component, local );
  }
  
}
