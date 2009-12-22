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
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.ws;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.NeedsDeferredInitialization;
import com.eucalyptus.bootstrap.SystemBootstrapper;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.StorageProperties;
import com.eucalyptus.util.WalrusProperties;

import edu.ucsb.eucalyptus.cloud.AccessDeniedException;
import edu.ucsb.eucalyptus.cloud.EntityTooLargeException;
import edu.ucsb.eucalyptus.cloud.NoSuchVolumeException;
import edu.ucsb.eucalyptus.cloud.SnapshotInUseException;
import edu.ucsb.eucalyptus.cloud.VolumeAlreadyExistsException;
import edu.ucsb.eucalyptus.cloud.VolumeNotReadyException;
import edu.ucsb.eucalyptus.cloud.entities.SnapshotInfo;
import edu.ucsb.eucalyptus.cloud.entities.StorageInfo;
import edu.ucsb.eucalyptus.cloud.entities.VolumeInfo;
import edu.ucsb.eucalyptus.ic.StorageController;
import edu.ucsb.eucalyptus.msgs.CreateStorageSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.CreateStorageSnapshotType;
import edu.ucsb.eucalyptus.msgs.CreateStorageVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.CreateStorageVolumeType;
import edu.ucsb.eucalyptus.msgs.DeleteStorageSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteStorageSnapshotType;
import edu.ucsb.eucalyptus.msgs.DeleteStorageVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteStorageVolumeType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageSnapshotsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageSnapshotsType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageVolumesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageVolumesType;
import edu.ucsb.eucalyptus.msgs.GetStorageConfigurationResponseType;
import edu.ucsb.eucalyptus.msgs.GetStorageConfigurationType;
import edu.ucsb.eucalyptus.msgs.GetStorageVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.GetStorageVolumeType;
import edu.ucsb.eucalyptus.msgs.InitializeStorageManagerResponseType;
import edu.ucsb.eucalyptus.msgs.InitializeStorageManagerType;
import edu.ucsb.eucalyptus.msgs.StorageSnapshot;
import edu.ucsb.eucalyptus.msgs.StorageVolume;
import edu.ucsb.eucalyptus.msgs.UpdateStorageConfigurationResponseType;
import edu.ucsb.eucalyptus.msgs.UpdateStorageConfigurationType;
import edu.ucsb.eucalyptus.storage.BlockStorageChecker;
import edu.ucsb.eucalyptus.storage.BlockStorageManagerFactory;
import edu.ucsb.eucalyptus.storage.LogicalStorageManager;
import edu.ucsb.eucalyptus.storage.StorageManager;
import edu.ucsb.eucalyptus.storage.fs.FileSystemStorageManager;
import edu.ucsb.eucalyptus.util.EucaSemaphore;
import edu.ucsb.eucalyptus.util.EucaSemaphoreDirectory;

@NeedsDeferredInitialization(component = Component.storage)
public class BlockStorage {

	private static Logger LOG = Logger.getLogger(BlockStorage.class);

	static StorageManager volumeStorageManager;
	static StorageManager snapshotStorageManager;
	static LogicalStorageManager blockManager;
	static BlockStorageChecker checker;
	static BlockStorageStatistics blockStorageStatistics;

	public static void deferredInitializer() {
		volumeStorageManager = new FileSystemStorageManager(StorageProperties.storageRootDirectory);
		snapshotStorageManager = new FileSystemStorageManager(StorageProperties.storageRootDirectory);
		blockManager = BlockStorageManagerFactory.getBlockStorageManager();
		checker = new BlockStorageChecker(volumeStorageManager, snapshotStorageManager, blockManager);
		if(StorageProperties.trackUsageStatistics) 
			blockStorageStatistics = new BlockStorageStatistics();
		initialize();
	}

	public static void initialize() {
		blockManager.configure();
		blockManager.initialize();
		configure();
		StorageProperties.enableSnapshots = StorageProperties.enableStorage = true;
		try {
			startupChecks();
		} catch(EucalyptusCloudException ex) {
			LOG.error("Startup checks failed ", ex);
		}
	}

