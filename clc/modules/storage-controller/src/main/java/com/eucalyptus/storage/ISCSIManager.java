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

package com.eucalyptus.storage;

import java.util.List;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.storage.TGTWrapper.CallTimeoutException;
import com.eucalyptus.storage.TGTWrapper.OperationFailedException;
import com.eucalyptus.storage.TGTWrapper.ResourceNotFoundException;
import com.eucalyptus.util.BlockStorageUtil;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.StorageProperties;
import edu.ucsb.eucalyptus.cloud.entities.CHAPUserInfo;
import edu.ucsb.eucalyptus.cloud.entities.DirectStorageInfo;
import edu.ucsb.eucalyptus.cloud.entities.ISCSIMetaInfo;
import edu.ucsb.eucalyptus.cloud.entities.ISCSIVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.LVMVolumeInfo;


public class ISCSIManager implements StorageExportManager {
	private static Logger LOG = Logger.getLogger(ISCSIManager.class);
	public ISCSIManager()  {}
	
	@Override
	public void checkPreconditions() throws EucalyptusCloudException {		
		Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
		TGTWrapper.precheckService(timeout);
	}

	@Override
	public void check() throws EucalyptusCloudException {
		Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
		TGTWrapper.checkService(timeout);
	}

	public void addUser(String username, String password) throws EucalyptusCloudException {
		Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
		TGTWrapper.addUser(username, password, timeout);
	}

	public void deleteUser(String username) throws EucalyptusCloudException {
		Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
		TGTWrapper.deleteUser(username, timeout);
	}

	// Modified logic for implementing EUCA-3597
	public void exportTarget(String volumeId, int tid, String name, int lun, String path, String user) throws EucalyptusCloudException {
		LOG.debug("Exporting " + volumeId + " as target: " + tid + "," + name + "," + lun + "," + path + "," + user);
		checkAndAddUser();

		Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
		
		if(TGTWrapper.targetExists(volumeId, tid, path, timeout)) {
			LOG.debug("Target " + tid + " already exists for "+ volumeId + " with path " + path + " no need to export again");
			return;
		}

		try {
			LOG.debug("Creating target " + tid + " for " + volumeId);
			TGTWrapper.createTarget(volumeId, tid, name, timeout);							
			
			LOG.debug("Creating lun " + lun + " for " + volumeId);
			TGTWrapper.createLun(volumeId, tid, lun, path, timeout);
			
			LOG.debug("Binding user " + user + " for " + volumeId);
			TGTWrapper.bindUser(volumeId, user, tid, timeout);
			
			LOG.debug("Binding target " + tid + " for " + volumeId);
			TGTWrapper.bindTarget(volumeId, tid, timeout);
		} catch(Exception e) {
			LOG.error("Failed creating target " + tid + " for " + volumeId);
			throw new EucalyptusCloudException(e);
		}
		
		
//		output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "new", "--mode", "target", "--tid", String.valueOf(tid), "-T", name }, timeout);
//		if (StringUtils.isNotBlank(output.error)) {
//			throw new EucalyptusCloudException(output.error);
//		}
		
		
//		output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "new", "--mode", "logicalunit", "--tid", String.valueOf(tid), "--lun", String.valueOf(lun), "-b", path }, timeout);
//		if (StringUtils.isNotBlank(output.error)) {
//			throw new EucalyptusCloudException(output.error);
//		}
		
//		output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "bind", "--mode", "account", "--tid", String.valueOf(tid), "--user", user }, timeout);
//		if (StringUtils.isNotBlank(output.error)) {
//			throw new EucalyptusCloudException(output.error);
//		}

		
//		output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "bind", "--mode", "target", "--tid", String.valueOf(tid), "-I", "ALL" }, timeout);
//		if (StringUtils.isNotBlank(output.error)) {
//			throw new EucalyptusCloudException(output.error);
//		}
	}

