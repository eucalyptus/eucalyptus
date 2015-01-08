/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.empyrean;

import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import com.eucalyptus.component.ServiceOrderings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
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

public class EmpyreanService {
  private static Logger LOG = Logger.getLogger( EmpyreanService.class );
  
  @ServiceOperation
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
  
  @ServiceOperation
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
  
  @ServiceOperation
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
  
  @ServiceOperation
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
  
  @ServiceOperation
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
  
  public static ServiceConfiguration findService( final String name ) {
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
        LOG.error( ex, ex );
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
  
  @ServiceOperation(user=true)
  public enum DescribeService implements Function<DescribeServicesType, DescribeServicesResponseType> {
    INSTANCE;
    
    @Override
    public DescribeServicesResponseType apply( final DescribeServicesType input ) {
      try {
        if ( !Contexts.lookup( ).getUser( ).isSystemAdmin( ) ) {
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
}
