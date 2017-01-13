/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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

package com.eucalyptus.blockstorage;

import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.entities.CHAPUserInfo;
import com.eucalyptus.blockstorage.entities.DirectStorageInfo;
import com.eucalyptus.blockstorage.entities.ISCSIMetaInfo;
import com.eucalyptus.blockstorage.entities.ISCSIVolumeInfo;
import com.eucalyptus.blockstorage.entities.LVMVolumeInfo;
import com.eucalyptus.blockstorage.util.BlockStorageUtilSvc;
import com.eucalyptus.blockstorage.util.BlockStorageUtilSvcImpl;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.EucalyptusCloudException;

public class ISCSIManager implements StorageExportManager {
  private static Logger LOG = Logger.getLogger(ISCSIManager.class);

  protected TGTService tgtService;
  protected BlockStorageUtilSvc blockStorageUtilSvc;

  public ISCSIManager() {
    this.tgtService = new TGTServiceUsingTGTWrapper();
    this.blockStorageUtilSvc = new BlockStorageUtilSvcImpl();
  }

  public ISCSIManager(TGTService tgtService, BlockStorageUtilSvc blockStorageUtilSvc) {
    this.tgtService = tgtService;
    this.blockStorageUtilSvc = blockStorageUtilSvc;
  }

  @Override
  public void checkPreconditions() throws EucalyptusCloudException {
    Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
    tgtService.precheckService(timeout);
  }

  @Override
  public void check() throws EucalyptusCloudException {
    Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
    TGTWrapper.checkService(timeout);
  }

  public void addUser(String username, String password) throws EucalyptusCloudException {
    Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
    tgtService.addUser(username, password, timeout);
  }

  public void deleteUser(String username) throws EucalyptusCloudException {
    Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
    tgtService.deleteUser(username, timeout);
  }

  // Modified logic for implementing EUCA-3597 and EUCA-13029
  public void exportTarget(String volumeId, int tid, String name, int lun, String path, String user) throws EucalyptusCloudException {
    LOG.debug("Exporting " + volumeId + " as target: " + tid + "," + name + "," + lun + "," + path + "," + user);
    checkAndAddUser();

    Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();

    try {
      // Does the target ID exist? 
      if (!tgtService.targetExists(volumeId, tid, null /*Don't check if our LUN exists*/, timeout)) {
        LOG.debug("Creating target " + tid + " for " + volumeId);
        tgtService.createTarget(volumeId, tid, name, timeout);
      } else {
        LOG.debug("Target " + tid + " already exists");
      }

      // Does the LUN exist with our backing store path for our target ID?
      if (!tgtService.targetExists(volumeId, tid, path, timeout)) {
        LOG.debug("Creating lun " + lun + " for " + volumeId);
        tgtService.createLun(volumeId, tid, lun, path, timeout);
      } else {
        LOG.debug("Target " + tid + " with LUN " + lun + " and backing store path " + path +
            " already exists for " + volumeId);
      }

      // Is the target bound to our user?
      if (!tgtService.targetConfigured(volumeId, tid, path, timeout, user, false /*don't check initiators list*/)) {
        LOG.debug("Binding user " + user + " for " + volumeId);
        tgtService.bindUser(volumeId, user, tid, timeout);
      } else {
        LOG.debug("Target " + tid + " already bound to user " + user + " for " + volumeId);
      }
      
      // Is the target bound to the right initiators access list (ACL)?
      if (!tgtService.targetConfigured(volumeId, tid, path, timeout, null /*don't check user*/, true /*check initiators list*/)) {
        LOG.debug("Binding target " + tid + " initiators for " + volumeId);
        tgtService.bindTarget(volumeId, tid, timeout);
      } else {
        LOG.debug("Target " + tid + " initiators already bound for " + volumeId);
      }
      
    } catch (Exception e) {
      LOG.error("Failed creating target " + tid + " for " + volumeId);
      throw new EucalyptusCloudException(e);
    }
  }

