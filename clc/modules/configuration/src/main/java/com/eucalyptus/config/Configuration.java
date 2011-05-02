/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.config;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentRegistrationHandler;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Service;
import com.eucalyptus.component.ServiceBuilder;
import com.eucalyptus.component.ServiceBuilderRegistry;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.scripting.groovy.GroovyUtil;
import com.eucalyptus.util.Assertions;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class Configuration {
  public static Logger LOG                 = Logger.getLogger( Configuration.class );
  static String        CLUSTER_KEY_FSTRING = "cc-%s";
  static String        NODE_KEY_FSTRING    = "nc-%s";
  
  public RegisterComponentResponseType registerComponent( final RegisterComponentType request ) throws EucalyptusCloudException {
    final Component component = Components.oneWhichHandles( request.getClass( ) );
    final ComponentId componentId = component.getComponentId( );
    final RegisterComponentResponseType reply = ( RegisterComponentResponseType ) request.getReply( );
    final String name = request.getName( );
    final String hostName = request.getHost( );
    final Integer port = request.getPort( );
    Assertions.assertNotNull( name, "Name must not be null: " + request );
    Assertions.assertNotNull( hostName, "Hostname must not be null: " + request );
    Assertions.assertNotNull( port, "Port must not be null: " + request );
    
    String partition = request.getPartition( );
    if ( !component.getComponentId( ).isPartitioned( ) ) {//TODO:GRZE: convert to @NotNull
      partition = componentId.getPartition( );
      LOG.error( "Unpartitioned component (" + componentId.getFullName( ) + ") is being registered w/o a partition.  Using fixed partition=" + partition
                 + " for request: " + request );
    } else if ( component.getComponentId( ).isPartitioned( ) && ( partition == null ) ) {
      partition = name;
      LOG.error( "Partitioned component is being registered w/o a partition.  Using partition=name=" + partition + " for request: " + request );
    }
    try {
      reply.set_return( ComponentRegistrationHandler.register( component, partition, name, hostName, port ) );
    } catch ( final Throwable ex ) {
      throw new EucalyptusCloudException( "Component registration failed because: " + ex.getMessage( ), ex );
    }
    return reply;
  }
  
  public DeregisterComponentResponseType deregisterComponent( final DeregisterComponentType request ) throws EucalyptusCloudException {
    final Component component = Components.oneWhichHandles( request.getClass( ) );
    final DeregisterComponentResponseType reply = ( DeregisterComponentResponseType ) request.getReply( );
    try {
      reply.set_return( ComponentRegistrationHandler.deregister( component, request.getPartition( ), request.getName( ) ) );
    } catch ( final Throwable ex ) {
      throw new EucalyptusCloudException( "Component deregistration failed because: " + ex.getMessage( ), ex );
    }
    return reply;
  }
  
  public DescribeNodesResponseType listComponents( final DescribeNodesType request ) throws EucalyptusCloudException {
    final DescribeNodesResponseType reply = ( DescribeNodesResponseType ) request.getReply( );
    reply.setRegistered( ( ArrayList<NodeComponentInfoType> ) GroovyUtil.evaluateScript( "describe_nodes" ) );
    return reply;
  }
  
  private static final Set<String> attributes = Sets.newHashSet( "partition", "state" );
  
  public ModifyComponentAttributeResponseType modify( final ModifyComponentAttributeType request ) throws EucalyptusCloudException {
    final ModifyComponentAttributeResponseType reply = request.getReply( );
    if ( !attributes.contains( request.getAttribute( ) ) ) {
      throw new EucalyptusCloudException( "Request to modify unknown attribute: " + request.getAttribute( ) );
    }
    final Component component = Components.oneWhichHandles( request.getClass( ) );
    final ServiceBuilder builder = component.getBuilder( );
    LOG.info( "Using builder: " + builder.getClass( ).getSimpleName( ) );
    if ( "state".equals( request.getAttribute( ) ) ) {
      ServiceConfiguration conf;
      try {
        conf = builder.lookupByName( request.getName( ) );
      } catch ( final ServiceRegistrationException e ) {
        LOG.info( builder.getClass( ).getSimpleName( ) + ": lookupByName failed." );
        LOG.error( e, e );
        throw e;
      }
      if ( "enable".startsWith( request.getValue( ).toLowerCase( ) ) ) {
        try {
          builder.getComponent( ).enableTransition( conf ).get( );
        } catch ( final Exception ex ) {
          LOG.error( ex, ex );
          throw new EucalyptusCloudException( ex.getMessage( ), ex );
        }
      } else if ( "disable".startsWith( request.getValue( ).toLowerCase( ) ) ) {
        builder.getComponent( ).disableService( conf );
      }
    }
    return reply;
  }
  
  public DescribeComponentsResponseType listComponents( final DescribeComponentsType request ) throws EucalyptusCloudException {
    final DescribeComponentsResponseType reply = ( DescribeComponentsResponseType ) request.getReply( );
    final List<ComponentInfoType> listConfigs = reply.getRegistered( );
    if ( DescribeComponentsType.class.equals( request.getClass( ) ) ) {
      for ( final Component c : Components.list( ) ) {
        if ( !c.hasLocalService( ) ) {
          listConfigs.add( new ComponentInfoType( ) {
            {
              setType( c.getComponentId( ).name( ) );
              setPartition( c.getComponentId( ).getPartition( ) );
              setName( "" );
              setHostName( "" );
              setFullName( "" );
              setState( c.getState( ).toString( ) );
              setDetail( "" );
            }
          } );
        } else {
          final ServiceConfiguration config = c.getLocalServiceConfiguration( );
          listConfigs.add( new ComponentInfoType( ) {
            {
              setType( config.getComponentId( ).name( ) );
              setPartition( config.getPartition( ) );
              setName( config.getName( ) );
              setHostName( config.getHostName( ) );
              setFullName( config.getFullName( ).toString( ) );
              setState( config.lookupState( ).toString( ) );
              setDetail( config.lookupDetails( ).isEmpty( ) || !Boolean.TRUE.equals( request.getVerbose( ) )
                         ? ""
                         : config.lookupDetails( ).iterator( ).next( ).toString( ) );
            }
          } );
        }
      }
    } else {
      for ( final ServiceConfiguration config : ServiceBuilderRegistry.handles( request.getClass( ) ).list( ) ) {
        try {
          listConfigs.add( new ComponentInfoType( ) {
            {
              setType( config.getComponentId( ).name( ) );
              setPartition( config.getPartition( ) );
              setName( config.getName( ) );
              setHostName( config.getHostName( ) );
              setFullName( config.getFullName( ).toString( ) );
              setState( config.lookupState( ).toString( ) );
              setDetail( config.lookupDetails( ).isEmpty( ) || !Boolean.TRUE.equals( request.getVerbose( ) )
                ? ""
                : config.lookupDetails( ).iterator( ).next( ).toString( ) );
            }
          } );
        } catch ( final NoSuchElementException ex ) {
          LOG.error( ex, ex );
          listConfigs.add( new ComponentInfoType( ) {
            {
              setType( config.getComponentId( ).name( ) );
              setPartition( config.getPartition( ) );
              setName( config.getName( ) );
              setHostName( config.getHostName( ) );
              setFullName( config.getFullName( ).toString( ) );
              setState( config.lookupComponent( ).getState( ).toString( ) );
              setDetail( config.lookupDetails( ).isEmpty( ) || !Boolean.TRUE.equals( request.getVerbose( ) )
                         ? ""
                         : config.lookupDetails( ).iterator( ).next( ).toString( ) );
            }
          } );
        }
      }
    }
    return reply;
  }
}