	/**
	 * Execute the tgt commands to remove the iscsi target. Removes the target for *all* hosts.
	 * Verifies that the target does indeed export the resource of the given path with the given
	 * lun before unexport.
	 * 
	 * @param tid
	 * @param lun
	 * @param path - the absolute path of the resource expected exported in the target.
	 */
	public void unexportTarget(String volumeId, int tid, int lun, String path) throws EucalyptusCloudException {
		final int opMaxRetry = 3;
		try
		{
			Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
			
			LOG.debug("Unexport target: tid=" + tid + ",lun=" + lun + " for " + volumeId);
			if(TGTWrapper.targetExists(volumeId, tid, path, timeout)) {
				LOG.info("Attempting to unexport target: " + tid);
			} else {
				LOG.info("Volume: " + volumeId + " Target: " + tid + " not found, cannot unexport it.");
				return;
			}

			//output = execute (new String[]{ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "unbind", "--mode", "target", "--tid", String.valueOf(tid),  "-I", "ALL"} ,timeout);
			//if (StringUtils.isNotBlank(output.error)) {
			LOG.debug("Unbinding target " + tid + " for " + volumeId);
			TGTWrapper.unbindTarget(volumeId, tid, timeout);

			int retryCount = 0;
			do {
				//output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "delete", "--mode", "logicalunit", "--tid", String.valueOf(tid), "--lun", String.valueOf(lun) }, timeout);				
				//if(StringUtils.isNotBlank(output.error)) {
				try {
					LOG.debug("Deleting lun " + lun + " on target " + tid + " for " + volumeId);
					TGTWrapper.deleteLun(volumeId, tid, lun, timeout);
					break;
				} catch(ResourceNotFoundException e) {
					LOG.warn("Resource not found when deleting lun for " + volumeId + ". Continuing unexport",e);
					break;
				} catch(EucalyptusCloudException e) {
					LOG.warn("Volume " + volumeId + " Unable to delete lun for target: " + tid);
					Thread.sleep(1000);
					continue;
				}
			} while (retryCount++ < opMaxRetry);
			
			if (retryCount>=opMaxRetry){
				LOG.error("Volume: " + volumeId + " Gave up deleting the lun for: " + tid);
			}

			retryCount = 0;
			do {
				//output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "delete", "--mode", "target","--tid", String.valueOf(tid) }, timeout);
				//if (StringUtils.isNotBlank(output.error)) {
				try {
					LOG.debug("Deleting target " + tid + " for " + volumeId);
					TGTWrapper.deleteTarget(volumeId, tid, timeout, false);
				} catch(ResourceNotFoundException e) {
					//no-op
					LOG.warn("Resource not found when deleting target for volume " + volumeId + " Continuing.",e);
				} catch(EucalyptusCloudException e) {
					LOG.warn("Volume: " + volumeId + " Unable to delete target: " + tid, e);
					Thread.sleep(1000);
					continue;
				}

				//output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "show", "--mode", "target", "--tid", String.valueOf(tid) }, timeout);
				//if (StringUtils.isBlank(output.error)) {
				if(TGTWrapper.targetExists(volumeId, tid, null, timeout)) {
					LOG.warn("Volume: " + volumeId + " Target: " + tid + " still exists...");
					Thread.sleep(1000);
				} else {
					break;
				}
			} while (retryCount++ < opMaxRetry);
			
			//Do a forcible delete of the target
			if (retryCount>=opMaxRetry && !TGTWrapper.targetHasLun(volumeId, tid, lun, timeout)){
				LOG.info("Forcibly deleting volume " + volumeId + " iscsi target " + tid);
				//output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "delete", "--mode", "target","--force", "--tid", String.valueOf(tid) }, timeout);
				//if (StringUtils.isNotBlank(output.error)) {
				TGTWrapper.deleteTarget(volumeId, tid, timeout, true);
				if(!TGTWrapper.targetExists(volumeId, tid, null, timeout)) {
					LOG.error("Volume: " + volumeId + " Target: " + tid + " still exists after forcible deletion");
					throw new Exception("Failed to delete iscsi target " + tid + " for volume " + volumeId);
				}
			}
		} catch (Exception t) {
			LOG.error("Unexpected error encountered during unexport process for volume " + volumeId,t);
		}
	}

	@Override
	public void configure() {
		EntityWrapper<ISCSIMetaInfo> db = StorageProperties.getEntityWrapper();
		ISCSIMetaInfo metaInfo = new ISCSIMetaInfo(StorageProperties.NAME);
		try {
			List<ISCSIMetaInfo> metaInfoList = db.query(metaInfo);
			if(metaInfoList.size() <= 0) {
				metaInfo.setStorePrefix(StorageProperties.STORE_PREFIX);
				metaInfo.setStoreNumber(0);
				metaInfo.setStoreUser("eucalyptus");
				metaInfo.setTid(1);
				db.add(metaInfo);
				db.commit();		
			}
		} catch(Exception e) {
			db.rollback();
			LOG.error(e);
		}
		checkAndAddUser();
	}

