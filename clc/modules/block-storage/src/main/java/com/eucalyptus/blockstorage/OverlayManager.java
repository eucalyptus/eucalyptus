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

package com.eucalyptus.blockstorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityNotFoundException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.StorageManagers.StorageManagerProperty;
import com.eucalyptus.blockstorage.entities.DirectStorageInfo;
import com.eucalyptus.blockstorage.entities.ISCSIVolumeInfo;
import com.eucalyptus.blockstorage.entities.LVMVolumeInfo;
import com.eucalyptus.blockstorage.entities.StorageInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.storage.common.CheckerTask;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.base.Joiner;

import edu.ucsb.eucalyptus.msgs.ComponentProperty;
import edu.ucsb.eucalyptus.util.StreamConsumer;
import edu.ucsb.eucalyptus.util.SystemUtil;
import edu.ucsb.eucalyptus.util.SystemUtil.CommandOutput;

@StorageManagerProperty("overlay")
public class OverlayManager extends DASManager {
  private static Logger LOG = Logger.getLogger(OverlayManager.class);

  private static final Joiner JOINER = Joiner.on(" ").skipNulls();

  @Override
  public void checkPreconditions() throws EucalyptusCloudException {
    // check if binaries exist, commands can be executed, etc.
    if (!new File(StorageProperties.EUCA_ROOT_WRAPPER).exists()) {
      throw new EucalyptusCloudException("root wrapper (euca_rootwrap) does not exist in " + StorageProperties.EUCA_ROOT_WRAPPER);
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
      exportManager.checkPreconditions();
    } catch (EucalyptusCloudException ex) {
      String error = "Unable to run command: " + ex.getMessage();
      LOG.error(error);
      throw new EucalyptusCloudException(error);
    }
  }

  private String findFreeLoopback() throws EucalyptusCloudException {
    return SystemUtil.run(new String[] {StorageProperties.EUCA_ROOT_WRAPPER, "losetup", "-f"}).replaceAll("\n", "");
  }

  private String getLoopback(String loDevName) throws EucalyptusCloudException {
    return SystemUtil.run(new String[] {StorageProperties.EUCA_ROOT_WRAPPER, "losetup", loDevName});
  }

  // losetup -d fails a LOT of times, added retries.
  // losetup -d does not return any output, so there's no way to detect if the command was successful with previous logic
  // losetup return code is not reliable, introduced check on the error stream and loop device
  private String removeLoopback(String loDevName) throws EucalyptusCloudException {
    int retryCount = 0;
    do {
      try {
        String[] command = new String[] {StorageProperties.EUCA_ROOT_WRAPPER, "losetup", "-d", loDevName};
        CommandOutput resultObj1 = SystemUtil.runWithRawOutput(command);
        LOG.debug("Executed: " + JOINER.join(command) + "\n return=" + resultObj1.returnValue + "\n stdout=" + resultObj1.output + "\n stderr="
            + resultObj1.error);

        command = new String[] {StorageProperties.EUCA_ROOT_WRAPPER, "losetup", loDevName};
        CommandOutput resultObj2 = SystemUtil.runWithRawOutput(command);
        LOG.debug("Executed: " + JOINER.join(command) + "\n return=" + resultObj2.returnValue + "\n stdout=" + resultObj2.output + "\n stderr="
            + resultObj2.error);

        if (StringUtils.isNotBlank(resultObj2.output)) {
          LOG.debug("Unable to disconnect the loop device at: " + loDevName);
          Thread.sleep(1000);
        } else {
          LOG.debug("Detached loop device: " + loDevName);
          return "";
        }
      } catch (Exception e) {
        LOG.error("Error removing loop device", e);
      }
      retryCount++;
    } while (retryCount < 3);

    LOG.error("All attempts to remove loop device " + loDevName + " failed.");
    return "";
  }

  private boolean volumeGroupExists(String vgName) {
    boolean success = false;
    String returnValue = SystemUtil.run(new String[] {StorageProperties.EUCA_ROOT_WRAPPER, "vgdisplay", vgName});
    if (returnValue.length() > 0) {
      success = true;
    }
    return success;
  }

  private boolean physicalVolumeExists(String pvName) {
    boolean success = false;
    String returnValue = SystemUtil.run(new String[] {StorageProperties.EUCA_ROOT_WRAPPER, "pvdisplay", pvName});
    if (returnValue.length() > 0) {
      success = true;
    }
    return success;
  }

  private int losetup(String absoluteFileName, String loDevName) {
    try {
      Runtime rt = Runtime.getRuntime();
      Process proc = rt.exec(new String[] {StorageProperties.EUCA_ROOT_WRAPPER, "losetup", loDevName, absoluteFileName});
      StreamConsumer error = new StreamConsumer(proc.getErrorStream());
      StreamConsumer output = new StreamConsumer(proc.getInputStream());
      error.start();
      output.start();
      int errorCode = proc.waitFor();
      output.join();
      LOG.info("Finished executing: losetup " + loDevName + " " + absoluteFileName);
      LOG.info("Result of: losetup " + loDevName + " " + absoluteFileName + " stdout: " + output.getReturnValue());
      LOG.info("Result of: losetup" + loDevName + " " + absoluteFileName + " return value: " + error.getReturnValue());
      return errorCode;
    } catch (Exception t) {
      LOG.error(t);
    }
    return -1;
  }

