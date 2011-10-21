package com.eucalyptus.component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.NoSuchElementException;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import com.eucalyptus.empyrean.ServiceId;
import com.eucalyptus.empyrean.ServiceStatusDetail;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class ServiceConfigurations {
  static Logger LOG = Logger.getLogger( ServiceConfigurations.class );
  
  public static <T extends ServiceConfiguration> List<ServiceConfiguration> list( ) throws PersistenceException {
    Predicate<ServiceConfiguration> alwaysTrue = Predicates.alwaysTrue( );
    return Lists.newArrayList( filter( alwaysTrue ) );
  }
  
  public static <T extends ServiceConfiguration> Iterable<ServiceConfiguration> filter( final Predicate<T> pred ) throws PersistenceException {
    List<ServiceConfiguration> configs = Lists.newArrayList( );
    for ( ComponentId compId : ComponentIds.list( ) ) {
      Iterables.addAll( configs, filter( compId.getClass( ), pred ) );
    }
    return configs;
  }
  
  private enum DatabaseProvider {
    INSTANCE;
    
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
    
    public <T extends ServiceConfiguration> T lookup( final T example ) {
      final EntityTransaction db = Entities.get( example );
      T existingName = null;
      try {
        existingName = Entities.uniqueResult( example );
        db.commit( );
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
    
    public <T extends ServiceConfiguration> T store( T config ) {
      final EntityWrapper<T> db = EntityWrapper.get( config );
      try {
        config = db.persist( config );
        db.commit( );
        EventRecord.here( ServiceConfigurations.class, EventClass.COMPONENT, EventType.COMPONENT_REGISTERED, config.toString( ) ).info( );
      } catch ( final PersistenceException ex ) {
        LOG.trace( ex );
        EventRecord.here( ServiceConfigurations.class, EventClass.COMPONENT, EventType.COMPONENT_REGISTERED, "FAILED", config.toString( ) ).error( );
        db.rollback( );
        throw ex;
      } catch ( final Throwable ex ) {
        LOG.trace( ex );
        EventRecord.here( ServiceConfigurations.class, EventClass.COMPONENT, EventType.COMPONENT_REGISTERED, "FAILED", config.toString( ) ).error( );
        db.rollback( );
        throw new PersistenceException( "Service configuration storing failed for: " + LogUtil.dumpObject( config ), ex );
      }
      return config;
    }
    
    public <T extends ServiceConfiguration> T remove( final T config ) {
      final EntityWrapper<T> db = EntityWrapper.get( config );
      try {
        final T searchConfig = ( T ) config.getClass( ).newInstance( );
        searchConfig.setName( config.getName( ) );
        final T exists = db.getUnique( searchConfig );
        db.delete( exists );
        db.commit( );
        EventRecord.here( ServiceConfigurations.class, EventClass.COMPONENT, EventType.COMPONENT_DEREGISTERED, config.toString( ) ).info( );
      } catch ( final PersistenceException ex ) {
        LOG.trace( ex );
        EventRecord.here( ServiceConfigurations.class, EventClass.COMPONENT, EventType.COMPONENT_DEREGISTERED, "FAILED", config.toString( ) ).error( );
        db.rollback( );
        throw ex;
      } catch ( final Throwable ex ) {
        LOG.trace( ex );
        EventRecord.here( ServiceConfigurations.class, EventClass.COMPONENT, EventType.COMPONENT_DEREGISTERED, "FAILED", config.toString( ) ).error( );
        db.rollback( );
        throw new PersistenceException( "Service configuration removal failed for: " + LogUtil.dumpObject( config ), ex );
      }
      return config;
    }
    
  }
  
  public static Function<ServiceConfiguration, ServiceStatusType> asServiceStatus( ) {
    return asServiceStatus( false, false );
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
          throw Exceptions.toUndeclared( ex );
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
            this.setUri( ServiceUris.remote( arg0.getComponentId( ) ).toASCIIString( ) );
          } else {
            this.setUri( ServiceUris.remote( arg0 ).toASCIIString( ) );
          }
        }
      };
    }
    
  }
  
  public static ServiceConfiguration createEphemeral( final ComponentId compId, final String partition, final String name, final URI remoteUri ) {
    return new EphemeralConfiguration( compId, partition, name, remoteUri );
  }
  
  public static ServiceConfiguration createEphemeral( final ComponentId compId ) {
    return createEphemeral( compId, Internets.localHostInetAddress( ) );
  }
  
  public static ServiceConfiguration createEphemeral( final ComponentId compId, final InetAddress host ) {
    return new EphemeralConfiguration( compId, compId.getPartition( ), host.getHostAddress( ), ServiceUris.remote( compId ) );
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
  
  public static <T extends ServiceConfiguration, C extends ComponentId> Iterable<T> enabledServices( final Class<C> type ) throws PersistenceException {
    Predicate<T> enabledService = enabledService( );
    return ServiceConfigurations.filter( type, enabledService );
  }
  
  public static <T extends ServiceConfiguration, C extends ComponentId> Iterable<T> filter( final Class<C> type, final Predicate<T> pred ) throws PersistenceException {
    List<T> list = ServiceConfigurations.list( type );
    return Iterables.filter( list, pred );
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
  
  public static <T extends ServiceConfiguration, C extends ComponentId> T lookupByHost( final Class<C> type, final String host ) {
    if ( !ComponentId.class.isAssignableFrom( type ) ) {
      throw new PersistenceException( "Unknown configuration type passed: " + type.getCanonicalName( ) );
    }
    final T example = ( T ) ServiceBuilders.lookup( type ).newInstance( );
    example.setHostName( host );
    return lookup( example );
  }
  
  public static <T extends ServiceConfiguration, C extends ComponentId> T lookupByAliasHost( final Class<C> type, final String host ) {
    if ( !ComponentId.class.isAssignableFrom( type ) ) {
      throw new PersistenceException( "Unknown configuration type passed: " + type.getCanonicalName( ) );
    }
    final T example = ( T ) ServiceBuilders.lookup( type ).newInstance( );
    example.setSourceHostName( host );
    return lookup( example );
  }
  
  public static <T extends ServiceConfiguration> List<T> list( final T type ) {
    return DatabaseProvider.INSTANCE.list( type );
  }
  
  public static <T extends ServiceConfiguration> T store( final T t ) {
    return DatabaseProvider.INSTANCE.store( t );
  }
  
  public static <T extends ServiceConfiguration> T remove( final T t ) {
    return DatabaseProvider.INSTANCE.remove( t );
  }
  
  public static <T extends ServiceConfiguration> T lookup( final T type ) {
    return DatabaseProvider.INSTANCE.lookup( type );
  }
  
  enum ServiceIsHostLocal implements Predicate<ServiceConfiguration> {
    INSTANCE;
    
    @Override
    public boolean apply( ServiceConfiguration input ) {
      return input.isHostLocal( );
    }
    
  }
  
  public static Predicate<ServiceConfiguration> filterHostLocal( ) {
    return ServiceIsHostLocal.INSTANCE;
  }
  
  public static Predicate<ServiceConfiguration> filterByPartition( final Partition partition ) {
    return new Predicate<ServiceConfiguration>( ) {
      
      @Override
      public boolean apply( ServiceConfiguration arg0 ) {
        return partition.getName( ).equals( arg0.getPartition( ) ) && Component.State.ENABLED.equals( arg0.lookupState( ) );
      }
    };
  }
  
  @TypeMapper( { ServiceCheckRecord.class, ServiceStatusDetail.class } )
  public enum ServiceCheckRecordMapper implements Function<ServiceCheckRecord, ServiceStatusDetail> {
    INSTANCE;
    @Override
    public ServiceStatusDetail apply( final ServiceCheckRecord input ) {
      return new ServiceStatusDetail( ) {
        {
          this.setSeverity( input.getSeverity( ).toString( ) );
          this.setUuid( input.getUuid( ) );
          this.setTimestamp( input.getTimestamp( ).toString( ) );
          this.setMessage( input.getMessage( ) != null
            ? input.getMessage( )
            : "No summary information available." );
          this.setStackTrace( input.getStackTrace( ) != null
            ? input.getStackTrace( )
            : Exceptions.string( new RuntimeException( "Error while mapping service event record:  No stack information available" ) ) );
          this.setServiceFullName( input.getServiceFullName( ) );
          this.setServiceHost( input.getServiceHost( ) );
          this.setServiceName( input.getServiceName( ) );
        }
      };
    }
  }
  
  @TypeMapper
  public enum ServiceBuilderMapper implements Function<ServiceConfiguration, ServiceBuilder<? extends ServiceConfiguration>> {
    INSTANCE;
    
    @Override
    public ServiceBuilder<? extends ServiceConfiguration> apply( final ServiceConfiguration input ) {
      return ServiceBuilders.lookup( input.getComponentId( ) );
    }
    
  }
}
