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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.persistence.EntityNotFoundException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.auth.util.X509CertHelper;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.storage.StorageManagers.StorageManagerProperty;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.StorageProperties;
import com.eucalyptus.util.WalrusProperties;

import edu.ucsb.eucalyptus.cloud.entities.AOEVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.DASInfo;
import edu.ucsb.eucalyptus.cloud.entities.DirectStorageInfo;
import edu.ucsb.eucalyptus.cloud.entities.ISCSIVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.LVMVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.StorageInfo;
import edu.ucsb.eucalyptus.msgs.ComponentProperty;
import edu.ucsb.eucalyptus.util.SystemUtil;
import edu.ucsb.eucalyptus.util.SystemUtil.CommandOutput;

@StorageManagerProperty("das")
public class DASManager implements LogicalStorageManager {

	public static final String lvmRootDirectory = "/dev";
	public static final String PATH_SEPARATOR = "/";
	public static boolean initialized = false;
	public static final int MAX_LOOP_DEVICES = 256;
	public static final String EUCA_ROOT_WRAPPER = BaseDirectory.LIBEXEC.toString() + "/euca_rootwrap";
	public static final String EUCA_VAR_RUN_PATH = System.getProperty("euca.run.dir");
	private static Logger LOG = Logger.getLogger(DASManager.class);
	public static StorageExportManager exportManager;
	private static String volumeGroup;

