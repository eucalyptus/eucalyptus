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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;
import org.mule.RequestContext;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.storage.BlockStorageChecker;
import com.eucalyptus.storage.BlockStorageManagerFactory;
import com.eucalyptus.storage.LogicalStorageManager;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.StorageProperties;
import com.eucalyptus.ws.util.Messaging;

import edu.ucsb.eucalyptus.cloud.AccessDeniedException;
import edu.ucsb.eucalyptus.cloud.EntityTooLargeException;
import edu.ucsb.eucalyptus.cloud.NoSuchEntityException;
import edu.ucsb.eucalyptus.cloud.NoSuchVolumeException;
import edu.ucsb.eucalyptus.cloud.SnapshotInUseException;
import edu.ucsb.eucalyptus.cloud.VolumeAlreadyExistsException;
import edu.ucsb.eucalyptus.cloud.VolumeNotReadyException;
import edu.ucsb.eucalyptus.cloud.entities.SnapshotInfo;
import edu.ucsb.eucalyptus.cloud.entities.StorageInfo;
import edu.ucsb.eucalyptus.cloud.entities.VolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.WalrusInfo;
import edu.ucsb.eucalyptus.msgs.ComponentProperty;
import edu.ucsb.eucalyptus.msgs.ConvertVolumesResponseType;
import edu.ucsb.eucalyptus.msgs.ConvertVolumesType;
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
import edu.ucsb.eucalyptus.msgs.StorageSnapshot;
import edu.ucsb.eucalyptus.msgs.StorageVolume;
import edu.ucsb.eucalyptus.msgs.UpdateStorageConfigurationResponseType;
import edu.ucsb.eucalyptus.msgs.UpdateStorageConfigurationType;
import edu.ucsb.eucalyptus.util.EucaSemaphore;
import edu.ucsb.eucalyptus.util.EucaSemaphoreDirectory;

public class BlockStorage {

	private static Logger LOG = Logger.getLogger(BlockStorage.class);

	static LogicalStorageManager blockManager;
	static BlockStorageChecker checker;
	static BlockStorageStatistics blockStorageStatistics;
	static VolumeService volumeService;
	static SnapshotService snapshotService;

	public static void configure() {
		StorageProperties.updateWalrusUrl();
		StorageProperties.updateName();
		StorageProperties.updateStorageHost();
		blockManager = BlockStorageManagerFactory.getBlockStorageManager();
		checker = new BlockStorageChecker(blockManager);
		if(StorageProperties.trackUsageStatistics) 
			blockStorageStatistics = new BlockStorageStatistics();
		volumeService = new VolumeService();
		snapshotService = new SnapshotService();
		blockManager.configure();
		blockManager.initialize();
		StorageProperties.enableSnapshots = StorageProperties.enableStorage = true;
		try {
			startupChecks();
		} catch(EucalyptusCloudException ex) {
			LOG.error("Startup checks failed ", ex);
		}
	}

	public BlockStorage() {}

