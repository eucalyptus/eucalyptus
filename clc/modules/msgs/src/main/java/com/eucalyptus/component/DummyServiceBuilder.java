package com.eucalyptus.component;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Formerly known as {@link DefaultServiceBuilder}
 */
public class DummyServiceBuilder implements ServiceBuilder<ServiceConfiguration> {
  private Map<String, ServiceConfiguration> services = Maps.newConcurrentMap( );
  private final Component                   component;
  
  DummyServiceBuilder( Component component ) {
    this.component = component;
  }
  
  @Override
  public Boolean checkRemove( String partition, String name ) throws ServiceRegistrationException {
    return this.services.containsKey( name );
  }
  
  @Override
  public Component getComponent( ) {
    return this.component;
  }
  
  @Override
  public List<ServiceConfiguration> list( ) throws ServiceRegistrationException {
    return Lists.newArrayList( this.getComponent( ).lookupServiceConfigurations( ) );
  }
  
  @Override
  public ServiceConfiguration add( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    throw new RuntimeException( "Not supported." );
  }
  
  @Override
  public Boolean checkAdd( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    return !this.services.containsKey( name );
  }
  
  @Override
  public ServiceConfiguration lookupByHost( String name ) throws ServiceRegistrationException {
    throw new RuntimeException( "Not supported." );
  }
  
  @Override
  public ServiceConfiguration lookupByName( String name ) throws ServiceRegistrationException {
    try {
      return this.getComponent( ).lookupService( name ).getServiceConfiguration( );
    } catch ( NoSuchElementException ex ) {
      throw new ServiceRegistrationException( ex );
    }
  }
  
  @Override
  public ServiceConfiguration remove( ServiceConfiguration config ) throws ServiceRegistrationException {
    return config;
  }
  
  @Override
  public ServiceConfiguration lookup( String partition, String name ) throws ServiceRegistrationException {
    Service service;
    try {
      service = this.getComponent( ).lookupService( name );
    } catch ( NoSuchElementException ex ) {
      throw new ServiceRegistrationException( ex );
    }
    if ( service.getPartition( ).equals( partition ) ) {
      return service.getServiceConfiguration( );
    } else {
      throw new ServiceRegistrationException( "No service found matching partition: " + partition + " and name: " + name + " for component: "
                                              + this.getComponent( ).getName( ) );
    }
  }
  
  @Override
  public ServiceConfiguration newInstance( String partition, String name, String host, Integer port ) {
    ComponentId compId = this.getComponent( ).getComponentId( );
    return ServiceConfigurations.createEphemeral( compId, compId.getPartition( ), compId.name( ), compId.makeRemoteUri( host, port ) );
  }
  
  @Override
  public ServiceConfiguration newInstance( ) {
    ComponentId compId = this.getComponent( ).getComponentId( );
    return ServiceConfigurations.createEphemeral( compId, compId.getPartition( ), compId.name( ), compId.getLocalEndpointUri( ) );
  }
  
  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.here( ServiceBuilder.class, EventType.COMPONENT_SERVICE_START, config.getFullName( ).toString( ), config.toString( ) ).debug( );
  }
  
  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.here( ServiceBuilder.class, EventType.COMPONENT_SERVICE_STOP, config.getFullName( ).toString( ), config.toString( ) ).debug( );
  }
  
  @Override
  public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.here( ServiceBuilder.class, EventType.COMPONENT_SERVICE_ENABLE, config.getFullName( ).toString( ), config.toString( ) ).debug( );
  }
  
  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.here( ServiceBuilder.class, EventType.COMPONENT_SERVICE_DISABLE, config.getFullName( ).toString( ), config.toString( ) ).debug( );
  }
  
  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.here( ServiceBuilder.class, EventType.COMPONENT_SERVICE_CHECK, config.getFullName( ).toString( ), config.toString( ) ).debug( );
  }
}