	public void checkPreconditions() throws EucalyptusCloudException {
		//check if binaries exist, commands can be executed, etc.
		if(!new File(EUCA_ROOT_WRAPPER).exists()) {
			throw new EucalyptusCloudException("root wrapper (euca_rootwrap) does not exist in " + EUCA_ROOT_WRAPPER);
		}
		File varDir = new File(EUCA_VAR_RUN_PATH);
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
		} catch(EucalyptusCloudException ex) {
			String error = "Unable to run command: " + ex.getMessage();
			LOG.error(error);
			throw new EucalyptusCloudException(error);
		}
	}

	private void updateVolumeGroup() throws EucalyptusCloudException {
		if(volumeGroup == null) {
			String dasDevice = DASInfo.getStorageInfo().getDASDevice();
			if(dasDevice != null) {
				try {
					boolean volumeGroupFound = false;
					String returnValue = null;
					try {
						returnValue = getVolumeGroup(dasDevice);
						if(returnValue.length() > 0) {
							volumeGroupFound = true;
						}
					} catch(EucalyptusCloudException e) {
						LOG.warn(e);
					}
					if(volumeGroupFound) {
						Pattern volumeGroupPattern = Pattern.compile("(?s:.*VG Name)(.*)\n.*");
						Matcher m = volumeGroupPattern.matcher(returnValue);
						if(m.find()) 
							volumeGroup = m.group(1).trim();
						else
							throw new EucalyptusCloudException("Not a volume group: " + dasDevice);
					} else {
						boolean physicalVolumeGroupFound = false;
						try {
							returnValue = getPhysicalVolume(dasDevice);
							if(returnValue.matches("(?s:.*)PV Name.*" + dasDevice + "(?s:.*)")) {
								physicalVolumeGroupFound = true;
							}
						} catch(EucalyptusCloudException e) {
							LOG.warn(e);
						}
						if(!physicalVolumeGroupFound) {
							returnValue = createPhysicalVolume(dasDevice);
							if(returnValue.length() == 0) {
								throw new EucalyptusCloudException("Unable to create physical volume on device: " + dasDevice);
							}
						}
						//PV should be initialized at this point.
						returnValue = getPhysicalVolumeVerbose(dasDevice);
						if(returnValue.matches("(?s:.*)PV Name.*" + dasDevice + "(?s:.*)")) {
							Pattern volumeGroupPattern = Pattern.compile("(?s:.*VG Name)(.*)\n.*");
							Matcher m = volumeGroupPattern.matcher(returnValue);
							if(m.find()) { 
								volumeGroup = m.group(1).trim();
							}
							if((volumeGroup == null) || (volumeGroup.length() == 0)) {
								volumeGroup = "vg-" + Hashes.getRandom(10);
								returnValue = createVolumeGroup(dasDevice, volumeGroup);
								if(returnValue.length() == 0) {
									throw new EucalyptusCloudException("Unable to create volume group: " + volumeGroup + " physical volume: " + dasDevice);
								}
							}
						} else {
							Pattern volumeGroupPattern = Pattern.compile("(?s:.*VG Name)(.*)\n.*");
							Matcher m = volumeGroupPattern.matcher(returnValue);
							if(m.find()) 
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

	private String getLvmVersion() throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "lvm", "version"});
	}

	private String createPhysicalVolume(String loDevName) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "pvcreate", loDevName});
	}

	private String getPhysicalVolume(String partition) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "pvdisplay", partition});
	}

	private String getVolumeGroup(String volumeGroup) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "vgdisplay", volumeGroup});
	}

	private String createVolumeGroup(String pvName, String vgName) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "vgcreate", vgName, pvName});
	}

	private String getPhysicalVolumeVerbose(String pvName) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "pvdisplay", "-v", pvName});
	}

	private String createLogicalVolume(String vgName, String lvName, long size) throws EucalyptusCloudException {
		// return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "lvcreate", "-n", lvName, "-L", String.valueOf(size) + "G", vgName});
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "lvcreate", "-n", lvName, "-L", String.valueOf(size) + "M", vgName});
	}

	private String createSnapshotLogicalVolume(String lvName, String snapLvName, long size) throws EucalyptusCloudException {
		// return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "lvcreate", "-n", snapLvName, "-s", "-L", String.valueOf(size) + "G", lvName});
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "lvcreate", "-n", snapLvName, "-s", "-L", String.valueOf(size) + "M", lvName});
	}

	private String removeLogicalVolume(String lvName) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "lvremove", "-f", lvName});
	}

	private String enableLogicalVolume(String lvName) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "lvchange", "-ay", lvName});
	}

	private String duplicateLogicalVolume(String oldLvName, String newLvName) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "dd", "if=" + oldLvName, "of=" + newLvName, "bs=" + StorageProperties.blockSize});
	}

	private String createFile(String fileName, long size) throws EucalyptusCloudException {
		if(!DirectStorageInfo.getStorageInfo().getZeroFillVolumes())
			return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "dd", "if=/dev/zero", "of=" + fileName, "count=1", "bs=" + StorageProperties.blockSize, "seek=" + (size -1)});
		else
			return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "dd", "if=/dev/zero", "of=" + fileName, "count=" + size, "bs=" + StorageProperties.blockSize});
	}

	private String createEmptyFile(String fileName, int size) throws EucalyptusCloudException {
		long fileSize = size * 1024;
		return createFile(fileName, fileSize);
	}

	public String createAbsoluteEmptyFile(String fileName, long size) throws EucalyptusCloudException {
		size = size / WalrusProperties.M;
		return createFile(fileName, size);
	}


	public void initialize() {
		if(!initialized) {
			System.loadLibrary("dascontrol");
			registerSignals();
			initialized = true;
		}
	}

	public void configure() {
		exportManager.configure();
		//dummy init
		LOG.info(StorageInfo.getStorageInfo().getName());
		LOG.info(DirectStorageInfo.getStorageInfo().getName());
		LOG.info(DASInfo.getStorageInfo().getName());
	}

	public void startupChecks() {
		reload();
	}

	public void cleanVolume(String volumeId) {
		try {
			updateVolumeGroup();
		} catch (EucalyptusCloudException e) {
			LOG.error(e);
			return;
		}
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo lvmVolInfo = volumeManager.getVolumeInfo(volumeId);
		if(lvmVolInfo != null) {
			volumeManager.unexportVolume(lvmVolInfo);
			String lvName = lvmVolInfo.getLvName();
			String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + lvName;
			try {
				String returnValue = removeLogicalVolume(absoluteLVName);
			} catch(EucalyptusCloudException ex) {
				volumeManager.abort();
				String error = "Unable to run command: " + ex.getMessage();
				LOG.error(error);
			}
			volumeManager.remove(lvmVolInfo);
			volumeManager.finish();
		}
	}

	public void cleanSnapshot(String snapshotId) {
		try {
			updateVolumeGroup();
		} catch (EucalyptusCloudException e) {
			LOG.error(e);
			return;
		}
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo lvmVolInfo = volumeManager.getVolumeInfo(snapshotId);
		if(lvmVolInfo != null) {
			volumeManager.remove(lvmVolInfo);
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

	//creates a logical volume (and a new physical volume and volume group)
	public void createLogicalVolume(String lvName, long size) throws EucalyptusCloudException {
		if(volumeGroup != null) {
			String returnValue = createLogicalVolume(volumeGroup, lvName, size);
			if(returnValue.length() == 0) {
				throw new EucalyptusCloudException("Unable to create logical volume " + lvName + " in volume group " + volumeGroup);
			}
		} else {
			throw new EucalyptusCloudException("Volume group is null! This should never happen");
		}
	}

	public void createVolume(String volumeId, int size) throws EucalyptusCloudException {
		updateVolumeGroup();
		File volumeDir = new File(DirectStorageInfo.getStorageInfo().getVolumesDir());
		volumeDir.mkdirs();
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();

		String lvName = "lv-" + Hashes.getRandom(4);
		LVMVolumeInfo lvmVolumeInfo = null;
		if(exportManager instanceof AOEManager) {
			lvmVolumeInfo = new AOEVolumeInfo();
		} else {
			lvmVolumeInfo = new ISCSIVolumeInfo();
		}
		volumeManager.finish();
		//create file and attach to loopback device
		try {
			//create logical volume
			createLogicalVolume(lvName, (size * StorageProperties.KB));
			//export logical volume
			try {
				volumeManager.exportVolume(lvmVolumeInfo, volumeGroup, lvName);
			} catch(EucalyptusCloudException ex) {
				LOG.error(ex);
				String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + lvName;
				String returnValue = removeLogicalVolume(absoluteLVName);
				throw ex;
			}
			lvmVolumeInfo.setVolumeId(volumeId);
			lvmVolumeInfo.setLvName(lvName);
			lvmVolumeInfo.setStatus(StorageProperties.Status.available.toString());
			lvmVolumeInfo.setSize(size);
			volumeManager = new VolumeEntityWrapperManager();
			volumeManager.add(lvmVolumeInfo);
			volumeManager.finish();
		} catch(EucalyptusCloudException ex) {
			String error = "Unable to run command: " + ex.getMessage();
			volumeManager.abort();
			LOG.error(error);
			throw new EucalyptusCloudException(error);
		}
	}

	public int createVolume(String volumeId, String snapshotId, int size) throws EucalyptusCloudException {
		updateVolumeGroup();
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo foundSnapshotInfo = volumeManager.getVolumeInfo(snapshotId);
		if(foundSnapshotInfo != null) {
			String status = foundSnapshotInfo.getStatus();
			if(status.equals(StorageProperties.Status.available.toString())) {
				String lvName = "lv-" + Hashes.getRandom(4);
				LVMVolumeInfo lvmVolumeInfo = volumeManager.getVolumeInfo();
				String snapId = foundSnapshotInfo.getVolumeId();
				String loFileName = foundSnapshotInfo.getLoFileName();
				volumeManager.finish();
				try {
					File snapshotFile = new File(DirectStorageInfo.getStorageInfo().getVolumesDir() + PATH_SEPARATOR + snapId);
					assert(snapshotFile.exists());
					long absoluteSize;
					if (size <= 0 || size == foundSnapshotInfo.getSize()) {
						// size = (int)(snapshotFile.length() / StorageProperties.GB);
						absoluteSize = snapshotFile.length() / StorageProperties.MB;
						size = (int)(absoluteSize / StorageProperties.KB);
					} else {
						absoluteSize = size * StorageProperties.KB;
					}
					//create physical volume, volume group and logical volume
					createLogicalVolume(lvName, absoluteSize);
					//duplicate snapshot volume
					String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + lvName;
					duplicateLogicalVolume(loFileName, absoluteLVName);
					//export logical volume
					try {
						volumeManager.exportVolume(lvmVolumeInfo, volumeGroup, lvName);
					} catch(EucalyptusCloudException ex) {
						String returnValue = removeLogicalVolume(absoluteLVName);
						throw ex;
					}
					lvmVolumeInfo.setVolumeId(volumeId);
					lvmVolumeInfo.setLvName(lvName);
					lvmVolumeInfo.setStatus(StorageProperties.Status.available.toString());
					lvmVolumeInfo.setSize(size);
					volumeManager = new VolumeEntityWrapperManager();
					volumeManager.add(lvmVolumeInfo);
					volumeManager.finish();
				}  catch(EucalyptusCloudException ex) {
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

	public void cloneVolume(String volumeId, String parentVolumeId)
	throws EucalyptusCloudException {
		updateVolumeGroup();
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo foundVolumeInfo = volumeManager.getVolumeInfo(parentVolumeId);
		if(foundVolumeInfo != null) {
			String status = foundVolumeInfo.getStatus();
			String lvName = "lv-" + Hashes.getRandom(4);
			LVMVolumeInfo lvmVolumeInfo = volumeManager.getVolumeInfo();
			String parentLvName = foundVolumeInfo.getLvName();
			int size = foundVolumeInfo.getSize();
			volumeManager.finish();
			try {
				File parentVolumeFile = new File(DirectStorageInfo.getStorageInfo().getVolumesDir() + PATH_SEPARATOR + parentVolumeId);
				assert(parentVolumeFile.exists());
				long absouluteSize = (parentVolumeFile.length() / StorageProperties.MB);
				//create physical volume, volume group and logical volume
				createLogicalVolume(lvName, absouluteSize);
				//duplicate snapshot volume
				String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + lvName;
				String absoluteParentLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + parentLvName;
				duplicateLogicalVolume(absoluteParentLVName, absoluteLVName);
				//export logical volume
				try {
					volumeManager.exportVolume(lvmVolumeInfo, volumeGroup, lvName);
				} catch(EucalyptusCloudException ex) {
					String returnValue = removeLogicalVolume(absoluteLVName);
					throw ex;
				}
				lvmVolumeInfo.setVolumeId(volumeId);
				lvmVolumeInfo.setLvName(lvName);
				lvmVolumeInfo.setStatus(StorageProperties.Status.available.toString());
				lvmVolumeInfo.setSize(size);
				volumeManager = new VolumeEntityWrapperManager();
				volumeManager.add(lvmVolumeInfo);
				volumeManager.finish();
			}  catch(EucalyptusCloudException ex) {
				volumeManager.abort();
				String error = "Unable to run command: " + ex.getMessage();
				LOG.error(error);
				throw new EucalyptusCloudException(error);
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
		updateVolumeGroup();
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo foundLVMVolumeInfo = volumeManager.getVolumeInfo(volumeId);
		if(foundLVMVolumeInfo != null) {
			//remove aoe export
			String lvName = foundLVMVolumeInfo.getLvName();
			String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + lvName;
			volumeManager.unexportVolume(foundLVMVolumeInfo);
			try {
				String returnValue = "";				
				for (int i = 0; i < 5 ; ++i) {
					returnValue = removeLogicalVolume(absoluteLVName);
					if(returnValue.length() != 0) {
						if(returnValue.contains("successfully removed")) {
							break;
						}
					}
					//retry lv deletion (can take a while).
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						LOG.error(e);
						break;
					}
				}
				if(returnValue.length() == 0) {
					throw new EucalyptusCloudException("Unable to remove logical volume " + absoluteLVName);
				}
				volumeManager.remove(foundLVMVolumeInfo);
				volumeManager.finish();
			} catch(EucalyptusCloudException ex) {
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


	public List<String> createSnapshot(String volumeId, String snapshotId, Boolean shouldTransferSnapshot) throws EucalyptusCloudException {
		updateVolumeGroup();
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo foundLVMVolumeInfo = volumeManager.getVolumeInfo(volumeId);
		ArrayList<String> returnValues = new ArrayList<String>();
		if(foundLVMVolumeInfo != null) {
			LVMVolumeInfo snapshotInfo = volumeManager.getVolumeInfo();
			snapshotInfo.setVolumeId(snapshotId);
			File snapshotDir = new File(DirectStorageInfo.getStorageInfo().getVolumesDir());
			snapshotDir.mkdirs();

			String lvName = "lv-snap-" + Hashes.getRandom(4);
			String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + foundLVMVolumeInfo.getLvName();

			int size = foundLVMVolumeInfo.getSize();
			volumeManager.finish();
			volumeManager = null;
			try {
				long absoluteSize;
				CommandOutput result = SystemUtil.runWithRawOutput(new String[]{EUCA_ROOT_WRAPPER, "blockdev", "--getsize64", absoluteLVName});
				if (null != result && result.returnValue == 0 && StringUtils.isNotBlank(StringUtils.trim(result.output))) {
					try{
						absoluteSize = (Long.parseLong(StringUtils.trim(result.output)) / StorageProperties.MB);
					} catch (NumberFormatException e) {
						LOG.debug("Failed to parse size of volume " + volumeId, e);
						absoluteSize = size * StorageProperties.KB;
					}
				} else {
					absoluteSize = size * StorageProperties.KB;
				}
				
				//create physical volume, volume group and logical volume
				// String returnValue = createSnapshotLogicalVolume(absoluteLVName, lvName, size);
				String returnValue = createSnapshotLogicalVolume(absoluteLVName, lvName, absoluteSize);
				if(returnValue.length() == 0) {
					throw new EucalyptusCloudException("Unable to create snapshot logical volume " + lvName + " for volume " + lvName);
				}
				String snapRawFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + snapshotId;
				String absoluteSnapLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + lvName;

				duplicateLogicalVolume(absoluteSnapLVName, snapRawFileName);

				returnValue = removeLogicalVolume(absoluteSnapLVName);
				if(returnValue.length() == 0) {
					throw new EucalyptusCloudException("Unable to remove logical volume " + absoluteSnapLVName);
				}
				snapshotInfo.setLoFileName(snapRawFileName);
				// snapshotInfo.setStatus(StorageProperties.Status.available.toString());
				snapshotInfo.setSize(size);
				volumeManager = new VolumeEntityWrapperManager();
				volumeManager.add(snapshotInfo);
				returnValues.add(snapRawFileName);
				returnValues.add(String.valueOf(size * WalrusProperties.G));
			// } catch(EucalyptusCloudException ex) {
			} catch(Exception ex) {
				if(volumeManager != null)
					volumeManager.abort();
				String error = "Unable to run command: " + ex.getMessage();
				LOG.error(error);
				throw new EucalyptusCloudException(error);
			}

		}
		volumeManager.finish();
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
		updateVolumeGroup();
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

	public String getVolumeProperty(String volumeId) throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		String returnValue = volumeManager.getVolumeProperty(volumeId);
		volumeManager.finish();
		return returnValue;
	}

	public void reload() {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		List<LVMVolumeInfo> volumeInfos = volumeManager.getAllVolumeInfos();
		//now enable them
		for(LVMVolumeInfo foundVolumeInfo : volumeInfos) {
			try {
				if(foundVolumeInfo.getVgName() != null) {
					volumeManager.exportVolume(foundVolumeInfo);
				}
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
				returnValues.add(volumeGroup);
				returnValues.add(lvmVolumeInfo.getLvName());
			}
			return returnValues;
		}

		public void exportVolume(LVMVolumeInfo lvmVolumeInfo) throws EucalyptusCloudException {
			if(exportManager instanceof AOEManager) {
				AOEVolumeInfo aoeVolumeInfo = (AOEVolumeInfo) lvmVolumeInfo;

				int pid = aoeVolumeInfo.getVbladePid();
				if(pid > 0) {
					//enable logical volumes
					String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + aoeVolumeInfo.getLvName();
					try {
						enableLogicalVolume(absoluteLVName);
					} catch(EucalyptusCloudException ex) {
						String error = "Unable to run command: " + ex.getMessage();
						LOG.error(error);
						throw new EucalyptusCloudException(ex);
					}
					String returnValue = aoeStatus(pid);
					if(returnValue.length() == 0) {
						int majorNumber = aoeVolumeInfo.getMajorNumber();
						int minorNumber = aoeVolumeInfo.getMinorNumber();
						pid = exportManager.exportVolume(DirectStorageInfo.getStorageInfo().getStorageInterface(), absoluteLVName, majorNumber, minorNumber);
						aoeVolumeInfo.setVbladePid(pid);
						File vbladePidFile = new File(EUCA_VAR_RUN_PATH + "/vblade-" + majorNumber + minorNumber + ".pid");
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
			} else if(exportManager instanceof ISCSIManager) {
				ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) lvmVolumeInfo;
				String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + iscsiVolumeInfo.getLvName();
				try {
					enableLogicalVolume(absoluteLVName);
				} catch(EucalyptusCloudException ex) {
					String error = "Unable to run command: " + ex.getMessage();
					LOG.error(error);
					throw new EucalyptusCloudException(ex);
				}
				((ISCSIManager)exportManager).exportTarget(iscsiVolumeInfo.getTid(), iscsiVolumeInfo.getStoreName(), iscsiVolumeInfo.getLun(), absoluteLVName, iscsiVolumeInfo.getStoreUser());
			}	
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
					return ",,," + encryptedPassword + ",," + StorageProperties.STORAGE_HOST + "," + storeName;
				}
			}
			return null;
		}

		public void unexportVolume(LVMVolumeInfo volumeInfo) {
			if(volumeInfo instanceof AOEVolumeInfo) {
				AOEVolumeInfo aoeVolumeInfo = (AOEVolumeInfo) volumeInfo;
				int pid = aoeVolumeInfo.getVbladePid();
				if(pid > 0) {
					String returnValue = aoeStatus(pid);
					if(returnValue.length() > 0) {
						exportManager.unexportVolume(pid);
						int majorNumber = aoeVolumeInfo.getMajorNumber();
						int minorNumber = aoeVolumeInfo.getMinorNumber();
						File vbladePidFile = new File(EUCA_VAR_RUN_PATH + "/vblade-" + majorNumber + minorNumber + ".pid");
						if(vbladePidFile.exists()) {
							vbladePidFile.delete();
						}
					}
				}
			} else if(volumeInfo instanceof ISCSIVolumeInfo) {
				ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) volumeInfo;
				((ISCSIManager)exportManager).unexportTarget(iscsiVolumeInfo.getTid(), iscsiVolumeInfo.getLun());
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
			if(exportManager instanceof AOEManager) {
				AOEVolumeInfo AOEVolumeInfo = new AOEVolumeInfo();
				return entityWrapper.query(AOEVolumeInfo);
			} else if(exportManager instanceof ISCSIManager) {
				ISCSIVolumeInfo ISCSIVolumeInfo = new ISCSIVolumeInfo();
				return entityWrapper.query(ISCSIVolumeInfo);
			}
			return new ArrayList<LVMVolumeInfo>();
		}

		private void add(LVMVolumeInfo volumeInfo) {
			entityWrapper.add(volumeInfo);
		}

		private void remove(LVMVolumeInfo volumeInfo) {
			entityWrapper.delete(volumeInfo);
		}

		private String encryptTargetPassword(String password) throws EucalyptusCloudException {
			try {
				List<ServiceConfiguration> partitionConfigs = ServiceConfigurations.listPartition( ClusterController.class, StorageProperties.NAME );
				ServiceConfiguration clusterConfig = partitionConfigs.get( 0 );
				PublicKey ncPublicKey = Partitions.lookup(clusterConfig).getNodeCertificate().getPublicKey();
				Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				cipher.init(Cipher.ENCRYPT_MODE, ncPublicKey);
				return new String(Base64.encode(cipher.doFinal(password.getBytes())));	      
			} catch ( Exception e ) {
				LOG.error( "Unable to encrypt storage target password" );
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
					for(int i=0; i < 5; ++i) {
						returnValue = aoeStatus(pid);
						if(returnValue.length() == 0) {
							success = false;
							try {
								Thread.sleep(timeout);
							} catch(InterruptedException ie) {
								LOG.error(ie);
							}
							timeout += 300;
						} else {
							success = true;
							break;
						}
					}
				}
				if(!success) {
					throw new EucalyptusCloudException("Could not export AoE device " + absoluteLVName + " iface: " + DirectStorageInfo.getStorageInfo().getStorageInterface() + " pid: " + pid + " returnValue: " + returnValue);
				}

				File vbladePidFile = new File(EUCA_VAR_RUN_PATH + "/vblade-" + majorNumber + minorNumber + ".pid");
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
	public void loadSnapshots(List<String> snapshotSet,
			List<String> snapshotFileNames) throws EucalyptusCloudException {
		// TODO Auto-generated method stub

	}
	@Override
	public String prepareSnapshot(String snapshotId, int sizeExpected, long actualSizeInMB)
	throws EucalyptusCloudException {
		String deviceName = null;
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo foundSnapshotInfo = volumeManager.getVolumeInfo(snapshotId);
		if (null  == foundSnapshotInfo) {
			LVMVolumeInfo snapshotInfo = volumeManager.getVolumeInfo();
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
		configurableClass = DASInfo.class.getAnnotation(ConfigurableClass.class);
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
	}

	@Override
	public String getStorageRootDirectory() {
		return DirectStorageInfo.getStorageInfo().getVolumesDir();
	}

	@Override
	public void finishVolume(String snapshotId) throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo foundSnapshotInfo = volumeManager.getVolumeInfo(snapshotId);
		if (null != foundSnapshotInfo) {
			foundSnapshotInfo.setStatus(StorageProperties.Status.available.toString());
		} 
		volumeManager.finish();
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
	public void importSnapshot(String snapshotId, String snapPath,
			String volumeId, int size) throws EucalyptusCloudException {
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
	public String attachVolume(String volumeId, List<String> nodeIqns)
	throws EucalyptusCloudException {
		return getVolumeProperty(volumeId);
	}

	@Override
	public void detachVolume(String volumeId, String nodeIqn)
	throws EucalyptusCloudException {
	}

	@Override
	public void checkReady() throws EucalyptusCloudException {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() throws EucalyptusCloudException {
		// TODO Auto-generated method stub

	}

	@Override
	public void disable() throws EucalyptusCloudException {
		// TODO Auto-generated method stub

	}

	@Override
	public void enable() throws EucalyptusCloudException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean getFromBackend(String snapshotId, int size)
			throws EucalyptusCloudException {
		return false;
	}

	@Override
	public void checkVolume(String volumeId) throws EucalyptusCloudException {
	}
	
	@Override
	public List<CheckerTask> getCheckers() {
		List<CheckerTask> checkers = new ArrayList<CheckerTask>();
		return checkers;
	}
}
