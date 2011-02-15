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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import netapp.manage.NaElement;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.BlockStorageUtil;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.ExecutionException;
import com.eucalyptus.util.StorageProperties;

import edu.ucsb.eucalyptus.cloud.entities.CHAPUserInfo;
import edu.ucsb.eucalyptus.cloud.entities.IgroupInfo;
import edu.ucsb.eucalyptus.cloud.entities.NetappInfo;
import edu.ucsb.eucalyptus.cloud.entities.SANInfo;
import edu.ucsb.eucalyptus.cloud.entities.SANVolumeInfo;
import edu.ucsb.eucalyptus.msgs.ComponentProperty;
import edu.ucsb.eucalyptus.util.ConfigParser;
import edu.ucsb.eucalyptus.util.StreamConsumer;
import edu.ucsb.eucalyptus.util.SystemUtil;

public class NetappProvider implements SANProvider {
	private static Logger LOG = Logger.getLogger(NetappProvider.class);
	private static final String ISCSI_INITIATOR_NAME_CONF = "/etc/iscsi/initiatorname.iscsi";
	private final String TARGET_USERNAME = "eucalyptus"; 
	public static int API_MAJOR_VERSION = 1;
	public static int API_MINOR_VERSION = 3;
	private NetappSessionManager sessionManager;
	private String targetIqn;
	private final long TASK_TIMEOUT = 5 * 60 * 1000;
	private final long CLONE_STATUS_SLEEP = 1 * 5000L;

	private final String CLONE_OKAY_MESSAGE = "The volume is not a clone";

	public NetappProvider() {
		sessionManager = new NetappSessionManager();
		StorageProperties.IQN = getSCIqn();
	}

	private String getSCIqn() {
		try {
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(new String[]{ StorageProperties.eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "cat", ISCSI_INITIATOR_NAME_CONF});
			StreamConsumer error = new StreamConsumer(proc.getErrorStream());
			ConfigParser output = new ConfigParser(proc.getInputStream());
			error.start();
			output.start();
			output.join();
			error.join();
			if(output.getValues().containsKey("InitiatorName")) {
				return output.getValues().get("InitiatorName");
			}
		} catch (Throwable t) {
			LOG.error(t);
		}
		return null;		
	}

	@Override
	public void addUser(String userName) {
		try {
			NaElement request = makeRequest("iscsi-initiator-set-default-auth", 
					"auth-type", "deny");
			NaElement reply = execCommand(request);
		} catch (EucalyptusCloudException e) {
			LOG.error("Unable to add default deny all " + e);
		}
	}

	@Override
	public void checkConnection() {
		addUser(TARGET_USERNAME);
	}

	@Override
	public void checkPreconditions() throws EucalyptusCloudException {
	}

	@Override
	public void configure() {
		SANInfo sanInfo = SANInfo.getStorageInfo();
		if(!StorageProperties.DUMMY_SAN_HOST.equals(sanInfo.getSanHost())) {
			try {
				sessionManager.update();
				addUser(TARGET_USERNAME);
				targetIqn = getTargetIqn();
			} catch (Exception e) {
				LOG.error(e);
			}
		}
		LOG.info("Netapp aggregate: " + NetappInfo.getStorageInfo().getAggregate());		
	}

	private String getTargetIqn() throws EucalyptusCloudException {
		if(targetIqn == null) {
			NaElement request = makeRequest("iscsi-node-get-name");		
			NaElement reply = execCommand(request);
			targetIqn = reply.getChildContent("node-name");
		}
		return targetIqn;
	}

