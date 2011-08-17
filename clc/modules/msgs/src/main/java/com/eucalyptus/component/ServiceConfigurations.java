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
import com.eucalyptus.empyrean.ServiceId;
import com.eucalyptus.empyrean.ServiceStatusDetail;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;

public class ServiceConfigurations {
  static Logger LOG = Logger.getLogger( ServiceConfigurations.class );
  
  public interface Provider {
    public abstract <T extends ServiceConfiguration> List<T> list( T type );
    
    public abstract <T extends ServiceConfiguration> T store( T t );
    
    public abstract <T extends ServiceConfiguration> T remove( T t );
    
    public abstract <T extends ServiceConfiguration> T lookup( T type );
  }
  
  private enum DatabaseProvider implements Provider {
    INSTANCE;
    
    public <T extends ServiceConfiguration> List<T> list( T example ) {
      EntityWrapper<T> db = EntityWrapper.get( example );
      List<T> componentList;
      try {
        componentList = db.query( example );
        db.commit( );
        return componentList;
      } catch ( PersistenceException ex ) {
        LOG.trace( ex );
        db.rollback( );
        throw ex;
      } catch ( Throwable ex ) {
        LOG.trace( ex );
        db.rollback( );
        throw new PersistenceException( "Service configuration lookup failed for: " + LogUtil.dumpObject( example ), ex );
      }
    }
    
    @Override
    public <T extends ServiceConfiguration> T lookup( T example ) {
      EntityWrapper<T> db = EntityWrapper.get( example );
      T existingName = null;
      try {
        existingName = db.getUnique( example );
        db.rollback( );
        return existingName;
      } catch ( PersistenceException ex ) {
        LOG.trace( ex );
        db.rollback( );
        throw ex;
      } catch ( Throwable ex ) {
        LOG.trace( ex );
        db.rollback( );
        throw new PersistenceException( "Service configuration lookup failed for: " + LogUtil.dumpObject( example ), ex );
      }
    }
    
    @Override
    public <T extends ServiceConfiguration> T store( T config ) {
      EntityWrapper<T> db = EntityWrapper.get( config );
      try {
        db.add( config );
        config = db.getUnique( config );
        db.commit( );
        EventRecord.here( Provider.class, EventClass.COMPONENT, EventType.COMPONENT_REGISTERED, config.toString( ) ).info( );
      } catch ( PersistenceException ex ) {
        LOG.trace( ex );
        EventRecord.here( Provider.class, EventClass.COMPONENT, EventType.COMPONENT_REGISTERED, "FAILED", config.toString( ) ).error( );
        db.rollback( );
        throw ex;
      } catch ( Throwable ex ) {
        LOG.trace( ex );
        EventRecord.here( Provider.class, EventClass.COMPONENT, EventType.COMPONENT_REGISTERED, "FAILED", config.toString( ) ).error( );
        db.rollback( );
        throw new PersistenceException( "Service configuration storing failed for: " + LogUtil.dumpObject( config ), ex );
      }
      return config;
    }
    
    @Override
    public <T extends ServiceConfiguration> T remove( T config ) {
      EntityWrapper<T> db = EntityWrapper.get( config );
      try {
        T searchConfig = ( T ) config.getClass( ).newInstance( );
        searchConfig.setName( config.getName( ) );
        T exists = db.getUnique( searchConfig );
        db.delete( exists );
        db.commit( );
        EventRecord.here( Provider.class, EventClass.COMPONENT, EventType.COMPONENT_DEREGISTERED, config.toString( ) ).info( );
      } catch ( PersistenceException ex ) {
        LOG.trace( ex );
        EventRecord.here( Provider.class, EventClass.COMPONENT, EventType.COMPONENT_DEREGISTERED, "FAILED", config.toString( ) ).error( );
        db.rollback( );
        throw ex;
      } catch ( Throwable ex ) {
        LOG.trace( ex );
        EventRecord.here( Provider.class, EventClass.COMPONENT, EventType.COMPONENT_DEREGISTERED, "FAILED", config.toString( ) ).error( );
        db.rollback( );
        throw new PersistenceException( "Service configuration removal failed for: " + LogUtil.dumpObject( config ), ex );
      }
      return config;
    }
    
  }
  
  public static Function<ServiceConfiguration, ServiceStatusType> asServiceStatus( final boolean showEvents, final boolean showEventStacks ) {
    return new Function<ServiceConfiguration, ServiceStatusType>( ) {
      
      @Override
      public ServiceStatusType apply( final ServiceConfiguration config ) {
        return new ServiceStatusType( ) {
          {
            this.setServiceId( TypeMappers.transform( config, ServiceId.class ) );
            this.setLocalEpoch( Topology.epoch( ) );
            try {
              this.setLocalState( config.lookupState( ).toString( ) );
            } catch ( Exception ex ) {
              this.setLocalState( "n/a: " + ex.getMessage( ) );
            }
            if ( showEvents ) {
              this.getStatusDetails( ).addAll( Collections2.transform( config.lookupDetails( ),
                                                                       TypeMappers.lookup( ServiceCheckRecord.class, ServiceStatusDetail.class ) ) );
              if ( !showEventStacks ) {
                for ( ServiceStatusDetail a : this.getStatusDetails( ) ) {
                  a.setStackTrace( "" );
                }
              }
            }
          }
        };
      }
    };
  }
  
