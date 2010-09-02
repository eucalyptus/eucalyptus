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
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.component.ServiceBuilder;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.scripting.groovy.GroovyUtil;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.NetworkUtil;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.ComponentInfoType;
import edu.ucsb.eucalyptus.msgs.DeregisterComponentResponseType;
import edu.ucsb.eucalyptus.msgs.DeregisterComponentType;
import edu.ucsb.eucalyptus.msgs.DescribeComponentsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeComponentsType;
import edu.ucsb.eucalyptus.msgs.DescribeNodesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeNodesType;
import edu.ucsb.eucalyptus.msgs.NodeComponentInfoType;
import edu.ucsb.eucalyptus.msgs.RegisterComponentResponseType;
import edu.ucsb.eucalyptus.msgs.RegisterComponentType;

public class Configuration {
  static Logger         LOG                 = Logger.getLogger( Configuration.class );
  public static String DB_NAME             = "eucalyptus_config";
  static String         CLUSTER_KEY_FSTRING = "cc-%s";
  static String         NODE_KEY_FSTRING    = "nc-%s";

  private static Map<Class,ServiceBuilder<ComponentConfiguration>> builders = Maps.newConcurrentHashMap( );
  public static void addBuilder( Class c, ServiceBuilder b ) {
    builders.put( c, b );
  }
  
  public RegisterComponentResponseType registerComponent( RegisterComponentType request ) throws EucalyptusCloudException {
    RegisterComponentResponseType reply = ( RegisterComponentResponseType ) request.getReply( );
    reply.set_return( false );
    String name = request.getName( );
    String hostName = request.getHost();
    Integer port = request.getPort( );
    ServiceBuilder builder = builders.get( request.getClass( ) );
    if( !builder.checkAdd( name, hostName, port ) ) {
      reply.set_return(true);
      return reply;
    }
    ServiceConfiguration newComponent = builder.add( name, hostName, port );
    builder.getComponent( ).buildService( newComponent );
    builder.getComponent( ).startService( newComponent );
    reply.set_return( true );
    return reply;
  }
    
  public DeregisterComponentResponseType deregisterComponent( DeregisterComponentType request ) throws EucalyptusCloudException {
    DeregisterComponentResponseType reply = ( DeregisterComponentResponseType ) request.getReply( );
    reply.set_return( false );
    ServiceBuilder builder = builders.get( request.getClass( ) );
    if( !builder.checkRemove( request.getName( ) ) ) {
      return reply;
    }
    ServiceConfiguration conf;
    try {
      conf = builder.lookupByName( request.getName( ) );
      builder.remove( conf );
      builder.getComponent( ).removeService( conf );
//      builder.fireStop( conf );
      reply.set_return( true );
    } catch( EucalyptusCloudException e ) {
      throw e;
    } catch ( Exception e ) {
      LOG.debug( e, e );
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
    for( ComponentConfiguration conf : builders.get( request.getClass( ) ).list( ) ) {
      listConfigs.add( new ComponentInfoType( conf.getName( ), conf.getHostName( ) ) );
    }
    return reply;
  }
  
  public static List<ClusterConfiguration> getClusterConfigurations( ) throws EucalyptusCloudException {
    EntityWrapper<ClusterConfiguration> db = ServiceConfigurations.getEntityWrapper( );
    try {
      List<ClusterConfiguration> componentList = db.query( new ClusterConfiguration( ) );
      for ( ClusterConfiguration cc : componentList ) {
        if ( cc.getMinVlan( ) == null ) cc.setMinVlan( 10 );
        if ( cc.getMaxVlan( ) == null ) cc.setMaxVlan( 4095 );
      }
      db.commit( );
      return componentList;
    } catch ( Exception e ) {
      db.rollback( );
      LOG.error( e, e );
      throw new EucalyptusCloudException( e );
    }
  }
  
  public static List<StorageControllerConfiguration> getStorageControllerConfigurations( ) throws EucalyptusCloudException {
    EntityWrapper<StorageControllerConfiguration> db = ServiceConfigurations.getEntityWrapper( );
    try {
      List<StorageControllerConfiguration> componentList = db.query( new StorageControllerConfiguration( ) );
      db.commit( );
      return componentList;
    } catch ( Exception e ) {
      db.rollback( );
      LOG.error( e, e );
      throw new EucalyptusCloudException( e );
    }
  }
  
  public static List<WalrusConfiguration> getWalrusConfigurations( ) throws EucalyptusCloudException {
    EntityWrapper<WalrusConfiguration> db = ServiceConfigurations.getEntityWrapper( );
    try {
      List<WalrusConfiguration> componentList = db.query( new WalrusConfiguration( ) );
      db.commit( );
      return componentList;
    } catch ( Exception e ) {
      db.rollback( );
      LOG.error( e, e );
      throw new EucalyptusCloudException( e );
    }
  }
  
  public static StorageControllerConfiguration getStorageControllerConfiguration( String scName ) throws EucalyptusCloudException {
    List<StorageControllerConfiguration> scs = Configuration.getStorageControllerConfigurations( );
    for ( StorageControllerConfiguration sc : scs ) {
      if ( sc.getName( ).equals( scName ) ) {
        return sc;
      }
    }
    throw new NoSuchComponentException( StorageControllerConfiguration.class.getSimpleName( ) + " named " + scName );
  }
  
  public static WalrusConfiguration getWalrusConfiguration( String walrusName ) throws EucalyptusCloudException {
    List<WalrusConfiguration> walri = Configuration.getWalrusConfigurations( );
    for ( WalrusConfiguration w : walri ) {
      if ( w.getName( ).equals( walrusName ) ) {
        return w;
      }
    }
    throw new NoSuchComponentException( WalrusConfiguration.class.getSimpleName( ) + " named " + walrusName );
  }
  
  public static ClusterConfiguration getClusterConfiguration( String clusterName ) throws EucalyptusCloudException {
    List<ClusterConfiguration> clusters = Configuration.getClusterConfigurations( );
    for ( ClusterConfiguration c : clusters ) {
      if ( c.getName( ).equals( clusterName ) ) {
        return c;
      }
    }
    throw new NoSuchComponentException( ClusterConfiguration.class.getSimpleName( ) + " named " + clusterName );
  }
    
}
