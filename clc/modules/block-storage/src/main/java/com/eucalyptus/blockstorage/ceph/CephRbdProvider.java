/*******************************************************************************
 *Copyright (c) 2014  Eucalyptus Systems, Inc.
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

package com.eucalyptus.blockstorage.ceph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ceph.rbd.Rbd;
import com.eucalyptus.blockstorage.StorageManagers.StorageManagerProperty;
import com.eucalyptus.blockstorage.StorageResource;
import com.eucalyptus.blockstorage.ceph.entities.CephRbdImageToBeDeleted;
import com.eucalyptus.blockstorage.ceph.entities.CephRbdInfo;
import com.eucalyptus.blockstorage.ceph.exceptions.CephImageNotFoundException;
import com.eucalyptus.blockstorage.ceph.exceptions.EucalyptusCephException;
import com.eucalyptus.blockstorage.san.common.SANManager;
import com.eucalyptus.blockstorage.san.common.SANProvider;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.storage.common.CheckerTask;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.ComponentProperty;

/**
 * CephProvider implements the Eucalyptus Storage Controller plug-in for interacting with a Ceph cluster
 * 
 */
@StorageManagerProperty(value = "ceph-rbd", manager = SANManager.class)
public class CephRbdProvider implements SANProvider {

  private static final Logger LOG = Logger.getLogger(CephRbdProvider.class);
  private static final Splitter POOL_SPLITTER = Splitter.on(CephRbdInfo.POOL_IMAGE_DELIMITER).trimResults().omitEmptyStrings();
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
    LOG.debug("Creating volume volumeId=" + volumeId + ", snapshotId=" + snapshotId + ", size=" + size + "GB, snapshotIqn=" + snapshotIqn);
    // snapshotIqn is of the form pool/image, get the pool information
    String snapPoolName = separateAndReturnPoolName(snapshotIqn);
    String iqn = null;
    if (size > snapSize) {
      iqn = rbdService.cloneAndResizeImage(snapshotId, CephRbdInfo.SNAPSHOT_ON_PREFIX + snapshotId, volumeId,
          Long.valueOf(size * StorageProperties.GB), snapPoolName);
    } else {
      iqn = rbdService.cloneAndResizeImage(snapshotId, CephRbdInfo.SNAPSHOT_ON_PREFIX + snapshotId, volumeId, null, snapPoolName);
    }
    return iqn;
  }

  @Override
  public String cloneVolume(String volumeId, String parentVolumeId, String parentVolumeIqn) throws EucalyptusCloudException {
    LOG.debug("Cloneing volume volumeId=" + volumeId + ", parentVolumeId=" + parentVolumeId + ", parentVolumeIqn=" + parentVolumeIqn);
    // parentVolumeIqn is of the form pool/image, get the pool information
    String parentPoolName = separateAndReturnPoolName(parentVolumeIqn);
    String snapshotPoint = CephRbdInfo.SNAPSHOT_ON_PREFIX + parentVolumeId;
    String iqn = rbdService.cloneAndResizeImage(parentVolumeId, snapshotPoint, volumeId, null, parentPoolName);
    return iqn;
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
    LOG.debug("Creating volume volumeId=" + volumeName + ", size=" + size + "GB");
    long sizeInBytes = size * StorageProperties.GB; // need to go from gb to bytes
    String iqn = rbdService.createImage(volumeName, sizeInBytes);
    return iqn;
  }

  @Override
  public boolean deleteVolume(String volumeId, String volumeIqn) {
    LOG.debug("Delete volume volumeId=" + volumeId + ", volumeIqn=" + volumeIqn);

    boolean result = false;
    try {
      // volumeIqn is of the form pool/image, get the pool information
      String poolName = separateAndReturnPoolName(volumeIqn);

      // Add images to be removed to the database, duty cycles will clean them up and update the database
      Transactions.save(new CephRbdImageToBeDeleted(volumeId, poolName));
      result = true;
    } catch (Exception e) {
      LOG.warn("Failed to save metadata for asynchronous deletion of " + volumeId);
    }

    return result;
  }

  @Override
  public String createSnapshot(String volumeId, String snapshotId, String snapshotPointId, String volumeIqn) throws EucalyptusCloudException {
    LOG.debug("Creating snapshot snapshotId=" + snapshotId + ", volumeId=" + volumeId + ", snapshotPointId=" + snapshotPointId + ", volumeIqn="
        + volumeIqn);
    // volumeIqn is of the form pool/image, get the pool information
    String volPoolName = separateAndReturnPoolName(volumeIqn);
    String iqn = rbdService.cloneAndResizeImage(volumeId, snapshotPointId, snapshotId, null, volPoolName);
    return iqn;
  }

  @Override
  public boolean deleteSnapshot(String volumeId, String snapshotId, boolean locallyCreated, String snapshotIqn) {
    LOG.debug("Deleting snapshot snapshotId=" + snapshotId + ", volumeId=" + volumeId + ", locallCreated=" + locallyCreated + ", snapshotIqn="
        + snapshotIqn);
    try {
      deleteVolume(snapshotId, snapshotIqn);
    } catch (CephImageNotFoundException e) {
      return true;
    } catch (EucalyptusCephException e) {
      return false;
    }
    return true;
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
    LOG.debug("Unnexporting volumeId=" + volumeId + ", nodeIqn=" + nodeIqn + ". This is a no-op");
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
    LOG.debug("Creating snapshot point parentVolumeId=" + parentVolumeId + ", snapshotId=" + snapshotId + ", parentVolumeIqn=" + parentVolumeIqn);
    // parentVolumeIqn is of the form pool/image, get the pool information
    String parentPoolName = separateAndReturnPoolName(parentVolumeIqn);
    String snapshotPoint = CephRbdInfo.SNAPSHOT_FOR_PREFIX + snapshotId;
    rbdService.createSnapshot(parentVolumeId, snapshotPoint, parentPoolName);
    return snapshotPoint;
  }

  @Override
  public void deleteSnapshotPoint(String parentVolumeId, String snapshotPointId, String parentVolumeIqn) throws EucalyptusCloudException {
    LOG.debug("Deleting snapshot point  parentVolumeId=" + parentVolumeId + ", snapshotPointId=" + snapshotPointId + ", parentVolumeIqn="
        + parentVolumeIqn);
    // parentVolumeIqn is of the form pool/image, get the pool information
    String parentPoolName = separateAndReturnPoolName(parentVolumeIqn);
    rbdService.deleteSnapshot(parentVolumeId, snapshotPointId, parentPoolName);
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
      String poolName = separateAndReturnPoolName(volumeIqn);
      if (poolName != null) {
        return rbdService.listPool(poolName).contains(volumeId);
      } else {
        if (null != rbdService.getImagePool(volumeId)) {
          return true;
        } else {
          return false;
        }
      }
    } catch (Exception e) {
      LOG.debug("Caught error in check for " + volumeId, e);
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
        Set<String> poolSet = new HashSet<String>(Arrays.asList(cachedConfig.getCephVolumePools().split(",")));
        poolSet.addAll(Arrays.asList(cachedConfig.getCephSnapshotPools().split(",")));

        for (final String pool : poolSet) { // Cycle through all pools
          try {
            CephRbdImageToBeDeleted search = new CephRbdImageToBeDeleted().withPoolName(pool);

            // Get the images that were marked for deletion from the database
            final List<String> imagesToBeCleaned = Transactions.transform(search, IMAGE_NAME_FUNCTION);

            // Invoke clean up
            rbdService.cleanUpImages(pool, cachedConfig.getDeletedImagePrefix(), imagesToBeCleaned);

            // Delete database records after call to rbd succeeds
            if (imagesToBeCleaned != null && !imagesToBeCleaned.isEmpty()) {
              Transactions.deleteAll(search, new Predicate<CephRbdImageToBeDeleted>() {
                @Override
                public boolean apply(CephRbdImageToBeDeleted arg0) {
                  return imagesToBeCleaned.contains(arg0.getImageName());
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

  @Override
  public void waitAndComplete(String snapshotId, String snapshotIqn) throws EucalyptusCloudException {
    LOG.debug("Waiting for snapshot completion snapshotId=" + snapshotId + ", snapshotIqn=" + snapshotIqn);

    // Create a snapshot on the image for future use as one might not exist
    String snapshotPoint = CephRbdInfo.SNAPSHOT_ON_PREFIX + snapshotId;

    // snapshotIqn is of the form pool/image, get the pool information
    String poolName = separateAndReturnPoolName(snapshotIqn);

    if ( StringUtils.isNotBlank(poolName)) {
      rbdService.createSnapshot(snapshotId, snapshotPoint, poolName);
    } else {
      LOG.warn("Cannot create Ceph snapshot " + snapshotPoint + " on image " + snapshotId + ". Missing pool information");
    }
  }

  @Override
  public List<CheckerTask> getCheckers() {
    List<CheckerTask> list = Lists.newArrayList();
    list.add(new CephRbdImageDeleter());
    return list;
  }

  private String separateAndReturnPoolName(String poolAndImage) {
    if (StringUtils.isNotBlank(poolAndImage)) {
      Iterable<String> parts = POOL_SPLITTER.split(poolAndImage);
      if (parts != null && parts.iterator() != null && parts.iterator().hasNext()) {
        return parts.iterator().next();
      }
    }
    return null;
  }
}