	@Override
	public String connectTarget(String iqn) throws EucalyptusCloudException {
		try {
			SANInfo sanInfo = SANInfo.getStorageInfo();
			String encryptedPassword = "not_required";
			try {
				String deviceString = null;
				String[] parts = iqn.split(",");
				if(parts.length > 1) {
					deviceString = System.getProperty("euca.home") + "," + 
					sanInfo.getSanHost() + "," + 
					parts[0] + "," + 
					encryptedPassword + "," + 
					parts[1] + "," + 
					StorageProperties.IscsiAuthType.HBA.toString();
				} else {
					throw new EucalyptusCloudException("Invalid remote device string.");
				}
				String deviceName = SystemUtil.run(new String[]{StorageProperties.eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, BaseDirectory.LIB.toString() + File.separator + "connect_iscsitarget_sc.pl", 
						deviceString});
				if(deviceName.length() == 0) {
					throw new EucalyptusCloudException("Unable to get device name. Connect failed.");
				}
				return deviceName;
			} catch (ExecutionException e) {
				throw new EucalyptusCloudException("Unable to connect to storage target");
			}				
		} catch(EucalyptusCloudException ex) {
			throw ex;
		}
	}

	public String createSnapshot(String volumeId, String snapshotId) throws EucalyptusCloudException {
		//create flexclone
		volumeId = sanitizeVolumeId(volumeId);
		snapshotId = sanitizeVolumeId(snapshotId);
		if(volumeExists(volumeId)) {
			try {
				createCloneAndWaitForCompletion(volumeId, snapshotId);
			} catch (EucalyptusCloudException e) {
				LOG.error("Unable to create snapshot: " + snapshotId);
				NaElement request = makeRequest("volume-offline", "name", snapshotId);
				NaElement reply = execCommand(request);
				request = makeRequest("volume-destroy", "name", snapshotId);
				reply = execCommand(request);
				throw e;
			}		
		} else {
			throw new EucalyptusCloudException("Volume not found: " + volumeId);			
		}
		EntityWrapper<IgroupInfo> db = StorageProperties.getEntityWrapper();
		String igroupName;
		try {
			IgroupInfo igroup = db.getUnique(new IgroupInfo(StorageProperties.NAME));
			igroupName = igroup.getiGroupName();			
		} catch (EucalyptusCloudException ex) {
			List<IgroupInfo> igroups = db.query(new IgroupInfo(StorageProperties.NAME));
			if(igroups.size() > 1) {
				for(IgroupInfo igroup : igroups) {
					try {
						removeIgroup(igroup.getiGroupName(), true);
					} catch (EucalyptusCloudException e) {
						LOG.error(e);
					}
					db.delete(igroup);
				}
			}
			igroupName = "igroup" + Hashes.getRandom(16);
			try {
				createIgroup(igroupName);
				//add iqn to igroup
				try {
					addInitiator(igroupName, StorageProperties.IQN);
				} catch(EucalyptusCloudException e) {
					removeInitiator(igroupName, StorageProperties.IQN);
					removeIgroup(igroupName, true);
				}
			} catch (EucalyptusCloudException e) {
				LOG.error(e);
				NaElement request = makeRequest("volume-offline", "name", snapshotId);
				NaElement reply = execCommand(request);
				request = makeRequest("volume-destroy", "name", snapshotId);
				reply = execCommand(request);
				db.rollback();
				throw e;
			}
			IgroupInfo igroup = new IgroupInfo(igroupName, StorageProperties.NAME, StorageProperties.IQN);
			db.add(igroup);			
		} finally {
			try {
				db.commit();
			} catch(Exception e) {
				db = StorageProperties.getEntityWrapper();
				try {
					IgroupInfo igroup = db.getUnique(new IgroupInfo(StorageProperties.NAME));
					igroupName = igroup.getiGroupName();
					LOG.info("Got igroup: " + igroupName);
					db.commit();
				} catch (EucalyptusCloudException ex) {
					db.rollback();
					LOG.error("Unable to get initiator group for: " + StorageProperties.NAME);
					throw ex;	
				}
			}
		}
		//map clone's lun to igroup that SC is part of (so connect can succeed).
		String lunPath = "/vol/" + snapshotId + "/lun1";
		try {
			//mark it online
			NaElement request = makeRequest("lun-online", "path", lunPath);
			NaElement reply = execCommand(request);
			int lun = mapLun(lunPath, igroupName);
			String iqn = getTargetIqn() + "," + lun;
			//add incoming security rule (if one does not exist).
			checkAddInitiatorAuth(StorageProperties.IQN);
			return iqn;
		} catch (EucalyptusCloudException e) {
			LOG.error(e);
			try {
				unmapLun(lunPath, igroupName);
			} catch(EucalyptusCloudException ex) {
				LOG.warn(ex);
			}
			NaElement request = makeRequest("volume-offline", "name", snapshotId);
			NaElement reply = execCommand(request);
			request = makeRequest("volume-destroy", "name", snapshotId);
			reply = execCommand(request);
			throw e;
		}
	}

