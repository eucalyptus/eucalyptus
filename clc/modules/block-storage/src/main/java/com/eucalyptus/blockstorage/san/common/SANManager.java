/*******************************************************************************
 *Copyright (c) 2009-2014  Eucalyptus Systems, Inc.
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
import java.util.NoSuchElementException;

import javax.persistence.EntityTransaction;

import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.async.Callbacks;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.LogicalStorageManager;
import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.StorageManagers;
import com.eucalyptus.blockstorage.StorageResource;
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
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.storage.common.CheckerTask;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Functions;
import com.google.common.base.Strings;

import edu.ucsb.eucalyptus.cloud.VolumeAlreadyExistsException;
import edu.ucsb.eucalyptus.msgs.ComponentProperty;
import edu.ucsb.eucalyptus.util.SystemUtil;

public class SANManager implements LogicalStorageManager {

	private SANProvider connectionManager;

	private static SANManager singleton;
	private static Logger LOG = Logger.getLogger(SANManager.class);

	public static LogicalStorageManager getInstance() {
		synchronized (SANManager.class) {
			if (singleton == null) {
				singleton = new SANManager();
			}
		}
		return singleton;
	}

	public SANManager() {
		Component sc = Components.lookup(Storage.class);
		if (sc == null) {
			throw Exceptions.toUndeclared("Cannot instantiate SANManager, no SC component found");
		}

		ServiceConfiguration scConfig = sc.getLocalServiceConfiguration();
		if (scConfig == null) {
			throw Exceptions.toUndeclared("Cannot instantiate SANManager without SC service configuration");
		}

		String sanProvider = null;
		EntityTransaction trans = Entities.get(StorageControllerConfiguration.class);
		try {
			StorageControllerConfiguration config = Entities.uniqueResult((StorageControllerConfiguration) scConfig);
			sanProvider = config.getBlockStorageManager();
			trans.commit();
		} catch (Exception e) {
			throw Exceptions.toUndeclared("Cannot get backend configuration for SC.");
		} finally {
			trans.rollback();
		}

		if (sanProvider == null) {
			throw Exceptions.toUndeclared("Cannot instantiate SAN Provider, none specified");
		}

		Class providerClass = StorageManagers.lookupProvider(sanProvider);
		if (providerClass != null && SANProvider.class.isAssignableFrom(providerClass)) {
			try {
				connectionManager = (SANProvider) providerClass.newInstance();
			} catch (IllegalAccessException e) {
				throw Exceptions.toUndeclared("Cannot create SANManager.", e);
			} catch (InstantiationException e) {
				throw Exceptions.toUndeclared("Cannot create SANManager. Cannot instantiate the SAN Provider", e);
			}
		} else {
			throw Exceptions.toUndeclared("Provider not of correct type or not found.");
		}
	}

    /**
     * used for unit testing, allows a (mock) connectionManager to be injected
     *
     * @param connectionManager
     */
    SANManager(SANProvider connectionManager) {
        this.connectionManager = connectionManager;
    }

	private boolean checkSANCredentialsExist() {
		SANInfo info = SANInfo.getStorageInfo();

		if (info == null || SANProperties.DUMMY_SAN_HOST.equals(info.getSanHost()) || SANProperties.SAN_PASSWORD.equals(info.getSanPassword())
				|| SANProperties.SAN_USERNAME.equals(info.getSanUser())) {
			return false;
		} else {
			return true;
		}
	}

	public void addSnapshot(String snapshotId) throws EucalyptusCloudException {
		finishVolume(snapshotId);
	}

	public void checkPreconditions() throws EucalyptusCloudException {
		if (!new File(BaseDirectory.LIB.toString() + File.separator + "connect_iscsitarget_sc.pl").exists()) {
			throw new EucalyptusCloudException("connect_iscitarget_sc.pl not found");
		}
		if (!new File(BaseDirectory.LIB.toString() + File.separator + "disconnect_iscsitarget_sc.pl").exists()) {
			throw new EucalyptusCloudException("disconnect_iscitarget_sc.pl not found");
		}

		if (connectionManager != null) {
			connectionManager.checkPreconditions();
		} else {
			LOG.warn("Cannot configure SANManager because the connectionManager is null. Please configure the sanprovider, sanuser, sanhost, and sanpassword.");
			throw new EucalyptusCloudException("SAN Provider not fully configured.");
		}

	}

	public void cleanSnapshot(String snapshotId) {
		String sanSnapshotId = null;
		try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
			// make sure it exists
			SANVolumeInfo volumeInfo = Entities.uniqueResult(new SANVolumeInfo(snapshotId));
			if (volumeInfo == null || StringUtils.isBlank(volumeInfo.getSanVolumeId())) {
				throw new EucalyptusCloudException(snapshotId + ": Backend ID not found");
			}
			sanSnapshotId = volumeInfo.getSanVolumeId();
            tran.commit();
		} catch (TransactionException | NoSuchElementException | EucalyptusCloudException ex) {
			LOG.debug(snapshotId + ": Snapshot not found", ex);
			return;
		}

		LOG.info("Deleting backend snapshot " + sanSnapshotId + " mapping to " + snapshotId);
		if (connectionManager.deleteVolume(sanSnapshotId)) {
			try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
				SANVolumeInfo snapInfo = Entities.uniqueResult(new SANVolumeInfo(snapshotId).withSanVolumeId(sanSnapshotId));
				Entities.delete(snapInfo);
                tran.commit();
			} catch (TransactionException | NoSuchElementException ex) {
				LOG.error(snapshotId + ": Unable to clean failed snapshot", ex);
				return;
			}
		}
	}

	public void cleanVolume(String volumeId) {
		String sanVolumeId = null;
		try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
			// make sure it exists
			SANVolumeInfo volumeInfo = Entities.uniqueResult(new SANVolumeInfo(volumeId));
			if (volumeInfo == null || StringUtils.isBlank(volumeInfo.getSanVolumeId())) {
				throw new EucalyptusCloudException(volumeId + ": Backend ID not found");
			}
			sanVolumeId = volumeInfo.getSanVolumeId();
		} catch (NoSuchElementException | TransactionException | EucalyptusCloudException ex) {
			LOG.debug(volumeId + ": Volume not found", ex);
			return;
		}

		LOG.info("Deleting backend volume " + sanVolumeId + " mapping to " + volumeId);
		if (connectionManager.deleteVolume(sanVolumeId)) {
			try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
				SANVolumeInfo volumeInfo = Entities.uniqueResult(new SANVolumeInfo(volumeId).withSanVolumeId(sanVolumeId));
				Entities.delete(volumeInfo);
                tran.commit();
			} catch (NoSuchElementException | TransactionException ex) {
				LOG.error("Unable to clean failed volume: " + volumeId);
				return;
			}
		}
	}

	public void configure() throws EucalyptusCloudException {
		// dummy init
		LOG.info("" + StorageInfo.getStorageInfo().getName());

		// configure provider
		if (connectionManager != null && connectionManager.checkSANCredentialsExist()) {
			connectionManager.configure();
		} else {
			LOG.warn("Cannot fully configure SAN Provider because of missing fileds. Please configure the sanuser, sanhost, sanpassword and chapuser");
			throw new EucalyptusCloudException("SAN Provider not fully configured.");
		}
	}

	public StorageResource createSnapshot(String volumeId, String snapshotId, String snapshotPointId, Boolean shouldTransferSnapshots)
			throws EucalyptusCloudException {
		String sanSnapshotId = resourceIdOnSan(snapshotId);
		String sanVolumeId = null;
		SANVolumeInfo snapInfo = new SANVolumeInfo(snapshotId);
		int size = -1;
		StorageResource storageResource = null;

		try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
			// Look up source volume in the database and get the backend volume ID
			try {
				SANVolumeInfo volumeInfo = Entities.uniqueResult(new SANVolumeInfo(volumeId));
				if (volumeInfo == null || StringUtils.isBlank(volumeInfo.getSanVolumeId())) {
					throw new EucalyptusCloudException("Backend ID not found for " + volumeId);
				}
				sanVolumeId = volumeInfo.getSanVolumeId();
				size = volumeInfo.getSize();
			} catch (TransactionException | NoSuchElementException ex) {
				LOG.error(volumeId + ": Failed to lookup source volume entity", ex);
				throw new EucalyptusCloudException("Failed to lookup source volume entity for " + volumeId, ex);
			}

			// Check to make sure that snapshot does not already exist on the backend
			try {
				SANVolumeInfo existingSnap = Entities.uniqueResult(snapInfo);
				if (connectionManager.snapshotExists(existingSnap.getSanVolumeId())) {
					throw new VolumeAlreadyExistsException("Snapshot already exists on storage backend for " + snapshotId);
				} else {
					LOG.debug(snapshotId + ": Found the database entity but the snapshot does not exist on SAN. Deleting the database entity");
					Entities.delete(existingSnap);
                    tran.commit();
				}
			} catch (TransactionException | VolumeAlreadyExistsException ex) {
				throw new EucalyptusCloudException(ex);
			} catch (NoSuchElementException ex) {
                // intentional no-op
            }
		}

		try {
			Transactions.save(snapInfo.withSanVolumeId(sanSnapshotId).withSize(size).withSnapshotOf(volumeId));
		} catch (Exception ex) {
			LOG.error(snapshotId + ": Failed to add database entity" + snapshotId, ex);
			throw new EucalyptusCloudException("Failed to add database entity for " + snapshotId, ex);
		}

		LOG.info("Creating backend snapshot " + sanSnapshotId + " mapping to " + snapshotId + " from backend volume " + sanVolumeId + " mapping to " + volumeId
				+ " using snapshot point " + snapshotPointId);
		String iqn = connectionManager.createSnapshot(sanVolumeId, sanSnapshotId, snapshotPointId);

		if (iqn != null) {
			// login to target and return dev
			// Moved this to before the connection is attempted since the volume does exist, it may need to be cleaned
			try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
				SANVolumeInfo existingSnap = Entities.uniqueResult(snapInfo);
				existingSnap.setIqn(iqn);
                Entities.merge(existingSnap);
                tran.commit();
			} catch (Exception ex) {
				LOG.error(snapshotId + ": Failed to update database entity with IQN post snapshot creation");
				throw new EucalyptusCloudException("Failed to update database entity with IQN post snapshot creation for " + snapshotId, ex);
				// TODO some cleanup pending here
			}

			if (shouldTransferSnapshots) { 
				storageResource = connectionManager.connectTarget(iqn);
				storageResource.setId(snapshotId);
			} else { // snapshots are always exported to the SC, unexport it from SC if its not being uploaded
				connectionManager.removeAllInitiatorRules(sanSnapshotId);
			}
		} else {
			throw new EucalyptusCloudException("Unable to create snapshot: " + snapshotId + " from volume: " + volumeId);
		}

		return storageResource;
	}

	public void createVolume(String volumeId, int size) throws EucalyptusCloudException {
		String sanVolumeId = resourceIdOnSan(volumeId);
		SANVolumeInfo volumeInfo = new SANVolumeInfo(volumeId);

		try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
			SANVolumeInfo existingVol = Entities.uniqueResult(volumeInfo);
			if (connectionManager.volumeExists(existingVol.getSanVolumeId())) {
				throw new VolumeAlreadyExistsException("Volume already exists on storage backend for " + volumeId);
			} else {
				LOG.debug(volumeId + ": Found the database entity but the volume does not exist on SAN. Deleting the database entity");
				Entities.delete(existingVol);
                tran.commit();
			}
		}
        catch (VolumeAlreadyExistsException ex) {
            throw ex;
        }
        catch (Exception ex) {
            // intentional no-op
		}

		try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
			Entities.persist(volumeInfo.withSanVolumeId(sanVolumeId).withSize(size));
            tran.commit();
		} catch (Exception ex) {
			LOG.error(volumeId + ": Failed to add database entity", ex);
			throw new EucalyptusCloudException("Failed to add database entity for " + volumeId, ex);
		}

		LOG.info("Creating backend volume " + sanVolumeId + " mapping to " + volumeId);
		String iqn = connectionManager.createVolume(sanVolumeId, size);
		if (iqn != null) {
			try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
				SANVolumeInfo existingVol = Entities.uniqueResult(volumeInfo);
				existingVol.setIqn(iqn);
                tran.commit();
			} catch (Exception ex) {
				LOG.error(volumeId + ": Failed to update database entity with IQN post volume creation");
				throw new EucalyptusCloudException("Failed to update database entity with IQN post volume creation for " + volumeId, ex);
				// TODO some cleanup pending here
			}
		} else {
			throw new EucalyptusCloudException("Unable to create volume: " + volumeId);
		}
	}

	private String resourceIdOnSan(String resourceId) {
		try {
			SANInfo sanInfo = Transactions.one(new SANInfo(), Functions.<SANInfo> identity());
			return (StringUtils.trimToEmpty(sanInfo.getResourcePrefix()) + resourceId + StringUtils.trimToEmpty(sanInfo.getResourceSuffix()));
		} catch (TransactionException ex) {
			LOG.error("Unable to retrieve resource prefix/suffix from databse", ex);
			return resourceId;
		}
	}

	public int createVolume(String volumeId, String snapshotId, int size) throws EucalyptusCloudException {
		String sanSnapshotId = null;
		String sanVolumeId = resourceIdOnSan(volumeId);
		SANVolumeInfo volumeInfo = new SANVolumeInfo(volumeId);

		int snapSize = -1;

		try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
			// Look up source snapshot in the database and get the backend snapshot ID
			try {
				SANVolumeInfo snapInfo = Entities.uniqueResult(new SANVolumeInfo(snapshotId));
				if (snapInfo == null || StringUtils.isBlank(snapInfo.getSanVolumeId())) {
					throw new EucalyptusCloudException("Backend ID not found for " + snapshotId);
				}
				sanSnapshotId = snapInfo.getSanVolumeId();
				snapSize = snapInfo.getSize();
				if (size <= 0) {
					size = snapSize;
				}
			} catch (TransactionException | NoSuchElementException ex) {
				LOG.error(snapshotId + ": Failed to lookup source snapshot entity", ex);
				throw new EucalyptusCloudException("Failed to lookup source snapshot entity for " + snapshotId, ex);
			}

			// Check to make sure that volume does not already exist on the backend
			try {
				SANVolumeInfo existingVol = Entities.uniqueResult(volumeInfo);
				if (connectionManager.volumeExists(existingVol.getSanVolumeId())) {
					throw new VolumeAlreadyExistsException("Volume already exists on storage backend for " + volumeId);
				} else {
					LOG.debug(volumeId + ": Found the database entity but the volume does not exist on SAN. Deleting the database entity");
					Entities.delete(existingVol);
                    tran.commit();
				}
			} catch (VolumeAlreadyExistsException ex) {
				throw ex;
			} catch (NoSuchElementException ex) {
                // intentional no-op
            } catch (Exception ex) {
                throw new EucalyptusCloudException(ex);
            }

		}

		try {
			Transactions.save(volumeInfo.withSanVolumeId(sanVolumeId).withSize(size));
		} catch (Exception ex) {
			LOG.error(volumeId + ": Failed to add database entity", ex);
			throw new EucalyptusCloudException("Failed to add database entity for " + volumeId, ex);
		}

		LOG.info("Creating backend volume " + sanVolumeId + " mapping to " + volumeId + " from backend snapshot " + sanSnapshotId + " mapping to " + snapshotId);
		String iqn = connectionManager.createVolume(sanVolumeId, sanSnapshotId, snapSize, size);
		if (iqn != null) {
			try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
				SANVolumeInfo existingVol = Entities.uniqueResult(volumeInfo);
				existingVol.setIqn(iqn);
                Entities.merge(existingVol);
                tran.commit();
			} catch (Exception ex) {
				LOG.error(volumeId + ": Failed to update database entity with IQN post volume creation");
				throw new EucalyptusCloudException("Failed to update database entity with IQN post volume creation for " + volumeId, ex);
				// TODO some cleanup pending here
			}
		} else {
			throw new EucalyptusCloudException("Unable to create volume: " + volumeId + " from snapshot: " + snapshotId);
		}

		return size;
	}

	public void cloneVolume(String volumeId, String parentVolumeId) throws EucalyptusCloudException {
		String sanVolumeId = resourceIdOnSan(volumeId);
		String sanParentVolumeId = null;
		SANVolumeInfo volInfo = new SANVolumeInfo(volumeId);
		//EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		int size = -1;


			// Look up source volume in the database and get the backend volume ID
			try {
				SANVolumeInfo parentVolumeInfo = Transactions.find(new SANVolumeInfo(parentVolumeId));
				if (parentVolumeInfo == null || StringUtils.isBlank(parentVolumeInfo.getSanVolumeId())) {
					throw new EucalyptusCloudException("Backend ID not found for " + parentVolumeId);
				}
				sanParentVolumeId = parentVolumeInfo.getSanVolumeId();
				size = parentVolumeInfo.getSize();
			} catch (TransactionException | NoSuchElementException ex) {
				LOG.error(volumeId + ": Failed to lookup source volume entity", ex);
				throw new EucalyptusCloudException("Failed to lookup source volume entity for " + parentVolumeId, ex);
			}

			// Check to make sure that cloned volume does not already exist on the backend
			try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
				SANVolumeInfo existingVol = Entities.uniqueResult(volInfo);
				if (connectionManager.snapshotExists(existingVol.getSanVolumeId())) {
					throw new VolumeAlreadyExistsException("Volume already exists on storage backend for " + volumeId);
				} else {
					LOG.debug(volumeId + ": Found the database entity but the volume does not exist on SAN. Deleting the database entity");
					Entities.delete(existingVol);
                    tran.commit();
				}
			} catch (VolumeAlreadyExistsException ex) {
				throw ex;
			} catch (Exception ex) {

            }



		try {
			Transactions.save(volInfo.withSanVolumeId(sanVolumeId).withSize(size));
		} catch (Exception ex) {
			LOG.error(volumeId + ": Failed to add database entity" + volumeId, ex);
			throw new EucalyptusCloudException("Failed to add database entity for " + volumeId, ex);
		}

		LOG.info("Cloning backend volume " + sanVolumeId + " mapping to " + volumeId + " from backend volume " + sanParentVolumeId + " mapping to "
				+ parentVolumeId);
		String iqn = connectionManager.cloneVolume(sanVolumeId, sanParentVolumeId);
		if (iqn != null) {
			try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
				SANVolumeInfo existingVol = Entities.uniqueResult(volInfo);
				existingVol.setIqn(iqn);
                Entities.merge(existingVol);
                tran.commit();
			} catch (Exception ex) {
				LOG.error(volumeId + ": Failed to update database entity with IQN post volume creation");
				throw new EucalyptusCloudException("Failed to update database entity with IQN post volume creation for " + volumeId, ex);
				// TODO some cleanup pending here
			}
		} else {
			throw new EucalyptusCloudException("Unable to create volume: " + volumeId);
		}
	}

	public void deleteSnapshot(String snapshotId) throws EucalyptusCloudException {
		String sanSnapshotId = null;
		try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
			// make sure it exists
			SANVolumeInfo volumeInfo = Entities.uniqueResult(new SANVolumeInfo(snapshotId));
			if (volumeInfo == null || StringUtils.isBlank(volumeInfo.getSanVolumeId())) {
				throw new EucalyptusCloudException(snapshotId + ": Backend ID not found");
			}
			sanSnapshotId = volumeInfo.getSanVolumeId();
		}
        catch (NoSuchElementException ex) {
            throw new EucalyptusCloudException(snapshotId + ": Backend ID not found");
        }
        catch (Exception ex) {
            throw new EucalyptusCloudException("caught exception while querying the database for snapshot " + snapshotId, ex);
        }

		LOG.info("Deleting backend snapshot " + sanSnapshotId + " mapping to " + snapshotId);
		
		boolean deleteEntity = false;
		
		// Try deleting the snapshot. It might fail as snapshots are global and another SC may have already deleted it
		if (connectionManager.deleteVolume(sanSnapshotId)) {
			deleteEntity = true;
		} else {
			// If snapshot deletion failed, check to see if the snapshot even exists
			LOG.debug("Unable to delete backend snapshot " + sanSnapshotId + ". Checking to see if the snapshot exists");
			if(!connectionManager.snapshotExists(sanSnapshotId)) {
				LOG.debug("Backend snapshot " + sanSnapshotId + " not found. Safe to delete database entity");
				deleteEntity = true;
			} else {
				LOG.warn("Failed to delete backend snapshot " +  sanSnapshotId + " mapping to " + snapshotId);
			}
		}
		
		if (deleteEntity) {
			try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
				SANVolumeInfo snapInfo = Entities.uniqueResult(new SANVolumeInfo(snapshotId).withSanVolumeId(sanSnapshotId));
				Entities.delete(snapInfo);
                tran.commit();
			} catch (TransactionException | NoSuchElementException ex) {
				LOG.error(snapshotId + ": Failed to delete database entity post snapshot deletion", ex);
			}
		} 
	}

	public void deleteVolume(String volumeId) throws EucalyptusCloudException {
		String sanVolumeId = null;
		try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
			// make sure it exists
			SANVolumeInfo volumeInfo = Entities.uniqueResult(new SANVolumeInfo(volumeId));
			if (volumeInfo == null || StringUtils.isBlank(volumeInfo.getSanVolumeId())) {
				throw new EucalyptusCloudException(volumeId + ": Backend ID not found");
			}
			sanVolumeId = volumeInfo.getSanVolumeId();
		} catch (TransactionException | NoSuchElementException ex) {
            throw new EucalyptusCloudException(ex);
        }

		LOG.info("Deleting backend volume " + sanVolumeId + " mapping to " + volumeId);
		if (connectionManager.deleteVolume(sanVolumeId)) {
			try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
				SANVolumeInfo snapInfo = Entities.uniqueResult(new SANVolumeInfo(volumeId).withSanVolumeId(sanVolumeId));
				Entities.delete(snapInfo);
                tran.commit();
			} catch (TransactionException | NoSuchElementException ex) {
				LOG.error(volumeId + ": Failed to delete database entity post volume deletion", ex);
			}
		}
	}

	public int getSnapshotSize(String snapshotId) throws EucalyptusCloudException {
		try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
			SANVolumeInfo snapInfo = Entities.uniqueResult(new SANVolumeInfo(snapshotId));
			return snapInfo.getSize();
		} catch (Exception ex) {
			LOG.error(ex);
            throw new EucalyptusCloudException(ex);
        }
	}

	public String getVolumeConnectionString(String volumeId) throws EucalyptusCloudException {
		return connectionManager.getVolumeConnectionString(volumeId);
	}

	public void initialize() {
		LOG.info("Initializing SANInfo entity");
		SANInfo.getStorageInfo();
		connectionManager.initialize();
	}

	public void loadSnapshots(List<String> snapshotSet, List<String> snapshotFileNames) throws EucalyptusCloudException {
		// TODO Auto-generated method stub

	}

	public List<String> prepareForTransfer(String snapshotId) throws EucalyptusCloudException {
		// Nothing to do here
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
		try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
			SANVolumeInfo snapInfo = Entities.uniqueResult(new SANVolumeInfo(snapshotId));
			String iqn = snapInfo.getIqn();
			String sanVolumeId = snapInfo.getSanVolumeId();
			tran.commit();
			connectionManager.disconnectTarget(sanVolumeId, iqn);
		} catch (TransactionException | NoSuchElementException ex) {
			LOG.error(ex);
			throw new EucalyptusCloudException("Unable to finalize snapshot: " + snapshotId);
		}
	}

	public StorageResource prepareSnapshot(String snapshotId, int sizeExpected, long actualSizeInMB) throws EucalyptusCloudException {

		LOG.info("Preparing snapshot " + snapshotId + " of size: " + sizeExpected);

		try {
			// If any record for the snapshot exists, just copy that info
			SANVolumeInfo volInfo = new SANVolumeInfo(snapshotId);
			SANVolumeInfo foundVolInfo = Transactions.find(volInfo); // will return ok even with multiple results
			LOG.debug("Found an existing snapshot record for " + snapshotId + " and will use that lun and record.");
			return null;
		} catch (TransactionException | NoSuchElementException e) {
		}

		LOG.debug(snapshotId + " not found on this SC's SAN. Now creating a lun on the SAN for the snapshot to be copied from ObjectStorage.");

		String sanSnapshotId = resourceIdOnSan(snapshotId);
		String iqn = null;
		try {
			// TODO Create a database record first before firing off the volume creation
			LOG.info("Creating backend snapshot holder " + sanSnapshotId + " mapping to " + snapshotId);
			iqn = connectionManager.createSnapshotHolder(sanSnapshotId, actualSizeInMB);
		} catch (EucalyptusCloudException e) {
			LOG.error("Could not create a volume to hold snapshot " + snapshotId);
			iqn = null;
		}

		if (iqn != null) {
			try {
				String scIqn = StorageProperties.getStorageIqn();
				if (scIqn == null) {
					throw new EucalyptusCloudException("Could not get the SC's initiator IQN, found null.");
				}

				// Ensure that the SC can attach to the volume.
				String lun = null;
				try {
					LOG.info("Exporting backend snapshot holder " + sanSnapshotId + " mapping to " + snapshotId + " to SC host IQN " + scIqn);
					lun = connectionManager.addInitiatorRule(sanSnapshotId, scIqn);
				} catch (EucalyptusCloudException attEx) {
					LOG.debug("Failed to setup attachment for snapshot " + snapshotId + " to SC", attEx);
					throw new EucalyptusCloudException("Could not setup snapshot volume " + snapshotId + " to SC because of error in attach prep", attEx);
				}
				
				if (lun == null) {
					throw new EucalyptusCloudException("Failed to export backend snapshot holder " + sanSnapshotId + " mapping to " + snapshotId
							+ " to SC host IQN " + scIqn);
				}

				// Store the lun ID in the iqn string, its needed for disconnecting the snapshot from SC later
				iqn = iqn + ',' + lun;
				StorageResource storageResource = null;
				try {
					// Run the connect
					storageResource = connectionManager.connectTarget(iqn);
					storageResource.setId(snapshotId);
				} catch (Exception connEx) {
					LOG.debug("Failed to connect SC to snapshot volume on SAN for snapshot " + snapshotId + ". Detaching and cleaning up.");
					try {
						LOG.info("Unexporting backend snapshot holder " + sanSnapshotId + " mapping to " + snapshotId + " from SC host IQN " + scIqn);
						connectionManager.removeInitiatorRule(sanSnapshotId, scIqn);
					} catch (EucalyptusCloudException detEx) {
						LOG.debug("Could not detach snapshot volume " + snapshotId + " during cleanup of failed connection");
					}
					throw new EucalyptusCloudException("Could not connect SC to target snapshot volume to prep for snapshot download from ObjectStorage",
							connEx);
				}
				
				SANVolumeInfo snapInfo = new SANVolumeInfo(snapshotId, iqn, sizeExpected).withSanVolumeId(sanSnapshotId);
                try {
                    Transactions.save(snapInfo);
                } catch (TransactionException e) {
                    LOG.error("Error occured trying to save the snapshot info to the database", e);
                    throw new EucalyptusCloudException(e);
                }
                return storageResource;
			} catch (EucalyptusCloudException e) {
				LOG.error("Error occured trying to connect the SC to the snapshot lun on the SAN.", e);
				if (!connectionManager.deleteVolume(snapshotId)) {
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
		if (configurableClass != null) {
			String root = configurableClass.root();
			String alias = configurableClass.alias();
			componentProperties = (ArrayList<ComponentProperty>) PropertyDirectory.getComponentPropertySet(StorageProperties.NAME + "." + root, alias);
		}
		configurableClass = SANInfo.class.getAnnotation(ConfigurableClass.class);
		if (configurableClass != null) {
			String root = configurableClass.root();
			String alias = configurableClass.alias();
			if (componentProperties == null)
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
				// type parser will correctly covert the value
				entry.setValue(prop.getValue());
			} catch (IllegalAccessException | ConfigurablePropertyException e) {
				LOG.error(e, e);
			}
		}
		connectionManager.setStorageProps(storageProps);
	}

	public String getStorageRootDirectory() {
		return StorageProperties.storageRootDirectory;
	}

	public String getVolumePath(String volumeId) throws EucalyptusCloudException {
		try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
			SANVolumeInfo volumeInfo = Entities.uniqueResult(new SANVolumeInfo(volumeId));
			String iqn = volumeInfo.getIqn();
			String deviceName = connectionManager.connectTarget(iqn).getPath();
			return deviceName;
		} catch (TransactionException | NoSuchElementException ex) {
			LOG.error("Unable to find volume: " + volumeId);
			throw new EucalyptusCloudException("Unable to find volume path for volume: " + volumeId,  ex );
		}
	}

	public void importVolume(String volumeId, String volumePath, int size) throws EucalyptusCloudException {
		try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
			Entities.uniqueResult(new SANVolumeInfo(volumeId));
			throw new EucalyptusCloudException("Volume " + volumeId + " already exists. Import failed.");
		} catch (NoSuchElementException ex) {
			// all good, move on
		} catch (TransactionException ex) {
			throw new EucalyptusCloudException(ex);
		}

		String sanVolumeId = resourceIdOnSan(volumeId);

		String iqn = connectionManager.createVolume(sanVolumeId, size);
		if (iqn != null) {
			String deviceName = connectionManager.connectTarget(iqn).getPath();
			// now copy
			try {
				SystemUtil.run(new String[] { StorageProperties.EUCA_ROOT_WRAPPER, "dd", "if=" + volumePath, "of=" + deviceName,
						"bs=" + StorageProperties.blockSize });
			} finally {
				connectionManager.disconnectTarget(sanVolumeId, iqn);
			}
			SANVolumeInfo volumeInfo = new SANVolumeInfo(volumeId, iqn, size).withSanVolumeId(sanVolumeId);
            try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
                Entities.persist(volumeInfo);
                tran.commit();
            }
		}
	}

	public String getSnapshotPath(String snapshotId) throws EucalyptusCloudException {
		return getVolumePath(snapshotId);
	}

	public void importSnapshot(String snapshotId, String volumeId, String snapPath, int size) throws EucalyptusCloudException {
		try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
            Entities.uniqueResult(new SANVolumeInfo(snapshotId));
            throw new EucalyptusCloudException("Snapshot " + snapshotId + " already exists. Import failed.");
        } catch (NoSuchElementException ex) {
            // all good, move on
        } catch (TransactionException ex) {
            throw new EucalyptusCloudException(ex);
        }

		String sanSnapshotId = resourceIdOnSan(snapshotId);
		String iqn = connectionManager.createVolume(sanSnapshotId, size);
		if (iqn != null) {
			String deviceName = connectionManager.connectTarget(iqn).getPath();
			// now copy
			try {
				SystemUtil.run(new String[] { StorageProperties.EUCA_ROOT_WRAPPER, "dd", "if=" + snapPath, "of=" + deviceName,
						"bs=" + StorageProperties.blockSize });
			} finally {
				connectionManager.disconnectTarget(sanSnapshotId, iqn);
			}
			SANVolumeInfo volumeInfo = new SANVolumeInfo(snapshotId, iqn, size).withSanVolumeId(sanSnapshotId).withSnapshotOf(volumeId);
            try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
                Entities.persist(volumeInfo);
                tran.commit();
            }
		}
	}

	public String exportVolume(String volumeId, String nodeIqn) throws EucalyptusCloudException {
		String sanVolumeId = null;
		try {
			SANVolumeInfo volumeInfo = Transactions.find(new SANVolumeInfo(volumeId) );
			if (volumeInfo == null || StringUtils.isBlank(volumeInfo.getSanVolumeId())) {
				throw new EucalyptusCloudException("Backend ID not found for " + volumeId);
			}
			sanVolumeId = volumeInfo.getSanVolumeId();
		} catch (TransactionException | NoSuchElementException ex) {
            throw new EucalyptusCloudException("Backend ID not found for " + volumeId, ex);
		}

		LOG.info("Exporting backend volume " + sanVolumeId + " mapping to " + volumeId + " to NC host IQN " + nodeIqn);
		String lun = connectionManager.addInitiatorRule(sanVolumeId, nodeIqn);
		if (lun == null) {
			throw new EucalyptusCloudException("No LUN found from connection manager");
		}

		String volumeConnectionString = connectionManager.getVolumeConnectionString(volumeId);
		if (Strings.isNullOrEmpty(volumeConnectionString)) {
			throw new EucalyptusCloudException("Could not get valid volume property");
		}
		String auth = connectionManager.getAuthType();
		String optionalUser = connectionManager.getOptionalChapUser();

		// Construct the correct connect string to return:
		// <user>,<authmode>,<lun string>,<volume property/SAN iqn>
		StringBuilder sb = new StringBuilder();
		sb.append(connectionManager.getProtocol()).append(',');
		sb.append(connectionManager.getProviderName()).append(',');
		sb.append(optionalUser == null ? "" : optionalUser).append(',');
		sb.append(auth == null ? "" : auth).append(',');
		sb.append(lun).append(',');
		sb.append(volumeConnectionString);
		return sb.toString();
	}

	public void unexportVolumeFromAll(String volumeId) throws EucalyptusCloudException {
		String sanVolumeId = null;
		try {
			SANVolumeInfo volumeInfo = Transactions.find(new SANVolumeInfo(volumeId));
			if (volumeInfo == null || StringUtils.isBlank(volumeInfo.getSanVolumeId())) {
				throw new EucalyptusCloudException("Backend ID not found for " + volumeId);
			}
			sanVolumeId = volumeInfo.getSanVolumeId();
		} catch (EucalyptusCloudException ex) {
			throw ex;
		}
        catch (TransactionException | NoSuchElementException ex) {
            throw new EucalyptusCloudException("Backend ID not found for " + volumeId, ex);
        }
		LOG.info("Unexporting backend volume " + sanVolumeId + " mapping to " + volumeId + " from all hosts");
		connectionManager.removeAllInitiatorRules(sanVolumeId);
	}

	public void unexportVolume(String volumeId, String nodeIqn) throws EucalyptusCloudException, UnsupportedOperationException {
		String sanVolumeId = null;
		try {
			SANVolumeInfo volumeInfo = Transactions.find(new SANVolumeInfo(volumeId));
			if (volumeInfo == null || StringUtils.isBlank(volumeInfo.getSanVolumeId())) {
				throw new EucalyptusCloudException("Backend ID not found for " + volumeId);
			}
			sanVolumeId = volumeInfo.getSanVolumeId();
		} catch (EucalyptusCloudException ex) {
            throw ex;
        }
        catch (TransactionException | NoSuchElementException ex) {
            throw new EucalyptusCloudException("Backend ID not found for " + volumeId, ex);
        }
		LOG.info("Unexporting backend volume " + sanVolumeId + " mapping to " + volumeId + " from NC host IQN " + nodeIqn);
		connectionManager.removeInitiatorRule(sanVolumeId, nodeIqn);
	}

	public void checkReady() throws EucalyptusCloudException {
		if (Component.State.ENABLED.equals(Components.lookup(Storage.class).getState())) {
			connectionManager.checkConnection();
		}
	}

	public void stop() throws EucalyptusCloudException {
		try {
			connectionManager.stop();
		} catch (EucalyptusCloudException e) {
			LOG.error("Exception stopping connection manager", e);
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

	public boolean getFromBackend(String snapshotId, int size) throws EucalyptusCloudException {
		SANVolumeInfo snapInfo = new SANVolumeInfo(snapshotId);

		// Look for the unique snapshot entity for this partition.
		try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
			SANVolumeInfo foundSnapInfo = Entities.uniqueResult(snapInfo);
			// Found the snapshot entity. Check if the snapshot really exists on SAN
			if (foundSnapInfo == null || StringUtils.isBlank(foundSnapInfo.getSanVolumeId())) {
				throw new EucalyptusCloudException("Backend ID not found for " + snapshotId);
			}
			LOG.info("Checking for backend snapshot " + foundSnapInfo.getSanVolumeId() + " mapping to " + snapshotId);
			if (connectionManager.snapshotExists(foundSnapInfo.getSanVolumeId())) { // Snapshot does exist. Nothing to do
				return true;
			} else { // Snapshot does not exist on SAN. Delete the record and move to the next part
			    Entities.delete(foundSnapInfo);
			}
            tran.commit();
		} catch (Exception ex) {
			// Could be an error for snapshot lookup
		}

		// Either no unique snapshot entity was found for this partition or one did exist but the snapshot was not present on the SAN
		// Look for the snapshot in all partitions
		snapInfo.setScName(null);
		try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
			List<SANVolumeInfo> foundSnapInfos = Entities.query(snapInfo);

			// Loop through the snapshot records and check if one of them exists on the SAN this partition is connected to
			for (SANVolumeInfo foundSnapInfo : foundSnapInfos) {
				LOG.info("Checking for backend snapshot " + foundSnapInfo.getSanVolumeId() + " mapping to " + snapshotId);
				if (connectionManager.snapshotExists(foundSnapInfo.getSanVolumeId())) { // Snapshot does exist on SAN.
					// Create a record for it in this partition
					SANVolumeInfo newSnapInfo = new SANVolumeInfo(snapshotId, foundSnapInfo.getIqn(), foundSnapInfo.getSize()).withSanVolumeId(
							foundSnapInfo.getSanVolumeId()).withSnapshotOf(foundSnapInfo.getSnapshotOf());
					Entities.persist(newSnapInfo);
					tran.commit();
					return true;
				}
			}
		} catch (Exception ex) {
			// Could be an error for snapshot lookup
		}
		return false;
	}

	public void checkVolume(String volumeId) throws EucalyptusCloudException {
	}

	public List<CheckerTask> getCheckers() {
		return new ArrayList<CheckerTask>();
	}

	public String createSnapshotPoint(String parentVolumeId, String volumeId) throws EucalyptusCloudException {
		if (connectionManager != null) {
			String sanParentVolumeId = null;
			try {
				SANVolumeInfo parentVolInfo = Transactions.find(new SANVolumeInfo(parentVolumeId));
				if (parentVolInfo == null || StringUtils.isBlank(parentVolInfo.getSanVolumeId())) {
					throw new EucalyptusCloudException("Backend ID not found for " + parentVolumeId);
				}
				sanParentVolumeId = parentVolInfo.getSanVolumeId();
			} catch (TransactionException | NoSuchElementException ex) {
				LOG.error(parentVolumeId + ": Failed to lookup source volume entity", ex);
				throw new EucalyptusCloudException("Failed to lookup source snapshot volume for " + parentVolumeId, ex);
			}
			String snapshotPoint = resourceIdOnSan(volumeId);
			LOG.info("Creating backend snapshot point " + snapshotPoint + " against backend parent volume " + sanParentVolumeId + " mapping to "
					+ parentVolumeId);
			return connectionManager.createSnapshotPoint(sanParentVolumeId, snapshotPoint);
		} else {
			throw new EucalyptusCloudException("Cannot create snapshot point, no SAN provider found");
		}
	}

	// TODO: zhill, should I removed the extra params or only allow the parent and vol Id and then calculate the snapPointId from that?
	// If the desire is to make this idempotent then a calculation is ideal since the original may have been lost (i.e. restart)
	public void deleteSnapshotPoint(String parentVolumeId, String volumeId, String snapshotPointId) throws EucalyptusCloudException {
		if (connectionManager != null) {
			connectionManager.deleteSnapshotPoint(parentVolumeId, snapshotPointId);
		} else {
			throw new EucalyptusCloudException("Cannot delete snapshot point, no SAN provider found");
		}

	}
}
