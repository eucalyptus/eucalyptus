/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentRegistrationHandler;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceBuilder;
import com.eucalyptus.component.ServiceBuilders;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.collect.Sets;
import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.notNullValue;

public class ConfigurationManager {
  public static Logger LOG                 = Logger.getLogger( ConfigurationManager.class );
  static String        CLUSTER_KEY_FSTRING = "cc-%s";
  static String        NODE_KEY_FSTRING    = "nc-%s";
  
  @TypeMapper
  public enum ComponentInfoMapper implements Function<ServiceConfiguration, ComponentInfoType> {
    INSTANCE;
    
    @Override
    public ComponentInfoType apply( final ServiceConfiguration input ) {
      return new ComponentInfoType( ) {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        {
          this.setType( input.getComponentId( ).name( ) );
          this.setPartition( input.getPartition( ) );
          this.setName( input.getName( ) );
          this.setHostName( input.getHostName( ) );
          this.setFullName( input.getFullName( ).toString( ) );
          try {
            this.setState( input.lookupState( ).toString( ) );
          } catch ( final Exception ex ) {
            this.setState( "n/a: " + ex.getMessage( ) );
          }
          this.setDetail( "" );
        }
      };
    }
    
  }
  
  public static RegisterComponentResponseType registerComponent( final RegisterComponentType request ) throws EucalyptusCloudException {
    final ServiceBuilder<? extends ServiceConfiguration> builder = ServiceBuilders.handles( request.getClass( ) );
    final ComponentId componentId = builder.getComponentId( );
    final RegisterComponentResponseType reply = request.getReply( );
    final String name = request.getName( );
    final String hostName = request.getHost( );
    final Integer port = request.getPort( );
    checkParam( "Name must not be null: " + request, name, notNullValue() );
    checkParam( "Hostname must not be null: " + request, hostName, notNullValue() );
    checkParam( "Port must not be null: " + request, port, notNullValue() );
    
    String partition = request.getPartition( );
    if ( !componentId.isPartitioned( ) ) {//TODO:GRZE: convert to @NotNull
      partition = componentId.getPartition( );
      LOG.error( "Unpartitioned component (" + componentId.getName( )
                 + ") is being registered w/o a partition.  Using fixed partition="
                 + partition
                 + " for request: "
                 + request );
    } else if ( componentId.isPartitioned( ) && ( partition == null ) ) {
      partition = name;
      LOG.error( "Partitioned component is being registered w/o a partition.  Using partition=name=" + partition
                 + " for request: "
                 + request );
    }
    try {
      reply.set_return( ComponentRegistrationHandler.register( componentId, partition, name, hostName, port ) );
    } catch ( final Throwable ex ) {
      //  throw new EucalyptusCloudException( "Component registration failed because: " + ex.getMessage( ), ex );
      reply.set_return( false );
      reply.setStatusMessage( ex.getMessage( ) );
      
    }
    return reply;
  }
  
  public static DeregisterComponentResponseType deregisterComponent( final DeregisterComponentType request ) throws EucalyptusCloudException {
    final ServiceBuilder<? extends ServiceConfiguration> builder = ServiceBuilders.handles( request.getClass( ) );
    final ComponentId componentId = builder.getComponentId( );
    final DeregisterComponentResponseType reply = ( DeregisterComponentResponseType ) request.getReply( );
    try {
      reply.set_return( ComponentRegistrationHandler.deregister( componentId, request.getName( ) ) );
    } catch ( final Throwable ex ) {
      //throw new EucalyptusCloudException( "Component deregistration failed because: " + ex.getMessage( ), ex );
      reply.set_return( false );
      reply.setStatusMessage( ex.getMessage( ) );
    }
    return reply;
  }
  
  private static final Set<String> attributes = Sets.newHashSet( "partition", "state" );
  
  public static ModifyComponentAttributeResponseType modify( final ModifyComponentAttributeType request ) throws EucalyptusCloudException {
    final ModifyComponentAttributeResponseType reply = request.getReply( );
    if ( !attributes.contains( request.getAttribute( ) ) ) {
      throw new EucalyptusCloudException( "Request to modify unknown attribute: " + request.getAttribute( ) );
    }
    final ServiceBuilder builder = ServiceBuilders.handles( request.getClass( ) );
    LOG.info( "Using builder: " + builder.getClass( ).getSimpleName( ) );
    LOG.error( "Nothing to do while processing: " + request );
    return reply;
  }
  
  public DescribeComponentsResponseType listComponents( final DescribeComponentsType request ) throws EucalyptusCloudException {
    final DescribeComponentsResponseType reply = ( DescribeComponentsResponseType ) request.getReply( );
    final List<ComponentInfoType> listConfigs = reply.getRegistered( );
    if ( DescribeComponentsType.class.equals( request.getClass( ) ) ) {
      for ( final Component c : Components.list( ) ) {
        if ( !c.hasLocalService( ) ) {
          listConfigs.add( new ComponentInfoType( ) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            {
              this.setType( c.getComponentId( ).name( ) );
              this.setPartition( c.getComponentId( ).getPartition( ) );
              this.setName( "" );
              this.setHostName( "" );
              this.setFullName( "" );
              this.setState( c.getState( ).toString( ) );
              this.setDetail( "" );
            }
          } );
        } else {
          final ServiceConfiguration config = c.getLocalServiceConfiguration( );
          final ComponentInfoType info = TypeMappers.transform( config, ComponentInfoType.class );
          if ( !Boolean.TRUE.equals( request.getVerbose( ) ) ) {
            info.setDetail( "" );
          }
          listConfigs.add( info );
        }
      }
    } else {
      final ServiceBuilder<? extends ServiceConfiguration> compId = ServiceBuilders.handles( request.getClass( ) );
      for ( final ServiceConfiguration config : ServiceConfigurations.list( compId.getComponentId( ).getClass( ) ) ) {
        final ComponentInfoType info = TypeMappers.transform( config, ComponentInfoType.class );
        if ( !Boolean.TRUE.equals( request.getVerbose( ) ) ) {
          info.setDetail( "" );
        }
        listConfigs.add( info );
      }
    }
    return reply;
  }
}
