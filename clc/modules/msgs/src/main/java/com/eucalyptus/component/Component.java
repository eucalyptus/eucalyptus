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
import com.eucalyptus.records.Record;
import com.eucalyptus.util.Nameable;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.eucalyptus.records.EventRecord;

/**
 * @author decker
 *
 */
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
  
  public void removeService( final ServiceConfiguration config ) throws ServiceRegistrationException {
    this.enabled = false;
    Service remove = Iterables.find( this.services.values(), new Predicate<Service>() {
      @Override
      public boolean apply( Service arg0 ) {
        return arg0.getHost( ).equals( config.getHostName( ) );
      }} );
    Service service = this.services.remove( remove.getName( ) );
    Components.deregister( service );
    EventRecord.caller( Component.class, config.isLocal( ) ? EventType.COMPONENT_SERVICE_STOP : EventType.COMPONENT_SERVICE_STOP_REMOTE, this.getName( ),
                        service.getName( ), service.getUri( ).toString( ) ).info( );
  }
  
  /**
   * Builds a Service instance for this component using a service configuration created with the specified URI.
   * @return
   * @throws ServiceRegistrationException
   */
  public Service buildService( URI uri ) throws ServiceRegistrationException {
    this.enabled = true;
    ServiceConfiguration config = this.builder.add( uri );
    Service service = new Service( this, config );
    return this.setupService( config, service );
  }
  
  /**
   * Builds a Service instance for this component using the provided service configuration.
   * @return
   * @throws ServiceRegistrationException
   */
  public Service buildService( ServiceConfiguration config ) throws ServiceRegistrationException {
    this.enabled = true;
    Service service = new Service( this, config );
    return this.setupService( config, service );
  }
  
  
  /**
   * Builds a Service instance for this component using the local default values.
   * @return
   * @throws ServiceRegistrationException
   */
  public Service buildService( ) throws ServiceRegistrationException {
    this.enabled = true;
    ServiceConfiguration conf = this.builder.add( this.getConfiguration( ).getLocalUri( ) );
    Service service = new Service( this, conf );
    return this.setupService( conf, service );
  }
  
  private Service setupService( ServiceConfiguration config, Service service ) throws ServiceRegistrationException {
    this.services.put( service.getName( ), service );
    Components.register( service );
    EventRecord.caller( Component.class, config.isLocal( ) ? EventType.COMPONENT_SERVICE_INIT : EventType.COMPONENT_SERVICE_INIT_REMOTE, this.getName( ),
                        service.getName( ), service.getUri( ).toString( ) ).info( );
    return service;
  }
  
  public void startService( ServiceConfiguration service ) {
    try {
      if ( service.isLocal( ) ) {
        EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_START, this.getName( ), service.getName( ), service.getUri( ).toString( ) ).info( );
        this.builder.fireStart( service );
      } else {
        EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_START_REMOTE, this.getName( ), service.getName( ), service.getUri( ).toString( ) )
                   .info( );
      }
    } catch ( ServiceRegistrationException e ) {
      LOG.debug( e, e );
    }
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
    Record rec = EventRecord.caller( Component.class, EventType.COMPONENT_INFO, this.getName( ), "enabled", this.isEnabled( ), "local", this.isLocal( ),
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