	private void createCloneAndWaitForCompletion(String parent, String clone)
	throws EucalyptusCloudException {
		NaElement request = makeRequest("volume-clone-create",
				"parent-volume", parent,
				"volume",clone);
		NaElement reply = execCommand(request);
		request = makeRequest("volume-clone-split-start",						
				"volume", clone);
		reply = execCommand(request);
		try {
			for(int trial = 0; trial < 20; ++trial) {
				request = makeRequest("volume-clone-split-status", 
						"volume", clone);
				reply = execCommand(request);
				List volStatus = reply.getChildByName("clone-split-details").getChildren();
				for(Iterator i = volStatus.iterator(); i.hasNext();) {
					NaElement status = (NaElement) i.next();
					String percentComplete = status.getChildContent("inode-percentage-complete");
				}
				try {
					Thread.sleep(CLONE_STATUS_SLEEP);
				} catch (InterruptedException e) {
					LOG.error(e);
					throw new EucalyptusCloudException(e);
				}
			}
		} catch (EucalyptusCloudException ex) {
			if(!ex.getMessage().contains(CLONE_OKAY_MESSAGE)) {
				throw ex;
			}
		}
	}

	@Override
	public String createVolume(String volumeId, String snapshotId, int snapSize, int size) throws EucalyptusCloudException {
		//either get the flexclone of the original vol or the vol that external snap 
		//was copied to (they have the same name).
		volumeId = sanitizeVolumeId(volumeId);
		snapshotId = sanitizeVolumeId(snapshotId);
		int sizeDiff = size - snapSize;
		if(volumeExists(snapshotId)) {
			//flexclone it and split the new clone. get status and wait until split is complete.
			try {
				createCloneAndWaitForCompletion(snapshotId, volumeId);
				if(sizeDiff > 0) {
					//resize it
					long vol_size = size * StorageProperties.GB;
					vol_size += (vol_size * (NetappInfo.getStorageInfo().getSnapReserve()/100)) + (vol_size * (StorageProperties.NETAPP_META_OVERHEAD / 1000));
					int sizeInMB = (int) (vol_size / StorageProperties.MB);
					NaElement request = makeRequest("volume-size", "volume", volumeId, "new-size", sizeInMB + "m");
					NaElement reply = execCommand(request);
				}
			} catch (EucalyptusCloudException e) {
				LOG.error("Unable to create volume: " + volumeId + " " + e);
				NaElement request = makeRequest("volume-offline", "name", volumeId);
				NaElement reply = execCommand(request);
				request = makeRequest("volume-destroy", "name", volumeId);
				reply = execCommand(request);
				throw e;
			}
			try {
				String snapPercent = String.valueOf((int)(NetappInfo.getStorageInfo().getSnapReserve()));
				NaElement request = makeRequest("snapshot-set-reserve",
						"percentage", snapPercent,
						"volume", volumeId);
				NaElement reply = execCommand(request);
				request = makeRequest("volume-list-info", "volume", volumeId);
				reply = execCommand(request);
				String sizeTotal = null;
				List volStatus = reply.getChildByName("volumes").getChildren();
				for(Iterator i = volStatus.iterator(); i.hasNext();) {
					NaElement volumeInfo = (NaElement) i.next();
					sizeTotal = volumeInfo.getChildContent("size-total");
				}
				long volSizeTotal = Long.parseLong(sizeTotal);
				long lunSize = (long) ((volSizeTotal) - (volSizeTotal * (StorageProperties.NETAPP_META_OVERHEAD / 1000))); 
				//resize if necessary
				if(sizeDiff > 0) {
					request = makeRequest("lun-resize", "path", "/vol/" + volumeId + "/lun1", "size", String.valueOf(lunSize));
					reply = execCommand(request);
				}
				//set lun online
				request = makeRequest("lun-online", 
						"path", "/vol/" + volumeId + "/lun1");
				reply = execCommand(request);
				//target IQN is the same for all volumes
				return getTargetIqn();
			} catch (EucalyptusCloudException e) {
				LOG.error("Unable to create lun for volume: " + volumeId);
				NaElement request = makeRequest("volume-offline", "name", volumeId);
				NaElement reply = execCommand(request);
				request = makeRequest("volume-destroy", "name", volumeId);
				reply = execCommand(request);
				throw e;
			}		
		} else {
			throw new EucalyptusCloudException("Snapshot " + snapshotId + " not found");
		}
	}

