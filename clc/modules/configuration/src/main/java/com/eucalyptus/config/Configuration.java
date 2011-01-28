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
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Service;
import com.eucalyptus.component.ServiceBuilder;
import com.eucalyptus.component.ServiceBuilderRegistry;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.scripting.groovy.GroovyUtil;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.ComponentInfoType;
import edu.ucsb.eucalyptus.msgs.DeregisterComponentResponseType;
import edu.ucsb.eucalyptus.msgs.DeregisterComponentType;
import edu.ucsb.eucalyptus.msgs.DescribeComponentsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeComponentsType;
import edu.ucsb.eucalyptus.msgs.DescribeNodesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeNodesType;
import edu.ucsb.eucalyptus.msgs.ModifyComponentAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ModifyComponentAttributeType;
import edu.ucsb.eucalyptus.msgs.NodeComponentInfoType;
import edu.ucsb.eucalyptus.msgs.RegisterComponentResponseType;
import edu.ucsb.eucalyptus.msgs.RegisterComponentType;

public class Configuration {
  static Logger         LOG                 = Logger.getLogger( Configuration.class );
  public static String DB_NAME             = "eucalyptus_config";
  static String         CLUSTER_KEY_FSTRING = "cc-%s";
  static String         NODE_KEY_FSTRING    = "nc-%s";

  public RegisterComponentResponseType registerComponent( RegisterComponentType request ) throws EucalyptusCloudException {
    ServiceBuilder builder = ServiceBuilderRegistry.get( request.getClass( ) );
    RegisterComponentResponseType reply = ( RegisterComponentResponseType ) request.getReply( );
    String name = request.getName( );
    String partition = request.getPartition( );
    String hostName = request.getHost();
    Integer port = request.getPort( );
    reply.set_return( register( builder, partition != null ? partition : name, name, hostName, port ) );
    return reply;
  }

  private boolean register( ServiceBuilder builder, String partition, String name, String hostName, Integer port ) throws ServiceRegistrationException {
    LOG.info( "Using builder: " + builder.getClass( ).getSimpleName( ) + " for: " + name + "@" + hostName + ":" + port );
    if( !builder.checkAdd( null, name, hostName, port ) ) {
      LOG.info( builder.getClass( ).getSimpleName( ) + ": checkAdd failed." );
      return false;
    }
    try {
      ServiceConfiguration newComponent = builder.add( partition, name, hostName, port );
      try {
        builder.getComponent( ).loadService( newComponent );
      } catch ( Exception ex ) {
        LOG.error( ex , ex );
      }
      try {
        builder.getComponent( ).startService( newComponent );
      } catch ( Exception ex ) {
        LOG.error( ex , ex );
      }
      try {
        builder.getComponent( ).enableService( newComponent );
      } catch ( Exception ex ) {
        LOG.error( ex , ex );
      }
      return true;
    } catch ( Throwable e ) {
      LOG.info( builder.getClass( ).getSimpleName( ) + ": add failed." );
      LOG.info( e.getMessage( ) );
      LOG.error( e, e );
      throw new ServiceRegistrationException( builder.getClass( ).getSimpleName( ) + ": add failed with message: " + e.getMessage( ), e );
    }
  }
  
  public DeregisterComponentResponseType deregisterComponent( DeregisterComponentType request ) throws EucalyptusCloudException {
    ServiceBuilder builder = ServiceBuilderRegistry.get( request.getClass( ) );
    DeregisterComponentResponseType reply = ( DeregisterComponentResponseType ) request.getReply( );
    reply.set_return( deregister( request.getPartition( ) != null ? request.getPartition( ) : request.getName( ), request.getName( ), builder ) );
    return reply;
  }

  private boolean deregister( String partition, String name, ServiceBuilder builder ) throws ServiceRegistrationException, EucalyptusCloudException {
    LOG.info( "Using builder: " + builder.getClass( ).getSimpleName( ) );
    try {
      if( !builder.checkRemove( partition, name ) ) {
        LOG.info( builder.getClass( ).getSimpleName( ) + ": checkRemove failed." );
        throw new ServiceRegistrationException( builder.getClass( ).getSimpleName( ) + ": checkRemove returned false.  It is unsafe to currently deregister, please check the logs for additional information." );
      }
    } catch ( Exception e ) {
      LOG.info( builder.getClass( ).getSimpleName( ) + ": checkRemove failed." );
      throw new ServiceRegistrationException( builder.getClass( ).getSimpleName( ) + ": checkRemove failed with message: " + e.getMessage( ), e );
    }
    ServiceConfiguration conf;
    try {
      conf = builder.lookupByName( name );
    } catch ( ServiceRegistrationException e ) {
      LOG.info( builder.getClass( ).getSimpleName( ) + ": lookupByName failed." );
      LOG.error( e, e );
      throw e;
    }
    try {
      try {
        builder.getComponent( ).disableService( conf );
      } catch ( Throwable ex ) {
        LOG.error( ex , ex );
      }
      try {
        builder.getComponent( ).stopService( conf );
      } catch ( Throwable ex ) {
        LOG.error( ex , ex );
      }
      try {
        builder.getComponent( ).destroyService( conf );
      } catch ( Throwable ex ) {
        LOG.error( ex, ex );
      }
      builder.remove( conf );
      return true;
    } catch ( Throwable e ) {
      LOG.info( builder.getClass( ).getSimpleName( ) + ": remove failed." );
      LOG.info( e.getMessage( ) );
      LOG.error( e, e );
      throw new ServiceRegistrationException( builder.getClass( ).getSimpleName( ) + ": remove failed with message: " + e.getMessage( ), e );
    }
  }
  
