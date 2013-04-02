/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

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
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.UUID;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.blockstorage.Volume;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.reporting.event.SnapShotEvent;
import com.eucalyptus.storage.BlockStorageChecker;
import com.eucalyptus.storage.CheckerTask;
import com.eucalyptus.storage.LogicalStorageManager;
import com.eucalyptus.storage.StorageManagers;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.StorageProperties;

import edu.ucsb.eucalyptus.cloud.AccessDeniedException;
import edu.ucsb.eucalyptus.cloud.EntityTooLargeException;
import edu.ucsb.eucalyptus.cloud.NoSuchEntityException;
import edu.ucsb.eucalyptus.cloud.NoSuchVolumeException;
import edu.ucsb.eucalyptus.cloud.SnapshotInUseException;
import edu.ucsb.eucalyptus.cloud.VolumeAlreadyExistsException;
import edu.ucsb.eucalyptus.cloud.VolumeNotReadyException;
import edu.ucsb.eucalyptus.cloud.VolumeSizeExceededException;
import edu.ucsb.eucalyptus.cloud.entities.SnapshotInfo;
import edu.ucsb.eucalyptus.cloud.entities.StorageInfo;
import edu.ucsb.eucalyptus.cloud.entities.VolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.WalrusInfo;
import edu.ucsb.eucalyptus.msgs.AttachStorageVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.AttachStorageVolumeType;
import edu.ucsb.eucalyptus.msgs.CloneVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.CloneVolumeType;
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
import edu.ucsb.eucalyptus.msgs.DetachStorageVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.DetachStorageVolumeType;
import edu.ucsb.eucalyptus.msgs.GetStorageConfigurationResponseType;
import edu.ucsb.eucalyptus.msgs.GetStorageConfigurationType;
import edu.ucsb.eucalyptus.msgs.GetStorageVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.GetStorageVolumeType;
import edu.ucsb.eucalyptus.msgs.StorageSnapshot;
import edu.ucsb.eucalyptus.msgs.StorageVolume;
import edu.ucsb.eucalyptus.msgs.UpdateStorageConfigurationResponseType;
import edu.ucsb.eucalyptus.msgs.UpdateStorageConfigurationType;
import edu.ucsb.eucalyptus.storage.StorageCheckerService;
import edu.ucsb.eucalyptus.util.EucaSemaphore;
import edu.ucsb.eucalyptus.util.EucaSemaphoreDirectory;
import edu.ucsb.eucalyptus.util.SystemUtil;
import edu.ucsb.eucalyptus.util.SystemUtil.CommandOutput;

public class BlockStorage {
	private static Logger LOG = Logger.getLogger(BlockStorage.class);

	static LogicalStorageManager blockManager;
	static BlockStorageChecker checker;
	static VolumeService volumeService;
	static SnapshotService snapshotService;
	static StorageCheckerService checkerService;

	//TODO: zhill, this can be added later for snapshot abort capabilities
	//static ConcurrentHashMap<String,HttpTransfer> httpTransferMap; //To keep track of current transfers to support aborting

	public static Random randomGenerator = new Random();