  @TypeMapper
  enum ServiceConfigurationToStatus implements Function<ServiceConfiguration, ServiceStatusType> {
    INSTANCE;
    
    @Override
    public ServiceStatusType apply( final ServiceConfiguration config ) {
      return new ServiceStatusType( ) {
        {
          this.setServiceId( TypeMappers.transform( config, ServiceId.class ) );
          this.setLocalEpoch( Topology.epoch( ) );
          try {
            this.setLocalState( config.lookupState( ).toString( ) );
          } catch ( Exception ex ) {
            this.setLocalState( "n/a: " + ex.getMessage( ) );
          }
          this.getStatusDetails( ).addAll( Collections2.transform( config.lookupDetails( ),
                                                                   TypeMappers.lookup( ServiceCheckRecord.class, ServiceStatusDetail.class ) ) );
          for ( ServiceStatusDetail a : this.getStatusDetails( ) ) {
            a.setStackTrace( "" );
          }
        }
      };
    }
  };
  
  @TypeMapper
  public enum ServiceIdToServiceConfiguration implements Function<ServiceId, ServiceConfiguration> {
    INSTANCE;
    
    @Override
    public ServiceConfiguration apply( ServiceId arg0 ) {
      Component comp = Components.lookup( arg0.getType( ) );
      ServiceConfiguration config;
      try {
        config = comp.lookupServiceConfiguration( arg0.getName( ) );
      } catch ( NoSuchElementException ex1 ) {
        ServiceBuilder<? extends ServiceConfiguration> builder = comp.getBuilder( );
        try {
          URI uri = new URI( arg0.getUri( ) );
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
  public enum ServiceConfigurationToServiceId implements Function<ServiceConfiguration, ServiceId> {
    INSTANCE;
    
    @Override
    public ServiceId apply( final ServiceConfiguration arg0 ) {
      return new ServiceId( ) {
        {
          setPartition( arg0.getPartition( ) );
          setName( arg0.getName( ) );
          setType( arg0.getComponentId( ).name( ) );
          setFullName( arg0.getFullName( ).toString( ) );
          if ( arg0.isVmLocal( ) ) {
            setUri( arg0.getComponentId( ).makeExternalRemoteUri( Internets.localHostAddress( ), arg0.getComponentId( ).getPort( ) ).toASCIIString( ) );
          } else {
            setUri( arg0.getUri( ).toASCIIString( ) );
          }
        }
      };
    }
    
  }
  
  private static Provider getProvider( ) {
    return DatabaseProvider.INSTANCE;
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
  
  public static <T extends ServiceConfiguration, C extends ComponentId> List<T> list( Class<C> type ) throws PersistenceException {
    if ( !ComponentId.class.isAssignableFrom( type ) ) {
      throw new PersistenceException( "Unknown configuration type passed: " + type.getCanonicalName( ) );
    } else {
      T example = ( T ) ServiceBuilders.lookup( type ).newInstance( );
      return ServiceConfigurations.list( example );
    }
  }
  
  public static <T extends ServiceConfiguration, C extends ComponentId> List<T> listPartition( Class<C> type, String partition ) throws PersistenceException, NoSuchElementException {
    if ( !ComponentId.class.isAssignableFrom( type ) ) {
      throw new PersistenceException( "Unknown configuration type passed: " + type.getCanonicalName( ) );
    }
    T example = ( T ) ServiceBuilders.lookup( type ).newInstance( );
    example.setPartition( partition );
    return list( example );
  }
  
  public static <T extends ServiceConfiguration, C extends ComponentId> T lookupByName( Class<C> type, String name ) {
    if ( !ComponentId.class.isAssignableFrom( type ) ) {
      throw new PersistenceException( "Unknown configuration type passed: " + type.getCanonicalName( ) );
    }
    T example = ( T ) ServiceBuilders.lookup( type ).newInstance( );
    example.setName( name );
    return lookup( example );
  }
 
  @Deprecated//GRZE:PLS not using.
  public static <T extends ServiceConfiguration, C extends ComponentId> T lookupByHost( final Class<C> type, final String host ) {
    if ( !ComponentId.class.isAssignableFrom( type ) ) {
      throw new PersistenceException( "Unknown configuration type passed: " + type.getCanonicalName( ) );
    }
    final T example = ( T ) ServiceBuilders.lookup( type ).newInstance( );
    example.setHostName( host );
    return lookup( example );
  }
 
  public static <T extends ServiceConfiguration> List<T> list( T type ) {
    return getProvider( ).list( type );
  }
  
  public static <T extends ServiceConfiguration> T store( T t ) {
    return getProvider( ).store( t );
  }
  
  public static <T extends ServiceConfiguration> T remove( T t ) {
    return getProvider( ).remove( t );
  }
  
  public static <T extends ServiceConfiguration> T lookup( T type ) {
    return getProvider( ).lookup( type );
  }

}
