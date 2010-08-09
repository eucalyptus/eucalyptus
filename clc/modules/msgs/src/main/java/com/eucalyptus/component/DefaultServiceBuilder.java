package com.eucalyptus.component;

import java.net.URI;
import java.util.List;
import java.util.Map;
import com.eucalyptus.config.LocalConfiguration;
import com.eucalyptus.config.RemoteConfiguration;
import com.eucalyptus.util.NetworkUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class DefaultServiceBuilder extends AbstractServiceBuilder<ServiceConfiguration> {
  private Component                         component;
  private Map<String, ServiceConfiguration> services = Maps.newConcurrentHashMap( );
  
  public DefaultServiceBuilder( Component component ) {
    this.component = component;
  }
  
  @Override
  public Boolean checkRemove( String name ) throws ServiceRegistrationException {
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
  public ServiceConfiguration add( String name, String host, Integer port ) throws ServiceRegistrationException {
    throw new RuntimeException( "Not implemented yet." );
  }

  @Override
  public ServiceConfiguration toConfiguration( URI uri ) throws ServiceRegistrationException {
    try {
      if( uri.getScheme( ).matches( ".*vm.*" ) || ( uri.getHost( ) != null && NetworkUtil.testLocal( uri.getHost( ) ) ) ) {
        return new LocalConfiguration( this.component.getPeer( ), uri );      
      } else {
        return new RemoteConfiguration( this.component.getPeer( ), uri );      
      }
    } catch ( Throwable t ) {
      return new LocalConfiguration( this.component.getPeer( ), uri );      
    }
  }

  @Override
  public Boolean checkAdd( String name, String host, Integer port ) throws ServiceRegistrationException {
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
    
}
