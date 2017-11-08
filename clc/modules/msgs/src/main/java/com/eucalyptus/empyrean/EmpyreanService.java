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

package com.eucalyptus.empyrean;

import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.notNullValue;

import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;
import com.eucalyptus.component.ServiceOrderings;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.component.groups.ServiceGroups;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.util.fsm.OrderlyTransitionException;
import com.google.common.base.Functions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.annotation.ServiceOperation;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.BaseEncoding;

@ComponentNamed
public class EmpyreanService {
  private static Logger LOG = Logger.getLogger( EmpyreanService.class );

  @ServiceOperation( hostDispatch = true )
  public enum ModifyService implements Function<ModifyServiceType, ModifyServiceResponseType> {
    INSTANCE;

    @Override
    public ModifyServiceResponseType apply( final ModifyServiceType input ) {
      try {
        return EmpyreanService.modifyService( input );
      } catch ( final Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }

  }

  @ServiceOperation( hostDispatch = true )
  public enum StartService implements Function<StartServiceType, StartServiceResponseType> {
    INSTANCE;

    @Override
    public StartServiceResponseType apply( final StartServiceType input ) {
      try {
        return EmpyreanService.startService( input );
      } catch ( final Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }

  }

  @ServiceOperation( hostDispatch = true )
  public enum StopService implements Function<StopServiceType, StopServiceResponseType> {
    INSTANCE;

    @Override
    public StopServiceResponseType apply( final StopServiceType input ) {
      try {
        return EmpyreanService.stopService( input );
      } catch ( final Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }

  }

  @ServiceOperation( hostDispatch = true )
  public enum EnableService implements Function<EnableServiceType, EnableServiceResponseType> {
    INSTANCE;

    @Override
    public EnableServiceResponseType apply( final EnableServiceType input ) {
      try {
        return EmpyreanService.enableService( input );
      } catch ( final Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }

  }

  @ServiceOperation( hostDispatch = true )
  public enum DisableService implements Function<DisableServiceType, DisableServiceResponseType> {
    INSTANCE;

    @Override
    public DisableServiceResponseType apply( final DisableServiceType input ) {
      try {
        return EmpyreanService.disableService( input );
      } catch ( final Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }

  }

  enum NamedTransition implements Predicate<ModifyServiceType> {
    INSTANCE;

    @Override
    public boolean apply( ModifyServiceType request ) {
      try {
        final Topology.Transitions transition = Topology.Transitions.valueOf( request.getState( ).toUpperCase( ) );
        String name = request.getName( );
        ServiceConfiguration config = findService( name );
        if ( Topology.Transitions.RESTART.equals( transition ) ) {
          Topology.stop( config ).get( );
          try {
            Topology.start( config ).get( );
          } catch ( Exception ex ) {
            Exceptions.maybeInterrupted( ex );
            Logs.extreme( ).error( ex, ex );
            throw Exceptions.toUndeclared( ex );
          }
        } else {
          Topology.transition( transition.get( ) ).apply( config ).get( );
        }
      } catch ( final IllegalArgumentException ex ) {
        return false;
      } catch ( final Exception ex ) {
        Exceptions.maybeInterrupted( ex );
        Logs.extreme( ).error( ex, ex );
        throw Exceptions.toUndeclared( ex );
      }

      return true;
    }
  }

  private static ServiceConfiguration findService( final String name ) {
    checkParam( name, notNullValue() );
    Predicate<ServiceConfiguration> nameOrFullName = new Predicate<ServiceConfiguration>( ) {

      @Override
      public boolean apply( ServiceConfiguration input ) {
        return name.equals( input.getName( ) ) || name.equals( input.getFullName( ).toString( ) );
      }
    };
    for ( final ComponentId compId : ComponentIds.list( ) ) {
      ServiceConfiguration a;
      try {
        return Iterables.find( Components.lookup( compId ).services( ), nameOrFullName );
      } catch ( NoSuchElementException ex ) {
        if ( compId.isRegisterable( ) ) {
          try {
            return ServiceConfigurations.lookupByName( compId.getClass( ), name );
          } catch ( Exception ex1 ) {}
        }
      }
    }
    throw new NoSuchElementException( "Failed to lookup service named: " + name );
  }