	private static void configure() {
		StorageProperties.updateWalrusUrl();
		StorageInfo storageInfo = getConfig();
		StorageProperties.MAX_TOTAL_VOLUME_SIZE = storageInfo.getMaxTotalVolumeSizeInGb();
		StorageProperties.iface = storageInfo.getStorageInterface();
		StorageProperties.MAX_VOLUME_SIZE = storageInfo.getMaxVolumeSizeInGB();
		StorageProperties.storageRootDirectory = storageInfo.getVolumesDir();
		StorageProperties.zeroFillVolumes = storageInfo.getZeroFillVolumes();
		StorageProperties.updateStorageHost();
	}

	private static StorageInfo getConfig() {
		StorageProperties.updateName();		
		EntityWrapper<StorageInfo> db = StorageController.getEntityWrapper();
		StorageInfo storageInfo;
		try {
			storageInfo = db.getUnique(new StorageInfo(StorageProperties.NAME));
			db.commit();
		} catch(EucalyptusCloudException ex) {
			storageInfo = new StorageInfo(StorageProperties.NAME, 
					StorageProperties.MAX_TOTAL_VOLUME_SIZE, 
					StorageProperties.iface, 
					StorageProperties.MAX_VOLUME_SIZE, 
					StorageProperties.storageRootDirectory,
					StorageProperties.zeroFillVolumes);
			db.add(storageInfo);
			db.commit();
		} 
		return storageInfo;
	}

	private static void updateConfig() {
		EntityWrapper<StorageInfo> db = StorageController.getEntityWrapper();
		StorageInfo storageInfo;
		try {
			storageInfo = db.getUnique(new StorageInfo(StorageProperties.NAME));
			storageInfo.setMaxTotalVolumeSizeInGb(StorageProperties.MAX_TOTAL_VOLUME_SIZE);
			storageInfo.setStorageInterface(StorageProperties.iface);
			storageInfo.setMaxVolumeSizeInGB(StorageProperties.MAX_VOLUME_SIZE);
			storageInfo.setVolumesDir(StorageProperties.storageRootDirectory);
			storageInfo.setZeroFillVolumes(StorageProperties.zeroFillVolumes);
			db.commit();
		} catch(EucalyptusCloudException ex) {
			storageInfo = new StorageInfo(StorageProperties.NAME, 
					StorageProperties.MAX_TOTAL_VOLUME_SIZE, 
					StorageProperties.iface, 
					StorageProperties.MAX_VOLUME_SIZE, 
					StorageProperties.storageRootDirectory,
					StorageProperties.zeroFillVolumes);
			db.add(storageInfo);
			db.commit();
		} 
	}

	public BlockStorage() {}

	private static void startupChecks() throws EucalyptusCloudException {
		check();
		if(checker != null) {
			checker.startupChecks();
		}
	}

	public static void checkPending() {
		if(checker != null) {
			StorageProperties.updateWalrusUrl();
			try {
				checker.transferPendingSnapshots();
			} catch (Exception ex) {
				LOG.error("unable to transfer pending snapshots", ex);
			}
		}
	}

	public static void check() {
		File volumeDir = new File(StorageProperties.storageRootDirectory);
		if(!volumeDir.exists()) {
			if(!volumeDir.mkdirs()) {
				LOG.fatal("Unable to make volume root directory: " + StorageProperties.storageRootDirectory);
			}
		} else if(!volumeDir.canWrite()) {
			LOG.fatal("Cannot write to volume root directory: " + StorageProperties.storageRootDirectory);
		}

	}

	public InitializeStorageManagerResponseType InitializeStorageManager(InitializeStorageManagerType request) throws EucalyptusCloudException {
		InitializeStorageManagerResponseType reply = (InitializeStorageManagerResponseType) request.getReply();
		initialize();
		return reply;
	}

