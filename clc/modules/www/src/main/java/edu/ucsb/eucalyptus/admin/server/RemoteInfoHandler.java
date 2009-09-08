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
package edu.ucsb.eucalyptus.admin.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.config.StorageControllerConfiguration;
import com.eucalyptus.config.WalrusConfiguration;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.StorageProperties;
import com.eucalyptus.util.WalrusProperties;
import com.eucalyptus.ws.client.ServiceDispatcher;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gwt.user.client.rpc.SerializableException;

import edu.ucsb.eucalyptus.admin.client.ClusterInfoWeb;
import edu.ucsb.eucalyptus.admin.client.StorageInfoWeb;
import edu.ucsb.eucalyptus.admin.client.VmTypeWeb;
import edu.ucsb.eucalyptus.admin.client.WalrusInfoWeb;
import edu.ucsb.eucalyptus.cloud.cluster.VmTypes;
import edu.ucsb.eucalyptus.cloud.entities.VmType;
import edu.ucsb.eucalyptus.msgs.GetStorageConfigurationResponseType;
import edu.ucsb.eucalyptus.msgs.GetStorageConfigurationType;
import edu.ucsb.eucalyptus.msgs.GetWalrusConfigurationResponseType;
import edu.ucsb.eucalyptus.msgs.GetWalrusConfigurationType;
import edu.ucsb.eucalyptus.msgs.UpdateStorageConfigurationType;
import edu.ucsb.eucalyptus.msgs.UpdateWalrusConfigurationType;

public class RemoteInfoHandler {

	private static Logger LOG = Logger.getLogger( RemoteInfoHandler.class );

	public static synchronized void setClusterList( List<ClusterInfoWeb> newClusterList ) throws EucalyptusCloudException {		
		//TODO: Min/max vlans values should be updated
		List<ClusterConfiguration> clusterConfig = Lists.newArrayList( );
		for ( ClusterInfoWeb clusterWeb : newClusterList ) {
			clusterConfig.add( new ClusterConfiguration( clusterWeb.getName( ), clusterWeb.getHost( ), clusterWeb.getPort( ) ) );
		}
		Configuration.updateClusterConfigurations( clusterConfig );
	}

	public static synchronized List<ClusterInfoWeb> getClusterList( ) throws EucalyptusCloudException {
		List<ClusterInfoWeb> clusterList = new ArrayList<ClusterInfoWeb>( );
		//TODO: Min/max vlans values should be obtained
		for ( ClusterConfiguration c : Configuration.getClusterConfigurations( ) )
			clusterList.add( new ClusterInfoWeb( c.getName( ), c.getHostName( ), c.getPort( ), 10, 4096 ) );
		return clusterList;
	}

	public static synchronized void setStorageList( List<StorageInfoWeb> newStorageList ) throws EucalyptusCloudException {
		List<StorageControllerConfiguration> storageControllerConfig = Lists.newArrayList( );
		for ( StorageInfoWeb storageControllerWeb : newStorageList ) {
			storageControllerConfig.add( new StorageControllerConfiguration( storageControllerWeb.getName( ), storageControllerWeb.getHost( ), storageControllerWeb.getPort( ) ) );
		}
		Configuration.updateStorageControllerConfigurations( storageControllerConfig );

		for(StorageInfoWeb storageControllerWeb : newStorageList) {
			UpdateStorageConfigurationType updateStorageConfiguration = new UpdateStorageConfigurationType();
			updateStorageConfiguration.setName(storageControllerWeb.getName());
			updateStorageConfiguration.setMaxTotalVolumeSize(storageControllerWeb.getTotalVolumesSizeInGB());
			updateStorageConfiguration.setMaxVolumeSize(storageControllerWeb.getMaxVolumeSizeInGB());
			updateStorageConfiguration.setStorageInterface(storageControllerWeb.getStorageInterface());
			updateStorageConfiguration.setStorageRootDirectory(storageControllerWeb.getVolumesPath());
			updateStorageConfiguration.setZeroFillVolumes(storageControllerWeb.getZeroFillVolumes());
			ServiceDispatcher scDispatch = ServiceDispatcher.lookup(Component.storage, 
					storageControllerWeb.getHost());
			if(Component.eucalyptus.isLocal()) {
				updateStorageConfiguration.setName(StorageProperties.SC_LOCAL_NAME);
			}
			scDispatch.send(updateStorageConfiguration);
		}
	}