  public static ModifyServiceResponseType modifyService( final ModifyServiceType request ) throws Exception {
    final ModifyServiceResponseType reply = request.getReply( );
    try {
      if ( NamedTransition.INSTANCE.apply( request ) ) {
        reply.markWinning( );
      } else {
        Component.State nextState = Component.State.valueOf( request.getState( ).toUpperCase( ) );
        ServiceConfiguration config = findService( request.getName( ) );
        Topology.transition( nextState ).apply( config ).get( );
        reply.markWinning( );
      }
    } catch ( Exception ex ) {
      Exceptions.maybeInterrupted( ex );
      throw new EucalyptusCloudException( "Failed to execute request transition: "
                                     + request.getState( )
                                     + "\nDue to:\n"
                                     + Throwables.getRootCause( ex ).getMessage( )
                                     + "\nPossible arguments are: \n"
                                     + "TRANSITIONS\n\t"
                                     + Joiner.on( "\n\t" ).join( Topology.Transitions.values( ) )
                                     + "STATES\n\t"
                                     + Joiner.on( "\n\t" ).join( Component.State.values( ) ),
                                     ex );
    }
    return reply;
  }

  public static StartServiceResponseType startService( final StartServiceType request ) throws Exception {
    final StartServiceResponseType reply = request.getReply( );
    for ( final ServiceId serviceInfo : request.getServices( ) ) {
      try {
        final Component comp = Components.lookup( serviceInfo.getType( ) );
        final ServiceConfiguration service = TypeMappers.transform( serviceInfo, ServiceConfiguration.class );
        if ( service.isVmLocal( ) ) {
          try {
            Topology.start( service ).get( );
            reply.getServices( ).add( serviceInfo );
          } catch ( final IllegalStateException ex ) {
            LOG.error( ex, ex );
            throw ex;
          }
        }
      } catch ( final Exception ex ) {
        final OrderlyTransitionException otex = Exceptions.findCause( ex, OrderlyTransitionException.class );
        if ( otex != null ) {
          LOG.warn( otex );
        } else {
          LOG.error( ex, ex );
        }
        throw ex;
      }
    }
    return reply;
  }

  public static DestroyServiceResponseType destroyService( final DestroyServiceType request ) throws Exception {
    DestroyServiceResponseType reply = request.getReply( );
    for ( final ServiceId serviceInfo : request.getServices( ) ) {
      try {
        final ServiceConfiguration service = TypeMappers.transform( serviceInfo, ServiceConfiguration.class );
        if ( service.isVmLocal( ) ) {
          try {
            Topology.destroy( service ).get( );
          } catch ( final IllegalStateException ex ) {
            LOG.error( ex, ex );
          }
        }
        reply.getServices( ).add( serviceInfo );
      } catch ( final Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).debug( ex, ex );
      }
    }
    return reply;
  }

  public static StopServiceResponseType stopService( final StopServiceType request ) throws Exception {
    final StopServiceResponseType reply = request.getReply( );
    for ( final ServiceId serviceInfo : request.getServices( ) ) {
      try {
        final Component comp = Components.lookup( serviceInfo.getType( ) );
        final ServiceConfiguration service = TypeMappers.transform( serviceInfo, ServiceConfiguration.class );
        if ( service.isVmLocal( ) ) {
          try {
            Topology.stop( service ).get( );
            reply.getServices( ).add( serviceInfo );
          } catch ( final IllegalStateException ex ) {
            LOG.error( ex, ex );
            throw ex;
          }
        }
      } catch ( final Exception ex ) {
        LOG.error( ex, ex );
        throw ex;
      }
    }
    return reply;
  }

  public static EnableServiceResponseType enableService( final EnableServiceType request ) throws Exception {
    final EnableServiceResponseType reply = request.getReply( );
    for ( final ServiceId serviceInfo : request.getServices( ) ) {
      try {
        final Component comp = Components.lookup( serviceInfo.getType( ) );
        final ServiceConfiguration service = TypeMappers.transform( serviceInfo, ServiceConfiguration.class );
        if ( service.isVmLocal( ) ) {
          try {
            Topology.enable( service ).get( );
            reply.getServices( ).add( serviceInfo );
          } catch ( final IllegalStateException ex ) {
            LOG.error( ex, ex );
            throw ex;
          }
        }
      } catch ( final Exception ex ) {
        LOG.error( ex, ex );
        throw ex;
      }
    }
    return reply;
  }