	@Override
	public String createVolume(String volumeId, int size) throws EucalyptusCloudException {
		volumeId = sanitizeVolumeId(volumeId);
		if(volumeExists(volumeId)) {
			throw new EucalyptusCloudException("Volume already exists: " + volumeId);
		}
		//create flexvol
		int sizeInMB;
		try {
			long vol_size = size * StorageProperties.GB;
			vol_size += (vol_size * (NetappInfo.getStorageInfo().getSnapReserve() / 100)) + (vol_size * (StorageProperties.NETAPP_META_OVERHEAD / 1000));
			sizeInMB = (int) (vol_size / StorageProperties.MB);
			NaElement request = makeRequest("volume-create", 
					"containing-aggr-name", NetappInfo.getStorageInfo().getAggregate(), 
					"volume", volumeId,
					"size", sizeInMB + "m");
			NaElement reply = execCommand(request);
		} catch (EucalyptusCloudException e) {
			throw new EucalyptusCloudException("Unable to create volume: " + volumeId + " " + e);
		}		
		try {
			String snapPercent = String.valueOf(NetappInfo.getStorageInfo().getSnapReserve());
			NaElement request = makeRequest("snapshot-set-reserve",
					"percentage", snapPercent,
					"volume", volumeId);
			NaElement reply = execCommand(request);
			request = makeRequest("volume-list-info", "volume", volumeId);
			reply = execCommand(request);
			String sizeAvailable = null;
			List volStatus = reply.getChildByName("volumes").getChildren();
			for(Iterator i = volStatus.iterator(); i.hasNext();) {
				NaElement volumeInfo = (NaElement) i.next();
				sizeAvailable = volumeInfo.getChildContent("size-available");
			}
			long volSizeAvailable = Long.parseLong(sizeAvailable);
			long lunSize = (long) ((volSizeAvailable) - (volSizeAvailable * (StorageProperties.NETAPP_META_OVERHEAD / 1000))); 
			//create lun
			request = makeRequest("lun-create-by-size", 
					"ostype", "linux",
					"path", "/vol/" + volumeId + "/lun1",
					"size", String.valueOf(lunSize));
			reply = execCommand(request);
			//target IQN is the same for all volumes
			return getTargetIqn();
		} catch (EucalyptusCloudException e) {
			LOG.error("Unable to create lun for volume: " + volumeId);
			NaElement request = makeRequest("volume-offline", "name", volumeId);
			NaElement reply = execCommand(request);
			request = makeRequest("volume-destroy", "name", volumeId);
			reply = execCommand(request);
			throw e;
		}
	}

	private String sanitizeVolumeId(String volumeId) {
		volumeId = volumeId.replaceAll("-", "_");
		return volumeId;
	}

