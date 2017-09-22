/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2017 Ent. Services Development Corporation LP
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

package com.eucalyptus.blockstorage.ceph;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ceph.rbd.Rbd;

import com.eucalyptus.blockstorage.FileResource;
import com.eucalyptus.blockstorage.StorageManagers.StorageManagerProperty;
import com.eucalyptus.blockstorage.StorageResource;
import com.eucalyptus.blockstorage.ceph.entities.CephRbdImageToBeDeleted;
import com.eucalyptus.blockstorage.ceph.entities.CephRbdInfo;
import com.eucalyptus.blockstorage.ceph.entities.CephRbdSnapshotToBeDeleted;
import com.eucalyptus.blockstorage.ceph.entities.CephRbdSnapshotToBeDeleted_;
import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.entities.SnapshotInfo_;
import com.eucalyptus.blockstorage.san.common.SANManager;
import com.eucalyptus.blockstorage.san.common.SANProvider;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.storage.common.CheckerTask;
import com.eucalyptus.util.EucalyptusCloudException;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import edu.ucsb.eucalyptus.msgs.ComponentProperty;
import edu.ucsb.eucalyptus.util.SystemUtil;
import edu.ucsb.eucalyptus.util.SystemUtil.CommandOutput;
import edu.ucsb.eucalyptus.util.EucaSemaphore;
import edu.ucsb.eucalyptus.util.EucaSemaphoreDirectory;

/**
 * CephProvider implements the Eucalyptus Storage Controller plug-in for interacting with a Ceph cluster
 *
 */
@StorageManagerProperty(value = "ceph-rbd", manager = SANManager.class)
public class CephRbdProvider implements SANProvider {

  private static final Logger LOG = Logger.getLogger(CephRbdProvider.class);
  private static final Splitter COMMA_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();
  private static Set<String> accessiblePools;
  private static final String SEMAPHORE_PREFIX = "snapshot point volume ";
  private static final Function<CephRbdImageToBeDeleted, String> IMAGE_NAME_FUNCTION = new Function<CephRbdImageToBeDeleted, String>() {
    @Override
    public String apply(CephRbdImageToBeDeleted arg0) {
      return arg0.getImageName();
    }
  };

  private CephRbdAdapter rbdService;
  private CephRbdInfo cachedConfig;

  @Override
  public void initialize() {
    // Create and persist ceph info entity if its not already there
    LOG.info("Initializing CephInfo entity");
    CephRbdInfo.getStorageInfo();
  }

  @Override
  public void configure() throws EucalyptusCloudException {
    CephRbdInfo cephInfo = CephRbdInfo.getStorageInfo();
    initializeRbdService(cephInfo);
  }

  private void initializeRbdService(CephRbdInfo info) {
    LOG.info("Initializing Ceph RBD service provider");

    cachedConfig = info;

    if (rbdService == null) {
      rbdService = new CephRbdFormatTwoAdapter(cachedConfig);
    } else {
      // Changing the configuration in the existing reference rather than instantiating a new object as that might end up interrupting an already
      // existing operation
      rbdService.setCephConfig(cachedConfig);
    }

    // TODO Some way to check connectivity to ceph cluster

    accessiblePools = Sets.newHashSet();
    accessiblePools.addAll(COMMA_SPLITTER.splitToList(cachedConfig.getCephVolumePools()));
    accessiblePools.addAll(COMMA_SPLITTER.splitToList(cachedConfig.getCephSnapshotPools()));
  }

  @Override
  public void checkConnection() throws EucalyptusCloudException {
    CephRbdInfo info = CephRbdInfo.getStorageInfo();
    if (info != null && !cachedConfig.isSame(info)) {
      LOG.info("Detected a change in Ceph configuration");
      initializeRbdService(info);
    } else {
      // Nothing to do here
    }
  }

