package com.eucalyptus.event;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.config.LocalConfiguration;

public class StartComponentEvent extends ComponentEvent {
  private static Logger LOG = Logger.getLogger( StartComponentEvent.class );
  public static StartComponentEvent getLocal( ComponentConfiguration config ) {
    URI uri = null;
    try {
      uri = new URI( config.getUri( ) );
    } catch ( URISyntaxException e ) {
      LOG.fatal( "Failed to construct a valid URL from a component configuration", e );
    }
    return new StartComponentEvent( new LocalConfiguration( config.getComponent( ), uri ), config.getComponent(), true );
  }
  public static StartComponentEvent getLocal( Component c ) {
    return new StartComponentEvent( new LocalConfiguration( c, c.getUri( ) ), c, true );
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
