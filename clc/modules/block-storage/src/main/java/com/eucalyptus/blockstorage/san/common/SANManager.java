/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/
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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.LogicalStorageManager;
import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.StorageManagers;
import com.eucalyptus.blockstorage.StorageResource;
import com.eucalyptus.blockstorage.StorageResourceWithCallback;
import com.eucalyptus.blockstorage.config.StorageControllerConfiguration;
import com.eucalyptus.blockstorage.entities.StorageInfo;
import com.eucalyptus.blockstorage.exceptions.ConnectionInfoNotFoundException;
import com.eucalyptus.blockstorage.exceptions.NoSuchRecordException;
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
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.storage.common.CheckerTask;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Strings;

import edu.ucsb.eucalyptus.cloud.VolumeAlreadyExistsException;
import edu.ucsb.eucalyptus.msgs.ComponentProperty;

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

  public void addSnapshot(String snapshotId) throws EucalyptusCloudException {
    finishVolume(snapshotId);
  }

  public void checkPreconditions() throws EucalyptusCloudException {
    if (!new File(BaseDirectory.LIB.toString() + File.separator + "connect_iscsitarget_sc.pl").exists()) {
      throw new EucalyptusCloudException("connect_iscsitarget_sc.pl not found");
    }
    if (!new File(BaseDirectory.LIB.toString() + File.separator + "disconnect_iscsitarget_sc.pl").exists()) {
      throw new EucalyptusCloudException("disconnect_iscsitarget_sc.pl not found");
    }

    if (connectionManager != null) {
      connectionManager.checkPreconditions();
    } else {
      LOG.warn("Invalid/uninitialized reference to blockstorage backend");
      throw new EucalyptusCloudException("Invalid/uninitialized reference to blockstorage backend");
    }

  }

  @Override
  public void cleanSnapshot(String snapshotId, String snapshotPointId) {
    SANVolumeInfo sanSnapshot = null;
    String sanSnapshotId = null;
    String iqn = null;

    try {
      sanSnapshot = lookup(snapshotId);
      sanSnapshotId = sanSnapshot.getSanVolumeId();
      iqn = sanSnapshot.getIqn();
    } catch (Exception e) {
      LOG.debug("Skipping clean up for " + snapshotId);
      return;
    }

    LOG.info("Deleting " + sanSnapshotId + " on backend");
    if (connectionManager.deleteSnapshot(sanSnapshotId, iqn, snapshotPointId)) {
      try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
        SANVolumeInfo snapInfo = Entities.uniqueResult(new SANVolumeInfo(snapshotId).withSanVolumeId(sanSnapshotId));
        Entities.delete(snapInfo);
        tran.commit();
      } catch (TransactionException | NoSuchElementException ex) {
        LOG.warn("Unable to clean failed backend resource " + snapshotId, ex);
        return;
      }
    }
  }

  public void cleanVolume(String volumeId) {
    SANVolumeInfo sanVolume = null;
    String sanVolumeId = null;
    try {
      sanVolume = lookup(volumeId);
      sanVolumeId = sanVolume.getSanVolumeId();
    } catch (Exception e) {
      LOG.debug("Skipping clean up for " + volumeId);
      return;
    }

    LOG.info("Deleting " + sanVolumeId + " on backend");
    if (connectionManager.deleteVolume(sanVolumeId, sanVolume.getIqn())) {
      try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
        SANVolumeInfo volumeInfo = Entities.uniqueResult(new SANVolumeInfo(volumeId).withSanVolumeId(sanVolumeId));
        Entities.delete(volumeInfo);
        tran.commit();
      } catch (NoSuchElementException | TransactionException ex) {
        LOG.warn("Unable to clean failed backend resource " + volumeId);
        return;
      }
    }
  }

  public void configure() throws EucalyptusCloudException {
    // configure provider
    if (connectionManager != null) {
      try {
        LOG.info("Checking backend connection information for cluster: " + StorageInfo.getStorageInfo().getName());
        connectionManager.checkConnectionInfo();
      } catch (ConnectionInfoNotFoundException e) {
        LOG.warn("Cannot configure SC blockstorage backend due to missing properties. " + e.getMessage());
        throw new EucalyptusCloudException("Cannot configure SC blockstorage backend due to missing properties. " + e.getMessage());
      }

      LOG.info("Configuring block storage backend for cluster: " + StorageInfo.getStorageInfo().getName());
      connectionManager.configure();
    } else {
      LOG.warn("Invalid/uninitialized reference to blockstorage backend");
      throw new EucalyptusCloudException("Invalid/uninitialized reference to blockstorage backend");
    }
  }

  public StorageResource createSnapshot(String volumeId, String snapshotId, String snapshotPointId, Boolean shouldTransferSnapshots)
      throws EucalyptusCloudException {
    String sanSnapshotId = resourceIdOnSan(snapshotId);
    SANVolumeInfo snapInfo = new SANVolumeInfo(snapshotId);
    StorageResource storageResource = null;

    // Look up source volume in the database and get the backend volume ID
    SANVolumeInfo volumeInfo = lookup(volumeId);
    String sanVolumeId = volumeInfo.getSanVolumeId();
    int size = volumeInfo.getSize();

    // Check to make sure that snapshot does not already exist on the backend
    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      SANVolumeInfo existingSnap = Entities.uniqueResult(snapInfo);
      LOG.info("Checking for " + existingSnap.getSanVolumeId() + " on backend");
      if (connectionManager.snapshotExists(existingSnap.getSanVolumeId(), existingSnap.getIqn())) {
        throw new VolumeAlreadyExistsException("Existing resource found on backend for " + existingSnap.getSanVolumeId());
      } else {
        LOG.debug("Found database record but resource does not exist on backend. Deleting database record for " + snapshotId);
        Entities.delete(existingSnap);
        tran.commit();
      }
    } catch (NoSuchElementException ex) {
      // intentional no-op
    } catch (VolumeAlreadyExistsException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new EucalyptusCloudException(ex);
    }

    try {
      Transactions.save(snapInfo.withSanVolumeId(sanSnapshotId).withSize(size).withSnapshotOf(volumeId));
    } catch (Exception ex) {
      LOG.warn("Failed to persist database record for " + snapshotId, ex);
      throw new EucalyptusCloudException("Failed to persist database record for " + snapshotId, ex);
    }

    LOG.info("Creating " + sanSnapshotId + " from " + sanVolumeId + " using snapshot point " + snapshotPointId + " on backend");
    String iqn = connectionManager.createSnapshot(sanVolumeId, sanSnapshotId, snapshotPointId);

    if (iqn != null) {

      if (shouldTransferSnapshots) {
        String scIqn = StorageProperties.getStorageIqn();
        if (scIqn == null) {
          LOG.warn("Storage Controller IQN not found");
          throw new EucalyptusCloudException("Storage Controller IQN not found");
        }

        // Ensure that the SC can attach to the volume.
        String lun = null;
        try {
          LOG.info("Exporting " + sanSnapshotId + " on backend to Storage Controller host IQN " + scIqn);
          lun = connectionManager.exportResource(sanSnapshotId, scIqn, iqn);
        } catch (EucalyptusCloudException attEx) {
          LOG.warn("Failed to export " + sanSnapshotId + " on backend to Storage Controller", attEx);
          throw new EucalyptusCloudException(
              "Failed to create " + snapshotId + " due to error exporting " + sanSnapshotId + " on backend to Storage Controller", attEx);
        }

        if (lun == null) {
          LOG.warn("Invalid value found for LUN upon exporting " + sanSnapshotId + " on backend");
          throw new EucalyptusCloudException(
              "Failed to create " + snapshotId + " due to invalid value for LUN upon exporting " + sanSnapshotId + " on backend");
        }

        // Moved this to before the connection is attempted since the volume does exist, it may need to be cleaned
        try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
          SANVolumeInfo existingSnap = Entities.uniqueResult(snapInfo);
          existingSnap.setIqn(iqn + ',' + lun); // Store the lun ID in the iqn string, its needed for disconnecting the snapshot from SC later
          Entities.merge(existingSnap);
          tran.commit();
        } catch (Exception e) {
          LOG.warn("Failed to update database record with IQN and LUN post creation for " + snapshotId);
          throw new EucalyptusCloudException("Failed to update database entity with IQN and LUN post creation for " + snapshotId, e);
        }

        // Run the connect
        try {
          LOG.info("Connecting " + sanSnapshotId + " on backend to Storage Controller for transfer");
          storageResource = connectionManager.connectTarget(iqn, lun);
          storageResource.setId(snapshotId);
        } catch (Exception connEx) {
          LOG.warn("Failed to connect " + sanSnapshotId + " on backend to Storage Controller. Detaching and cleaning up", connEx);
          try {
            LOG.info("Unexporting " + sanSnapshotId + " on backend from Storage Controller host IQN " + scIqn);
            connectionManager.unexportResource(sanSnapshotId, scIqn);
          } catch (EucalyptusCloudException detEx) {
            LOG.debug("Could not unexport " + sanSnapshotId + " during cleanup of failed connection");
          }
          throw new EucalyptusCloudException(
              "Failed to create " + snapshotId + " due to an error connecting " + sanSnapshotId + " on backend to Storage Controller", connEx);
        }
      } else {
        // Just save the iqn of the snapshot, nothing more to do since upload is not necessary
        try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
          SANVolumeInfo existingSnap = Entities.uniqueResult(snapInfo);
          existingSnap.setIqn(iqn);
          Entities.merge(existingSnap);
          tran.commit();
        } catch (Exception ex) {
          LOG.warn("Failed to update database record with IQN post creation for " + snapshotId);
          throw new EucalyptusCloudException("Failed to update database record with IQN post creation for " + snapshotId, ex);
        }
      }
    } else {
      LOG.warn("Invalid IQN from backend for " + sanSnapshotId);
      throw new EucalyptusCloudException("Failed to create " + snapshotId + " due to invalid IQN from backend for " + sanSnapshotId);
    }

    return storageResource;
  }

  public void createSnapshot(String volumeId, String snapshotId, String snapshotPointId) throws EucalyptusCloudException {
    String sanSnapshotId = resourceIdOnSan(snapshotId);
    SANVolumeInfo snapInfo = new SANVolumeInfo(snapshotId);

    // Look up source volume in the database and get the backend volume ID
    SANVolumeInfo volumeInfo = lookup(volumeId);
    String sanVolumeId = volumeInfo.getSanVolumeId();
    int size = volumeInfo.getSize();

    // Check to make sure that snapshot does not already exist on the backend
    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      SANVolumeInfo existingSnap = Entities.uniqueResult(snapInfo);
      LOG.info("Checking for " + existingSnap.getSanVolumeId() + " on backend");
      if (connectionManager.snapshotExists(existingSnap.getSanVolumeId(), existingSnap.getIqn())) {
        throw new VolumeAlreadyExistsException("Existing resource found on backend for " + existingSnap.getSanVolumeId());
      } else {
        LOG.debug("Found database record but resource does not exist on backend. Deleting database record for " + snapshotId);
        Entities.delete(existingSnap);
        tran.commit();
      }
    } catch (NoSuchElementException ex) {
      // intentional no-op
    } catch (VolumeAlreadyExistsException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new EucalyptusCloudException(ex);
    }

    try {
      Transactions.save(snapInfo.withSanVolumeId(sanSnapshotId).withSize(size).withSnapshotOf(volumeId));
    } catch (Exception ex) {
      LOG.warn("Failed to persist database record for " + snapshotId, ex);
      throw new EucalyptusCloudException("Failed to persist database record for " + snapshotId, ex);
    }

    LOG.info("Creating " + sanSnapshotId + " from " + sanVolumeId + " using snapshot point " + snapshotPointId + " on backend");
    String iqn = connectionManager.createSnapshot(sanVolumeId, sanSnapshotId, snapshotPointId);

    if (iqn != null) {
      // Just save the iqn of the snapshot, nothing more to do since upload is not necessary
      try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
        SANVolumeInfo existingSnap = Entities.uniqueResult(snapInfo);
        existingSnap.setIqn(iqn);
        Entities.merge(existingSnap);
        tran.commit();
      } catch (Exception ex) {
        LOG.warn("Failed to update database record with IQN post creation for " + snapshotId);
        throw new EucalyptusCloudException("Failed to update database record with IQN post creation for " + snapshotId, ex);
      }
    } else {
      LOG.warn("Invalid IQN from backend for " + sanSnapshotId);
      throw new EucalyptusCloudException("Failed to create " + snapshotId + " due to invalid IQN from backend for " + sanSnapshotId);
    }
  }

  public void createVolume(String volumeId, int size) throws EucalyptusCloudException {
    String sanVolumeId = resourceIdOnSan(volumeId);
    SANVolumeInfo volumeInfo = new SANVolumeInfo(volumeId);

    // Check to make sure that volume does not already exist on the backend
    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      SANVolumeInfo existingVol = Entities.uniqueResult(volumeInfo);
      LOG.info("Checking for " + existingVol.getSanVolumeId() + " on backend");
      if (connectionManager.volumeExists(existingVol.getSanVolumeId(), existingVol.getIqn())) {
        throw new VolumeAlreadyExistsException("Existing resource found on backend for " + existingVol.getSanVolumeId());
      } else {
        LOG.debug("Found database record but resource does not exist on backend. Deleting database record for " + volumeId);
        Entities.delete(existingVol);
        tran.commit();
      }
    } catch (NoSuchElementException ex) {
      // intentional no-op
    } catch (VolumeAlreadyExistsException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new EucalyptusCloudException(ex);
    }

    try {
      Transactions.save(volumeInfo.withSanVolumeId(sanVolumeId).withSize(size));
    } catch (Exception ex) {
      LOG.warn("Failed to persist database record for " + volumeId, ex);
      throw new EucalyptusCloudException("Failed to persist database record for " + volumeId, ex);
    }

    LOG.info("Creating " + sanVolumeId + " on backend");
    String iqn = connectionManager.createVolume(sanVolumeId, size);
    if (iqn != null) {
      try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
        SANVolumeInfo existingVol = Entities.uniqueResult(volumeInfo);
        existingVol.setIqn(iqn);
        tran.commit();
      } catch (Exception ex) {
        LOG.warn("Failed to update database record with IQN post creation for " + volumeId);
        throw new EucalyptusCloudException("Failed to update database record with IQN post creation for " + volumeId, ex);
      }
    } else {
      LOG.warn("Invalid IQN from backend for " + sanVolumeId);
      throw new EucalyptusCloudException("Failed to create " + volumeId + " due to invalid IQN from backend for " + sanVolumeId);
    }
  }

  private String resourceIdOnSan(String resourceId) {
    try {
      SANInfo sanInfo = Transactions.one(new SANInfo(), Functions.<SANInfo>identity());
      return (StringUtils.trimToEmpty(sanInfo.getResourcePrefix()) + resourceId + StringUtils.trimToEmpty(sanInfo.getResourceSuffix()));
    } catch (TransactionException ex) {
      LOG.warn("Unable to retrieve resource prefix/suffix from databse", ex);
      return resourceId;
    }
  }

  public int createVolume(String volumeId, String snapshotId, int size) throws EucalyptusCloudException {
    String sanVolumeId = resourceIdOnSan(volumeId);
    SANVolumeInfo volumeInfo = new SANVolumeInfo(volumeId);

    // Look up source snapshot in the database and get the backend snapshot ID
    SANVolumeInfo snapInfo = lookup(snapshotId);
    String sanSnapshotId = snapInfo.getSanVolumeId();
    int snapSize = snapInfo.getSize();
    if (size <= 0) {
      size = snapSize;
    }

    // Check to make sure that volume does not already exist on the backend
    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      SANVolumeInfo existingVol = Entities.uniqueResult(volumeInfo);
      LOG.info("Checking for " + existingVol.getSanVolumeId() + " on backend");
      if (connectionManager.volumeExists(existingVol.getSanVolumeId(), existingVol.getIqn())) {
        throw new VolumeAlreadyExistsException("Existing resource found on backend for " + existingVol.getSanVolumeId());
      } else {
        LOG.debug("Found database record but resource does not exist on backend. Deleting database record for " + volumeId);
        Entities.delete(existingVol);
        tran.commit();
      }
    } catch (NoSuchElementException ex) {
      // intentional no-op
    } catch (VolumeAlreadyExistsException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new EucalyptusCloudException(ex);
    }

    try {
      Transactions.save(volumeInfo.withSanVolumeId(sanVolumeId).withSize(size));
    } catch (Exception ex) {
      LOG.warn("Failed to persist database record for " + volumeId, ex);
      throw new EucalyptusCloudException("Failed to persist database record for " + volumeId, ex);
    }

    LOG.info("Creating " + sanVolumeId + " from " + sanSnapshotId + " on backend");
    String iqn = connectionManager.createVolume(sanVolumeId, sanSnapshotId, snapSize, size, snapInfo.getIqn());
    if (iqn != null) {
      try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
        SANVolumeInfo existingVol = Entities.uniqueResult(volumeInfo);
        existingVol.setIqn(iqn);
        Entities.merge(existingVol);
        tran.commit();
      } catch (Exception ex) {
        LOG.warn("Failed to update database record with IQN post creation for " + volumeId);
        throw new EucalyptusCloudException("Failed to update database record with IQN post creation for " + volumeId, ex);
      }
    } else {
      LOG.warn("Invalid IQN from backend for " + sanVolumeId);
      throw new EucalyptusCloudException("Failed to create " + volumeId + " due to invalid IQN from backend for " + sanVolumeId);
    }

    return size;
  }

  @Override
  public int resizeVolume(final String volumeId, final int size) throws EucalyptusCloudException {
    final SANVolumeInfo volumeInfo = lookup(volumeId);
    final String sanVolumeId = volumeInfo.getSanVolumeId();

    LOG.info("Resizing " + sanVolumeId + " to size " + size);
    final int resultSize;
    try {
      resultSize = connectionManager.resizeVolume(sanVolumeId, volumeInfo.getIqn(), size);
    } catch (Exception ex) {
      LOG.warn("Failed to resize volume " + volumeId, ex);
      throw new EucalyptusCloudException("Failed resize for " + volumeId, ex);
    }

    if (resultSize != volumeInfo.getSize()) {
      try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
        final SANVolumeInfo existingVol = Entities.uniqueResult(volumeInfo);
        existingVol.setSize(size);
        tran.commit();
      } catch (Exception ex) {
        LOG.warn("Failed to update database record with size for " + volumeId);
        throw new EucalyptusCloudException(
            "Failed to update database record with size for " + volumeId, ex);
      }
    }

    return resultSize;
  }

  public void cloneVolume(String volumeId, String parentVolumeId) throws EucalyptusCloudException {
    String sanVolumeId = resourceIdOnSan(volumeId);
    SANVolumeInfo volInfo = new SANVolumeInfo(volumeId);

    // Look up source volume in the database and get the backend volume ID
    SANVolumeInfo parentVolumeInfo = lookup(parentVolumeId);
    String sanParentVolumeId = parentVolumeInfo.getSanVolumeId();
    int size = parentVolumeInfo.getSize();

    // Check to make sure that cloned volume does not already exist on the backend
    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      SANVolumeInfo existingVol = Entities.uniqueResult(volInfo);
      LOG.info("Checking for " + existingVol.getSanVolumeId() + " on backend");
      if (connectionManager.snapshotExists(existingVol.getSanVolumeId(), existingVol.getIqn())) {
        throw new VolumeAlreadyExistsException("Existing resource found on backend for " + existingVol.getSanVolumeId());
      } else {
        LOG.debug("Found database record but resource does not exist on backend. Deleting database record for " + volumeId);
        Entities.delete(existingVol);
        tran.commit();
      }
    } catch (NoSuchElementException ex) {
      // intentional no-op
    } catch (VolumeAlreadyExistsException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new EucalyptusCloudException(ex);
    }

    try {
      Transactions.save(volInfo.withSanVolumeId(sanVolumeId).withSize(size));
    } catch (Exception ex) {
      LOG.warn("Failed to persist database record for " + volumeId, ex);
      throw new EucalyptusCloudException("Failed to persist database record for " + volumeId, ex);
    }

    LOG.info("Cloning " + sanVolumeId + " from " + sanParentVolumeId + " on backend");
    String iqn = connectionManager.cloneVolume(sanVolumeId, sanParentVolumeId, parentVolumeInfo.getIqn());
    if (iqn != null) {
      try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
        SANVolumeInfo existingVol = Entities.uniqueResult(volInfo);
        existingVol.setIqn(iqn);
        Entities.merge(existingVol);
        tran.commit();
      } catch (Exception ex) {
        LOG.warn("Failed to update database record with IQN post creation for " + volumeId);
        throw new EucalyptusCloudException("Failed to update database record with IQN post creation for " + volumeId, ex);
      }
    } else {
      LOG.warn("Invalid IQN from backend for " + sanVolumeId);
      throw new EucalyptusCloudException("Failed to create " + volumeId + " due to invalid IQN from backend for " + sanVolumeId);
    }
  }

  @Override
  public void deleteSnapshot(String snapshotId, String snapshotPointId) throws EucalyptusCloudException {
    SANVolumeInfo sanSnapshot = null;
    String sanSnapshotId = null;
    String iqn = null;

    // Look up source snapshot in the database and get the backend snapshot ID
    try {
      sanSnapshot = lookup(snapshotId);
      sanSnapshotId = sanSnapshot.getSanVolumeId();
      iqn = sanSnapshot.getIqn();
    } catch (Exception ex) {
      LOG.debug("Skipping deletion for " + snapshotId);
      return;
    }

    boolean deleteEntity = false;

    // Try deleting the snapshot. It might fail as snapshots are global and another SC may have already deleted it
    LOG.info("Deleting " + sanSnapshotId + " on backend");
    if (connectionManager.deleteSnapshot(sanSnapshotId, iqn, snapshotPointId)) {
      deleteEntity = true;
    } else {
      // If snapshot deletion failed, check to see if the snapshot even exists
      LOG.debug("Unable to delete " + sanSnapshotId + " on backend. Checking to see if the resource exists");
      if (!connectionManager.snapshotExists(sanSnapshotId, iqn)) {
        LOG.debug("Resource not found on backend for " + sanSnapshotId + ". Safe to delete database record for " + snapshotId);
        deleteEntity = true;
      } else {
        LOG.warn("Failed to delete backend resource " + sanSnapshotId);
        throw new EucalyptusCloudException("Failed to delete backend resource " + sanSnapshotId);
      }
    }

    if (deleteEntity) {
      try {
        Transactions.delete(new SANVolumeInfo(snapshotId).withSanVolumeId(sanSnapshotId));
      } catch (Exception e) {
        LOG.warn("Failed to remove database record post deletion for " + snapshotId, e);
        throw new EucalyptusCloudException("Failed to remove database record post deletion for " + snapshotId, e);
      }
    }
  }

  public void deleteVolume(String volumeId) throws EucalyptusCloudException {
    SANVolumeInfo sanVolume = null;
    String sanVolumeId = null;

    // Look up source volume in the database and get the backend snapshot ID
    try {
      sanVolume = lookup(volumeId);
      sanVolumeId = sanVolume.getSanVolumeId();
    } catch (Exception ex) {
      LOG.debug("Skipping deletion for " + volumeId);
      return;
    }

    LOG.info("Deleting " + sanVolumeId + " on backend");
    if (connectionManager.deleteVolume(sanVolumeId, sanVolume.getIqn())) {
      try {
        Transactions.delete(new SANVolumeInfo(volumeId).withSanVolumeId(sanVolumeId));
      } catch (Exception e) {
        LOG.warn("Failed to remove database record post deletion for " + volumeId, e);
        throw new EucalyptusCloudException("Failed to remove database record post deletion for " + volumeId, e);
      }
    } else {
      LOG.warn("Failed to delete backend resource " + sanVolumeId);
      throw new EucalyptusCloudException("Failed to delete backend resource " + sanVolumeId);
    }
  }

  public int getSnapshotSize(String snapshotId) throws EucalyptusCloudException {
    try {
      SANVolumeInfo snapInfo = Transactions.find(new SANVolumeInfo(snapshotId));
      return snapInfo.getSize();
    } catch (Exception ex) {
      LOG.warn("Encountered error during lookup for " + snapshotId, ex);
      throw new EucalyptusCloudException("Encountered error during lookup for " + snapshotId, ex);
    }
  }

  public String getVolumeConnectionString(String volumeId) throws EucalyptusCloudException {
    return connectionManager.getVolumeConnectionString(volumeId);
  }

  public void initialize() throws EucalyptusCloudException {
    LOG.info("Initializing SANInfo entity");
    SANInfo.getStorageInfo();
    connectionManager.initialize();
  }

  public void loadSnapshots(List<String> snapshotSet, List<String> snapshotFileNames) throws EucalyptusCloudException {
    // Nothing to do here
  }

  public List<String> prepareForTransfer(String snapshotId) throws EucalyptusCloudException {
    // Nothing to do here
    return new ArrayList<String>();
  }

  public void reload() {}

  public void startupChecks() throws EucalyptusCloudException {
    connectionManager.checkConnection();
  }

  public void finishVolume(String snapshotId) throws EucalyptusCloudException {
    SANVolumeInfo snapInfo = null;
    String iqnAndLun = null;
    String sanVolumeId = null;

    try {
      snapInfo = lookup(snapshotId);
      iqnAndLun = snapInfo.getIqn();
      sanVolumeId = snapInfo.getSanVolumeId();
    } catch (NoSuchRecordException e) {
      LOG.debug("Skipping finish up for " + snapshotId);
      return;
    }

    if (iqnAndLun != null && iqnAndLun.contains(",")) { // disconnect and unexport the snapshot from SC
      String[] parts = iqnAndLun.split(",");
      if (parts.length == 2) {
        // disconnect SC-snapshot iscsi connection
        try {
          LOG.info("Disconnecting " + sanVolumeId + " on backend from Storage Controller");
          connectionManager.disconnectTarget(sanVolumeId, parts[0], parts[1]);
        } catch (Exception e) {
          LOG.warn("Failed to disconnect iscsi session between " + sanVolumeId + " and SC", e);
        }
      } else {
        LOG.warn("Unable to disconnect " + sanVolumeId
            + " from Storage Controller due to invalid iqn format. Expected iqn to contain one ',' but got: " + iqnAndLun);
        throw new EucalyptusCloudException("Unable to disconnect " + sanVolumeId + " from Storage Controller due to invalid iqn format");
      }

      // Unexport volume from SC
      String scIqn = StorageProperties.getStorageIqn();
      try {
        LOG.info("Unexporting " + sanVolumeId + " on backend from Storage Controller host IQN " + scIqn);
        connectionManager.unexportResource(sanVolumeId, scIqn);
      } catch (Exception e) {
        LOG.warn("Could not unexport " + sanVolumeId + " on backend from Storage Controller");
      }

      // Remove lun and update snapshot IQN
      try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
        SANVolumeInfo existingSnap = Entities.uniqueResult(snapInfo);
        existingSnap.setIqn(parts[0]); // Store the lun ID in the iqn string, its needed for disconnecting the snapshot from SC later
        Entities.merge(existingSnap);
        tran.commit();
      } catch (Exception e) {
        LOG.warn("Failed to update IQN after disconnecting " + snapshotId + " from Storage Controller", e);
        // Warn and move on, no need to throw exception
      }
    } else {
      // snapshot was never exported to SC, no need to disconnect it and un-export it
      // this could happen if snapshot upload was not necessary
      LOG.debug("Skipping disconnect and unexport operations for " + sanVolumeId);
    }

    // wait for snapshot operation to complete
    try {
      LOG.info("Waiting for " + sanVolumeId + " to complete on backend");
      connectionManager.waitAndComplete(sanVolumeId, iqnAndLun);
    } catch (EucalyptusCloudException e) {
      LOG.warn("Failed during wait for " + sanVolumeId + " to complete on backend", e);
      throw e;
    } catch (Exception e) {
      LOG.warn("Failed during wait for " + sanVolumeId + " to complete on backend", e);
      throw new EucalyptusCloudException("Failed during wait for " + sanVolumeId + " to complete on backend", e);
    }
  }

  public StorageResourceWithCallback prepSnapshotForDownload(final String snapshotId, int sizeExpected, long actualSizeInMB)
      throws EucalyptusCloudException {

    try {
      // If any record for the snapshot exists, just copy that info
      lookup(snapshotId);
      LOG.debug("Found existing database record for " + snapshotId + ".  Will use that lun and record.");
      return null;
    } catch (NoSuchRecordException e) {
      // going forward with the assumption that snapshot record is not found for this SC
      LOG.debug(
          "Backend database record for " + snapshotId + " not found. Setting up holder on backend to hold the snapshot content downloaded from OSG");
    }

    String sanSnapshotId = resourceIdOnSan(snapshotId);
    String iqn = null;
    try {
      // TODO Create a database record first before firing off the volume creation
      LOG.info("Creating " + sanSnapshotId + " of size " + actualSizeInMB + " MB on backend");
      iqn = connectionManager.createSnapshotHolder(sanSnapshotId, actualSizeInMB);
    } catch (EucalyptusCloudException e) {
      LOG.warn("Failed to create backend resource for " + snapshotId);
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
          LOG.info("Exporting " + sanSnapshotId + " on backend to Storage Controller host IQN " + scIqn);
          lun = connectionManager.exportResource(sanSnapshotId, scIqn, iqn);
        } catch (EucalyptusCloudException attEx) {
          LOG.warn("Failed to export " + sanSnapshotId + " on backend to Storage Controller", attEx);
          throw new EucalyptusCloudException(
              "Failed to create " + snapshotId + " due to error exporting " + sanSnapshotId + " on backend to Storage Controller", attEx);
        }

        if (lun == null) {
          LOG.warn("Invalid value found for LUN upon exporting " + sanSnapshotId + " on backend");
          throw new EucalyptusCloudException(
              "Failed to create " + snapshotId + " due to invalid value for LUN upon exporting " + sanSnapshotId + " on backend");
        }

        // Store the lun ID in the iqn string, its needed for disconnecting the snapshot from SC later
        SANVolumeInfo snapInfo = new SANVolumeInfo(snapshotId, iqn + ',' + lun, sizeExpected).withSanVolumeId(sanSnapshotId);
        try {
          Transactions.save(snapInfo);
        } catch (TransactionException e) {
          LOG.warn("Failed to update database record with IQN and LUN post creation for " + snapshotId);
          throw new EucalyptusCloudException("Failed to update database entity with IQN and LUN post creation for " + snapshotId, e);
        }

        // Run the connect
        StorageResource storageResource = null;
        try {
          LOG.info("Connecting " + sanSnapshotId + " on backend to Storage Controller for transfer");
          storageResource = connectionManager.connectTarget(iqn, lun);
          storageResource.setId(snapshotId);
        } catch (Exception connEx) {
          LOG.warn("Failed to connect " + sanSnapshotId + " on backend to Storage Controller. Detaching and cleaning up", connEx);
          try {
            LOG.info("Unexporting " + sanSnapshotId + " on backend from Storage Controller host IQN " + scIqn);
            connectionManager.unexportResource(sanSnapshotId, scIqn);
          } catch (EucalyptusCloudException detEx) {
            LOG.debug("Could not unexport " + sanSnapshotId + " during cleanup of failed connection");
          }
          throw new EucalyptusCloudException(
              "Failed to create " + snapshotId + " due to an error connecting " + sanSnapshotId + " on backend to Storage Controller", connEx);
        }

        return new StorageResourceWithCallback(storageResource, new Function<StorageResource, String>() {

          @Override
          public String apply(StorageResource arg0) {
            try {
              LOG.debug("Executing callback for prepareSnapshotForDownload() with " + snapshotId);

              SANVolumeInfo snapInfo = null;
              String iqnAndLun = null;
              String sanVolumeId = null;

              try {
                snapInfo = lookup(snapshotId);
                iqnAndLun = snapInfo.getIqn();
                sanVolumeId = snapInfo.getSanVolumeId();
              } catch (NoSuchRecordException e) {
                LOG.debug("Skipping finish up for " + snapshotId);
                return null;
              }

              if (iqnAndLun != null && iqnAndLun.contains(",")) { // disconnect and unexport the snapshot from SC
                String[] parts = iqnAndLun.split(",");
                if (parts.length == 2) {
                  // disconnect SC-snapshot iscsi connection
                  try {
                    LOG.info("Disconnecting " + sanVolumeId + " on backend from Storage Controller");
                    connectionManager.disconnectTarget(sanVolumeId, parts[0], parts[1]);
                  } catch (Exception e) {
                    LOG.warn("Failed to disconnect iscsi session between " + sanVolumeId + " and SC", e);
                  }
                } else {
                  LOG.warn("Unable to disconnect " + sanVolumeId
                      + " from Storage Controller due to invalid iqn format. Expected iqn to contain one ',' but got: " + iqnAndLun);
                  throw new EucalyptusCloudException("Unable to disconnect " + sanVolumeId + " from Storage Controller due to invalid iqn format");
                }

                // Unexport volume from SC
                String scIqn = StorageProperties.getStorageIqn();
                try {
                  LOG.info("Unexporting " + sanVolumeId + " on backend from Storage Controller host IQN " + scIqn);
                  connectionManager.unexportResource(sanVolumeId, scIqn);
                } catch (Exception e) {
                  LOG.warn("Could not unexport " + sanVolumeId + " on backend from Storage Controller");
                }

                // Remove lun and update snapshot IQN
                try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
                  SANVolumeInfo existingSnap = Entities.uniqueResult(snapInfo);
                  existingSnap.setIqn(parts[0]); // Store the lun ID in the iqn string, its needed for disconnecting the snapshot from SC later
                  Entities.merge(existingSnap);
                  tran.commit();
                } catch (Exception e) {
                  LOG.warn("Failed to update IQN after disconnecting " + snapshotId + " from Storage Controller", e);
                  // Warn and move on, no need to throw exception
                }
              } else {
                // snapshot was never exported to SC, no need to disconnect it and un-export it
                // this could happen if snapshot upload was not necessary
                LOG.debug("Skipping disconnect and unexport operations for " + sanVolumeId);
              }

              // wait for snapshot operation to complete
              try {
                LOG.info("Waiting for " + sanVolumeId + " to complete on backend");
                connectionManager.waitAndComplete(sanVolumeId, iqnAndLun);
              } catch (EucalyptusCloudException e) {
                LOG.warn("Failed during wait for " + sanVolumeId + " to complete on backend", e);
                throw e;
              } catch (Exception e) {
                LOG.warn("Failed during wait for " + sanVolumeId + " to complete on backend", e);
                throw new EucalyptusCloudException("Failed during wait for " + sanVolumeId + " to complete on backend", e);
              }
            } catch (Exception e) {
              Exceptions.toException("", e);
            }

            return null;
          }
        });
      } catch (EucalyptusCloudException e) {
        LOG.debug("Deleting " + sanSnapshotId + " on backend");
        if (!connectionManager.deleteVolume(sanSnapshotId, iqn)) {
          LOG.debug("Failed to delete backend resource " + snapshotId);
        }
        throw e;
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
        LOG.warn("Encountered error while setting storage properties", e);
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
      String deviceName = connectionManager.connectTarget(iqn, null).getPath();
      return deviceName;
    } catch (TransactionException | NoSuchElementException ex) {
      LOG.warn("Block storage backend database record not found for " + volumeId);
      throw new EucalyptusCloudException("Block storage backend database record not found for " + volumeId, ex);
    }
  }

  public void importVolume(String volumeId, String volumePath, int size) throws EucalyptusCloudException {
    // may be throw an unsupported exception here
  }

  public String getSnapshotPath(String snapshotId) throws EucalyptusCloudException {
    return getVolumePath(snapshotId);
  }

  public void importSnapshot(String snapshotId, String volumeId, String snapPath, int size) throws EucalyptusCloudException {
    // may be throw an unsupported exception here
  }

  public String exportVolume(String volumeId, String nodeIqn) throws EucalyptusCloudException {
    SANVolumeInfo volumeInfo = lookup(volumeId);
    String sanVolumeId = volumeInfo.getSanVolumeId();

    LOG.info("Exporting " + sanVolumeId + " on backend to Node Controller host IQN " + nodeIqn);
    String lun = connectionManager.exportResource(sanVolumeId, nodeIqn, volumeInfo.getIqn());
    if (lun == null) {
      LOG.warn("Invalid value found for LUN upon exporting " + sanVolumeId + " on backend");
      throw new EucalyptusCloudException("Invalid value found for LUN upon exporting " + sanVolumeId + " on backend");
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
    String sanVolumeId = lookup(volumeId).getSanVolumeId();
    LOG.info("Unexporting " + sanVolumeId + " on backend from all hosts");
    connectionManager.unexportResourceFromAll(sanVolumeId);
  }

  public void unexportVolume(String volumeId, String nodeIqn) throws EucalyptusCloudException, UnsupportedOperationException {
    String sanVolumeId = lookup(volumeId).getSanVolumeId();
    LOG.info("Unexporting " + sanVolumeId + " on backend from Node Controller host IQN " + nodeIqn);
    connectionManager.unexportResource(sanVolumeId, nodeIqn);
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
      LOG.warn("Encountered error stopping connection manager", e);
      throw e;
    } finally {
      connectionManager = null;
    }
  }

  public void disable() throws EucalyptusCloudException {
    connectionManager.stop();
  }

  public void enable() throws EucalyptusCloudException {
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
      LOG.info("Checking for " + foundSnapInfo.getSanVolumeId() + " on backend");
      if (connectionManager.snapshotExists(foundSnapInfo.getSanVolumeId(), foundSnapInfo.getIqn())) { // Snapshot does exist. Nothing to do
        LOG.debug("Found database record and backend resource for " + snapshotId + ". Nothing to do here");
        return true;
      } else { // Snapshot does not exist on SAN. Delete the record and move to the next part
        LOG.debug("Found database record but resource does not exist on backend. Deleting database record for " + snapshotId);
        Entities.delete(foundSnapInfo);
        tran.commit();
      }
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
        LOG.info("Checking for " + foundSnapInfo.getSanVolumeId() + " on backend");
        if (connectionManager.snapshotExists(foundSnapInfo.getSanVolumeId(), foundSnapInfo.getIqn())) { // Snapshot does exist on SAN.
          // Create a record for it in this partition
          SANVolumeInfo newSnapInfo = new SANVolumeInfo(snapshotId, foundSnapInfo.getIqn(), foundSnapInfo.getSize())
              .withSanVolumeId(foundSnapInfo.getSanVolumeId()).withSnapshotOf(foundSnapInfo.getSnapshotOf());
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

  public void checkVolume(String volumeId) throws EucalyptusCloudException {}

  public List<CheckerTask> getCheckers() {
    return connectionManager.getCheckers();
  }

  public String createSnapshotPoint(String parentVolumeId, String volumeId) throws EucalyptusCloudException {
    String snapshotPoint = resourceIdOnSan(volumeId);
    SANVolumeInfo sanParentVolume = lookup(parentVolumeId);
    String sanParentVolumeId = sanParentVolume.getSanVolumeId();

    LOG.info("Creating snapshot point " + snapshotPoint + " on " + sanParentVolumeId);
    return connectionManager.createSnapshotPoint(sanParentVolumeId, snapshotPoint, sanParentVolume.getIqn());
  }

  // TODO: zhill, should I removed the extra params or only allow the parent and vol Id and then calculate the snapPointId from that?
  // If the desire is to make this idempotent then a calculation is ideal since the original may have been lost (i.e. restart)
  public void deleteSnapshotPoint(String parentVolumeId, String volumeId, String snapshotPointId) throws EucalyptusCloudException {
    SANVolumeInfo sanParentVolume = lookup(parentVolumeId);
    String sanParentVolumeId = sanParentVolume.getSanVolumeId();

    LOG.info("Deleting snapshot point " + snapshotPointId + " on " + sanParentVolumeId);
    connectionManager.deleteSnapshotPoint(sanParentVolumeId, snapshotPointId, sanParentVolume.getIqn());
  }

  @Override
  public boolean supportsIncrementalSnapshots() throws EucalyptusCloudException {
    return connectionManager.supportsIncrementalSnapshots();
  }

  @Override
  public StorageResourceWithCallback prepIncrementalSnapshotForUpload(String volumeId, String snapshotId, String snapPointId, String prevSnapshotId,
      String prevSnapPointId) throws EucalyptusCloudException {

    String sanVolId = lookup(volumeId).getSanVolumeId();
    String sanSnapId = lookup(snapshotId).getSanVolumeId();
    String prevSanSnapId = lookup(prevSnapshotId).getSanVolumeId();

    // TODO lookup IDs to make sure they are there
    return new StorageResourceWithCallback(connectionManager.generateSnapshotDelta(sanVolId, sanSnapId, snapPointId, prevSanSnapId, prevSnapPointId),
        new Function<StorageResource, String>() {

          @Override
          public String apply(StorageResource arg0) {
            try {
              LOG.debug("Executing callback after generating incremental snapshot for " + snapshotId);
              connectionManager.cleanupSnapshotDelta(sanSnapId, arg0);
            } catch (Exception e) {
              LOG.warn("Failed to execute callback after generating incremental snapshot for " + snapshotId, e);
            }
            return null;
          }

        });
  }

  @Override
  public StorageResource prepSnapshotForUpload(String volumeId, String snapshotId, String snapPointId) throws EucalyptusCloudException {
    // Look up snapshot in the database and get the backend snapshot ID
    SANVolumeInfo snapInfo = lookup(snapshotId);
    String snapIqn = snapInfo.getIqn();
    String sanSnapshotId = snapInfo.getSanVolumeId();
    StorageResource storageResource = null;

    String scIqn = StorageProperties.getStorageIqn();
    if (scIqn == null) {
      LOG.warn("Storage Controller IQN not found");
      throw new EucalyptusCloudException("Storage Controller IQN not found");
    }

    // Ensure that the SC can attach to the volume.
    String lun = null;
    try {
      LOG.info("Exporting " + sanSnapshotId + " on backend to Storage Controller host IQN " + scIqn);
      lun = connectionManager.exportResource(sanSnapshotId, scIqn, snapIqn);
    } catch (EucalyptusCloudException attEx) {
      LOG.warn("Failed to export " + sanSnapshotId + " on backend to Storage Controller", attEx);
      throw new EucalyptusCloudException(
          "Failed to create " + snapshotId + " due to error exporting " + sanSnapshotId + " on backend to Storage Controller", attEx);
    }

    if (lun == null) {
      LOG.warn("Invalid value found for LUN upon exporting " + sanSnapshotId + " on backend");
      throw new EucalyptusCloudException(
          "Failed to create " + snapshotId + " due to invalid value for LUN upon exporting " + sanSnapshotId + " on backend");
    }

    // Moved this to before the connection is attempted since the volume does exist, it may need to be cleaned
    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      SANVolumeInfo existingSnap = Entities.uniqueResult(snapInfo);
      existingSnap.setIqn(snapIqn + ',' + lun); // Store the lun ID in the iqn string, its needed for disconnecting the snapshot from SC later
      Entities.merge(existingSnap);
      tran.commit();
    } catch (Exception e) {
      LOG.warn("Failed to update IQN after exporting " + snapshotId + " to Storage Controller", e);
      throw new EucalyptusCloudException("Failed to update IQN after exporting " + snapshotId + " to Storage Controller", e);
    }

    // Run the connect
    try {
      LOG.info("Connecting " + sanSnapshotId + " on backend to Storage Controller for transfer");
      storageResource = connectionManager.connectTarget(snapIqn, lun);
      storageResource.setId(snapshotId);
    } catch (Exception connEx) {
      LOG.warn("Failed to connect " + sanSnapshotId + " on backend to Storage Controller. Detaching and cleaning up", connEx);
      try {
        LOG.info("Unexporting " + sanSnapshotId + " on backend from Storage Controller host IQN " + scIqn);
        connectionManager.unexportResource(sanSnapshotId, scIqn);
      } catch (EucalyptusCloudException detEx) {
        LOG.debug("Could not unexport " + sanSnapshotId + " during cleanup of failed connection");
      }
      throw new EucalyptusCloudException(
          "Failed to create " + snapshotId + " due to an error connecting " + sanSnapshotId + " on backend to Storage Controller", connEx);
    }

    return storageResource;
  }

  @Override
  public StorageResourceWithCallback prepSnapshotBaseForRestore(final String snapshotId, final int size, final String snapshotPointId)
      throws EucalyptusCloudException {
    try {
      // If any record for the snapshot exists, just copy that info
      lookup(snapshotId);
      LOG.debug("Found existing database record for " + snapshotId + ".  Will use that lun and record.");
      return null;
    } catch (NoSuchRecordException e) {
      // going forward with the assumption that snapshot record is not found for this SC
      LOG.debug(
          "Backend database record for " + snapshotId + " not found. Setting up holder on backend to hold the snapshot content downloaded from OSG");
    }

    String sanSnapshotId = resourceIdOnSan(snapshotId);
    String iqn = null;
    long actualSizeInMB = size * 1024;
    try {
      // TODO Create a database record first before firing off the volume creation
      LOG.info("Creating " + sanSnapshotId + " of size " + actualSizeInMB + " MB on backend");
      iqn = connectionManager.createSnapshotHolder(sanSnapshotId, actualSizeInMB);
    } catch (EucalyptusCloudException e) {
      LOG.warn("Failed to create backend resource for " + snapshotId);
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
          LOG.info("Exporting " + sanSnapshotId + " on backend to Storage Controller host IQN " + scIqn);
          lun = connectionManager.exportResource(sanSnapshotId, scIqn, iqn);
        } catch (EucalyptusCloudException attEx) {
          LOG.warn("Failed to export " + sanSnapshotId + " on backend to Storage Controller", attEx);
          throw new EucalyptusCloudException(
              "Failed to create " + snapshotId + " due to error exporting " + sanSnapshotId + " on backend to Storage Controller", attEx);
        }

        if (lun == null) {
          LOG.warn("Invalid value found for LUN upon exporting " + sanSnapshotId + " on backend");
          throw new EucalyptusCloudException(
              "Failed to create " + snapshotId + " due to invalid value for LUN upon exporting " + sanSnapshotId + " on backend");
        }

        // Store the lun ID in the iqn string, its needed for disconnecting the snapshot from SC later
        SANVolumeInfo snapInfo = new SANVolumeInfo(snapshotId, iqn + ',' + lun, size).withSanVolumeId(sanSnapshotId);
        try {
          Transactions.save(snapInfo);
        } catch (TransactionException e) {
          LOG.warn("Failed to update database record with IQN and LUN post creation for " + snapshotId);
          throw new EucalyptusCloudException("Failed to update database entity with IQN and LUN post creation for " + snapshotId, e);
        }

        // Run the connect
        StorageResource storageResource = null;
        try {
          LOG.info("Connecting " + sanSnapshotId + " on backend to Storage Controller for transfer");
          storageResource = connectionManager.connectTarget(iqn, lun);
          storageResource.setId(snapshotId);
        } catch (Exception connEx) {
          LOG.warn("Failed to connect " + sanSnapshotId + " on backend to Storage Controller. Detaching and cleaning up", connEx);
          try {
            LOG.info("Unexporting " + sanSnapshotId + " on backend from Storage Controller host IQN " + scIqn);
            connectionManager.unexportResource(sanSnapshotId, scIqn);
          } catch (EucalyptusCloudException detEx) {
            LOG.debug("Could not unexport " + sanSnapshotId + " during cleanup of failed connection");
          }
          throw new EucalyptusCloudException(
              "Failed to create " + snapshotId + " due to an error connecting " + sanSnapshotId + " on backend to Storage Controller", connEx);
        }

        return new StorageResourceWithCallback(storageResource, new Function<StorageResource, String>() {

          @Override
          public String apply(StorageResource arg0) {
            try {
              LOG.debug("Executing callback after prepping base for restoration of " + snapshotId);

              SANVolumeInfo snapInfo = null;
              String iqnAndLun = null;
              String sanVolumeId = null;

              try {
                snapInfo = lookup(snapshotId);
                iqnAndLun = snapInfo.getIqn();
                sanVolumeId = snapInfo.getSanVolumeId();
              } catch (NoSuchRecordException e) {
                LOG.debug("Skipping cleanup for " + snapshotId);
                return null;
              }

              if (iqnAndLun != null && iqnAndLun.contains(",")) { // disconnect and unexport the snapshot from SC
                String[] parts = iqnAndLun.split(",");
                if (parts.length == 2) {
                  // disconnect SC-snapshot iscsi connection
                  try {
                    LOG.info("Disconnecting " + sanVolumeId + " on backend from Storage Controller");
                    connectionManager.disconnectTarget(sanVolumeId, parts[0], parts[1]);
                  } catch (Exception e) {
                    LOG.warn("Failed to disconnect iscsi session between " + sanVolumeId + " and SC", e);
                  }
                } else {
                  LOG.warn("Unable to disconnect " + sanVolumeId
                      + " from Storage Controller due to invalid iqn format. Expected iqn to contain one ',' but got: " + iqnAndLun);
                  throw new EucalyptusCloudException("Unable to disconnect " + sanVolumeId + " from Storage Controller due to invalid iqn format");
                }

                // Unexport volume from SC
                String scIqn = StorageProperties.getStorageIqn();
                try {
                  LOG.info("Unexporting " + sanVolumeId + " on backend from Storage Controller host IQN " + scIqn);
                  connectionManager.unexportResource(sanVolumeId, scIqn);
                } catch (Exception e) {
                  LOG.warn("Could not unexport " + sanVolumeId + " on backend from Storage Controller");
                }

                // Remove lun and update snapshot IQN
                try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
                  SANVolumeInfo existingSnap = Entities.uniqueResult(snapInfo);
                  existingSnap.setIqn(parts[0]); // Store the lun ID in the iqn string, its needed for disconnecting the snapshot from SC later
                  Entities.merge(existingSnap);
                  tran.commit();
                } catch (Exception e) {
                  LOG.warn("Failed to update IQN after disconnecting " + snapshotId + " from Storage Controller", e);
                  // Warn and move on, no need to throw exception
                }

                // Complete snapshot base restoration
                return connectionManager.completeSnapshotBaseRestoration(sanVolumeId, snapshotPointId, parts[0]);
              } else {
                // snapshot was never exported to SC, no need to disconnect it and un-export it
                // this could happen if snapshot upload was not necessary
                LOG.debug("Skipping disconnect and unexport operations for " + sanVolumeId);
              }

            } catch (EucalyptusCloudException e) {
              Exceptions.toException(e);
            }
            return null;
          }
        });
      } catch (EucalyptusCloudException e) {
        LOG.debug("Deleting " + sanSnapshotId + " on backend");
        if (!connectionManager.deleteVolume(sanSnapshotId, iqn)) {
          LOG.debug("Failed to delete backend resource " + snapshotId);
        }
        throw e;
      }
    }
    return null;
  }

  @Override
  public void restoreSnapshotDelta(String currentSnapId, String prevSnapId, String baseId, StorageResource sr) throws EucalyptusCloudException {
    SANVolumeInfo snapInfo = null;
    String baseIqn = null;
    try {
      snapInfo = lookup(baseId);
      baseIqn = snapInfo.getIqn();
    } catch (NoSuchRecordException e) {
      LOG.warn("Unable to lookup " + baseId, e);
      throw new EucalyptusCloudException("Unable to lookup " + baseId, e);
    }

    // Apply delta
    try {
      LOG.debug("Applying snapshot delta between " + currentSnapId + " and " + prevSnapId + " on base " + baseIqn);
      connectionManager.restoreSnapshotDelta(baseIqn, sr);
    } catch (Exception e) {
      LOG.warn("Failed to apply delta between " + currentSnapId + " and " + prevSnapId + " on base " + baseIqn);
      throw new EucalyptusCloudException("Failed to apply delta between " + currentSnapId + " and " + prevSnapId + " on base " + baseIqn);
    }
  }

  @Override
  public void completeSnapshotRestorationFromDeltas(String snapshotId) throws EucalyptusCloudException {
    SANVolumeInfo snapInfo = null;
    String sanVolumeId = null;
    String iqn = null;
    try {
      snapInfo = lookup(snapshotId);
      sanVolumeId = snapInfo.getSanVolumeId();
      iqn = snapInfo.getIqn();
    } catch (NoSuchRecordException e) {
      LOG.warn("Unable to lookup " + snapshotId, e);
      throw new EucalyptusCloudException("Unable to lookup " + snapshotId, e);
    }

    // Any remaining tasks/clean up for restoring post delta application
    try {
      LOG.debug("Finishing up restoration and configuration of expected snapshot state for " + snapshotId);
      connectionManager.completeSnapshotDeltaRestoration(sanVolumeId, iqn);
    } catch (Exception e) {
      LOG.warn("Failed to finish restoration and configuration of expected snapshot state for  " + snapshotId);
      throw new EucalyptusCloudException("Failed to finish restoration and configuration of expected snapshot state for " + snapshotId, e);
    }
  }

  @Override
  public <F, T> T executeCallback(Function<F, T> callback, F input) throws EucalyptusCloudException {
    try {
      return callback.apply(input);
    } catch (Throwable t) {
      throw new EucalyptusCloudException("Unable to execute callback for due to", t);
    }
  }

  private SANVolumeInfo lookup(String resourceId) throws EucalyptusCloudException {
    SANVolumeInfo resourceInfo = null;
    try {
      resourceInfo = Transactions.find(new SANVolumeInfo(resourceId));
      if (resourceInfo == null || StringUtils.isBlank(resourceInfo.getSanVolumeId())) {
        LOG.warn("Backend name/ID not found for " + resourceId);
        throw new EucalyptusCloudException("Backend name/ID not found for " + resourceId);
      } else {
        return resourceInfo;
      }
    } catch (NoSuchElementException e) {
      LOG.warn("Block storage backend database record not found for " + resourceId);
      throw new NoSuchRecordException("Block storage backend database record not found for " + resourceId);
    } catch (EucalyptusCloudException e) {
      throw e;
    } catch (Exception e) {
      LOG.warn("Encountered error during block storage backend database lookup for " + resourceId);
      throw new EucalyptusCloudException("Encountered error during block storage backend database lookup for " + resourceId, e);
    }
  }
}