  @Override
  public String createVolume(String volumeId, String snapshotId, int snapSize, int size, String snapshotIqn) throws EucalyptusCloudException {
    LOG.info("Creating volume volumeId=" + volumeId + ", snapshotId=" + snapshotId + ", size=" + size + "GB, snapshotIqn=" + snapshotIqn);

    CanonicalRbdObject parent = CanonicalRbdObject.parse(snapshotIqn);
    if (parent != null && !Strings.isNullOrEmpty(parent.getPool())) {
      String iqn = null;
      if (size > snapSize) {
        iqn = rbdService.cloneAndResizeImage(snapshotId, CephRbdInfo.SNAPSHOT_ON_PREFIX + snapshotId, volumeId,
            Long.valueOf(size * StorageProperties.GB), parent.getPool());
      } else {
        iqn = rbdService.cloneAndResizeImage(snapshotId, CephRbdInfo.SNAPSHOT_ON_PREFIX + snapshotId, volumeId, null, parent.getPool());
      }
      return iqn;
    } else {
      LOG.warn("Expected snapshotIqn format: pool/image, actual snapshotIqn: " + snapshotIqn);
      throw new EucalyptusCloudException(
          "Failed to create volume " + volumeId + ". Expected snapshotIqn format: pool/image, actual snapshotIqn: " + snapshotIqn);

    }
  }

  @Override
  public String cloneVolume(String volumeId, String parentVolumeId, String parentVolumeIqn) throws EucalyptusCloudException {
    LOG.info("Cloning volume volumeId=" + volumeId + ", parentVolumeId=" + parentVolumeId + ", parentVolumeIqn=" + parentVolumeIqn);

    CanonicalRbdObject parent = CanonicalRbdObject.parse(parentVolumeIqn);
    if (parent != null && !Strings.isNullOrEmpty(parent.getPool())) {
      String snapshotPoint = CephRbdInfo.SNAPSHOT_ON_PREFIX + parentVolumeId;
      String iqn = rbdService.cloneAndResizeImage(parentVolumeId, snapshotPoint, volumeId, null, parent.getPool());
      return iqn;
    } else {
      LOG.warn("Expected snapshotIqn format: pool/image, actual parentVolumeIqn: " + parentVolumeIqn);
      throw new EucalyptusCloudException(
          "Failed to create volume " + volumeId + ". Expected snapshotIqn format: pool/image, actual parentVolumeIqn: " + parentVolumeIqn);

    }
  }

  @Override
  public StorageResource connectTarget(String iqn, String lun) throws EucalyptusCloudException {
    LOG.debug("Connecting iqn=" + iqn + ", lun=" + lun + ". This is a no-op");
    // iqn and lun are be the same, use one of them
    // SANManager changes the ID, so dont bother setting the volume ID (first parameter) here
    LOG.trace("Returning CephRbdResource initialized with inbound argument lun=" + lun);
    return new CephRbdResource(lun, lun);
  }

  @Override
  public String getVolumeConnectionString(String volumeId) {
    LOG.debug("Getting volume connection string volumeId=" + volumeId);
    return CephRbdInfo.getStorageInfo().getVirshSecret() + ",,,"; // <virsh secret uuid>,<empty path>
  }

  @Override
  public String createVolume(String volumeName, int size) throws EucalyptusCloudException {
    LOG.info("Creating volume volumeId=" + volumeName + ", size=" + size + "GB");
    long sizeInBytes = size * StorageProperties.GB; // need to go from gb to bytes
    String iqn = rbdService.createImage(volumeName, sizeInBytes);
    return iqn;
  }

  @Override
  public boolean deleteVolume(String volumeId, String volumeIqn) {
    LOG.info("Deleting volume volumeId=" + volumeId + ", volumeIqn=" + volumeIqn);

    boolean result = false;
    try {
      // volumeIqn is of the form pool/image, get the pool information
      CanonicalRbdObject can = CanonicalRbdObject.parse(volumeIqn);

      // Add images to be removed to the database, duty cycles will clean them up and update the database
      Transactions.save(new CephRbdImageToBeDeleted(volumeId, (can != null ? can.getPool() : null)));
      EucaSemaphoreDirectory.removeSemaphore(SEMAPHORE_PREFIX + volumeId);
      result = true;
    } catch (Exception e) {
      LOG.warn("Failed to save metadata for asynchronous deletion of " + volumeId);
    }

    return result;
  }

