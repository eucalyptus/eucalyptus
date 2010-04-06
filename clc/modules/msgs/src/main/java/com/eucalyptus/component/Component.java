package com.eucalyptus.component;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import org.apache.log4j.Logger;
import org.mule.config.ConfigResource;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Nameable;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.EventRecord;

public class Component implements ComponentInformation, Nameable<Component> {
  private static Logger                            LOG             = Logger.getLogger( Component.class );
  public static String                             DISABLE_PATTERN = "euca.disable.%s";
  public static String                             REMOTE_PATTERN  = "euca.remote.%s";
  private final String                             name;
  private final com.eucalyptus.bootstrap.Component component;
  private final Configuration                      configuration;
  private ServiceBuilder<ServiceConfiguration>     builder;
  private Lifecycle                                lifecycle;
  private Boolean                                  enabled;
  private Boolean                                  local;
  private final Boolean                            singleton;
  private Map<String, Service>                     services        = Maps.newConcurrentHashMap( );
  
  Component( String name, URI configFile ) throws ServiceRegistrationException {
    this.name = name;
    boolean enabled = false, local = false;
    if ( System.getProperty( String.format( DISABLE_PATTERN, this.name ) ) == null ) {
      enabled = true;
      if ( System.getProperty( String.format( REMOTE_PATTERN, this.name ) ) == null ) {
        local = true;
      }
    }
    this.enabled = enabled;
    this.local = local;
    this.component = initComponent( );
    this.singleton = this.component.isSingleton( );
    if ( configFile != null ) {
      this.configuration = new Configuration( this, configFile );
      Components.register( this.configuration );
    } else {
      this.configuration = null;
    }
    this.lifecycle = new Lifecycle( this );
    Components.register( this.lifecycle );
    this.builder = new DefaultServiceBuilder( this );
  }
  
  public void removeService( ServiceConfiguration config ) throws ServiceRegistrationException {
    this.enabled = false;
    Service service = this.services.remove( config.getName( ) );
    Components.deregister( service );
    this.builder.fireStop( config );
    EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE, this.getName( ), service.getName( ), service.getUri( ).toString( ) ).info( );
  }
  
  public Service buildService( String hostName ) throws ServiceRegistrationException {
    return this.buildService( hostName, this.getConfiguration( ).getDefaultPort( ) );
  }
  
  public Service buildService( String hostName, Integer port ) throws ServiceRegistrationException {
    this.enabled = true;
    ServiceConfiguration config = this.builder.add( name, hostName, port );
    Service service = new Service( this, config.getHostName( ), config.getPort( ) );
    return setupService( config, service );
  }
  
  public Service buildService( URI uri ) throws ServiceRegistrationException {
    this.enabled = true;
    ServiceConfiguration config = this.builder.add( uri );
    Service service = null;
    if ( config.isLocal( ) ) {
      service = new Service( this );
    } else {
      service = new Service( this, config.getHostName( ), config.getPort( ) );
    }
    return this.setupService( config, service );
  }
  
  public Service buildService( ServiceConfiguration config ) throws ServiceRegistrationException {
    this.enabled = true;
    Service service = new Service( this, config.getHostName( ), config.getPort( ) );
    return this.setupService( config, service );
  }
  
  public Service buildLocalService( ) throws ServiceRegistrationException {
    this.enabled = true;
    ServiceConfiguration conf = this.builder.add( this.getConfiguration( ).getLocalUri( ) );
    Service service = new Service( this );
    return this.setupService( conf, service );
  }
  
  private Service setupService( ServiceConfiguration config, Service service ) throws ServiceRegistrationException {
    this.services.put( service.getName( ), service );
    Components.register( service );
    this.builder.fireStart( config );
    EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE, this.getName( ), service.getName( ), service.getUri( ).toString( ) ).info( );
    return service;
  }
  
  public Boolean isSingleton( ) {
    return this.singleton;
  }
  
  private com.eucalyptus.bootstrap.Component initComponent( ) {
    try {
      com.eucalyptus.bootstrap.Component component = com.eucalyptus.bootstrap.Component.valueOf( name );
      if ( component == null ) {
        throw BootstrapException.throwError( "Error loading component.  Failed to find component named '" + name );
      }
      return component;
    } catch ( Exception e ) {
      throw BootstrapException.throwError( "Error loading component.  Failed to find component named '" + name, e );
    }
  }
  
  public com.eucalyptus.bootstrap.Component getPeer( ) {
    return this.component;
  }
  
  public String getName( ) {
    return this.name;
  }
  
  public String toString( ) {
    EventRecord rec = EventRecord.caller( Component.class, EventType.COMPONENT_INFO, this.getName( ), "enabled", this.isEnabled( ), "local", this.isLocal( ),
                                          "state", this.getLifecycle( ).getState( ) );
    for ( ConfigResource cfg : this.getConfiguration( ).getResource( ).getConfigurations( ) ) {
      rec.next( ).append( ConfigResource.class, EventType.COMPONENT_INFO, this.getName( ), "->" + cfg.getUrl( ) );
    }
    for ( Bootstrapper b : this.configuration.getBootstrappers( ) ) {
      rec.next( ).append( Bootstrapper.class, EventType.COMPONENT_INFO, this.getName( ), "->" + b.getClass( ).getSimpleName( ) );
    }
    return rec.toString( );
  }
  
  public Configuration getConfiguration( ) {
    return this.configuration;
  }
  
  public ServiceBuilder<ServiceConfiguration> getBuilder( ) {
    return this.builder;
  }
  
  public void markRemote( ) {
    this.local = false;
  }
  
  public void markDisabled( ) {
    this.local = false;
    this.enabled = false;
  }
  
  public Boolean isEnabled( ) {
    return this.enabled;
  }
  
  public Boolean isLocal( ) {
    return this.local;
  }
  
  @Override
  public int compareTo( Component that ) {
    return this.getName( ).compareTo( that.getName( ) );
  }
  
  public List<ServiceConfiguration> list( ) throws ServiceRegistrationException {
    return this.builder.list( );
  }
  
  public URI getUri( String hostName, Integer port ) {
    return this.getConfiguration( ).makeUri( hostName, port );
  }
  
  public void setBuilder( ServiceBuilder<ServiceConfiguration> builder ) {
    this.builder = builder;
  }
  
  public Lifecycle getLifecycle( ) {
    return this.lifecycle;
  }
  
  public Boolean isInitialized( ) {
    return Lifecycles.State.INITIALIZED.equals( this.getLifecycle( ).getState( ) );
  }
  
  public NavigableSet<Service> getServices( ) {
    return Sets.newTreeSet( this.services.values( ) );
  }
  
}
