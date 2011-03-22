package com.eucalyptus.component;

import java.net.URI;
import java.util.List;
import java.util.Map;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Formerly known as {@link DefaultServiceBuilder}
 */
public class DummyServiceBuilder extends AbstractServiceBuilder<ServiceConfiguration> {
  private Component                         component;
  private Map<String, ServiceConfiguration> services = Maps.newConcurrentMap( );
  
  public DummyServiceBuilder( Component component ) {
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
    return Lists.newArrayList( this.services.values( ) );
  }
  
  @Override
  public ServiceConfiguration add( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    throw new RuntimeException( "Not implemented yet." );
  }

  @Override
  public ServiceConfiguration toConfiguration( URI uri ) throws ServiceRegistrationException {
    return ServiceConfigurations.uriToServiceConfiguration( this.component, uri );
  }

  @Override
  public Boolean checkAdd( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    return !this.services.containsKey( name );
  }
  
  @Override
  public ServiceConfiguration lookupByHost( String name ) throws ServiceRegistrationException {
    throw new RuntimeException( "Not implemented yet." );
  }
  
  @Override
  public ServiceConfiguration lookupByName( String name ) throws ServiceRegistrationException {
    return this.services.get( name );
  }
  
  @Override
  public ServiceConfiguration remove( ServiceConfiguration config ) throws ServiceRegistrationException {
    return this.services.remove( config.getName( ) );
  }

  @Override
  public ServiceConfiguration lookup( String partition, String name ) throws ServiceRegistrationException {
    return this.services.get( name );
  }
    
}
