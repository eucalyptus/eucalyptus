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
package edu.ucsb.eucalyptus.admin.server;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.ClusterState;
import com.eucalyptus.cluster.VmTypes;
import com.eucalyptus.component.ComponentRegistrationHandler;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Dispatcher;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.config.DeregisterClusterType;
import com.eucalyptus.config.DeregisterComponentType;
import com.eucalyptus.config.DeregisterStorageControllerType;
import com.eucalyptus.config.DeregisterWalrusType;
import com.eucalyptus.config.RegisterClusterType;
import com.eucalyptus.config.RegisterComponentType;
import com.eucalyptus.config.RegisterStorageControllerType;
import com.eucalyptus.config.RegisterWalrusType;
import com.eucalyptus.config.StorageControllerConfiguration;
import com.eucalyptus.config.WalrusConfiguration;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.StorageProperties;
import com.eucalyptus.util.WalrusProperties;
import com.eucalyptus.vm.VmType;
import com.eucalyptus.ws.client.ServiceDispatcher;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gwt.user.client.rpc.SerializableException;
import edu.ucsb.eucalyptus.admin.client.ClusterInfoWeb;
import edu.ucsb.eucalyptus.admin.client.StorageInfoWeb;
import edu.ucsb.eucalyptus.admin.client.VmTypeWeb;
import edu.ucsb.eucalyptus.admin.client.WalrusInfoWeb;
import edu.ucsb.eucalyptus.msgs.ComponentProperty;
import edu.ucsb.eucalyptus.msgs.GetStorageConfigurationResponseType;
import edu.ucsb.eucalyptus.msgs.GetStorageConfigurationType;
import edu.ucsb.eucalyptus.msgs.GetWalrusConfigurationResponseType;
import edu.ucsb.eucalyptus.msgs.GetWalrusConfigurationType;
import edu.ucsb.eucalyptus.msgs.UpdateStorageConfigurationType;
import edu.ucsb.eucalyptus.msgs.UpdateWalrusConfigurationType;

public class RemoteInfoHandler {
  private static Logger LOG = Logger.getLogger( RemoteInfoHandler.class );
  
  public static synchronized void setClusterList( List<ClusterInfoWeb> newClusterList ) throws EucalyptusCloudException {
    //FIXME: Min/max vlans values should be updated
    List<ClusterConfiguration> clusterConfig = Lists.newArrayList( );
    for ( ClusterInfoWeb clusterWeb : newClusterList ) {
      try {
        ClusterConfiguration ccConfig = ServiceConfigurations.getConfiguration( ClusterConfiguration.class, clusterWeb.getName( ) );
        ccConfig.setMaxVlan( clusterWeb.getMaxVlans( ) );
        ccConfig.setMinVlan( clusterWeb.getMinVlans( ) );
        EntityWrapper.get( ccConfig ).mergeAndCommit( ccConfig );
      } catch ( Exception e ) {
        LOG.debug( e, e );
      }
      clusterConfig.add( new ClusterConfiguration( clusterWeb.getName( ) /**ASAP: FIXME: GRZE **/, clusterWeb.getName( ), clusterWeb.getHost( ), clusterWeb.getPort( ), clusterWeb.getMinVlans( ),
                                                   clusterWeb.getMaxVlans( ) ) );
    }
    updateClusterConfigurations( clusterConfig );
  }
  
  public static synchronized List<ClusterInfoWeb> getClusterList( ) throws EucalyptusCloudException {
    List<ClusterInfoWeb> clusterList = new ArrayList<ClusterInfoWeb>( );
    try {
      for ( ClusterConfiguration c : ServiceConfigurations.getConfigurations( ClusterConfiguration.class ) )
        clusterList.add( new ClusterInfoWeb( c.getName( ), c.getHostName( ), c.getPort( ), c.getMinVlan( ), c.getMaxVlan( ) ) );
    } catch ( Throwable e ) {
      LOG.debug( "Got an error while trying to retrieving storage controller configuration list", e );
    }
    return clusterList;
  }
  