  @Override
  public void initialize() throws EucalyptusCloudException {
    File storageRootDir = new File(getStorageRootDirectory());
    if (!storageRootDir.exists()) {
      if (!storageRootDir.mkdirs()) {
        throw new EucalyptusCloudException("Unable to make volume root directory: " + getStorageRootDirectory());
      }
    }
    // The following should be executed only once during the entire lifetime of the VM.
    if (!initialized) {
      // System.loadLibrary("lvm2control");
      // registerSignals();
      initialized = true;
    }
  }

  @Override
  public void configure() throws EucalyptusCloudException {
    exportManager.configure();
    // First call to StorageInfo.getStorageInfo will add entity if it does not exist
    LOG.info("" + StorageInfo.getStorageInfo().getName());
    checkVolumesDir();
  }

  private void checkVolumesDir() {
    String volumesDir = DirectStorageInfo.getStorageInfo().getVolumesDir();
    File volumes = new File(volumesDir);
    if (!volumes.exists()) {
      if (!volumes.mkdirs()) {
        LOG.fatal("Unable to make volume root directory: " + volumesDir);
      }
    } else if (!volumes.canWrite()) {
      LOG.fatal("Cannot write to volume root directory: " + volumesDir);
    }
    try {
      SystemUtil.setEucaReadWriteOnly(volumesDir);
    } catch (EucalyptusCloudException ex) {
      LOG.fatal(ex);
    }
  }