	public static synchronized List<StorageInfoWeb> getStorageList( ) throws EucalyptusCloudException {
		List<StorageInfoWeb> storageList = new ArrayList<StorageInfoWeb>( );
		for (StorageControllerConfiguration c : Configuration.getStorageControllerConfigurations()) {
			GetStorageConfigurationType getStorageConfiguration = new GetStorageConfigurationType(c.getName());
			if(Component.eucalyptus.isLocal()) {
				getStorageConfiguration.setName(StorageProperties.SC_LOCAL_NAME);
			}
			ServiceDispatcher scDispatch = ServiceDispatcher.lookup(Component.storage, 
					c.getHostName());
			GetStorageConfigurationResponseType getStorageConfigResponse = 
				scDispatch.send(getStorageConfiguration, GetStorageConfigurationResponseType.class);
			storageList.add(new StorageInfoWeb(c.getName(), 
					c.getHostName(), 
					c.getPort(), 
					getStorageConfigResponse.getStorageRootDirectory(), 
					getStorageConfigResponse.getMaxVolumeSize(), 
					getStorageConfigResponse.getMaxTotalVolumeSize(), 
					getStorageConfigResponse.getStorageInterface(), 
					getStorageConfigResponse.getZeroFillVolumes()));
		}
		return storageList;
	}

	public static synchronized void setWalrusList( List<WalrusInfoWeb> newWalrusList ) throws EucalyptusCloudException {
		List<WalrusConfiguration> walrusConfig = Lists.newArrayList( );
		for ( WalrusInfoWeb walrusControllerWeb : newWalrusList ) {
			walrusConfig.add( new WalrusConfiguration( walrusControllerWeb.getName( ), walrusControllerWeb.getHost( ), walrusControllerWeb.getPort( ) ) );
		}
		Configuration.updateWalrusConfigurations(walrusConfig );

		for(WalrusInfoWeb walrusInfoWeb : newWalrusList) {
			UpdateWalrusConfigurationType updateWalrusConfiguration = new UpdateWalrusConfigurationType();
			updateWalrusConfiguration.setName(WalrusProperties.NAME);
			updateWalrusConfiguration.setBucketRootDirectory(walrusInfoWeb.getBucketsRootDirectory());
			updateWalrusConfiguration.setMaxBucketsPerUser(walrusInfoWeb.getMaxBucketsPerUser());
			updateWalrusConfiguration.setMaxBucketSize(walrusInfoWeb.getMaxBucketSizeInMB());
			updateWalrusConfiguration.setImageCacheSize(walrusInfoWeb.getMaxCacheSizeInMB());
			updateWalrusConfiguration.setTotalSnapshotSize(walrusInfoWeb.getSnapshotsTotalInGB());
			ServiceDispatcher scDispatch = ServiceDispatcher.lookupSingle(Component.walrus); 
			scDispatch.send(updateWalrusConfiguration);
		}
	}

	public static synchronized List<WalrusInfoWeb> getWalrusList( ) throws EucalyptusCloudException {
		List<WalrusInfoWeb> walrusList = new ArrayList<WalrusInfoWeb>( );
		for (WalrusConfiguration c : Configuration.getWalrusConfigurations()) {
			GetWalrusConfigurationType getWalrusConfiguration = new GetWalrusConfigurationType(WalrusProperties.NAME);
			ServiceDispatcher scDispatch = ServiceDispatcher.lookupSingle(Component.walrus);
			GetWalrusConfigurationResponseType getWalrusConfigResponse = 
				scDispatch.send(getWalrusConfiguration, GetWalrusConfigurationResponseType.class);
			walrusList.add(new WalrusInfoWeb(c.getName(),
					c.getHostName(),
					c.getPort(),
					getWalrusConfigResponse.getBucketRootDirectory(),
					getWalrusConfigResponse.getMaxBucketsPerUser(),
					getWalrusConfigResponse.getMaxBucketSize(),
					getWalrusConfigResponse.getImageCacheSize(),
					getWalrusConfigResponse.getTotalSnapshotSize()));
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
}
