package com.eucalyptus.empyrean.configuration;

/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * 
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
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

import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.common.base.Throwables;
import org.apache.log4j.Logger;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceBuilder;
import com.eucalyptus.component.ServiceBuilders;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.annotation.Description;
import com.eucalyptus.component.annotation.ServiceOperation;
import com.eucalyptus.component.events.ServiceEvents;
import com.eucalyptus.component.groups.ServiceGroup;
import com.eucalyptus.component.groups.ServiceGroupBuilder;
import com.eucalyptus.component.groups.ServiceGroupConfiguration;
import com.eucalyptus.component.groups.ServiceGroups;
import com.eucalyptus.empyrean.ServiceId;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
public class ServiceRegistrationManager {
  private static Logger LOG = Logger.getLogger( ServiceRegistrationManager.class );
  
  @ServiceOperation
  public enum RegisterService implements Function<RegisterServiceType, RegisterServiceResponseType> {
    INSTANCE;
    
    public static Collection<Future<ServiceConfiguration>> register( String partition,
                                                                     String name,
                                                                     String hostName,
                                                                     Integer port,
                                                                     ComponentId componentId ) throws EucalyptusCloudException {
      final Collection<Future<ServiceConfiguration>> registered = Lists.newArrayList( );
      final ServiceBuilder<? extends ServiceConfiguration> builder = ServiceBuilders.lookup( componentId );
      final ServiceConfiguration config = builder.newInstance( partition, name, hostName, port );
      if ( ServiceGroups.isGroup( config ) ) {
        final ServiceGroupConfiguration groupConfig = ( ServiceGroupConfiguration ) config;
        final ServiceGroupBuilder<ServiceGroupConfiguration> groupBuilder = ( ServiceGroupBuilder<ServiceGroupConfiguration> ) builder;
        final Collection<ServiceConfiguration> configsToRegister = groupBuilder.onRegister( groupConfig );
        //do registrations
        final Future<ServiceConfiguration> groupFuture = ServiceEvents.registerFunction().apply( groupConfig );
        registered.add( groupFuture );
        try {//require that the group op succeeds before doing the members
          groupFuture.get();
          registered.addAll( Collections2.transform( configsToRegister, ServiceEvents.registerFunction() ) );
        } catch ( Exception e ) {
        }
      } else {
        registered.add( ServiceEvents.registerFunction( ).apply( config ) );
      }
      return registered;
    }
    
    @Override
    public RegisterServiceResponseType apply( final RegisterServiceType request ) {
      final RegisterServiceResponseType reply = request.getReply( );
      final String partition = request.getPartition( );
      final String name = request.getName( );
      final String hostName = request.getHost( );
      final Integer port = request.getPort( );
      try {
        /**
         * Check all the parameters.
         */
        final ComponentId componentId = ComponentIds.lookup( request.getType( ) );
        ServiceBuilders.lookup( componentId );//NOTE: this is an existence test which can fail w/ an exception.
        checkParam( "Name must not be null: " + request, name, notNullValue( ) );
        checkParam( "Hostname must not be null: " + request, hostName, notNullValue( ) );
        checkParam( "Port must not be null: " + request, port, notNullValue( ) );
        /**
         * Do the thing.
         */
        final Collection<Future<ServiceConfiguration>> configurations = register( partition, name, hostName, port, componentId );
        final Function<ServiceConfiguration, ServiceId> typeMapper = TypeMappers.lookup( ServiceConfiguration.class, ServiceId.class );
        for ( Future<ServiceConfiguration> regResult : configurations ) {
          try {
            reply.getRegisteredServices( ).add( typeMapper.apply( regResult.get( ) ) );
          } catch ( Exception e ) {
            reply.set_return( Boolean.FALSE );
            reply.getStatusMessages().add( Throwables.getRootCause( e ).getMessage() );
          }
        }
      } catch ( final Exception ex ) {
        reply.set_return( false );
        reply.getStatusMessages( ).add( "Failed to register "
                                        + request.getType( )
                                        + ": "
                                        + name
                                        + " at host: "
                                        + hostName
                                        + ":"
                                        + port
                                        + " because of : "
                                        + ex.getMessage( ) );
      }
      return reply;
    }
  }
  
  @ServiceOperation
  public enum DeregisterService implements Function<DeregisterServiceType, DeregisterServiceResponseType> {
    INSTANCE;
    