  @Override
  public boolean deleteSnapshot(String snapshotId, String snapshotIqn, String snapshotPointId) {
    LOG.info("Deleting snapshot snapshotId=" + snapshotId + ", snapshotIqn=" + snapshotIqn + ", snapshotPointId=" + snapshotPointId);
    try {
      // Deleting EBS snapshots involves
      // 1. Deleting RBD image mapped to the EBS snapshot
      // 2. Deleting RBD snapshot on the image mapped to the EBS volume against which the EBS snapshot was created

      // Parse snapshotIqn for information about RBD image. snapshotIqn is of the form pool/image
      CanonicalRbdObject snapImage = CanonicalRbdObject.parse(snapshotIqn);

      // Add it to database, duty cycles will clean them up and update the database
      Transactions.save(new CephRbdImageToBeDeleted(snapshotId, (snapImage != null ? snapImage.getPool() : null)));

      // Parse snapshotPointId for information about RBD snapshot. snapshotPointId is of the form pool/image@snapshot
      CanonicalRbdObject snapParent = CanonicalRbdObject.parse(snapshotPointId);
      if (snapParent != null && !Strings.isNullOrEmpty(snapParent.getSnapshot()) && !Strings.isNullOrEmpty(snapParent.getPool())
          && !Strings.isNullOrEmpty(snapParent.getImage())) {
        // Add it to database, duty cycles will clean them up and update the database
        Transactions.save(new CephRbdSnapshotToBeDeleted(snapParent.getPool(), snapParent.getImage(), snapParent.getSnapshot()));
      } else {
        // If the snapshot was created before Euca v4.4.0, then there would be
        // no snapshot point ID, but the PopulateSnapPoints groovy script
        // (existing only in v4.4.x) should have found it and filled in the
        // snapshot point ID during Ceph provider initialization.
        LOG.debug("Cannot delete RBD snapshot for " + snapshotId + " due to an invalid snapshot point ID " +
            snapshotPointId + ". If this EBS snapshot originated in another AZ, then this is normal. " +
            "Otherwise, you may have to delete the Ceph RBD snapshot manually.");
      }

      return true;
    } catch (Exception e) {
      LOG.warn("Failed to save metadata for asynchronous deletion of " + snapshotId);
    }

    return false;
  }

  @Override
  public String createSnapshot(String volumeId, String snapshotId, String snapshotPointId) throws EucalyptusCloudException {
    LOG.info("Creating snapshot snapshotId=" + snapshotId + ", volumeId=" + volumeId + ", snapshotPointId=" + snapshotPointId);

    // snapshotPointId is of the form pool/image@snapshot
    CanonicalRbdObject parent = CanonicalRbdObject.parse(snapshotPointId);
    if (parent != null && !Strings.isNullOrEmpty(parent.getPool()) && !Strings.isNullOrEmpty(parent.getImage())
        && !Strings.isNullOrEmpty(parent.getSnapshot())) {
      String iqn = rbdService.cloneAndResizeImage(parent.getImage(), parent.getSnapshot(), snapshotId, null, parent.getPool());
      return iqn;
    } else {
      LOG.warn("Expected snapshotPointId format: pool/image@snapshot, actual snapshotPointId: " + snapshotPointId);
      throw new EucalyptusCloudException(
          "Failed to create " + snapshotId + ". Expected snapshotPointId format: pool/image@snapshot, actual snapshotPointId: " + snapshotPointId);
    }
  }

  @Override
  public void deleteUser(String userName) throws EucalyptusCloudException {

  }

  @Override
  public void addUser(String userName) throws EucalyptusCloudException {

  }

  @Override
  public void disconnectTarget(String snapshotId, String iqn, String lun) throws EucalyptusCloudException {
    // Nothing to do here
  }

  @Override
  public void checkPreconditions() throws EucalyptusCloudException {
    // If librbd is not installed, things don't get this far. The classloader tries to load Rbd JNA bindings which statically invoke librbd and things
    // go spiraling downward from there
    try {
      int[] version = Rbd.getVersion();
      if (version != null && version.length == 3) {
        LOG.info("librbd version: " + new StringBuffer().append(version[0]).append('.').append(version[1]).append('.').append(version[2]).toString());
      } else {
        throw new EucalyptusCloudException("Invalid librbd version info");
      }
    } catch (Exception e) {
      LOG.warn("librbd version not found, librbd may not be installed!");
      throw new EucalyptusCloudException("librbd version not found, librbd may not be installed!", e);
    }
  }

  @Override
  public String exportResource(String volumeId, String nodeIqn, String volumeIqn) throws EucalyptusCloudException {
    LOG.debug("Exporting volumeId=" + volumeId + ", nodeIqn=" + nodeIqn + ", volumeIqn=" + volumeIqn + ". This is a no-op");
    // Volume IQN is usually in the form pool/image. This is no-op
    LOG.trace("Returning inbound argument volumeIqn=" + volumeIqn);
    return volumeIqn;
  }

  @Override
  public void unexportResource(String volumeId, String nodeIqn) throws EucalyptusCloudException {
    LOG.debug("Unexporting volumeId=" + volumeId + ", nodeIqn=" + nodeIqn + ". This is a no-op");
  }