  /**
   * Execute the tgt commands to remove the iscsi target. Removes the target for *all* hosts. Verifies that the target does indeed export the resource
   * of the given path with the given lun before unexport.
   * 
   * @param tid
   * @param lun
   * @param path - the absolute path of the resource expected exported in the target.
   */
  public void unexportTarget(String volumeId, int tid, int lun, String path) throws EucalyptusCloudException {
    final int opMaxRetry = 3;
    try {
      Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();

      LOG.debug("Unexport target: tid=" + tid + ",lun=" + lun + " for " + volumeId);
      if (tgtService.targetExists(volumeId, tid, path, timeout)) {
        LOG.info("Attempting to unexport target: " + tid);
      } else {
        LOG.info("Volume: " + volumeId + " Target: " + tid + " not found, cannot unexport it.");
        return;
      }

      LOG.debug("Unbinding target " + tid + " for " + volumeId);
      tgtService.unbindTarget(volumeId, tid, timeout);

      int retryCount = 0;
      do {
        try {
          LOG.debug("Deleting lun " + lun + " on target " + tid + " for " + volumeId);
          tgtService.deleteLun(volumeId, tid, lun, timeout);
          break;
        } catch (TGTWrapper.ResourceNotFoundException e) {
          LOG.warn("Resource not found when deleting lun for " + volumeId + ". Continuing unexport", e);
          break;
        } catch (EucalyptusCloudException e) {
          LOG.warn("Volume " + volumeId + " Unable to delete lun for target: " + tid);
          Thread.sleep(1000);
          continue;
        }
      } while (retryCount++ < opMaxRetry);

      if (retryCount >= opMaxRetry) {
        LOG.error("Volume: " + volumeId + " Gave up deleting the lun for: " + tid);
      }

      retryCount = 0;
      do {
        try {
          LOG.debug("Deleting target " + tid + " for " + volumeId);
          tgtService.deleteTarget(volumeId, tid, timeout, false);
        } catch (TGTWrapper.ResourceNotFoundException e) {
          // no-op
          LOG.warn("Resource not found when deleting target for volume " + volumeId + " Continuing.", e);
        } catch (EucalyptusCloudException e) {
          LOG.warn("Volume: " + volumeId + " Unable to delete target: " + tid, e);
          Thread.sleep(1000);
          continue;
        }

        if (tgtService.targetExists(volumeId, tid, null, timeout)) {
          LOG.warn("Volume: " + volumeId + " Target: " + tid + " still exists...");
          Thread.sleep(1000);
        } else {
          break;
        }
      } while (retryCount++ < opMaxRetry);

      // Do a forcible delete of the target
      if (retryCount >= opMaxRetry && !tgtService.targetHasLun(volumeId, tid, lun, timeout)) {
        LOG.info("Forcibly deleting volume " + volumeId + " iscsi target " + tid);
        tgtService.deleteTarget(volumeId, tid, timeout, true);
        if (tgtService.targetExists(volumeId, tid, null, timeout)) {
          LOG.error("Volume: " + volumeId + " Target: " + tid + " still exists after forcible deletion");
          throw new Exception("Failed to delete iscsi target " + tid + " for volume " + volumeId);
        }
      }
    } catch (Exception t) {
      LOG.error("Unexpected error encountered during unexport process for volume " + volumeId, t);
    }
  }

  @Override
  public void configure() {
    ISCSIMetaInfo metaInfo = new ISCSIMetaInfo(StorageProperties.NAME);
    try (TransactionResource tran = Entities.transactionFor(ISCSIMetaInfo.class)) {
      List<ISCSIMetaInfo> metaInfoList = Entities.query(metaInfo);
      if (metaInfoList.size() <= 0) {
        metaInfo.setStorePrefix(StorageProperties.STORE_PREFIX);
        metaInfo.setStoreNumber(0);
        metaInfo.setStoreUser("eucalyptus");
        metaInfo.setTid(1);
        Entities.persist(metaInfo);
        tran.commit();
      }
    } catch (Exception e) {
      LOG.error(e);
    }
    checkAndAddUser();
  }

