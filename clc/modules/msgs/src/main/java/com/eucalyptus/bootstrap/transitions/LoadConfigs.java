package com.eucalyptus.bootstrap.transitions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Lifecycles;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.records.EventRecord;

public class LoadConfigs extends BootstrapTransition<Bootstrap.Stage> {
  private static Logger LOG = Logger.getLogger( LoadConfigs.class );
  
  public LoadConfigs( ) {
    super( "load-configurations", Lifecycles.State.DISABLED, Lifecycles.State.PRIMORDIAL );
  }
  
  @Override
  public void commit( Bootstrap.Stage stage ) {
    Enumeration<URL> p1;
    URI u = null;
    try {
      p1 = Thread.currentThread( ).getContextClassLoader( ).getResources( stage.getResourceName( ) );
      if ( !p1.hasMoreElements( ) ) return;
      LOG.info( LogUtil.header( "Initializing component resources Bootstrap.Stage: " + stage.name( ) ) );
      while ( p1.hasMoreElements( ) ) {
        u = p1.nextElement( ).toURI( );
        EventRecord.here( Bootstrap.class, EventType.BOOTSTRAP_INIT_RESOURCES, stage.name( ), u.toString( ) ).info( );
        Properties props = new Properties( );
        props.load( u.toURL( ).openStream( ) );
        String name = props.getProperty( "name" );
        EventRecord.here( Bootstrap.class, EventType.BOOTSTRAP_INIT_CONFIGURATION, name ).info( );
        if ( Components.contains( name ) ) {
          throw BootstrapException.throwFatal( "Duplicate component definition in: " + u.toASCIIString( ) );
        } else {
          try {
            Components.create( name, u );
            LOG.debug( "Loaded " + name + " from " + u );
          } catch ( ServiceRegistrationException e ) {
            LOG.debug( e, e );
            throw BootstrapException.throwFatal( "Error in component bootstrap: " + e.getMessage( ), e );
          }
        }
        EventRecord.here( Bootstrap.class, EventType.BOOTSTRAP_INIT_COMPONENT, name ).info( );
      }
    } catch ( IOException e ) {
      LOG.error( e, e );
      throw BootstrapException.throwFatal( "Failed to load component resources from: " + u, e );
    } catch ( URISyntaxException e ) {
      LOG.error( e, e );
      throw BootstrapException.throwFatal( "Failed to load component resources from: " + u, e );
    }
  }
  
}
