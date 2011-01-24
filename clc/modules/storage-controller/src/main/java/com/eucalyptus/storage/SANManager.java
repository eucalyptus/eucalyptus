/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 *
 * Author: Neil Soman neil@eucalyptus.com
 */

package com.eucalyptus.storage;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.ExecutionException;
import com.eucalyptus.util.StorageProperties;
import com.eucalyptus.util.WalrusProperties;

import edu.ucsb.eucalyptus.cloud.NoSuchEntityException;
import edu.ucsb.eucalyptus.cloud.entities.SANInfo;
import edu.ucsb.eucalyptus.cloud.entities.SANVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.StorageInfo;
import edu.ucsb.eucalyptus.msgs.ComponentProperty;
import edu.ucsb.eucalyptus.util.SystemUtil;

public class SANManager implements LogicalStorageManager {

	private SANProvider connectionManager;

	private static SANManager singleton;
	private static Logger LOG = Logger.getLogger(SANManager.class);

	public static LogicalStorageManager getInstance( ) {
		synchronized ( SANManager.class ) {
			if ( singleton == null ) {
				singleton = new SANManager( );
			}
		}
		return singleton;
	}

	public SANManager() {
		String sanProvider = "EquallogicProvider";
		if(System.getProperty("euca.disable.san") == null) {
			if(System.getProperty("ebs.san.provider") != null) {
				sanProvider = System.getProperty("ebs.san.provider");
			}
		}
		try {
			sanProvider = "com.eucalyptus.storage." + sanProvider;
			connectionManager = (SANProvider) ClassLoader.getSystemClassLoader().loadClass(sanProvider).newInstance();
		} catch (InstantiationException e) {
			LOG.error(e, e); 
		} catch (IllegalAccessException e) {
			LOG.error(e, e); 
		} catch (ClassNotFoundException e) {
			LOG.error(e, e); 
		}
	}

	@Override
	public void addSnapshot(String snapshotId) throws EucalyptusCloudException {
		finishVolume(snapshotId);
	}

	@Override
	public void checkPreconditions() throws EucalyptusCloudException {
		connectionManager.checkPreconditions();
	}

	@Override
	public void cleanSnapshot(String snapshotId) {
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();		
		try {
			//make sure it exists
			SANVolumeInfo volumeInfo = db.getUnique(new SANVolumeInfo(snapshotId));
		} catch(EucalyptusCloudException ex) {
			LOG.error("Unable to clean failed snapshot: " + snapshotId);
			return;
		} finally {
			db.commit();
		}

		if(connectionManager.deleteVolume(snapshotId)) {
			try {
				db = StorageProperties.getEntityWrapper();
				SANVolumeInfo snapInfo = db.getUnique(new SANVolumeInfo(snapshotId));
				db.delete(snapInfo);
			} catch(EucalyptusCloudException ex) {
				LOG.error("Unable to clean failed snapshot: " + snapshotId);
				return;
			} finally {
				db.commit();
			}
		}
	}

	@Override
	public void cleanVolume(String volumeId) {
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		try {
			//make sure it exists
			SANVolumeInfo volumeInfo = db.getUnique(new SANVolumeInfo(volumeId));
		} catch(EucalyptusCloudException ex) {
			LOG.error("Unable to clean failed volume: " + volumeId);
			return;
		} finally {
			db.commit();
		}
		if(connectionManager.deleteVolume(volumeId)) {
			db = StorageProperties.getEntityWrapper();
			try {
				SANVolumeInfo volumeInfo = db.getUnique(new SANVolumeInfo(volumeId));
				db.delete(volumeInfo);
			} catch(EucalyptusCloudException ex) {
				LOG.error("Unable to clean failed volume: " + volumeId);
				return;
			} finally {
				db.commit();
			}
		}
	}

	@Override
	public void configure() {
		//dummy init
		LOG.info(StorageInfo.getStorageInfo().getName());
		//configure provider
		connectionManager.configure();
	}