	private void checkAndAddUser() {
		EntityWrapper<CHAPUserInfo> dbUser = StorageProperties.getEntityWrapper();
		try {
			CHAPUserInfo userInfo = dbUser.getUnique(new CHAPUserInfo("eucalyptus"));
			//check if account actually exists, if not create it.			
			if(!checkUser("eucalyptus")) {
				try {
					addUser("eucalyptus", BlockStorageUtil.decryptSCTargetPassword(userInfo.getEncryptedPassword()));
				} catch (EucalyptusCloudException e1) {
					LOG.error(e1);					
					return;
				}
			}
		} catch(EucalyptusCloudException ex) {
			boolean addUser = true;
			String encryptedPassword = null; 

			if (checkUser("eucalyptus"))
			{
				try {
					LOG.debug("No DB record found for chapuser although a eucalyptus account exists on SC. Looking for all records with chapuser eucalyptus");
					CHAPUserInfo uesrInfo = new CHAPUserInfo("eucalyptus");
					uesrInfo.setScName(null);
					CHAPUserInfo currentUserInfo = dbUser.getUnique(uesrInfo);
					if (null != currentUserInfo && null != currentUserInfo.getEncryptedPassword()) {
						LOG.debug("Found a DB record, copying the password to the new record");
						addUser = false;
						encryptedPassword = currentUserInfo.getEncryptedPassword();
					}
				} catch (Exception e1) {
					LOG.debug("No old DB records found. The only way is to delete the chapuser and create a fresh account");
					try {
						deleteUser("eucalyptus");
					} catch (Exception e) {
						LOG.error("Failed to delete chapuser", e);
					}
				}
			} 

			if (addUser) {
				// Windows iscsi initiator requires the password length to be 12-16 bytes
				String password = Hashes.getRandom(16);
				password = password.substring(0,16);
				try {
					addUser("eucalyptus", password);
					encryptedPassword = BlockStorageUtil.encryptSCTargetPassword(password);
				} catch (Exception e) {
					LOG.error("Failed to add chapuser to SC", e);
					return;
				}
			}

			try{
				dbUser.add(new CHAPUserInfo("eucalyptus", encryptedPassword));
			} catch (Exception e) {
				dbUser.rollback();
				LOG.error(e);
			}
		} finally {
			dbUser.commit();
		}
	}