  private void checkAndAddUser() {
    try (TransactionResource outter = Entities.transactionFor(CHAPUserInfo.class)) {
      CHAPUserInfo userInfo = Entities.uniqueResult(new CHAPUserInfo("eucalyptus"));
      outter.commit();
      // check if account actually exists, if not create it.
      if (!checkUser("eucalyptus")) {
        try {
          addUser("eucalyptus", blockStorageUtilSvc.decryptSCTargetPassword(userInfo.getEncryptedPassword()));
        } catch (EucalyptusCloudException e1) {
          LOG.error(e1);
          return;
        }
      }
    } catch (NoSuchElementException ex) {
      boolean addUser = true;
      String encryptedPassword = null;
      try (TransactionResource inner = Entities.transactionFor(CHAPUserInfo.class)) {
        if (checkUser("eucalyptus")) {
          try {
            LOG.debug("No DB record found for chapuser although a eucalyptus account exists on SC. Looking for all records with chapuser eucalyptus");
            CHAPUserInfo userInfo = new CHAPUserInfo("eucalyptus");
            userInfo.setScName(null);
            CHAPUserInfo currentUserInfo = Entities.uniqueResult(userInfo);
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
          String password = Crypto.getRandom( 16 );
          password = password.substring(0, 16);
          try {
            addUser("eucalyptus", password);
            encryptedPassword = blockStorageUtilSvc.encryptSCTargetPassword(password);
          } catch (Exception e) {
            LOG.error("Failed to add chapuser to SC", e);
            return;
          }
        }

        try {
          Entities.persist(new CHAPUserInfo("eucalyptus", encryptedPassword));
        } catch (Exception e) {
          LOG.error(e);
        }
        inner.commit();
      }
    } catch (TransactionException e) {
      LOG.error(e);
    }
  }

  @Override
  public synchronized void allocateTarget(LVMVolumeInfo volumeInfo) throws EucalyptusCloudException {
    if (volumeInfo instanceof ISCSIVolumeInfo) {
      ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) volumeInfo;
      LOG.debug("Allocate target: " + iscsiVolumeInfo);
      if (iscsiVolumeInfo.getTid() > -1) {
        LOG.info("Volume already associated with a tid: " + iscsiVolumeInfo.getTid());
        return;
      }
      int tid = -1, storeNumber = -1;
      List<ISCSIMetaInfo> metaInfoList;
      try (TransactionResource tran = Entities.transactionFor(ISCSIMetaInfo.class)) {
        metaInfoList = Entities.query(new ISCSIMetaInfo(StorageProperties.NAME));

        if (metaInfoList.size() > 0) {
          ISCSIMetaInfo foundMetaInfo = metaInfoList.get(0);
          storeNumber = foundMetaInfo.getStoreNumber();
          tid = foundMetaInfo.getTid();
        }
        tran.commit();
      }

      // check if tid is in use
      int i = tid;

      Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
      do {
        if (!tgtService.targetExists(volumeInfo.getVolumeId(), i, null, timeout)) {
          tid = i;
          break;
        }
        i = (i + 1) % Integer.MAX_VALUE;
      } while (i != tid);

      LOG.debug("Volume " + iscsiVolumeInfo.getVolumeId() + " Target allocation found tid: " + tid);
      if (tid > 0) {
        try (TransactionResource tran = Entities.transactionFor(ISCSIMetaInfo.class)) {
          metaInfoList = Entities.query(new ISCSIMetaInfo(StorageProperties.NAME));
          if (metaInfoList.size() > 0) {
            ISCSIMetaInfo foundMetaInfo = metaInfoList.get(0);
            foundMetaInfo.setStoreNumber(++storeNumber);
            foundMetaInfo.setTid(tid + 1);
            iscsiVolumeInfo.setStoreName(foundMetaInfo.getStorePrefix() + StorageProperties.NAME + ":store" + storeNumber);
            iscsiVolumeInfo.setStoreUser(foundMetaInfo.getStoreUser());
            iscsiVolumeInfo.setTid(tid);
            // LUN cannot be 0 (some clients don't like that).
            iscsiVolumeInfo.setLun(1);
          }
          tran.commit();
        }
      } else {
        iscsiVolumeInfo.setTid(-1);
        LOG.fatal("Unable to allocate ISCSI target id for volume " + iscsiVolumeInfo.getVolumeId());
      }
    }
  }