  @Override
  public void unexportResourceFromAll(String volumeId) throws EucalyptusCloudException {
    LOG.debug("Unexporting from all volumeId=" + volumeId + ". This is a no-op");
  }

  @Override
  public void getStorageProps(ArrayList<ComponentProperty> componentProperties) {}

  @Override
  public void setStorageProps(ArrayList<ComponentProperty> storageProps) {}

  @Override
  public void stop() throws EucalyptusCloudException {}

  @Override
  public String getAuthType() {
    return null;
  }

  @Override
  public String getOptionalChapUser() {
    return null;
  }

  @Override
  public String createSnapshotHolder(String snapshotId, long snapSizeInMB) throws EucalyptusCloudException {
    LOG.debug("Creating snapshot holder snapshotId=" + snapshotId + ", size=" + snapSizeInMB + "MB");
    long sizeInBytes = snapSizeInMB * StorageProperties.MB; // need to go from mb to bytes
    String iqn = rbdService.createImage(snapshotId, sizeInBytes);
    return iqn;
  }

  @Override
  public boolean snapshotExists(String snapshotId, String snapshotIqn) throws EucalyptusCloudException {
    LOG.debug("Checking if snapshot exists snapshotId=" + snapshotId + ", snapshotIqn=" + snapshotIqn);
    return volumeExists(snapshotId, snapshotIqn);
  }

  @Override
  public String createSnapshotPoint(String parentVolumeId, String snapshotId, String parentVolumeIqn) throws EucalyptusCloudException {
    LOG.info("Creating snapshot point parentVolumeId=" + parentVolumeId + ", snapshotId=" + snapshotId + ", parentVolumeIqn=" + parentVolumeIqn);

    // parentVolumeIqn is of the form pool/image, get the pool information
    CanonicalRbdObject parent = CanonicalRbdObject.parse(parentVolumeIqn);
    if (parent != null && !Strings.isNullOrEmpty(parent.getPool())) {
      String snapshotPoint = CephRbdInfo.SNAPSHOT_FOR_PREFIX + snapshotId;
      String snapshotPointId = null;
      // Don't allow >1 concurrent snapshot operation on the same volume,
      // because Ceph sometimes has failures, see EUCA-13114
      EucaSemaphore semaphore = EucaSemaphoreDirectory.getSolitarySemaphore(SEMAPHORE_PREFIX + parentVolumeId);
      try {
        semaphore.acquire();
        LOG.trace("Acquired semaphore for Ceph createSnapshotPoint for volume " + parentVolumeId);
      } catch (InterruptedException ex) {
        throw new EucalyptusCloudException("Failed to create snapshot point " + snapshotId + " on volume " + parentVolumeId +
            " as the semaphore could not be acquired");
      }
      try {
        snapshotPointId = rbdService.createSnapshot(parentVolumeId, snapshotPoint, parent.getPool());
      } finally {
        LOG.trace("Releasing semaphore for Ceph createSnapshotPoint for volume " + parentVolumeId);
        semaphore.release();
      }
      LOG.info("Created snapshot point parentVolumeId=" + parentVolumeId + ", snapshotId=" + snapshotId + ", parentVolumeIqn=" + parentVolumeIqn);
      return snapshotPointId;
    } else {
      LOG.warn("Expected parentVolumeIqn format: pool/image, actual parentVolumeIqn: " + parentVolumeIqn);
      throw new EucalyptusCloudException("Failed to create snapshot point for " + snapshotId
          + ". Expected parentVolumeIqn format: pool/image, actual parentVolumeIqn: " + parentVolumeIqn);
    }
  }

  @Override
  public void deleteSnapshotPoint(String parentVolumeId, String snapshotPointId, String parentVolumeIqn) throws EucalyptusCloudException {
    LOG.info("Deleting snapshot point parentVolumeId=" + parentVolumeId + ", snapshotPointId=" + snapshotPointId + ", parentVolumeIqn="
        + parentVolumeIqn);
    // snapshotPointId is of the form pool/image@snapshot, get the pool information
    CanonicalRbdObject parent = CanonicalRbdObject.parse(snapshotPointId);
    if (parent != null && !Strings.isNullOrEmpty(parent.getPool()) && !Strings.isNullOrEmpty(parent.getImage())
        && !Strings.isNullOrEmpty(parent.getSnapshot())) {
      rbdService.deleteSnapshot(parent.getImage(), parent.getSnapshot(), parent.getPool());
    } else {
      LOG.warn("Expected snapshotPointId format: pool/image@snapshot, actual snapshotPointId: " + parentVolumeIqn);
      throw new EucalyptusCloudException("Failed to delete snapshot point " + snapshotPointId
          + ". Expected snapshotPointId format: pool/image@snapshot, actual snapshotPointId: " + parentVolumeIqn);
    }
  }

