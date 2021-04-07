/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2016 Ent. Services Development Corporation LP
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

package com.eucalyptus.blockstorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.blockstorage.StorageManagers.StorageManagerProperty;
import com.eucalyptus.blockstorage.entities.DASInfo;
import com.eucalyptus.blockstorage.entities.DirectStorageInfo;
import com.eucalyptus.blockstorage.entities.ISCSIVolumeInfo;
import com.eucalyptus.blockstorage.entities.LVMVolumeInfo;
import com.eucalyptus.blockstorage.entities.StorageInfo;
import com.eucalyptus.blockstorage.entities.VolumeInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.cluster.common.ClusterController;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.common.CheckerTask;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;

import edu.ucsb.eucalyptus.msgs.ComponentProperty;
import edu.ucsb.eucalyptus.util.SystemUtil;
import edu.ucsb.eucalyptus.util.SystemUtil.CommandOutput;

@StorageManagerProperty("das")
public class DASManager implements LogicalStorageManager {
  private static Logger LOG = Logger.getLogger(DASManager.class);
  public static final String lvmRootDirectory = "/dev";
  protected static final long LVM_HEADER_LENGTH = 4 * StorageProperties.MB;
  public static final String PATH_SEPARATOR = "/";
  public static boolean initialized = false;
  public static final int MAX_LOOP_DEVICES = 256;
  public static final String EUCA_ROOT_WRAPPER = BaseDirectory.LIBEXEC.toString() + "/euca_rootwrap";
  public static final String EUCA_VAR_RUN_PATH = BaseDirectory.RUN.toString( );
  public static final StorageExportManager exportManager = new ISCSIManager();
  public static final StorageExportManager threadedExportManager = new ThreadPoolDispatchingStorageExportManager(new ISCSIManager());
  private static String volumeGroup;
  protected ConcurrentHashMap<String, VolumeOpMonitor> volumeOps;

  @Override
  public void checkPreconditions() throws EucalyptusCloudException {
    // check if binaries exist, commands can be executed, etc.
    if (!new File(EUCA_ROOT_WRAPPER).exists()) {
      throw new EucalyptusCloudException("root wrapper (euca_rootwrap) does not exist in " + EUCA_ROOT_WRAPPER);
    }
    File varDir = new File(EUCA_VAR_RUN_PATH);
    if (!varDir.exists()) {
      varDir.mkdirs();
    }
    try {
      String returnValue = LVMWrapper.getLvmVersion();
      if (returnValue.length() == 0) {
        throw new EucalyptusCloudException("Is lvm installed?");
      } else {
        LOG.debug("lvm version: " + returnValue);
      }

      // exportManager = new ISCSIManager();
      if (exportManager != null) {
        exportManager.checkPreconditions();
      }
    } catch (EucalyptusCloudException ex) {
      String error = "Unable to run command: " + ex.getMessage();
      LOG.error(error);
      throw new EucalyptusCloudException(error);
    }
  }

  private void updateVolumeGroup() throws EucalyptusCloudException {
    if (volumeGroup == null) {
      String dasDevice = DASInfo.getStorageInfo().getDASDevice();
      if (dasDevice != null) {
        try {
          boolean volumeGroupFound = false;
          String returnValue = null;
          try {
            returnValue = LVMWrapper.getVolumeGroup(dasDevice);
            if (returnValue.length() > 0) {
              volumeGroupFound = true;
            }
          } catch (EucalyptusCloudException e) {
            LOG.warn(e);
          }
          if (volumeGroupFound) {
            Pattern volumeGroupPattern = Pattern.compile("(?s:.*VG Name)(.*)\n.*");
            Matcher m = volumeGroupPattern.matcher(returnValue);
            if (m.find())
              volumeGroup = m.group(1).trim();
            else
              throw new EucalyptusCloudException("Not a volume group: " + dasDevice);
          } else {
            boolean physicalVolumeGroupFound = false;
            try {
              returnValue = LVMWrapper.getPhysicalVolume(dasDevice);
              if (returnValue.matches("(?s:.*)PV Name.*" + dasDevice + "(?s:.*)")) {
                physicalVolumeGroupFound = true;
              }
            } catch (EucalyptusCloudException e) {
              LOG.warn(e);
            }
            if (!physicalVolumeGroupFound) {
              returnValue = LVMWrapper.createPhysicalVolume(dasDevice);
              if (returnValue.length() == 0) {
                throw new EucalyptusCloudException("Unable to create physical volume on device: " + dasDevice);
              }
            }
            // PV should be initialized at this point.
            returnValue = LVMWrapper.getPhysicalVolumeVerbose(dasDevice);
            if (returnValue.matches("(?s:.*)PV Name.*" + dasDevice + "(?s:.*)")) {
              Pattern volumeGroupPattern = Pattern.compile("(?s:.*VG Name)(.*)\n.*");
              Matcher m = volumeGroupPattern.matcher(returnValue);
              if (m.find()) {
                volumeGroup = m.group(1).trim();
              }
              if ((volumeGroup == null) || (volumeGroup.length() == 0)) {
                volumeGroup = generateVGName(Crypto.getRandom(10));
                returnValue = LVMWrapper.createVolumeGroup(dasDevice, volumeGroup);
                if (returnValue.length() == 0) {
                  throw new EucalyptusCloudException("Unable to create volume group: " + volumeGroup + " physical volume: " + dasDevice);
                }
              }
            } else {
              Pattern volumeGroupPattern = Pattern.compile("(?s:.*VG Name)(.*)\n.*");
              Matcher m = volumeGroupPattern.matcher(returnValue);
              if (m.find())
                volumeGroup = m.group(1).trim();
              else
                throw new EucalyptusCloudException("Unable to get volume group for physical volume: " + dasDevice);
            }
          }
        } catch (EucalyptusCloudException e) {
          LOG.error(e);
          throw new EucalyptusCloudException(e);
        }
      } else {
        throw new EucalyptusCloudException("DAS partition not yet configured. Please specify partition.");
      }
    }
  }

  protected String duplicateLogicalVolume(String oldLvName, String newLvName) throws EucalyptusCloudException {
    return SystemUtil.run(new String[] {EUCA_ROOT_WRAPPER, "dd", "if=" + oldLvName, "of=" + newLvName, "bs=" + StorageProperties.blockSize});
  }