	@Override
	public List<String> createSnapshot(String volumeId, String snapshotId, Boolean shouldTransferSnapshots)
	throws EucalyptusCloudException {
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		int size = -1;
		List<String> returnValues = new ArrayList<String>();
		try {
			SANVolumeInfo volumeInfo = db.getUnique(new SANVolumeInfo(volumeId));
			size = volumeInfo.getSize();
		} catch (EucalyptusCloudException ex) {
			LOG.error("Unable to find volume: " + volumeId);			
		} finally {
			db.commit();
		}
		String iqn = connectionManager.createSnapshot(volumeId, snapshotId);
		if(iqn != null) {
			//login to target and return dev
			if(shouldTransferSnapshots) {
				String deviceName = connectionManager.connectTarget(iqn);
				returnValues.add(deviceName);
				returnValues.add(String.valueOf(size * WalrusProperties.G));
			}
			SANVolumeInfo snapInfo = new SANVolumeInfo(snapshotId, iqn, size);
			snapInfo.setSnapshotOf(volumeId);
			db = StorageProperties.getEntityWrapper();
			db.add(snapInfo);
			db.commit();
		} else {
			db.rollback();
			throw new EucalyptusCloudException("Unable to create snapshot: " + snapshotId + " from volume: " + volumeId);
		}
		return returnValues;
	}

	@Override
	public void createVolume(String volumeId, int size)
	throws EucalyptusCloudException {
		String iqn = connectionManager.createVolume(volumeId, size);
		if(iqn != null) {
			SANVolumeInfo volumeInfo = new SANVolumeInfo(volumeId, iqn, size);
			EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
			db.add(volumeInfo);
			db.commit();
		}
	}

	@Override
	public int createVolume(String volumeId, String snapshotId, int size)
	throws EucalyptusCloudException {
		int snapSize = -1;
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		try {
			SANVolumeInfo snapInfo = db.getUnique(new SANVolumeInfo(snapshotId));
			db.commit();
			snapSize = snapInfo.getSize();
			if(size == 0) {
				size = snapSize;
			}
		} catch(EucalyptusCloudException ex) {
			LOG.error(ex);
			db.rollback();
			throw ex;
		}
		String iqn = connectionManager.createVolume(volumeId, snapshotId, snapSize, size);
		if(iqn != null) {
			SANVolumeInfo volumeInfo = new SANVolumeInfo(volumeId, iqn, size);
			db = StorageProperties.getEntityWrapper();
			db.add(volumeInfo);
			db.commit();
		} else {
			db.rollback();
			throw new EucalyptusCloudException("Unable to create volume: " + volumeId);
		}
		return size;
	}

	@Override
	public void deleteSnapshot(String snapshotId)
	throws EucalyptusCloudException {
		if(connectionManager.deleteVolume(snapshotId)) {
			EntityWrapper<SANVolumeInfo>  db = StorageProperties.getEntityWrapper();
			try {
				SANVolumeInfo snapInfo = db.getUnique(new SANVolumeInfo(snapshotId));
				db.delete(snapInfo);
			} catch(EucalyptusCloudException ex) {
				LOG.error(ex);
				throw ex;
			} finally {
				db.commit();
			}
		}
	}

	@Override
	public void deleteVolume(String volumeId) throws EucalyptusCloudException {
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		try {
			SANVolumeInfo volumeInfo = db.getUnique(new SANVolumeInfo(volumeId));
		} catch(EucalyptusCloudException ex) {
			LOG.error(ex);
			throw new NoSuchEntityException(volumeId);
		} finally {
			db.commit();
		}
		if(connectionManager.deleteVolume(volumeId)) {
			db = StorageProperties.getEntityWrapper();
			SANVolumeInfo volumeInfo = db.getUnique(new SANVolumeInfo(volumeId));
			db.delete(volumeInfo);
			db.commit();
		}
	}

	@Override
	public int getSnapshotSize(String snapshotId)
	throws EucalyptusCloudException {
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		try {
			SANVolumeInfo snapInfo = db.getUnique(new SANVolumeInfo(snapshotId));
			return snapInfo.getSize();
		} catch(EucalyptusCloudException ex) {
			LOG.error(ex);
			throw ex;
		}	finally {		
			db.commit();
		}
	}

	@Override
	public String getVolumeProperty(String volumeId)
	throws EucalyptusCloudException {
		return connectionManager.getVolumeProperty(volumeId);
	}

	@Override
	public void initialize() {
	}

	@Override
	public void loadSnapshots(List<String> snapshotSet,
			List<String> snapshotFileNames) throws EucalyptusCloudException {
		// TODO Auto-generated method stub

	}

	@Override
	public List<String> prepareForTransfer(String snapshotId)
	throws EucalyptusCloudException {
		//Nothing to do here
		return new ArrayList<String>();
	}

