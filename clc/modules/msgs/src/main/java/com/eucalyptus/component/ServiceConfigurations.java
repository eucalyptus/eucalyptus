package com.eucalyptus.component;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.NoSuchElementException;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.empyrean.ServiceInfoType;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;

public class ServiceConfigurations {
  private static Logger                       LOG       = Logger.getLogger( ServiceConfigurations.class );
  private static ServiceConfigurationProvider singleton = new DatabaseServiceConfigurationProvider( );
  
  @TypeMapper
  enum ServiceInfoToServiceConfiguration implements Function<ServiceInfoType, ServiceConfiguration> {
    INSTANCE;
    
    @Override
    public ServiceConfiguration apply( ServiceInfoType arg0 ) {
      Component comp = Components.lookup( arg0.getType( ) );
      ServiceConfiguration config;
      try {
        config = comp.lookupServiceConfiguration( arg0.getName( ) );
      } catch ( NoSuchElementException ex1 ) {
        ServiceBuilder<? extends ServiceConfiguration> builder = comp.getBuilder( );
        try {
          URI uri = new URI( arg0.getUris( ).get( 0 ) );
          config = builder.newInstance( arg0.getPartition( ), arg0.getName( ), uri.getHost( ), uri.getPort( ) );
          comp.loadService( config );
        } catch ( URISyntaxException ex ) {
          LOG.error( ex, ex );
          throw new UndeclaredThrowableException( ex );
        } catch ( ServiceRegistrationException ex ) {
          LOG.error( ex, ex );
          throw new UndeclaredThrowableException( ex );
        }
      }
      return config;
    }
    
  }
  
  @TypeMapper
  enum ServiceConfigurationToServiceInfo implements Function<ServiceConfiguration, ServiceInfoType> {
    INSTANCE;
    
    @Override
    public ServiceInfoType apply( final ServiceConfiguration arg0 ) {
      return new ServiceInfoType( ) {
        {
          setPartition( arg0.getPartition( ) );
          setName( arg0.getName( ) );
          setType( arg0.getComponentId( ).name( ) );
          getUris( ).add( arg0.getUri( ).toASCIIString( ) );
        }
      };;
    }
    
  }
  
  public static ServiceConfigurationProvider getInstance( ) {
    return singleton;
  }
  
  public static ServiceConfiguration createEphemeral( ComponentId compId, String partition, String name, URI remoteUri ) {
    return new EphemeralConfiguration( compId, partition, name, remoteUri );
  }
  
  public static ServiceConfiguration createEphemeral( ComponentId compId, InetAddress host ) {
    return new EphemeralConfiguration( compId, compId.getPartition( ), host.getHostAddress( ), compId.makeInternalRemoteUri( host.getHostAddress( ),
                                                                                                                             compId.getPort( ) ) );
  }
  
  public static ServiceConfiguration createEphemeral( Component component, InetAddress host ) {
    return createEphemeral( component.getComponentId( ), host );
  }
  
  public static <T extends ServiceConfiguration> List<T> getConfigurations( Class<T> type ) throws PersistenceException {
    if ( !ComponentConfiguration.class.isAssignableFrom( type ) ) {
      throw new PersistenceException( "Unknown configuration type passed: " + type.getCanonicalName( ) );
    }
    EntityWrapper<T> db = EntityWrapper.get( type );
    List<T> componentList;
    try {
      componentList = db.query( type.newInstance( ) );
      db.commit( );
      return componentList;
    } catch ( PersistenceException ex ) {
      LOG.error( ex, ex );
      db.rollback( );
      throw ex;
    } catch ( Throwable ex ) {
      LOG.error( ex, ex );
      db.rollback( );
      throw new PersistenceException( ex );
    }
  }
  
  public static <T extends ServiceConfiguration> List<T> getPartitionConfigurations( Class<T> type, String partition ) throws PersistenceException, NoSuchElementException {
    if ( !ComponentConfiguration.class.isAssignableFrom( type ) ) {
      throw new PersistenceException( "Unknown configuration type passed: " + type.getCanonicalName( ) );
    }
    EntityWrapper<T> db = EntityWrapper.get( type );
    List<T> componentList;
    try {
      T conf = type.newInstance( );
      conf.setPartition( partition );
      componentList = db.query( conf );
      if ( componentList.isEmpty( ) ) {
        throw new NoSuchElementException( "Failed to lookup registration for " + type.getSimpleName( ) + " in partition: " + partition );
      }
      db.commit( );
      return componentList;
    } catch ( NoSuchElementException ex ) {
      db.rollback( );
      throw ex;
    } catch ( PersistenceException ex ) {
      LOG.error( ex, ex );
      db.rollback( );
      throw ex;
    } catch ( Throwable ex ) {
      LOG.error( ex, ex );
      db.rollback( );
      throw new PersistenceException( ex );
    }
  }
  
  public static <T extends ServiceConfiguration> T getConfiguration( Class<T> type, String uniqueName ) throws PersistenceException, NoSuchElementException {
    if ( !ComponentConfiguration.class.isAssignableFrom( type ) ) {
      throw new PersistenceException( "Unknown configuration type passed: " + type.getCanonicalName( ) );
    }
    EntityWrapper<T> db = EntityWrapper.get( type );
    try {
      T conf = type.newInstance( );
      conf.setName( uniqueName );
      T configuration = db.getUnique( conf );
      db.commit( );
      return configuration;
    } catch ( EucalyptusCloudException ex ) {
      LOG.trace( ex );
      db.rollback( );
      throw new NoSuchElementException( ex.getMessage( ) );
    } catch ( PersistenceException ex ) {
      LOG.error( ex, ex );
      db.rollback( );
      throw ex;
    } catch ( Throwable ex ) {
      LOG.error( ex, ex );
      db.rollback( );
      throw new PersistenceException( ex );
    }
  }
  
}