	public UpdateStorageConfigurationResponseType UpdateStorageConfiguration(UpdateStorageConfigurationType request) throws EucalyptusCloudException {
		UpdateStorageConfigurationResponseType reply = (UpdateStorageConfigurationResponseType) request.getReply();
		if(Component.eucalyptus.name( ).equals(request.getEffectiveUserId()))
			throw new AccessDeniedException("Only admin can change walrus properties.");
		String storageRootDirectory = request.getStorageRootDirectory();
		if(storageRootDirectory != null)  {
			volumeStorageManager.setRootDirectory(storageRootDirectory);
			snapshotStorageManager.setRootDirectory(storageRootDirectory);
			StorageProperties.storageRootDirectory = storageRootDirectory;
		}

		Integer maxTotalVolumeSize = request.getMaxTotalVolumeSize();
		if(maxTotalVolumeSize != null) 
			StorageProperties.MAX_TOTAL_VOLUME_SIZE = maxTotalVolumeSize;        
		Integer maxVolumeSize = request.getMaxVolumeSize();
		if(maxVolumeSize != null)
			StorageProperties.MAX_VOLUME_SIZE = maxVolumeSize;
		String storageInterface = request.getStorageInterface();
		if(storageInterface != null)
			StorageProperties.iface = storageInterface;
		Boolean zeroFillVolumes = request.getZeroFillVolumes();
		if(zeroFillVolumes != null)
			StorageProperties.zeroFillVolumes = zeroFillVolumes;        
		check();
		//test connection to Walrus
		StorageProperties.updateWalrusUrl();
		try {
			blockManager.checkPreconditions();
			StorageProperties.enableStorage = true;
		} catch (Exception ex) {
			StorageProperties.enableStorage = false;
			LOG.error(ex);
		}
		updateConfig();
		return reply;
	}

	public GetStorageConfigurationResponseType GetStorageConfiguration(GetStorageConfigurationType request) throws EucalyptusCloudException {
		GetStorageConfigurationResponseType reply = (GetStorageConfigurationResponseType) request.getReply();
		if(Component.eucalyptus.name( ).equals(request.getEffectiveUserId()))
			throw new AccessDeniedException("Only admin can change walrus properties.");
		if(StorageProperties.NAME.equals(request.getName())) {
			reply.setStorageRootDirectory(StorageProperties.storageRootDirectory);
			reply.setMaxTotalVolumeSize(StorageProperties.MAX_TOTAL_VOLUME_SIZE);
			reply.setMaxVolumeSize(StorageProperties.MAX_VOLUME_SIZE);
			reply.setStorageInterface(StorageProperties.iface);
			reply.setZeroFillVolumes(StorageProperties.zeroFillVolumes);
			reply.setName(StorageProperties.NAME);
		}
		return reply;
	}

	public GetStorageVolumeResponseType GetStorageVolume(GetStorageVolumeType request) throws EucalyptusCloudException {
		GetStorageVolumeResponseType reply = (GetStorageVolumeResponseType) request.getReply();
		if(!StorageProperties.enableStorage) {
			LOG.error("BlockStorage has been disabled. Please check your setup");
			return reply;
		}

		String volumeId = request.getVolumeId();

		EntityWrapper<VolumeInfo> db = StorageController.getEntityWrapper();
		VolumeInfo volumeInfo = new VolumeInfo();
		volumeInfo.setVolumeId(volumeId);
		List <VolumeInfo> volumeInfos = db.query(volumeInfo);
		if(volumeInfos.size() > 0) {
			VolumeInfo foundVolumeInfo = volumeInfos.get(0);
			String deviceName = blockManager.getVolumeProperty(volumeId);
			reply.setVolumeId(foundVolumeInfo.getVolumeId());
			reply.setSize(foundVolumeInfo.getSize().toString());
			reply.setStatus(foundVolumeInfo.getStatus());
			reply.setSnapshotId(foundVolumeInfo.getSnapshotId());
			if(deviceName != null)
				reply.setActualDeviceName(deviceName);
			else
				reply.setActualDeviceName("invalid");
		} else {
			db.rollback();
			throw new NoSuchVolumeException(volumeId);
		}
		db.commit();
		return reply;
	}