  public static synchronized void setStorageList( List<StorageInfoWeb> newStorageList ) throws EucalyptusCloudException {
    List<StorageControllerConfiguration> storageControllerConfig = Lists.newArrayList( );
    List<Runnable> dispatchParameters = Lists.newArrayList( );
    for ( StorageInfoWeb storageControllerWeb : newStorageList ) {
      final StorageControllerConfiguration scConfig = new StorageControllerConfiguration( null /**ASAP: FIXME: GRZE **/, storageControllerWeb.getName( ), storageControllerWeb.getHost( ),
                                          storageControllerWeb.getPort( ) );
      final UpdateStorageConfigurationType updateStorageConfiguration = new UpdateStorageConfigurationType( );
      updateStorageConfiguration.setName( scConfig.getName( ) );
      updateStorageConfiguration.setStorageParams( convertProps( storageControllerWeb.getStorageParams( ) ) );
      dispatchParameters.add( new Runnable( ) {
        @Override
        public void run( ) {
          Dispatcher scDispatch = ServiceDispatcher.lookup( scConfig );
          try {
            scDispatch.send( updateStorageConfiguration );
          } catch ( Exception e ) {
            LOG.error( "Error sending update configuration message to storage controller: " + updateStorageConfiguration );
            LOG.error( "The storage controller's configuration may be out of sync!" );
            LOG.debug( e, e );
          }
        }} );
      storageControllerConfig.add( scConfig );
    }
    updateStorageControllerConfigurations( storageControllerConfig );
    for ( Runnable dispatch : dispatchParameters ) {
      dispatch.run( );
    }
  }
  
  public static synchronized List<StorageInfoWeb> getStorageList( ) throws EucalyptusCloudException {
    List<StorageInfoWeb> storageList = new ArrayList<StorageInfoWeb>( );
    for ( ClusterConfiguration cc : ServiceConfigurations.getConfigurations( ClusterConfiguration.class ) ) {
      try {
        if ( Internets.testLocal( cc.getHostName( ) ) && !Components.lookup( "storage" ).isRunningLocally( ) ) {
          storageList.add( StorageInfoWeb.DEFAULT_SC );
          continue;
        }
      } catch ( Exception e ) {
        LOG.debug( "Got an error while trying to retrieving storage controller configuration list", e );
      }
      StorageControllerConfiguration c;
      try {
        c = ServiceConfigurations.getConfiguration( StorageControllerConfiguration.class, cc.getName( ) );
        StorageInfoWeb scInfo = new StorageInfoWeb( c.getName( ), c.getHostName( ), c.getPort( ) );
        try {
          GetStorageConfigurationResponseType getStorageConfigResponse = RemoteInfoHandler.sendForStorageInfo( cc, c );
          if ( c.getName( ).equals( getStorageConfigResponse.getName( ) ) ) {
            scInfo.setStorageParams( convertParams( getStorageConfigResponse.getStorageParams( ) ) );
          } else {
            LOG.debug( "Unexpected storage controller name: " + getStorageConfigResponse.getName( ), new Exception( ) );
            LOG.debug( "Expected configuration for SC related to CC: " + LogUtil.dumpObject( c ) );
            LOG.debug( "Received configuration for SC related to CC: " + LogUtil.dumpObject( getStorageConfigResponse ) );
          }
        } catch ( Throwable e ) {
          LOG.debug( "Got an error while trying to communicate with remote storage controller", e );
        }
        storageList.add( scInfo );
      } catch ( Exception e1 ) {
        storageList.add( StorageInfoWeb.DEFAULT_SC );
      }
    }
    return storageList;
  }
  
  private static GetStorageConfigurationResponseType sendForStorageInfo( ClusterConfiguration cc, StorageControllerConfiguration c ) throws EucalyptusCloudException {
    GetStorageConfigurationType getStorageConfiguration = new GetStorageConfigurationType( c.getName( ) );
    Dispatcher scDispatch = ServiceDispatcher.lookup( c );
    GetStorageConfigurationResponseType getStorageConfigResponse = scDispatch.send( getStorageConfiguration );
    return getStorageConfigResponse;
  }
  