  public static DisableServiceResponseType disableService( final DisableServiceType request ) throws Exception {
    final DisableServiceResponseType reply = request.getReply( );
    for ( final ServiceId serviceInfo : request.getServices( ) ) {
      try {
        final Component comp = Components.lookup( serviceInfo.getType( ) );
        final ServiceConfiguration service = TypeMappers.transform( serviceInfo, ServiceConfiguration.class );
        if ( service.isVmLocal( ) ) {
          try {
            Topology.disable( service ).get( );
            reply.getServices( ).add( serviceInfo );
          } catch ( final IllegalStateException ex ) {
            LOG.error( ex, ex );
            throw ex;
          }
        }
      } catch ( final NoSuchElementException ex ) {
        LOG.error( ex, ex );
        throw ex;
      }
    }
    return reply;
  }

  static class Filters {
    /**
     * Build a predicate from the given filters
     */
    private static Predicate<ServiceConfiguration> from( Iterable<Filter> filters ) {
      final List<Predicate<ServiceConfiguration>> predicates = Lists.newArrayList( );
      for ( final Filter filter : filters ) {
        final Function<String,Predicate<ComponentId>> cbuilder = componentIdFilterBuilders.get( filter.getName( ) );
        if ( cbuilder != null ) {
          predicates.add( Predicates.compose(
              Predicates.or( Iterables.transform( filter.getValues( ), cbuilder ) ),
              ServiceConfigurations.componentId( ) ) );
        } else {
          final Function<String, Predicate<ServiceConfiguration>> builder = serviceConfigurationFilterBuilders.get( filter.getName( ) );
          predicates.add( builder == null || filter.getValues( ) == null ?
              Predicates.<ServiceConfiguration>alwaysFalse( ) :
              Predicates.or( Iterables.transform( filter.getValues( ), builder ) ) );
        }
      }
      return Predicates.and( predicates );
    }

    /**
     * Build a predicate from the given filters
     */
    private static Predicate<ComponentId> componentFrom( Iterable<Filter> filters ) {
      final List<Predicate<ComponentId>> predicates = Lists.newArrayList( );
      for ( final Filter filter : filters ) {
        final Function<String,Predicate<ComponentId>> builder = componentIdFilterBuilders.get( filter.getName( ) );
        predicates.add( builder == null || filter.getValues( ) == null ?
            Predicates.<ComponentId>alwaysFalse( ) :
            Predicates.or( Iterables.transform( filter.getValues( ), builder ) ) );
      }
      return Predicates.and( predicates );
    }

    private static Predicate<String> usageFrom( Iterable<Filter> filters ) {
      final List<Predicate<String>> predicates = Lists.newArrayList( );
      for ( final Filter filter : filters ) {
        if ( "certificate-usage".equals( filter.getName( ) ) ) {
          predicates.add( Predicates.in( filter.getValues( ) ) );
        }
      }
      return Predicates.and( predicates );
    }

    /**
     * Create a string filter builder
     */
    private static <I> Function<String,Predicate<I>> sfb( final Function<I,String> f ) {
      return new Function<String,Predicate<I>>( ) {
        @Nullable
        @Override
        public final Predicate<I> apply( final String filter ) {
          return new Predicate<I>( ){
            @Override
            public boolean apply( final I item ) {
              return filter.equals( f.apply( item ) );
            }
          };
        }
      };
    };

    /**
     * Create a string set filter builder
     */
    private static <I> Function<String,Predicate<I>> ssfb( final Function<I,Set<String>> f ) {
      return new Function<String,Predicate<I>>( ) {
        @Nullable
        @Override
        public final Predicate<I> apply( final String filter ) {
          return new Predicate<I>( ){
            @Override
            public boolean apply( final I item ) {
              //noinspection ConstantConditions
              return f.apply( item ).contains( filter );
            }
          };
        }
      };
    };

    /**
     * Create a boolean filter builder
     */
    private static <I> Function<String,Predicate<I>> bfb( final Function<I,Boolean> f ) {
      return sfb( Functions.compose( Functions.toStringFunction( ), f ) );
    }

