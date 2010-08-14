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
 * Author: Neil Soman neil@eucalyptus.com
 */

package com.eucalyptus.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.persistence.EntityNotFoundException;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.auth.Authentication;
import com.eucalyptus.auth.SystemCredentialProvider;
import com.eucalyptus.auth.X509Cert;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.config.StorageControllerBuilder;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.ExecutionException;
import com.eucalyptus.util.StorageProperties;
import com.eucalyptus.util.WalrusProperties;

import edu.ucsb.eucalyptus.cloud.entities.AOEVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.DirectStorageInfo;
import edu.ucsb.eucalyptus.cloud.entities.ISCSIVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.LVMVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.StorageInfo;
import edu.ucsb.eucalyptus.cloud.entities.VolumeInfo;
import edu.ucsb.eucalyptus.msgs.ComponentProperty;
import edu.ucsb.eucalyptus.util.StreamConsumer;
import edu.ucsb.eucalyptus.util.SystemUtil;

public class OverlayManager implements LogicalStorageManager {

	public static final String lvmRootDirectory = "/dev";
	public static final String PATH_SEPARATOR = File.separator;
	public static boolean initialized = false;
	public static final int MAX_LOOP_DEVICES = 256;
	public static final String EUCA_VAR_RUN_PATH = "/var/run/eucalyptus";
	private static Logger LOG = Logger.getLogger(OverlayManager.class);
	public static String eucaHome = System.getProperty("euca.home");
	private static final long LVM_HEADER_LENGTH = 4 * StorageProperties.MB;
	public static StorageExportManager exportManager;

	public static String iface = "eth0";
	public static boolean zeroFillVolumes = false;

	public void checkPreconditions() throws EucalyptusCloudException {
		//check if binaries exist, commands can be executed, etc.
		String eucaHomeDir = System.getProperty("euca.home");
		if(eucaHomeDir == null) {
			throw new EucalyptusCloudException("euca.home not set");
		}
		eucaHome = eucaHomeDir;
		if(!new File(eucaHome + StorageProperties.EUCA_ROOT_WRAPPER).exists()) {
			throw new EucalyptusCloudException("root wrapper (euca_rootwrap) does not exist in " + eucaHome + StorageProperties.EUCA_ROOT_WRAPPER);
		}
		File varDir = new File(eucaHome + EUCA_VAR_RUN_PATH);
		if(!varDir.exists()) {
			varDir.mkdirs();
		}
		try {
			String returnValue = getLvmVersion();
			if(returnValue.length() == 0) {
				throw new EucalyptusCloudException("Is lvm installed?");
			} else {
				LOG.info(returnValue);
			}
			if(System.getProperty("euca.disable.iscsi") != null) {
				exportManager = new AOEManager();
			} else {
				exportManager = new ISCSIManager();
			}
			exportManager.checkPreconditions();
		} catch(ExecutionException ex) {
			String error = "Unable to run command: " + ex.getMessage();
			LOG.error(error);
			throw new EucalyptusCloudException(error);
		}
	}