  @Override
  public void checkConnectionInfo() {
    // nothing to do here
  }

  @Override
  public boolean volumeExists(String volumeId, String volumeIqn) throws EucalyptusCloudException {
    LOG.debug("Checking if volume exists volumeId=" + volumeId + ", volumeIqn=" + volumeIqn);
    try {
      // volumeIqn is of the form pool/image, get the pool information
      CanonicalRbdObject vol = CanonicalRbdObject.parse(volumeIqn);
      if (vol != null && !Strings.isNullOrEmpty(vol.getPool())) {
        return rbdService.listPool(vol.getPool()).contains(volumeId);
      } else {
        if (null != rbdService.getImagePool(volumeId)) {
          return true;
        } else {
          return false;
        }
      }
    } catch (Exception e) {
      LOG.debug("Failed to find " + volumeId + ", considering volume non-existent or inaccessible to this az", e);
      return false;
    }
  }

  @Override
  public String getProtocol() {
    return "rbd";
  }

  @Override
  public String getProviderName() {
    return "ceph";
  }

  class CephRbdImageDeleter extends CheckerTask {

    public CephRbdImageDeleter() {
      this.name = CephRbdImageDeleter.class.getSimpleName();
      this.runInterval = 60;
      this.runIntervalUnit = TimeUnit.SECONDS;
      this.isFixedDelay = Boolean.TRUE;
    }

    @Override
    public void run() {
      try {
        LOG.trace("Starting Ceph RBD image cleanup process");
        for (final String pool : accessiblePools) { // Cycle through all pools
          try {
            CephRbdImageToBeDeleted search = new CephRbdImageToBeDeleted().withPoolName(pool);

            // Get the images that were marked for deletion from the database
            final List<String> imagesToBeCleaned = Transactions.transform(search, IMAGE_NAME_FUNCTION);
            LOG.trace("List of images to be cleaned up for pool " + pool + ": " + imagesToBeCleaned);

            // Invoke clean up
            List<String> imageSnapshotsDeleted =
                rbdService.cleanUpImages(pool, cachedConfig.getDeletedImagePrefix(), imagesToBeCleaned);
            if (imageSnapshotsDeleted != null && !imageSnapshotsDeleted.isEmpty()) {
              LOG.debug("List of snapshots (on images) that were cleaned up for pool " + pool + ": " + imageSnapshotsDeleted);
            }

            // Delete database records of to-be-deleted images after call to rbd succeeds
            if (imagesToBeCleaned != null && !imagesToBeCleaned.isEmpty()) {
              Transactions.deleteAll(search, new Predicate<CephRbdImageToBeDeleted>() {
                @Override
                public boolean apply(CephRbdImageToBeDeleted arg0) {
                  return imagesToBeCleaned.contains(arg0.getImageName());
                }
              });
            }

            // Delete database records of to-be-deleted snapshots for those snapshots
            // that were actually deleted.
            if (imageSnapshotsDeleted != null && !imageSnapshotsDeleted.isEmpty()) {
              CephRbdSnapshotToBeDeleted searchDeleted = new CephRbdSnapshotToBeDeleted().withPool(pool);
              Transactions.deleteAll(searchDeleted, new Predicate<CephRbdSnapshotToBeDeleted>() {
                @Override
                public boolean apply(CephRbdSnapshotToBeDeleted arg0) {
                  return imageSnapshotsDeleted.contains(arg0.getSnapshot());
                }
              });
            }
          } catch (Throwable t) {
            LOG.debug("Encountered error while cleaning up images in pool " + pool, t);
          }
        }
      } catch (Exception e) {
        LOG.debug("Ignoring exception during clean up of images marked for deletion", e);
      }

    }
  }

  class CephRbdSnapshotDeleter extends CheckerTask {

    public CephRbdSnapshotDeleter() {
      this.name = CephRbdSnapshotDeleter.class.getSimpleName();
      this.runInterval = 120;
      this.runIntervalUnit = TimeUnit.SECONDS;
      this.isFixedDelay = Boolean.TRUE;
    }