    public static Collection<Future<ServiceConfiguration>> deregister( ComponentId componentId, String name ) throws EucalyptusCloudException {
      final List<Future<ServiceConfiguration>> deregistered = Lists.newArrayList( );
      try {
        final ServiceBuilder<? extends ServiceConfiguration> builder = ServiceBuilders.lookup( componentId );
        final ServiceConfiguration config = ServiceConfigurations.lookupByName( componentId.getClass( ), name );
        if ( ServiceGroups.isGroup( config ) ) {
          final ServiceGroupConfiguration groupConfig = ( ServiceGroupConfiguration ) config;
          final ServiceGroupBuilder<ServiceGroupConfiguration> groupBuilder = ( ServiceGroupBuilder<ServiceGroupConfiguration> ) builder;
          final Collection<ServiceConfiguration> configsToRegister = groupBuilder.onRegister( groupConfig );
          //do registrations
          final Future<ServiceConfiguration> groupFuture = ServiceEvents.deregisterFunction().apply( groupConfig );
          deregistered.add( groupFuture );
          try {//require that the group op succeeds before doing the members
            groupFuture.get();
            deregistered.addAll( Collections2.transform( configsToRegister, ServiceEvents.deregisterFunction( ) ) );
          } catch ( Exception e ) {
            e.printStackTrace();
          }
        } else {
          deregistered.add( ServiceEvents.deregisterFunction( ).apply( config ) );
        }
      } catch ( Exception e ) {
        LOG.error( e );
        LOG.debug( e, e );
        throw e;
      }
      return deregistered;
    }
    
    @Override
    public DeregisterServiceResponseType apply( final DeregisterServiceType request ) {
      final DeregisterServiceResponseType reply = request.getReply( );
      final String name = request.getName( );
      try {
        /**
         * Check all the parameters.
         */
        checkParam( "Name must not be null: " + request, name, notNullValue( ) );
        ServiceConfiguration config = ServiceConfigurations.lookupByName( request.getName( ) );
        final ComponentId componentId = config.getComponentId( );
        ServiceBuilders.lookup( componentId );//NOTE: this is an existence test which can fail w/ an exception.
        Collection<Future<ServiceConfiguration>> configurations = deregister( componentId, name );
        final Function<ServiceConfiguration, ServiceId> typeMapper = TypeMappers.lookup( ServiceConfiguration.class, ServiceId.class );
        for ( Future<ServiceConfiguration> regResult : configurations ) {
          try {
            reply.getDeregisteredServices( ).add( typeMapper.apply( regResult.get( ) ) );
          } catch ( Exception e ) {
            reply.set_return( Boolean.FALSE );
            reply.getStatusMessages( ).add( Throwables.getRootCause( e ).getMessage() );
          }
        }
      } catch ( final Exception ex ) {
        reply.set_return( false );
        reply.getStatusMessages( ).add( "Failed to deregister "
                                        + request.getType( )
                                        + ": "
                                        + name
                                        + " because of : "
                                        + ex.getMessage( ) );
      }
      return reply;
    }
  }
  
  @ServiceOperation
  public enum DescribeAvailableComponents implements Function<DescribeAvailableServiceTypesType, DescribeAvailableServiceTypesResponseType> {
    INSTANCE;
    
    @Override
    public DescribeAvailableServiceTypesResponseType apply( final DescribeAvailableServiceTypesType input ) {
      try {
        DescribeAvailableServiceTypesResponseType reply = input.getReply( );
        for ( Class<? extends ComponentId> compId : ServiceBuilders.listRegisterableComponents( ) ) {
          final ComponentId componentId = ComponentIds.lookup( compId );
          Predicate<ComponentId> filterIn = new Predicate<ComponentId>( ) {
            @Override
            public boolean apply( ComponentId input ) {
              return input.isRegisterable( );
            }
          };
          if ( !input.getVerbose( ) && !filterIn.apply( componentId ) ) {
            continue;
          }
          final AvailableComponentInfo compInfo = new AvailableComponentInfo( );
          String description = componentId.getAwsServiceName( ) + " service implementation";
          if ( Ats.from( componentId ).has( Description.class ) ) {
            description = Ats.from( componentId ).get( Description.class ).value( );
          }
          if ( componentId.isPartitioned( ) && !componentId.isRegisterable( ) ) {
            description = "A sub component of " + componentId.getPartition( ) + " for: " + componentId.getCapitalizedName( );
          }
          /**
           * Info about component
           */
          compInfo.setComponentName( componentId.name( ) );
          compInfo.setComponentCapitalizedName( componentId.getCapitalizedName( ) );
          compInfo.setDescription( description );
          compInfo.setHasCredentials( componentId.hasCredentials( ) );
          //GRZE:YAWN: this above should be a component specific (read: @annotated) value and not a cobbled together generic string.
          
          /**
           * Info about its registration requirements
           */
          compInfo.setRegisterable( componentId.isRegisterable( ) );
          compInfo.setPartitioned( componentId.isPartitioned( ) );
          compInfo.setPublicApiService( componentId.isPublicService( ) );
          compInfo.setRequiresName( componentId.isPartitioned( ) );//GRZE: this condition will change going forward; they are not equivalent notions.
          
          /**
           * Info about service groups
           */
          if ( componentId instanceof ServiceGroup ) {
            ServiceGroup sg = ( ServiceGroup ) componentId;
            for ( ComponentId m : sg.list( ) ) {
              compInfo.getServiceGroupMembers( ).add( m.name( ) );
            }
          }
          for ( ComponentId m : ServiceGroups.listMembership( componentId ) ) {
            compInfo.getServiceGroups( ).add( m.name( ) );
          }
          reply.getAvailable( ).add( compInfo );
        }
        return reply;
      } catch ( final Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }
  }
  
}