	private boolean volumeExists(String volumeId) {
		//return true if exists
		try {
			NaElement request = makeRequest("volume-list-info", "volume", volumeId);
			NaElement reply = execCommand(request);
			return true;
		} catch (EucalyptusCloudException ex) {
			//volume does not exist. do nothing.
		}
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		SANVolumeInfo searchVolumeInfo = new SANVolumeInfo(volumeId);
		try {
			SANVolumeInfo volumeInfo = db.getUnique(searchVolumeInfo);
			db.delete(volumeInfo);
		} catch (EucalyptusCloudException ex) {			
		} finally {
			db.commit();
		}
		return false;
	}

	@Override
	public boolean deleteSnapshot(String volumeId, String snapshotId,
			boolean locallyCreated) {
		volumeId = sanitizeVolumeId(volumeId);
		snapshotId = sanitizeVolumeId(snapshotId);
		if(volumeExists(snapshotId)) {
			EntityWrapper<IgroupInfo> db = StorageProperties.getEntityWrapper();
			String groupName = null;
			try {
				IgroupInfo igroup = db.getUnique(new IgroupInfo(StorageProperties.NAME));
				groupName = igroup.getiGroupName();
			} catch (EucalyptusCloudException ex) {
				LOG.error("Unable to find initiator group for: " + StorageProperties.NAME);
			} finally {
				db.commit();
			}
			String lunPath = "/vol/" + snapshotId + "/lun1";
			try {
				unmapLun(lunPath, groupName);
				//destroy lun
				NaElement request = makeRequest("lun-destroy", "path", lunPath);
				NaElement reply = execCommand(request);
				//offline volume and delete it
				request = makeRequest("volume-offline", "name", snapshotId);
				reply = execCommand(request);
				request = makeRequest("volume-destroy", "name", snapshotId);
				reply = execCommand(request);
			} catch (EucalyptusCloudException e) {
				LOG.error(e);
				return false;
			}			
			return true;
		} else {
			LOG.error("Snapshot not found: " + snapshotId);
			return true;				
		}
	}

	@Override
	public void deleteUser(String userName) throws EucalyptusCloudException {
		//not currently used
	}

	@Override
	public boolean deleteVolume(String volumeId) {
		volumeId = sanitizeVolumeId(volumeId);
		if(volumeExists(volumeId)) {
			try {
				//make sure no lun mappings exist and
				//remove igroup
				EntityWrapper<IgroupInfo> db = StorageProperties.getEntityWrapper();
				try {
					IgroupInfo igroup = db.getUnique(new IgroupInfo(volumeId));
					NaElement request = makeRequest("igroup-destroy", 
							"initiator-group-name", igroup.getiGroupName());
					NaElement reply = execCommand(request);
					db.delete(igroup);
				} catch (EucalyptusCloudException ex) {
					//Check if it is a snapshot
					if(volumeId.startsWith("snap")) {
						String groupName = null;
						try {
							IgroupInfo igroup = db.getUnique(new IgroupInfo(StorageProperties.NAME));
							groupName = igroup.getiGroupName();
						} catch (EucalyptusCloudException e) {
							LOG.warn("Unable to find initiator group for: " + StorageProperties.NAME);
						}
						if(groupName != null) {
							String lunPath = "/vol/" + volumeId + "/lun1";
							try {
								unmapLun(lunPath, groupName);
							} catch (EucalyptusCloudException e) {
								LOG.warn(e);
							}
						}
					}
				} finally {
					db.commit();
				}
				//destroy lun
				NaElement request = makeRequest("lun-destroy", "path", "/vol/" + volumeId + "/lun1");
				NaElement reply = execCommand(request);
				//offline volume and delete it
				request = makeRequest("volume-offline", "name", volumeId);
				reply = execCommand(request);
				request = makeRequest("volume-destroy", "name", volumeId);
				reply = execCommand(request);
				return true;
			} catch (EucalyptusCloudException ex) {
				LOG.error(ex);
				return false;
			}		
		} else {
			//record error, clean up anyway
			LOG.error("Volume not found: " + volumeId);
			return true;
		}
	}

