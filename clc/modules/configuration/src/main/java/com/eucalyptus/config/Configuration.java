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
import com.eucalyptus.component.ComponentRegistrationHandler;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Service;
import com.eucalyptus.component.ServiceBuilder;
import com.eucalyptus.component.ServiceBuilderRegistry;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.scripting.groovy.GroovyUtil;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Sets;

public class Configuration {
  public static Logger        LOG                 = Logger.getLogger( Configuration.class );
  public static String DB_NAME             = "eucalyptus_config";
  static String        CLUSTER_KEY_FSTRING = "cc-%s";
  static String        NODE_KEY_FSTRING    = "nc-%s";
  
  public RegisterComponentResponseType registerComponent( RegisterComponentType request ) throws EucalyptusCloudException {
    Component component = Components.oneWhichHandles( request.getClass( ) );
    RegisterComponentResponseType reply = ( RegisterComponentResponseType ) request.getReply( );
    String name = request.getName( );
    String partition = request.getPartition( );
    String hostName = request.getHost( );
    Integer port = request.getPort( );
    reply.set_return( ComponentRegistrationHandler.register( component, partition, name, hostName, port ) );
    return reply;
  }
  
  public DeregisterComponentResponseType deregisterComponent( DeregisterComponentType request ) throws EucalyptusCloudException {
    Component component = Components.oneWhichHandles( request.getClass( ) );
    DeregisterComponentResponseType reply = ( DeregisterComponentResponseType ) request.getReply( );
    reply.set_return( ComponentRegistrationHandler.deregister( component, request.getPartition( ), request.getName( ) ) );
    return reply;
  }
    
  private static final Set<String> attributes = Sets.newHashSet( "partition", "state" );
  public ModifyComponentAttributeResponseType modify( ModifyComponentAttributeType request ) throws EucalyptusCloudException {
    ModifyComponentAttributeResponseType reply = request.getReply( );
    if ( !attributes.contains( request.getAttribute( ) ) ) {
      throw new EucalyptusCloudException( "Request to modify unknown attribute: " + request.getAttribute( ) );
    }
    Component component = Components.oneWhichHandles( request.getClass( ) );
    ServiceBuilder builder = component.getBuilder( );
    LOG.info( "Using builder: " + builder.getClass( ).getSimpleName( ) );
    if ( "state".equals( request.getAttribute( ) ) ) {
      ServiceConfiguration conf;
      try {
        conf = builder.lookupByName( request.getName( ) );
      } catch ( ServiceRegistrationException e ) {
        LOG.info( builder.getClass( ).getSimpleName( ) + ": lookupByName failed." );
        LOG.error( e, e );
        throw e;
      }
      if ( "enable".startsWith( request.getValue( ).toLowerCase( ) ) ) {
        builder.getComponent( ).enableService( conf );
      } else if ( "disable".startsWith( request.getValue( ).toLowerCase( ) ) ) {
        builder.getComponent( ).disableService( conf );
      }
    }
    return reply;
  }
  
  public DescribeNodesResponseType listComponents( DescribeNodesType request ) throws EucalyptusCloudException {
    DescribeNodesResponseType reply = ( DescribeNodesResponseType ) request.getReply( );
    reply.setRegistered( ( ArrayList<NodeComponentInfoType> ) GroovyUtil.evaluateScript( "describe_nodes" ) );
    return reply;
  }
  
  public DescribeComponentsResponseType listComponents( DescribeComponentsType request ) throws EucalyptusCloudException {
    DescribeComponentsResponseType reply = ( DescribeComponentsResponseType ) request.getReply( );
    List<ComponentInfoType> listConfigs = reply.getRegistered( );
    if ( DescribeComponentsType.class.equals( request.getClass( ) ) ) {
      for ( Component c : Components.list( ) ) {
        if( c.lookupServices( ).isEmpty( ) ) {
          listConfigs.add( new ComponentInfoType( String.format( "%-15.15s", c.getComponentId( ).name( ).toUpperCase( ) ), c.getComponentId( ).name( ), "", c.getState( ).toString( ), "" ) );
        } else {
          for ( Service s : c.lookupServices( ) ) {
            ServiceConfiguration conf = s.getServiceConfiguration( );
            listConfigs.add( new ComponentInfoType( String.format( "%-15.15s", conf.getComponentId( ).name( ).toUpperCase( ) ) + ( conf.getPartition( ) != null
              ? conf.getPartition( )
              : "-" ),
                                                    conf.getName( ), conf.getHostName( ), s.getState( ).toString( ), "" ) );
            for ( String d : s.getDetails( ) ) {
              listConfigs.add( new ComponentInfoType( String.format( "%-15.15s", conf.getComponentId( ).name( ).toUpperCase( ) ) + ( conf.getPartition( ) != null
                ? conf.getPartition( )
                : "-" ),
                                                      conf.getName( ), "detail", d, "" ) );
            }
          }
        }
      }
    } else {
      for ( ServiceConfiguration conf : ServiceBuilderRegistry.handles( request.getClass( ) ).list( ) ) {
        try {
          Service s = Components.lookup( conf.getComponentId( ) ).lookupService( conf );
          listConfigs.add( new ComponentInfoType( conf.getPartition( ), conf.getName( ), conf.getHostName( ), s.getState( ).toString( ), s.getDetails( ) ) );
        } catch ( NoSuchElementException ex ) {
          LOG.error( ex, ex );
        }
      }
    }
    return reply;
  }
  
}