    /**
     * Map of filter names to component filter builders
     */
    private static final Map<String,Function<String,Predicate<ComponentId>>> componentIdFilterBuilders =
        ImmutableMap.<String,Function<String,Predicate<ComponentId>>>builder( )
            .put( "internal", bfb( new Function<ComponentId, Boolean>( ) {
              @Override
              public Boolean apply( final ComponentId componentId ) {
                return componentId.isInternal( );
              }
            } ) )
            .put( "user-service", bfb( new Function<ComponentId, Boolean>( ) {
              @Override
              public Boolean apply( final ComponentId componentId ) {
                return componentId.isAdminService( );
              }
            } ) )
            .put( "public", bfb( new Function<ComponentId, Boolean>( ) {
              @Override public Boolean apply( final ComponentId componentId ) {
                return componentId.isPublicService( );
              }
            } ) )
            .put( "service-type", sfb( ComponentIds.name( ) ) )
            .put( "service-group", ssfb( new Function<ComponentId, Set<String>>( ) {
              @Nullable
              @Override
              public Set<String> apply( final ComponentId componentId ) {
                return Sets.newHashSet( Iterables.transform(
                    ServiceGroups.listMembership( componentId ),
                    ComponentIds.name( ) ) );
              }
            } ) )
            .put( "service-group-member", bfb( new Function<ComponentId, Boolean>( ) {
              @Override
              public Boolean apply( final ComponentId componentId ) {
                return !ServiceGroups.listMembership( componentId ).isEmpty( );
              }
            } ) )
            .put( "certificate-usage", ssfb( new Function<ComponentId, Set<String>>( ) {
              @Override
              public Set<String> apply( final ComponentId componentId ) {
                return componentId.getCertificateUsages( );
              }
            } ) )
            .build( );

    /**
     * Map of filter names to service configuration filter builders
     */
    private static final Map<String,Function<String,Predicate<ServiceConfiguration>>> serviceConfigurationFilterBuilders =
        ImmutableMap.<String,Function<String,Predicate<ServiceConfiguration>>>builder( )
        .put( "host", sfb( new Function<ServiceConfiguration, String>( ) {
          @Override
          public String apply( final ServiceConfiguration serviceConfiguration ) {
            return serviceConfiguration.getHostName( );
          }
        } ) )
        .put( "state", new Function<String,Predicate<ServiceConfiguration>>( ){
          @Nullable
          @Override
          public Predicate<ServiceConfiguration> apply( final String filter ) {
            return state( Component.State.valueOf( filter.toUpperCase( ) ) );
          }
        } )
        .put( "partition", sfb( new Function<ServiceConfiguration, String>( ) {
          @Override
          public String apply( final ServiceConfiguration serviceConfiguration ) {
            return serviceConfiguration.getPartition( );
          }
        } ) )
        .build( );

    static Predicate<ServiceConfiguration> publicService( ) {
      return new Predicate<ServiceConfiguration>( ) {
        @Override
        public boolean apply( final ServiceConfiguration input ) {
          return input.getComponentId( ).isPublicService( );
        }
      };
    }

    static Predicate<ServiceConfiguration> partition( final String partition ) {
      return new Predicate<ServiceConfiguration>( ) {
        @Override
        public boolean apply( final ServiceConfiguration input ) {
          return ( partition == null ) || partition.equals( input.getPartition( ) );
        }
      };
    }

    static Predicate<ServiceConfiguration> host( final String host ) {
      return new Predicate<ServiceConfiguration>( ) {
        @Override
        public boolean apply( final ServiceConfiguration input ) {
          return ( host == null ) || host.equals( input.getHostName( ) );
        }
      };
    }

    static Predicate<ServiceConfiguration> name( final List<String> names ) {
      return new Predicate<ServiceConfiguration>( ) {
        @Override
        public boolean apply( final ServiceConfiguration input ) {
          return ( names == null ) || names.isEmpty( ) || names.contains( input.getName() ) || names.contains( input.getFullName().toString() );
        }
      };
    }

    static Predicate<ServiceConfiguration> state( final Component.State state ) {
      return new Predicate<ServiceConfiguration>( ) {
        @Override
        public boolean apply( final ServiceConfiguration input ) {
          try {
            return input.lookupState( ).equals( state );
          } catch ( final Exception ex ) {
            return false;
          }
        }
      };
    }

    static Predicate<Component> componentType( final ComponentId compId ) {
      return new Predicate<Component>( ) {
        @Override
        public boolean apply( final Component input ) {
          return Empyrean.class.equals( compId.getClass( ) ) || input.getComponentId( ).equals( compId );
        }
      };
    }