	private String getLvmVersion() throws ExecutionException {
		return SystemUtil.run(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "lvm", "version"});
	}

	private String findFreeLoopback() throws ExecutionException {
		return SystemUtil.run(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "losetup", "-f"}).replaceAll("\n", "");
	}

	private  String getLoopback(String loDevName) throws ExecutionException {
		return SystemUtil.run(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "losetup", loDevName});
	}

	private String createPhysicalVolume(String loDevName) throws ExecutionException {
		return SystemUtil.run(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "pvcreate", loDevName});
	}

	private String createVolumeGroup(String pvName, String vgName) throws ExecutionException {
		return SystemUtil.run(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "vgcreate", vgName, pvName});
	}

	private String extendVolumeGroup(String pvName, String vgName) throws ExecutionException {
		return SystemUtil.run(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "vgextend", vgName, pvName});
	}

	private String scanVolumeGroups() throws ExecutionException {
		return SystemUtil.run(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "vgscan"});
	}

	private String createLogicalVolume(String vgName, String lvName) throws ExecutionException {
		return SystemUtil.run(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "lvcreate", "-n", lvName, "-l", "100%FREE", vgName});
	}

	private String createSnapshotLogicalVolume(String lvName, String snapLvName) throws ExecutionException {
		return SystemUtil.run(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "lvcreate", "-n", snapLvName, "-s", "-l", "100%FREE", lvName});
	}

	private String removeLogicalVolume(String lvName) throws ExecutionException {
		return SystemUtil.run(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "lvremove", "-f", lvName});
	}

	private String removeVolumeGroup(String vgName) throws ExecutionException {
		return SystemUtil.run(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "vgremove", vgName});
	}

	private String removePhysicalVolume(String loDevName) throws ExecutionException {
		return SystemUtil.run(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "pvremove", loDevName});
	}

	private String removeLoopback(String loDevName) throws ExecutionException {
		return SystemUtil.run(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "losetup", "-d", loDevName});
	}

	private String reduceVolumeGroup(String vgName, String pvName) throws ExecutionException {
		return SystemUtil.run(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "vgreduce", vgName, pvName});
	}

	private String enableLogicalVolume(String lvName) throws ExecutionException {
		return SystemUtil.run(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "lvchange", "-ay", lvName});
	}

	private int losetup(String absoluteFileName, String loDevName) {
		try
		{
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "losetup", loDevName, absoluteFileName});
			StreamConsumer error = new StreamConsumer(proc.getErrorStream());
			StreamConsumer output = new StreamConsumer(proc.getInputStream());
			error.start();
			output.start();
			int errorCode = proc.waitFor();
			output.join();
			LOG.info("losetup " + loDevName + " " + absoluteFileName);
			LOG.info(output.getReturnValue());
			LOG.info(error.getReturnValue());
			return errorCode;
		} catch (Throwable t) {
			LOG.error(t);
		}
		return -1;
	}

	private String duplicateLogicalVolume(String oldLvName, String newLvName) throws ExecutionException {
		return SystemUtil.run(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "dd", "if=" + oldLvName, "of=" + newLvName, "bs=" + StorageProperties.blockSize});
	}

	private String createFile(String fileName, long size) throws ExecutionException {
		if(!DirectStorageInfo.getStorageInfo().getZeroFillVolumes())
			return SystemUtil.run(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "dd", "if=/dev/zero", "of=" + fileName, "count=1", "bs=" + StorageProperties.blockSize, "seek=" + (size -1)});
		else
			return SystemUtil.run(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "dd", "if=/dev/zero", "of=" + fileName, "count=" + size, "bs=" + StorageProperties.blockSize});
	}

	private String createEmptyFile(String fileName, int size) throws ExecutionException {
		long fileSize = size * 1024;
		return createFile(fileName, fileSize);
	}

	public String createAbsoluteEmptyFile(String fileName, long size) throws ExecutionException {
		size = size / WalrusProperties.M;
		return createFile(fileName, size);
	}


	public void initialize() {
		if(!initialized) {
			System.loadLibrary("lvm2control");
			registerSignals();
			File storageRootDir = new File(getStorageRootDirectory());
			if(!storageRootDir.exists()) {
				if(!storageRootDir.mkdirs()) {
					LOG.fatal("Unable to make volume root directory: " + getStorageRootDirectory());
				}
			}
			initialized = true;
		}
	}

	public void configure() {
		exportManager.configure();
		//First call to StorageInfo.getStorageInfo will add entity if it does not exist
		LOG.info(StorageInfo.getStorageInfo().getName());
		checkVolumesDir();
	}

	public void startupChecks() {
		reload();
	}

	private void checkVolumesDir() {
		File volumeDir = new File(DirectStorageInfo.getStorageInfo().getVolumesDir());
		if(!volumeDir.exists()) {
			if(!volumeDir.mkdirs()) {
				LOG.fatal("Unable to make volume root directory: " + DirectStorageInfo.getStorageInfo().getVolumesDir());
			}
		} else if(!volumeDir.canWrite()) {
			LOG.fatal("Cannot write to volume root directory: " + DirectStorageInfo.getStorageInfo().getVolumesDir());
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
				String returnValue = removeLogicalVolume(absoluteLVName);
				returnValue = removeVolumeGroup(vgName);
				returnValue = removePhysicalVolume(loDevName);
				removeLoopback(loDevName);
			} catch(ExecutionException ex) {
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

	public String createDuplicateLoopback(String oldRawFileName, String rawFileName) throws EucalyptusCloudException, ExecutionException {
		dupFile(oldRawFileName, rawFileName);
		return createLoopback(rawFileName);
	}

	public String createLoopback(String fileName, int size) throws EucalyptusCloudException, ExecutionException {
		createEmptyFile(fileName, size);
		if(!(new File(fileName).exists()))
			throw new EucalyptusCloudException("Unable to create file " + fileName);
		return createLoopback(fileName);
	}

	public synchronized String createLoopback(String fileName) throws EucalyptusCloudException, ExecutionException {
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

	public String createLoopback(String fileName, long size) throws EucalyptusCloudException, ExecutionException {
		createAbsoluteEmptyFile(fileName, size);
		if(!(new File(fileName).exists()))
			throw new EucalyptusCloudException("Unable to create file " + fileName);
		return createLoopback(fileName);
	}

	//creates a logical volume (and a new physical volume and volume group)
	public void createLogicalVolume(String loDevName, String vgName, String lvName) throws EucalyptusCloudException, ExecutionException {
		String returnValue = createPhysicalVolume(loDevName);
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("Unable to create physical volume for " + loDevName);
		}
		returnValue = createVolumeGroup(loDevName, vgName);
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("Unable to create volume group " + vgName + " for " + loDevName);
		}
		returnValue = createLogicalVolume(vgName, lvName);
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("Unable to create logical volume " + lvName + " in volume group " + vgName);
		}
	}

	public  void createSnapshotLogicalVolume(String loDevName, String vgName, String lvName, String snapLvName) throws EucalyptusCloudException, ExecutionException {
		String returnValue = createPhysicalVolume(loDevName);
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("Unable to create physical volume for " + loDevName);
		}
		returnValue = extendVolumeGroup(loDevName, vgName);
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("Unable to extend volume group " + vgName + " for " + loDevName);
		}
		returnValue = createSnapshotLogicalVolume(lvName, snapLvName);
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("Unable to create snapshot logical volume " + snapLvName + " for volume " + lvName);
		}
	}

	public void createVolume(String volumeId, int size) throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();

		String vgName = "vg-" + Hashes.getRandom(4);
		String lvName = "lv-" + Hashes.getRandom(4);
		LVMVolumeInfo lvmVolumeInfo = null;
		if(exportManager instanceof AOEManager) {
			lvmVolumeInfo = new AOEVolumeInfo();
		} else {
			lvmVolumeInfo = new ISCSIVolumeInfo();
		}
		volumeManager.finish();
		String rawFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + volumeId;
		//create file and attach to loopback device
		long absoluteSize = size * StorageProperties.GB + LVM_HEADER_LENGTH;
		try {
			String loDevName = createLoopback(rawFileName, absoluteSize);
			//create physical volume, volume group and logical volume
			createLogicalVolume(loDevName, vgName, lvName);
			//export logical volume
			try {
				volumeManager.exportVolume(lvmVolumeInfo, vgName, lvName);
			} catch(EucalyptusCloudException ex) {
				LOG.error(ex);
				String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;
				String returnValue = removeLogicalVolume(absoluteLVName);
				returnValue = removeVolumeGroup(vgName);
				returnValue = removePhysicalVolume(loDevName);
				removeLoopback(loDevName);
				throw ex;
			}
			lvmVolumeInfo.setVolumeId(volumeId);
			lvmVolumeInfo.setLoDevName(loDevName);
			lvmVolumeInfo.setPvName(loDevName);
			lvmVolumeInfo.setVgName(vgName);
			lvmVolumeInfo.setLvName(lvName);
			lvmVolumeInfo.setStatus(StorageProperties.Status.available.toString());
			lvmVolumeInfo.setSize(size);
			volumeManager = new VolumeEntityWrapperManager();
			volumeManager.add(lvmVolumeInfo);
			volumeManager.finish();
		} catch(ExecutionException ex) {
			String error = "Unable to run command: " + ex.getMessage();
			volumeManager.abort();
			LOG.error(error);
			throw new EucalyptusCloudException(error);
		}
	}

	public int createVolume(String volumeId, String snapshotId, int size) throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo foundSnapshotInfo = volumeManager.getVolumeInfo(snapshotId);
		if(foundSnapshotInfo != null) {
			String status = foundSnapshotInfo.getStatus();
			if(status.equals(StorageProperties.Status.available.toString())) {
				String vgName = "vg-" + Hashes.getRandom(4);
				String lvName = "lv-" + Hashes.getRandom(4);
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
					if(size > 0) {
						absoluteSize = size * StorageProperties.GB + LVM_HEADER_LENGTH;	
					} else {
						size = (int)(snapshotFile.length() / StorageProperties.GB);
						absoluteSize = snapshotFile.length() + LVM_HEADER_LENGTH;
					}
					String loDevName = createLoopback(rawFileName, absoluteSize);
					//create physical volume, volume group and logical volume
					createLogicalVolume(loDevName, vgName, lvName);
					//duplicate snapshot volume
					String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;
					duplicateLogicalVolume(loFileName, absoluteLVName);
					//export logical volume
					try {
						volumeManager.exportVolume(lvmVolumeInfo, vgName, lvName);
					} catch(EucalyptusCloudException ex) {
						String returnValue = removeLogicalVolume(absoluteLVName);
						returnValue = removeVolumeGroup(vgName);
						returnValue = removePhysicalVolume(loDevName);
						removeLoopback(loDevName);
						throw ex;
					}
					lvmVolumeInfo.setVolumeId(volumeId);
					lvmVolumeInfo.setLoDevName(loDevName);
					lvmVolumeInfo.setPvName(loDevName);
					lvmVolumeInfo.setVgName(vgName);
					lvmVolumeInfo.setLvName(lvName);
					lvmVolumeInfo.setStatus(StorageProperties.Status.available.toString());
					lvmVolumeInfo.setSize(size);
					volumeManager = new VolumeEntityWrapperManager();
					volumeManager.add(lvmVolumeInfo);
					volumeManager.finish();
				}  catch(ExecutionException ex) {
					volumeManager.abort();
					String error = "Unable to run command: " + ex.getMessage();
					LOG.error(error);
					throw new EucalyptusCloudException(error);
				}
			}
		} else {
			volumeManager.abort();
			throw new EucalyptusCloudException("Unable to find snapshot: " + snapshotId);
		}
		return size;
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
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo foundLVMVolumeInfo = volumeManager.getVolumeInfo(volumeId);
		if(foundLVMVolumeInfo != null) {
			//remove aoe export
			String loDevName = foundLVMVolumeInfo.getLoDevName();
			String vgName = foundLVMVolumeInfo.getVgName();
			String lvName = foundLVMVolumeInfo.getLvName();
			String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;
			volumeManager.unexportVolume(foundLVMVolumeInfo);
			try {
				String returnValue = removeLogicalVolume(absoluteLVName);
				if(returnValue.length() == 0) {
					throw new EucalyptusCloudException("Unable to remove logical volume " + absoluteLVName);
				}
				returnValue = removeVolumeGroup(vgName);
				if(returnValue.length() == 0) {
					throw new EucalyptusCloudException("Unable to remove volume group " + vgName);
				}
				returnValue = removePhysicalVolume(loDevName);
				if(returnValue.length() == 0) {
					throw new EucalyptusCloudException("Unable to remove physical volume " + loDevName);
				}
				returnValue = removeLoopback(loDevName);
				File rawFile = new File(DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + volumeId);
				if (rawFile.exists()) {
					if(!rawFile.delete()) {
						throw new EucalyptusCloudException("Unable to delete: " + rawFile.getAbsolutePath());
					}
				}
				volumeManager.remove(foundLVMVolumeInfo);				
				volumeManager.finish();
			} catch(ExecutionException ex) {
				volumeManager.abort();
				String error = "Unable to run command: " + ex.getMessage();
				LOG.error(error);
				throw new EucalyptusCloudException(error);
			}
		}  else {
			volumeManager.abort();
			throw new EucalyptusCloudException("Unable to find volume: " + volumeId);
		}
	}


	public List<String> createSnapshot(String volumeId, String snapshotId) throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo foundLVMVolumeInfo = volumeManager.getVolumeInfo(volumeId);
		ArrayList<String> returnValues = new ArrayList<String>();
		if(foundLVMVolumeInfo != null) {
			LVMVolumeInfo snapshotInfo = volumeManager.getVolumeInfo();
			snapshotInfo.setVolumeId(snapshotId);
			String vgName = foundLVMVolumeInfo.getVgName();
			String lvName = "lv-snap-" + Hashes.getRandom(4);
			String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + foundLVMVolumeInfo.getLvName();

			int size = foundLVMVolumeInfo.getSize();
			long snapshotSize = (size * StorageProperties.GB) / 2;
			String rawFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + volumeId + Hashes.getRandom(6);
			//create file and attach to loopback device
			volumeManager.finish();
			volumeManager = null;
			try {
				String loDevName = createLoopback(rawFileName, snapshotSize);
				//create physical volume, volume group and logical volume
				createSnapshotLogicalVolume(loDevName, vgName, absoluteLVName, lvName);

				String snapRawFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + snapshotId;
				String absoluteSnapLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;

				duplicateLogicalVolume(absoluteSnapLVName, snapRawFileName);

				String returnValue = removeLogicalVolume(absoluteSnapLVName);
				if(returnValue.length() == 0) {
					throw new EucalyptusCloudException("Unable to remove logical volume " + absoluteSnapLVName);
				}
				returnValue = reduceVolumeGroup(vgName, loDevName);
				if(returnValue.length() == 0) {
					throw new EucalyptusCloudException("Unable to reduce volume group " + vgName + " logical volume: " + loDevName);
				}
				returnValue = removePhysicalVolume(loDevName);
				if(returnValue.length() == 0) {
					throw new EucalyptusCloudException("Unable to remove physical volume " + loDevName);
				}
				returnValue = removeLoopback(loDevName);
				if(!(new File(rawFileName)).delete()) {
					LOG.error("Unable to remove temporary snapshot file: " + rawFileName);
				}
				snapshotInfo.setLoFileName(snapRawFileName);
				snapshotInfo.setStatus(StorageProperties.Status.available.toString());
				snapshotInfo.setSize(size);
				returnValues.add(snapRawFileName);
				returnValues.add(String.valueOf(size * WalrusProperties.G));
				volumeManager = new VolumeEntityWrapperManager();
				volumeManager.add(snapshotInfo);
				volumeManager.finish();
			} catch(ExecutionException ex) {
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
			if (snapFile.exists()) {
				if(!snapFile.delete()) {
					throw new EucalyptusCloudException("Unable to delete: " + snapFile.getAbsolutePath());
				}
			}
		}  else {
			volumeManager.abort();
			throw new EucalyptusCloudException("Unable to find snapshot: " + snapshotId);
		}
		volumeManager.finish();
	}

	public String getVolumeProperty(String volumeId) throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		String returnValue = volumeManager.getVolumeProperty(volumeId);
		volumeManager.finish();
		return returnValue;
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
			} catch(ExecutionException ex) {
				volumeManager.abort();
				String error = "Unable to run command: " + ex.getMessage();
				LOG.error(error);
				throw new EucalyptusCloudException(error);
			}

		}
		volumeManager.finish();
	}

	public void reload() {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		List<LVMVolumeInfo> volumeInfos = volumeManager.getAllVolumeInfos();
		for(LVMVolumeInfo foundVolumeInfo : volumeInfos) {
			String loDevName = foundVolumeInfo.getLoDevName();
			if(loDevName != null) {
				String loFileName = foundVolumeInfo.getVolumeId();
				String absoluteLoFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + PATH_SEPARATOR + loFileName;
				if(!new File(absoluteLoFileName).exists()) {
					LOG.error("Backing volume: " + absoluteLoFileName + " not found. Invalidating volume."); 
					foundVolumeInfo.setStatus(StorageProperties.Status.failed.toString());
					continue;
				}
				try {
					String returnValue = getLoopback(loDevName);
					if(returnValue.length() <= 0) {
						createLoopback(absoluteLoFileName, loDevName);
					}
				} catch(ExecutionException ex) {
					String error = "Unable to run command: " + ex.getMessage();
					LOG.error(error);
				}
			}
		}
		//now enable them
		try {
			scanVolumeGroups();
		} catch (ExecutionException e) {
			LOG.error(e);
		}
		for(LVMVolumeInfo foundVolumeInfo : volumeInfos) {
			try {
				LOG.info("Scanning volume groups. This might take a little while...");
				volumeManager.exportVolume(foundVolumeInfo);
			} catch(EucalyptusCloudException ex) {
				LOG.error("Unable to reload volume: " + foundVolumeInfo.getVolumeId() + ex);
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

	private String aoeStatus(int pid) {
		File file = new File("/proc/" + pid + "/cmdline");
		String returnString = "";
		if(file.exists()) {
			FileInputStream fileIn = null;
			try {
				fileIn = new FileInputStream(file);
				byte[] bytes = new byte[512];
				int bytesRead;
				while((bytesRead = fileIn.read(bytes)) > 0) {
					returnString += new String(bytes, 0, bytesRead);
				}
			} catch (Exception ex) {
				LOG.warn("could not find " + file.getAbsolutePath());
			} finally {
				if(fileIn != null)
					try {
						fileIn.close();
					} catch (IOException e) {
						LOG.error(e);
					}
			}
		}
		return returnString;
	}

	private class VolumeEntityWrapperManager {
		private EntityWrapper entityWrapper;

		private VolumeEntityWrapperManager() {
			entityWrapper = StorageProperties.getEntityWrapper();
		}

		public List<String> getSnapshotValues(String snapshotId) {
			ArrayList<String> returnValues = new ArrayList<String>();
			LVMVolumeInfo lvmVolumeInfo = getVolumeInfo(snapshotId);
			if(lvmVolumeInfo instanceof AOEVolumeInfo) {
				returnValues.add(lvmVolumeInfo.getVgName());
				returnValues.add(lvmVolumeInfo.getLvName());
			}
			return returnValues;
		}

		public void exportVolume(LVMVolumeInfo lvmVolumeInfo) throws EucalyptusCloudException {
			if(exportManager instanceof AOEManager) {
				if(lvmVolumeInfo instanceof AOEVolumeInfo) {
					AOEVolumeInfo aoeVolumeInfo = (AOEVolumeInfo) lvmVolumeInfo;

					int pid = aoeVolumeInfo.getVbladePid();
					if(pid > 0) {
						//enable logical volumes
						String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + aoeVolumeInfo.getVgName() + PATH_SEPARATOR + aoeVolumeInfo.getLvName();
						try {
							enableLogicalVolume(absoluteLVName);
						} catch(ExecutionException ex) {
							String error = "Unable to run command: " + ex.getMessage();
							LOG.error(error);
							return;
						}
						String returnValue = aoeStatus(pid);
						if(returnValue.length() == 0) {
							int majorNumber = aoeVolumeInfo.getMajorNumber();
							int minorNumber = aoeVolumeInfo.getMinorNumber();
							pid = exportManager.exportVolume(DirectStorageInfo.getStorageInfo().getStorageInterface(), absoluteLVName, majorNumber, minorNumber);
							aoeVolumeInfo.setVbladePid(pid);
							File vbladePidFile = new File(eucaHome + EUCA_VAR_RUN_PATH + "/vblade-" + majorNumber + minorNumber + ".pid");
							FileOutputStream fileOutStream = null;
							try {
								fileOutStream = new FileOutputStream(vbladePidFile);
								String pidString = String.valueOf(pid);
								fileOutStream.write(pidString.getBytes());
								fileOutStream.close();
							} catch (Exception ex) {
								if(fileOutStream != null)
									try {
										fileOutStream.close();
									} catch (IOException e) {
										LOG.error(e);
									}
									LOG.error("Could not write pid file vblade-" + majorNumber + minorNumber + ".pid");
							}
						}
					}
				} else {
					//convert it
					AOEVolumeInfo volumeInfo = new AOEVolumeInfo();
					convertVolumeInfo(lvmVolumeInfo, volumeInfo);
					try {
						unexportVolume(lvmVolumeInfo);
						exportVolume(volumeInfo, volumeInfo.getVgName(), volumeInfo.getLvName());
						add(volumeInfo);
						remove(lvmVolumeInfo);
					} catch(EucalyptusCloudException ex) {
						LOG.error(ex);
					}
				}
			} else if(exportManager instanceof ISCSIManager) {
				if(lvmVolumeInfo instanceof ISCSIVolumeInfo) {
					ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) lvmVolumeInfo;
					String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + iscsiVolumeInfo.getVgName() + PATH_SEPARATOR + iscsiVolumeInfo.getLvName();
					((ISCSIManager)exportManager).exportTarget(iscsiVolumeInfo.getTid(), iscsiVolumeInfo.getStoreName(), iscsiVolumeInfo.getLun(), absoluteLVName, iscsiVolumeInfo.getStoreUser());
				} else {
					ISCSIVolumeInfo volumeInfo = new ISCSIVolumeInfo();
					convertVolumeInfo(lvmVolumeInfo, volumeInfo);
					try {
						unexportVolume(lvmVolumeInfo);
						exportVolume(volumeInfo, volumeInfo.getVgName(), volumeInfo.getLvName());
						add(volumeInfo);
						remove(lvmVolumeInfo);
					} catch(EucalyptusCloudException ex) {
						LOG.error(ex);
					}
				}
			}	
		}

		private void convertVolumeInfo(LVMVolumeInfo lvmVolumeSource, LVMVolumeInfo lvmVolumeDestination) {
			lvmVolumeDestination.setScName(lvmVolumeSource.getScName());
			lvmVolumeDestination.setLoFileName(lvmVolumeSource.getLoFileName());
			lvmVolumeDestination.setLoDevName(lvmVolumeSource.getLoDevName());
			lvmVolumeDestination.setLvName(lvmVolumeSource.getLvName());
			lvmVolumeDestination.setVgName(lvmVolumeSource.getVgName());
			lvmVolumeDestination.setPvName(lvmVolumeSource.getPvName());
			lvmVolumeDestination.setSize(lvmVolumeSource.getSize());
			lvmVolumeDestination.setSnapshotOf(lvmVolumeSource.getSnapshotOf());
			lvmVolumeDestination.setStatus(lvmVolumeSource.getStatus());
			lvmVolumeDestination.setVolumeId(lvmVolumeSource.getVolumeId());			
		}

		public String getVolumeProperty(String volumeId) {
			LVMVolumeInfo lvmVolumeInfo = getVolumeInfo(volumeId);
			if(lvmVolumeInfo != null) {
				if(exportManager instanceof AOEManager) {
					AOEVolumeInfo aoeVolumeInfo = (AOEVolumeInfo) lvmVolumeInfo;
					return StorageProperties.ETHERD_PREFIX + aoeVolumeInfo.getMajorNumber() + "." + aoeVolumeInfo.getMinorNumber();
				} else if(exportManager instanceof ISCSIManager) {
					ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) lvmVolumeInfo;
					String storeName = iscsiVolumeInfo.getStoreName();
					String encryptedPassword;
					try {
						encryptedPassword = ((ISCSIManager)exportManager).getEncryptedPassword();
					} catch (EucalyptusCloudException e) {
						LOG.error(e);
						return null;
					}
					return System.getProperty("euca.home") + "," + StorageProperties.STORAGE_HOST + "," + storeName + "," + encryptedPassword;
				}
			}
			return null;
		}

		public void unexportVolume(LVMVolumeInfo volumeInfo) {
			StorageExportManager manager = exportManager;
			if(volumeInfo instanceof AOEVolumeInfo) {
				AOEVolumeInfo aoeVolumeInfo = (AOEVolumeInfo) volumeInfo;
				if(!(exportManager instanceof AOEManager)) {
					manager = new AOEManager();
				}
				int pid = aoeVolumeInfo.getVbladePid();
				if(pid > 0) {
					String returnValue = aoeStatus(pid);
					if(returnValue.length() > 0) {
						manager.unexportVolume(pid);
						int majorNumber = aoeVolumeInfo.getMajorNumber();
						int minorNumber = aoeVolumeInfo.getMinorNumber();
						File vbladePidFile = new File(eucaHome + EUCA_VAR_RUN_PATH + "/vblade-" + majorNumber + minorNumber + ".pid");
						if(vbladePidFile.exists()) {
							vbladePidFile.delete();
						}
					}
				}
			} else if(volumeInfo instanceof ISCSIVolumeInfo) {
				if(!(exportManager instanceof ISCSIManager)) {
					manager = new ISCSIManager();
				}
				ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) volumeInfo;
				((ISCSIManager)manager).unexportTarget(iscsiVolumeInfo.getTid(), iscsiVolumeInfo.getLun());
			}			
		}

		private void finish() {
			entityWrapper.commit();
		}

		private void abort() {
			entityWrapper.rollback();
		}


		private LVMVolumeInfo getVolumeInfo(String volumeId) {
			if(exportManager instanceof AOEManager) {
				AOEVolumeInfo AOEVolumeInfo = new AOEVolumeInfo(volumeId);
				List<AOEVolumeInfo> AOEVolumeInfos = entityWrapper.query(AOEVolumeInfo);
				if(AOEVolumeInfos.size() > 0) {
					return AOEVolumeInfos.get(0);
				}
			} else if(exportManager instanceof ISCSIManager) {
				ISCSIVolumeInfo ISCSIVolumeInfo = new ISCSIVolumeInfo(volumeId);
				List<ISCSIVolumeInfo> ISCSIVolumeInfos = entityWrapper.query(ISCSIVolumeInfo);
				if(ISCSIVolumeInfos.size() > 0) {
					return ISCSIVolumeInfos.get(0);
				}
			}
			return null;
		}

		private LVMVolumeInfo getVolumeInfo() {
			if(exportManager instanceof AOEManager) {
				AOEVolumeInfo aoeVolumeInfo = new AOEVolumeInfo();
				aoeVolumeInfo.setVbladePid(-1);
				aoeVolumeInfo.setMajorNumber(-1);
				aoeVolumeInfo.setMinorNumber(-1);
				return aoeVolumeInfo;
			} else if(exportManager instanceof ISCSIManager) {
				return new ISCSIVolumeInfo();
			}
			return null;
		}

		private List<LVMVolumeInfo> getAllVolumeInfos() {
			List<LVMVolumeInfo> volumeInfos = new ArrayList<LVMVolumeInfo>();
			volumeInfos.addAll(entityWrapper.query(new AOEVolumeInfo()));
			volumeInfos.addAll(entityWrapper.query(new ISCSIVolumeInfo()));	
			return volumeInfos;
		}

		private void add(LVMVolumeInfo volumeInfo) {
			entityWrapper.add(volumeInfo);
		}

		private void remove(LVMVolumeInfo volumeInfo) {
			entityWrapper.delete(volumeInfo);
		}

		private String encryptTargetPassword(String password) throws EucalyptusCloudException {
			EntityWrapper<ClusterCredentials> credDb = Authentication.getEntityWrapper( );
			try {
				ClusterCredentials credentials = credDb.getUnique( new ClusterCredentials( StorageProperties.NAME ) );
				PublicKey ncPublicKey = X509Cert.toCertificate(credentials.getNodeCertificate()).getPublicKey();
				credDb.commit();
				Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				cipher.init(Cipher.ENCRYPT_MODE, ncPublicKey);
				return new String(Base64.encode(cipher.doFinal(password.getBytes())));	      
			} catch ( Exception e ) {
				LOG.error( "Unable to encrypt storage target password" );
				credDb.rollback( );
				throw new EucalyptusCloudException(e.getMessage(), e);
			}
		}

		private int exportVolume(LVMVolumeInfo lvmVolumeInfo, String vgName, String lvName) throws EucalyptusCloudException {
			if(exportManager instanceof AOEManager) {
				AOEVolumeInfo aoeVolumeInfo = (AOEVolumeInfo) lvmVolumeInfo;
				exportManager.allocateTarget(aoeVolumeInfo);
				int majorNumber = aoeVolumeInfo.getMajorNumber();
				int minorNumber = aoeVolumeInfo.getMinorNumber();
				String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;
				int pid = exportManager.exportVolume(DirectStorageInfo.getStorageInfo().getStorageInterface(), absoluteLVName, majorNumber, minorNumber);
				boolean success = false;
				String returnValue = "";
				int timeout = 300;
				if(pid > 0) {
					for(int i=0; i < 3; ++i) {
						returnValue = aoeStatus(pid);						
						if(returnValue.length() == 0) {
							success = false;
						} else {
							success = true;
						}
						try {
							Thread.sleep(timeout);
						} catch(InterruptedException ie) {
							LOG.error(ie);
						}
					}
				}
				if(!success) {
					throw new EucalyptusCloudException("Could not export AoE device " + absoluteLVName + " StorageInfo.getStorageInfo().getStorageInterface(): " + DirectStorageInfo.getStorageInfo().getStorageInterface() + " pid: " + pid + " returnValue: " + returnValue);
				}

				File vbladePidFile = new File(eucaHome + EUCA_VAR_RUN_PATH + "/vblade-" + majorNumber + minorNumber + ".pid");
				FileOutputStream fileOutStream = null;
				try {
					fileOutStream = new FileOutputStream(vbladePidFile);
					String pidString = String.valueOf(pid);
					fileOutStream.write(pidString.getBytes());
				} catch (Exception ex) {
					LOG.error("Could not write pid file vblade-" + majorNumber + minorNumber + ".pid");
				} finally {
					if(fileOutStream != null)
						try {
							fileOutStream.close();
						} catch (IOException e) {
							LOG.error(e);
						}
				}
				if(pid < 0)
					throw new EucalyptusCloudException("invalid vblade pid: " + pid);
				aoeVolumeInfo.setVbladePid(pid);
				return pid;
			} else if(exportManager instanceof ISCSIManager) {
				ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) lvmVolumeInfo;
				exportManager.allocateTarget(iscsiVolumeInfo);
				String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;
				((ISCSIManager)exportManager).exportTarget(iscsiVolumeInfo.getTid(), iscsiVolumeInfo.getStoreName(), iscsiVolumeInfo.getLun(), absoluteLVName, iscsiVolumeInfo.getStoreUser());
			}
			return 0;
		}
	}

	@Override
	public void finishVolume(String snapshotId) throws EucalyptusCloudException{
		//Nothing to do here
	}

	@Override
	public String prepareSnapshot(String snapshotId, int sizeExpected)
	throws EucalyptusCloudException {
		return DirectStorageInfo.getStorageInfo().getVolumesDir() + File.separator + snapshotId;
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
			try {
				SystemUtil.run(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, 
						"dd", "if=" + volumePath, 
						"of=" + lvmRootDirectory + File.separator + volumeInfo.getVgName() + 
						File.separator + volumeInfo.getLvName(), "bs=" + StorageProperties.blockSize});			
			} catch (ExecutionException e) {
				LOG.error(e);
				throw new EucalyptusCloudException(e);
			}
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
		try {
			SystemUtil.run(new String[]{eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, 
					"dd", "if=" + snapPath, 
					"of=" + snapFileName, "bs=" + StorageProperties.blockSize});			
		} catch (ExecutionException e) {
			LOG.error(e);
			throw new EucalyptusCloudException(e);
		}
		volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo snapshotInfo = volumeManager.getVolumeInfo();
		snapshotInfo.setVolumeId(snapshotId);
		snapshotInfo.setLoFileName(snapFileName);
		snapshotInfo.setSize(size);
		snapshotInfo.setSnapshotOf(volumeId);
		volumeManager.add(snapshotInfo);
		volumeManager.finish();
	}
}
