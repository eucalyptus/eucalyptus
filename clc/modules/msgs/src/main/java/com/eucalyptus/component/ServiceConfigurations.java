package com.eucalyptus.component;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.NoSuchElementException;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Component.State;
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
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;

public class ServiceConfigurations {
  static Logger LOG = Logger.getLogger( ServiceConfigurations.class );
  
  interface Provider {
    public abstract <T extends ServiceConfiguration> List<T> list( T type );
    
    public abstract <T extends ServiceConfiguration> T store( T t );
    
    public abstract <T extends ServiceConfiguration> T remove( T t );
    
    public abstract <T extends ServiceConfiguration> T lookup( T type );
  }
  
  private enum DatabaseProvider implements Provider {
    INSTANCE;
    
    @Override
    public <T extends ServiceConfiguration> List<T> list( final T example ) {
      final EntityWrapper<T> db = EntityWrapper.get( example );
      List<T> componentList;
      try {
        componentList = db.query( example );
        db.commit( );
        return componentList;
      } catch ( final PersistenceException ex ) {
        LOG.trace( ex );
        db.rollback( );
        throw ex;
      } catch ( final Throwable ex ) {
        LOG.trace( ex );
        db.rollback( );
        throw new PersistenceException( "Service configuration lookup failed for: " + LogUtil.dumpObject( example ), ex );
      }
    }
    
    @Override
    public <T extends ServiceConfiguration> T lookup( final T example ) {
      final EntityWrapper<T> db = EntityWrapper.get( example );
      T existingName = null;
      try {
        existingName = db.getUnique( example );
        db.rollback( );
        return existingName;
      } catch ( final PersistenceException ex ) {
        LOG.trace( ex );
        db.rollback( );
        throw ex;
      } catch ( final Throwable ex ) {
        LOG.trace( ex );
        db.rollback( );
        throw new PersistenceException( "Service configuration lookup failed for: " + LogUtil.dumpObject( example ), ex );
      }
    }
    
    @Override
    public <T extends ServiceConfiguration> T store( T config ) {
      final EntityWrapper<T> db = EntityWrapper.get( config );
      try {
        db.add( config );
        config = db.getUnique( config );
        db.commit( );
        EventRecord.here( Provider.class, EventClass.COMPONENT, EventType.COMPONENT_REGISTERED, config.toString( ) ).info( );
      } catch ( final PersistenceException ex ) {
        LOG.trace( ex );
        EventRecord.here( Provider.class, EventClass.COMPONENT, EventType.COMPONENT_REGISTERED, "FAILED", config.toString( ) ).error( );
        db.rollback( );
        throw ex;
      } catch ( final Throwable ex ) {
        LOG.trace( ex );
        EventRecord.here( Provider.class, EventClass.COMPONENT, EventType.COMPONENT_REGISTERED, "FAILED", config.toString( ) ).error( );
        db.rollback( );
        throw new PersistenceException( "Service configuration storing failed for: " + LogUtil.dumpObject( config ), ex );
      }
      return config;
    }
    