  private static final Set<String> attributes = Sets.newHashSet( "partition", "state" );
  public ModifyComponentAttributeResponseType modify( ModifyComponentAttributeType request ) throws EucalyptusCloudException {
    ModifyComponentAttributeResponseType reply = request.getReply( );
    if( !attributes.contains( request.getAttribute( ) ) ) {
      throw new EucalyptusCloudException( "Request to modify unknown attribute: " + request.getAttribute() );
    }
    ServiceBuilder builder = ServiceBuilderRegistry.get( request.getClass( ) );
    LOG.info( "Using builder: " + builder.getClass( ).getSimpleName( ) );
    if( "state".equals( request.getAttribute( ) ) ) {
      ServiceConfiguration conf;
      try {
        conf = builder.lookupByName( request.getName( ) );
      } catch ( ServiceRegistrationException e ) {
        LOG.info( builder.getClass( ).getSimpleName( ) + ": lookupByName failed." );
        LOG.error( e, e );
        throw e;
      }
      if( "enabled".startsWith( request.getValue( ).toLowerCase( ) ) ) {
        builder.getComponent( ).enableService( conf );
      } else if( "disabled".startsWith( request.getValue( ).toLowerCase( ) ) ) {
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
    if( DescribeComponentsType.class.equals( request.getClass( ) ) ) {
      for( Component c : Components.list( ) ) {
        for ( Service s : c.getServices( ) ) {
          ServiceConfiguration conf = s.getServiceConfiguration( );
          listConfigs.add( new ComponentInfoType( String.format( "%-15.15s", conf.getComponent( ).name( ).toUpperCase( ) ) + ( conf.getPartition( ) != null ?  conf.getPartition( ) : "-" ), 
                                                  conf.getName( ), conf.getHostName( ), s.getState( ).toString( ), "" ) );
          for( String d : s.getDetails( ) ) {
            listConfigs.add( new ComponentInfoType( String.format( "%-15.15s", conf.getComponent( ).name( ).toUpperCase( ) ) + ( conf.getPartition( ) != null ?  conf.getPartition( ) : "-" ), 
                                                    conf.getName( ), "detail", d, "" ) );
          }
        }        
      }
    } else {
      for( ServiceConfiguration conf : ServiceBuilderRegistry.get( request.getClass( ) ).list( ) ) {
        try {
          Service s = Components.lookup( conf );
          listConfigs.add( new ComponentInfoType( conf.getPartition( ), conf.getName( ), conf.getHostName( ), s.getState( ).toString( ), s.getDetails( ) ) );
        } catch ( NoSuchElementException ex ) {
          LOG.error( ex , ex );
        }
      }
    }
    return reply;
  }
  
  public static StorageControllerConfiguration lookupSc( final String requestedZone ) throws EucalyptusCloudException {
    return getStorageControllerConfiguration( requestedZone );
    /*
    try {
      return getStorageControllerConfiguration( requestedZone );
    } catch ( Exception e ) {
      try {
        Group g = Groups.lookupGroup( requestedZone );
        for( Authorization a : g.getAuthorizations( ) ) {
          if( a instanceof AvailabilityZonePermission ) {
            try {
              return Configuration.getStorageControllerConfiguration( a.getValue( ) );
            } catch ( NoSuchComponentException ex ) {
            }
          }
        }
        return getStorageControllerConfiguration( g.getName( ) );
      } catch ( NoSuchGroupException ex1 ) {
        try {
          Group g = Iterables.find( Groups.listAllGroups( ), new Predicate<Group>( ) {
            @Override
            public boolean apply( Group arg0 ) {
              return Iterables.any( arg0.getAuthorizations( ), new Predicate<Authorization>( ) {
                @Override
                public boolean apply( Authorization arg0 ) {
                  return arg0.check( new AvailabilityZonePermission( requestedZone ) );
                }
              } );
            }
          } );
          return getStorageControllerConfiguration( g.getName( ) );
        } catch ( Exception ex ) {
          throw new EucalyptusCloudException( "Storage services are not available for the requested availability zone: " + requestedZone );
        }
      } catch ( Throwable ex ) {
        LOG.error( ex, ex );
        throw new EucalyptusCloudException( "Storage services are not available for the requested availability zone: " + requestedZone, ex );
      }
    }
    */
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
  
  public static List<VMwareBrokerConfiguration> getVMwareBrokerConfigurations( ) throws EucalyptusCloudException {
    EntityWrapper<VMwareBrokerConfiguration> db = ServiceConfigurations.getEntityWrapper( );
    try {
      List<VMwareBrokerConfiguration> componentList = db.query( new VMwareBrokerConfiguration( ) );
      db.commit( );
      return componentList;
    } catch ( Exception e ) {
      db.rollback( );
      LOG.error( e, e );
      throw new EucalyptusCloudException( e );
    }
  }
  
  public static List<ArbitratorConfiguration> getArbitratorConfigurations( ) throws EucalyptusCloudException {
	EntityWrapper<ArbitratorConfiguration> db = ServiceConfigurations.getEntityWrapper( );
    try {
      List<ArbitratorConfiguration> componentList = db.query( new ArbitratorConfiguration( ) );
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