  public static synchronized void setWalrusList( List<WalrusInfoWeb> newWalrusList ) throws EucalyptusCloudException {
    List<WalrusConfiguration> walrusConfig = Lists.newArrayList( );
    for ( WalrusInfoWeb walrusControllerWeb : newWalrusList ) {
      walrusConfig.add( new WalrusConfiguration( walrusControllerWeb.getName( ), walrusControllerWeb.getHost( ), walrusControllerWeb.getPort( ) ) );
    }
    updateWalrusConfigurations( walrusConfig );
    
    for ( WalrusInfoWeb walrusInfoWeb : newWalrusList ) {
      UpdateWalrusConfigurationType updateWalrusConfiguration = new UpdateWalrusConfigurationType( );
      updateWalrusConfiguration.setName( WalrusProperties.NAME );
      updateWalrusConfiguration.setProperties(convertProps(walrusInfoWeb.getProperties()));
      Dispatcher scDispatch = ServiceDispatcher.lookupSingle( Components.lookup( "walrus" ) );
      scDispatch.send( updateWalrusConfiguration );
    }
  }
  
  public static synchronized List<WalrusInfoWeb> getWalrusList( ) throws EucalyptusCloudException {
    List<WalrusInfoWeb> walrusList = new ArrayList<WalrusInfoWeb>( );
    try {
      for ( WalrusConfiguration c : ServiceConfigurations.getConfigurations( WalrusConfiguration.class ) ) {
        GetWalrusConfigurationType getWalrusConfiguration = new GetWalrusConfigurationType( WalrusProperties.NAME );
        Dispatcher scDispatch = ServiceDispatcher.lookupSingle( Components.lookup( "walrus" ) );
        GetWalrusConfigurationResponseType getWalrusConfigResponse = scDispatch.send( getWalrusConfiguration );
        walrusList.add( new WalrusInfoWeb( c.getName( ), 
      		  c.getHostName( ), 
      		  c.getPort( ),
      		  convertParams(getWalrusConfigResponse.getProperties())));
      }
    } catch ( PersistenceException ex ) {
      LOG.error( ex , ex );
      throw new EucalyptusCloudException( ex );
    } catch ( NoSuchElementException ex ) {
      LOG.error( ex , ex );
      throw new EucalyptusCloudException( ex );
    }
    return walrusList;
  }
  
  public static List<VmTypeWeb> getVmTypes( ) {
    List<VmTypeWeb> ret = new ArrayList<VmTypeWeb>( );
    for ( VmType v : VmTypes.list( ) )
      ret.add( new VmTypeWeb( v.getName( ), v.getCpu( ), v.getMemory( ), v.getDisk( ) ) );
    return ret;
  }
  
  public static void setVmTypes( final List<VmTypeWeb> vmTypes ) throws SerializableException {
    Set<VmType> newVms = Sets.newTreeSet( );
    for ( VmTypeWeb vmw : vmTypes ) {
      newVms.add( new VmType( vmw.getName( ), vmw.getCpu( ), vmw.getDisk( ), vmw.getMemory( ) ) );
    }
    try {
      VmTypes.update( newVms );
    } catch ( EucalyptusCloudException e ) {
      throw new SerializableException( e.getMessage( ) );
    }
  }
  
  public static void updateClusterConfigurations( List<ClusterConfiguration> clusterConfigs ) throws EucalyptusCloudException {
    updateComponentConfigurations( ServiceConfigurations.getConfigurations( ClusterConfiguration.class ), clusterConfigs );
    ClusterState.trim( );
  }
  
  public static void updateStorageControllerConfigurations( List<StorageControllerConfiguration> storageControllerConfigs ) throws EucalyptusCloudException {
    updateComponentConfigurations( ServiceConfigurations.getConfigurations( StorageControllerConfiguration.class ), storageControllerConfigs );
  }
  