    static Predicate<ServiceConfiguration> listAllOrInternal( final Boolean listAllArg, final Boolean listUserServicesArg, final Boolean listInternalArg ) {
      final boolean listAll = Boolean.TRUE.equals( listAllArg );
      final boolean listInternal = Boolean.TRUE.equals( listInternalArg );
      final boolean listUserServices = Boolean.TRUE.equals( listUserServicesArg );
      return new Predicate<ServiceConfiguration>( ) {
        @Override
        public boolean apply( final ServiceConfiguration input ) {
          if ( listAll ) {
            return true;
          } else if ( input.getComponentId( ).isDistributedService( ) || Empyrean.class.equals( input.getComponentId( ).getClass() ) ) {
            return true;
          } else if ( input.getComponentId( ).isPublicService( ) ) {
            return Internets.testLocal( input.getHostName( ) );
          } else if ( input.getComponentId( ).isAdminService( ) && listUserServices ) {
            return Internets.testLocal( input.getHostName( ) );
          } else if ( input.getComponentId( ).isInternal( ) && listInternal ) {
            return Internets.testLocal( input.getHostName( ) );
          } else {
            return false;
          }
        }
      };
    }
  }

  @ServiceOperation( user = true, hostDispatch = true )
  public enum DescribeService implements Function<DescribeServicesType, DescribeServicesResponseType> {
    INSTANCE;

    @Override
    public DescribeServicesResponseType apply( final DescribeServicesType input ) {
      try {
        if ( !Contexts.lookup( ).isAdministrator() ) {
          return user( input );
        } else {
          return EmpyreanService.describeService( input );
        }
      } catch ( final Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }

    public DescribeServicesResponseType user(  final DescribeServicesType request ) {
      final DescribeServicesResponseType reply = request.getReply( );
      /**
       * Only show public services to normal users.
       * Allow for filtering by component and state w/in that set.
       */
      final List<Predicate<ServiceConfiguration>> filters = new ArrayList<Predicate<ServiceConfiguration>>( ) {
        {
          this.add( Filters.from( request.getFilters( ) ) );
          this.add( Filters.publicService( ) );
          if ( request.getByPartition( ) != null ) {
            this.add( Filters.partition( request.getByPartition( ) ) );
          }
          if ( request.getByState( ) != null ) {
            this.add( Filters.state( Component.State.valueOf( request.getByState( ).toUpperCase( ) ) ) );
          }
        }
      };
      final Predicate<Component> componentFilter = ( Predicate<Component> ) (
          request.getByServiceType( ) != null
            ? Filters.componentType( ComponentIds.lookup( request.getByServiceType( ).toLowerCase( ) ) )
            : Predicates.alwaysTrue( ) );
      final Predicate<ServiceConfiguration> configPredicate = Predicates.and( filters );


      final Collection<ServiceConfiguration> replyConfigs = Lists.newArrayList();
      for ( final Component comp : Iterables.filter( Components.list( ), componentFilter ) ) {
        replyConfigs.addAll( Collections2.filter( comp.services( ), configPredicate ) );
      }
      final ImmutableList<ServiceConfiguration> sortedReplyConfigs = ServiceOrderings.defaultOrdering().immutableSortedCopy( replyConfigs );
      final Collection<ServiceStatusType> replyStatuses = Collections2.transform( sortedReplyConfigs, ServiceConfigurations.asServiceStatus( false, false ) );
      reply.getServiceStatuses().addAll( replyStatuses );
      return reply;
    }

  }


