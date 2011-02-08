package com.eucalyptus.component.event;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.log4j.Logger;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.config.LocalConfiguration;

public class StopComponentEvent extends LifecycleEvent{
  private static Logger LOG = Logger.getLogger( StopComponentEvent.class );
  public static StopComponentEvent getLocal( ServiceConfiguration config ) {
    URI uri = null;
    try {
      uri = new URI( config.getUri( ) );
    } catch ( URISyntaxException e ) {
      LOG.fatal( "Failed to construct a valid URL from a component configuration", e );
    }
    return new StopComponentEvent( new LocalConfiguration( null, config.getComponentId( ), uri ), config.getComponentId(), true );
  }
  public static StopComponentEvent getRemote( ServiceConfiguration  config ) {
    return new StopComponentEvent( config, config.getComponentId( ), false );
  }

  private StopComponentEvent( ServiceConfiguration configuration, ComponentId componentId, boolean local ) {
    super( configuration, componentId, local );
  }
  
}