	@Override
	public synchronized void allocateTarget(LVMVolumeInfo volumeInfo) throws EucalyptusCloudException {
		if(volumeInfo instanceof ISCSIVolumeInfo) {
			ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) volumeInfo;
			LOG.debug("Allocate target: " + iscsiVolumeInfo);
			if(iscsiVolumeInfo.getTid() > -1) {
				LOG.info("Volume already associated with a tid: " + iscsiVolumeInfo.getTid());
				return;
			}
			EntityWrapper<ISCSIMetaInfo> db = StorageProperties.getEntityWrapper();
			List<ISCSIMetaInfo> metaInfoList = db.query(new ISCSIMetaInfo(StorageProperties.NAME));
			int tid = -1, storeNumber = -1;
			if(metaInfoList.size() > 0) {
				ISCSIMetaInfo foundMetaInfo = metaInfoList.get(0);
				storeNumber = foundMetaInfo.getStoreNumber();
				tid = foundMetaInfo.getTid();
			}
			db.commit();
			//check if tid is in use
			int i = tid;

			Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
			do {
				//CommandOutput output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "show", "--mode", "target", "--tid", String.valueOf(i) }, timeout);
				if(!TGTWrapper.targetExists(volumeInfo.getVolumeId(), i, null, timeout)){
				//if(StringUtils.isNotBlank(output.error)) {
					tid = i;
					break;
				}
				i = (i + 1) % Integer.MAX_VALUE;
			} while(i != tid);
			
			LOG.debug("Volume " + iscsiVolumeInfo.getVolumeId() + " Target allocation found tid: " + tid);
			if(tid > 0) {
				db = StorageProperties.getEntityWrapper();
				metaInfoList = db.query(new ISCSIMetaInfo(StorageProperties.NAME));
				if(metaInfoList.size() > 0) {
					ISCSIMetaInfo foundMetaInfo = metaInfoList.get(0);
					foundMetaInfo.setStoreNumber(++storeNumber);
					foundMetaInfo.setTid(tid + 1);
					iscsiVolumeInfo.setStoreName(foundMetaInfo.getStorePrefix() + StorageProperties.NAME + ":store" + storeNumber);
					iscsiVolumeInfo.setStoreUser(foundMetaInfo.getStoreUser());
					iscsiVolumeInfo.setTid(tid);
					//LUN cannot be 0 (some clients don't like that).
					iscsiVolumeInfo.setLun(1);
				}
				db.commit();
			} else {
				iscsiVolumeInfo.setTid(-1);
				LOG.fatal("Unable to allocate ISCSI target id for volume " + iscsiVolumeInfo.getVolumeId());
			}
		}
	}

	private boolean checkUser(String username) {
		try {
			Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
			return TGTWrapper.userExists(username, timeout);			
		} catch (EucalyptusCloudException e) {
			LOG.error(e);
			return false;
		}
	}

	public String getEncryptedPassword() throws EucalyptusCloudException {
		EntityWrapper<CHAPUserInfo> db = StorageProperties.getEntityWrapper();
		try {
			CHAPUserInfo userInfo = db.getUnique(new CHAPUserInfo("eucalyptus"));
			String encryptedPassword = userInfo.getEncryptedPassword();
			return BlockStorageUtil.encryptNodeTargetPassword(BlockStorageUtil.decryptSCTargetPassword(encryptedPassword));
		} catch(EucalyptusCloudException ex) {
			throw new EucalyptusCloudException("Unable to get CHAP password for: " + "eucalyptus");
		} finally {
			db.commit();
		}
	}

	/**
	 * Unexport the volume from ISCSI and clean the db state to indicate that
	 */
	public void cleanup(LVMVolumeInfo volumeInfo) throws EucalyptusCloudException {
		if(volumeInfo instanceof ISCSIVolumeInfo) {			
			ISCSIVolumeInfo volInfo = (ISCSIVolumeInfo)volumeInfo;
			EntityWrapper<ISCSIVolumeInfo> db = StorageProperties.getEntityWrapper();
			try {
				ISCSIVolumeInfo volEntity = db.merge(volInfo);
				
				if(isExported(volEntity)) {
					unexportTarget(volEntity.getVolumeId(), volEntity.getTid(), volEntity.getLun(), volEntity.getAbsoluteLVPath());
					//Verify it really is gone
					if (!isExported(volEntity)) {
						volEntity.setTid(-1);
						volEntity.setLun(-1);
						volEntity.setStoreName(null);
					} else {
						throw new EucalyptusCloudException("Unable to remove tid: " + volEntity.getTid());
					}
				} else {
					//Ensure that db indicates the vol is not exported
					volEntity.setTid(-1);
					volEntity.setLun(-1);
					volEntity.setStoreName(null);
					volEntity.setStoreUser(null);
				}
			} catch(Exception e){ 
				LOG.error("Something went wrong, exiting iscsi cleanup for volume " + volumeInfo.getVolumeId());
			} finally {
				try {
					db.commit();
				} catch(Exception e) {
					LOG.error("Commit failed. Rolling back");
					db.rollback();
				}			
			}
		} else {
			LOG.error("Unknown volume type for volume " + volumeInfo.getVolumeId() + " - cannot cleanpup");
			throw new EucalyptusCloudException("Unknown type for volumeInfo in ISCSI cleanup. Did not find ISCSIVolumeInfo as expected");
		}
	}

	@Override
	public void stop() {
		TGTWrapper.stop();
	}

	@Override
	public boolean isExported(LVMVolumeInfo volumeInfo) throws EucalyptusCloudException {
		if(volumeInfo instanceof ISCSIVolumeInfo) {			
			if(((ISCSIVolumeInfo) volumeInfo).getTid() > -1) {
				ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) volumeInfo;
				Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();				
				return TGTWrapper.targetExists(iscsiVolumeInfo.getVolumeId(), iscsiVolumeInfo.getTid(), iscsiVolumeInfo.getAbsoluteLVPath(), timeout);
								
				//CommandOutput output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "show", "--mode", "target", "--tid", String.valueOf(iscsiVolumeInfo.getTid()) }, timeout);
				//if (StringUtils.isBlank(output.error)) {
				//return true;
				//}
			}
		}
		return false;
	}
}