    @Override
    public void run() {
      try {
        LOG.trace("Starting Ceph RBD snapshot cleanup process");
        for (final String pool : accessiblePools) { // Cycle through all pools
          try {
            // Get the snapshots that were marked for deletion from the database,
            // in reverse time order so we never try to delete a parent before a child
            List<CephRbdSnapshotToBeDeleted> listToBeDeleted = null;
            try (TransactionResource tr = Entities.transactionFor(CephRbdSnapshotToBeDeleted.class)) {
              listToBeDeleted = Entities.criteriaQuery(
                  Entities.restriction(CephRbdSnapshotToBeDeleted.class)
                  .like(CephRbdSnapshotToBeDeleted_.pool, pool).build())
                  .orderByDesc(CephRbdSnapshotToBeDeleted_.creationTimestamp)
                  .list();
              tr.commit();
            } catch (Exception e) {
              LOG.warn("Failed database lookup of snapshots marked for deletion from OSG", e);
              return;
            }

            if (listToBeDeleted != null && !listToBeDeleted.isEmpty()) {
              SetMultimap<String, String> toBeDeleted = Multimaps.newSetMultimap(Maps.newHashMap(), new Supplier<Set<String>>() {

                @Override
                public Set<String> get() {
                  return Sets.newHashSet();
                }
              });

              // Organize stuff into a multimap
              for (CephRbdSnapshotToBeDeleted r : listToBeDeleted) {
                toBeDeleted.put(r.getImage(), r.getSnapshot());
              }
              LOG.trace("List of snapshots to be cleaned up for pool " + pool + ": " + toBeDeleted.values());

              // Invoke clean up
              SetMultimap<String, String> cantBeDeleted =
                  rbdService.cleanUpSnapshots(pool, cachedConfig.getDeletedImagePrefix(), toBeDeleted);
              LOG.trace("List of snapshots that can't be cleaned up for pool " + pool + ": " + cantBeDeleted.values());

              // Delete database records for all except those that couldn't be cleaned up
              CephRbdSnapshotToBeDeleted search = new CephRbdSnapshotToBeDeleted().withPool(pool);
              Transactions.deleteAll(search, new Predicate<CephRbdSnapshotToBeDeleted>() {
                @Override
                public boolean apply(CephRbdSnapshotToBeDeleted arg0) {
                  return toBeDeleted.containsEntry(arg0.getImage(), arg0.getSnapshot())
                      && !cantBeDeleted.containsEntry(arg0.getImage(), arg0.getSnapshot());
                }
              });

            } else {
              // nothing to do here, no snaps to be deleted in this pool
            }
          } catch (Throwable t) {
            LOG.debug("Encountered error while cleaning up rbd snapshots in pool " + pool, t);
          }
        }
      } catch (Exception e) {
        LOG.debug("Ignoring exception during clean up of rbd snapshots marked for deletion", e);
      }
    }
  }

  @Override
  public void waitAndComplete(String snapshotId, String snapshotIqn) throws EucalyptusCloudException {
    LOG.debug("Waiting for snapshot completion snapshotId=" + snapshotId + ", snapshotIqn=" + snapshotIqn);

    // snapshotIqn is of the form pool/image, get the pool information
    CanonicalRbdObject parent = CanonicalRbdObject.parse(snapshotIqn);
    if (parent != null && !Strings.isNullOrEmpty(parent.getPool())) {
      // Create a snapshot on the image for future use as one might not exist
      String snapshotPoint = CephRbdInfo.SNAPSHOT_ON_PREFIX + snapshotId;
      rbdService.createSnapshot(snapshotId, snapshotPoint, parent.getPool());
    } else {
      LOG.warn("Expected snapshotIqn format: pool/image, actual snapshotIqn: " + snapshotIqn);
      throw new EucalyptusCloudException(
          "Failed to complete " + snapshotId + ". Expected snapshotIqn format: pool/image, actual snapshotIqn: " + snapshotIqn);
    }
  }

  @Override
  public List<CheckerTask> getCheckers() {
    List<CheckerTask> list = Lists.newArrayList();
    list.add(new CephRbdImageDeleter());
    list.add(new CephRbdSnapshotDeleter());
    return list;
  }

  @Override
  public boolean supportsIncrementalSnapshots() throws EucalyptusCloudException {
    // TODO check configuration to see if delta support is enabled
    return true;
  }