	private static void startupChecks() throws EucalyptusCloudException {
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

	public UpdateStorageConfigurationResponseType UpdateStorageConfiguration(UpdateStorageConfigurationType request) throws EucalyptusCloudException {
		UpdateStorageConfigurationResponseType reply = (UpdateStorageConfigurationResponseType) request.getReply();
		if(Component.eucalyptus.name( ).equals(request.getEffectiveUserId()))
			throw new AccessDeniedException("Only admin can change walrus properties.");
		//test connection to Walrus
		StorageProperties.updateWalrusUrl();
		try {
			blockManager.checkPreconditions();
			StorageProperties.enableStorage = true;
		} catch (Exception ex) {
			StorageProperties.enableStorage = false;
			LOG.error(ex);
		}
		if(request.getStorageParams() != null) {
			for(ComponentProperty param : request.getStorageParams()) {
				LOG.debug("Storage Param: " + param.getDisplayName() + " Qname: " + param.getQualifiedName() + " Value: " + param.getValue());
			}
			blockManager.setStorageProps(request.getStorageParams());
		}
		return reply;
	}

	public GetStorageConfigurationResponseType GetStorageConfiguration(GetStorageConfigurationType request) throws EucalyptusCloudException {
		GetStorageConfigurationResponseType reply = (GetStorageConfigurationResponseType) request.getReply();
		StorageProperties.updateName();
		if(Component.eucalyptus.name( ).equals(request.getEffectiveUserId()))
			throw new AccessDeniedException("Only admin can change walrus properties.");
		if(StorageProperties.NAME.equals(request.getName())) {
			reply.setName(StorageProperties.NAME);
			ArrayList<ComponentProperty> storageParams = blockManager.getStorageProps();
			reply.setStorageParams(storageParams);
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

		EntityWrapper<VolumeInfo> db = StorageProperties.getEntityWrapper();
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

		EntityWrapper<VolumeInfo> db = StorageProperties.getEntityWrapper();
		VolumeInfo volumeInfo = new VolumeInfo();
		volumeInfo.setVolumeId(volumeId);
		List<VolumeInfo> volumeList = db.query(volumeInfo);

		//always return true. 
		reply.set_return(Boolean.TRUE);
		if(volumeList.size() > 0) {
			VolumeInfo foundVolume = volumeList.get(0);
			//check its status
			String status = foundVolume.getStatus();
			if(status.equals(StorageProperties.Status.available.toString()) || status.equals(StorageProperties.Status.failed.toString())) {
				VolumeDeleter volumeDeleter = new VolumeDeleter(volumeId);
				volumeService.add(volumeDeleter);
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
		EntityWrapper<VolumeInfo> db = StorageProperties.getEntityWrapper();
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
					snapInfo.setStatus(StorageProperties.Status.available.toString());
					EntityWrapper<SnapshotInfo> dbSnap = db.recast(SnapshotInfo.class);

					List<SnapshotInfo> snapInfos = dbSnap.query(snapInfo);
					for (SnapshotInfo sInfo : snapInfos) {
						try {
							totalSnapshotSize += blockManager.getSnapshotSize(sInfo.getSnapshotId());
						} catch(EucalyptusCloudException e) {
							LOG.error(e);
						}
					}
					if((totalSnapshotSize + volSize) > WalrusInfo.getWalrusInfo().getStorageMaxTotalSnapshotSizeInGb()) {
						db.rollback();
						throw new EntityTooLargeException(snapshotId);
					}
				}
				EntityWrapper<SnapshotInfo> db2 = StorageProperties.getEntityWrapper();
				SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
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
				snapshotService.add(snapshotter);
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
		EntityWrapper<SnapshotInfo> db = StorageProperties.getEntityWrapper();
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

		EntityWrapper<SnapshotInfo> db = StorageProperties.getEntityWrapper();
		SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
		List<SnapshotInfo> snapshotInfos = db.query(snapshotInfo);

		reply.set_return(true);
		if(snapshotInfos.size() > 0) {
			SnapshotInfo  foundSnapshotInfo = snapshotInfos.get(0);
			String status = foundSnapshotInfo.getStatus();
			db.commit();
			if(status.equals(StorageProperties.Status.available.toString()) || status.equals(StorageProperties.Status.failed.toString())) {
				SnapshotDeleter snapshotDeleter = new SnapshotDeleter(snapshotId);
				snapshotService.add(snapshotDeleter);				
			} else {
				//snapshot is still in progress.
				reply.set_return(false);
				throw new SnapshotInUseException(snapshotId);
			}
		} else {
			//the SC knows nothing about this snapshot.
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
				;				if(((totalVolumeSize + sizeAsInt) > StorageInfo.getStorageInfo().getMaxTotalVolumeSizeInGb()) ||
						(sizeAsInt > StorageInfo.getStorageInfo().getMaxVolumeSizeInGB()))
					throw new EntityTooLargeException(volumeId);
			}
		}
		EntityWrapper<VolumeInfo> db = StorageProperties.getEntityWrapper();

		VolumeInfo volumeInfo = new VolumeInfo(volumeId);
		List<VolumeInfo> volumeInfos = db.query(volumeInfo);
		if(volumeInfos.size() > 0) {
			db.rollback();
			throw new VolumeAlreadyExistsException(volumeId);
		}
		if(snapshotId != null) {
			SnapshotInfo snapInfo = new SnapshotInfo(snapshotId);
			snapInfo.setStatus(StorageProperties.Status.available.toString());
			EntityWrapper<SnapshotInfo> dbSnap = db.recast(SnapshotInfo.class);			
			List<SnapshotInfo> snapInfos = dbSnap.query(snapInfo);
			if(snapInfos.size() != 1) {
				db.rollback();
				throw new NoSuchEntityException("Snapshot " + snapshotId + " does not exist or is unavailable");
			}
			volumeInfo.setSnapshotId(snapshotId);
			reply.setSnapshotId(snapshotId);
		}
		volumeInfo.setUserName(userId);
		volumeInfo.setSize(sizeAsInt);
		volumeInfo.setStatus(StorageProperties.Status.creating.toString());
		Date creationDate = new Date();
		volumeInfo.setCreateTime(creationDate);
		db.add(volumeInfo);
		reply.setVolumeId(volumeId);
		reply.setCreateTime(DateUtils.format(creationDate.getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
		reply.setSize(size);
		reply.setStatus(volumeInfo.getStatus());
		db.commit();

		//create volume asynchronously
		VolumeCreator volumeCreator = new VolumeCreator(volumeId, "snapset", snapshotId, sizeAsInt);
		volumeService.add(volumeCreator);

		return reply;
	}

	//TODO: this depends on which target you are getting the snapshot to and should be handled by a lower level manager.
	private void getSnapshot(String snapshotId, String snapDestination) throws EucalyptusCloudException {
		if(!StorageProperties.enableSnapshots) {
			LOG.error("Snapshot functionality disabled. Please check connection to Walrus");
			throw new EucalyptusCloudException("could not connect to Walrus.");
		}
		String snapshotLocation = "snapshots" + "/" + snapshotId;
		String absoluteSnapshotPath = snapDestination;
		File file = new File(absoluteSnapshotPath);
		HttpReader snapshotGetter = new HttpReader(snapshotLocation, null, file, "GetWalrusSnapshot", "", true, blockManager.getStorageRootDirectory());
		snapshotGetter.run();
		EntityWrapper<SnapshotInfo> db = StorageProperties.getEntityWrapper();
		SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
		snapshotInfo.setProgress("100");
		snapshotInfo.setStartTime(new Date());
		snapshotInfo.setStatus(StorageProperties.Status.available.toString());
		blockManager.addSnapshot(snapshotId);
	}

	private int getSnapshotSize(String snapshotId) throws EucalyptusCloudException {
		StorageProperties.updateWalrusUrl();
		if(!StorageProperties.enableSnapshots) {
			LOG.error("Snapshot functionality disabled. Please check connection to Walrus");
			throw new EucalyptusCloudException("could not connect to Walrus.");
		}
		String snapshotLocation = "snapshots" + "/" + snapshotId;
		HttpReader snapshotGetter = new HttpReader(snapshotLocation, null, null, "GetWalrusSnapshotSize", "");
		int size = Integer.parseInt(snapshotGetter.getResponseHeader("SnapshotSize"));
		return size;
	}

	public DescribeStorageVolumesResponseType DescribeStorageVolumes(DescribeStorageVolumesType request) throws EucalyptusCloudException {
		DescribeStorageVolumesResponseType reply = (DescribeStorageVolumesResponseType) request.getReply();

		List<String> volumeSet = request.getVolumeSet();
		ArrayList<VolumeInfo> volumeInfos = new ArrayList<VolumeInfo>();
		EntityWrapper<VolumeInfo> db = StorageProperties.getEntityWrapper();

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

	public ConvertVolumesResponseType ConvertVolumes(ConvertVolumesType request) throws EucalyptusCloudException {
		ConvertVolumesResponseType reply = (ConvertVolumesResponseType) request.getReply();
		String provider = request.getOriginalProvider();
		provider = "com.eucalyptus.storage." + provider;
		if(!blockManager.getClass().getName().equals(provider)) {
			//different backend provider. Try upgrade
			try {
				LogicalStorageManager fromBlockManager = (LogicalStorageManager) ClassLoader.getSystemClassLoader().loadClass(provider).newInstance();
				fromBlockManager.checkPreconditions();
				//initialize fromBlockManager
				new VolumesConvertor(fromBlockManager).start();
			} catch(InstantiationException e) {
				LOG.error(e);
				throw new EucalyptusCloudException(e);
			} catch(ClassNotFoundException e) {
				LOG.error(e);
				throw new EucalyptusCloudException(e);
			} catch(IllegalAccessException e) {
				LOG.error(e);
				throw new EucalyptusCloudException(e);
			}
		}
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

	public abstract class SnapshotTask implements Runnable {
	}

	public abstract class VolumeTask implements Runnable {
	}

	public class Snapshotter extends SnapshotTask {
		private String volumeId;
		private String snapshotId;
		private String volumeBucket;
		private String snapshotFileName;

		public Snapshotter(String volumeBucket, String volumeId, String snapshotId) {
			this.volumeBucket = volumeBucket;
			this.volumeId = volumeId;
			this.snapshotId = snapshotId;
		}

		@Override
		public void run() {
			EucaSemaphore semaphore = EucaSemaphoreDirectory.getSolitarySemaphore(volumeId);
			try {
				try {
					semaphore.acquire();
				} catch(InterruptedException ex) {
					throw new EucalyptusCloudException("semaphore could not be acquired");
				}
				List<String> returnValues = blockManager.createSnapshot(volumeId, snapshotId);
				semaphore.release();
				if(returnValues.size() < 2) {
					throw new EucalyptusCloudException("Unable to transfer snapshot");
				}
				snapshotFileName = returnValues.get(0);
				transferSnapshot(returnValues.get(1));
				blockManager.finishVolume(snapshotId);
				SnapshotInfo snapInfo = new SnapshotInfo(snapshotId);
				EntityWrapper<SnapshotInfo> db = StorageProperties.getEntityWrapper();
				try {
					SnapshotInfo snapshotInfo = db.getUnique(snapInfo);
					snapshotInfo.setStatus(StorageProperties.Status.available.toString());
				} catch(EucalyptusCloudException e) {
					LOG.error(e);
				} finally {
					db.commit();
				}
			} catch(Exception ex) {
				semaphore.release();
				try {
					blockManager.finishVolume(snapshotId);
				} catch (EucalyptusCloudException e1) {
					LOG.error(e1);
				}
				SnapshotInfo snapInfo = new SnapshotInfo(snapshotId);
				EntityWrapper<SnapshotInfo> db = StorageProperties.getEntityWrapper();
				try {
					SnapshotInfo snapshotInfo = db.getUnique(snapInfo);
					snapshotInfo.setStatus(StorageProperties.Status.failed.toString());
				} catch(EucalyptusCloudException e) {
					LOG.error(e);
				} finally {
					db.commit();
				}
				LOG.error(ex);
			}
		}

		private void transferSnapshot(String sizeAsString) throws EucalyptusCloudException {
			long size = Long.parseLong(sizeAsString);

			File snapshotFile = new File(snapshotFileName);
			assert(snapshotFile.exists());
			//do a little test to check if we can read from it
			FileInputStream snapInStream = null;
			try {
				snapInStream = new FileInputStream(snapshotFile);
				byte[] bytes = new byte[1024];
				if(snapInStream.read(bytes) <= 0) {
					throw new EucalyptusCloudException("Unable to read snapshot file");
				}				
			} catch (FileNotFoundException e) {
				throw new EucalyptusCloudException(e);
			} catch (IOException e) {
				throw new EucalyptusCloudException(e);
			} finally {
				if(snapInStream != null) {
					try {
						snapInStream.close();
					} catch (IOException e) {
						throw new EucalyptusCloudException(e);
					}
				}
			}
			SnapshotProgressCallback callback = new SnapshotProgressCallback(snapshotId, size, StorageProperties.TRANSFER_CHUNK_SIZE);
			Map<String, String> httpParamaters = new HashMap<String, String>();
			HttpWriter httpWriter;
			httpWriter = new HttpWriter("PUT", snapshotFile, sizeAsString, callback, volumeBucket, snapshotId, "StoreSnapshot", null, httpParamaters);
			try {
				httpWriter.run();
			} catch(Exception ex) {
				LOG.error(ex, ex);
				checker.cleanFailedSnapshot(snapshotId);
			}
		}
	}

	private class SnapshotDeleter extends SnapshotTask {
		private String snapshotId;

		public SnapshotDeleter(String snapshotId) {
			this.snapshotId = snapshotId;
		}

		@Override
		public void run() {
			try {
				blockManager.deleteSnapshot(snapshotId);
			} catch (EucalyptusCloudException e1) {
				LOG.error(e1);
				return;
			}
			SnapshotInfo snapInfo = new SnapshotInfo(snapshotId);
			EntityWrapper<SnapshotInfo> db = StorageProperties.getEntityWrapper();
			SnapshotInfo foundSnapshotInfo;
			try {
				foundSnapshotInfo = db.getUnique(snapInfo);
				db.delete(foundSnapshotInfo);
				db.commit();
			} catch (EucalyptusCloudException e) {
				db.rollback();
				LOG.error(e);
				return;
			}
			HttpWriter httpWriter = new HttpWriter("DELETE", "snapset", snapshotId, "DeleteWalrusSnapshot", null);
			try {
				httpWriter.run();
			} catch(EucalyptusCloudException ex) {
				LOG.error(ex);
			}
		}
	}

	public class VolumeCreator extends VolumeTask {
		private String volumeId;
		private String snapshotId;
		private int size;

		public VolumeCreator(String volumeId, String snapshotSetName, String snapshotId, int size) {
			this.volumeId = volumeId;
			this.snapshotId = snapshotId;
			this.size = size;
		}

		@Override
		public void run() {
			boolean success = true;
			if(snapshotId != null) {
				EntityWrapper<SnapshotInfo> db = StorageProperties.getEntityWrapper();
				try {
					SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
					List<SnapshotInfo> foundSnapshotInfos = db.query(snapshotInfo);
					if(foundSnapshotInfos.size() == 0) {
						db.commit();						
						//get snapshot size from walrus
						int sizeExpected;
						if(size <= 0) {
							sizeExpected = getSnapshotSize(snapshotId);
						} else {
							sizeExpected = size;
						}
						String snapDestination = blockManager.prepareSnapshot(snapshotId, sizeExpected);
						getSnapshot(snapshotId, snapDestination);
						size = blockManager.createVolume(volumeId, snapshotId, size);
					} else {
						SnapshotInfo foundSnapshotInfo = foundSnapshotInfos.get(0);
						if(!foundSnapshotInfo.getStatus().equals(StorageProperties.Status.available.toString())) {
							success = false;
							db.rollback();
							LOG.warn("snapshot " + foundSnapshotInfo.getSnapshotId() + " not available.");
						} else {
							db.commit();
							size = blockManager.createVolume(volumeId, snapshotId, size);
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
			EntityWrapper<VolumeInfo> db = StorageProperties.getEntityWrapper();
			VolumeInfo volumeInfo = new VolumeInfo(volumeId);
			try {
				VolumeInfo foundVolumeInfo = db.getUnique(volumeInfo);
				if(foundVolumeInfo != null) {
					if(success) {
						if(StorageProperties.shouldEnforceUsageLimits && 
								StorageProperties.trackUsageStatistics) {
							int totalVolumeSize = (int)(blockStorageStatistics.getTotalSpaceUsed() / StorageProperties.GB);
							if((totalVolumeSize + size) > StorageInfo.getStorageInfo().getMaxTotalVolumeSizeInGb() ||
									(size > StorageInfo.getStorageInfo().getMaxVolumeSizeInGB())) {
								LOG.error("Volume size limit exceeeded");
								db.commit();
								checker.cleanFailedVolume(volumeId);
								return;
							}
						}
						foundVolumeInfo.setStatus(StorageProperties.Status.available.toString());
						if(StorageProperties.trackUsageStatistics) {
							blockStorageStatistics.incrementVolumeCount((size * StorageProperties.GB));
						}
					} else {
						foundVolumeInfo.setStatus(StorageProperties.Status.failed.toString());
					}
					if(snapshotId != null) {
						foundVolumeInfo.setSize(size);
					}
				} else {
					throw new EucalyptusCloudException();
				}
				db.commit();
			} catch(EucalyptusCloudException ex) {
				db.rollback();
				LOG.error(ex);
			}
		}
	}

	public class VolumeDeleter extends VolumeTask {
		private String volumeId;

		public VolumeDeleter(String volumeId) {
			this.volumeId = volumeId;
		}

		@Override
		public void run() {
			try {
				blockManager.deleteVolume(volumeId);
			} catch (EucalyptusCloudException e1) {
				LOG.error(e1);
			}
			EntityWrapper<VolumeInfo> db = StorageProperties.getEntityWrapper();
			VolumeInfo foundVolume;
			try {
				foundVolume = db.getUnique(new VolumeInfo(volumeId));
				db.delete(foundVolume);
				db.commit();
				EucaSemaphoreDirectory.removeSemaphore(volumeId);
				if(StorageProperties.trackUsageStatistics) { 
					blockStorageStatistics.decrementVolumeCount(-(foundVolume.getSize() * StorageProperties.GB));
				}
			} catch (EucalyptusCloudException e) {
				db.rollback();
			}
		}
	}

	public class VolumesConvertor extends Thread {
		private LogicalStorageManager fromBlockManager;

		public VolumesConvertor(LogicalStorageManager fromBlockManager) {
			this.fromBlockManager = fromBlockManager;
		}

		@Override
		public void run() {
			//This is a heavy weight operation. It must execute atomically.
			//All other volume operations are forbidden when a conversion is in progress.
			synchronized (blockManager) {
				StorageProperties.enableStorage = StorageProperties.enableSnapshots = false;
				EntityWrapper<VolumeInfo> db = StorageProperties.getEntityWrapper();
				VolumeInfo volumeInfo = new VolumeInfo();
				volumeInfo.setStatus(StorageProperties.Status.available.toString());
				List<VolumeInfo> volumeInfos = db.query(volumeInfo);
				List<VolumeInfo> volumes = new ArrayList<VolumeInfo>();
				volumes.addAll(volumeInfos);

				SnapshotInfo snapInfo = new SnapshotInfo();
				snapInfo.setStatus(StorageProperties.Status.available.toString());
				EntityWrapper<SnapshotInfo> dbSnap = db.recast(SnapshotInfo.class);
				List<SnapshotInfo> snapshotInfos = dbSnap.query(snapInfo);
				List<SnapshotInfo> snapshots = new ArrayList<SnapshotInfo>();
				snapshots.addAll(snapshotInfos);

				db.commit();

				for(VolumeInfo volume : volumes) {
					try {
						String volumeId = volume.getVolumeId();
						LOG.info("Converting volume: " + volumeId);
						String volumePath = fromBlockManager.getVolumePath(volumeId);
						blockManager.importVolume(volumeId, volumePath, volume.getSize());
						fromBlockManager.finishVolume(volumeId);
						LOG.info("Done converting volume: " + volumeId);
					} catch (Exception ex) {
						LOG.error(ex);
						//this one failed, continue processing the rest
					}
				}

				for(SnapshotInfo snap : snapshots) {
					try {
						String snapshotId = snap.getSnapshotId();
						LOG.info("Converting snapshot: " + snapshotId);
						String snapPath = fromBlockManager.getSnapshotPath(snapshotId);
						int size = fromBlockManager.getSnapshotSize(snapshotId);
						blockManager.importSnapshot(snapshotId, snap.getVolumeId(), snapPath, size);
						fromBlockManager.finishVolume(snapshotId);
						LOG.info("Done converting snapshot: " + snapshotId);
					} catch (Exception ex) {
						LOG.error(ex);
						//this one failed, continue processing the rest
					}
				}
				LOG.info("Conversion complete");
				StorageProperties.enableStorage = StorageProperties.enableSnapshots = true;
			}
		}
	}
}