    @Override
    public <T extends ServiceConfiguration> T remove( final T config ) {
      final EntityWrapper<T> db = EntityWrapper.get( config );
      try {
        final T searchConfig = ( T ) config.getClass( ).newInstance( );
        searchConfig.setName( config.getName( ) );
        final T exists = db.getUnique( searchConfig );
        db.delete( exists );
        db.commit( );
        EventRecord.here( Provider.class, EventClass.COMPONENT, EventType.COMPONENT_DEREGISTERED, config.toString( ) ).info( );
      } catch ( final PersistenceException ex ) {
        LOG.trace( ex );
        EventRecord.here( Provider.class, EventClass.COMPONENT, EventType.COMPONENT_DEREGISTERED, "FAILED", config.toString( ) ).error( );
        db.rollback( );
        throw ex;
      } catch ( final Throwable ex ) {
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
          /**
           * 
           */
          private static final long serialVersionUID = 1L;
          
          {
            this.setServiceId( TypeMappers.transform( config, ServiceId.class ) );
            this.setLocalEpoch( Topology.epoch( ) );
            try {
              this.setLocalState( config.lookupState( ).toString( ) );
            } catch ( final Exception ex ) {
              this.setLocalState( "n/a: " + ex.getMessage( ) );
            }
            if ( showEvents ) {
              this.getStatusDetails( ).addAll( Collections2.transform( config.lookupDetails( ),
                                                                       TypeMappers.lookup( ServiceCheckRecord.class, ServiceStatusDetail.class ) ) );
              if ( !showEventStacks ) {
                for ( final ServiceStatusDetail a : this.getStatusDetails( ) ) {
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
        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        
        {
          this.setServiceId( TypeMappers.transform( config, ServiceId.class ) );
          this.setLocalEpoch( Topology.epoch( ) );
          try {
            this.setLocalState( config.lookupState( ).toString( ) );
          } catch ( final Exception ex ) {
            this.setLocalState( "n/a: " + ex.getMessage( ) );
          }
          this.getStatusDetails( ).addAll( Collections2.transform( config.lookupDetails( ),
                                                                   TypeMappers.lookup( ServiceCheckRecord.class, ServiceStatusDetail.class ) ) );
          for ( final ServiceStatusDetail a : this.getStatusDetails( ) ) {
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
    public ServiceConfiguration apply( final ServiceId arg0 ) {
      final Component comp = Components.lookup( arg0.getType( ) );
      ServiceConfiguration config;
      try {
        config = comp.lookupServiceConfiguration( arg0.getName( ) );
      } catch ( final NoSuchElementException ex1 ) {
        final ServiceBuilder<? extends ServiceConfiguration> builder = comp.getBuilder( );
        try {
          final URI uri = new URI( arg0.getUri( ) );
          config = builder.newInstance( arg0.getPartition( ), arg0.getName( ), uri.getHost( ), uri.getPort( ) );
          comp.loadService( config );
        } catch ( final URISyntaxException ex ) {
          LOG.error( ex, ex );
          throw new UndeclaredThrowableException( ex );
        } catch ( final ServiceRegistrationException ex ) {
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
        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        
        {
          this.setPartition( arg0.getPartition( ) );
          this.setName( arg0.getName( ) );
          this.setType( arg0.getComponentId( ).name( ) );
          this.setFullName( arg0.getFullName( ).toString( ) );
          if ( arg0.isVmLocal( ) ) {
            this.setUri( arg0.getComponentId( ).makeExternalRemoteUri( Internets.localHostAddress( ), arg0.getComponentId( ).getPort( ) ).toASCIIString( ) );
          } else {
            this.setUri( arg0.getUri( ).toASCIIString( ) );
          }
        }
      };
    }
    
  }
  
  private static Provider getProvider( ) {
    return DatabaseProvider.INSTANCE;
  }
  
  public static ServiceConfiguration createEphemeral( final ComponentId compId, final String partition, final String name, final URI remoteUri ) {
    return new EphemeralConfiguration( compId, partition, name, remoteUri );
  }
  
  public static ServiceConfiguration createEphemeral( final ComponentId compId, final InetAddress host ) {
    return new EphemeralConfiguration( compId, compId.getPartition( ), host.getHostAddress( ), compId.makeInternalRemoteUri( host.getHostAddress( ),
                                                                                                                             compId.getPort( ) ) );
  }
  
  public static ServiceConfiguration createEphemeral( final Component component, final InetAddress host ) {
    return createEphemeral( component.getComponentId( ), host );
  }
  
  enum EnabledServiceConfiguration implements Predicate<ServiceConfiguration> {
    INSTANCE;
    @Override
    public boolean apply( final ServiceConfiguration arg0 ) {
      return Component.State.ENABLED.equals( arg0.lookupState( ) );
    }
  };
  
  @SuppressWarnings( "unchecked" )
  public static final <T extends ServiceConfiguration> Predicate<T> enabledService( ) {
    return ( Predicate<T> ) EnabledServiceConfiguration.INSTANCE;
  }
  
  public static <T extends ServiceConfiguration, C extends ComponentId> Iterable<ServiceConfiguration> enabledServices( final Class<C> type ) throws PersistenceException {
    return ServiceConfigurations.filter( type, enabledService( ) );
  }
  
  public static <T extends ServiceConfiguration, C extends ComponentId> Iterable<ServiceConfiguration> filter( final Class<C> type, final Predicate<T> pred ) throws PersistenceException {
    return Iterables.filter( ServiceConfigurations.list( type ), enabledService( ) );
  }
  
  public static <T extends ServiceConfiguration, C extends ComponentId> List<T> list( final Class<C> type ) throws PersistenceException {
    if ( !ComponentId.class.isAssignableFrom( type ) ) {
      throw new PersistenceException( "Unknown configuration type passed: " + type.getCanonicalName( ) );
    } else {
      final T example = ( T ) ServiceBuilders.lookup( type ).newInstance( );
      return ServiceConfigurations.list( example );
    }
  }
  
  public static <T extends ServiceConfiguration, C extends ComponentId> List<T> listPartition( final Class<C> type, final String partition ) throws PersistenceException, NoSuchElementException {
    if ( !ComponentId.class.isAssignableFrom( type ) ) {
      throw new PersistenceException( "Unknown configuration type passed: " + type.getCanonicalName( ) );
    }
    final T example = ( T ) ServiceBuilders.lookup( type ).newInstance( );
    example.setPartition( partition );
    return list( example );
  }
  
  public static <T extends ServiceConfiguration, C extends ComponentId> T lookupByName( final Class<C> type, final String name ) {
    if ( !ComponentId.class.isAssignableFrom( type ) ) {
      throw new PersistenceException( "Unknown configuration type passed: " + type.getCanonicalName( ) );
    }
    final T example = ( T ) ServiceBuilders.lookup( type ).newInstance( );
    example.setName( name );
    return lookup( example );
  }
  
  public static <T extends ServiceConfiguration> List<T> list( final T type ) {
    return getProvider( ).list( type );
  }
  
  public static <T extends ServiceConfiguration> T store( final T t ) {
    return getProvider( ).store( t );
  }
  
  public static <T extends ServiceConfiguration> T remove( final T t ) {
    return getProvider( ).remove( t );
  }
  
  public static <T extends ServiceConfiguration> T lookup( final T type ) {
    return getProvider( ).lookup( type );
  }

  public static Predicate<ServiceConfiguration> serviceInPartition( final Partition partition ) {
    return new Predicate<ServiceConfiguration>( ) {
      
      @Override
      public boolean apply( ServiceConfiguration arg0 ) {
        return partition.getName( ).equals( arg0.getPartition( ) ) && Component.State.ENABLED.equals( arg0.lookupState( ) );
      }
    };
  }
  
}