  @Override
  public StorageResource generateSnapshotDelta(String volumeId, String snapshotId, String snapPointId, String prevSnapshotId, String prevSnapPointId)
      throws EucalyptusCloudException {
    LOG.info("Generating snapshot delta volumeId=" + volumeId + ", snapshot=" + snapshotId + ", snapshotPointId=" + snapPointId + ", prevSnapshotId="
        + prevSnapshotId + ", prevSnapPointId=" + prevSnapPointId);
    String diffName = null;
    try {
      String prevSnapPoint = null;
      if (StringUtils.isBlank(prevSnapPointId)) {
        prevSnapPoint = CephRbdInfo.SNAPSHOT_FOR_PREFIX + prevSnapshotId;
      } else if (prevSnapPointId.contains(CephRbdInfo.POOL_IMAGE_DELIMITER) && prevSnapPointId.contains(CephRbdInfo.IMAGE_SNAPSHOT_DELIMITER)) {
        CanonicalRbdObject prevSnap = CanonicalRbdObject.parse(prevSnapPointId);
        if (prevSnap != null && !Strings.isNullOrEmpty(prevSnap.getSnapshot())) {
          prevSnapPoint = prevSnap.getSnapshot();
        } else {
          throw new EucalyptusCloudException("Invalid snapshotPointId, expected pool/image@snapshot format but got " + prevSnapPointId);
        }
      } else {
        prevSnapPoint = prevSnapPointId;
      }
      Path diffPath = Files.createTempFile(Paths.get("/var/tmp"), snapshotId + "_" + prevSnapshotId + "_", ".diff");
      Files.deleteIfExists(diffPath); // Delete the file before invoking rbd. rbd does not like the file being present
      diffName = diffPath.toString();

      String[] cmd = new String[] {StorageProperties.EUCA_ROOT_WRAPPER, "rbd", "--id", cachedConfig.getCephUser(), "--keyring",
          cachedConfig.getCephKeyringFile(), "export-diff", snapPointId, diffName, "--from-snap", prevSnapPoint};
      LOG.debug("Executing: " + Joiner.on(" ").skipNulls().join(cmd));
      CommandOutput output = SystemUtil.runWithRawOutput(cmd);
      if (output != null) {
        LOG.debug("Dump from rbd command:\nreturn=" + output.returnValue + "\nstdout=" + output.output + "\nstderr=" + output.error);
        if (output.returnValue != 0) {
          throw new EucalyptusCloudException(
              "Unable to execute rbd command. return=" + output.returnValue + ", stdout=" + output.output + ", stderr=" + output.error);
        }
      }

      return new FileResource(snapshotId, diffName);
    } catch (Exception e) {
      LOG.warn("Failed to generate snapshot delta between " + snapshotId + " and " + prevSnapshotId, e);
      try {
        if (!Strings.isNullOrEmpty(diffName)) {
          LOG.debug("Deleting file " + diffName);
          new File(diffName).delete();
        }
      } catch (Exception ie) {
        LOG.warn("Failed to delete file " + diffName, ie);
      }
      throw new EucalyptusCloudException("Failed to generate snapshot delta between " + snapshotId + " and " + prevSnapshotId, e);
    }
  }

  @Override
  public void cleanupSnapshotDelta(String snapshotId, StorageResource sr) throws EucalyptusCloudException {
    LOG.info("Cleaning up snapshot delta for snapshotId=" + snapshotId);
    if (sr != null && !Strings.isNullOrEmpty(sr.getPath())) {
      try {
        // root wrap shell out to delete the file since its owned by root and sticky bits on /var/tmp don't let unprivileged users to delete
        String[] cmd = new String[] {StorageProperties.EUCA_ROOT_WRAPPER, "rm", "-f", sr.getPath()};
        LOG.debug("Executing: " + Joiner.on(" ").skipNulls().join(cmd));
        CommandOutput output = SystemUtil.runWithRawOutput(cmd);
        if (output != null) {
          LOG.trace("Dump from command execution:\nreturn=" + output.returnValue + "\nstdout=" + output.output + "\nstderr=" + output.error);
        } else {
          LOG.warn("Received invalid response from deletion of " + sr.getPath());
        }
      } catch (Exception e) {
        LOG.warn("Failed to delete file " + sr.getPath(), e);
      }
    }
  }