	@Override
	public void reload() {
	}

	@Override
	public void startupChecks() {
		connectionManager.configure();
		connectionManager.checkConnection();
	}

	@Override
	public void finishVolume(String snapshotId) throws EucalyptusCloudException {
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		try {
			SANVolumeInfo snapInfo = db.getUnique(new SANVolumeInfo(snapshotId));
			String iqn = snapInfo.getIqn();
			db.commit();
			connectionManager.disconnectTarget(snapshotId, iqn);
		} catch(EucalyptusCloudException ex) {
			LOG.error(ex);
			db.rollback();
			throw new EucalyptusCloudException("Unable to finalize snapshot: " + snapshotId);
		} 		
	}

	@Override
	public String prepareSnapshot(String snapshotId, int sizeExpected)
	throws EucalyptusCloudException {
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		try {
			SANVolumeInfo volInfo = new SANVolumeInfo(snapshotId);
			volInfo.setScName(null);
			SANVolumeInfo foundVolInfo = db.getUnique(volInfo);
			SANVolumeInfo volumeInfo = new SANVolumeInfo(snapshotId, foundVolInfo.getIqn(), sizeExpected);
			volumeInfo.setStatus(foundVolInfo.getStatus());
			volumeInfo.setSnapshotOf(foundVolInfo.getSnapshotOf());
			volumeInfo.setStoreUser(foundVolInfo.getStoreUser());			
			db.add(volumeInfo);
			return null;
		} catch(EucalyptusCloudException e) {
		} finally {
			db.commit();
		}
		
		String iqn = connectionManager.createVolume(snapshotId, sizeExpected);
		if(iqn != null) {
			String deviceName = connectionManager.connectTarget(iqn);
			SANVolumeInfo snapInfo = new SANVolumeInfo(snapshotId, iqn, sizeExpected);
			db = StorageProperties.getEntityWrapper();
			db.add(snapInfo);
			db.commit();
			return deviceName;
		}
		return null;
	}

	@Override
	public ArrayList<ComponentProperty> getStorageProps() {
		ArrayList<ComponentProperty> componentProperties = null;
		ConfigurableClass configurableClass = StorageInfo.class.getAnnotation(ConfigurableClass.class);
		if(configurableClass != null) {
			String root = configurableClass.root();
			String alias = configurableClass.alias();
			componentProperties = (ArrayList<ComponentProperty>) PropertyDirectory.getComponentPropertySet(StorageProperties.NAME + "." + root, alias);
		}
		configurableClass = SANInfo.class.getAnnotation(ConfigurableClass.class);
		if(configurableClass != null) {
			String root = configurableClass.root();
			String alias = configurableClass.alias();
			if(componentProperties == null)
				componentProperties = (ArrayList<ComponentProperty>) PropertyDirectory.getComponentPropertySet(StorageProperties.NAME + "." + root, alias);
			else 
				componentProperties.addAll(PropertyDirectory.getComponentPropertySet(StorageProperties.NAME + "." + root, alias));
		}
		connectionManager.getStorageProps(componentProperties);
		return componentProperties;
	}

	@Override
	public void setStorageProps(ArrayList<ComponentProperty> storageProps) {
		for (ComponentProperty prop : storageProps) {
			try {
				ConfigurableProperty entry = PropertyDirectory.getPropertyEntry(prop.getQualifiedName());
				//type parser will correctly covert the value
				entry.setValue(prop.getValue());
			} catch (IllegalAccessException e) {
				LOG.error(e, e);
			}
		}
		connectionManager.setStorageProps(storageProps);
	}

	@Override
	public String getStorageRootDirectory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVolumePath(String volumeId)
	throws EucalyptusCloudException {
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		List<String> returnValues = new ArrayList<String>();
		try {
			SANVolumeInfo volumeInfo = db.getUnique(new SANVolumeInfo(volumeId));
			String iqn = volumeInfo.getIqn();
			String deviceName = connectionManager.connectTarget(iqn);
			return deviceName;
		} catch (EucalyptusCloudException ex) {
			LOG.error("Unable to find volume: " + volumeId);
			throw ex;
		} finally {
			db.commit();
		}
	}

