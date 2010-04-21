package com.eucalyptus.component.event;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Component;
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
    return new StartComponentEvent( new LocalConfiguration( config.getComponent( ), uri ), config.getComponent(), true );
  }
  public static StartComponentEvent getLocal( com.eucalyptus.bootstrap.Component c ) {
    return new StartComponentEvent( new LocalConfiguration( c, c.getLocalUri( ) ), c, true );
  }
  public static StartComponentEvent getLocal( Component c ) {
    return new StartComponentEvent( new LocalConfiguration( c.getPeer( ), c.getConfiguration( ).getLocalUri( ) ), c.getPeer( ), true );
  }
  public static StartComponentEvent getRemote( ServiceConfiguration config ) {
    return new StartComponentEvent( config, config.getComponent( ), false );
  }
  
  private StartComponentEvent( ServiceConfiguration configuration, com.eucalyptus.bootstrap.Component component, boolean local ) {
    super( configuration, component.name( ), local );
  }
  @Override
  public String toString( ) {
    return "Start" + super.toString( );
  }
  
  

  
}