	@Override
	public void disconnectTarget(String snapshotId, String iqn) throws EucalyptusCloudException {
		snapshotId = sanitizeVolumeId(snapshotId);
		if(volumeExists(snapshotId)) {
			String igroupName;
			EntityWrapper<IgroupInfo> db = StorageProperties.getEntityWrapper();
			try {
				IgroupInfo igroup = db.getUnique(new IgroupInfo(StorageProperties.NAME));
				igroupName = igroup.getiGroupName();
			} catch (EucalyptusCloudException ex) {
				LOG.error(ex);
				throw ex;
			} finally {
				db.commit();
			}
			String lunPath = "/vol/" + snapshotId + "/lun1";
			try {
				unmapLun(lunPath, igroupName);				
			} catch (EucalyptusCloudException e) {
				LOG.error(e);
				throw e;
			}
			try {
				String encryptedPassword = "not_required";
				try {
					SANInfo sanInfo = SANInfo.getStorageInfo();
					String deviceString = null;
					String[] parts = iqn.split(",");
					if(parts.length > 1) {
						deviceString = System.getProperty("euca.home") + "," + 
						sanInfo.getSanHost() + "," + 
						parts[0] + "," + 
						encryptedPassword + "," + 
						parts[1] + "," + 
						StorageProperties.IscsiAuthType.HBA.toString();
					} else {
						throw new EucalyptusCloudException("Invalid remote device string.");
					}
					String returnValue = SystemUtil.run(new String[]{StorageProperties.eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, BaseDirectory.LIB.toString() + File.separator + 
							"disconnect_iscsitarget_sc.pl", deviceString});
					if(returnValue.length() == 0) {
						throw new EucalyptusCloudException("Unable to disconnect target");
					}
				} catch (ExecutionException e) {
					throw new EucalyptusCloudException("Unable to connect to storage target");
				}				
			} catch(EucalyptusCloudException ex) {
				throw new EucalyptusCloudException(ex);
			}
		} else {
			throw new EucalyptusCloudException("Snapshot not found: " + snapshotId);
		}
	}

	public NaElement execCommand(NaElement request) throws EucalyptusCloudException {
		NetappSANTask task = new NetappSANTask(request);
		try {
			sessionManager.addTask(task);
			synchronized (task) {
				if(task.getValue() == null) {
					task.wait(TASK_TIMEOUT);
				}
			}
			if(task.getValue() == null) {				
				throw new EucalyptusCloudException("Unable to execute command: " + task.getCommand() + " Error:" + task.getErrorMessage());
			} else {
				return task.getValue();
			}					
		} catch (InterruptedException e) {
			LOG.error(e);
			throw new EucalyptusCloudException("Command interrupted: " + task.getCommand());
		}
	}

	@Override
	public String getVolumeProperty(String volumeId) {
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		try {
			SANInfo sanInfo = SANInfo.getStorageInfo();
			SANVolumeInfo searchVolumeInfo = new SANVolumeInfo(volumeId);
			SANVolumeInfo volumeInfo = db.getUnique(searchVolumeInfo);
			String property = System.getProperty("euca.home") + "," + 
			sanInfo.getSanHost() + "," + 
			volumeInfo.getIqn() + "," + "not_required";
			db.commit();
			return property;
		} catch(EucalyptusCloudException ex) {
			LOG.error(ex);
			db.rollback();
			return null;
		}
	}

	private NaElement makeRequest(String command, String... args) {
		NaElement request = new NaElement(command);
		if ((args.length % 2) != 0) { 
			return request;
		}
		for (int i = 0; i < args.length ; i += 2) {
			request.addNewChild(args[i], args[i+1]);
		}
		return request;
	}

