/*******************************************************************************
 *Copyright (c) 2009-2014  Eucalyptus Systems, Inc.
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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.blockstorage.StorageManagers.StorageManagerProperty;
import com.eucalyptus.blockstorage.entities.DASInfo;
import com.eucalyptus.blockstorage.entities.DirectStorageInfo;
import com.eucalyptus.blockstorage.entities.ISCSIVolumeInfo;
import com.eucalyptus.blockstorage.entities.LVMVolumeInfo;
import com.eucalyptus.blockstorage.entities.StorageInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.common.CheckerTask;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.EucalyptusCloudException;
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
	public static final String EUCA_VAR_RUN_PATH = System.getProperty("euca.run.dir");	
	public static final StorageExportManager exportManager  = new ISCSIManager();
	private static String volumeGroup;
	protected ConcurrentHashMap<String, VolumeOpMonitor> volumeOps;

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
			String returnValue = LVMWrapper.getLvmVersion();
			if(returnValue.length() == 0) {
				throw new EucalyptusCloudException("Is lvm installed?");
			} else {
				LOG.info(returnValue);
			}

			//exportManager = new ISCSIManager();
			if(exportManager != null) {
				exportManager.checkPreconditions();
			}
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
						returnValue = LVMWrapper.getVolumeGroup(dasDevice);
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
							returnValue = LVMWrapper.getPhysicalVolume(dasDevice);
							if(returnValue.matches("(?s:.*)PV Name.*" + dasDevice + "(?s:.*)")) {
								physicalVolumeGroupFound = true;
							}
						} catch(EucalyptusCloudException e) {
							LOG.warn(e);
						}
						if(!physicalVolumeGroupFound) {
							returnValue = LVMWrapper.createPhysicalVolume(dasDevice);
							if(returnValue.length() == 0) {
								throw new EucalyptusCloudException("Unable to create physical volume on device: " + dasDevice);
							}
						}
						//PV should be initialized at this point.
						returnValue = LVMWrapper.getPhysicalVolumeVerbose(dasDevice);
						if(returnValue.matches("(?s:.*)PV Name.*" + dasDevice + "(?s:.*)")) {
							Pattern volumeGroupPattern = Pattern.compile("(?s:.*VG Name)(.*)\n.*");
							Matcher m = volumeGroupPattern.matcher(returnValue);
							if(m.find()) { 
								volumeGroup = m.group(1).trim();
							}
							if((volumeGroup == null) || (volumeGroup.length() == 0)) {
								volumeGroup = generateVGName(Hashes.getRandom(10));
								returnValue = LVMWrapper.createVolumeGroup(dasDevice, volumeGroup);
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



	protected String duplicateLogicalVolume(String oldLvName, String newLvName) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "dd", "if=" + oldLvName, "of=" + newLvName, "bs=" + StorageProperties.blockSize});
	}

	protected String createFile(String fileName, long size) throws EucalyptusCloudException {
		if(!DirectStorageInfo.getStorageInfo().getZeroFillVolumes())
			return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "dd", "if=/dev/zero", "of=" + fileName, "count=1", "bs=" + StorageProperties.blockSize, "seek=" + (size -1)});
		else
			return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "dd", "if=/dev/zero", "of=" + fileName, "count=" + size, "bs=" + StorageProperties.blockSize});
	}

	protected String createEmptyFile(String fileName, int size) throws EucalyptusCloudException {
		long fileSize = size * 1024;
		return createFile(fileName, fileSize);
	}

	public String createAbsoluteEmptyFile(String fileName, long size) throws EucalyptusCloudException {
		size = size / ObjectStorageProperties.M;
		return createFile(fileName, size);
	}


	public void initialize() throws EucalyptusCloudException {
		if(!initialized) {
			//DO NOT WANT!
			//System.loadLibrary("dascontrol");
			//registerSignals();
			initialized = true;
		}
	}

	public void configure() throws EucalyptusCloudException {
		exportManager.configure();
		//dummy init
		//this is retarded. wtf?
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
			VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
			LVMVolumeInfo lvmVolInfo = volumeManager.getVolumeInfo(volumeId);
			if(lvmVolInfo != null) {
				volumeManager.unexportVolume(lvmVolInfo);
				String lvName = lvmVolInfo.getLvName();
				String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + lvName;
				try {
					String returnValue = LVMWrapper.removeLogicalVolume(absoluteLVName);
				} catch(EucalyptusCloudException ex) {
					volumeManager.abort();
					String error = "Unable to run command: " + ex.getMessage();
					LOG.error(error);
				}
				volumeManager.remove(lvmVolInfo);
				volumeManager.finish();
			}
		} catch (EucalyptusCloudException e) {
			LOG.debug("Failed to clean volume: " + volumeId, e);
			return;
		}
	}

	public void cleanSnapshot(String snapshotId) {
		try {
			updateVolumeGroup();
			VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
			LVMVolumeInfo lvmVolInfo = volumeManager.getVolumeInfo(snapshotId);
			if(lvmVolInfo != null) {
				volumeManager.remove(lvmVolInfo);
			}
			volumeManager.finish();
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
	public void createLogicalVolume(String volumeId, String lvName, long size) throws EucalyptusCloudException {
		if(volumeGroup != null) {
			String returnValue = LVMWrapper.createLogicalVolume(volumeId, volumeGroup, lvName, size);
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

		String lvName = generateLVName(volumeId);//"lv-" + Hashes.getRandom(4);
		LVMVolumeInfo lvmVolumeInfo = null;
		lvmVolumeInfo = new ISCSIVolumeInfo();

		volumeManager.finish();
		//create file and attach to loopback device
		try {
			//create logical volume
			createLogicalVolume(volumeId, lvName, (size * StorageProperties.KB));
			lvmVolumeInfo.setVolumeId(volumeId);
			lvmVolumeInfo.setVgName(volumeGroup);
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
				String lvName = generateLVName(volumeId); //"lv-" + Hashes.getRandom(4);
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
					createLogicalVolume(volumeId, lvName, absoluteSize);
					//duplicate snapshot volume
					String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + lvName;
					duplicateLogicalVolume(loFileName, absoluteLVName);
					lvmVolumeInfo.setVolumeId(volumeId);
					lvmVolumeInfo.setVgName(volumeGroup);
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
			String lvName = generateLVName(volumeId); //"lv-" + Hashes.getRandom(4);
			LVMVolumeInfo lvmVolumeInfo = volumeManager.getVolumeInfo();
			String parentLvName = foundVolumeInfo.getLvName();
			int size = foundVolumeInfo.getSize();
			volumeManager.finish();
			try {
				File parentVolumeFile = new File(DirectStorageInfo.getStorageInfo().getVolumesDir() + PATH_SEPARATOR + parentVolumeId);
				assert(parentVolumeFile.exists());
				long absouluteSize = (parentVolumeFile.length() / StorageProperties.MB);
				//create physical volume, volume group and logical volume
				createLogicalVolume(volumeId, lvName, absouluteSize);
				//duplicate snapshot volume
				String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + lvName;
				String absoluteParentLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + parentLvName;
				duplicateLogicalVolume(absoluteParentLVName, absoluteLVName);
				//export logical volume
				try {
					volumeManager.exportVolume(lvmVolumeInfo, volumeGroup, lvName);
				} catch(EucalyptusCloudException ex) {
					String returnValue = LVMWrapper.removeLogicalVolume(absoluteLVName);
					throw ex;
				}
				lvmVolumeInfo.setVolumeId(volumeId);
				lvmVolumeInfo.setVgName(volumeGroup);
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
						LOG.info("Deleting volume " + volumeId);
						String lvName = foundLVMVolumeInfo.getLvName();
						String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + lvName;
						String returnValue = "";		
						for (int i = 0; i < 5 ; ++i) {
							returnValue = LVMWrapper.removeLogicalVolume(absoluteLVName);
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


	protected static String generateLVName(String baseName) {
		return "euca-" + baseName;
	}

	protected static String generateVGName(String baseName) {
		return "euca-ebs-storage-vg-" + baseName;
	}

	public List<String> createSnapshot(String volumeId, String snapshotId, String snapshotPointId, Boolean shouldTransferSnapshot) throws EucalyptusCloudException {
		if(snapshotPointId != null) {
			throw new EucalyptusCloudException("Synchronous snapshot points not supported in DAS storage manager");			
		}

		updateVolumeGroup();
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo foundLVMVolumeInfo = volumeManager.getVolumeInfo(volumeId);
		ArrayList<String> returnValues = new ArrayList<String>();
		if(foundLVMVolumeInfo != null) {
			LVMVolumeInfo snapshotInfo = volumeManager.getVolumeInfo();
			snapshotInfo.setVolumeId(snapshotId);
			File snapshotDir = new File(DirectStorageInfo.getStorageInfo().getVolumesDir());
			snapshotDir.mkdirs();

			String lvName = generateLVName(snapshotId);//"lv-snap-" + Hashes.getRandom(4);
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
				String returnValue = LVMWrapper.createSnapshotLogicalVolume(absoluteLVName, lvName, absoluteSize);
				if(returnValue.length() == 0) {
					throw new EucalyptusCloudException("Unable to create snapshot logical volume " + lvName + " for volume " + lvName);
				}
				String snapRawFileName = DirectStorageInfo.getStorageInfo().getVolumesDir() + "/" + snapshotId;
				String absoluteSnapLVName = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + lvName;

				duplicateLogicalVolume(absoluteSnapLVName, snapRawFileName);

				returnValue = LVMWrapper.removeLogicalVolume(absoluteSnapLVName);
				if(returnValue.length() == 0) {
					throw new EucalyptusCloudException("Unable to remove logical volume " + absoluteSnapLVName);
				}
				snapshotInfo.setLoFileName(snapRawFileName);
				// snapshotInfo.setStatus(StorageProperties.Status.available.toString());
				snapshotInfo.setSize(size);
				volumeManager = new VolumeEntityWrapperManager();
				volumeManager.add(snapshotInfo);
				returnValues.add(snapRawFileName);
				returnValues.add(String.valueOf(size * ObjectStorageProperties.G));
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

	public String getVolumeConnectionString(String volumeId) throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		String returnValue = volumeManager.getConnectionString(volumeId);
		volumeManager.finish();
		return returnValue;
	}

	public void reload() {
		LOG.info("Reload: starting reload process to re-export volumes if necessary");
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		List<LVMVolumeInfo> volumeInfos = volumeManager.getAllVolumeInfos();
		//now enable them
		for(LVMVolumeInfo foundVolumeInfo : volumeInfos) {
			try {
				LOG.info("Reload: Checking volume " + foundVolumeInfo.getVolumeId() + " for export");
				if(foundVolumeInfo.getVgName() != null && volumeManager.shouldExportOnReload(foundVolumeInfo)) {
					LOG.info("Reload: Volume " + foundVolumeInfo.getVolumeId() + " was exported at shutdown. Not found to be already exported. Re-exporting volume.");
					volumeManager.exportVolume(foundVolumeInfo);
				} else {
					LOG.info("Reload: volume " + foundVolumeInfo.getVolumeId() + " not previously exported or already exported, no action required. Skipping");
				}
			} catch(EucalyptusCloudException ex) {
				LOG.error("Unable to reload volume: " + foundVolumeInfo.getVolumeId() + " due to: " + ex);
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

	protected class VolumeEntityWrapperManager {
		private EntityWrapper entityWrapper;

		protected VolumeEntityWrapperManager() {
			entityWrapper = StorageProperties.getEntityWrapper();
		}

		/**
		 * Returns if the volume should be re-exported on reload based on DB state. Masks the implementation
		 * details from DASManager so it can be changed based on DB or iscsi implementation
		 * @param lvmVolumeInfo
		 * @return
		 */
		public boolean shouldExportOnReload(LVMVolumeInfo lvmVolumeInfo) {
			if(lvmVolumeInfo instanceof ISCSIVolumeInfo) {
				ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) lvmVolumeInfo;
				try {
					return iscsiVolumeInfo.getTid() >= 0 && !exportManager.isExported(lvmVolumeInfo);
				} catch(EucalyptusCloudException e) {
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
			if(lvmVolumeInfo instanceof ISCSIVolumeInfo) {
				ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) lvmVolumeInfo;
				String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + iscsiVolumeInfo.getVgName() + PATH_SEPARATOR + iscsiVolumeInfo.getLvName();
				if(!LVMWrapper.logicalVolumeExists(absoluteLVName)) {
					LOG.error("Backing volume not found: " + absoluteLVName);
					throw new EucalyptusCloudException("Logical volume not found: " + absoluteLVName);
				}
				try {
					LVMWrapper.enableLogicalVolume(absoluteLVName);
				} catch(EucalyptusCloudException ex) {
					String error = "Unable to run command: " + ex.getMessage();
					LOG.error(error);
					throw new EucalyptusCloudException(ex);
				}
				((ISCSIManager)exportManager).exportTarget(iscsiVolumeInfo.getVolumeId(), iscsiVolumeInfo.getTid(), iscsiVolumeInfo.getStoreName(), iscsiVolumeInfo.getLun(), absoluteLVName, iscsiVolumeInfo.getStoreUser());
			}

		}

		public String getConnectionString(String volumeId) {
			LVMVolumeInfo lvmVolumeInfo = getVolumeInfo(volumeId);
			if(lvmVolumeInfo != null) {
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
			return null;
		}

		public void unexportVolume(LVMVolumeInfo volumeInfo) {
			StorageExportManager manager = exportManager;
			if(!(exportManager instanceof ISCSIManager)) {
				manager = new ISCSIManager();
			}
			ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) volumeInfo;

			//Use the absolute name to verify that the target is correct before unexport
			String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + iscsiVolumeInfo.getVgName() + PATH_SEPARATOR + iscsiVolumeInfo.getLvName();
			if(LVMWrapper.logicalVolumeExists(absoluteLVName)) {
				try {
					((ISCSIManager)manager).unexportTarget(volumeInfo.getVolumeId(), iscsiVolumeInfo.getTid(), iscsiVolumeInfo.getLun(), absoluteLVName);
				} catch(EucalyptusCloudException e) {
					LOG.error("Error unexporting target for volume " + volumeInfo.getVolumeId(),e);
					return;
				}
			} 
			iscsiVolumeInfo.setTid(-1);
		}

		protected void finish() {
			try {
				entityWrapper.commit();
			} catch (Exception ex) {
				LOG.error(ex, ex);
				entityWrapper.rollback();
			}
		}

		protected void abort() {
			entityWrapper.rollback();
		}

		protected LVMVolumeInfo getVolumeInfo(String volumeId) {
			ISCSIVolumeInfo ISCSIVolumeInfo = new ISCSIVolumeInfo(volumeId);
			List<ISCSIVolumeInfo> ISCSIVolumeInfos = entityWrapper.query(ISCSIVolumeInfo);
			if(ISCSIVolumeInfos.size() > 0) {
				return ISCSIVolumeInfos.get(0);
			}
			return null;
		}

		protected boolean areSnapshotsPending(String volumeId) {
			ISCSIVolumeInfo ISCSIVolumeInfo = new ISCSIVolumeInfo();
			ISCSIVolumeInfo.setSnapshotOf(volumeId);
			ISCSIVolumeInfo.setStatus(StorageProperties.Status.pending.toString());
			List<ISCSIVolumeInfo> ISCSIVolumeInfos = entityWrapper.query(ISCSIVolumeInfo);
			if(ISCSIVolumeInfos.size() > 0) {
				return true;
			}
			return false;
		}

		protected LVMVolumeInfo getVolumeInfo() {
			return new ISCSIVolumeInfo();
		}

		protected List<LVMVolumeInfo> getAllVolumeInfos() {
			List<LVMVolumeInfo> volumeInfos = new ArrayList<LVMVolumeInfo>();
			volumeInfos.addAll(entityWrapper.query(new ISCSIVolumeInfo()));	
			return volumeInfos;
		}

		protected void add(LVMVolumeInfo volumeInfo) {
			entityWrapper.add(volumeInfo);
		}

		protected void remove(LVMVolumeInfo volumeInfo) {
			entityWrapper.delete(volumeInfo);
		}

		protected String encryptTargetPassword(String password) throws EucalyptusCloudException {
			try {
				List<ServiceConfiguration> partitionConfigs = ServiceConfigurations.listPartition( ClusterController.class, StorageProperties.NAME );
				ServiceConfiguration clusterConfig = partitionConfigs.get( 0 );
				PublicKey ncPublicKey = Partitions.lookup( clusterConfig ).getNodeCertificate( ).getPublicKey();
				Cipher cipher = Ciphers.RSA_PKCS1.get();
				cipher.init(Cipher.ENCRYPT_MODE, ncPublicKey);
				return new String(Base64.encode(cipher.doFinal(password.getBytes())));	      
			} catch ( Exception e ) {
				LOG.error( "Unable to encrypt storage target password" );
				throw new EucalyptusCloudException(e.getMessage(), e);
			}
		}

		protected void exportVolume(LVMVolumeInfo lvmVolumeInfo, String vgName, String lvName) throws EucalyptusCloudException {
			ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) lvmVolumeInfo;

			String absoluteLVName = lvmRootDirectory + PATH_SEPARATOR + vgName + PATH_SEPARATOR + lvName;
			int max_tries = 10;
			int i = 0;
			EucalyptusCloudException ex = null;
			do {
				exportManager.allocateTarget(iscsiVolumeInfo);
				try {
					((ISCSIManager)exportManager).exportTarget(iscsiVolumeInfo.getVolumeId(), iscsiVolumeInfo.getTid(), iscsiVolumeInfo.getStoreName(), iscsiVolumeInfo.getLun(), absoluteLVName, iscsiVolumeInfo.getStoreUser());
					ex = null;
					//it worked. break out. may be break is a better way of breaking out?
					//i = max_tries;
					break;
				} catch (EucalyptusCloudException e) {
					ex = e;
					LOG.error(e);				
				}
			} while (i++ < max_tries);

			// EUCA-3597 After all retries, check if the process actually completed
			if (null != ex){
				throw ex;
			}
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

	/**
	 * NOTE: once exported to one host, it is exported to all in this implementation
	 */
	@Override
	public String exportVolume(String volumeId, String nodeIqn)
			throws EucalyptusCloudException {
                try {
                        updateVolumeGroup();
                } catch (EucalyptusCloudException e) {
                        LOG.error(e);
			throw e;
                }
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
			VolumeOpMonitor monitor = getMonitor(volumeId);
			synchronized (monitor) {
				final VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
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
			}// synchronized
		}
		//Return the connection string.
		return getVolumeConnectionString(volumeId);
	}

	/**
	 * This implementation does not do export/unexport from single hosts, only all or none.
	 * Caller should be keeping track of exports and call @unexportVolumeFromAll when appropriate
	 */
	@Override
	public void unexportVolume(String volumeId, String nodeIqn)
			throws EucalyptusCloudException, UnsupportedOperationException {
		throw new UnsupportedOperationException("DASManager does not support node-specific export/unexport");

		/*VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		LVMVolumeInfo foundLVMVolumeInfo = volumeManager.getVolumeInfo(volumeId);
		if(foundLVMVolumeInfo != null) {
			LOG.info("Marking volume: " + volumeId + " for cleanup");
			foundLVMVolumeInfo.setCleanup(true);
			volumeManager.finish();
		}  else {
			volumeManager.abort();
			throw new EucalyptusCloudException("Unable to find volume: " + volumeId);
		}*/
	}

	@Override
	public void unexportVolumeFromAll(String volumeId) throws EucalyptusCloudException {
		VolumeEntityWrapperManager volumeManager = new VolumeEntityWrapperManager();
		try {
			LVMVolumeInfo foundLVMVolumeInfo = volumeManager.getVolumeInfo(volumeId);
			if(foundLVMVolumeInfo != null) {
				//LOG.info("Marking volume: " + volumeId + " for cleanup");
				//foundLVMVolumeInfo.setCleanup(true);
				LOG.info("Unexporting volume " + volumeId + " from all clients");
				VolumeOpMonitor monitor = getMonitor(volumeId);
				synchronized (monitor) {
					try {
						LOG.info("Unexporting volume " + foundLVMVolumeInfo.getVolumeId());
						try {
							String path = lvmRootDirectory + PATH_SEPARATOR + volumeGroup + PATH_SEPARATOR + foundLVMVolumeInfo.getLvName();
							if(LVMWrapper.logicalVolumeExists(path)) {
								//guard this. tgt is not happy when you ask it to
								//get rid of a non existent tid
								exportManager.cleanup(foundLVMVolumeInfo);
							}
							foundLVMVolumeInfo.setStatus("available");
							LOG.info("Done cleaning up: " + foundLVMVolumeInfo.getVolumeId());
						} catch (EucalyptusCloudException ee) {
							LOG.error(ee, ee);
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
		} catch(Exception e) {
			LOG.error("Failed to unexport volume " + volumeId);
			throw new EucalyptusCloudException("Failed to unexport volume " + volumeId);
		} finally {
			volumeManager.finish();
		}		
	}

	@Override
	public void checkReady() throws EucalyptusCloudException {
		//check if binaries exist, commands can be executed, etc.
		if(!new File(StorageProperties.EUCA_ROOT_WRAPPER).exists()) {
			throw new EucalyptusCloudException("root wrapper (euca_rootwrap) does not exist in " + StorageProperties.EUCA_ROOT_WRAPPER);
		}
		File varDir = new File(EUCA_VAR_RUN_PATH);
		if(!varDir.exists()) {
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
		//Volume cleanup is now synchronous, no need for background tasks
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


}
