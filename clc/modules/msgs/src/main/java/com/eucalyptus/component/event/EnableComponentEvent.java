package com.eucalyptus.component.event;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.log4j.Logger;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.config.LocalConfiguration;

public class EnableComponentEvent extends LifecycleEvent {
  private static Logger LOG = Logger.getLogger( EnableComponentEvent.class );
  public static EnableComponentEvent getLocal( ServiceConfiguration config ) {
    URI uri = null;
    try {
      uri = new URI( config.getUri( ) );
    } catch ( URISyntaxException e ) {
      LOG.fatal( "Failed to construct a valid URL from a component configuration", e );
    }
    return new EnableComponentEvent( new LocalConfiguration( config.getComponentId( ), config.getPartition( ), config.getName( ), uri ), config.getComponentId(), true );
  }
  public static EnableComponentEvent getRemote( ServiceConfiguration config ) {
    return new EnableComponentEvent( config, config.getComponentId( ), false );
  }
  
  private EnableComponentEvent( ServiceConfiguration configuration, ComponentId componentId, boolean local ) {
    super( configuration, componentId, local );
  }
  @Override
  public String toString( ) {
    return "Enable" + super.toString( );
  }
  
  

  
}