	public DeleteStorageVolumeResponseType DeleteStorageVolume(DeleteStorageVolumeType request) throws EucalyptusCloudException {
		DeleteStorageVolumeResponseType reply = (DeleteStorageVolumeResponseType) request.getReply();
		if(!StorageProperties.enableStorage) {
			LOG.error("BlockStorage has been disabled. Please check your setup");
			return reply;
		}

		String volumeId = request.getVolumeId();

		EntityWrapper<VolumeInfo> db = StorageController.getEntityWrapper();
		VolumeInfo volumeInfo = new VolumeInfo();
		volumeInfo.setVolumeId(volumeId);
		List<VolumeInfo> volumeList = db.query(volumeInfo);

		reply.set_return(Boolean.FALSE);
		if(volumeList.size() > 0) {
			VolumeInfo foundVolume = volumeList.get(0);
			//check its status
			String status = foundVolume.getStatus();
			if(status.equals(StorageProperties.Status.available.toString()) || status.equals(StorageProperties.Status.failed.toString())) {
				try {
					blockManager.deleteVolume(volumeId);
					volumeStorageManager.deleteObject("", volumeId);
					db.delete(foundVolume);
					EucaSemaphoreDirectory.removeSemaphore(volumeId);
					if(StorageProperties.trackUsageStatistics) { 
						blockStorageStatistics.decrementVolumeCount();
						blockStorageStatistics.updateSpaceUsed(-(foundVolume.getSize() * StorageProperties.GB));
					}
					reply.set_return(Boolean.TRUE);
				} catch ( IOException ex) {
					LOG.error(ex, ex);
				}
			}
		} 
		db.commit();
		return reply;
	}

