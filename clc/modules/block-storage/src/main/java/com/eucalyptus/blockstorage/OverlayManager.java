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

import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.blockstorage.StorageManagers.StorageManagerProperty;
import com.eucalyptus.blockstorage.entities.DirectStorageInfo;
import com.eucalyptus.blockstorage.entities.ISCSIVolumeInfo;
import com.eucalyptus.blockstorage.entities.LVMVolumeInfo;
import com.eucalyptus.blockstorage.entities.StorageInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.objectstorage.util.WalrusProperties;
import com.eucalyptus.storage.common.CheckerTask;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Joiner;

import edu.ucsb.eucalyptus.msgs.ComponentProperty;
import edu.ucsb.eucalyptus.util.StreamConsumer;
import edu.ucsb.eucalyptus.util.SystemUtil;
import edu.ucsb.eucalyptus.util.SystemUtil.CommandOutput;

@StorageManagerProperty("overlay")
public class OverlayManager extends DASManager {
	private static Logger LOG = Logger.getLogger(OverlayManager.class);

	public static boolean zeroFillVolumes = false;

	private static final Joiner JOINER = Joiner.on(" ").skipNulls();

	public void checkPreconditions() throws EucalyptusCloudException {
		//check if binaries exist, commands can be executed, etc.
		if(!new File(StorageProperties.EUCA_ROOT_WRAPPER).exists()) {
			throw new EucalyptusCloudException("root wrapper (euca_rootwrap) does not exist in " + StorageProperties.EUCA_ROOT_WRAPPER);
		}
		File varDir = new File(EUCA_VAR_RUN_PATH);
		if(!varDir.exists()) {
			varDir.mkdirs();
		}
		try {
			String returnValue = LVMWrapper.getLvmVersion();
			if(returnValue.length() == 0) {
				throw new EucalyptusCloudException("Is lvm installed?");
			} else {
				LOG.info(returnValue);
			}
			//exportManager = new ISCSIManager();
			exportManager.checkPreconditions();
		} catch(EucalyptusCloudException ex) {
			String error = "Unable to run command: " + ex.getMessage();
			LOG.error(error);
			throw new EucalyptusCloudException(error);
		}
	}