	@Override
	public int addInitiatorRule(String volumeId, String nodeIqn)
	throws EucalyptusCloudException {
		if(nodeIqn != null) {
			volumeId = sanitizeVolumeId(volumeId);
			EntityWrapper<IgroupInfo> db = StorageProperties.getEntityWrapper();
			String igroupName;
			try {
				IgroupInfo igroup = db.getUnique(new IgroupInfo(volumeId));
				//add iqn to existing igroup
				igroupName = igroup.getiGroupName();
				addInitiator(igroup.getiGroupName(), nodeIqn);
				igroup.setIqn(nodeIqn);
			} catch (EucalyptusCloudException ex) {
				List<IgroupInfo> igroups = db.query(new IgroupInfo(volumeId));
				if(igroups.size() > 1) {
					for(IgroupInfo igroup : igroups) {
						try {
							removeIgroup(igroup.getiGroupName(),true);
						} catch (EucalyptusCloudException e) {
							LOG.error(e);
						}
						db.delete(igroup);
					}
				}
				igroupName = "igroup" + Hashes.getRandom(16);
				createIgroup(igroupName);//create igroup and add iqn
				//add iqn to igroup
				try {
					addInitiator(igroupName, nodeIqn);
					IgroupInfo igroup = new IgroupInfo(igroupName, volumeId, nodeIqn);
					db.add(igroup);
				} catch(EucalyptusCloudException e) {
					removeInitiator(igroupName, nodeIqn);
					removeIgroup(igroupName, true);
				}
			} finally {
				db.commit();
			}
			//map lun.		
			volumeId = sanitizeVolumeId(volumeId);
			String lunPath = "/vol/" + volumeId + "/lun1";
			int lun = mapLun(lunPath, igroupName);
			//create incoming security rule
			addInitiatorAuth(nodeIqn);
			return lun;
		} else {
			throw new EucalyptusCloudException("addInitiatorRule: IQN is null. Cannot proceed.");
		}
	}

	private void addInitiatorAuth(String iqn) throws EucalyptusCloudException {
		NaElement request = makeRequest("iscsi-initiator-add-auth",
				"auth-type", "none",
				"initiator", iqn);
		NaElement reply = execCommand(request);
	}

	private void checkAddInitiatorAuth(String iqn) throws EucalyptusCloudException {
		try {
			NaElement request = makeRequest("iscsi-initiator-get-auth",
					"initiator", iqn);
			NaElement reply = execCommand(request);
			String authType = reply.getChildContent("auth-type");
			if(!("none".equals(authType))) {
				request = makeRequest("iscsi-initiator-delete-auth",
						"initiator", iqn);
				reply = execCommand(request);
				request = makeRequest("iscsi-initiator-add-auth",
						"auth-type", "none",
						"initiator", iqn);
				reply = execCommand(request);
			}
		} catch (EucalyptusCloudException ex) {
			NaElement request = makeRequest("iscsi-initiator-add-auth",
					"auth-type", "none",
					"initiator", iqn);
			NaElement reply = execCommand(request);
		}
	}

	private void removeInitiatorAuth(String iqn) throws EucalyptusCloudException {
		NaElement request = makeRequest("iscsi-initiator-delete-auth",
				"initiator", iqn);
		NaElement reply = execCommand(request);
	}

	private int mapLun(String lunPath, String igroupName) throws EucalyptusCloudException {
		NaElement request = makeRequest("lun-map",
				"initiator-group", igroupName,
				"path", lunPath);
		NaElement reply = execCommand(request);
		return Integer.parseInt(reply.getChildContent("lun-id-assigned"));
	}

	private void addInitiator(String igroupName, String nodeIqn) throws EucalyptusCloudException {
		if(nodeIqn != null) {
			NaElement request = makeRequest("igroup-list-info", "initiator-group-name", igroupName);
			NaElement reply = execCommand(request);
			//remove any existing initiators
			List igroups = reply.getChildByName("initiator-groups").getChildren();
			for (Iterator i = igroups.iterator(); i.hasNext();) {
				NaElement igroup = (NaElement) i.next();
				List initiators = igroup.getChildByName("initiators").getChildren();
				for (Iterator ii = initiators.iterator(); ii.hasNext();) {
					NaElement initiator = (NaElement) ii.next();
					String iname = initiator.getChildContent("initiator-name");
					request = makeRequest("igroup-remove",
							"initiator-group-name", igroupName,
							"initiator", iname);
					reply = execCommand(request);
				}
			}
			request = makeRequest("igroup-add",
					"initiator", nodeIqn,
					"initiator-group-name", igroupName);
			reply = execCommand(request);
		} else {
			throw new EucalyptusCloudException("addInitiator: IQN is null. Cannot proceed.");
		}
	}

