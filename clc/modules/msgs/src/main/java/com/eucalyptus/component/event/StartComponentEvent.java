package com.eucalyptus.component.event;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.log4j.Logger;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.config.LocalConfiguration;

public class StartComponentEvent extends LifecycleEvent {
  private static Logger LOG = Logger.getLogger( StartComponentEvent.class );
  public static StartComponentEvent getLocal( ServiceConfiguration config ) {
    URI uri = null;
    try {
      uri = new URI( config.getUri( ) );
    } catch ( URISyntaxException e ) {
      LOG.fatal( "Failed to construct a valid URL from a component configuration", e );
    }
    return new StartComponentEvent( new LocalConfiguration( config.getPartition( ), config.getComponentId( ), uri ), config.getComponentId(), true );
  }
  public static StartComponentEvent getRemote( ServiceConfiguration config ) {
    return new StartComponentEvent( config, config.getComponentId( ), false );
  }
  
  private StartComponentEvent( ServiceConfiguration configuration, ComponentId componentId, boolean local ) {
    super( configuration, componentId, local );
  }
  @Override
  public String toString( ) {
    return "Start" + super.toString( );
  }
  
  

  
}