  public static void updateWalrusConfigurations( List<WalrusConfiguration> walrusConfigs ) throws EucalyptusCloudException {
    updateComponentConfigurations( ServiceConfigurations.getConfigurations( WalrusConfiguration.class ), walrusConfigs );
  }
  
  private static void updateComponentConfigurations( List componentConfigs, List newComponentConfigs ) throws EucalyptusCloudException {
    try {
      ArrayList<ComponentConfiguration> addComponents = new ArrayList<ComponentConfiguration>( );
      List<ComponentConfiguration> removeComponents = new ArrayList<ComponentConfiguration>( );
      for ( Object o : newComponentConfigs ) {
        ComponentConfiguration config = ( ComponentConfiguration ) o;
        if ( !componentConfigs.contains( config ) ) addComponents.add( config );
      }
      for ( Object o : componentConfigs ) {
        ComponentConfiguration config = ( ComponentConfiguration ) o;
        if ( !newComponentConfigs.contains( config ) ) removeComponents.add( config );
      }
      LOG.info( "Planning to updating configs with: " );
      LOG.info( "-> add: " + addComponents );
      LOG.info( "-> remove: " + removeComponents );
      for ( ComponentConfiguration config : removeComponents ) {
//        DeregisterComponentType regComponent = null;
//        if ( config instanceof StorageControllerConfiguration ) {
//          regComponent = new DeregisterStorageControllerType( );
//        } else if ( config instanceof WalrusConfiguration ) {
//          regComponent = new DeregisterWalrusType( );
//        } else if ( config instanceof ClusterConfiguration ) {
//          regComponent = new DeregisterClusterType( );
//        } else {
//          regComponent = new DeregisterComponentType( );
//        }
//        regComponent.setName( config.getName( ) );
        ComponentRegistrationHandler.deregister( Components.oneWhichHandles( config.getClass( ) ), 
                                                 config.getPartition( ), config.getHostName( ) );
      }
      for ( ComponentConfiguration config : addComponents ) {
//        RegisterComponentType regComponent = null;
//        if ( config instanceof StorageControllerConfiguration ) {
//          regComponent = new RegisterStorageControllerType( );
//        } else if ( config instanceof WalrusConfiguration ) {
//          regComponent = new RegisterWalrusType( );
//        } else if ( config instanceof ClusterConfiguration ) {
//          regComponent = new RegisterClusterType( );
//        } else {
//          regComponent = new RegisterComponentType( );
//        }
//        regComponent.setName( config.getName( ) );
//        regComponent.setHost( config.getHostName( ) );
//        regComponent.setPort( config.getPort( ) );
        ComponentRegistrationHandler.register( Components.oneWhichHandles( config.getClass( ) ), 
                                               config.getPartition( ), config.getName( ), config.getHostName( ), config.getPort( ) );
      }
    } catch ( Exception e ) {
      LOG.error( e, e );
      throw new EucalyptusCloudException( "Changing component configurations failed: " + e.getMessage( ), e );
    }
    
  }
  private static ArrayList<String> convertParams( ArrayList<ComponentProperty> properties ) {
    ArrayList<String> params = new ArrayList<String>( );
    for ( ComponentProperty property : properties ) {
      params.add( property.getType( ) );
      params.add( property.getDisplayName( ) );
      params.add( property.getValue( ) );
      params.add( property.getQualifiedName( ) );
    }
    return params;
  }
  
  private static ArrayList<ComponentProperty> convertProps( ArrayList<String> params ) {
    ArrayList<ComponentProperty> props = new ArrayList<ComponentProperty>( );
    for ( int i = 0; i < ( params.size( ) / 4 ); ++i ) {
      props.add( new ComponentProperty( params.get( 4 * i ), params.get( 4 * i + 1 ), params.get( 4 * i + 2 ), params.get( 4 * i + 3 ) ) );
    }
    return props;
  }
}
