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

package com.eucalyptus.blockstorage.san.common;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.LogicalStorageManager;
import com.eucalyptus.blockstorage.StorageManagers;
import com.eucalyptus.blockstorage.config.StorageControllerConfiguration;
import com.eucalyptus.blockstorage.entities.StorageInfo;
import com.eucalyptus.blockstorage.san.common.entities.SANInfo;
import com.eucalyptus.blockstorage.san.common.entities.SANVolumeInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.objectstorage.util.WalrusProperties;
import com.eucalyptus.blockstorage.san.common.SANProvider;
import com.eucalyptus.storage.common.CheckerTask;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Strings;

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
		Component sc = Components.lookup(Storage.class);
		if(sc == null) {
			throw Exceptions.toUndeclared("Cannot instantiate SANManager, no SC component found");
		}
		
		ServiceConfiguration scConfig = sc.getLocalServiceConfiguration();
		if(scConfig == null) {
			throw Exceptions.toUndeclared("Cannot instantiate SANManager without SC service configuration");
		}
		
		String sanProvider = null;
		EntityTransaction trans = Entities.get(StorageControllerConfiguration.class);
		try {
			StorageControllerConfiguration config = Entities.uniqueResult((StorageControllerConfiguration)scConfig);		
			sanProvider = config.getBlockStorageManager();
			trans.commit();
		} catch(Exception e) {
			throw Exceptions.toUndeclared("Cannot get backend configuration for SC.");
		} finally {
			trans.rollback();
		}
		
		if(sanProvider == null) {
			throw Exceptions.toUndeclared("Cannot instantiate SAN Provider, none specified");
		}
		
		Class providerClass = StorageManagers.lookupProvider(sanProvider);
		if(providerClass != null && SANProvider.class.isAssignableFrom(providerClass)) { 
			try {
				connectionManager = (SANProvider)providerClass.newInstance();
			} catch (IllegalAccessException e) {
				throw Exceptions.toUndeclared("Cannot create SANManager.",e);
			} catch (InstantiationException e) {
				throw Exceptions.toUndeclared("Cannot create SANManager. Cannot instantiate the SAN Provider",e);
			}
		}
		else {
			throw Exceptions.toUndeclared("Provider not of correct type or not found.");
		}		
	}
	
	private boolean checkSANCredentialsExist() {
		SANInfo info = SANInfo.getStorageInfo();
		
		if(info == null || SANProperties.DUMMY_SAN_HOST.equals(info.getSanHost()) 
				|| SANProperties.SAN_PASSWORD.equals(info.getSanPassword()) 
				|| SANProperties.SAN_USERNAME.equals(info.getSanUser())) {
			return false;
		}
		else {
			return true;
		}
	}

	public void addSnapshot(String snapshotId) throws EucalyptusCloudException {
		finishVolume(snapshotId);
	}

	public void checkPreconditions() throws EucalyptusCloudException {
		if(!new File(BaseDirectory.LIB.toString() + File.separator + "connect_iscsitarget_sc.pl").exists()) {
			throw new EucalyptusCloudException("connect_iscitarget_sc.pl not found");
		}
		if(!new File(BaseDirectory.LIB.toString() + File.separator + "disconnect_iscsitarget_sc.pl").exists()) {
			throw new EucalyptusCloudException("disconnect_iscitarget_sc.pl not found");
		}
		
		if(connectionManager != null) {
			connectionManager.checkPreconditions();
		}
		else {
			LOG.warn("Cannot configure SANManager because the connectionManager is null. Please configure the sanprovider, sanuser, sanhost, and sanpassword.");
			throw new EucalyptusCloudException("SAN Provider not fully configured.");
		}
		
	}

	public void cleanSnapshot(String snapshotId) {
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();		
		try {
			//make sure it exists
			SANVolumeInfo volumeInfo = db.getUnique(new SANVolumeInfo(snapshotId));
		} catch(EucalyptusCloudException ex) {
			LOG.debug("Snapshot not found: " + snapshotId);
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

	public void cleanVolume(String volumeId) {
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		try {
			//make sure it exists
			SANVolumeInfo volumeInfo = db.getUnique(new SANVolumeInfo(volumeId));
		} catch(EucalyptusCloudException ex) {
			LOG.debug("Volume not found: " + volumeId);
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

	public void configure() throws EucalyptusCloudException {
		//dummy init
		LOG.info("" + StorageInfo.getStorageInfo().getName());

		//configure provider
		if(connectionManager != null && connectionManager.checkSANCredentialsExist()) {
			connectionManager.configure();
		}
		else {
			LOG.warn("Cannot fully configure SAN Provider because of missing fileds. Please configure the sanuser, sanhost, sanpassword and chapuser");
			throw new EucalyptusCloudException("SAN Provider not fully configured.");
		}
	}

	public List<String> createSnapshot(String volumeId, String snapshotId, String snapshotPointId, Boolean shouldTransferSnapshots)
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

		LOG.info("Creating snapshot " + snapshotId + " from volume " + volumeId + " using snapshot point " + snapshotPointId);
		String iqn = connectionManager.createSnapshot(volumeId, snapshotId, snapshotPointId);
		
		if(iqn != null) {
			//login to target and return dev
			// Moved this to before the connection is attempted since the volume does exist, it may need to be cleaned
			SANVolumeInfo snapInfo = new SANVolumeInfo(snapshotId, iqn, size);
			snapInfo.setSnapshotOf(volumeId);
			db = StorageProperties.getEntityWrapper();
			db.add(snapInfo);
			db.commit();

			if(shouldTransferSnapshots) {
				String deviceName = connectionManager.connectTarget(iqn);
				returnValues.add(deviceName);
				returnValues.add(String.valueOf(size * WalrusProperties.G));
			}

		} else {
			db.rollback();
			throw new EucalyptusCloudException("Unable to create snapshot: " + snapshotId + " from volume: " + volumeId);
		}
		return returnValues;
	}

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

	public int createVolume(String volumeId, String snapshotId, int size)
	throws EucalyptusCloudException {
		int snapSize = -1;
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		try {
			SANVolumeInfo snapInfo = db.getUnique(new SANVolumeInfo(snapshotId));
			db.commit();
			snapSize = snapInfo.getSize();
			if(size <= 0) {
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

	public void cloneVolume(String volumeId, String parentVolumeId)
	throws EucalyptusCloudException {
		int size = -1;
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		try {
			SANVolumeInfo parentVolumeInfo = db.getUnique(new SANVolumeInfo(parentVolumeId));
			db.commit();
			size = parentVolumeInfo.getSize();
		} catch(EucalyptusCloudException ex) {
			LOG.error(ex);
			db.rollback();
			throw ex;
		}
		String iqn = connectionManager.cloneVolume(volumeId, parentVolumeId);
		if(iqn != null) {
			SANVolumeInfo volumeInfo = new SANVolumeInfo(volumeId, iqn, size);
			db = StorageProperties.getEntityWrapper();
			db.add(volumeInfo);
			db.commit();
		} else {
			db.rollback();
			throw new EucalyptusCloudException("Unable to create volume: " + volumeId);
		}
	}

	public void deleteSnapshot(String snapshotId)
	throws EucalyptusCloudException {
		if(connectionManager.deleteVolume(snapshotId)) {
			EntityWrapper<SANVolumeInfo>  db = StorageProperties.getEntityWrapper();
			try {
				SANVolumeInfo snapInfo = db.getUnique(new SANVolumeInfo(snapshotId));
				db.delete(snapInfo);
			} catch(EucalyptusCloudException ex) {
				LOG.error("Failed to update the database", ex);
			} finally {
				db.commit();
			}
		}
	}

	public void deleteVolume(String volumeId) throws EucalyptusCloudException {
		if(connectionManager.deleteVolume(volumeId)) {
			EntityWrapper<SANVolumeInfo>  db = StorageProperties.getEntityWrapper();
			try {
				SANVolumeInfo snapInfo = db.getUnique(new SANVolumeInfo(volumeId));
				db.delete(snapInfo);
			} catch(EucalyptusCloudException ex) {
				LOG.error("Failed to update the database", ex);
			} finally {
				db.commit();
			}
		}
	}

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

	public String getVolumeConnectionString(String volumeId)
	throws EucalyptusCloudException {
		return connectionManager.getVolumeConnectionString(volumeId);
	}

	public void initialize() {}

	public void loadSnapshots(List<String> snapshotSet,
			List<String> snapshotFileNames) throws EucalyptusCloudException {
		// TODO Auto-generated method stub

	}

	public List<String> prepareForTransfer(String snapshotId)
	throws EucalyptusCloudException {
		//Nothing to do here
		return new ArrayList<String>();
	}

	public void reload() {
	}

	public void startupChecks() throws EucalyptusCloudException {
		try {
			connectionManager.configure();
		} catch (EucalyptusCloudException e) {
			LOG.error(e);
			throw e;
		}
		connectionManager.checkConnection();
	}

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

	public String prepareSnapshot(String snapshotId, int sizeExpected, long actualSizeInMB) throws EucalyptusCloudException {

		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		LOG.info("Preparing snapshot " + snapshotId + " of size: " + sizeExpected);

		try {
			//If any record for the snapshot exists, just copy that info
			SANVolumeInfo volInfo = new SANVolumeInfo(snapshotId);
			SANVolumeInfo foundVolInfo = db.getUnique(volInfo); //will return ok even with multiple results
			SANVolumeInfo volumeInfo = new SANVolumeInfo(snapshotId, foundVolInfo.getIqn(), sizeExpected);
			volumeInfo.setStatus(foundVolInfo.getStatus());
			volumeInfo.setSnapshotOf(foundVolInfo.getSnapshotOf());
			volumeInfo.setStoreUser(foundVolInfo.getStoreUser());			
			db.add(volumeInfo);
			LOG.debug("Found an existing snapshot record for " + snapshotId + " and will use that lun and record.");
			return null;
		} catch(EucalyptusCloudException e) {
		} finally {
			db.commit();
		}

		LOG.debug(snapshotId + " not found on this SC's SAN. Now creating a lun on the SAN for the snapshot to be copied from Walrus.");
		String iqn = null;		
		try {
			// TODO
			iqn = connectionManager.createSnapshotHolder(snapshotId, actualSizeInMB);
		} catch(EucalyptusCloudException e) {
			LOG.error("Could not create a volume to hold snapshot " + snapshotId);
			iqn = null;
		}

		if(iqn != null) {
			try {
				String scIqn = StorageProperties.getStorageIqn();
				if(scIqn == null) {
					throw new EucalyptusCloudException("Could not get the SC's initiator IQN, found null.");
				}

				//Ensure that the SC can attach to the volume.
				Integer lun = -1;				
				try {
					lun = connectionManager.addInitiatorRule(snapshotId, scIqn);
				} catch(EucalyptusCloudException attEx) {
					LOG.debug("Failed to setup attachment for snapshot " + snapshotId + " to SC",attEx);
					throw new EucalyptusCloudException("Could not setup snapshot volume " + snapshotId + " to SC because of error in attach prep",attEx);
				}

				//Run the connect
				String deviceName = null;
				try {
					//Negative luns are invalid, so don't include, i.e Equallogic uses no lun
					if(lun >= 0) {
						iqn = iqn + "," + String.valueOf(lun);
					}
					deviceName = connectionManager.connectTarget(iqn);
				} catch(Exception connEx) {
					LOG.debug("Failed to connect SC to snapshot volume on SAN for snapshot " + snapshotId + ". Detaching and cleaning up.");
					try {
						connectionManager.removeInitiatorRule(snapshotId, scIqn);
					} catch(EucalyptusCloudException detEx) {
						LOG.debug("Could not detach snapshot volume " + snapshotId + " during cleanup of failed connection");
					}
					throw new EucalyptusCloudException("Could not connect SC to target snapshot volume to prep for snapshot download from Walrus",connEx);
				}

				SANVolumeInfo snapInfo = new SANVolumeInfo(snapshotId, iqn, sizeExpected);
				db = StorageProperties.getEntityWrapper();
				db.add(snapInfo);
				db.commit();
				return deviceName;
			} catch(EucalyptusCloudException e) {
				LOG.error("Error occured trying to connect the SC to the snapshot lun on the SAN.", e);				
				if(!connectionManager.deleteVolume(snapshotId)) {
					LOG.error("Failed to delete the snapshot volume during cleanup of failed snapshot prep for " + snapshotId);					
				}				
				throw new EucalyptusCloudException("Failed to create new LUN for snapshot " + snapshotId, e);
			}
		}
		return null;
	}

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

	public String getStorageRootDirectory() {
		return StorageProperties.storageRootDirectory;
	}

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

	public void importVolume(String volumeId, String volumePath, int size)
	throws EucalyptusCloudException {
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
				SystemUtil.run(new String[]{StorageProperties.EUCA_ROOT_WRAPPER, "dd", "if=" + volumePath, "of=" + deviceName, "bs=" + StorageProperties.blockSize});			
			} finally {
				connectionManager.disconnectTarget(volumeId, iqn);
			}
			SANVolumeInfo volumeInfo = new SANVolumeInfo(volumeId, iqn, size);
			db = StorageProperties.getEntityWrapper();
			db.add(volumeInfo);
			db.commit();
		}		
	}

	public String getSnapshotPath(String snapshotId)
	throws EucalyptusCloudException {
		return getVolumePath(snapshotId);
	}

	public void importSnapshot(String snapshotId, String volumeId, String snapPath, int size)
	throws EucalyptusCloudException {
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
				SystemUtil.run(new String[]{StorageProperties.EUCA_ROOT_WRAPPER, "dd", "if=" + snapPath, "of=" + deviceName, "bs=" + StorageProperties.blockSize});			
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

	public String exportVolume(String volumeId, String nodeIqn)
	throws EucalyptusCloudException {
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		try {
			SANVolumeInfo volumeInfo = db.getUnique(new SANVolumeInfo(volumeId));
		} catch (EucalyptusCloudException ex) {
			throw ex;
		} finally {
			db.commit();
		}
		
		Integer lun = connectionManager.addInitiatorRule(volumeId, nodeIqn);
		if(lun == null) {
			throw new EucalyptusCloudException("No LUN found from connection manager");
		}
				
		String volumeConnectionString = connectionManager.getVolumeConnectionString(volumeId);
		if (Strings.isNullOrEmpty(volumeConnectionString)) {
			throw new EucalyptusCloudException("Could not get valid volume property");
		}
		String auth = connectionManager.getAuthType();
		String optionalUser = connectionManager.getOptionalChapUser();
		
		//Construct the correct connect string to return:
		// <user>,<authmode>,<lun string>,<volume property/SAN iqn>
		StringBuilder sb = new StringBuilder();
		sb.append(optionalUser == null ? "" : optionalUser).append(',');
		sb.append(auth == null ? "" : auth).append(',');
		sb.append(lun.toString()).append(',');
		sb.append(volumeConnectionString);
		return sb.toString();
	}
	
	public void unexportVolumeFromAll(String volumeId) throws EucalyptusCloudException {
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		try {
			SANVolumeInfo volumeInfo = db.getUnique(new SANVolumeInfo(volumeId));
		} catch (EucalyptusCloudException ex) {
			throw ex;
		} finally {
			db.commit();
		}
		connectionManager.removeAllInitiatorRules(volumeId);
	}	

	public void unexportVolume(String volumeId, String nodeIqn)
	throws EucalyptusCloudException, UnsupportedOperationException {
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

	public void checkReady() throws EucalyptusCloudException {
		if (Component.State.ENABLED.equals(Components.lookup(Storage.class).getState())) {
			connectionManager.checkConnection();
		}
	}

	public void stop() throws EucalyptusCloudException {
		try {
			connectionManager.stop();
		} catch(EucalyptusCloudException e) {
			LOG.error("Exception stopping connection manager",e);
			throw e;
		} finally {			
			connectionManager = null;
		}
	}

	public void disable() throws EucalyptusCloudException {
		connectionManager.stop();	
	}

	public void enable() throws EucalyptusCloudException {
		connectionManager.configure();
		connectionManager.checkConnection();
	}

	public boolean getFromBackend(String snapshotId, int size)
	throws EucalyptusCloudException {
		if (connectionManager.snapshotExists(snapshotId)) {
			EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
			
			// Create a DB record for the snapshot on this SC, otherwise createVolume will not find it
			try {
				SANVolumeInfo snapInfo = new SANVolumeInfo(snapshotId);
				snapInfo.setSize(size);
				db.add(snapInfo);
				db.commit();
			} catch (Exception e) {
				LOG.error("Failed to create record for the snapshot", e);
			} finally {
				db.rollback();
			}
			return true;
		} else {
			return false;
		}
	}

	public void checkVolume(String volumeId) throws EucalyptusCloudException {}
	
	public List<CheckerTask> getCheckers() {
		return new ArrayList<CheckerTask>();
	}
	
	public String createSnapshotPoint(String parentVolumeId, String volumeId) throws EucalyptusCloudException {
		if(connectionManager != null) {
			return connectionManager.createSnapshotPoint(parentVolumeId, volumeId);
		} else {
			throw new EucalyptusCloudException("Cannot create snapshot point, no SAN provider found");
		}
	}
	
	//TODO: zhill, should I removed the extra params or only allow the parent and vol Id and then calculate the snapPointId from that?
	// If the desire is to make this idempotent then a calculation is ideal since the original may have been lost (i.e. restart)
	public void deleteSnapshotPoint(String parentVolumeId, String volumeId, String snapshotPointId) throws EucalyptusCloudException {
		if(connectionManager != null) {
			connectionManager.deleteSnapshotPoint(snapshotPointId);
		} else {
			throw new EucalyptusCloudException("Cannot delete snapshot point, no SAN provider found");
		}
		
	}
}

