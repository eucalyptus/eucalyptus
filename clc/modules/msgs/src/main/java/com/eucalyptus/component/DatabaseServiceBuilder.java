package com.eucalyptus.component;

import java.net.URI;
import java.util.List;
import com.eucalyptus.config.LocalConfiguration;
import com.eucalyptus.config.RemoteConfiguration;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.NetworkUtil;

public abstract class DatabaseServiceBuilder<T extends ServiceConfiguration> extends AbstractServiceBuilder<T> {
  
  protected abstract T newInstance( );
  
  protected abstract T newInstance( String name, String host, Integer port );
  
  @Override
  public List<T> list( ) throws ServiceRegistrationException {
    return ServiceConfigurations.getInstance( ).list( this.newInstance( ) );
  }
  
  @Override
  public T lookupByName( String name ) throws ServiceRegistrationException {
    T conf = this.newInstance( );
    conf.setName( name );
    return ( T ) ServiceConfigurations.getInstance( ).lookup( conf );
  }
  
  @Override
  public T lookupByHost( String hostName ) throws ServiceRegistrationException {
    T conf = this.newInstance( );
    conf.setHostName( hostName );
    return ( T ) ServiceConfigurations.getInstance( ).lookup( conf );
  }
  
  @Override
  public Boolean checkAdd( String name, String host, Integer port ) throws ServiceRegistrationException {
    try {
      if ( !NetworkUtil.testGoodAddress( host ) ) {
        throw new EucalyptusCloudException( "Components cannot be registered using local, link-local, or multicast addresses." );
      } else if ( NetworkUtil.testLocal( host ) && !this.getComponent( ).isLocal( ) ) {
        throw new EucalyptusCloudException( "You do not have a local " + this.newInstance( ).getClass( ).getSimpleName( ).replaceAll( "Configuration", "" )
                                            + " enabled (or it is not installed)." );
      }
    } catch ( EucalyptusCloudException e ) {
      throw new ServiceRegistrationException( e.getMessage( ), e );
    } catch ( Exception e ) {
      throw new ServiceRegistrationException( "Service registration failed: " + e.getMessage( ), e );
    }
    try {
      ServiceConfiguration existingName = this.lookupByHost( name );
      throw new EucalyptusCloudException( "Component with name=" + name + " already exists at host=" + existingName.getHostName( ) );      
    } catch( EucalyptusCloudException e ) {
      try {
        ServiceConfiguration existingHost = this.lookupByHost( host );
        throw new EucalyptusCloudException( "Component with host=" + name + " already exists with name=" + existingHost.getHostName( ) );
      } catch( EucalyptusCloudException e1 ) {
      } catch ( Exception e1 ) {
        throw new ServiceRegistrationException( e1 );
      }      
    } catch ( Exception e ) {
      throw new ServiceRegistrationException( e );
    }
    return true;
  }

  @Override
  public T add( String name, String host, Integer port ) throws ServiceRegistrationException {
    T config = this.newInstance( name, host, port );
    ServiceConfigurations.getInstance( ).store( config );
    return config;
  }

  @Override
  public ServiceConfiguration toConfiguration( URI uri ) throws ServiceRegistrationException {
    try {
      if( "vm".equals( uri.getScheme( ) ) || NetworkUtil.testLocal( uri.getHost( ) ) ) {
        return new LocalConfiguration( this.getComponent( ).getPeer( ), uri );      
      } else {
        return new RemoteConfiguration( this.getComponent( ).getPeer( ), uri );
      }
    } catch ( Exception e ) {
      return new LocalConfiguration( this.getComponent( ).getPeer( ), uri );
    }
  }

  @Override
  public T remove( ServiceConfiguration config ) throws ServiceRegistrationException {
    T removeConf = this.lookupByName( config.getName( ) );
    ServiceConfigurations.getInstance( ).remove( removeConf );
    return removeConf;
  }
  
}