  @Override
  public String completeSnapshotBaseRestoration(String snapshotId, String baseSnapPointId, String snapshotIqn) throws EucalyptusCloudException {
    LOG.info("Completing restoration of base snapshotId=" + snapshotId + ", snapshotPointId=" + baseSnapPointId + ", snapshotIqn=" + snapshotIqn);
    // parentVolumeIqn is of the form pool/image, get the pool information
    CanonicalRbdObject parent = CanonicalRbdObject.parse(snapshotIqn);
    if (parent != null && !Strings.isNullOrEmpty(parent.getPool())) {

      // baseSnapPointId is of the form pool/image@snapshot
      CanonicalRbdObject base = CanonicalRbdObject.parse(baseSnapPointId);
      if (base != null && !Strings.isNullOrEmpty(base.getSnapshot())) {
        return rbdService.createSnapshot(snapshotId, base.getSnapshot(), parent.getPool());
      } else {
        LOG.warn("Expected baseSnapPointId format: pool/image@snapshot, actual baseSnapPointId: " + baseSnapPointId);
        throw new EucalyptusCloudException("Failed to complete restoration of base for  " + snapshotId
            + ". Expected baseSnapPointId format: pool/image@snapshot, actual baseSnapPointId: " + baseSnapPointId);
      }
    } else {
      LOG.warn("Expected snapshotIqn format: pool/image, actual baseSnapPointId: " + snapshotIqn);
      throw new EucalyptusCloudException("Failed to complete restoration of base for  " + snapshotId
          + ". Expected snapshotIqn format: pool/image, actual baseSnapPointId: " + snapshotIqn);
    }
  }

  @Override
  public void restoreSnapshotDelta(String baseIqn, StorageResource sr) throws EucalyptusCloudException {
    LOG.info("Restoring delta on base=" + baseIqn + ", snapshotId=" + sr.getId() + ", snapsDeltaFile=" + sr.getPath());
    try {
      // Apply diff
      String[] cmd = new String[] {StorageProperties.EUCA_ROOT_WRAPPER, "rbd", "--id", cachedConfig.getCephUser(), "--keyring",
          cachedConfig.getCephKeyringFile(), "import-diff", sr.getPath(), baseIqn};
      LOG.debug("Executing: " + Joiner.on(" ").skipNulls().join(cmd));
      CommandOutput output = SystemUtil.runWithRawOutput(cmd);
      if (output != null) {
        LOG.debug("Dump from rbd command:\nReturn value=" + output.returnValue + "\nOutput=" + output.output + "\nDebug=" + output.error);
        if (output.returnValue != 0) {
          throw new EucalyptusCloudException("Unable to execute rbd command. Failed with error: " + output.output + "\n" + output.error);
        }
      }
    } catch (Exception e) {
      LOG.warn("Failed to apply snapshot delta on " + baseIqn, e);
      throw new EucalyptusCloudException("Failed to apply snapshot delta on " + baseIqn, e);
    } finally {
      // clean up the diff file
      try {
        LOG.trace("About to delete diff file " + sr.getPath());
        if (!Files.deleteIfExists(Paths.get(sr.getPath()))) {
          LOG.warn("Diff file " + sr.getPath() + "did not exist to delete.");
        } else {
          LOG.trace("Successfully deleted diff file " + sr.getPath());
        }
      } catch (Exception e) {
        LOG.warn("Failed to delete diff file " + sr.getPath(), e);
      }
    }
  }

  @Override
  public void completeSnapshotDeltaRestoration(String snapshotId, String snapshotIqn) throws EucalyptusCloudException {
    LOG.info("Cleaning up all existing rbd snapshots and creating a fresh new one, snapshotId=" + snapshotId + ", snapshotIqn=" + snapshotIqn);

    // snapshotIqn is of the form pool/image, get the pool information
    CanonicalRbdObject parent = CanonicalRbdObject.parse(snapshotIqn);
    if (parent != null && !Strings.isNullOrEmpty(parent.getPool()) && !Strings.isNullOrEmpty(parent.getImage())) {
      rbdService.deleteAllSnapshots(parent.getImage(), parent.getPool(), CephRbdInfo.SNAPSHOT_ON_PREFIX + snapshotId);
    } else {
      LOG.warn("Expected snapshotIqn format: pool/image, actual snapshotIqn: " + snapshotIqn);
      throw new EucalyptusCloudException(
          "Failed to clean up and restore " + snapshotId + ". Expected snapshotIqn format: pool/image, actual snapshotIqn: " + snapshotIqn);
    }
  }
}