	@Override
	public void importVolume(String volumeId, String volumePath, int size)
	throws EucalyptusCloudException {
		String eucaHomeDir = System.getProperty("euca.home");
		if(eucaHomeDir == null) {
			throw new EucalyptusCloudException("euca.home not set");
		}
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		try {
			db.getUnique(new SANVolumeInfo(volumeId));
			throw new EucalyptusCloudException("Volume " + volumeId + " already exists. Import failed.");
		} catch (EucalyptusCloudException ex) {
			//all okay. proceed with import
		} finally {
			db.commit();
		}
		String iqn = connectionManager.createVolume(volumeId, size);
		if(iqn != null) {
			String deviceName = connectionManager.connectTarget(iqn);
			//now copy
			try {
				SystemUtil.run(new String[]{eucaHomeDir + StorageProperties.EUCA_ROOT_WRAPPER, "dd", "if=" + volumePath, "of=" + deviceName, "bs=" + StorageProperties.blockSize});			
			} catch (ExecutionException e) {
				LOG.error(e);
				throw new EucalyptusCloudException(e);
			} finally {
				connectionManager.disconnectTarget(volumeId, iqn);
			}
			SANVolumeInfo volumeInfo = new SANVolumeInfo(volumeId, iqn, size);
			db = StorageProperties.getEntityWrapper();
			db.add(volumeInfo);
			db.commit();
		}		
	}

	@Override
	public String getSnapshotPath(String snapshotId)
	throws EucalyptusCloudException {
		return getVolumePath(snapshotId);
	}

	@Override
	public void importSnapshot(String snapshotId, String volumeId, String snapPath, int size)
	throws EucalyptusCloudException {
		String eucaHomeDir = System.getProperty("euca.home");
		if(eucaHomeDir == null) {
			throw new EucalyptusCloudException("euca.home not set");
		}
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		try {
			db.getUnique(new SANVolumeInfo(snapshotId));
			throw new EucalyptusCloudException("Snapshot " + snapshotId + " already exists. Import failed.");
		} catch (EucalyptusCloudException ex) {
			//all okay. proceed with import
		} finally {
			db.commit();
		}
		String iqn = connectionManager.createVolume(snapshotId, size);
		if(iqn != null) {
			String deviceName = connectionManager.connectTarget(iqn);
			//now copy
			try {
				SystemUtil.run(new String[]{eucaHomeDir + StorageProperties.EUCA_ROOT_WRAPPER, "dd", "if=" + snapPath, "of=" + deviceName, "bs=" + StorageProperties.blockSize});			
			} catch (ExecutionException e) {
				LOG.error(e);
				throw new EucalyptusCloudException(e);
			} finally {
				connectionManager.disconnectTarget(snapshotId, iqn);
			}
			SANVolumeInfo volumeInfo = new SANVolumeInfo(snapshotId, iqn, size);
			volumeInfo.setSnapshotOf(volumeId);
			db = StorageProperties.getEntityWrapper();
			db.add(volumeInfo);
			db.commit();
		}		
	}

	@Override
	public String attachVolume(String volumeId, String nodeIqn)
	throws EucalyptusCloudException {
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		try {
			SANVolumeInfo volumeInfo = db.getUnique(new SANVolumeInfo(volumeId));
		} catch (EucalyptusCloudException ex) {
			throw ex;
		} finally {
			db.commit();
		}
		int lun = connectionManager.addInitiatorRule(volumeId, nodeIqn);
		return connectionManager.getVolumeProperty(volumeId) + "," + 
		lun + "," + StorageProperties.IscsiAuthType.HBA.toString();
	}		

	@Override
	public void detachVolume(String volumeId, String nodeIqn)
	throws EucalyptusCloudException {
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		try {
			SANVolumeInfo volumeInfo = db.getUnique(new SANVolumeInfo(volumeId));
		} catch (EucalyptusCloudException ex) {
			throw ex;
		} finally {
			db.commit();
		}
		connectionManager.removeInitiatorRule(volumeId, nodeIqn);
	}

	@Override
	public void checkReady() throws EucalyptusCloudException {
		connectionManager.checkConnection();		
	}

	@Override
	public void stop() throws EucalyptusCloudException {
		connectionManager.stop();
	}

	@Override
	public void disable() throws EucalyptusCloudException {
		connectionManager.stop();	
	}

	@Override
	public void enable() throws EucalyptusCloudException {
		connectionManager.configure();
		connectionManager.checkConnection();
	}
}