	public CreateStorageSnapshotResponseType CreateStorageSnapshot( CreateStorageSnapshotType request ) throws EucalyptusCloudException {
		CreateStorageSnapshotResponseType reply = ( CreateStorageSnapshotResponseType ) request.getReply();

		StorageProperties.updateWalrusUrl();
		if(!StorageProperties.enableSnapshots) {
			LOG.error("Snapshots have been disabled. Please check connection to Walrus.");
			return reply;
		}

		String volumeId = request.getVolumeId();
		String snapshotId = request.getSnapshotId();
		EntityWrapper<VolumeInfo> db = StorageController.getEntityWrapper();
		VolumeInfo volumeInfo = new VolumeInfo(volumeId);
		List<VolumeInfo> volumeInfos = db.query(volumeInfo);

		if(volumeInfos.size() > 0) {
			VolumeInfo foundVolumeInfo = volumeInfos.get(0);
			//check status
			if(foundVolumeInfo.getStatus().equals(StorageProperties.Status.available.toString())) {
				//create snapshot
				if(StorageProperties.shouldEnforceUsageLimits) {
					int volSize = foundVolumeInfo.getSize();
					int totalSnapshotSize = 0;
					SnapshotInfo snapInfo = new SnapshotInfo();
					EntityWrapper<SnapshotInfo> dbSnap = db.recast(SnapshotInfo.class);

					List<SnapshotInfo> snapInfos = dbSnap.query(snapInfo);
					for (SnapshotInfo sInfo : snapInfos) {
						totalSnapshotSize += blockManager.getSnapshotSize(sInfo.getSnapshotId());
					}
					if((totalSnapshotSize + volSize) > WalrusProperties.MAX_TOTAL_SNAPSHOT_SIZE) {
						db.rollback();
						throw new EntityTooLargeException(snapshotId);
					}
				}
				EntityWrapper<SnapshotInfo> db2 = StorageController.getEntityWrapper();
				edu.ucsb.eucalyptus.cloud.entities.SnapshotInfo snapshotInfo = new edu.ucsb.eucalyptus.cloud.entities.SnapshotInfo(snapshotId);
				snapshotInfo.setUserName(foundVolumeInfo.getUserName());
				snapshotInfo.setVolumeId(volumeId);
				Date startTime = new Date();
				snapshotInfo.setStartTime(startTime);
				snapshotInfo.setProgress("0");
				snapshotInfo.setStatus(StorageProperties.Status.creating.toString());
				db2.add(snapshotInfo);
				//snapshot asynchronously
				String snapshotSet = "snapset-" + UUID.randomUUID();

				Snapshotter snapshotter = new Snapshotter(snapshotSet, volumeId, snapshotId);
				snapshotter.start();
				db2.commit();
				db.commit();
				reply.setSnapshotId(snapshotId);
				reply.setVolumeId(volumeId);
				reply.setStatus(snapshotInfo.getStatus());
				reply.setStartTime(DateUtils.format(startTime.getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
				reply.setProgress(snapshotInfo.getProgress());
			} else {
				db.rollback();
				throw new VolumeNotReadyException(volumeId);
			}
		} else {
			db.rollback();
			throw new NoSuchVolumeException(volumeId);
		}
		return reply;
	}

	//returns snapshots in progress or at the SC
	public DescribeStorageSnapshotsResponseType DescribeStorageSnapshots( DescribeStorageSnapshotsType request ) throws EucalyptusCloudException {
		DescribeStorageSnapshotsResponseType reply = ( DescribeStorageSnapshotsResponseType ) request.getReply();
		checker.transferPendingSnapshots();
		List<String> snapshotSet = request.getSnapshotSet();
		ArrayList<SnapshotInfo> snapshotInfos = new ArrayList<SnapshotInfo>();
		EntityWrapper<SnapshotInfo> db = StorageController.getEntityWrapper();
		if((snapshotSet != null) && !snapshotSet.isEmpty()) {
			for(String snapshotSetEntry: snapshotSet) {
				SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotSetEntry);
				List<SnapshotInfo> foundSnapshotInfos = db.query(snapshotInfo);
				if(foundSnapshotInfos.size() > 0) {
					snapshotInfos.add(foundSnapshotInfos.get(0));
				}
			}
		} else {
			SnapshotInfo snapshotInfo = new SnapshotInfo();
			List<SnapshotInfo> foundSnapshotInfos = db.query(snapshotInfo);
			for(SnapshotInfo snapInfo : foundSnapshotInfos) {
				snapshotInfos.add(snapInfo);
			}
		}

		ArrayList<StorageSnapshot> snapshots = reply.getSnapshotSet();
		for(SnapshotInfo snapshotInfo: snapshotInfos) {
			snapshots.add(convertSnapshotInfo(snapshotInfo));
			if(snapshotInfo.getStatus().equals(StorageProperties.Status.failed.toString()))
				checker.cleanFailedSnapshot(snapshotInfo.getSnapshotId());
		}
		db.commit();
		return reply;
	}


	public DeleteStorageSnapshotResponseType DeleteStorageSnapshot( DeleteStorageSnapshotType request ) throws EucalyptusCloudException {
		DeleteStorageSnapshotResponseType reply = ( DeleteStorageSnapshotResponseType ) request.getReply();

		StorageProperties.updateWalrusUrl();
		if(!StorageProperties.enableSnapshots) {
			LOG.error("Snapshots have been disabled. Please check connection to Walrus.");
			return reply;
		}

		String snapshotId = request.getSnapshotId();

		EntityWrapper<SnapshotInfo> db = StorageController.getEntityWrapper();
		SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
		List<SnapshotInfo> snapshotInfos = db.query(snapshotInfo);

		reply.set_return(true);
		if(snapshotInfos.size() > 0) {
			SnapshotInfo  foundSnapshotInfo = snapshotInfos.get(0);
			String status = foundSnapshotInfo.getStatus();
			if(status.equals(StorageProperties.Status.available.toString()) || status.equals(StorageProperties.Status.failed.toString())) {
				try {
					SnapshotInfo snapInfo = new SnapshotInfo();
					snapInfo.setVolumeId(foundSnapshotInfo.getVolumeId());
					List<SnapshotInfo> snapInfos = db.query(snapInfo);
					blockManager.deleteSnapshot(snapshotId);
					snapshotStorageManager.deleteObject("", snapshotId);
					db.delete(foundSnapshotInfo);
					db.commit();
					SnapshotDeleter snapshotDeleter = new SnapshotDeleter(snapshotId);
					snapshotDeleter.start();				
				} catch (IOException ex) {
					LOG.error(ex);
				}
			} else {
				//snapshot is still in progress.
				reply.set_return(false);
				db.rollback();
				throw new SnapshotInUseException(snapshotId);
			}
		} else {
			//the SC knows nothing about this snapshot. It should be deleted directly from Walrus
			db.rollback();
		}
		return reply;
	}

	public void DeleteWalrusSnapshot(String snapshotId) {
		HttpWriter httpWriter = new HttpWriter("DELETE", "snapset", snapshotId, "DeleteWalrusSnapshot", null);
		try {
			httpWriter.run();
		} catch(Exception ex) {
			LOG.error(ex);
		}
	}

	public CreateStorageVolumeResponseType CreateStorageVolume(CreateStorageVolumeType request) throws EucalyptusCloudException {
		CreateStorageVolumeResponseType reply = (CreateStorageVolumeResponseType) request.getReply();

		if(!StorageProperties.enableStorage) {
			LOG.error("BlockStorage has been disabled. Please check your setup");
			return reply;
		}

		String snapshotId = request.getSnapshotId();
		String userId = request.getUserId();
		String volumeId = request.getVolumeId();

		//in GB
		String size = request.getSize();
		int sizeAsInt = 0;
		if(StorageProperties.shouldEnforceUsageLimits && StorageProperties.trackUsageStatistics) {
			if(size != null) {
				sizeAsInt = Integer.parseInt(size);
				int totalVolumeSize = (int)(blockStorageStatistics.getTotalSpaceUsed() / StorageProperties.GB);
				;				if(((totalVolumeSize + sizeAsInt) > StorageProperties.MAX_TOTAL_VOLUME_SIZE) ||
						(sizeAsInt > StorageProperties.MAX_VOLUME_SIZE))
					throw new EntityTooLargeException(volumeId);
			}
		}
		EntityWrapper<VolumeInfo> db = StorageController.getEntityWrapper();

		VolumeInfo volumeInfo = new VolumeInfo(volumeId);
		List<VolumeInfo> volumeInfos = db.query(volumeInfo);
		if(volumeInfos.size() > 0) {
			db.rollback();
			throw new VolumeAlreadyExistsException(volumeId);
		}
		volumeInfo.setUserName(userId);
		volumeInfo.setSize(sizeAsInt);
		volumeInfo.setStatus(StorageProperties.Status.creating.toString());
		Date creationDate = new Date();
		volumeInfo.setCreateTime(creationDate);
		if(snapshotId != null) {
			volumeInfo.setSnapshotId(snapshotId);
			reply.setSnapshotId(snapshotId);
		}
		db.add(volumeInfo);
		reply.setVolumeId(volumeId);
		reply.setCreateTime(DateUtils.format(creationDate.getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
		reply.setSize(size);
		reply.setStatus(volumeInfo.getStatus());
		db.commit();

		//create volume asynchronously
		VolumeCreator volumeCreator = new VolumeCreator(volumeId, "snapset", snapshotId, sizeAsInt);
		volumeCreator.start();

		return reply;
	}

	public class VolumeCreator extends Thread {
		private String volumeId;
		private String snapshotId;
		private int size;

		public VolumeCreator(String volumeId, String snapshotSetName, String snapshotId, int size) {
			this.volumeId = volumeId;
			this.snapshotId = snapshotId;
			this.size = size;
		}

		public void run() {
			boolean success = true;
			if(snapshotId != null) {
				EntityWrapper<SnapshotInfo> db = StorageController.getEntityWrapper();
				try {
					SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
					List<SnapshotInfo> foundSnapshotInfos = db.query(snapshotInfo);
					if(foundSnapshotInfos.size() == 0) {
						db.commit();
						getSnapshot(snapshotId);
						size = blockManager.createVolume(volumeId, snapshotId);
					} else {
						SnapshotInfo foundSnapshotInfo = foundSnapshotInfos.get(0);
						if(!foundSnapshotInfo.getStatus().equals(StorageProperties.Status.available.toString())) {
							success = false;
							db.rollback();
							LOG.warn("snapshot " + foundSnapshotInfo.getSnapshotId() + " not available.");
						} else {
							db.commit();
							size = blockManager.createVolume(volumeId, snapshotId);
						}
					}
				} catch(Exception ex) {
					success = false;
					LOG.error(ex);
				}
			} else {
				try {
					assert(size > 0);
					blockManager.createVolume(volumeId, size);
				} catch(Exception ex) {
					success = false;
					LOG.error(ex);
				}
			}
			EntityWrapper<VolumeInfo> db = StorageController.getEntityWrapper();
			VolumeInfo volumeInfo = new VolumeInfo(volumeId);
			try {
				VolumeInfo foundVolumeInfo = db.getUnique(volumeInfo);
				if(foundVolumeInfo != null) {
					if(success) {
						if(StorageProperties.shouldEnforceUsageLimits && 
								StorageProperties.trackUsageStatistics) {
							int totalVolumeSize = (int)(blockStorageStatistics.getTotalSpaceUsed() / StorageProperties.GB);
							;							if((totalVolumeSize + size) > StorageProperties.MAX_TOTAL_VOLUME_SIZE ||
									(size > StorageProperties.MAX_VOLUME_SIZE)) {
								LOG.error("Volume size limit exceeeded");
								db.commit();
								checker.cleanFailedVolume(volumeId);
								return;
							}
						}
						foundVolumeInfo.setStatus(StorageProperties.Status.available.toString());
					} else {
						foundVolumeInfo.setStatus(StorageProperties.Status.failed.toString());
					}
					if(snapshotId != null) {
						foundVolumeInfo.setSize(size);
					}
				} else {
					db.rollback();
					throw new EucalyptusCloudException();
				}
				db.commit();
				if(StorageProperties.trackUsageStatistics) {
					blockStorageStatistics.incrementVolumeCount();
					blockStorageStatistics.updateSpaceUsed((size * StorageProperties.GB));
				}
			} catch(EucalyptusCloudException ex) {
				LOG.error(ex);
			}
		}
	}

	private void getSnapshot(String snapshotId) throws EucalyptusCloudException {
		StorageProperties.updateWalrusUrl();
		if(!StorageProperties.enableSnapshots) {
			LOG.error("Snapshot functionality disabled. Please check connection to Walrus");
			throw new EucalyptusCloudException("could not connect to Walrus.");
		}
		String snapshotLocation = "snapshots" + "/" + snapshotId;
		String absoluteSnapshotPath = StorageProperties.storageRootDirectory + "/" + snapshotId;
		File file = new File(absoluteSnapshotPath);
		if(file.exists()) {
			//something went wrong in the past. remove and retry
			file.delete();
		}
		HttpReader snapshotGetter = new HttpReader(snapshotLocation, null, file, "GetWalrusSnapshot", "", true);
		snapshotGetter.run();
		int snapshotSize = (int)(file.length() / StorageProperties.GB);
		if(snapshotSize == 0) {
			throw new EucalyptusCloudException("could not download snapshot: " + snapshotId + " from Walrus.");
		} 
		EntityWrapper<SnapshotInfo> db = StorageController.getEntityWrapper();
		SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
		snapshotInfo.setProgress("100");
		snapshotInfo.setStartTime(new Date());
		snapshotInfo.setStatus(StorageProperties.Status.available.toString());
		blockManager.addSnapshot(snapshotId);
	}



	public DescribeStorageVolumesResponseType DescribeStorageVolumes(DescribeStorageVolumesType request) throws EucalyptusCloudException {
		DescribeStorageVolumesResponseType reply = (DescribeStorageVolumesResponseType) request.getReply();

		List<String> volumeSet = request.getVolumeSet();
		ArrayList<VolumeInfo> volumeInfos = new ArrayList<VolumeInfo>();
		EntityWrapper<VolumeInfo> db = StorageController.getEntityWrapper();

		if((volumeSet != null) && !volumeSet.isEmpty()) {
			for(String volumeSetEntry: volumeSet) {
				VolumeInfo volumeInfo = new VolumeInfo(volumeSetEntry);
				List<VolumeInfo> foundVolumeInfos = db.query(volumeInfo);
				if(foundVolumeInfos.size() > 0) {
					volumeInfos.add(foundVolumeInfos.get(0));
				}
			}
		} else {
			VolumeInfo volumeInfo = new VolumeInfo();
			List<VolumeInfo> foundVolumeInfos = db.query(volumeInfo);
			for(VolumeInfo volInfo : foundVolumeInfos) {
				volumeInfos.add(volInfo);
			}
		}

		ArrayList<StorageVolume> volumes = reply.getVolumeSet();
		for(VolumeInfo volumeInfo: volumeInfos) {
			volumes.add(convertVolumeInfo(volumeInfo));
			if(volumeInfo.getStatus().equals(StorageProperties.Status.failed.toString())) {
				LOG.warn( "Volume looks like it has failed removing it: " + volumeInfo.getVolumeId() );
				checker.cleanFailedVolume(volumeInfo.getVolumeId());
			}
		}
		db.commit();
		return reply;
	}


	private StorageVolume convertVolumeInfo(VolumeInfo volInfo) throws EucalyptusCloudException {
		StorageVolume volume = new StorageVolume();
		String volumeId = volInfo.getVolumeId();
		volume.setVolumeId(volumeId);
		volume.setStatus(volInfo.getStatus());
		volume.setCreateTime(DateUtils.format(volInfo.getCreateTime().getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
		volume.setSize(String.valueOf(volInfo.getSize()));
		volume.setSnapshotId(volInfo.getSnapshotId());
		String deviceName = blockManager.getVolumeProperty(volumeId);
		if(deviceName != null)
			volume.setActualDeviceName(deviceName);
		else
			volume.setActualDeviceName("invalid");
		return volume;
	}

	private StorageSnapshot convertSnapshotInfo(SnapshotInfo snapInfo) {
		StorageSnapshot snapshot = new StorageSnapshot();
		snapshot.setVolumeId(snapInfo.getVolumeId());
		snapshot.setStatus(snapInfo.getStatus());
		snapshot.setSnapshotId(snapInfo.getSnapshotId());
		String progress = snapInfo.getProgress();
		progress = progress != null ? progress + "%" : progress;
		snapshot.setProgress(progress);
		snapshot.setStartTime(DateUtils.format(snapInfo.getStartTime().getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
		return snapshot;
	}

	public class Snapshotter extends Thread {
		private String volumeId;
		private String snapshotId;
		private String volumeBucket;
		private String snapshotFileName;

		public Snapshotter(String volumeBucket, String volumeId, String snapshotId) {
			this.volumeBucket = volumeBucket;
			this.volumeId = volumeId;
			this.snapshotId = snapshotId;
		}

		public void run() {
			try {
				EucaSemaphore semaphore = EucaSemaphoreDirectory.getSolitarySemaphore(volumeId);
				try {
					semaphore.acquire();
				} catch(InterruptedException ex) {
					throw new EucalyptusCloudException("semaphore could not be acquired");
				}
				blockManager.createSnapshot(volumeId, snapshotId);
				semaphore.release();
				List<String> returnValues = blockManager.prepareForTransfer(snapshotId);
				snapshotFileName = returnValues.get(0);
				transferSnapshot();				
			} catch(EucalyptusCloudException ex) {
				LOG.error(ex);
			}
		}

		private void transferSnapshot() {
			long size = 0;

			File snapshotFile = new File(snapshotFileName);
			assert(snapshotFile.exists());
			size += snapshotFile.length();
			SnapshotProgressCallback callback = new SnapshotProgressCallback(snapshotId, size, StorageProperties.TRANSFER_CHUNK_SIZE);
			Map<String, String> httpParamaters = new HashMap<String, String>();
			HttpWriter httpWriter;
			httpWriter = new HttpWriter("PUT", snapshotFile, callback, volumeBucket, snapshotId, "StoreSnapshot", null, httpParamaters);
			try {
				httpWriter.run();
			} catch(Exception ex) {
				LOG.error(ex, ex);
				checker.cleanFailedSnapshot(snapshotId);
			}
		}
	}

	private class SnapshotDeleter extends Thread {
		private String snapshotId;

		public SnapshotDeleter(String snapshotId) {
			this.snapshotId = snapshotId;
		}

		public void run() {
			HttpWriter httpWriter = new HttpWriter("DELETE", "snapset", snapshotId, "DeleteWalrusSnapshot", null);
			try {
				httpWriter.run();
			} catch(EucalyptusCloudException ex) {
				LOG.error(ex);
			}
		}
	}

}