	private void createIgroup(String igroupName) throws EucalyptusCloudException {
		NaElement request = makeRequest("igroup-create", 
				"initiator-group-name", igroupName,
				"initiator-group-type", "iscsi",
				"os-type", "linux");
		NaElement reply = execCommand(request);
	}

	private void removeIgroup(String igroupName, Boolean force) throws EucalyptusCloudException {
		NaElement request = makeRequest("igroup-destroy", 
				"initiator-group-name", igroupName);
		if(force) {
			request.addNewChild("force", "true");
		}
		NaElement reply = execCommand(request);
	}

	@Override
	public void removeInitiatorRule(String volumeId, String nodeIqn)
	throws EucalyptusCloudException {
		volumeId = sanitizeVolumeId(volumeId);
		EntityWrapper<IgroupInfo> db = StorageProperties.getEntityWrapper();
		String groupName = null;
		try {
			IgroupInfo igroup = db.getUnique(new IgroupInfo(volumeId));
			groupName = igroup.getiGroupName();
			igroup.setIqn(null);
		} catch (EucalyptusCloudException ex) {
			throw new EucalyptusCloudException("Unable to find initiator group for volume: " + volumeId);
		} finally {
			db.commit();
		}
		String lunPath = "/vol/" + volumeId + "/lun1";
		if(onlyNode(nodeIqn)) {
			try { 
				removeInitiatorAuth(nodeIqn);
			} catch(EucalyptusCloudException ex) {
				LOG.error(ex);
			}
		}
		try { 
			unmapLun(lunPath, groupName);
		} catch(EucalyptusCloudException ex) {
			LOG.error(ex);
		}
		removeInitiator(groupName, nodeIqn);
	}

	private boolean onlyNode(String nodeIqn) {
		EntityWrapper<IgroupInfo> db = StorageProperties.getEntityWrapper();
		IgroupInfo igroup = new IgroupInfo();
		igroup.setIqn(nodeIqn);
		List<IgroupInfo> igroups = db.query(igroup);
		boolean success = false;		
		if(igroups.size() == 0) {
			success = true;
		}
		db.commit();
		return success;
	}

	private void unmapLun(String lunPath, String groupName) throws EucalyptusCloudException {
		NaElement request = makeRequest("lun-unmap",
				"initiator-group", groupName,
				"path", lunPath);
		execCommand(request);
	}

	private void removeInitiator(String groupName, String nodeIqn) throws EucalyptusCloudException {
		NaElement request = makeRequest("igroup-remove",
				"initiator", nodeIqn,
				"initiator-group-name", groupName);
		execCommand(request);
	}

	@Override
	public void getStorageProps(ArrayList<ComponentProperty> componentProperties) {
		ConfigurableClass configurableClass = NetappInfo.class.getAnnotation(ConfigurableClass.class);
		if(configurableClass != null) {
			String root = configurableClass.root();
			String alias = configurableClass.alias();
			if(componentProperties == null)
				componentProperties = (ArrayList<ComponentProperty>) PropertyDirectory.getComponentPropertySet(StorageProperties.NAME + "." + root, alias);
			else 
				componentProperties.addAll(PropertyDirectory.getComponentPropertySet(StorageProperties.NAME + "." + root, alias));
		}
	}

	@Override
	public void setStorageProps(ArrayList<ComponentProperty> storageProps) {
		this.configure();
	}

	@Override
	public void stop() throws EucalyptusCloudException {
	}
}