	public static void configure() throws EucalyptusCloudException {
		StorageProperties.updateWalrusUrl();
		StorageProperties.updateName();
		StorageProperties.updateStorageHost();

		try {
			blockManager = StorageManagers.getInstance();
			if(blockManager != null) {
				blockManager.initialize();
			}
			else {
				throw new EucalyptusCloudException("Got null block manager. Cannot configure.");
			}
		} catch (Exception e) {
			throw new EucalyptusCloudException(e);
		}

		checker = new BlockStorageChecker(blockManager);
		volumeService = new VolumeService();
		snapshotService = new SnapshotService();
		checkerService = new StorageCheckerService();
		//httpTransferMap = new ConcurrentHashMap<String, HttpTransfer>();
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

	public static void check() throws EucalyptusCloudException {
		blockManager.checkReady();
	}

	public static void stop() throws EucalyptusCloudException {
		if(blockManager != null) {
			LOG.info("Stopping blockmanager");
			blockManager.stop();
		}
		//clean all state.
		blockManager = null;
		checker = null;
		if(volumeService != null) {
			volumeService.shutdown();
		}
		if(snapshotService != null) {
			snapshotService.shutdown();
		}
		if(checkerService != null) {
			checkerService.shutdown();
		}
		StorageProperties.enableSnapshots = StorageProperties.enableStorage = false;
	}

	public static void enable() throws EucalyptusCloudException {
		blockManager.configure();
		//blockManager.initialize();
		try {
			startupChecks();
		} catch(EucalyptusCloudException ex) {
			LOG.error("Startup checks failed ", ex);
		}
		blockManager.enable();
		checkerService.add(new VolumeStateChecker(blockManager));
		//add any block manager checkers
		for(CheckerTask checker : blockManager.getCheckers()) {
			checkerService.add(checker);
		}
		checkerService.add(new VolumeDeleterTask());
		checkerService.add(new SnapshotDeleterTask());
		StorageProperties.enableSnapshots = StorageProperties.enableStorage = true;
	}

	public static void disable() throws EucalyptusCloudException {
		blockManager.disable();
	}

	public static void addChecker(CheckerTask checkerTask) {
		if(checkerService != null) {
			checkerService.add(checkerTask);
		}
	}

	public UpdateStorageConfigurationResponseType UpdateStorageConfiguration(UpdateStorageConfigurationType request) throws EucalyptusCloudException {
		UpdateStorageConfigurationResponseType reply = (UpdateStorageConfigurationResponseType) request.getReply();
		if(ComponentIds.lookup(Eucalyptus.class).name( ).equals(request.getEffectiveUserId()))
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
		if(ComponentIds.lookup(Eucalyptus.class).name( ).equals(request.getEffectiveUserId()))
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

		reply.set_return(Boolean.FALSE);
		if(volumeList.size() > 0) {
			VolumeInfo foundVolume = volumeList.get(0);
			//check its status
			String status = foundVolume.getStatus();
			if(status.equals(StorageProperties.Status.available.toString()) || status.equals(StorageProperties.Status.failed.toString())) {
				foundVolume.setStatus(StorageProperties.Status.deleting.toString());
				reply.set_return(Boolean.TRUE);
			} 
		} 
		db.commit();
		return reply;
	}

	/**
	 * Checks to see if a new snapshot of size volSize will exceed the quota
	 * @param volSize
	 * @return
	 */
	private boolean totalSnapshotSizeLimitExceeded(String snapshotId, int volSize) throws EucalyptusCloudException {

		int totalSnapshotSize = 0;
		EntityTransaction dbTrans = Entities.get(SnapshotInfo.class);
		Criteria query = Entities.createCriteria(SnapshotInfo.class);
		query.setReadOnly(true);

		//TODO: zhill, fix this logic by adding a size value to the snapshot record, should do the calculation on the DB
		// this will be very poor for performance as the number of snapshots increases.

		//Only look for snaps that are not failed and not error
		query.add(Restrictions.not(Restrictions.and(
				Restrictions.eq("status", StorageProperties.Status.failed.toString()), 
				Restrictions.eq("status", StorageProperties.Status.error.toString()))));
		try {
			List<SnapshotInfo> snapInfos = (List<SnapshotInfo>)query.list();			
			for (SnapshotInfo sInfo : snapInfos) {
				try {
					totalSnapshotSize += blockManager.getSnapshotSize(sInfo.getSnapshotId());
				} catch(EucalyptusCloudException e) {
					LOG.error(e);
				}
			}
			int sizeLimitGB = WalrusInfo.getWalrusInfo().getStorageMaxTotalSnapshotSizeInGb();
			LOG.debug("Snapshot " + snapshotId + " checking snapshot total size of  " + totalSnapshotSize + " against limit of " + sizeLimitGB);
			return (totalSnapshotSize + volSize) > sizeLimitGB; 
		} catch(final Throwable e) {
			LOG.error("Error finding total snapshot used size " + e.getMessage());
			throw new EucalyptusCloudException("Failed to check snapshot total size limit",e);
		} finally {
			if(dbTrans.isActive()) {
				dbTrans.rollback();
			}
		}

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
		EntityTransaction dbTrans = Entities.get(VolumeInfo.class);
		VolumeInfo sourceVolumeInfo = null;
		try {
			VolumeInfo volumeInfo = new VolumeInfo(volumeId);
			sourceVolumeInfo = Entities.uniqueResult(volumeInfo);
			dbTrans.commit();
		} catch(NoSuchElementException e) {
			LOG.debug("Volume " + volumeId + " not found in db");
			throw new NoSuchVolumeException(volumeId);
		} catch(final Throwable e) {
			LOG.warn("Volume " + volumeId + " error getting info from db. May not exist. " + e.getMessage());
			throw new EucalyptusCloudException("Could not get volume information for volume " + volumeId, e);
		} finally {
			if(dbTrans.isActive()) {
				dbTrans.rollback();
			}
			dbTrans = null;
		}

		if(sourceVolumeInfo == null) {
			//Another check to be sure that we have the source volume
			throw new NoSuchVolumeException(volumeId);
		} else {
			//check status
			if(!sourceVolumeInfo.getStatus().equals(StorageProperties.Status.available.toString())) {
				throw new VolumeNotReadyException(volumeId);
			} else {
				//create snapshot
				if(StorageProperties.shouldEnforceUsageLimits && totalSnapshotSizeLimitExceeded(snapshotId, sourceVolumeInfo.getSize())) {
					LOG.info("Snapshot " + snapshotId + " exceeds total snapshot size limit of " + WalrusInfo.getWalrusInfo().getStorageMaxTotalSnapshotSizeInGb());
					throw new EntityTooLargeException(snapshotId);
				}

				SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
				EntityTransaction snapTrans = Entities.get(SnapshotInfo.class);
				Date startTime = new Date();
				try {
					snapshotInfo.setUserName(sourceVolumeInfo.getUserName());
					snapshotInfo.setVolumeId(volumeId);					
					snapshotInfo.setStartTime(startTime);
					snapshotInfo.setProgress("0");
					snapshotInfo.setStatus(StorageProperties.Status.creating.toString());

					/* Change to support sync snap consistency point set on CLC round-trip */
					/*
					 * Always do this operation. On backends that don't support it they will
					 * return null. In that case it is effectively a no-op and we continue normal
					 * async snapshot.
					 * 
					 * If the snap point is set, then we update the DB properly. 
					 */
					String snapPointId = null;
					try {
						//This will be a no-op if the backend doesn't support it. Will return null.
						snapPointId = blockManager.createSnapshotPoint(volumeId, snapshotId);
						if(snapPointId == null) {
							LOG.debug("Synchronous snap point not supported for this backend. Cleanly skipped.");
						} else {
							snapshotInfo.setSnapPointId(snapPointId);
						}
						//Do a commit here because the snapshotter expects to find db entry.
						snapshotInfo.setStatus(StorageProperties.Status.creating.toString());

						Context ctx = null;
						try {
							ctx = Contexts.lookup(request.getCorrelationId());
							if(!ctx.getChannel().isOpen()) {
								throw new NoSuchContextException("Channel is closed");
							}
						} catch(NoSuchContextException e) {
							if(snapPointId != null) {
								//Other end hung up, mark this as failed since this is a sync operation
								throw new EucalyptusCloudException("Channel closed, aborting snapshot.");
							}
						}						
					} catch(EucalyptusCloudException e) {
						//If the snapshot was done but took too long then delete the snap and fail the op.	
						try {
							blockManager.deleteSnapshotPoint(volumeId, snapshotId, snapPointId);
						} catch(Exception ex) {
							LOG.error("Snapshot " + snapshotId + " exception on snap point cleanup after failure: " + e.getMessage());
						}
						LOG.error("Snapshot " + snapshotId + " failed to create snap point successfully: " + e.getMessage());
						throw e;
					} finally {
						Entities.persist(snapshotInfo);
					}

					/* Resume old code path and finish the snapshot process if already started */				
					//snapshot asynchronously
					String snapshotSet = "snapset-" + UUID.randomUUID();					
					Snapshotter snapshotter = new Snapshotter(snapshotSet, volumeId, snapshotId, snapPointId);
					snapshotService.add(snapshotter);

					reply.setSnapshotId(snapshotId);
					reply.setVolumeId(volumeId);
					reply.setStartTime(DateUtils.format(startTime.getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
					reply.setProgress(snapshotInfo.getProgress());
				} catch(EucalyptusCloudException cloudEx) {
					snapshotInfo.setStatus(StorageProperties.Status.failed.toString());
					LOG.error("Snapshot " + snapshotId + " creation failed with exception ", cloudEx);
					throw cloudEx;
				} catch(final Throwable e) {
					snapshotInfo.setStatus(StorageProperties.Status.failed.toString());
					LOG.error("Snapshot " + snapshotId + " Error committing state update to failed", e);
					throw new EucalyptusCloudException("Snapshot " + snapshotId + " unexpected throwable exception caught", e);
				} finally {
					if(snapTrans.isActive()) {						
						snapTrans.commit();
					}
					snapTrans = null;
				}
				reply.setStatus(snapshotInfo.getStatus());
			}
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

		reply.set_return(false);
		if(snapshotInfos.size() > 0) {
			SnapshotInfo  foundSnapshotInfo = snapshotInfos.get(0);
			String status = foundSnapshotInfo.getStatus();
			if(status.equals(StorageProperties.Status.available.toString()) || status.equals(StorageProperties.Status.failed.toString())) {
				foundSnapshotInfo.setStatus(StorageProperties.Status.deleting.toString());
				db.commit();
				reply.set_return(true);
			} else {
				//snapshot is still in progress.
				db.rollback();
				throw new SnapshotInUseException(snapshotId);
			}
		} else {
			//the SC knows nothing about this snapshot.
			db.rollback();
		}
		return reply;
	}

	/* TODO: zhill, removed this since it isn't necessary, but can be added-back later when we have time for full dev and testing
	 * public AbortStorageSnapshotResponseType AbortSnapshotPoint( AbortStorageSnapshotType request ) throws EucalyptusCloudException {
		AbortStorageSnapshotResponseType reply = ( AbortStorageSnapshotResponseType ) request.getReply();
		String snapshotId = request.getSnapshotId();

		EntityWrapper<SnapshotInfo> db = StorageProperties.getEntityWrapper();
		SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
		List<SnapshotInfo> snapshotInfos = db.query(snapshotInfo);

		reply.set_return(true);
		if(snapshotInfos.size() > 0) {
			SnapshotInfo  foundSnapshotInfo = snapshotInfos.get(0);
			String status = foundSnapshotInfo.getStatus();
			if(status.equals(StorageProperties.Status.available.toString()) || status.equals(StorageProperties.Status.failed.toString())) {
				SnapshotDeleter snapshotDeleter = new SnapshotDeleter(snapshotId);
				snapshotService.add(snapshotDeleter);
			} else {
				//snapshot is still in progress.
				foundSnapshotInfo.setStatus(StorageProperties.Status.failed.toString());			
				db.commit();
				 if(httpTransferMap.contains(snapshotId)) {
					LOG.info("Aborting http transfer for " + snapshotId);
					httpTransferMap.get(snapshotId).abortTransfer();
				}
			}
		} else {
			//the SC knows nothing about this snapshot.
			db.rollback();
		}

		return reply;
	}*/

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
		String parentVolumeId = request.getParentVolumeId();
		String userId = request.getUserId();
		String volumeId = request.getVolumeId();

		//in GB
		String size = request.getSize();
		int sizeAsInt = 0;
		if(StorageProperties.shouldEnforceUsageLimits) {
			if(size != null) {
				int totalVolumeSize = 0;
				VolumeInfo volInfo = new VolumeInfo();
				EntityWrapper<VolumeInfo> db = StorageProperties.getEntityWrapper();
				List<VolumeInfo> volInfos = db.query(volInfo);
				for (VolumeInfo vInfo : volInfos) {
					if (!vInfo.getStatus().equals(StorageProperties.Status.failed.toString()) &&
							!vInfo.getStatus().equals(StorageProperties.Status.error.toString())) {
						totalVolumeSize += vInfo.getSize();
					}
				}
				db.rollback();
				sizeAsInt = Integer.parseInt(size);
				if(((totalVolumeSize + sizeAsInt) > StorageInfo.getStorageInfo().getMaxTotalVolumeSizeInGb())) {
					throw new VolumeSizeExceededException(volumeId, "Total Volume Size Limit Exceeded");
				}
				if(sizeAsInt > StorageInfo.getStorageInfo().getMaxVolumeSizeInGB()) {
					throw new VolumeSizeExceededException(volumeId, "Max Volume Size Limit Exceeded");
				}
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
			snapInfo.setScName(null);
			snapInfo.setStatus(StorageProperties.Status.available.toString());
			EntityWrapper<SnapshotInfo> dbSnap = db.recast(SnapshotInfo.class);			
			List<SnapshotInfo> snapInfos = dbSnap.query(snapInfo);
			if(snapInfos.size() == 0) {
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
		VolumeCreator volumeCreator = new VolumeCreator(volumeId, "snapset", snapshotId, parentVolumeId, sizeAsInt);
		volumeService.add(volumeCreator);

		return reply;
	}

	/* Make sure snapDestination is NOT a bare block device */
	private String getSnapshot(String snapshotId) throws EucalyptusCloudException {
		if(!StorageProperties.enableSnapshots) {
			LOG.error("Snapshot functionality disabled. Please check connection to Walrus");
			throw new EucalyptusCloudException("could not connect to Walrus.");
		}
		/*if(snapDestination.startsWith("/dev/")) {
			throw new EucalyptusCloudException("Cannot get snapshot directly to block device: " + snapDestination);
		}*/

		String snapshotLocation = "snapshots" + "/" + snapshotId;

		String tmpUncompressedFileName = null;
		String tmpCompressedFileName = null;
		File tmpCompressedFile = null;
		int retry = 0;
		int maxRetry = 5;

		do{
			tmpUncompressedFileName = StorageProperties.storageRootDirectory + File.separator + snapshotId + "-" + String.valueOf(randomGenerator.nextInt());
			tmpCompressedFileName = tmpUncompressedFileName + ".gz";
			tmpCompressedFile = new File(tmpCompressedFileName);
		} while (tmpCompressedFile.exists() && retry++ < maxRetry);

		// This should be *very* rare
		if (retry >= maxRetry) {
			// Nothing to clean up at this point
			throw new EucalyptusCloudException("Could not get a temporary file for snapshot download after " + maxRetry + " attempts");
		}

		// Download the snapshot from walrus
		try {
			HttpReader snapshotGetter = new HttpReader(snapshotLocation, null, tmpCompressedFile, "GetWalrusSnapshot", "");
			snapshotGetter.run();
		} catch (Exception ex) {
			// Cleanup the compressed snapshot
			cleanupFile(tmpCompressedFile);
			throw new EucalyptusCloudException("Failed to download snapshot " + snapshotId + " from Walrus", ex);
		}

		// Uncompress the snapshot and move it to the right location
		try{
			CommandOutput output = SystemUtil.runWithRawOutput(new String[] { "/bin/gunzip", tmpCompressedFile.getAbsolutePath() });
			if (output.returnValue != 0) {
				throw new EucalyptusCloudException("Failed to uncompress snapshot " + snapshotId + " due to: " + output.error);
			}
		} catch (Exception ex) {
			if (ex instanceof EucalyptusCloudException) {
				throw (EucalyptusCloudException) ex;
			} else {
				throw new EucalyptusCloudException("Failed to uncompress and/or move snapshot " + snapshotId, ex);
			}
		} finally {
			// Cleanup the compressed snapshot
			cleanupFile(tmpCompressedFile);
		}

		return tmpUncompressedFileName;

		// LOG.info("Downloading snapshot " + snapshotId + " from Walrus to " + snapDestination);
		// String snapshotLocation = "snapshots" + "/" + snapshotId;
		// String absoluteSnapshotPath = snapDestination;
		// File file = new File(absoluteSnapshotPath);
		// HttpReader snapshotGetter = new HttpReader(snapshotLocation, null, file, "GetWalrusSnapshot", "", true, blockManager.getStorageRootDirectory());
		// snapshotGetter.run();
	}

	private void cleanupFile(String fileName) {
		try {
			cleanupFile(new File(fileName));
		} catch (Exception e) {
			LOG.error("Failed to delete file", e);
		}
	}

	private void cleanupFile(File file) {
		if (file != null && file.exists()) {
			try {
				file.delete();
			} catch (Exception e) {
				LOG.error("Failed to delete file", e);
			}
		}
	}

	/*
	 * Does a check of the snapshot's status as reflected in the DB.
	 */
	private static boolean isSnapshotMarkedFailed(String snapshotId) {
		EntityTransaction db = Entities.get(SnapshotInfo.class);
		db.setRollbackOnly();
		try {
			SnapshotInfo snap = Entities.uniqueResult(new SnapshotInfo(snapshotId));
			if(snap != null && StorageProperties.Status.failed.toString().equals(snap.getStatus())) {
				return true;
			}
		} catch(Exception e) {
			LOG.error("Error determining status of snapshot " + snapshotId);
		} finally {
			db.rollback();
		}
		return false;
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
				LOG.warn( "Failed volume, removing it: " + volumeInfo.getVolumeId() );
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

	public AttachStorageVolumeResponseType attachVolume(AttachStorageVolumeType request) throws EucalyptusCloudException {
		AttachStorageVolumeResponseType reply = request.getReply();
		String volumeId = request.getVolumeId();
		ArrayList<String> nodeIqns = request.getNodeIqns();

		EntityWrapper<VolumeInfo> db = StorageProperties.getEntityWrapper();
		try {
			VolumeInfo volumeInfo = db.getUnique(new VolumeInfo(volumeId));			
		} catch (EucalyptusCloudException ex) {
			LOG.error("Unable to find volume: " + volumeId + ex);
			throw new NoSuchEntityException("Unable to find volume: " + volumeId + ex);
		} finally {
			db.commit();
		}
		try {
			String deviceConnectString = blockManager.attachVolume(volumeId, nodeIqns);
			reply.setRemoteDeviceString(deviceConnectString);
		} catch (EucalyptusCloudException ex) {
			LOG.error(ex, ex);
			throw ex;
		}
		return reply;
	}

	public DetachStorageVolumeResponseType detachVolume(DetachStorageVolumeType request) throws EucalyptusCloudException {
		DetachStorageVolumeResponseType reply = request.getReply();
		String volumeId = request.getVolumeId();
		String nodeIqn = request.getNodeIqn();

		EntityWrapper<VolumeInfo> db = StorageProperties.getEntityWrapper();
		try {
			VolumeInfo volumeInfo = db.getUnique(new VolumeInfo(volumeId));			
		} catch (EucalyptusCloudException ex) {
			LOG.error("Unable to find volume: " + volumeId + ex);
			throw new NoSuchEntityException("Unable to find volume: " + volumeId + ex);
		} finally {
			db.commit();
		}
		try {
			blockManager.detachVolume(volumeId, nodeIqn);
		} catch (EucalyptusCloudException ex) {
			throw ex;
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
		private String snapPointId;

		/**
		 * Initializes the Snapshotter task. snapPointId should be null if no snap point has been created yet.
		 * @param volumeBucket
		 * @param volumeId
		 * @param snapshotId
		 * @param snapPointId
		 */
		public Snapshotter(String volumeBucket, String volumeId, String snapshotId, String snapPointId) {
			this.volumeBucket = volumeBucket;
			this.volumeId = volumeId;
			this.snapshotId = snapshotId;
			this.snapPointId = snapPointId;
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
				Boolean shouldTransferSnapshots = true;
				List<String> returnValues = null;

				try {
					//Check to ensure that a failed/cancellation has not be set
					if(!isSnapshotMarkedFailed(snapshotId)) {
						shouldTransferSnapshots = StorageInfo.getStorageInfo().getShouldTransferSnapshots();
						returnValues = blockManager.createSnapshot(this.volumeId, this.snapshotId, this.snapPointId, shouldTransferSnapshots);
					} else {
						throw new EucalyptusCloudException("Snapshot " + this.snapshotId + " marked as failed by another thread");
					}
				} finally {
					semaphore.release();
				}

				if(shouldTransferSnapshots) {
					if(returnValues.size() < 2) {
						throw new EucalyptusCloudException("Unable to transfer snapshot");
					}
					snapshotFileName = returnValues.get(0);
					String snapshotSize = returnValues.get(1);
					int retry = StorageInfo.getStorageInfo().getMaxSnapTransferRetries();
					boolean transferSuccess = false;

					//Use exponential backoff for retries
					int backoffTime = 1;
					while(!transferSuccess) {
						if(!isSnapshotMarkedFailed(snapshotId)) {
							try {
								//Keep checking to see if failed state has been set by another thread								
								transferSnapshot(snapshotSize);
								transferSuccess = true;
							} catch(Exception e) {
								LOG.warn("Transfer failed. Retrying");
								if(retry > 0) {
									retry --;	
									Thread.sleep(backoffTime * 1000);
									backoffTime = backoffTime * 2;
								} else {
									//Use retry counter so that we can include this exception info
									throw new EucalyptusCloudException("Snapshot transfer failed", e);
								}
							}
						} else {
							throw new EucalyptusCloudException("Snapshot " + this.snapshotId + " marked as failed by another thread");
						}
					}

					try {
						blockManager.finishVolume(snapshotId);
					} catch(EucalyptusCloudException ex) {
						blockManager.cleanSnapshot(snapshotId);
						LOG.error(ex);
					}
				}

				SnapshotInfo snapInfo = new SnapshotInfo(snapshotId);
				SnapshotInfo snapshotInfo = null;
				EntityWrapper<SnapshotInfo> db = StorageProperties.getEntityWrapper();
				try {
					snapshotInfo = db.getUnique(snapInfo);
					snapshotInfo.setStatus(StorageProperties.Status.available.toString());
					snapshotInfo.setProgress("100");
					snapshotInfo.setSnapPointId(null); //remove snap point Id since it is no longer valid
				} catch(EucalyptusCloudException e) {
					LOG.error(e);
				} finally {
					db.commit();
				}

				if ( snapshotInfo != null ) {
					//Fire the event to indicate the usage for reporting
					try {
						final int snapshotSize = blockManager.getSnapshotSize(snapshotInfo.getSnapshotId());
						final String volumeUuid = Transactions.find( Volume.named( null, volumeId ) ).getNaturalId();
						ListenerRegistry.getInstance().fireEvent( SnapShotEvent.with(
								SnapShotEvent.forSnapShotCreate(
										snapshotSize,
										volumeUuid,
										volumeId ),
										snapshotInfo.getNaturalId(),
										snapshotInfo.getSnapshotId(),
										snapshotInfo.getUserName() ) ); // snapshot info user name is user id
					} catch ( final Throwable e ) {
						LOG.error( e, e  );
					}
				}
			} catch(Exception ex) {
				try {
					LOG.error("Disconnecting snapshot " + snapshotId + " on failed snapshot attempt");
					blockManager.finishVolume(snapshotId);
				} catch (EucalyptusCloudException e1) {					
					LOG.error("Disconnecting snapshot " + snapshotId + " on failed snapshot attempt", e1);
					blockManager.cleanSnapshot(snapshotId);
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
					throw new EucalyptusCloudException("Unable to read snapshot file: " + snapshotFileName);
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

			//TODO: zhill, leaving this in for future use if we add snapshot abort capability
			//Add to the map to make available for cancellation asynchronously
			//httpTransferMap.put(snapshotId, httpWriter);
			try {
				httpWriter.run();
			} catch(Exception ex) {
				LOG.error(ex, ex);
				checker.cleanFailedSnapshot(snapshotId);
			} 
			/*Part of snap abort if added
			 * finally {
				//httpTransferMap.remove(snapshotId);
			}*/
		}
	}

	public class VolumeCreator extends VolumeTask {
		private String volumeId;
		private String snapshotId;
		private String parentVolumeId;
		private int size;

		public VolumeCreator(String volumeId, String snapshotSetName, String snapshotId, String parentVolumeId, int size) {
			this.volumeId = volumeId;
			this.snapshotId = snapshotId;
			this.parentVolumeId = parentVolumeId;
			this.size = size;
		}

		// DO NOT throw any exceptions from cleaning routines. Log the errors and move on
		private void cleanFailedSnapshot(String snapshotId) {
			if(snapshotId == null) return;
			LOG.debug("Disconnecting and cleaning local snapshot after failed snapshot transfer: " + snapshotId);			
			try {
				blockManager.finishVolume(snapshotId);
			} catch(Exception e) {
				LOG.error("Error finishing failed snapshot " + snapshotId, e);
			} finally {
				try {
					blockManager.cleanSnapshot(snapshotId);
				} catch (Exception e) {
					LOG.error("Error deleting failed snapshot " + snapshotId, e);
				}
			}
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
						// Close DB connection. Dont want concurrent threads that could be waiting around for a semaphore below to keep the DB connection open
						db.commit();

						// This SC may not have the snapshot record in its DB, synchronize the snapshot setup
						EucaSemaphore semaphore = EucaSemaphoreDirectory.getSolitarySemaphore(snapshotId);
						try {
							semaphore.acquire();

							// Reopen the db connection, it was closed previously. Check if a concurrent thread already setup the snapshot for us
							db = StorageProperties.getEntityWrapper();
							foundSnapshotInfos = db.query(snapshotInfo);

							if (foundSnapshotInfos.size() == 0) {
								db.commit();

								// This SC definitely does not have a record of the snapshot in its DB. Check for the snpahsot on the storage backend.
								// Clusters/zones/partitions may be connected to the same storage backend in which case snapshot does not have to be downloaded
								// from Walrus.

								int walrusSnapSize = getSnapshotSize(snapshotId);

								if (!blockManager.getFromBackend(snapshotId, walrusSnapSize)) {

									// Snapshot does not exist on the backend. Needs to be downloaded from Walrus.

									/* START: Snapshot preparation on storage back end */

									String tmpSnapshotFileName = null;

									try {
										// Download the snapshot from walrus and find out the size
										tmpSnapshotFileName = getSnapshot(snapshotId);

										File snapFile = new File(tmpSnapshotFileName);
										if (!snapFile.exists()) {
											throw new EucalyptusCloudException("Unable to find snapshot " + snapshotId + "on SC");
										}

										long actualSnapSizeInMB = (long) Math.ceil((double) snapFile.length() / StorageProperties.MB);

										try {
											// Allocates the necessary resources on the backend
											String snapDestination = blockManager.prepareSnapshot(snapshotId, walrusSnapSize, actualSnapSizeInMB);

											if (snapDestination != null) {
												// Check if the destination is a block device
												if (snapDestination.startsWith("/dev/")) {
													CommandOutput output = SystemUtil.runWithRawOutput(new String[] { StorageProperties.EUCA_ROOT_WRAPPER,
															"dd", "if=" + tmpSnapshotFileName, "of=" + snapDestination, "bs=" + StorageProperties.blockSize });
													LOG.debug("Output of dd command: " + output.error);
													if (output.returnValue != 0) {
														throw new EucalyptusCloudException("Failed to copy the snapshot to the right location due to: "
																+ output.error);
													}
													cleanupFile(tmpSnapshotFileName);
												} else {
													// Rename file
													if (!snapFile.renameTo(new File(snapDestination))) {
														throw new EucalyptusCloudException("Failed to rename the snapshot");
													}
												}

												// Finish the snapshot
												blockManager.finishVolume(snapshotId);

												/*
												 * if(snapDestination.startsWith("/dev/")) { //Destination is a block device, so use a temp file first String
												 * tmpFileName = StorageProperties.storageRootDirectory + File.pathSeparator + snapshotId + "-" +
												 * Hashes.getRandom(8); File tmpFile = null; try { tmpFile = new File(tmpFileName); int retry = 0; int maxRetry
												 * = 5; while(tmpFile.exists() && retry++ < maxRetry) { LOG.debug("Temporary file " + tmpFileName +
												 * " for snapshot download already exists, trying another. Retry " + retry + " of " + maxRetry); tmpFileName =
												 * StorageProperties.storageRootDirectory + File.pathSeparator + snapshotId + "-" + Hashes.getRandom(8); tmpFile
												 * = new File(tmpFileName); }
												 * 
												 * //This should be *very* rare if(retry >= maxRetry) { throw new
												 * EucalyptusCloudException("Could not get a temporary file for snapshot download after " + maxRetry +
												 * " attempts"); }
												 * 
												 * getSnapshot(snapshotId, tmpFileName);
												 * 
												 * 
												 * //Copy file to block device if(SystemUtil.runAndGetCode(new
												 * String[]{"dd","if=",tmpFileName,"of=",snapDestination,"bs=",StorageProperties.blockSize}) < 0) { throw new
												 * EucalyptusCloudException("Failed to copy the downloaded snapshot in " + tmpFileName + " to block device " +
												 * snapDestination); } } catch(Exception e) { LOG.error("Failure downloading snapshot " + snapshotId);
												 * cleanFailedSnapshotDownload(snapshotId); throw new EucalyptusCloudException("Snapshot " + snapshotId +
												 * " download and transfer to block device " + snapDestination + " failed.", e); } finally { //Delete the temp
												 * file no matter what try { if(tmpFile != null && tmpFile.exists()) { tmpFile.delete(); } } catch(Exception e)
												 * { LOG.error("Could not delete temporary file " + tmpFileName +
												 * " during cleanup of failed volume creation from snapshot"); } } } else {
												 */

												// Download the snapshot from walrus and set it up on the storage device
												// try {
												// getSnapshot(snapshotId, snapDestination, sizeExpected);
												// } catch (EucalyptusCloudException e) {
												// LOG.error("Failed to get snapshot " + snapshotId + " from Walrus. Now cleaning up.");
												// cleanFailedSnapshotDownload(snapshotId);
												// throw e;
												// }
												// }

											} else {
												LOG.warn("Block Manager replied that " + snapshotId
														+ " not on backend, but snapshot preparation indicated that the snapshot is already present");
											}
										} catch (Exception ex) {
											LOG.error("Failed to prepare the snapshot " + snapshotId + " on SAN. Now cleaning up (snapshot on SAN)", ex);
											cleanFailedSnapshot(snapshotId);
											throw ex;
										}
									} catch (Exception ex) {
										LOG.error("Failed to prepare the snapshot " + snapshotId + " on the storage backend. Now cleaning up (snapshot on SC)",
												ex);
										cleanupFile(tmpSnapshotFileName);
										throw ex;
									}

									/* END: Snapshot preparation on storage back end */
								} else {
									// Snapshot does exist on the backend, no prepping required! Just create a record of it for this partition in the DB and get
									// going!
								}
								db = StorageProperties.getEntityWrapper();
								snapshotInfo = new SnapshotInfo(snapshotId);
								//snapshotInfo.setVolumeId(volumeId);
								snapshotInfo.setProgress("100");
								snapshotInfo.setStartTime(new Date());
								snapshotInfo.setStatus(StorageProperties.Status.available.toString());
								db.add(snapshotInfo);
								db.commit();
								// This should not be synchronized. Concurrent threads should only wait for snapshot setup to complete
								// size = blockManager.createVolume(volumeId, snapshotId, size); // leave the snapshot even on failure here
							} else {
								// This condition is hit when concurrent threads try to create a volume from a snapshot that did not exist on this SC
								// according to the first DB query. However, one of the concurrent threads might have finished the setup and hence a snapshot is
								// available to this thread
								SnapshotInfo foundSnapshotInfo = foundSnapshotInfos.get(0);
								if (!StorageProperties.Status.available.toString().equals(foundSnapshotInfo.getStatus())) {
									success = false;
									db.rollback();
									LOG.warn("snapshot " + foundSnapshotInfo.getSnapshotId() + " not available.");
								} else {
									db.commit();
									// This should not be synchronized
									// size = blockManager.createVolume(volumeId, snapshotId, size);
								}
							}
						} catch (InterruptedException ex) {
							throw new EucalyptusCloudException("semaphore could not be acquired");
						} finally {
							try{
								semaphore.release();
							} finally {
								EucaSemaphoreDirectory.removeSemaphore(snapshotId);
							}
						}

						// Create the volume from the snapshot, this can happen in parallel.
						size = blockManager.createVolume(volumeId, snapshotId, size);
					} else {
						// Snapshot does exist on this SC. Repeated logic, fix it!
						SnapshotInfo foundSnapshotInfo = foundSnapshotInfos.get(0);
						if(!StorageProperties.Status.available.toString().equals(foundSnapshotInfo.getStatus())) {
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
				//Not a snapshot-based volume create.
				try {
					if(parentVolumeId != null) {
						//Clone the parent volume.
						blockManager.cloneVolume(volumeId, parentVolumeId);
					} else {
						//Create a regular empty volume
						blockManager.createVolume(volumeId, size);
					}
				} catch(Exception ex) {
					success = false;
					LOG.error(ex,ex);
				}
			}
			//Create the necessary database entries for the newly created volume.
			EntityWrapper<VolumeInfo> db = StorageProperties.getEntityWrapper();
			VolumeInfo volumeInfo = new VolumeInfo(volumeId);
			try {
				VolumeInfo foundVolumeInfo = db.getUnique(volumeInfo);
				if(foundVolumeInfo != null) {
					if(success) {
						//Check resource constraints, if thresholds are exceeded, fail the operation
						if(StorageProperties.shouldEnforceUsageLimits) {
							int totalVolumeSize = 0;
							VolumeInfo volInfo = new VolumeInfo();
							volInfo.setStatus(StorageProperties.Status.available.toString());
							List<VolumeInfo> volInfos = db.query(volInfo);
							for (VolumeInfo vInfo : volInfos) {
								totalVolumeSize += vInfo.getSize();
							}
							if((totalVolumeSize + size) > StorageInfo.getStorageInfo().getMaxTotalVolumeSizeInGb() ||
									(size > StorageInfo.getStorageInfo().getMaxVolumeSizeInGB())) {
								LOG.error("Max Total Volume size limit exceeded creating " + volumeId + ". Removing volume and cancelling operation");
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
					throw new EucalyptusCloudException();
				}
				db.commit();
			} catch(EucalyptusCloudException ex) {
				db.rollback();
				LOG.error(ex);
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
					String volumeId = volume.getVolumeId();
					try {
						LOG.info("Converting volume: " + volumeId + " please wait...");
						String volumePath = fromBlockManager.getVolumePath(volumeId);
						blockManager.importVolume(volumeId, volumePath, volume.getSize());
						fromBlockManager.finishVolume(volumeId);
						LOG.info("Done converting volume: " + volumeId);
					} catch (Exception ex) {
						LOG.error(ex);
						try {
							blockManager.deleteVolume(volumeId);
						} catch (EucalyptusCloudException e1) {
							LOG.error(e1);
						}
						//this one failed, continue processing the rest
					}
				}

				for(SnapshotInfo snap : snapshots) {
					String snapshotId = snap.getSnapshotId();
					try {
						LOG.info("Converting snapshot: " + snapshotId + " please wait...");
						String snapPath = fromBlockManager.getSnapshotPath(snapshotId);
						int size = fromBlockManager.getSnapshotSize(snapshotId);
						blockManager.importSnapshot(snapshotId, snap.getVolumeId(), snapPath, size);
						fromBlockManager.finishVolume(snapshotId);
						LOG.info("Done converting snapshot: " + snapshotId);
					} catch (Exception ex) {
						LOG.error(ex);
						try {
							blockManager.deleteSnapshot(snapshotId);
						} catch (EucalyptusCloudException e1) {
							LOG.error(e1);
						}
						//this one failed, continue processing the rest
					}
				}
				LOG.info("Conversion complete");
				StorageProperties.enableStorage = StorageProperties.enableSnapshots = true;
			}
		}
	}

	public CloneVolumeResponseType CloneVolume(CloneVolumeType request) throws EucalyptusCloudException {
		CloneVolumeResponseType reply = request.getReply();
		CreateStorageVolumeType createStorageVolume = new CreateStorageVolumeType();
		createStorageVolume.setParentVolumeId(request.getVolumeId());
		CreateStorageVolumeResponseType createStorageVolumeResponse = CreateStorageVolume(createStorageVolume);
		return reply;
	}

	public static class VolumeDeleterTask extends CheckerTask {

		public VolumeDeleterTask() {
			this.name = "VolumeDeleter";
		}
		
		@Override
		public void run() {
			EntityWrapper<VolumeInfo> db = StorageProperties.getEntityWrapper();
			VolumeInfo searchVolume = new VolumeInfo();
			searchVolume.setStatus(StorageProperties.Status.deleting.toString());
			List<VolumeInfo> volumes = db.query(searchVolume);
			db.commit();
			for (VolumeInfo vol : volumes) {
				String volumeId = vol.getVolumeId();
				LOG.info("Volume: " + volumeId + " was marked for deletion. Cleaning up...");
				try {
					blockManager.deleteVolume(volumeId);
				} catch (EucalyptusCloudException e) {
					LOG.error(e, e);
					continue;
				}
				db = StorageProperties.getEntityWrapper();
				VolumeInfo foundVolume;
				try {
					foundVolume = db.getUnique(new VolumeInfo(volumeId));
					db.delete(foundVolume);
					db.commit();
					EucaSemaphoreDirectory.removeSemaphore(volumeId);
				} catch (EucalyptusCloudException e) {
					db.rollback();
				}
			}
		}
	}
	
	public static class SnapshotDeleterTask extends CheckerTask {

		public SnapshotDeleterTask() {
			this.name = "SnapshotDeleter";
		}
		
		@Override
		public void run() {
			EntityWrapper<SnapshotInfo> db = StorageProperties.getEntityWrapper();
			SnapshotInfo searchSnap = new SnapshotInfo();
			searchSnap.setStatus(StorageProperties.Status.deleting.toString());
			List<SnapshotInfo> snapshots = db.query(searchSnap);
			db.commit();
			for (SnapshotInfo snap : snapshots) {
				String snapshotId = snap.getSnapshotId();
				LOG.info("Snapshot: " + snapshotId + " was marked for deletion. Cleaning up...");
				try {
					blockManager.deleteSnapshot(snapshotId);
				} catch (EucalyptusCloudException e1) {
					LOG.error(e1);
					return;
				}
				SnapshotInfo snapInfo = new SnapshotInfo(snapshotId);
				db = StorageProperties.getEntityWrapper();
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
	}

}