  protected String createFile(String fileName, long size) throws EucalyptusCloudException {
    if (!DirectStorageInfo.getStorageInfo().getZeroFillVolumes())
      return SystemUtil.run(new String[] {EUCA_ROOT_WRAPPER, "dd", "if=/dev/zero", "of=" + fileName, "count=1", "bs=" + StorageProperties.blockSize,
          "seek=" + (size - 1)});
    else
      return SystemUtil
          .run(new String[] {EUCA_ROOT_WRAPPER, "dd", "if=/dev/zero", "of=" + fileName, "count=" + size, "bs=" + StorageProperties.blockSize});
  }

  protected String createEmptyFile(String fileName, int size) throws EucalyptusCloudException {
    long fileSize = size * 1024;
    return createFile(fileName, fileSize);
  }

  public String createAbsoluteEmptyFile(String fileName, long size) throws EucalyptusCloudException {
    size = size / ObjectStorageProperties.M;
    return createFile(fileName, size);
  }

  @Override
  public void initialize() throws EucalyptusCloudException {
    if (!initialized) {
      // DO NOT WANT!
      // System.loadLibrary("dascontrol");
      // registerSignals();
      initialized = true;
    }
  }

  @Override
  public void configure() throws EucalyptusCloudException {
    exportManager.configure();
    // Initialize StorageInfo, DirectStorageInfo and DASInfo entities
    StorageInfo.getStorageInfo();
    DirectStorageInfo.getStorageInfo();
    DASInfo.getStorageInfo();
  }

  public void startupChecks() {
    reload();
  }