  @Override
  public void cleanVolume(String volumeId) {
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      LVMVolumeInfo lvmVolInfo = volumeManager.getVolumeInfo(volumeId);
      if (lvmVolInfo != null) {
        String loDevName = lvmVolInfo.getLoDevName();
        volumeManager.unexportVolume(lvmVolInfo);
        String vgName = lvmVolInfo.getVgName();
        String lvName = lvmVolInfo.getLvName();
        String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;

        try {
          String returnValue = LVMWrapper.removeLogicalVolume(absoluteLVName);
          returnValue = LVMWrapper.removeVolumeGroup(vgName);
          returnValue = LVMWrapper.removePhysicalVolume(loDevName);
          removeLoopback(loDevName);
          lvmVolInfo.setLoDevName(null);
        } catch (EucalyptusCloudException ex) {
          // volumeManager.abort();
          String error = "Unable to run command: " + ex.getMessage();
          LOG.error(error);
        }
        // Always delete the metadata regardless of the actual clean up
        volumeManager.remove(lvmVolInfo);
        File volFile = new File(DirectStorageInfo.getStorageInfo().getVolumesDir() + File.separator + lvmVolInfo.getVolumeId());
        if (volFile.exists()) {
          if (!volFile.delete()) {
            LOG.error("Unable to delete: " + volFile.getAbsolutePath() + " for failed volume");
          }
        }
      }
      volumeManager.finish();
    }
  }

  @Override
  public void cleanSnapshot(String snapshotId, String snapshotPointId) {
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      LVMVolumeInfo lvmVolInfo = volumeManager.getVolumeInfo(snapshotId);
      if (lvmVolInfo != null) {
        volumeManager.remove(lvmVolInfo);
        File volFile = new File(DirectStorageInfo.getStorageInfo().getVolumesDir() + File.separator + lvmVolInfo.getVolumeId());
        if (volFile.exists()) {
          if (!volFile.delete()) {
            LOG.error("Unable to delete: " + volFile.getAbsolutePath() + " for failed snapshot");
          }
        }
      }
      volumeManager.finish();
    }
  }

  // NOTE: Overlay, like DASManager does not support host-specific detach.

  /**
   * Do the unexport synchronously
   * 
   * @param volumeId
   * @throws EucalyptusCloudException
   */
  @Override
  public void unexportVolumeFromAll(String volumeId) throws EucalyptusCloudException {
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      LVMVolumeInfo volumeInfo = volumeManager.getVolumeInfo(volumeId);
      if (volumeInfo == null) {
        volumeManager.abort();
        LOG.error("Cannot unexport volume for all hosts because volume " + volumeId + " not found in db");
        throw new EucalyptusCloudException("Volume " + volumeId + " not found");
      } else {
        LOG.debug("Unexporting volume " + volumeId);
        try {
          doUnexport(volumeId);
        } catch (EucalyptusCloudException e) {
          LOG.error("Unexport failed for volume " + volumeId);
          throw e;
        } finally {
          volumeManager.finish();
        }
      }
    }
  }

  @Override
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

  public String createDuplicateLoopback(String oldRawFileName, String rawFileName) throws EucalyptusCloudException {
    dupFile(oldRawFileName, rawFileName);
    return createLoopback(rawFileName);
  }

  public String createLoopback(String fileName, int size) throws EucalyptusCloudException {
    createEmptyFile(fileName, size);
    if (!(new File(fileName).exists()))
      throw new EucalyptusCloudException("Unable to create file " + fileName);
    return createLoopback(fileName);
  }

  public synchronized String createLoopback(String fileName) throws EucalyptusCloudException {
    int number_of_retries = 0;
    int status = -1;
    String loDevName;
    do {
      loDevName = findFreeLoopback();
      if (loDevName.length() > 0) {
        status = losetup(fileName, loDevName);
      }
      if (number_of_retries++ >= MAX_LOOP_DEVICES)
        break;
    } while (status != 0);

    if (status != 0) {
      throw new EucalyptusCloudException("Could not create loopback device for " + fileName + ". Please check the max loop value and permissions");
    }
    return loDevName;
  }

  public int createLoopback(String absoluteFileName, String loDevName) {
    return losetup(absoluteFileName, loDevName);
  }

  public String createLoopback(String fileName, long size) throws EucalyptusCloudException {
    createAbsoluteEmptyFile(fileName, size);
    if (!(new File(fileName).exists()))
      throw new EucalyptusCloudException("Unable to create file " + fileName);
    return createLoopback(fileName);
  }

  public void createEmptyFile(String fileName, long size) throws EucalyptusCloudException {
    createAbsoluteEmptyFile(fileName, size);
    if (!(new File(fileName).exists()))
      throw new EucalyptusCloudException("Unable to create file " + fileName);
  }

  // creates a logical volume (and a new physical volume and volume group)
  public void createLogicalVolume(String volumeId, String loDevName, String vgName, String lvName) throws EucalyptusCloudException {
    String returnValue = LVMWrapper.createPhysicalVolume(loDevName);
    if (returnValue.length() == 0) {
      throw new EucalyptusCloudException("Unable to create physical volume for " + loDevName);
    }
    returnValue = LVMWrapper.createVolumeGroup(loDevName, vgName);
    if (returnValue.length() == 0) {
      throw new EucalyptusCloudException("Unable to create volume group " + vgName + " for " + loDevName);
    }
    returnValue = LVMWrapper.createLogicalVolume(volumeId, vgName, lvName);
    if (returnValue.length() == 0) {
      throw new EucalyptusCloudException("Unable to create logical volume " + lvName + " in volume group " + vgName);
    }
  }

  public void createSnapshotLogicalVolume(String loDevName, String vgName, String lvName, String snapLvName) throws EucalyptusCloudException {
    String returnValue = LVMWrapper.createPhysicalVolume(loDevName);
    if (returnValue.length() == 0) {
      throw new EucalyptusCloudException("Unable to create physical volume for " + loDevName);
    }
    returnValue = LVMWrapper.extendVolumeGroup(loDevName, vgName);
    if (returnValue.length() == 0) {
      throw new EucalyptusCloudException("Unable to extend volume group " + vgName + " for " + loDevName);
    }
    returnValue = LVMWrapper.createSnapshotLogicalVolume(lvName, snapLvName);
    if (returnValue.length() == 0) {
      throw new EucalyptusCloudException("Unable to create snapshot logical volume " + snapLvName + " for volume " + lvName);
    }
  }

  @Override
  public void createVolume(String volumeId, int size) throws EucalyptusCloudException {
    LVMVolumeInfo lvmVolumeInfo = new ISCSIVolumeInfo();

    String rawFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + volumeId;
    // create file and attach to loopback device
    long absoluteSize = size * StorageProperties.GB + LVM_HEADER_LENGTH;
    try {
      // set up LVM
      String vgName = generateVGName(volumeId);
      String lvName = generateLVName(volumeId);
      // create file and attach to loopback device
      String loDevName = createLoopback(rawFileName, absoluteSize);
      lvmVolumeInfo.setVolumeId(volumeId);
      lvmVolumeInfo.setLoDevName(loDevName);

      // create physical volume, volume group and logical volume
      createLogicalVolume(volumeId, loDevName, vgName, lvName);
      lvmVolumeInfo.setVgName(vgName);
      lvmVolumeInfo.setLvName(lvName);

      lvmVolumeInfo.setStatus(StorageProperties.Status.available.toString());
      lvmVolumeInfo.setSize(size);
      // tear down
      String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;
      LVMWrapper.disableLogicalVolume(absoluteLVName);
      removeLoopback(loDevName);
      lvmVolumeInfo.setLoDevName(null);
      try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
        volumeManager.add(lvmVolumeInfo);
        volumeManager.finish();
      }
    } catch (EucalyptusCloudException ex) {
      String error = "Unable to run command: " + ex.getMessage();
      // zhill: should always commit what we have thus far
      // volumeManager.abort();
      LOG.error(error);
      throw new EucalyptusCloudException(error);
    }
  }

  @Override
  public int createVolume(String volumeId, String snapshotId, int size) throws EucalyptusCloudException {
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      LVMVolumeInfo foundSnapshotInfo = volumeManager.getVolumeInfo(snapshotId);
      if (foundSnapshotInfo != null) {
        String status = foundSnapshotInfo.getStatus();
        if (status.equals(StorageProperties.Status.available.toString())) {
          String vgName = generateVGName(volumeId);
          String lvName = generateLVName(volumeId);
          String loFileName = foundSnapshotInfo.getLoFileName();
          String snapId = foundSnapshotInfo.getVolumeId();
          LVMVolumeInfo lvmVolumeInfo = volumeManager.getVolumeInfo();
          volumeManager.finish();
          try {
            String rawFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + volumeId;
            // create file and attach to loopback device
            File snapshotFile = new File(DirectStorageInfo.getStorageInfo().getVolumesDir() + PATH_SEPARATOR + snapId);
            assert (snapshotFile.exists());
            long absoluteSize;
            if (size <= 0 || size == foundSnapshotInfo.getSize()) {
              size = (int) (snapshotFile.length() / StorageProperties.GB);
              absoluteSize = snapshotFile.length() + LVM_HEADER_LENGTH;
            } else {
              absoluteSize = size * StorageProperties.GB + LVM_HEADER_LENGTH;
            }

            String loDevName = createLoopback(rawFileName, absoluteSize);
            lvmVolumeInfo.setVolumeId(volumeId);
            lvmVolumeInfo.setLoDevName(loDevName);

            // create physical volume, volume group and logical volume
            createLogicalVolume(volumeId, loDevName, vgName, lvName);
            // duplicate snapshot volume
            String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;
            duplicateLogicalVolume(loFileName, absoluteLVName);

            lvmVolumeInfo.setVgName(vgName);
            lvmVolumeInfo.setLvName(lvName);
            lvmVolumeInfo.setStatus(StorageProperties.Status.available.toString());
            lvmVolumeInfo.setSize(size);
            // tear down
            LVMWrapper.disableLogicalVolume(absoluteLVName);
            removeLoopback(loDevName);
            lvmVolumeInfo.setLoDevName(null);
            try (VolumeMetadataManager nestedVolumeManager = new VolumeMetadataManager()) {
              nestedVolumeManager.add(lvmVolumeInfo);
              nestedVolumeManager.finish();
            }
          } catch (EucalyptusCloudException ex) {
            // zhill: always commit what we have, so as not to orphan resources. This allows cleanup to properly
            // clean local resources.
            // volumeManager.abort();
            String error = "Unable to run command: " + ex.getMessage();
            LOG.error(error);
            throw new EucalyptusCloudException(error);
          }
        }
      } else {
        throw new EucalyptusCloudException("Unable to find snapshot: " + snapshotId);
      }
    }
    return size;
  }

  @Override
  public void cloneVolume(String volumeId, String parentVolumeId) throws EucalyptusCloudException {
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      LVMVolumeInfo foundVolumeInfo = volumeManager.getVolumeInfo(parentVolumeId);
      if (foundVolumeInfo != null) {
        String vgName = generateVGName(volumeId);
        String lvName = generateLVName(volumeId);
        String parentVgName = foundVolumeInfo.getVgName();
        String parentLvName = foundVolumeInfo.getLvName();
        LVMVolumeInfo lvmVolumeInfo = volumeManager.getVolumeInfo();
        int size = foundVolumeInfo.getSize();
        volumeManager.finish();
        try {
          String rawFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + volumeId;
          // create file and attach to loopback device
          File parentVolumeFile = new File(DirectStorageInfo.getStorageInfo().getVolumesDir() + PATH_SEPARATOR + parentVolumeId);
          assert (parentVolumeFile.exists());
          long absoluteSize = parentVolumeFile.length();

          String loDevName = createLoopback(rawFileName, absoluteSize);
          lvmVolumeInfo.setLoDevName(loDevName);
          // create physical volume, volume group and logical volume
          createLogicalVolume(volumeId, loDevName, vgName, lvName);
          // duplicate snapshot volume
          String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;
          String absoluteParentLVName = lvmRootDirectory + PATH_SEPARATOR + parentVgName + PATH_SEPARATOR + parentLvName;
          duplicateLogicalVolume(absoluteParentLVName, absoluteLVName);
          // export logical volume
          try {
            volumeManager.exportVolume(lvmVolumeInfo, vgName, lvName);
          } catch (EucalyptusCloudException ex) {
            String returnValue = LVMWrapper.removeLogicalVolume(absoluteLVName);
            returnValue = LVMWrapper.removeVolumeGroup(vgName);
            returnValue = LVMWrapper.removePhysicalVolume(loDevName);
            removeLoopback(loDevName);
            lvmVolumeInfo.setLoDevName(null);
            throw ex;
          }
          lvmVolumeInfo.setVolumeId(volumeId);
          lvmVolumeInfo.setPvName(loDevName);
          lvmVolumeInfo.setVgName(vgName);
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
    LVMVolumeInfo foundLVMVolumeInfo = null;
    {
      try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
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
              LOG.debug("Deleting volume " + volumeId);
              File volFile = new File(DirectStorageInfo.getStorageInfo().getVolumesDir() + File.separator + volumeId);
              if (volFile.exists()) {
                if (!volFile.delete()) {
                  LOG.error("Unable to delete: " + volFile.getAbsolutePath());
                  throw new EucalyptusCloudException("Unable to delete volume file: " + volFile.getAbsolutePath());
                }
              }

              volumeManager.remove(volumeManager.getVolumeInfo(volumeId));
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

  /* LVM is flaky when there are a large number of concurrent removal requests. This workaround serializes lvm cleanup */
  private synchronized void deleteLogicalVolume(String loDevName, String vgName, String absoluteLVName)
      throws EucalyptusCloudException, EucalyptusCloudException {
    if (LVMWrapper.logicalVolumeExists(absoluteLVName)) {
      String returnValue = LVMWrapper.removeLogicalVolume(absoluteLVName);
      if (returnValue.length() == 0) {
        throw new EucalyptusCloudException("Unable to remove logical volume " + absoluteLVName + " " + returnValue);
      }
    }
    if (volumeGroupExists(vgName)) {
      String returnValue = LVMWrapper.removeVolumeGroup(vgName);
      if (returnValue.length() == 0) {
        throw new EucalyptusCloudException("Unable to remove volume group " + vgName + " " + returnValue);
      }
    }
    if (physicalVolumeExists(loDevName)) {
      String returnValue = LVMWrapper.removePhysicalVolume(loDevName);
      if (returnValue.length() == 0) {
        throw new EucalyptusCloudException("Unable to remove physical volume " + loDevName + " " + returnValue);
      }
    }
  }

  @Override
  public void createSnapshot(String volumeId, String snapshotId, String snapshotPointId) throws EucalyptusCloudException {
    if (snapshotPointId != null) {
      throw new EucalyptusCloudException("Synchronous snapshot points not supported in Overlay storage manager");
    }

    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      LVMVolumeInfo foundLVMVolumeInfo = volumeManager.getVolumeInfo(volumeId);
      // StorageResource snapInfo = null;
      if (foundLVMVolumeInfo != null) {
        LVMVolumeInfo snapshotInfo = volumeManager.getVolumeInfo();
        snapshotInfo.setVolumeId(snapshotId);
        String vgName = foundLVMVolumeInfo.getVgName();
        String lvName = generateLVName(snapshotId);
        String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + foundLVMVolumeInfo.getLvName();

        int size = foundLVMVolumeInfo.getSize();
        long snapshotSize = (size * StorageProperties.GB) / 2;
        String rawFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + volumeId + Crypto.getRandom(6);
        // create file and attach to loopback device
        volumeManager.finish();
        try {
          // mount volume and loopback and enable
          VolumeOpMonitor monitor = getMonitor(volumeId);
          String absoluteVolLVName =
              lvmRootDirectory + PATH_SEPARATOR + foundLVMVolumeInfo.getVgName() + PATH_SEPARATOR + foundLVMVolumeInfo.getLvName();
          String volLoDevName = foundLVMVolumeInfo.getLoDevName();
          boolean tearDown = false;
          synchronized (monitor) {
            if (!LVMWrapper.logicalVolumeExists(absoluteVolLVName)) {
              volLoDevName = createLoopback(DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + volumeId);
              // enable logical volume
              int enablementReturnCode = 0;
              try {
                enablementReturnCode = enableLogicalVolume(absoluteLVName);
              } catch (EucalyptusCloudException ex) {
                String error = "Failed to enable logical volume " + absoluteLVName + " for snapshot:" + ex.getMessage();
                LOG.error(error);
                throw new EucalyptusCloudException(ex);
              }
              if (enablementReturnCode != 0) {
                throw new EucalyptusCloudException("Failed to enable logical volume " + absoluteLVName + 
                    "for snapshot, return code: " + enablementReturnCode);
              }
              tearDown = true;
            }

            snapshotInfo.setStatus(StorageProperties.Status.pending.toString());
            snapshotInfo.setSize(size);
            snapshotInfo.setSnapshotOf(volumeId);
            try (VolumeMetadataManager nestedVolumeManager = new VolumeMetadataManager()) {
              nestedVolumeManager.add(snapshotInfo);
              nestedVolumeManager.finish();
            }

            String loDevName = createLoopback(rawFileName, snapshotSize);

            // create physical volume, volume group and logical volume
            createSnapshotLogicalVolume(loDevName, vgName, absoluteLVName, lvName);

            String snapRawFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + snapshotId;
            String absoluteSnapLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;

            duplicateLogicalVolume(absoluteSnapLVName, snapRawFileName);

            String returnValue = LVMWrapper.removeLogicalVolume(absoluteSnapLVName);
            if (returnValue.length() == 0) {
              throw new EucalyptusCloudException("Unable to remove logical volume " + absoluteSnapLVName);
            }
            returnValue = LVMWrapper.reduceVolumeGroup(vgName, loDevName);
            if (returnValue.length() == 0) {
              throw new EucalyptusCloudException("Unable to reduce volume group " + vgName + " logical volume: " + loDevName);
            }
            returnValue = LVMWrapper.removePhysicalVolume(loDevName);
            if (returnValue.length() == 0) {
              throw new EucalyptusCloudException("Unable to remove physical volume " + loDevName);
            }
            returnValue = removeLoopback(loDevName);
            if (!(new File(rawFileName)).delete()) {
              LOG.error("Unable to remove temporary snapshot file: " + rawFileName);
            }
            // tear down volume

            if (tearDown) {
              LOG.info("Snapshot complete. Detaching loop device" + volLoDevName);
              LVMWrapper.disableLogicalVolume(absoluteVolLVName);
              removeLoopback(volLoDevName);
            }
            try (VolumeMetadataManager nestedVolumeManager = new VolumeMetadataManager()) {
              LVMVolumeInfo foundSnapshotInfo = nestedVolumeManager.getVolumeInfo(snapshotId);
              foundSnapshotInfo.setLoFileName(snapRawFileName);
              foundSnapshotInfo.setStatus(StorageProperties.Status.available.toString());
              nestedVolumeManager.finish();
            }
          } // synchronized
        } catch (EucalyptusCloudException ex) {
          String error = "Unable to run command: " + ex.getMessage();
          LOG.error(error);
          throw new EucalyptusCloudException(error);
        }
      }
    }
  }

  @Override
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

  public void loadSnapshots(List<String> snapshotSet, List<String> snapshotFileNames) throws EucalyptusCloudException {
    assert (snapshotSet.size() == snapshotFileNames.size());
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      int i = 0;
      for (String snapshotFileName : snapshotFileNames) {
        try {
          String loDevName = createLoopback(snapshotFileName);
          LVMVolumeInfo lvmVolumeInfo = volumeManager.getVolumeInfo();
          lvmVolumeInfo.setVolumeId(snapshotSet.get(i++));
          lvmVolumeInfo.setLoDevName(loDevName);
          lvmVolumeInfo.setStatus(StorageProperties.Status.available.toString());
          volumeManager.add(lvmVolumeInfo);
        } catch (EucalyptusCloudException ex) {
          volumeManager.abort();
          String error = "Unable to run command: " + ex.getMessage();
          LOG.error(error);
          throw new EucalyptusCloudException(error);
        }

      }
      volumeManager.finish();
    }
  }

  /**
   * Called on service start to load and export any volumes that should be available to clients immediately. Bases that decision on the lodevName
   * field in the DB.
   * 
   */
  public void reload() {
    LOG.info("Initiating SC Reload of iSCSI targets");
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      List<LVMVolumeInfo> volumeInfos = volumeManager.getAllVolumeInfos();
      LOG.info("SC Reload found " + volumeInfos.size() + " volumes in the DB");

      // Ensure that all loopbacks are properly setup.
      for (LVMVolumeInfo foundVolumeInfo : volumeInfos) {
        String loDevName = foundVolumeInfo.getLoDevName();
        if (loDevName != null) {
          String loFileName = foundVolumeInfo.getVolumeId();
          LOG.info("SC Reload: found volume " + loFileName + " was exported at last shutdown. Ensuring export restored");
          String absoluteLoFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + PATH_SEPARATOR + loFileName;
          if (!new File(absoluteLoFileName).exists()) {
            LOG.error("SC Reload: Backing volume: " + absoluteLoFileName + " not found. Invalidating volume.");
            foundVolumeInfo.setStatus(StorageProperties.Status.failed.toString());
            continue;
          }
          try {
            // Ensure the loopback isn't used
            String returnValue = getLoopback(loDevName);
            if (returnValue.length() <= 0) {
              LOG.info(
                  "SC Reload: volume " + loFileName + " previously used loopback " + loDevName + ". No conflict detected, reusing same loopback");
              createLoopback(absoluteLoFileName, loDevName);
            } else {
              if (!returnValue.contains(loFileName)) {
                // Use a new loopback since the old one is used by something else
                String newLoDev = createLoopback(absoluteLoFileName);
                foundVolumeInfo.setLoDevName(newLoDev);
                LOG.info("SC Reload: volume " + loFileName + " previously used loopback " + loDevName
                    + ", but loopback already in used by something else. Using new loopback: " + newLoDev);
              } else {
                LOG.info("SC Reload: Detection of loopback for volume " + loFileName + " got " + returnValue
                    + ". Appears that loopback is already in-place. No losetup needed for this volume.");
              }
            }
          } catch (EucalyptusCloudException ex) {
            String error = "Unable to run command: " + ex.getMessage();
            LOG.error(error);
          }
        }
      }

      // now enable them
      try {
        LOG.info("SC Reload: Scanning volume groups. This might take a little while...");
        LVMWrapper.scanVolumeGroups();
      } catch (EucalyptusCloudException e) {
        LOG.error(e);
      }

      // Export volumes
      LOG.info("SC Reload: ensuring configured volumes are exported via iSCSI targets");
      for (LVMVolumeInfo foundVolumeInfo : volumeInfos) {
        try {
          // Only try to export volumes that have both a lodev and a vgname
          if (foundVolumeInfo.getLoDevName() != null && foundVolumeInfo.getVgName() != null) {
            LOG.info("SC Reload: exporting " + foundVolumeInfo.getVolumeId() + " in VG: " + foundVolumeInfo.getVgName());
            volumeManager.exportVolume(foundVolumeInfo);
          } else {
            LOG.info("SC Reload: no loopback configured for " + foundVolumeInfo.getVolumeId() + ". Skipping export for this volume.");
          }
        } catch (EucalyptusCloudException ex) {
          LOG.error("SC Reload: Unable to reload volume: " + foundVolumeInfo.getVolumeId() + ex.getMessage());
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
  public StorageResourceWithCallback prepSnapshotForDownload(String snapshotId, int sizeExpected, long actualSizeInMB)
      throws EucalyptusCloudException {
    String deviceName = null;
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      LVMVolumeInfo foundSnapshotInfo = volumeManager.getVolumeInfo(snapshotId);
      if (null == foundSnapshotInfo) {
        LVMVolumeInfo snapshotInfo = volumeManager.getVolumeInfo();
        snapshotInfo.setStatus(StorageProperties.Status.pending.toString());
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
            finishVolume(snapshotId);
          } catch (Exception e) {
            Exceptions.toException("Failed to execute callback for prepSnapshotForDownload() " + snapshotId, e);
          }
          return null;
        }
      });
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
    checkVolumesDir();
  }

  @Override
  public String getStorageRootDirectory() {
    return DirectStorageInfo.getStorageInfo().getVolumesDir();
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
  public void importSnapshot(String snapshotId, String volumeId, String snapPath, int size) throws EucalyptusCloudException {
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
  public String exportVolume(String volumeId, String nodeIqn) throws EucalyptusCloudException {
    LVMVolumeInfo lvmVolumeInfo = null;
    {
      try (final VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
        lvmVolumeInfo = volumeManager.getVolumeInfo(volumeId);
        volumeManager.finish();
      }
    }

    if (lvmVolumeInfo != null) {
      // create file and attach to loopback device
      String rawFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + volumeId;

      VolumeOpMonitor monitor = getMonitor(volumeId);
      synchronized (monitor) {
        try (final VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
          try {
            lvmVolumeInfo = volumeManager.getVolumeInfo(volumeId);

            String loDevName = lvmVolumeInfo.getLoDevName();
            if (loDevName == null) {
              try {
                loDevName = createLoopback(rawFileName);
                lvmVolumeInfo.setLoDevName(loDevName);
              } catch (EucalyptusCloudException ex) {
                LOG.error("Unable to create loop back device for " + volumeId, ex);
                throw ex;
              }
            }
            // TODO: check that loDevName refers to extant loopback... if not, re-assign and create.

            String vgName = lvmVolumeInfo.getVgName();
            String lvName = lvmVolumeInfo.getLvName();
            String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;

            // enable logical volume
            int enablementReturnCode = 0;
            try {
              enablementReturnCode = enableLogicalVolume(absoluteLVName);
            } catch (EucalyptusCloudException ex) {
              String error = "Failed to enable logical volume " + absoluteLVName + ": " + ex.getMessage();
              LOG.error(error);
              throw new EucalyptusCloudException(ex);
            }
            if (enablementReturnCode != 0) {
              throw new EucalyptusCloudException("Failed to enable logical volume " + absoluteLVName + 
                  ", return code: " + enablementReturnCode);
            }
            // export logical volume
            try {
              volumeManager.exportVolume(lvmVolumeInfo, vgName, lvName);
              lvmVolumeInfo.setCleanup(false);
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
    return getVolumeConnectionString(volumeId);
  }

  private int enableLogicalVolume(String lvName) throws EucalyptusCloudException {

    Exception exSaved = null;
    CommandOutput commandOutput = null;    
    // 1st attempt's retry timeout is 10 ms, 2nd is 40ms etc., up to the configurable total timeout.
    final int timeoutMultiplier = 4;
    int attempt = 1;
    Long retryTimeout = 10l; // ms
    Long totalTimeoutSoFar = 0l;
    Long totalTimeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();

    do {
      try {
        LOG.debug("Enabling logical volume " + lvName + ", attempt " + attempt);
        commandOutput = LVMWrapper.enableLogicalVolume(lvName);
        exSaved = null;
      } catch (EucalyptusCloudException exThis) {
        exSaved = exThis;
      }
      if (exSaved == null && commandOutput.returnValue == 0) {
        LOG.debug("Enable succeeded for volume " + lvName);
        if (attempt > 1) {
          LOG.info("Enabling " + lvName + " succeeded on retry attempt " + attempt);
        }
        break;
      }
      // It didn't work, and either returned empty output or threw an exception.
      // It might be because lvmetad hasn't yet scanned the VG and LV into its metadata cache.
      // This is very common as of el7 and Euca 4.3, so INFO level only.
      LOG.info("Failed to enable logical volume " + lvName + " on attempt " + attempt);
      LOG.debug("Enabling logical volume " + lvName + " output:\n return=" + commandOutput.returnValue + 
          "\n stdout=" + commandOutput.output + "\n stderr=" + commandOutput.error, exSaved);
      // If this is the first attempt, force a pvscan.
      if (attempt == 1) {
        LOG.debug("Initiating a physical volume scan before retrying to enable volume " + lvName);
        LVMWrapper.scanForPhysicalVolumes();
      }
      // If it's not our last retry, wait a bit longer, then try again.
      if (totalTimeoutSoFar + retryTimeout < totalTimeout) {
        try {
          Thread.sleep(retryTimeout);
        } catch (InterruptedException ie) {
          LOG.error(ie);
          break;
        }
      }
      totalTimeoutSoFar += retryTimeout;
      retryTimeout *= timeoutMultiplier;
      attempt++;
    } while (totalTimeoutSoFar < totalTimeout);

    if (exSaved != null) {
      LOG.error("Failed to enable logical volume " + lvName + ", all retries exhausted.", exSaved);
      throw new EucalyptusCloudException("Failed to enable logical volume " + lvName);
    }
    return commandOutput.returnValue;
  }

  @Override
  public boolean getFromBackend(String snapshotId, int size) throws EucalyptusCloudException {
    return false;
  }

  @Override
  public void checkVolume(String volumeId) throws EucalyptusCloudException {
    try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
      LVMVolumeInfo lvmVolInfo = volumeManager.getVolumeInfo(volumeId);
      if (lvmVolInfo != null) {
        if (!new File(DirectStorageInfo.getStorageInfo().getVolumesDir() + File.separator + lvmVolInfo.getVolumeId()).exists()) {
          volumeManager.abort();
          throw new EucalyptusCloudException("Unable to find backing volume for: " + volumeId);
        }
      }
      volumeManager.finish();
    }
  }

  @Override
  public List<CheckerTask> getCheckers() {
    List<CheckerTask> checkers = new ArrayList<CheckerTask>();
    return checkers;
  }

  @Override
  public String createSnapshotPoint(String volumeId, String snapshotId) throws EucalyptusCloudException {
    return null;
  }

  @Override
  public void deleteSnapshotPoint(String volumeId, String snapshotId, String snapshotPointId) throws EucalyptusCloudException {
    throw new EucalyptusCloudException("Synchronous snapshot points not supported in Overlay storage manager");
  }

  private void doUnexport(final String volumeId) throws EucalyptusCloudException {
    try {
      try (VolumeMetadataManager volumeManager = new VolumeMetadataManager()) {
        LVMVolumeInfo volume = volumeManager.getVolumeInfo(volumeId);
        VolumeOpMonitor monitor = getMonitor(volume.getVolumeId());
        synchronized (monitor) {
          try {
            LOG.info("Cleaning up volume: " + volume.getVolumeId());
            String path = lvmRootDirectory + PATH_SEPARATOR + volume.getVgName() + PATH_SEPARATOR + volume.getLvName();
            try {
              if (LVMWrapper.logicalVolumeExists(path)) {
                // guard this. tgt is not happy when you ask it to
                // get rid of a non existent tid
                LOG.debug("Found logical volume at " + path + " for " + volume.getVolumeId() + ". Now cleaning up");
                exportManager.cleanup(volume);
              } else {
                LOG.debug("Failed to find logical volume at " + path + " for " + volume.getVolumeId() + ". Skipping cleanup for tgt and lvm");
              }
              // volumeManager.finish();
            } catch (EucalyptusCloudException ee) {
              LOG.error("Error cleaning up volume iscsi state " + volume.getVolumeId(), ee);
              // volumeManager.abort();
              throw ee;
            } finally {
              volumeManager.finish();
            }

            try (VolumeMetadataManager nestedVolumeManager = new VolumeMetadataManager()) {
              try {
                volume = nestedVolumeManager.getVolumeInfo(volumeId);
                String loDevName = volume.getLoDevName();
                if (loDevName != null) {
                  if (volume != null) {
                    if (!nestedVolumeManager.areSnapshotsPending(volume.getVolumeId())) {
                      LOG.info("Disabling logical volume " + volume.getVolumeId());
                      LVMWrapper.disableLogicalVolume(path);
                      LOG.info("Detaching loop device: " + loDevName + " for volume " + volume.getVolumeId());
                      removeLoopback(loDevName);
                      volume.setLoDevName(null);
                      LOG.info("Done cleaning up: " + volume.getVolumeId());
                      volume.setCleanup(false);
                    } else {
                      LOG.info("Snapshot in progress. Not detaching loop device.");
                      // volumeManager.abort();
                    }
                  }
                }
              } catch (EucalyptusCloudException e) {
                LOG.error(e, e);
                throw e;
              } finally {
                nestedVolumeManager.finish();
              }
            }
          } finally {
            // Release any waiting
            monitor.notifyAll();
          }
        } // synchronized
      }
    } catch (Exception ex) {
      LOG.error("Error unexporting " + volumeId + " from all hosts", ex);
      throw new EucalyptusCloudException(ex);
    }
  }
}
