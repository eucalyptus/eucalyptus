/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.component;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.bootstrap.Host;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.empyrean.ServiceAccount;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.CompatPredicate;
import com.eucalyptus.util.NonNullFunction;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.component.Faults.CheckException;
import com.eucalyptus.empyrean.ServiceId;
import com.eucalyptus.empyrean.ServiceStatusDetail;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class ServiceConfigurations {
  static Logger LOG = Logger.getLogger( ServiceConfigurations.class );
  
  public static <T extends ServiceConfiguration> List<ServiceConfiguration> list( ) throws PersistenceException {
    Predicate<ServiceConfiguration> alwaysTrue = Predicates.alwaysTrue();
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
      final EntityTransaction db = Entities.get( example.getClass( ) );
      List<T> componentList;
      try {
        componentList = Entities.query( example );
        db.commit( );
        return componentList;
      } catch ( final PersistenceException ex ) {
        LOG.debug( ex );
        db.rollback( );
        throw ex;
      } catch ( final Throwable ex ) {
        LOG.debug( ex );
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
      } catch ( final NoSuchElementException ex ) {
        db.rollback( );
        throw ex;
      } catch ( final PersistenceException ex ) {
        LOG.debug( ex );
        db.rollback( );
        throw ex;
      } catch ( final Throwable ex ) {
        LOG.debug( ex );
        db.rollback( );
        throw new PersistenceException( "Service configuration lookup failed for: " + LogUtil.dumpObject( example ), ex );
      }
    }
    
    public <T extends ServiceConfiguration> T store( T config ) {
      final EntityTransaction db = Entities.get( config.getClass( ) );
      try {
        config = Entities.mergeDirect( config );
        db.commit( );
        EventRecord.here( ServiceConfigurations.class, EventClass.COMPONENT, EventType.COMPONENT_REGISTERED, config.toString( ) ).info( );
      } catch ( final PersistenceException ex ) {
        LOG.debug( ex );
        EventRecord.here( ServiceConfigurations.class, EventClass.COMPONENT, EventType.COMPONENT_REGISTERED, "FAILED", config.toString( ) ).error( );
        db.rollback( );
        throw ex;
      } catch ( final Throwable ex ) {
        LOG.debug( ex );
        EventRecord.here( ServiceConfigurations.class, EventClass.COMPONENT, EventType.COMPONENT_REGISTERED, "FAILED", config.toString( ) ).error( );
        db.rollback( );
        throw new PersistenceException( "Service configuration storing failed for: " + LogUtil.dumpObject( config ), ex );
      }
      return config;
    }
    
    public <T extends ServiceConfiguration> T remove( final T config ) {
      final EntityTransaction db = Entities.get( config.getClass( ) );
      try {
        final T searchConfig = ( T ) config.getClass( ).newInstance( );
        searchConfig.setName( config.getName( ) );
        final T exists = Entities.uniqueResult( searchConfig );
        Entities.delete( exists );
        db.commit( );
        EventRecord.here( ServiceConfigurations.class, EventClass.COMPONENT, EventType.COMPONENT_DEREGISTERED, config.toString( ) ).info( );
      } catch ( final NoSuchElementException ex ) {
        db.rollback( );
      } catch ( final PersistenceException ex ) {
        LOG.debug( ex );
        EventRecord.here( ServiceConfigurations.class, EventClass.COMPONENT, EventType.COMPONENT_DEREGISTERED, "FAILED", config.toString( ) ).error( );
        db.rollback( );
        throw ex;
      } catch ( final Throwable ex ) {
        LOG.debug( ex );
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

  public static enum ServiceConfigurationToServiceAccounts implements Function<ServiceConfiguration, ArrayList<ServiceAccount>> {
    INSTANCE;
    public ArrayList<ServiceAccount> apply(ServiceConfiguration conf) {
      if(conf.getComponentId().getPublicComponentAccounts().isPresent()) {
        return Lists.newArrayList(Iterables.transform(conf.getComponentId().getPublicComponentAccounts().get(), new Function<UserPrincipal, ServiceAccount>() {
          @Nullable
          @Override
          public ServiceAccount apply(@Nullable UserPrincipal userPrincipal) {
            if(userPrincipal != null) {
              try {
                return new ServiceAccount(userPrincipal.getAccountAlias(),
                                          userPrincipal.getAccountNumber(),
                                          userPrincipal.getCanonicalId());
              } catch(Exception e) {
                LOG.warn("Exception looking up account info for system account number: " + userPrincipal.getAccountNumber(), e);
                return null;
              }
            } else {
              return null;
            }
          }
        }));
      } else {
        return null;
      }
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
              this.getStatusDetails( ).addAll( Collections2.transform( Faults.lookup( config ),
                                                                       TypeMappers.lookup( CheckException.class, ServiceStatusDetail.class ) ) );
              if ( !showEventStacks ) {
                for ( final ServiceStatusDetail a : this.getStatusDetails( ) ) {
                  a.setStackTrace( "" );
                }
              }
            }

            //Populate the system accounts
            try {
              List<ServiceAccount> accnts = ServiceConfigurationToServiceAccounts.INSTANCE.apply(config);
              if(accnts != null && accnts.size() > 0) {
                this.getServiceAccounts().addAll(accnts);
              }
            } catch (Exception e) {
              LOG.warn("Failed finding service account for service: " + config.getName(), e);
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
          if ( config.isVmLocal( ) && Faults.isFailstop( ) ) {
            this.setLocalState( Component.State.NOTREADY.name( ) );
          } else {
            try {
              this.setLocalState( config.lookupState( ).toString( ) );
            } catch ( final Exception ex ) {
              this.setLocalState( "n/a: " + ex.getMessage( ) );
            }
          }
          this.getStatusDetails( ).addAll( Collections2.transform( Faults.lookup( config ),
                                                                   TypeMappers.lookup( CheckException.class, ServiceStatusDetail.class ) ) );
          for ( final ServiceStatusDetail a : this.getStatusDetails( ) ) {
            a.setStackTrace( "" );
          }
        }
      };
    }
  };
  
  @TypeMapper
  public enum ServiceIdToServiceStatus implements Function<ServiceId, ServiceStatusType> {
    INSTANCE;
    private final Function<ServiceId, ServiceStatusType> transform = Functions.compose( ServiceConfigurationToStatus.INSTANCE, ServiceIdToServiceConfiguration.INSTANCE );

    @Override
    public ServiceStatusType apply( ServiceId input ) {
      return transform.apply( input );
    }
    
  }
  @TypeMapper
  public enum ServiceIdToServiceConfiguration implements Function<ServiceId, ServiceConfiguration> {
    INSTANCE;
    
    @Override
    public ServiceConfiguration apply( final ServiceId arg0 ) {
        Component comp = null;
        try {
          comp = Components.lookup( arg0.getType( ) );
          try {
            return comp.lookup( arg0.getName( ) );
          } catch ( final NoSuchElementException ex1 ) {
            final ServiceBuilder<? extends ServiceConfiguration> builder = ServiceBuilders.lookup( comp.getComponentId( ) );
            try {
              final URI uri = new URI( arg0.getUri( ) );
              ServiceConfiguration config = builder.newInstance( arg0.getPartition( ), arg0.getName( ), uri.getHost( ), uri.getPort( ) );
              comp.setup( config );
              return config;
            } catch ( final URISyntaxException ex ) {
              LOG.error( ex, ex );
              throw Exceptions.toUndeclared( ex );
            }
          }
        } catch ( NoSuchElementException ex2 ) {//gracefully handle case where an as-yet unknown component type is referenced
          ComponentId compId = ComponentIds.createEphemeral( arg0.getType( ) );
          try {
            comp = Components.create( compId );
            return comp.lookup( arg0.getName( ) );
          } catch ( Exception ex ) {
            try {
              return ServiceConfigurations.createEphemeral( compId, arg0.getPartition( ), arg0.getName( ),  new URI( arg0.getUri( ) ) );
            } catch ( URISyntaxException ex1 ) {
              LOG.error( ex1 );
              throw Exceptions.toUndeclared( ex1 );
            }
          }
        }
    }
    
  }
  
  @TypeMapper
  public enum ServiceConfigurationToServiceId implements Function<ServiceConfiguration, ServiceId> {
    INSTANCE;

    private static final Function<InetAddress,Cidr> cidrLookup = CacheBuilder.newBuilder()
        .maximumSize( 64 )
        .expireAfterWrite( 1, TimeUnit.MINUTES )
        .build( CacheLoader.from( Functions.compose(
            CollectionUtils.optionalOr( Cidr.of( 0, 0 ) ),
            Internets.interfaceCidr() ) ) );

    @Override
    public ServiceId apply( final ServiceConfiguration config ) {
      return new ServiceId( ) {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        
        {
          this.setPartition( config.getPartition() );
          this.setName( config.getName() );
          this.setType( config.getComponentId( ).name( ) );
          this.setFullName( config.getFullName().toString() );
          if ( config.isVmLocal( ) ) {
            this.setServiceUri(ServiceUris.remote(config.getComponentId(),
                                                  maphost(Internets.localHostInetAddress())));
          } else {
            this.setServiceUri(ServiceUris.remote(config.getComponentId(),
                                                  maphost(config.getInetAddress()),
                                                  config.getPort()));
          }
        }
      };
    }

    @SuppressWarnings( "ConstantConditions" )
    private static InetAddress maphost( final InetAddress hostAddress ) {
      InetAddress result = hostAddress;

      try {
        final SocketAddress address = Contexts.lookup( ).getChannel( ).getLocalAddress( );
        if ( address instanceof InetSocketAddress ) {
          final Cidr cidr = cidrLookup.apply( ((InetSocketAddress)address).getAddress( ) );
          if ( !cidr.apply( result ) ) {
            final Host host = Hosts.lookup( hostAddress );
            if ( host != null ) {
              result = Iterables.tryFind( host.getHostAddresses( ), cidr ).or( result );
            }
          }
        }
      } catch ( IllegalContextAccessException e ) {
        // do not map
      }
      return result;
    }
  }

  public static ServiceConfiguration createEphemeral( final ComponentId compId, final String partition, final String name, final URI remoteUri ) {
    return new EphemeralConfiguration( compId, partition, name, remoteUri );
  }
  
  public static ServiceConfiguration createEphemeral( final ComponentId compId ) {
    return createEphemeral( compId, Internets.localHostInetAddress( ) );
  }
  
  public static ServiceConfiguration createEphemeral( final ComponentId compId, final InetAddress host ) {
    return new EphemeralConfiguration( compId, compId.getPartition( ), host.getHostAddress( ), ServiceUris.remote( compId, host ) );
  }
  
  public static ServiceConfiguration createEphemeral( final Component component, final InetAddress host ) {
    return createEphemeral( component.getComponentId( ), host );
  }
  
  public static ServiceConfiguration createBogus( final Class<? extends ComponentId> compIdClass, final Class<?> ownerType ) {
    ComponentId compId = ComponentIds.lookup( compIdClass ); 
    return new EphemeralConfiguration( compId, compId.getPartition( ), ownerType.getSimpleName( ), ServiceUris.internal( compId, Internets.localHostInetAddress( ), ownerType.getSimpleName( ) ) );
  }

  public static ServiceConfiguration createBogus( final ServiceConfiguration serviceConfiguration, final Class<?> ownerType ) {
    final ComponentId compId = serviceConfiguration.getComponentId();
    return new EphemeralConfiguration(
        compId,
        compId.getPartition( ),
        ownerType.getSimpleName( ) + "-" + serviceConfiguration.getHostName( ) +
            ( serviceConfiguration.getHostName( ).equals( serviceConfiguration.getName( ) ) ?
                "" :
                "-" + serviceConfiguration.getName( ) ),
        ServiceUris.internal( compId, Internets.localHostInetAddress( ), ownerType.getSimpleName( ) ) );
  }

  public static <T extends ServiceConfiguration, C extends ComponentId> Iterable<T> filter( final C componentId, final Predicate<T> pred ) throws PersistenceException {
    return filter( componentId.getClass( ), pred );
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
  
  public static <T extends ServiceConfiguration> T lookupByName( final String name ) {
    for ( ComponentId c : ComponentIds.list( ) ) {
      ServiceConfiguration example = ServiceBuilders.lookup( c.getClass( ) ).newInstance( );
      if ( example instanceof EphemeralConfiguration ) continue;
      example.setName( name );
      try {
        return ( T ) lookup( example );
      } catch ( Exception ex ) {}
    }
    throw new NoSuchElementException( "Failed to lookup any registered component with the name: " + name );
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
    example.setHostName( host );
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

  static void update( final ServiceConfiguration destination,
                      final ServiceConfiguration source ) {
    update( destination, source.getHostName( ), source.getPort( ) );
  }

  static void update( final ServiceConfiguration configuration,
                      final String host,
                      final Integer port ) {
    configuration.setHostName( host );
    configuration.setPort( port );
  }

  enum ServiceIsHostLocal implements CompatPredicate<ServiceConfiguration> {
    INSTANCE;
    
    @Override
    public boolean apply( ServiceConfiguration input ) {
      return Hosts.isServiceLocal( input );
    }
    
  }

  enum ServiceIsVmLocal implements CompatPredicate<ServiceConfiguration> {
    INSTANCE;

    @Override
    public boolean apply( ServiceConfiguration input ) {
      return input!=null && input.isVmLocal( );
    }
  }

  enum EnabledServiceConfiguration implements CompatPredicate<ServiceConfiguration> {
    INSTANCE;
    @Override
    public boolean apply( final ServiceConfiguration arg0 ) {
      return Component.State.ENABLED.equals( arg0.lookupState( ) );
    }
  };
  
  @SuppressWarnings( "unchecked" )
  public static final <T extends ServiceConfiguration> CompatPredicate<T> filterEnabled( ) {
    return ( CompatPredicate<T> ) EnabledServiceConfiguration.INSTANCE;
  }
  
  public static CompatPredicate<ServiceConfiguration> filterHostLocal( ) {
    return ServiceIsHostLocal.INSTANCE;
  }

  public static CompatPredicate<ServiceConfiguration> filterVmLocal( ) {
    return ServiceIsVmLocal.INSTANCE;
  }

  /**
   * Filter for enabled services in the specified partition.
   * @param partition
   * @return
   */
  public static CompatPredicate<ServiceConfiguration> filterEnabledByPartition( final Partition partition ) {
    return new CompatPredicate<ServiceConfiguration>( ) {
      
      @Override
      public boolean apply( ServiceConfiguration arg0 ) {
        return partition.getName( ).equals( arg0.getPartition( ) ) && Component.State.ENABLED.equals( arg0.lookupState( ) );
      }
    };
  }

  enum ServiceConfigurationToPublicEndpoint implements NonNullFunction<ServiceConfiguration,String> {
    INSTANCE;

    @Nonnull
    @Override
    public String apply( final ServiceConfiguration serviceConfiguration ) {
      return ServiceUris.remotePublicify( serviceConfiguration ).toASCIIString( );
    }
  }

  public static NonNullFunction<ServiceConfiguration,String> remotePublicify( ) {
    return ServiceConfigurationToPublicEndpoint.INSTANCE;
  }

  enum ServiceConfigurationToComponentId implements NonNullFunction<ServiceConfiguration,ComponentId> {
    INSTANCE;

    @Nonnull
    @Override
    public ComponentId apply( final ServiceConfiguration serviceConfiguration ) {
      return serviceConfiguration.getComponentId( );
    }
  }

  public static NonNullFunction<ServiceConfiguration,ComponentId> componentId( ) {
    return ServiceConfigurationToComponentId.INSTANCE;
  }

  @TypeMapper
  public enum CheckExceptionRecordMapper implements Function<CheckException, ServiceStatusDetail> {
    INSTANCE;
    @Override
    public ServiceStatusDetail apply( final CheckException input ) {
      ServiceConfiguration config = null;
      final String serviceFullName = Strings.nullToEmpty( input.getServiceFullName() );
      String checkName = Strings.nullToEmpty(input.getServiceName());
      for ( Component c : Components.list( ) ) {
        for ( ServiceConfiguration s : c.services( ) ) {
          if ( serviceFullName.equals( s.getFullName().toString() ) ) {
            config = s;
            checkName = s.getName();
            break;
          }
        }
      }
      if ( config == null ) try {
        config = ServiceConfigurations.lookupByName( checkName );
      } catch ( RuntimeException e ) {
        throw e;
      }
      final String serviceName = checkName;
      final ServiceConfiguration finalConfig = config;
      return new ServiceStatusDetail( ) {
        {
          this.setSeverity( input.getSeverity( ).toString( ) );
          this.setUuid( input.getCorrelationId( ) );
          this.setTimestamp( input.getTimestamp( ).toString( ) );
          this.setMessage( input.getMessage( ) != null
            ? input.getMessage( )
            : "No summary information available." );
          this.setStackTrace( input.getStackString( ) != null
            ? input.getStackString( )
            : Exceptions.string( new RuntimeException( "Error while mapping service event record:  No stack information available" ) ) );
          this.setServiceFullName( finalConfig.getFullName().toString( ) );
          this.setServiceHost( finalConfig.getHostName() );
          this.setServiceName( serviceName );
        }
      };
    }
  }
}