  public static DescribeServicesResponseType describeService( final DescribeServicesType request ) {
    final DescribeServicesResponseType reply = request.getReply( );
    Topology.touch( request );
    if ( request.getServices( ).isEmpty( ) ) {
      final ComponentId compId = ( request.getByServiceType( ) != null )
        ? ComponentIds.lookup( request.getByServiceType( ).toLowerCase( ) )
        : Empyrean.INSTANCE;
      final boolean showEventStacks = Boolean.TRUE.equals( request.getShowEventStacks( ) );
      final boolean showEvents = Boolean.TRUE.equals( request.getShowEvents( ) ) || showEventStacks;

      final Function<ServiceConfiguration, ServiceStatusType> transformToStatus = ServiceConfigurations.asServiceStatus( showEvents, showEventStacks );
      final List<Predicate<ServiceConfiguration>> filters = new ArrayList<Predicate<ServiceConfiguration>>( ) {
        {
          this.add( Filters.from( request.getFilters( ) ) );
          if ( request.getByPartition( ) != null ) {
            Partitions.exists( request.getByPartition( ) );
            this.add( Filters.partition( request.getByPartition( ) ) );
          }
          if ( request.getByState( ) != null ) {
            final Component.State stateFilter = Component.State.valueOf( request.getByState( ).toUpperCase( ) );
            this.add( Filters.state( stateFilter ) );
          }
          if ( !request.getServiceNames( ).isEmpty( ) ) {
            this.add( Filters.name( request.getServiceNames( ) ) );
          }
          this.add( Filters.host( request.getByHost( ) ) );
          this.add( Filters.listAllOrInternal( request.getListAll( ), request.getListUserServices( ), request.getListInternal( ) ) );
        }
      };
      final Predicate<Component> componentFilter = Filters.componentType( compId );
      final Predicate<ServiceConfiguration> configPredicate = Predicates.and( filters );

      List<ServiceConfiguration> replyConfigs = Lists.newArrayList();
      for ( final Component comp : Components.list( ) ) {
        if ( componentFilter.apply( comp ) ) {
          Collection<ServiceConfiguration> acceptedConfigs = Collections2.filter( comp.services(), configPredicate );
          replyConfigs.addAll( acceptedConfigs );
        }
      }
      ImmutableList<ServiceConfiguration> sortedReplyConfigs = ServiceOrderings.defaultOrdering().immutableSortedCopy( replyConfigs );
      final Collection<ServiceStatusType> transformedReplyConfigs = Collections2.transform( sortedReplyConfigs, transformToStatus );
      reply.getServiceStatuses( ).addAll( transformedReplyConfigs );
    } else {
      for ( ServiceId s : request.getServices( ) ) {
        reply.getServiceStatuses( ).add( TypeMappers.transform( s, ServiceStatusType.class ) );
      }
    }
    return reply;
  }

  @ServiceOperation( user = true, hostDispatch = true )
  public enum DescribeServiceCertificates implements Function<DescribeServiceCertificatesType, DescribeServiceCertificatesResponseType> {
    INSTANCE;

    @Override
    public DescribeServiceCertificatesResponseType apply( final DescribeServiceCertificatesType request ) {
      final DescribeServiceCertificatesResponseType reply = request.getReply( );
      /**
       * Only show public services to normal users.
       * Allow for filtering by component and state w/in that set.
       */
      final Predicate<ServiceConfiguration> configPredicate = Filters.publicService( );
      final List<ServiceCertificateType> serviceCertificates = Lists.newArrayList( );
      final Predicate<Component> componentPredicate =
          Predicates.compose( Filters.componentFrom( request.getFilters( ) ), Components.componentId( ) );
      final Predicate<String> usagePredicate = Filters.usageFrom( request.getFilters( ) );
      for ( final Component comp : Iterables.filter( Components.list( ), componentPredicate ) ) {
        if ( Iterables.any( comp.services( ), Predicates.and( configPredicate, Filters.from( request.getFilters( ) ) ) ) ) {
          final ComponentId currentComponentId = comp.getComponentId( );
          final ServiceCertificateType serviceCertificate = new ServiceCertificateType( );
          final Set<String> usages = currentComponentId.getCertificateUsages( );
          for ( final String usage : Iterables.filter( usages, usagePredicate ) ) {
            final X509Certificate certificate = currentComponentId.getCertificate( usage );
            try {
              final byte[] certificateBytes = certificate.getEncoded( );
              serviceCertificate.setServiceType( comp.getName( ) );
              if ( "der".equals( request.getFormat( ) ) ) {
                serviceCertificate.setCertificateFormat( "der" );
                serviceCertificate.setCertificate( BaseEncoding.base64( ).encode( certificateBytes ) );
              } else {
                serviceCertificate.setCertificateFormat( "pem" );
                serviceCertificate.setCertificate( new String( PEMFiles.getBytes( certificate ), StandardCharsets.UTF_8 ) );
              }
              final Digest digest = Digest.forAlgorithm( request.getFingerprintDigest( ) ).or( Digest.SHA256 );
              serviceCertificate.setCertificateFingerprintDigest( digest.algorithm( ) );
              serviceCertificate.setCertificateFingerprint( BaseEncoding.base16( ).withSeparator( ":", 2 ).encode( digest.digestBinary( certificateBytes ) ) );
              serviceCertificate.setCertificateUsage( usage );
              serviceCertificates.add( serviceCertificate );
            } catch ( final CertificateEncodingException e ) {
              LOG.error( "Error processing service certificate", e );
            }
          }
        }
      }
      reply.getServiceCertificates( ).addAll( serviceCertificates );
      return reply;
    }

  }
}