  public void cleanVolume(String volumeId) {
    try {
      updateVolumeGroup();
      try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
        LVMVolumeInfo lvmVolInfo = volumeManager.getVolumeInfo(volumeId);
        if (lvmVolInfo != null) {
          volumeManager.unexportVolume(lvmVolInfo);
          String lvName = lvmVolInfo.getLvName();
          String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + lvName;
          try {
            String returnValue = LVMWrapper.removeLogicalVolume(absoluteLVName);
          } catch (EucalyptusCloudException ex) {
            // volumeManager.abort();
            String error = "Unable to run command: " + ex.getMessage();
            LOG.error(error);
          }
          // Always delete the metadata regardless of the actual clean up
          volumeManager.remove(lvmVolInfo);
          volumeManager.finish();
        }
      }
    } catch (EucalyptusCloudException e) {
      LOG.debug("Failed to clean volume: " + volumeId, e);
      return;
    }
  }

  @Override
  public void cleanSnapshot(String snapshotId, String snapshotPointId) {
    try {
      updateVolumeGroup();
      try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
        LVMVolumeInfo lvmVolInfo = volumeManager.getVolumeInfo(snapshotId);
        if (lvmVolInfo != null) {
          volumeManager.remove(lvmVolInfo);
        }
        volumeManager.finish();
      }
    } catch (EucalyptusCloudException e) {
      LOG.debug("Failed to clean snapshotId: " + snapshotId, e);
      return;
    }
  }

  public native void registerSignals();

  public void dupFile(String oldFileName, String newFileName) {
    FileOutputStream fileOutputStream = null;
    FileChannel out = null;
    FileInputStream fileInputStream = null;
    FileChannel in = null;
    try {
      fileOutputStream = new FileOutputStream(new File(newFileName));
      out = fileOutputStream.getChannel();
      fileInputStream = new FileInputStream(new File(oldFileName));
      in = fileInputStream.getChannel();
      in.transferTo(0, in.size(), out);
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      if (fileOutputStream != null) {
        try {
          out.close();
          fileOutputStream.close();
        } catch (IOException e) {
          LOG.error(e);
        }
      }
      if (fileInputStream != null) {
        try {
          in.close();
          fileInputStream.close();
        } catch (IOException e) {
          LOG.error(e);
        }
      }
    }
  }

  // creates a logical volume (and a new physical volume and volume group)
  public void createLogicalVolume(String volumeId, String lvName, long size) throws EucalyptusCloudException {
    if (volumeGroup != null) {
      String returnValue = LVMWrapper.createLogicalVolume(volumeId, volumeGroup, lvName, size);
      if (returnValue.length() == 0) {
        throw new EucalyptusCloudException("Unable to create logical volume " + lvName + " in volume group " + volumeGroup);
      }
    } else {
      throw new EucalyptusCloudException("Volume group is null! This should never happen");
    }
  }

  @Override
  public void createVolume(String volumeId, int size) throws EucalyptusCloudException {
    updateVolumeGroup();
    File volumeDir = new File(DirectStorageInfo.getStorageInfo().getVolumesDir());
    volumeDir.mkdirs();
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {

      String lvName = generateLVName(volumeId);// "lv-" + Hashes.getRandom(4);
      LVMVolumeInfo lvmVolumeInfo = null;
      lvmVolumeInfo = new ISCSIVolumeInfo();

      volumeManager.finish();
      // create file and attach to loopback device
      try {
        // create logical volume
        createLogicalVolume(volumeId, lvName, (size * StorageProperties.KB));
        lvmVolumeInfo.setVolumeId(volumeId);
        lvmVolumeInfo.setVgName(volumeGroup);
        lvmVolumeInfo.setLvName(lvName);
        lvmVolumeInfo.setStatus(StorageProperties.Status.available.toString());
        lvmVolumeInfo.setSize(size);
        try (VolumeMetadataManager nestedVolumeManager = new VolumeMetadataManager()) {
          nestedVolumeManager.add(lvmVolumeInfo);
          nestedVolumeManager.finish();
        }
      } catch (EucalyptusCloudException ex) {
        String error = "Unable to run command: " + ex.getMessage();
        LOG.error(error);
        throw new EucalyptusCloudException(error);
      }
    }
  }

  @Override
  public int createVolume(String volumeId, String snapshotId, int size) throws EucalyptusCloudException {
    updateVolumeGroup();
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      LVMVolumeInfo foundSnapshotInfo = volumeManager.getVolumeInfo(snapshotId);
      if (foundSnapshotInfo != null) {
        String status = foundSnapshotInfo.getStatus();
        if (StorageProperties.Status.available.toString().equals(status)) {
          String lvName = generateLVName(volumeId); // "lv-" + Hashes.getRandom(4);
          LVMVolumeInfo lvmVolumeInfo = volumeManager.getVolumeInfo();
          String snapId = foundSnapshotInfo.getVolumeId();
          String loFileName = foundSnapshotInfo.getLoFileName();
          volumeManager.finish();
          try {
            File snapshotFile = new File(DirectStorageInfo.getStorageInfo().getVolumesDir() + PATH_SEPARATOR + snapId);
            assert (snapshotFile.exists());
            long absoluteSize;
            if (size <= 0 || size == foundSnapshotInfo.getSize()) {
              // size = (int)(snapshotFile.length() / StorageProperties.GB);
              absoluteSize = snapshotFile.length() / StorageProperties.MB;
              size = (int) (absoluteSize / StorageProperties.KB);
            } else {
              absoluteSize = size * StorageProperties.KB;
            }
            // create physical volume, volume group and logical volume
            createLogicalVolume(volumeId, lvName, absoluteSize);
            // duplicate snapshot volume
            String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + lvName;
            duplicateLogicalVolume(loFileName, absoluteLVName);
            lvmVolumeInfo.setVolumeId(volumeId);
            lvmVolumeInfo.setVgName(volumeGroup);
            lvmVolumeInfo.setLvName(lvName);
            lvmVolumeInfo.setStatus(StorageProperties.Status.available.toString());
            lvmVolumeInfo.setSize(size);
            try (VolumeMetadataManager nestedVolumeManager = new VolumeMetadataManager()) {
              nestedVolumeManager.add(lvmVolumeInfo);
              nestedVolumeManager.finish();
            }
          } catch (EucalyptusCloudException ex) {
            String error = "Unable to run command: " + ex.getMessage();
            LOG.error(error);
            throw new EucalyptusCloudException(error);
          }
        } else {
          throw new EucalyptusCloudException(
              "Cannot create volume " + volumeId + " from snapshot " + snapshotId + " since snapshot status is " + status);
        }
      } else {
        throw new EucalyptusCloudException("Unable to find snapshot: " + snapshotId);
      }
      return size;
    }
  }

  public int resizeVolume(String volumeId, int size) throws EucalyptusCloudException {
    return -1;
  }

  @Override
  public void cloneVolume(String volumeId, String parentVolumeId) throws EucalyptusCloudException {
    updateVolumeGroup();
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      LVMVolumeInfo foundVolumeInfo = volumeManager.getVolumeInfo(parentVolumeId);
      if (foundVolumeInfo != null) {
        String status = foundVolumeInfo.getStatus();
        String lvName = generateLVName(volumeId); // "lv-" + Hashes.getRandom(4);
        LVMVolumeInfo lvmVolumeInfo = volumeManager.getVolumeInfo();
        String parentLvName = foundVolumeInfo.getLvName();
        int size = foundVolumeInfo.getSize();
        volumeManager.finish();
        try {
          File parentVolumeFile = new File(DirectStorageInfo.getStorageInfo().getVolumesDir() + PATH_SEPARATOR + parentVolumeId);
          assert (parentVolumeFile.exists());
          long absouluteSize = (parentVolumeFile.length() / StorageProperties.MB);
          // create physical volume, volume group and logical volume
          createLogicalVolume(volumeId, lvName, absouluteSize);
          // duplicate snapshot volume
          String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + lvName;
          String absoluteParentLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + parentLvName;
          duplicateLogicalVolume(absoluteParentLVName, absoluteLVName);
          // export logical volume
          try {
            volumeManager.exportVolume(lvmVolumeInfo, volumeGroup, lvName);
          } catch (EucalyptusCloudException ex) {
            String returnValue = LVMWrapper.removeLogicalVolume(absoluteLVName);
            throw ex;
          }
          lvmVolumeInfo.setVolumeId(volumeId);
          lvmVolumeInfo.setVgName(volumeGroup);
          lvmVolumeInfo.setLvName(lvName);
          lvmVolumeInfo.setStatus(StorageProperties.Status.available.toString());
          lvmVolumeInfo.setSize(size);
          try (VolumeMetadataManager nestedVolumeManager = new VolumeMetadataManager()) {
            nestedVolumeManager.add(lvmVolumeInfo);
            nestedVolumeManager.finish();
          }
        } catch (EucalyptusCloudException ex) {
          String error = "Unable to run command: " + ex.getMessage();
          LOG.error(error);
          throw new EucalyptusCloudException(error);
        }
      } else {
        throw new EucalyptusCloudException("Unable to find volume: " + parentVolumeId);
      }
    }
  }

  @Override
  public void addSnapshot(String snapshotId) throws EucalyptusCloudException {
    String snapshotRawFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + snapshotId;
    File snapshotFile = new File(snapshotRawFileName);
    if (snapshotFile.exists()) {
      try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
        LVMVolumeInfo lvmVolumeInfo = volumeManager.getVolumeInfo();
        lvmVolumeInfo.setVolumeId(snapshotId);
        lvmVolumeInfo.setLoFileName(snapshotRawFileName);
        lvmVolumeInfo.setStatus(StorageProperties.Status.available.toString());
        lvmVolumeInfo.setSize((int) (snapshotFile.length() / StorageProperties.GB));
        volumeManager.add(lvmVolumeInfo);
        volumeManager.finish();
      }
    } else {
      throw new EucalyptusCloudException("Snapshot backing file does not exist for: " + snapshotId);
    }
  }

  @Override
  public void deleteVolume(String volumeId) throws EucalyptusCloudException {
    updateVolumeGroup();
    LVMVolumeInfo foundLVMVolumeInfo = null;
    {
      try (final VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
        foundLVMVolumeInfo = volumeManager.getVolumeInfo(volumeId);
        volumeManager.finish();
      }
    }

    if (foundLVMVolumeInfo != null) {
      boolean isReadyForDelete = false;
      int retryCount = 0;

      // Obtain a lock on the volume
      VolumeOpMonitor monitor = getMonitor(foundLVMVolumeInfo.getVolumeId());
      do {
        LOG.debug("Trying to lock volume for export detection and deletion " + volumeId);
        synchronized (monitor) {
          try (final VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
            foundLVMVolumeInfo = volumeManager.getVolumeInfo(volumeId);
            if (exportManager.isExported(foundLVMVolumeInfo)) {
              LOG.error("Cannot delete volume " + volumeId + " because it is currently exported");
              volumeManager.finish();
            } else {
              LOG.debug("Volume " + volumeId + " is prepped for deletion");
              isReadyForDelete = true;
              LOG.info("Deleting volume " + volumeId);
              String lvName = foundLVMVolumeInfo.getLvName();
              String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + lvName;
              String returnValue = "";
              for (int i = 0; i < 5; ++i) {
                returnValue = LVMWrapper.removeLogicalVolume(absoluteLVName);
                if (returnValue.length() != 0) {
                  if (returnValue.contains("successfully removed")) {
                    break;
                  }
                }
                // retry lv deletion (can take a while).
                try {
                  Thread.sleep(500);
                } catch (InterruptedException e) {
                  LOG.error(e);
                  break;
                }
              }
              if (returnValue.length() == 0) {
                throw new EucalyptusCloudException("Unable to remove logical volume " + absoluteLVName);
              }

              volumeManager.remove(foundLVMVolumeInfo);
              volumeManager.finish();
              break;
            }
          } catch (Exception e) {
            LOG.warn("Error cleaning up volume " + volumeId, e);
          }
          LOG.debug("Lap: " + retryCount++);
        } // Release the lock for retry.
        if (!isReadyForDelete) {
          try {
            Thread.sleep(10000); // sleep before the retry
          } catch (InterruptedException e) {
            throw new EucalyptusCloudException("Thread interrupted. Failing volume delete for volume " + volumeId);
          }
        }
      } while (!isReadyForDelete && retryCount < 20);

      // Remove the monitor
      removeMonitor(volumeId);

      if (!isReadyForDelete) {
        LOG.error("All attempts to cleanup volume " + volumeId + " failed");
        throw new EucalyptusCloudException("Unable to delete volume: " + volumeId + ". All attempts to cleanup the volume failed");
      }
    } else {
      throw new EucalyptusCloudException("Unable to find volume: " + volumeId);
    }
  }

  protected static String generateLVName(String baseName) {
    return "euca-" + baseName;
  }

  protected static String generateVGName(String baseName) {
    return "euca-ebs-storage-vg-" + baseName;
  }

  @Override
  public void createSnapshot(String volumeId, String snapshotId, String snapshotPointId) throws EucalyptusCloudException {
    if (snapshotPointId != null) {
      throw new EucalyptusCloudException("Synchronous snapshot points not supported in DAS storage manager");
    }

    updateVolumeGroup();
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      LVMVolumeInfo foundLVMVolumeInfo = volumeManager.getVolumeInfo(volumeId);
      // StorageResource snapInfo = null;
      if (foundLVMVolumeInfo != null) {
        LVMVolumeInfo snapshotInfo = volumeManager.getVolumeInfo();
        snapshotInfo.setVolumeId(snapshotId);
        File snapshotDir = new File(DirectStorageInfo.getStorageInfo().getVolumesDir());
        snapshotDir.mkdirs();

        String lvName = generateLVName(snapshotId);// "lv-snap-" + Hashes.getRandom(4);
        String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + foundLVMVolumeInfo.getLvName();

        int size = foundLVMVolumeInfo.getSize();
        volumeManager.finish();
        // volumeManager = null;
        try {
          long absoluteSize;
          CommandOutput result = SystemUtil.runWithRawOutput(new String[] {EUCA_ROOT_WRAPPER, "blockdev", "--getsize64", absoluteLVName});
          if (null != result && result.returnValue == 0 && StringUtils.isNotBlank(StringUtils.trim(result.output))) {
            try {
              absoluteSize = (Long.parseLong(StringUtils.trim(result.output)) / StorageProperties.MB);
            } catch (NumberFormatException e) {
              LOG.debug("Failed to parse size of volume " + volumeId, e);
              absoluteSize = size * StorageProperties.KB;
            }
          } else {
            absoluteSize = size * StorageProperties.KB;
          }

          // create physical volume, volume group and logical volume
          // String returnValue = createSnapshotLogicalVolume(absoluteLVName, lvName, size);
          String returnValue = LVMWrapper.createSnapshotLogicalVolume(absoluteLVName, lvName, absoluteSize);
          if (returnValue.length() == 0) {
            throw new EucalyptusCloudException("Unable to create snapshot logical volume " + lvName + " for volume " + lvName);
          }
          String snapRawFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + snapshotId;
          String absoluteSnapLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + lvName;

          duplicateLogicalVolume(absoluteSnapLVName, snapRawFileName);

          returnValue = LVMWrapper.removeLogicalVolume(absoluteSnapLVName);
          if (returnValue.length() == 0) {
            throw new EucalyptusCloudException("Unable to remove logical volume " + absoluteSnapLVName);
          }
          snapshotInfo.setLoFileName(snapRawFileName);
          // snapshotInfo.setStatus(StorageProperties.Status.available.toString());
          snapshotInfo.setSize(size);
          try (VolumeMetadataManager nestedVolumeManager = new VolumeMetadataManager()) {
            nestedVolumeManager.add(snapshotInfo);
            nestedVolumeManager.finish();
          }
          // snapInfo = new FileResource(snapshotId, snapRawFileName);
        } catch (Exception ex) {
          String error = "Unable to run command: " + ex.getMessage();
          LOG.error(error);
          throw new EucalyptusCloudException(error);
        }
      }
    }
  }

  public List<String> prepareForTransfer(String snapshotId) throws EucalyptusCloudException {
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      LVMVolumeInfo foundLVMVolumeInfo = volumeManager.getVolumeInfo(snapshotId);
      ArrayList<String> returnValues = new ArrayList<String>();

      if (foundLVMVolumeInfo != null) {
        returnValues.add(DirectStorageInfo.getStorageInfo().getVolumesDir() + PATH_SEPARATOR + foundLVMVolumeInfo.getVolumeId());
        volumeManager.finish();
      } else {
        volumeManager.abort();
        throw new EucalyptusCloudException("Unable to find snapshot: " + snapshotId);
      }
      return returnValues;
    }
  }

  @Override
  public void deleteSnapshot(String snapshotId, String snapshotPointId) throws EucalyptusCloudException {
    updateVolumeGroup();
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      LVMVolumeInfo foundLVMVolumeInfo = volumeManager.getVolumeInfo(snapshotId);

      if (foundLVMVolumeInfo != null) {
        volumeManager.remove(foundLVMVolumeInfo);
        File snapFile = new File(DirectStorageInfo.getStorageInfo().getVolumesDir() + File.separator + foundLVMVolumeInfo.getVolumeId());
        volumeManager.finish();
        if (snapFile.exists()) {
          if (!snapFile.delete()) {
            throw new EucalyptusCloudException("Unable to delete: " + snapFile.getAbsolutePath());
          }
        }
      } else {
        throw new EucalyptusCloudException("Unable to find snapshot: " + snapshotId);
      }
    }
  }

  public String getVolumeConnectionString(String volumeId) throws EucalyptusCloudException {
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      String returnValue = volumeManager.getConnectionString(volumeId);
      volumeManager.finish();
      return returnValue;
    }
  }

  public void reload() {
    LOG.info("Reload: starting reload process to re-export volumes if necessary");
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      List<LVMVolumeInfo> volumeInfos = volumeManager.getAllVolumeInfos();
      // now enable them
      for (LVMVolumeInfo foundVolumeInfo : volumeInfos) {
        try {
          LOG.info("Reload: Checking volume " + foundVolumeInfo.getVolumeId() + " for export");
          if (foundVolumeInfo.getVgName() != null && volumeManager.shouldExportOnReload(foundVolumeInfo)) {
            LOG.info("Reload: Volume " + foundVolumeInfo.getVolumeId()
                + " was exported at shutdown. Not found to be already exported. Re-exporting volume.");
            volumeManager.exportVolume(foundVolumeInfo);
          } else {
            LOG.info(
                "Reload: volume " + foundVolumeInfo.getVolumeId() + " not previously exported or already exported, no action required. Skipping");
          }
        } catch (EucalyptusCloudException ex) {
          LOG.error("Unable to reload volume: " + foundVolumeInfo.getVolumeId() + " due to: " + ex);
        }
      }
      volumeManager.finish();
    }
  }

  public int getSnapshotSize(String snapshotId) throws EucalyptusCloudException {
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      LVMVolumeInfo lvmVolumeInfo = volumeManager.getVolumeInfo(snapshotId);
      if (lvmVolumeInfo != null) {
        int snapSize = lvmVolumeInfo.getSize();
        volumeManager.finish();
        return snapSize;
      } else {
        volumeManager.abort();
        return 0;
      }
    }
  }

  protected static class VolumeMetadataManager implements AutoCloseable {
    private EntityTransaction transaction;

    protected VolumeMetadataManager() {
      transaction = Entities.get(VolumeInfo.class);
    }

    @Override
    public void close() {
      if (isActive()) {
        transaction.rollback();
      }
    }

    /**
     * Returns if the volume should be re-exported on reload based on DB state. Masks the implementation details from DASManager so it can be changed
     * based on DB or iscsi implementation
     * 
     * @param lvmVolumeInfo
     * @return
     */
    public boolean shouldExportOnReload(LVMVolumeInfo lvmVolumeInfo) {
      if (lvmVolumeInfo instanceof ISCSIVolumeInfo) {
        ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) lvmVolumeInfo;
        try {
          return iscsiVolumeInfo.getTid() >= 0 && !exportManager.isExported(lvmVolumeInfo);
        } catch (EucalyptusCloudException e) {
          LOG.error("Failed to determine if volume " + lvmVolumeInfo.getVolumeId() + " is already exported. Returning false", e);
        }
      }
      return false;
    }

    public List<String> getSnapshotValues(String snapshotId) {
      ArrayList<String> returnValues = new ArrayList<String>();
      LVMVolumeInfo lvmVolumeInfo = getVolumeInfo(snapshotId);
      return returnValues;
    }

    public void exportVolume(LVMVolumeInfo lvmVolumeInfo) throws EucalyptusCloudException {
      if (lvmVolumeInfo instanceof ISCSIVolumeInfo) {
        ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) lvmVolumeInfo;
        String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + iscsiVolumeInfo.getVgName() + PATH_SEPARATOR + iscsiVolumeInfo.getLvName();
        if (!LVMWrapper.logicalVolumeExists(absoluteLVName)) {
          LOG.error("Backing volume not found: " + absoluteLVName);
          throw new EucalyptusCloudException("Logical volume not found: " + absoluteLVName);
        }
        try {
          LVMWrapper.enableLogicalVolume(absoluteLVName);
        } catch (EucalyptusCloudException ex) {
          String error = "Unable to run command: " + ex.getMessage();
          LOG.error(error);
          throw new EucalyptusCloudException(ex);
        }
        ((ISCSIManager) exportManager).exportTarget(iscsiVolumeInfo.getVolumeId(), iscsiVolumeInfo.getTid(), iscsiVolumeInfo.getStoreName(),
            iscsiVolumeInfo.getLun(), absoluteLVName, iscsiVolumeInfo.getStoreUser());
      }
    }

    public String getConnectionString(String volumeId) {
      LVMVolumeInfo lvmVolumeInfo = getVolumeInfo(volumeId);
      if (lvmVolumeInfo != null) {
        ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) lvmVolumeInfo;
        String storeName = iscsiVolumeInfo.getStoreName();
        String encryptedPassword;
        try {
          encryptedPassword = ((ISCSIManager) exportManager).getEncryptedPassword();
        } catch (EucalyptusCloudException e) {
          LOG.error(e);
          return null;
        }
        return "iscsi,tgt,,,," + encryptedPassword + ",," + StorageProperties.STORAGE_HOST + "," + storeName;
      }
      return null;
    }

    public void unexportVolume(LVMVolumeInfo volumeInfo) {
      StorageExportManager manager = exportManager;
      if (!(exportManager instanceof ISCSIManager)) {
        manager = new ISCSIManager();
      }
      ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) volumeInfo;

      // Use the absolute name to verify that the target is correct before unexport
      String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + iscsiVolumeInfo.getVgName() + PATH_SEPARATOR + iscsiVolumeInfo.getLvName();
      if (LVMWrapper.logicalVolumeExists(absoluteLVName)) {
        try {
          ((ISCSIManager) manager).unexportTarget(volumeInfo.getVolumeId(), iscsiVolumeInfo.getTid(), iscsiVolumeInfo.getLun(), absoluteLVName);
        } catch (EucalyptusCloudException e) {
          LOG.error("Error unexporting target for volume " + volumeInfo.getVolumeId(), e);
          return;
        }
      }
      iscsiVolumeInfo.setTid(-1);
    }

    protected void finish() {
      try {
        transaction.commit();
      } catch (Exception ex) {
        LOG.error(ex, ex);
        transaction.rollback();
      }
    }

    protected void abort() {
      transaction.rollback();
    }

    protected boolean isActive() {
      return transaction != null && transaction.isActive();
    }

    protected LVMVolumeInfo getVolumeInfo(String volumeId) {
      ISCSIVolumeInfo ISCSIVolumeInfo = new ISCSIVolumeInfo(volumeId);
      List<ISCSIVolumeInfo> ISCSIVolumeInfos = Entities.query(ISCSIVolumeInfo);
      if (ISCSIVolumeInfos.size() > 0) {
        return ISCSIVolumeInfos.get(0);
      }
      return null;
    }

    protected boolean areSnapshotsPending(String volumeId) {
      ISCSIVolumeInfo ISCSIVolumeInfo = new ISCSIVolumeInfo();
      ISCSIVolumeInfo.setSnapshotOf(volumeId);
      ISCSIVolumeInfo.setStatus(StorageProperties.Status.pending.toString());
      List<ISCSIVolumeInfo> ISCSIVolumeInfos = Entities.query(ISCSIVolumeInfo);
      if (ISCSIVolumeInfos.size() > 0) {
        return true;
      }
      return false;
    }

    protected LVMVolumeInfo getVolumeInfo() {
      return new ISCSIVolumeInfo();
    }

    protected List<LVMVolumeInfo> getAllVolumeInfos() {
      List<LVMVolumeInfo> volumeInfos = new ArrayList<LVMVolumeInfo>();
      volumeInfos.addAll(Entities.query(new ISCSIVolumeInfo()));
      return volumeInfos;
    }

    protected void add(LVMVolumeInfo volumeInfo) {
      Entities.persist(volumeInfo);
    }

    protected void remove(LVMVolumeInfo volumeInfo) {
      Entities.delete(volumeInfo);
    }

    protected String encryptTargetPassword(String password) throws EucalyptusCloudException {
      try {
        List<ServiceConfiguration> partitionConfigs = ServiceConfigurations.listPartition(ClusterController.class, StorageProperties.NAME);
        ServiceConfiguration clusterConfig = partitionConfigs.get(0);
        PublicKey ncPublicKey = Partitions.lookup(clusterConfig).getNodeCertificate().getPublicKey();
        Cipher cipher = Ciphers.RSA_PKCS1.get();
        cipher.init(Cipher.ENCRYPT_MODE, ncPublicKey, Crypto.getSecureRandomSupplier().get());
        return new String(Base64.encode(cipher.doFinal(password.getBytes())));
      } catch (Exception e) {
        LOG.error("Unable to encrypt storage target password");
        throw new EucalyptusCloudException(e.getMessage(), e);
      }
    }

    protected void exportVolume(LVMVolumeInfo lvmVolumeInfo, String vgName, String lvName) throws EucalyptusCloudException {
      ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) lvmVolumeInfo;

      String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;
      EucalyptusCloudException ex = null;
      
      // 1st attempt's retry timeout is 10 ms, 2nd is 40ms etc., up to the configurable total timeout.
      final int timeoutMultiplier = 4;
      int attempt = 1;
      Long retryTimeout = 10l; // ms
      Long totalTimeoutSoFar = 0l;
      Long totalTimeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();

      try {
        threadedExportManager.allocateTarget(iscsiVolumeInfo);
      } catch (EucalyptusCloudException ece) {
        LOG.error("Failed to allocate target for volume " + iscsiVolumeInfo.getVolumeId(), ece);
        throw ece;
      }
      do {
        try {
          ((ISCSIManager) exportManager).exportTarget(iscsiVolumeInfo.getVolumeId(), iscsiVolumeInfo.getTid(), iscsiVolumeInfo.getStoreName(),
              iscsiVolumeInfo.getLun(), absoluteLVName, iscsiVolumeInfo.getStoreUser());
          ex = null;
          if (attempt > 1) {
            LOG.info("Exporting volume " + iscsiVolumeInfo.getVolumeId() + " as target " + iscsiVolumeInfo.getTid() + 
                " succeeded on retry attempt " + attempt);
          }
          break;
        } catch (EucalyptusCloudException ece) {
          ex = ece;
          LOG.warn("Failed to export volume " + iscsiVolumeInfo.getVolumeId() + " on attempt " + attempt);
          totalTimeoutSoFar += retryTimeout;
          retryTimeout *= timeoutMultiplier;
          attempt++;
        }
      } while (totalTimeoutSoFar < totalTimeout);

      // EUCA-3597 After all retries, check if the process actually completed
      if (null != ex) {
        LOG.error("Failed to export volume " + iscsiVolumeInfo.getVolumeId() + ", all retries exhausted.");
        throw ex;
      }
    }
  }

  @Override
  public void loadSnapshots(List<String> snapshotSet, List<String> snapshotFileNames) throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public StorageResourceWithCallback prepSnapshotForDownload(String snapshotId, int sizeExpected, long actualSizeInMB)
      throws EucalyptusCloudException {
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      String deviceName = null;
      LVMVolumeInfo foundSnapshotInfo = volumeManager.getVolumeInfo(snapshotId);
      if (null == foundSnapshotInfo) {
        LVMVolumeInfo snapshotInfo = volumeManager.getVolumeInfo();
        snapshotInfo.setVolumeId(snapshotId);
        snapshotInfo.setSize(sizeExpected);
        snapshotInfo.setLoFileName(DirectStorageInfo.getStorageInfo().getVolumesDir() + File.separator + snapshotId);
        deviceName = snapshotInfo.getLoFileName();
        volumeManager.add(snapshotInfo);
      }

      volumeManager.finish();
      return new StorageResourceWithCallback(new FileResource(snapshotId, deviceName), new Function<StorageResource, String>() {

        @Override
        public String apply(StorageResource arg0) {
          try {
            LOG.debug("Executing callback after prepping for download of " + snapshotId);
            finishVolume(snapshotId);
          } catch (Exception e) {
            LOG.warn("Failed to execute callback after prepping for download of " + snapshotId, e);
            Exceptions.toUndeclared("Failed to execute callback for prepSnapshotForDownload() " + snapshotId, e);
          }
          return null;
        }
      });
      // return DirectStorageInfo.getStorageInfo().getVolumesDir() + File.separator + snapshotId;
    }
  }

  @Override
  public ArrayList<ComponentProperty> getStorageProps() {
    ArrayList<ComponentProperty> componentProperties = null;
    ConfigurableClass configurableClass = StorageInfo.class.getAnnotation(ConfigurableClass.class);
    if (configurableClass != null) {
      String root = configurableClass.root();
      String alias = configurableClass.alias();
      componentProperties = (ArrayList<ComponentProperty>) PropertyDirectory.getComponentPropertySet(StorageProperties.NAME + "." + root, alias);
    }
    configurableClass = DirectStorageInfo.class.getAnnotation(ConfigurableClass.class);
    if (configurableClass != null) {
      String root = configurableClass.root();
      String alias = configurableClass.alias();
      if (componentProperties == null)
        componentProperties = (ArrayList<ComponentProperty>) PropertyDirectory.getComponentPropertySet(StorageProperties.NAME + "." + root, alias);
      else
        componentProperties.addAll(PropertyDirectory.getComponentPropertySet(StorageProperties.NAME + "." + root, alias));
    }
    configurableClass = DASInfo.class.getAnnotation(ConfigurableClass.class);
    if (configurableClass != null) {
      String root = configurableClass.root();
      String alias = configurableClass.alias();
      if (componentProperties == null)
        componentProperties = (ArrayList<ComponentProperty>) PropertyDirectory.getComponentPropertySet(StorageProperties.NAME + "." + root, alias);
      else
        componentProperties.addAll(PropertyDirectory.getComponentPropertySet(StorageProperties.NAME + "." + root, alias));
    }
    return componentProperties;
  }

  @Override
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
  }

  @Override
  public String getStorageRootDirectory() {
    return DirectStorageInfo.getStorageInfo().getVolumesDir();
  }

  @Override
  public void finishVolume(String snapshotId) throws EucalyptusCloudException {
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      LVMVolumeInfo foundSnapshotInfo = volumeManager.getVolumeInfo(snapshotId);
      if (null != foundSnapshotInfo) {
        foundSnapshotInfo.setStatus(StorageProperties.Status.available.toString());
      }
      volumeManager.finish();
    }
  }

  @Override
  public String getSnapshotPath(String snapshotId) throws EucalyptusCloudException {
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      LVMVolumeInfo volInfo = volumeManager.getVolumeInfo(snapshotId);
      if (volInfo != null) {
        String snapPath = volInfo.getLoFileName();
        volumeManager.finish();
        return snapPath;
      } else {
        volumeManager.abort();
        throw new EntityNotFoundException("Unable to find snapshot with id: " + snapshotId);
      }
    }
  }

  @Override
  public String getVolumePath(String volumeId) throws EucalyptusCloudException {
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      LVMVolumeInfo volInfo = volumeManager.getVolumeInfo(volumeId);
      if (volInfo != null) {
        String volumePath = lvmRootDirectory + File.separator + volInfo.getVgName() + File.separator + volInfo.getLvName();
        volumeManager.finish();
        return volumePath;
      } else {
        volumeManager.abort();
        throw new EntityNotFoundException("Unable to find volume with id: " + volumeId);
      }
    }
  }

  @Override
  public void importSnapshot(String snapshotId, String snapPath, String volumeId, int size) throws EucalyptusCloudException {
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      LVMVolumeInfo snapInfo = volumeManager.getVolumeInfo(snapshotId);
      if (snapInfo != null) {
        volumeManager.finish();
        throw new EucalyptusCloudException("Snapshot " + snapshotId + " already exists. Import failed.");
      }
      volumeManager.finish();
      String snapFileName = getStorageRootDirectory() + File.separator + snapshotId;
      SystemUtil
          .run(new String[] {StorageProperties.EUCA_ROOT_WRAPPER, "dd", "if=" + snapPath, "of=" + snapFileName, "bs=" + StorageProperties.blockSize});
      // volumeManager = new VolumeMetadataManager();
      LVMVolumeInfo snapshotInfo = volumeManager.getVolumeInfo();
      snapshotInfo.setVolumeId(snapshotId);
      snapshotInfo.setLoFileName(snapFileName);
      snapshotInfo.setSize(size);
      snapshotInfo.setSnapshotOf(volumeId);
      try (VolumeMetadataManager nestedVolumeManager = new VolumeMetadataManager()) {
        nestedVolumeManager.add(snapshotInfo);
        nestedVolumeManager.finish();
      }
    }
  }

  @Override
  public void importVolume(String volumeId, String volumePath, int size) throws EucalyptusCloudException {
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      LVMVolumeInfo volInfo = volumeManager.getVolumeInfo(volumeId);
      if (volInfo != null) {
        volumeManager.finish();
        throw new EucalyptusCloudException("Volume " + volumeId + " already exists. Import failed.");
      }
      volumeManager.finish();
      createVolume(volumeId, size);
      try (VolumeMetadataManager nestedVolumeManager = new VolumeMetadataManager()) {
        LVMVolumeInfo volumeInfo = nestedVolumeManager.getVolumeInfo(volumeId);
        if (volumeInfo != null) {
          SystemUtil.run(new String[] {StorageProperties.EUCA_ROOT_WRAPPER, "dd", "if=" + volumePath,
              "of=" + lvmRootDirectory + File.separator + volumeInfo.getVgName() + File.separator + volumeInfo.getLvName(),
              "bs=" + StorageProperties.blockSize});
          nestedVolumeManager.finish();
        } else {
          nestedVolumeManager.abort();
          throw new EucalyptusCloudException("Unable to find volume with id: " + volumeId);
        }
      }
    }
  }

  /**
   * NOTE: once exported to one host, it is exported to all in this implementation
   */
  @Override
  public String exportVolume(String volumeId, String nodeIqn) throws EucalyptusCloudException {
    try {
      updateVolumeGroup();
    } catch (EucalyptusCloudException e) {
      LOG.error(e);
      throw e;
    }
    LVMVolumeInfo lvmVolumeInfo = null;
    {
      try (final VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
        lvmVolumeInfo = volumeManager.getVolumeInfo(volumeId);
        volumeManager.finish();
      }
    }

    if (lvmVolumeInfo != null) {
      VolumeOpMonitor monitor = getMonitor(volumeId);
      synchronized (monitor) {
        try (final VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
          try {
            lvmVolumeInfo = volumeManager.getVolumeInfo(volumeId);
            String lvName = lvmVolumeInfo.getLvName();
            if (lvmVolumeInfo.getVgName() == null) {
              lvmVolumeInfo.setVgName(volumeGroup);
            }
            try {
              // export logical volume
              volumeManager.exportVolume(lvmVolumeInfo, volumeGroup, lvName);
            } catch (EucalyptusCloudException ex) {
              LOG.error("Unable to export volume " + volumeId, ex);
              throw ex;
            }
          } catch (Exception ex) {
            LOG.error("Failed to attach volume " + volumeId, ex);
            throw new EucalyptusCloudException("Failed to attach volume " + volumeId, ex);
          } finally {
            try {
              volumeManager.finish();
            } catch (Exception e) {
              LOG.error("Unable to commit the database transaction after an attempt to attach volume " + volumeId, e);
            }
          }
        }
      } // synchronized
    }
    // Return the connection string.
    return getVolumeConnectionString(volumeId);
  }

  /**
   * This implementation does not do export/unexport from single hosts, only all or none. Caller should be keeping track of exports and
   * call @unexportVolumeFromAll when appropriate
   */
  @Override
  public void unexportVolume(String volumeId, String nodeIqn) throws EucalyptusCloudException, UnsupportedOperationException {
    throw new UnsupportedOperationException("DASManager does not support node-specific export/unexport");
  }

  @Override
  public void unexportVolumeFromAll(String volumeId) throws EucalyptusCloudException {
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      try {
        LVMVolumeInfo foundLVMVolumeInfo = volumeManager.getVolumeInfo(volumeId);
        if (foundLVMVolumeInfo != null) {
          // LOG.info("Marking volume: " + volumeId + " for cleanup");
          // foundLVMVolumeInfo.setCleanup(true);
          LOG.info("Unexporting volume " + volumeId + " from all clients");
          VolumeOpMonitor monitor = getMonitor(volumeId);
          synchronized (monitor) {
            try {
              LOG.info("Unexporting volume " + foundLVMVolumeInfo.getVolumeId());
              try {
                String lvPath = foundLVMVolumeInfo.getAbsoluteLVPath();
                if (lvPath != null && LVMWrapper.logicalVolumeExists(lvPath)) {
                  // guard this. tgt is not happy when you ask it to
                  // get rid of a non existent tid
                  exportManager.cleanup(foundLVMVolumeInfo);
                }
                foundLVMVolumeInfo.setStatus("available");
                LOG.info("Done cleaning up: " + foundLVMVolumeInfo.getVolumeId());
              } catch (EucalyptusCloudException ee) {
                LOG.error("Failed to unexport from all hosts for " + volumeId, ee);
                throw ee;
              }
            } finally {
              monitor.notifyAll();
            }
          } // synchronized
        } else {
          volumeManager.abort();
          throw new EucalyptusCloudException("Unable to find volume: " + volumeId);
        }
      } catch (Exception e) {
        LOG.error("Failed to unexport volume " + volumeId);
        throw new EucalyptusCloudException("Failed to unexport volume " + volumeId);
      } finally {
        volumeManager.finish();
      }
    }
  }

  @Override
  public void checkReady() throws EucalyptusCloudException {
    // check if binaries exist, commands can be executed, etc.
    if (!new File(StorageProperties.EUCA_ROOT_WRAPPER).exists()) {
      throw new EucalyptusCloudException("root wrapper (euca_rootwrap) does not exist in " + StorageProperties.EUCA_ROOT_WRAPPER);
    }
    File varDir = new File(EUCA_VAR_RUN_PATH);
    if (!varDir.exists()) {
      varDir.mkdirs();
    }
    exportManager.check();
  }

  @Override
  public void stop() throws EucalyptusCloudException {
    exportManager.stop();
  }

  @Override
  public void disable() throws EucalyptusCloudException {
    volumeOps.clear();
    volumeOps = null;
  }

  @Override
  public void enable() throws EucalyptusCloudException {
    volumeOps = new ConcurrentHashMap<String, VolumeOpMonitor>();
  }

  @Override
  public boolean getFromBackend(String snapshotId, int size) throws EucalyptusCloudException {
    return false;
  }

  @Override
  public void checkVolume(String volumeId) throws EucalyptusCloudException {}

  @Override
  public List<CheckerTask> getCheckers() {
    List<CheckerTask> checkers = new ArrayList<CheckerTask>();
    // Volume cleanup is now synchronous, no need for background tasks
    return checkers;
  }

  @Override
  public String createSnapshotPoint(String volumeId, String snapshotId) throws EucalyptusCloudException {
    return null;
  }

  @Override
  public void deleteSnapshotPoint(String volumeId, String snapshotId, String snapshotPointId) throws EucalyptusCloudException {
    throw new EucalyptusCloudException("Synchronous snapshot points not supported in DAS storage manager");
  }

  protected class VolumeOpMonitor {
    public VolumeOpMonitor() {};
  }

  protected VolumeOpMonitor getMonitor(String key) {
    VolumeOpMonitor monitor = volumeOps.putIfAbsent(key, new VolumeOpMonitor());
    if (monitor == null) {
      monitor = volumeOps.get(key);
    }
    return monitor;
  }

  public void removeMonitor(String key) {
    volumeOps.remove(key);
  }

  @Override
  public boolean supportsIncrementalSnapshots() throws EucalyptusCloudException {
    return false;
  }

  @Override
  public StorageResourceWithCallback prepIncrementalSnapshotForUpload(String volumeId, String snapshotId, String snapPointId, String prevSnapshotId,
      String prevSnapPointId) throws EucalyptusCloudException {
    // TODO may be throw unsupported exception?
    return null;
  }

  @Override
  public StorageResource prepSnapshotForUpload(String volumeId, String snapshotId, String snapPointId) throws EucalyptusCloudException {
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      LVMVolumeInfo snapshotInfo = volumeManager.getVolumeInfo(snapshotId);
      volumeManager.finish();
      return new FileResource(snapshotId, snapshotInfo.getLoFileName());
    } catch (Exception e) {
      LOG.warn("Failed to lookup lo file name for snapshot " + snapshotId, e);
      throw new EucalyptusCloudException("Failed to lookup lo file name for snapshot " + snapshotId, e);
    }
  }

  @Override
  public StorageResourceWithCallback prepSnapshotBaseForRestore(String snapshotId, int size, String snapshotPointId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <F, T> T executeCallback(Function<F, T> callback, F input) throws EucalyptusCloudException {
    try {
      return callback.apply(input);
    } catch (Throwable t) {
      throw new EucalyptusCloudException("Unable to execute callback for due to", t);
    }
  }

  @Override
  public void restoreSnapshotDelta(String currentSnapId, String prevSnapId, String baseId, StorageResource sr) throws EucalyptusCloudException {
    // TODO Auto-generated method stub
  }

  @Override
  public void completeSnapshotRestorationFromDeltas(String snapshotId) throws EucalyptusCloudException {
    // TODO Auto-generated method stub
  }
}