  private boolean checkUser(String username) {
    try {
      Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
      return tgtService.userExists(username, timeout);
    } catch (EucalyptusCloudException e) {
      LOG.error(e);
      return false;
    }
  }

  public String getEncryptedPassword() throws EucalyptusCloudException {
    try (TransactionResource tran = Entities.transactionFor(CHAPUserInfo.class)) {
      CHAPUserInfo userInfo = Entities.uniqueResult(new CHAPUserInfo("eucalyptus"));
      String encryptedPassword = userInfo.getEncryptedPassword();
      tran.commit();
      return blockStorageUtilSvc.encryptNodeTargetPassword(blockStorageUtilSvc.decryptSCTargetPassword(encryptedPassword),
          blockStorageUtilSvc.getPartitionForLocalService(Storage.class));
    } catch (TransactionException | NoSuchElementException ex) {
      throw new EucalyptusCloudException("Unable to get CHAP password for: " + "eucalyptus");
    }
  }

  /**
   * Unexport the volume from ISCSI and clean the db state to indicate that
   */
  public void cleanup(LVMVolumeInfo volumeInfo) throws EucalyptusCloudException {
    if (volumeInfo instanceof ISCSIVolumeInfo) {
      ISCSIVolumeInfo volInfo = (ISCSIVolumeInfo) volumeInfo;
      try (TransactionResource tran = Entities.transactionFor(ISCSIVolumeInfo.class)) {
        ISCSIVolumeInfo volEntity = Entities.merge(volInfo);

        if (isExported(volEntity)) {
          unexportTarget(volEntity.getVolumeId(), volEntity.getTid(), volEntity.getLun(), volEntity.getAbsoluteLVPath());
          // Verify it really is gone
          if (!isExported(volEntity)) {
            volEntity.setTid(-1);
            volEntity.setLun(-1);
            volEntity.setStoreName(null);
            volEntity.setStoreUser(null);
          } else {
            throw new EucalyptusCloudException("Unable to remove tid: " + volEntity.getTid());
          }
        } else {
          LOG.debug("Target tid " + volEntity.getTid() + " is not exported for lun " + volEntity.getLun() + 
              " for volume " + volEntity.getVolumeId());
          // Ensure that db indicates the vol is not exported
          volEntity.setTid(-1);
          volEntity.setLun(-1);
          volEntity.setStoreName(null);
          volEntity.setStoreUser(null);
        }
        tran.commit();
      } catch (Exception e) {
        LOG.error("Something went wrong, exiting iscsi cleanup for volume " + volumeInfo.getVolumeId());
      }
    } else {
      LOG.error("Unknown volume type for volume " + volumeInfo.getVolumeId() + " - cannot cleanpup");
      throw new EucalyptusCloudException("Unknown type for volumeInfo in ISCSI cleanup. Did not find ISCSIVolumeInfo as expected");
    }
  }

  @Override
  public void stop() {
    tgtService.stop();
  }

  @Override
  public boolean isExported(LVMVolumeInfo volumeInfo) throws EucalyptusCloudException {
    if (volumeInfo instanceof ISCSIVolumeInfo) {
      if (((ISCSIVolumeInfo) volumeInfo).getTid() > -1) {
        ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) volumeInfo;
        Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
        return tgtService.targetExists(iscsiVolumeInfo.getVolumeId(), iscsiVolumeInfo.getTid(), iscsiVolumeInfo.getAbsoluteLVPath(), timeout);

        // CommandOutput output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "show", "--mode", "target", "--tid",
        // String.valueOf(iscsiVolumeInfo.getTid()) }, timeout);
        // if (StringUtils.isBlank(output.error)) {
        // return true;
        // }
      }
    }
    return false;
  }
}