	private String findFreeLoopback() throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{StorageProperties.EUCA_ROOT_WRAPPER, "losetup", "-f"}).replaceAll("\n", "");
	}

	private  String getLoopback(String loDevName) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{StorageProperties.EUCA_ROOT_WRAPPER, "losetup", loDevName});
	}

	// losetup -d fails a LOT of times, added retries.
	// losetup -d does not return any output, so there's no way to detect if the command was successful with previous logic
	// losetup return code is not reliable, introduced check on the error stream and loop device
	private String removeLoopback(String loDevName) throws EucalyptusCloudException {
		int retryCount = 0;
		do {
			try {
				String[] command = new String[] { StorageProperties.EUCA_ROOT_WRAPPER, "losetup", "-d", loDevName };
				CommandOutput resultObj1 = SystemUtil.runWithRawOutput(command);
				LOG.debug("Executed: " + JOINER.join(command) + "\n return=" + resultObj1.returnValue + "\n stdout=" + resultObj1.output + "\n stderr="
						+ resultObj1.error);

				command = new String[] { StorageProperties.EUCA_ROOT_WRAPPER, "losetup", loDevName };
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
		String returnValue = SystemUtil.run(new String[]{StorageProperties.EUCA_ROOT_WRAPPER, "vgdisplay", vgName});
		if(returnValue.length() > 0) {
			success = true;
		}
		return success;
	}

	private boolean physicalVolumeExists(String pvName) {
		boolean success = false;
		String returnValue = SystemUtil.run(new String[]{StorageProperties.EUCA_ROOT_WRAPPER, "pvdisplay", pvName});
		if(returnValue.length() > 0) {
			success = true;
		}
		return success;
	}

	private int losetup(String absoluteFileName, String loDevName) {
		try
		{
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(new String[]{StorageProperties.EUCA_ROOT_WRAPPER, "losetup", loDevName, absoluteFileName});
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

	public void initialize() throws EucalyptusCloudException {
		File storageRootDir = new File(getStorageRootDirectory());
		if(!storageRootDir.exists()) {
			if(!storageRootDir.mkdirs()) {
				throw new EucalyptusCloudException("Unable to make volume root directory: " + getStorageRootDirectory());
			}
		}
		//The following should be executed only once during the entire lifetime of the VM.
		if(!initialized) {
			//			System.loadLibrary("lvm2control");
			//			registerSignals();
			initialized = true;
		}
	}

	public void configure() throws EucalyptusCloudException {
		exportManager.configure();
		//First call to StorageInfo.getStorageInfo will add entity if it does not exist
		LOG.info(""+StorageInfo.getStorageInfo().getName());
		checkVolumesDir();
	}

	private void checkVolumesDir() {
		String volumesDir = DirectStorageInfo.getStorageInfo().getVolumesDir();
		File volumes = new File(volumesDir);
		if(!volumes.exists()) {
			if(!volumes.mkdirs()) {
				LOG.fatal("Unable to make volume root directory: " + volumesDir);
			}
		} else if(!volumes.canWrite()) {
			LOG.fatal("Cannot write to volume root directory: " + volumesDir);
		}
		try {
			SystemUtil.setEucaReadWriteOnly(volumesDir);
		} catch (EucalyptusCloudException ex) {
			LOG.fatal(ex);
		}
	}

	public void cleanVolume(String volumeId) {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo lvmVolInfo = volumeManager.getVolumeInfo(volumeId);
		if(lvmVolInfo != null) {
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
			} catch(EucalyptusCloudException ex) {
				volumeManager.abort();
				String error = "Unable to run command: " + ex.getMessage();
				LOG.error(error);
			}
			volumeManager.remove(lvmVolInfo);
			File volFile = new File (DirectStorageInfo.getStorageInfo().getVolumesDir() + File.separator + lvmVolInfo.getVolumeId());
			if (volFile.exists()) {
				if(!volFile.delete()) {
					LOG.error("Unable to delete: " + volFile.getAbsolutePath() + " for failed volume");
				}
			}
		}
		volumeManager.finish();
	}

	public void cleanSnapshot(String snapshotId) {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo lvmVolInfo = volumeManager.getVolumeInfo(snapshotId);
		if(lvmVolInfo != null) {
			volumeManager.remove(lvmVolInfo);
			File volFile = new File (DirectStorageInfo.getStorageInfo().getVolumesDir() + File.separator + lvmVolInfo.getVolumeId());
			if (volFile.exists()) {
				if(!volFile.delete()) {
					LOG.error("Unable to delete: " + volFile.getAbsolutePath() + " for failed snapshot");
				}
			}
		}
		volumeManager.finish();
	}

	//NOTE: Overlay, like DASManager does not support host-specific detach.

	/**
	 * Do the unexport synchronously
	 * @param volumeId
	 * @throws EucalyptusCloudException
	 */
	@Override
	public void unexportVolumeFromAll(String volumeId) throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo volumeInfo = volumeManager.getVolumeInfo(volumeId);
		if(volumeInfo == null) {
			volumeManager.abort();
			LOG.error("Cannot unexport volume for all hosts because volume " + volumeId + " not found in db");
			throw new EucalyptusCloudException("Volume " + volumeId + " not found");
		} else {
			LOG.debug("Unexporting volume " + volumeId);
			try {
				doUnexport(volumeId);
			} catch(EucalyptusCloudException e) {
				LOG.error("Unexport failed for volume " + volumeId);
				throw e;
			} finally {
				volumeManager.finish();
			}
		}
	}


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
			if(fileOutputStream != null) {
				try {
					out.close();
					fileOutputStream.close();
				} catch (IOException e) {
					LOG.error(e);
				}
			}
			if(fileInputStream != null) {
				try {
					in.close();
					fileInputStream.close();
				} catch(IOException e) {
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
		if(!(new File(fileName).exists()))
			throw new EucalyptusCloudException("Unable to create file " + fileName);
		return createLoopback(fileName);
	}

	public synchronized String createLoopback(String fileName) throws EucalyptusCloudException {
		int number_of_retries = 0;
		int status = -1;
		String loDevName;
		do {
			loDevName = findFreeLoopback();
			if(loDevName.length() > 0) {
				status = losetup(fileName, loDevName);
			}
			if(number_of_retries++ >= MAX_LOOP_DEVICES)
				break;
		} while(status != 0);

		if(status != 0) {
			throw new EucalyptusCloudException("Could not create loopback device for " + fileName +
					". Please check the max loop value and permissions");
		}
		return loDevName;
	}

	public int createLoopback(String absoluteFileName, String loDevName) {
		return losetup(absoluteFileName, loDevName);
	}

	public String createLoopback(String fileName, long size) throws EucalyptusCloudException {
		createAbsoluteEmptyFile(fileName, size);
		if(!(new File(fileName).exists()))
			throw new EucalyptusCloudException("Unable to create file " + fileName);
		return createLoopback(fileName);
	}

	public void createEmptyFile(String fileName, long size) throws EucalyptusCloudException {
		createAbsoluteEmptyFile(fileName, size);
		if(!(new File(fileName).exists()))
			throw new EucalyptusCloudException("Unable to create file " + fileName);
	}

	//creates a logical volume (and a new physical volume and volume group)
	public void createLogicalVolume(String volumeId, String loDevName, String vgName, String lvName) throws EucalyptusCloudException {
		String returnValue = LVMWrapper.createPhysicalVolume(loDevName);
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("Unable to create physical volume for " + loDevName);
		}
		returnValue = LVMWrapper.createVolumeGroup(loDevName, vgName);
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("Unable to create volume group " + vgName + " for " + loDevName);
		}
		returnValue = LVMWrapper.createLogicalVolume(volumeId, vgName, lvName);
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("Unable to create logical volume " + lvName + " in volume group " + vgName);
		}
	}

	public void createSnapshotLogicalVolume(String loDevName, String vgName, String lvName, String snapLvName) throws EucalyptusCloudException {
		String returnValue = LVMWrapper.createPhysicalVolume(loDevName);
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("Unable to create physical volume for " + loDevName);
		}
		returnValue = LVMWrapper.extendVolumeGroup(loDevName, vgName);
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("Unable to extend volume group " + vgName + " for " + loDevName);
		}
		returnValue = LVMWrapper.createSnapshotLogicalVolume(lvName, snapLvName);
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("Unable to create snapshot logical volume " + snapLvName + " for volume " + lvName);
		}
	}

	public void createVolume(String volumeId, int size) throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();

		LVMVolumeInfo lvmVolumeInfo = null;
		lvmVolumeInfo = new ISCSIVolumeInfo();

		volumeManager.finish();
		String rawFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + volumeId;
		//create file and attach to loopback device
		long absoluteSize = size * StorageProperties.GB + LVM_HEADER_LENGTH;
		try {
			//set up LVM
			String vgName = generateVGName(volumeId);
			String lvName = generateLVName(volumeId);
			//create file and attach to loopback device
			String loDevName = createLoopback(rawFileName, absoluteSize);
			lvmVolumeInfo.setVolumeId(volumeId);
			lvmVolumeInfo.setLoDevName(loDevName);

			//create physical volume, volume group and logical volume
			createLogicalVolume(volumeId, loDevName, vgName, lvName);
			lvmVolumeInfo.setVgName(vgName);
			lvmVolumeInfo.setLvName(lvName);

			lvmVolumeInfo.setStatus(StorageProperties.Status.available.toString());
			lvmVolumeInfo.setSize(size);
			//tear down
			String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;
			LVMWrapper.disableLogicalVolume(absoluteLVName);
			removeLoopback(loDevName);
			lvmVolumeInfo.setLoDevName(null);
			volumeManager = new VolumeEntityWrapperManager();
			volumeManager.add(lvmVolumeInfo);			
		} catch(EucalyptusCloudException ex) {
			String error = "Unable to run command: " + ex.getMessage();			
			//zhill: should always commit what we have thus far
			//volumeManager.abort();
			LOG.error(error);
			throw new EucalyptusCloudException(error);
		} finally {
			try {
				if(volumeManager != null) {
					volumeManager.finish();
				} else {
					LOG.error("Cannot commit to db. EntityWrapper is null");
				}
			} catch(final Throwable e) {
				LOG.error("Volume: " + volumeId + " db commit error: " + e.getMessage());
			}
		}
	}

	public int createVolume(String volumeId, String snapshotId, int size) throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo foundSnapshotInfo = volumeManager.getVolumeInfo(snapshotId);
		if(foundSnapshotInfo != null) {
			String status = foundSnapshotInfo.getStatus();
			if(status.equals(StorageProperties.Status.available.toString())) {
				String vgName = generateVGName(volumeId);
				String lvName = generateLVName(volumeId);
				String loFileName = foundSnapshotInfo.getLoFileName();
				String snapId = foundSnapshotInfo.getVolumeId();
				LVMVolumeInfo lvmVolumeInfo = volumeManager.getVolumeInfo();
				volumeManager.finish();
				try {
					String rawFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + volumeId;
					//create file and attach to loopback device
					File snapshotFile = new File(DirectStorageInfo.getStorageInfo().getVolumesDir() + PATH_SEPARATOR + snapId);
					assert(snapshotFile.exists());
					long absoluteSize;
					if (size <= 0 || size == foundSnapshotInfo.getSize()) {
						size = (int)(snapshotFile.length() / StorageProperties.GB);
						absoluteSize = snapshotFile.length() + LVM_HEADER_LENGTH;
					} else {
						absoluteSize = size * StorageProperties.GB + LVM_HEADER_LENGTH;
					}

					String loDevName = createLoopback(rawFileName, absoluteSize);
					lvmVolumeInfo.setVolumeId(volumeId);
					lvmVolumeInfo.setLoDevName(loDevName);

					//create physical volume, volume group and logical volume
					createLogicalVolume(volumeId, loDevName, vgName, lvName);
					//duplicate snapshot volume
					String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;
					duplicateLogicalVolume(loFileName, absoluteLVName);

					lvmVolumeInfo.setVgName(vgName);
					lvmVolumeInfo.setLvName(lvName);
					lvmVolumeInfo.setStatus(StorageProperties.Status.available.toString());
					lvmVolumeInfo.setSize(size);
					//tear down
					LVMWrapper.disableLogicalVolume(absoluteLVName);
					removeLoopback(loDevName);
					lvmVolumeInfo.setLoDevName(null);
					volumeManager = new VolumeEntityWrapperManager();
					volumeManager.add(lvmVolumeInfo);
					//Do this finish in the finally block
					//volumeManager.finish();
				}  catch(EucalyptusCloudException ex) {
					//zhill: always commit what we have, so as not to orphan resources. This allows cleanup to properly
					// clean local resources.
					//volumeManager.abort();
					String error = "Unable to run command: " + ex.getMessage();
					LOG.error(error);
					throw new EucalyptusCloudException(error);
				} finally {			
					try {
						if(volumeManager != null) {
							volumeManager.finish();
						} else {
							LOG.error("Cannot commit to db. EntityWrapper is null");
						}
					} catch(final Throwable e) {
						LOG.error("Volume: " + volumeId + " could not commit db transaction. " + e.getMessage());							
					}					
				}
			}
		} else {
			volumeManager.abort();
			throw new EucalyptusCloudException("Unable to find snapshot: " + snapshotId);
		}
		return size;
	}

	public void cloneVolume(String volumeId, String parentVolumeId)
			throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo foundVolumeInfo = volumeManager.getVolumeInfo(parentVolumeId);
		if(foundVolumeInfo != null) {
			String vgName = generateVGName(volumeId);
			String lvName = generateLVName(volumeId);
			String parentVgName = foundVolumeInfo.getVgName();
			String parentLvName = foundVolumeInfo.getLvName();
			LVMVolumeInfo lvmVolumeInfo = volumeManager.getVolumeInfo();
			int size = foundVolumeInfo.getSize();
			volumeManager.finish();
			try {
				String rawFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + volumeId;
				//create file and attach to loopback device
				File parentVolumeFile = new File(DirectStorageInfo.getStorageInfo().getVolumesDir() + PATH_SEPARATOR + parentVolumeId);
				assert(parentVolumeFile.exists());
				long absoluteSize = parentVolumeFile.length();

				String loDevName = createLoopback(rawFileName, absoluteSize);
				lvmVolumeInfo.setLoDevName(loDevName);
				//create physical volume, volume group and logical volume
				createLogicalVolume(volumeId, loDevName, vgName, lvName);
				//duplicate snapshot volume
				String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;
				String absoluteParentLVName = lvmRootDirectory + PATH_SEPARATOR + parentVgName + PATH_SEPARATOR + parentLvName;
				duplicateLogicalVolume(absoluteParentLVName, absoluteLVName);
				//export logical volume
				try {
					volumeManager.exportVolume(lvmVolumeInfo, vgName, lvName);
				} catch(EucalyptusCloudException ex) {
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
				volumeManager = new VolumeEntityWrapperManager();
				volumeManager.add(lvmVolumeInfo);	
			}  catch(EucalyptusCloudException ex) {
				volumeManager.abort();
				String error = "Unable to run command: " + ex.getMessage();
				LOG.error(error);
				throw new EucalyptusCloudException(error);
			} finally {
				if(null != volumeManager) {
					try {
						volumeManager.finish();
					} catch(final Throwable e) {
						LOG.error("Error committing to db.",e);
					}
				}				
			}		
		} else {
			volumeManager.abort();
			throw new EucalyptusCloudException("Unable to find volume: " + parentVolumeId);
		}
	}

	public void addSnapshot(String snapshotId) throws EucalyptusCloudException {
		String snapshotRawFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + snapshotId;
		File snapshotFile = new File(snapshotRawFileName);
		if(snapshotFile.exists()) {
			VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
			LVMVolumeInfo lvmVolumeInfo = volumeManager.getVolumeInfo();
			lvmVolumeInfo.setVolumeId(snapshotId);
			lvmVolumeInfo.setLoFileName(snapshotRawFileName);			
			lvmVolumeInfo.setStatus(StorageProperties.Status.available.toString());
			lvmVolumeInfo.setSize((int)(snapshotFile.length() / StorageProperties.GB));
			volumeManager.add(lvmVolumeInfo);
			volumeManager.finish();
		} else {
			throw new EucalyptusCloudException("Snapshot backing file does not exist for: " + snapshotId);
		}
	}

	public void deleteVolume(String volumeId) throws EucalyptusCloudException {
		LVMVolumeInfo foundLVMVolumeInfo = null;
		{
			final VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
			try {
				foundLVMVolumeInfo = volumeManager.getVolumeInfo(volumeId);
			} finally {
				volumeManager.finish();
			}
		}

		if (foundLVMVolumeInfo != null) {
			boolean isReadyForDelete = false;
			int retryCount = 0;

			// Obtain a lock on the volume
			VolumeOpMonitor monitor = getMonitor(foundLVMVolumeInfo.getVolumeId());
			VolumeEntityWrapperManager outerVolumeManager = null;
			do {
				LOG.debug("Trying to lock volume for export detection" + volumeId);
				synchronized (monitor) {
					final VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
					try {
						foundLVMVolumeInfo = volumeManager.getVolumeInfo(volumeId);
						if(exportManager.isExported(foundLVMVolumeInfo)) {
							LOG.error("Cannot delete volume " + volumeId + " because it is currently exported");
							volumeManager.finish();
						} else {
							LOG.debug("Volume " + volumeId + " is prepped for deletion");
							isReadyForDelete = true;
							break;
						}
					} catch (Exception e) {
						LOG.error("Error trying to check volume status", e);
					} finally {
						if ( isReadyForDelete ) {
							outerVolumeManager = volumeManager; // hand off without closing
						} else {
							volumeManager.abort(); //no-op if finish() called
						}
					}
					LOG.debug("Lap: " + retryCount++);
				} //Release the lock for retry.
				if(!isReadyForDelete) {
					try {
						Thread.sleep(10000); //sleep before the retry
					} catch(InterruptedException e) {
						throw new EucalyptusCloudException("Thread interrupted. Failing volume delete for volume " + volumeId);
					}
				}
			} while (!isReadyForDelete && retryCount < 20);

			LOG.debug("Trying to lock volume for volume deletion" + volumeId);
			synchronized(monitor) {
				// delete the volume
				if (isReadyForDelete) {
					try {
						LOG.debug("Deleting volume " + volumeId);
						File volFile = new File(DirectStorageInfo.getStorageInfo().getVolumesDir() + File.separator + volumeId);
						if (volFile.exists()) {
							if (!volFile.delete()) {
								LOG.error("Unable to delete: " + volFile.getAbsolutePath());
								throw new EucalyptusCloudException("Unable to delete volume file: " + volFile.getAbsolutePath());
							}
						}
						outerVolumeManager.remove(foundLVMVolumeInfo);
						try {
							outerVolumeManager.finish();
						} catch (Exception e) {
							LOG.error("Error deleting volume " + volumeId + ", failed to commit DB transaction", e);
						}
					} finally {
						outerVolumeManager.abort(); //no-op if finish() called
					}
				} else {
					LOG.error("All attempts to cleanup volume " + volumeId + " failed");
					throw new EucalyptusCloudException("Unable to delete volume: " + volumeId + ". All attempts to cleanup the volume failed");
				}				
			}
			// Remove the monitor
			removeMonitor(volumeId);
		} else {
			throw new EucalyptusCloudException("Unable to find volume: " + volumeId);
		}
	}

	/*LVM is flaky when there are a large number of concurrent removal requests. This workaround serializes lvm cleanup*/
	private synchronized void deleteLogicalVolume(String loDevName, String vgName,
			String absoluteLVName) throws EucalyptusCloudException,
			EucalyptusCloudException {
		if(LVMWrapper.logicalVolumeExists(absoluteLVName)) {
			String returnValue = LVMWrapper.removeLogicalVolume(absoluteLVName);
			if(returnValue.length() == 0) {
				throw new EucalyptusCloudException("Unable to remove logical volume " + absoluteLVName + " " + returnValue);
			}
		}
		if(volumeGroupExists(vgName)) {
			String returnValue = LVMWrapper.removeVolumeGroup(vgName);
			if(returnValue.length() == 0) {
				throw new EucalyptusCloudException("Unable to remove volume group " + vgName + " " + returnValue);
			}
		}
		if(physicalVolumeExists(loDevName)) { 
			String returnValue = LVMWrapper.removePhysicalVolume(loDevName);
			if(returnValue.length() == 0) {
				throw new EucalyptusCloudException("Unable to remove physical volume " + loDevName + " " + returnValue);
			}
		}
	}

	public List<String> createSnapshot(String volumeId, String snapshotId, String snapshotPointId, Boolean shouldTransferSnapshot) throws EucalyptusCloudException {
		if(snapshotPointId != null) {
			throw new EucalyptusCloudException("Synchronous snapshot points not supported in Overlay storage manager");
		}

		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo foundLVMVolumeInfo = volumeManager.getVolumeInfo(volumeId);
		ArrayList<String> returnValues = new ArrayList<String>();
		if(foundLVMVolumeInfo != null) {
			LVMVolumeInfo snapshotInfo = volumeManager.getVolumeInfo();
			snapshotInfo.setVolumeId(snapshotId);
			String vgName = foundLVMVolumeInfo.getVgName();
			String lvName = generateLVName(snapshotId);
			String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + foundLVMVolumeInfo.getLvName();

			int size = foundLVMVolumeInfo.getSize();
			long snapshotSize = (size * StorageProperties.GB) / 2;
			String rawFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + volumeId + Hashes.getRandom(6);
			//create file and attach to loopback device
			volumeManager.finish();
			try {
				//mount volume and loopback and enable
				VolumeOpMonitor monitor = getMonitor(volumeId);
				String absoluteVolLVName = lvmRootDirectory + PATH_SEPARATOR + foundLVMVolumeInfo.getVgName() + PATH_SEPARATOR + foundLVMVolumeInfo.getLvName();
				String volLoDevName = foundLVMVolumeInfo.getLoDevName();
				boolean tearDown = false;
				synchronized(monitor) {
					if(!LVMWrapper.logicalVolumeExists(absoluteVolLVName)) {
						volLoDevName = createLoopback(DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + volumeId);
						LVMWrapper.enableLogicalVolume(absoluteVolLVName);
						tearDown = true;
					}

					snapshotInfo.setStatus(StorageProperties.Status.pending.toString());
					snapshotInfo.setSize(size);
					snapshotInfo.setSnapshotOf(volumeId);
					volumeManager = new VolumeEntityWrapperManager();
					volumeManager.add(snapshotInfo);
					volumeManager.finish();

					String loDevName = createLoopback(rawFileName, snapshotSize);

					//create physical volume, volume group and logical volume
					createSnapshotLogicalVolume(loDevName, vgName, absoluteLVName, lvName);

					String snapRawFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + snapshotId;
					String absoluteSnapLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;

					duplicateLogicalVolume(absoluteSnapLVName, snapRawFileName);

					String returnValue = LVMWrapper.removeLogicalVolume(absoluteSnapLVName);
					if(returnValue.length() == 0) {
						throw new EucalyptusCloudException("Unable to remove logical volume " + absoluteSnapLVName);
					}
					returnValue = LVMWrapper.reduceVolumeGroup(vgName, loDevName);
					if(returnValue.length() == 0) {
						throw new EucalyptusCloudException("Unable to reduce volume group " + vgName + " logical volume: " + loDevName);
					}
					returnValue = LVMWrapper.removePhysicalVolume(loDevName);
					if(returnValue.length() == 0) {
						throw new EucalyptusCloudException("Unable to remove physical volume " + loDevName);
					}
					returnValue = removeLoopback(loDevName);
					if(!(new File(rawFileName)).delete()) {
						LOG.error("Unable to remove temporary snapshot file: " + rawFileName);
					}
					//tear down volume

					if(tearDown) {
						LOG.info("Snapshot complete. Detaching loop device" + volLoDevName);
						LVMWrapper.disableLogicalVolume(absoluteVolLVName);
						removeLoopback(volLoDevName);
					}		
					returnValues.add(snapRawFileName);
					returnValues.add(String.valueOf(size * WalrusProperties.G));
					volumeManager = new VolumeEntityWrapperManager();
					LVMVolumeInfo foundSnapshotInfo = volumeManager.getVolumeInfo(snapshotId);
					foundSnapshotInfo.setLoFileName(snapRawFileName);
					foundSnapshotInfo.setStatus(StorageProperties.Status.available.toString());
					volumeManager.finish();
				}//synchronized
			} catch(EucalyptusCloudException ex) {
				if(volumeManager != null)
					volumeManager.abort();
				String error = "Unable to run command: " + ex.getMessage();
				LOG.error(error);
				throw new EucalyptusCloudException(error);
			}
		}
		return returnValues;
	}

	public List<String> prepareForTransfer(String snapshotId) throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo foundLVMVolumeInfo = volumeManager.getVolumeInfo(snapshotId);
		ArrayList<String> returnValues = new ArrayList<String>();

		if(foundLVMVolumeInfo != null) {
			returnValues.add(DirectStorageInfo.getStorageInfo().getVolumesDir() + PATH_SEPARATOR + foundLVMVolumeInfo.getVolumeId());
			volumeManager.finish();
		} else {
			volumeManager.abort();
			throw new EucalyptusCloudException("Unable to find snapshot: " + snapshotId);
		}
		return returnValues;
	}

	public void deleteSnapshot(String snapshotId) throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo foundLVMVolumeInfo = volumeManager.getVolumeInfo(snapshotId);

		if(foundLVMVolumeInfo != null) {
			volumeManager.remove(foundLVMVolumeInfo);
			File snapFile = new File (DirectStorageInfo.getStorageInfo().getVolumesDir() + File.separator + foundLVMVolumeInfo.getVolumeId());
			volumeManager.finish();
			if (snapFile.exists()) {
				if(!snapFile.delete()) {
					throw new EucalyptusCloudException("Unable to delete: " + snapFile.getAbsolutePath());
				}
			}
		}  else {
			volumeManager.abort();
			throw new EucalyptusCloudException("Unable to find snapshot: " + snapshotId);
		}
	}

	public void loadSnapshots(List<String> snapshotSet, List<String> snapshotFileNames) throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		assert(snapshotSet.size() == snapshotFileNames.size());
		int i = 0;
		for(String snapshotFileName: snapshotFileNames) {
			try {
				String loDevName = createLoopback(snapshotFileName);
				LVMVolumeInfo lvmVolumeInfo = volumeManager.getVolumeInfo();
				lvmVolumeInfo.setVolumeId(snapshotSet.get(i++));
				lvmVolumeInfo.setLoDevName(loDevName);
				lvmVolumeInfo.setStatus(StorageProperties.Status.available.toString());
				volumeManager.add(lvmVolumeInfo);
			} catch(EucalyptusCloudException ex) {
				volumeManager.abort();
				String error = "Unable to run command: " + ex.getMessage();
				LOG.error(error);
				throw new EucalyptusCloudException(error);
			}

		}
		volumeManager.finish();
	}

	/**
	 * Called on service start to load and export any volumes that should be available to clients immediately.
	 * Bases that decision on the lodevName field in the DB.
	 * 
	 */
	public void reload() {
		LOG.info("Initiating SC Reload of iSCSI targets");
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		List<LVMVolumeInfo> volumeInfos = volumeManager.getAllVolumeInfos();
		LOG.info("SC Reload found " + volumeInfos.size() + " volumes in the DB");

		//Ensure that all loopbacks are properly setup.
		for(LVMVolumeInfo foundVolumeInfo : volumeInfos) {
			String loDevName = foundVolumeInfo.getLoDevName();
			if(loDevName != null) {
				String loFileName = foundVolumeInfo.getVolumeId();
				LOG.info("SC Reload: found volume " + loFileName + " was exported at last shutdown. Ensuring export restored");
				String absoluteLoFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + PATH_SEPARATOR + loFileName;
				if(!new File(absoluteLoFileName).exists()) {
					LOG.error("SC Reload: Backing volume: " + absoluteLoFileName + " not found. Invalidating volume."); 
					foundVolumeInfo.setStatus(StorageProperties.Status.failed.toString());
					continue;
				}
				try {
					//Ensure the loopback isn't used
					String returnValue = getLoopback(loDevName);
					if(returnValue.length() <= 0) {
						LOG.info("SC Reload: volume " + loFileName + " previously used loopback " + loDevName + ". No conflict detected, reusing same loopback");
						createLoopback(absoluteLoFileName, loDevName);
					} else {
						if(!returnValue.contains(loFileName)) {
							//Use a new loopback since the old one is used by something else
							String newLoDev = createLoopback(absoluteLoFileName);
							foundVolumeInfo.setLoDevName(newLoDev);
							LOG.info("SC Reload: volume " + loFileName + " previously used loopback " + loDevName + ", but loopback already in used by something else. Using new loopback: " + newLoDev);
						} else {
							LOG.info("SC Reload: Detection of loopback for volume " + loFileName + " got " + returnValue + ". Appears that loopback is already in-place. No losetup needed for this volume.");
						}
					}
				} catch(EucalyptusCloudException ex) {
					String error = "Unable to run command: " + ex.getMessage();
					LOG.error(error);
				}
			}
		}

		//now enable them
		try {
			LOG.info("SC Reload: Scanning volume groups. This might take a little while...");
			LVMWrapper.scanVolumeGroups();
		} catch (EucalyptusCloudException e) {
			LOG.error(e);
		}

		//Export volumes
		LOG.info("SC Reload: ensuring configured volumes are exported via iSCSI targets");
		for(LVMVolumeInfo foundVolumeInfo : volumeInfos) {
			try {
				//Only try to export volumes that have both a lodev and a vgname
				if (foundVolumeInfo.getLoDevName() != null && foundVolumeInfo.getVgName() != null) {
					LOG.info("SC Reload: exporting " + foundVolumeInfo.getVolumeId() + " in VG: " + foundVolumeInfo.getVgName());
					volumeManager.exportVolume(foundVolumeInfo);
				} else {
					LOG.info("SC Reload: no loopback configured for " + foundVolumeInfo.getVolumeId() + ". Skipping export for this volume.");
				}
			} catch(EucalyptusCloudException ex) {
				LOG.error("SC Reload: Unable to reload volume: " + foundVolumeInfo.getVolumeId() + ex.getMessage());
			}		
		}
		volumeManager.finish();
	}

	public int getSnapshotSize(String snapshotId) throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo lvmVolumeInfo = volumeManager.getVolumeInfo(snapshotId);
		if(lvmVolumeInfo != null) {
			int snapSize = lvmVolumeInfo.getSize();
			volumeManager.finish();
			return snapSize;
		} else {
			volumeManager.abort();
			return 0;
		}
	}

	@Override
	public void finishVolume(String snapshotId) throws EucalyptusCloudException{
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo foundSnapshotInfo = volumeManager.getVolumeInfo(snapshotId);
		if (null != foundSnapshotInfo) {		
			foundSnapshotInfo.setStatus(StorageProperties.Status.available.toString());
		}
		volumeManager.finish();
	}

	@Override
	public String prepareSnapshot(String snapshotId, int sizeExpected, long actualSizeInMB)
			throws EucalyptusCloudException {
		String deviceName = null;
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo foundSnapshotInfo = volumeManager.getVolumeInfo(snapshotId);
		if (null  == foundSnapshotInfo) {
			LVMVolumeInfo snapshotInfo = volumeManager.getVolumeInfo();
			snapshotInfo.setStatus(StorageProperties.Status.pending.toString());
			snapshotInfo.setVolumeId(snapshotId);
			snapshotInfo.setSize(sizeExpected);
			snapshotInfo.setLoFileName(DirectStorageInfo.getStorageInfo().getVolumesDir() + File.separator + snapshotId);
			deviceName = snapshotInfo.getLoFileName();
			volumeManager.add(snapshotInfo);
		}
		volumeManager.finish();
		return deviceName;

		// return DirectStorageInfo.getStorageInfo().getVolumesDir() + File.separator + snapshotId;
	}

	@Override
	public ArrayList<ComponentProperty> getStorageProps() {		
		ArrayList<ComponentProperty> componentProperties = null;
		ConfigurableClass configurableClass = StorageInfo.class.getAnnotation(ConfigurableClass.class);
		if(configurableClass != null) {
			String root = configurableClass.root();
			String alias = configurableClass.alias();
			componentProperties = (ArrayList<ComponentProperty>) PropertyDirectory.getComponentPropertySet(StorageProperties.NAME + "." + root, alias);
		}
		configurableClass = DirectStorageInfo.class.getAnnotation(ConfigurableClass.class);
		if(configurableClass != null) {
			String root = configurableClass.root();
			String alias = configurableClass.alias();
			if(componentProperties == null)
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
				//type parser will correctly covert the value
				entry.setValue(prop.getValue());
			} catch (IllegalAccessException e) {
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
	public String getVolumePath(String volumeId)
			throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo volInfo = volumeManager.getVolumeInfo(volumeId);
		if(volInfo != null) {
			String volumePath = lvmRootDirectory + File.separator + volInfo.getVgName() + File.separator + volInfo.getLvName();
			volumeManager.finish();
			return volumePath;
		} else {
			volumeManager.abort();
			throw new EntityNotFoundException("Unable to find volume with id: " + volumeId);
		}
	}

	@Override
	public void importVolume(String volumeId, String volumePath, int size)
			throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo volInfo = volumeManager.getVolumeInfo(volumeId);
		if(volInfo != null) {
			volumeManager.finish();
			throw new EucalyptusCloudException("Volume " + volumeId + " already exists. Import failed.");
		}
		volumeManager.finish();
		createVolume(volumeId, size);
		volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo volumeInfo = volumeManager.getVolumeInfo(volumeId);
		if(volumeInfo != null) {
			SystemUtil.run(new String[]{StorageProperties.EUCA_ROOT_WRAPPER, 
					"dd", "if=" + volumePath, 
					"of=" + lvmRootDirectory + File.separator + volumeInfo.getVgName() + 
					File.separator + volumeInfo.getLvName(), "bs=" + StorageProperties.blockSize});
		} else {
			volumeManager.abort();
			throw new EucalyptusCloudException("Unable to find volume with id: " + volumeId);
		}
	}

	@Override
	public String getSnapshotPath(String snapshotId)
			throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo volInfo = volumeManager.getVolumeInfo(snapshotId);
		if(volInfo != null) {
			String snapPath = volInfo.getLoFileName();
			volumeManager.finish();
			return snapPath;
		} else {
			volumeManager.abort();
			throw new EntityNotFoundException("Unable to find snapshot with id: " + snapshotId);
		}
	}

	@Override
	public void importSnapshot(String snapshotId, String volumeId, String snapPath, int size)
			throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo snapInfo = volumeManager.getVolumeInfo(snapshotId);
		if(snapInfo != null) {
			volumeManager.finish();
			throw new EucalyptusCloudException("Snapshot " + snapshotId + " already exists. Import failed.");
		}
		volumeManager.finish();
		String snapFileName = getStorageRootDirectory() + File.separator + snapshotId;
		SystemUtil.run(new String[]{StorageProperties.EUCA_ROOT_WRAPPER, 
				"dd", "if=" + snapPath, 
				"of=" + snapFileName, "bs=" + StorageProperties.blockSize});
		volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo snapshotInfo = volumeManager.getVolumeInfo();
		snapshotInfo.setVolumeId(snapshotId);
		snapshotInfo.setLoFileName(snapFileName);
		snapshotInfo.setSize(size);
		snapshotInfo.setSnapshotOf(volumeId);
		volumeManager.add(snapshotInfo);
		volumeManager.finish();
	}

	@Override
	public String exportVolume(String volumeId, String nodeIqn)
			throws EucalyptusCloudException {
		LVMVolumeInfo lvmVolumeInfo = null;
		{
			final VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
			try {
				lvmVolumeInfo = volumeManager.getVolumeInfo(volumeId);
			} finally {
				volumeManager.finish();
			}
		}

		if (lvmVolumeInfo != null) {
			// create file and attach to loopback device
			String rawFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + volumeId;

			VolumeOpMonitor monitor = getMonitor(volumeId);
			synchronized (monitor) {
				final VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
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
					//TODO: check that loDevName refers to extant loopback... if not, re-assign and create.
					
					String vgName = lvmVolumeInfo.getVgName();
					String lvName = lvmVolumeInfo.getLvName();
					String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;

					// enable logical volume
					LVMWrapper.enableLogicalVolume(absoluteLVName);

					try {
						// export logical volume
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
			}// synchronized
		}
		return getVolumeConnectionString(volumeId);
	}

	@Override
	public boolean getFromBackend(String snapshotId, int size)
			throws EucalyptusCloudException {
		return false;
	}

	@Override
	public void checkVolume(String volumeId) throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo lvmVolInfo = volumeManager.getVolumeInfo(volumeId);
		if(lvmVolInfo != null) {
			if(!new File(DirectStorageInfo.getStorageInfo().getVolumesDir() + File.separator + lvmVolInfo.getVolumeId()).exists()) {
				volumeManager.abort();
				throw new EucalyptusCloudException("Unable to find backing volume for: " + volumeId);
			}
		}
		volumeManager.finish();
	}

	@Override
	public List<CheckerTask> getCheckers() {
		List<CheckerTask> checkers = new ArrayList<CheckerTask>();
		return checkers;
	}

	@Override
	public String createSnapshotPoint(String volumeId, String snapshotId)
			throws EucalyptusCloudException {
		return null;
	}

	@Override
	public void deleteSnapshotPoint(String volumeId, String snapshotId, String snapshotPointId)
			throws EucalyptusCloudException {
		throw new EucalyptusCloudException("Synchronous snapshot points not supported in Overlay storage manager");
	}	
	
	private void doUnexport(final String volumeId) throws EucalyptusCloudException {
		try{ 
			VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
			LVMVolumeInfo volume = volumeManager.getVolumeInfo(volumeId);
			VolumeOpMonitor monitor = getMonitor(volume.getVolumeId());
			synchronized (monitor) {
				try {
					LOG.info("Cleaning up volume: " + volume.getVolumeId());
					String path = lvmRootDirectory + PATH_SEPARATOR + volume.getVgName() + PATH_SEPARATOR + volume.getLvName();
					try {						
						if(LVMWrapper.logicalVolumeExists(path)) {
							//guard this. tgt is not happy when you ask it to
							//get rid of a non existent tid
							LOG.debug("Found logical volume at " + path + " for " + volume.getVolumeId() + ". Now cleaning up");
							exportManager.cleanup(volume);
						} else {
							LOG.debug("Failed to find logical volume at " + path + " for " + volume.getVolumeId() + ". Skipping cleanup for tgt and lvm");
						}
						//volumeManager.finish();
					} catch (EucalyptusCloudException ee) {
						LOG.error("Error cleaning up volume iscsi state " + volume.getVolumeId(), ee);
						//volumeManager.abort();
						throw ee;
					} finally {
						volumeManager.finish();
					}
					
					volumeManager = new VolumeEntityWrapperManager();
					try {
						volume= volumeManager.getVolumeInfo(volumeId);
						String loDevName = volume.getLoDevName();
						if (loDevName != null) {
							if (volume != null) {
								if (!volumeManager.areSnapshotsPending(volume.getVolumeId())) {							
									LVMWrapper.disableLogicalVolume(path);
									LOG.info("Detaching loop device: " + loDevName + " for volume " + volume.getVolumeId());
									removeLoopback(loDevName);
									volume.setLoDevName(null);
									LOG.info("Done cleaning up: " + volume.getVolumeId());
									volume.setCleanup(false);									
								} else {
									LOG.info("Snapshot in progress. Not detaching loop device.");
									//volumeManager.abort();
								}
							}
						}
					} catch (EucalyptusCloudException e) {
						LOG.error(e, e);
						throw e;
					} finally {
						volumeManager.finish();
					}
				} finally {
					//Release any waiting
					monitor.notifyAll();
				}
			} // synchronized
		} catch (Exception ex) {
			LOG.error("Error unexporting " + volumeId + " from all hosts", ex);
			throw new EucalyptusCloudException(ex);
		}
	}
	
}
