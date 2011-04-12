package com.eucalyptus.component;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;

public class ServiceConfigurations {
  private static Logger                       LOG       = Logger.getLogger( ServiceConfigurations.class );
  private static ServiceConfigurationProvider singleton = new DatabaseServiceConfigurationProvider( );
  
  public static ServiceConfigurationProvider getInstance( ) {
    return singleton;
  }


  public static ServiceConfiguration createEphemeral( ComponentId compId, String partition, String name, URI remoteUri ) {
    return new EphemeralConfiguration( compId, partition, name, remoteUri );
  }

  public static ServiceConfiguration createEphemeral( Component component, InetAddress host ) {
    ComponentId compId = component.getComponentId( );
    return new EphemeralConfiguration( compId, compId.getPartition( ), host.getCanonicalHostName( ), compId.makeRemoteUri( host.getCanonicalHostName( ),
                                                                                                                           compId.getPort( ) ) );
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

  static class EphemeralConfiguration extends ComponentConfiguration {
    URI         uri;
    ComponentId c;
    
    public EphemeralConfiguration( ComponentId c, String partition, String name, URI uri ) {
      super( partition, name, uri.getHost( ), uri.getPort( ), uri.getPath( ) );
      this.uri = uri;
      this.c = c;
    }
    
    public ComponentId lookupComponentId( ) {
      return c;
    }
    
    public String getUri( ) {
      return this.uri.toASCIIString( );
    }

    @Override
    public String getName( ) {
      return super.getName( );
    }

    @Override
    public Boolean isLocal( ) {
      return super.isLocal( );
    }

    @Override
    public int compareTo( ServiceConfiguration that ) {
      return super.compareTo( that );
    }

    @Override
    public String toString( ) {
      return super.toString( );
    }

    @Override
    public int hashCode( ) {
      return super.hashCode( );
    }

    @Override
    public boolean equals( Object that ) {
      return super.equals( that );
    }

    @Override
    public String getPartition( ) {
      return super.getPartition( );
    }

    @Override
    public void setPartition( String partition ) {
      super.setPartition( partition );
    }

    @Override
    public String getHostName( ) {
      return super.getHostName( );
    }

    @Override
    public void setHostName( String hostName ) {
      super.setHostName( hostName );
    }

    @Override
    public Integer getPort( ) {
      return super.getPort( );
    }

    @Override
    public void setPort( Integer port ) {
      super.setPort( port );
    }

    @Override
    public String getServicePath( ) {
      return super.getServicePath( );
    }

    @Override
    public void setServicePath( String servicePath ) {
      super.setServicePath( servicePath );
    }

    @Override
    public void setName( String name ) {
      super.setName( name );
    }
    
  }
  
}
