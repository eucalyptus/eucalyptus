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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.BlockStorageUtil;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.ExecutionException;
import com.eucalyptus.util.StorageProperties;

import edu.ucsb.eucalyptus.cloud.entities.CHAPUserInfo;
import edu.ucsb.eucalyptus.cloud.entities.SANVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.SANInfo;
import edu.ucsb.eucalyptus.msgs.ComponentProperty;
import edu.ucsb.eucalyptus.util.SystemUtil;

public class EquallogicProvider implements SANProvider {
	private static Logger LOG = Logger.getLogger(EquallogicProvider.class);
	private final String TARGET_USERNAME = "eucalyptus"; 
	private boolean enabled;
	private ShellSessionManager sessionManager;

	private static final Pattern VOLUME_CREATE_PATTERN = Pattern.compile(".*iSCSI target name is (.*)\r");
	private static final Pattern VOLUME_DELETE_PATTERN = Pattern.compile("Volume deletion succeeded.");
	private static final Pattern VOLUME_SHOW_PATTERN = Pattern.compile("Volume Information");
	private static final Pattern USER_CREATE_PATTERN = Pattern.compile(".*Password is (.*)\r");
	private static final Pattern SNAPSHOT_CREATE_PATTERN = Pattern.compile(".*Snapshot name is (.*)\r");
	private static final Pattern SNAPSHOT_TARGET_NAME_PATTERN = Pattern.compile(".*iSCSI Name: (.*)\r");
	private static final Pattern SNAPSHOT_DELETE_PATTERN = Pattern.compile("Snapshot deletion succeeded.");
	private static final Pattern USER_DELETE_PATTERN = Pattern.compile("CHAP user deletion succeeded.");
	private static final Pattern USER_SHOW_PATTERN = Pattern.compile(".*Password: (.*)\r");
	private static final Pattern SHOW_SPACE_PATTERN = Pattern.compile("GB.* ([0-9]+\\.[0-9]+.*)\r");

	private static final String EOF_COMMAND = "whoami\r";

	private final long TASK_TIMEOUT = 5 * 60 * 1000;
	private ScheduledExecutorService sessionRefresh;
	private int REFRESH_PERIODICITY = 30;

	public EquallogicProvider() {
		sessionManager = new ShellSessionManager();
	}

	public void configure() {
		SANInfo sanInfo = SANInfo.getStorageInfo();
		try {
			if(!StorageProperties.DUMMY_SAN_HOST.equals(sanInfo.getSanHost())) {
				sessionManager.update();
				showFreeSpace();
				if(sessionRefresh == null) {
					sessionRefresh = Executors.newSingleThreadScheduledExecutor();
					sessionRefresh.scheduleAtFixedRate(new Runnable() {
						@Override
						public void run() {
							showFreeSpace();
						}}, 1, REFRESH_PERIODICITY, TimeUnit.MINUTES);				
				}
			}
		} catch (EucalyptusCloudException e) {
			LOG.error(e, e);
		}
	}

	public void checkPreconditions() throws EucalyptusCloudException {
	}
	
	public void checkConnection() {
		try {
			SANInfo sanInfo = SANInfo.getStorageInfo();
			if(!StorageProperties.DUMMY_SAN_HOST.equals(sanInfo.getSanHost())) 
				sessionManager.checkConnection();
		} catch (EucalyptusCloudException e) {
			enabled = false;
			return;
		}
		addUser(TARGET_USERNAME);
	}

	public String createVolume(String volumeId, String snapshotId, int snapSize, int size) throws EucalyptusCloudException {
		if(!enabled) {
			checkConnection();
			if(!enabled) {
				throw new EucalyptusCloudException("Unable to create user " + TARGET_USERNAME + " on target. Will not run command. ");				
			}
		}
		if(volumeExists(volumeId)) {
			throw new EucalyptusCloudException("Volume already exists: " + volumeId);				
		}
		String returnValue = execCommand("stty hardwrap off\rvolume select " + snapshotId + 
				" offline\rvolume select " + snapshotId + " clone " + volumeId + "\r");
		String targetName = matchPattern(returnValue, VOLUME_CREATE_PATTERN);
		if(targetName != null) {
			int sizeDiff = size - snapSize;
			if(sizeDiff > 0) {
				returnValue = execCommand("stty hardwrap off\rvolume select " + volumeId + " size " +  (size * StorageProperties.KB) + "\r");
				if(returnValue.length() == 0) {
					throw new EucalyptusCloudException("Unable to resize volume: " + volumeId);
				}
			}
			returnValue = execCommand("volume select " + volumeId + " access create username " + TARGET_USERNAME + "\r");
			if(returnValue.length() == 0) {
				throw new EucalyptusCloudException("Unable to set access for volume: " + volumeId);				
			}
		}
		return targetName;
	}

	public String connectTarget(String iqn) throws EucalyptusCloudException {
		EntityWrapper<CHAPUserInfo> db = StorageProperties.getEntityWrapper();
		try {
			SANInfo sanInfo = SANInfo.getStorageInfo();
			CHAPUserInfo userInfo = db.getUnique(new CHAPUserInfo(TARGET_USERNAME));
			String encryptedPassword = userInfo.getEncryptedPassword();
			db.commit();
			try {
				String deviceName = SystemUtil.run(new String[]{StorageProperties.eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, BaseDirectory.LIB.toString() + File.separator + "connect_iscsitarget_sc.pl", 
						System.getProperty("euca.home") + "," + sanInfo.getSanHost() + "," + iqn + "," + encryptedPassword});
				if(deviceName.length() == 0) {
					throw new EucalyptusCloudException("Unable to get device name. Connect failed.");
				}
				return deviceName;
			} catch (ExecutionException e) {
				throw new EucalyptusCloudException("Unable to connect to storage target");
			}				
		} catch(EucalyptusCloudException ex) {
			db.rollback();
			throw ex;
		}
	}

	public String getVolumeProperty(String volumeId) {
		EntityWrapper<SANVolumeInfo> db = StorageProperties.getEntityWrapper();
		try {
			SANInfo sanInfo = SANInfo.getStorageInfo();
			SANVolumeInfo searchVolumeInfo = new SANVolumeInfo(volumeId);
			SANVolumeInfo volumeInfo = db.getUnique(searchVolumeInfo);
			EntityWrapper<CHAPUserInfo> dbUser = db.recast(CHAPUserInfo.class);
			CHAPUserInfo userInfo = dbUser.getUnique(new CHAPUserInfo("eucalyptus"));
			String property = sanInfo.getSanHost() + "," + volumeInfo.getIqn() + "," + BlockStorageUtil.encryptNodeTargetPassword(BlockStorageUtil.decryptSCTargetPassword(userInfo.getEncryptedPassword()));
			db.commit();
			return property;
		} catch(EucalyptusCloudException ex) {
			LOG.error(ex);
			db.rollback();
			return null;
		}
	}

	public String execCommand(String command) throws EucalyptusCloudException {
		EquallogicSANTask task = new EquallogicSANTask(command, EOF_COMMAND);
		try {
			sessionManager.addTask(task);
			synchronized (task) {
				if(task.getValue() == null) {
					task.wait(TASK_TIMEOUT);
				}
			}
			if(task.getValue() == null) {
				LOG.error("Unable to execute command: " + task.getCommand());
				return "";
			} else {
				return task.getValue();
			}					

		} catch (InterruptedException e) {
			LOG.error(e);
			return "";
		}
	}

	private String matchPattern(String input,
			Pattern pattern) {
		Matcher m = pattern.matcher(input);
		if(m.find()) 
			return m.group(1);
		else
			return null;			
	}

	public String createVolume(String volumeName, int size) throws EucalyptusCloudException {
		if(!enabled) {
			checkConnection();
			if(!enabled) {
				throw new EucalyptusCloudException("Not enabled. Will not run command. ");				
			}
		}
		if(volumeExists(volumeName)) {
			throw new EucalyptusCloudException("Volume already exists: " + volumeName);				
		}
		String returnValue = execCommand("stty hardwrap off\rvolume create " + volumeName + " " + (size * StorageProperties.KB) + "\r");
		String targetName = matchPattern(returnValue, VOLUME_CREATE_PATTERN);
		if(targetName != null) {
			returnValue = execCommand("volume select " + volumeName + " access create username " + TARGET_USERNAME + "\r");
			if(returnValue.length() == 0) {
				throw new EucalyptusCloudException("Unable to set access for volume: " + volumeName);					
			}
		}
		return targetName;
	}

	public boolean deleteVolume(String volumeName) {
		if(!enabled) {
			checkConnection();
			if(!enabled) {
				LOG.error("Not enabled. Will not run command. ");
				return false;
			}
		}
		try {
			if(volumeExists(volumeName)) {
				String returnValue = execCommand("stty hardwrap off\rvolume select " + volumeName + " offline\rvolume delete " + volumeName + "\r");
				if(returnValue.split(VOLUME_DELETE_PATTERN.toString()).length > 1)
					return true;
				else
					return false;
			} else {
				//record error, clean up anyway
				LOG.error("Volume not found: " + volumeName);
				return true;
			}
		} catch(EucalyptusCloudException e) {
			LOG.error(e);
			return false;
		}
	}

	public String createSnapshot(String volumeId, String snapshotId) throws EucalyptusCloudException {
		if(!enabled) {
			checkConnection();
			if(!enabled) {
				throw new EucalyptusCloudException("Not enabled. Will not run command. ");				
			}
		}
		if(!volumeExists(volumeId)) {
			throw new EucalyptusCloudException("Volume not found: " + volumeId);				
		}
		String returnValue = execCommand("stty hardwrap off\rvolume select " + volumeId + " clone " + snapshotId + "\r");
		String targetName = matchPattern(returnValue, VOLUME_CREATE_PATTERN);
		if(targetName != null) {
			returnValue = execCommand("volume select " + snapshotId + " access create username " + TARGET_USERNAME + "\r");
			if(returnValue.length() == 0) {
				throw new EucalyptusCloudException("Unable to set access for volume: " + snapshotId);					
			}
		}
		return targetName;
	}

	public boolean deleteSnapshot(String volumeId, String snapshotId, boolean locallyCreated) {
		if(!enabled) {
			checkConnection();
			if(!enabled) {
				LOG.error("Not enabled. Will not run command. ");
				return false;
			}
		}
		if(locallyCreated) {
			try {				
				String returnValue = execCommand("stty hardwrap off\rvolume select " + volumeId + " snapshot select " + snapshotId + " offline\rvolume select " + volumeId + " snapshot delete " + snapshotId + "\r");
				if(returnValue.split(SNAPSHOT_DELETE_PATTERN.toString()).length > 1)
					return true;
				else
					return false;
			} catch(EucalyptusCloudException e) {
				LOG.error(e);
				return false;
			}
		} else {
			try {
				String returnValue = execCommand("stty hardwrap off\rvolume select " + snapshotId + " offline\rvolume delete " + snapshotId + "\r");
				if(returnValue.split(VOLUME_DELETE_PATTERN.toString()).length > 1)
					return true;
				else
					return false;
			} catch(EucalyptusCloudException e) {
				LOG.error(e);
				return false;
			}
		}
	}

	public void deleteUser(String userName) throws EucalyptusCloudException {
		if(!enabled) {
			checkConnection();
			if(!enabled) {
				throw new EucalyptusCloudException("Not enabled. Will not run command.");
			}
		}
		EntityWrapper<CHAPUserInfo> db = StorageProperties.getEntityWrapper();
		try {
			CHAPUserInfo userInfo = db.getUnique(new CHAPUserInfo(userName));
			String returnValue = execCommand("stty hardwrap off\rchapuser delete " + userName + "\r");
			if(matchPattern(returnValue, USER_DELETE_PATTERN) != null) {
				db.delete(userInfo);
			}
		} catch(EucalyptusCloudException ex) {
			throw new EucalyptusCloudException("Unable to find user: " + userName);
		} finally {
			db.commit();
		}

	}

	public void addUser(String userName){
		EntityWrapper<CHAPUserInfo> db = StorageProperties.getEntityWrapper();
		try {
			CHAPUserInfo userInfo = db.getUnique(new CHAPUserInfo(userName));
			db.commit();
			enabled = true;
		} catch(EucalyptusCloudException ex) {
			db.rollback();
			try {
				String returnValue = execCommand("stty hardwrap off\rchapuser create " + userName + "\r");
				String password = matchPattern(returnValue, USER_CREATE_PATTERN);
				if(password != null) {
					password = password.trim();
					db = StorageProperties.getEntityWrapper();
					CHAPUserInfo userInfo = new CHAPUserInfo(userName, BlockStorageUtil.encryptSCTargetPassword(password));
					db.add(userInfo);
					db.commit();
					enabled = true;
				} else {
					returnValue = execCommand("stty hardwrap off\rchapuser show " + userName + "\r");
					password = matchPattern(returnValue, USER_SHOW_PATTERN);
					if(password != null) {
						password = password.trim();
						db = StorageProperties.getEntityWrapper();
						CHAPUserInfo userInfo = new CHAPUserInfo(userName, BlockStorageUtil.encryptSCTargetPassword(password));
						db.add(userInfo);
						db.commit();
						enabled = true;
					}
				}
				returnValue = execCommand("stty hardwrap off\rcli-settings confirmation off\r");
			} catch (EucalyptusCloudException e) {
				LOG.error(e);
			}
		}
	}

	public void disconnectTarget(String snapshotId, String iqn) throws EucalyptusCloudException {
		EntityWrapper<CHAPUserInfo> db = StorageProperties.getEntityWrapper();
		try {
			CHAPUserInfo userInfo = db.getUnique(new CHAPUserInfo(TARGET_USERNAME));
			String encryptedPassword = userInfo.getEncryptedPassword();
			db.commit();
			try {
				SANInfo sanInfo = SANInfo.getStorageInfo();
				String returnValue = SystemUtil.run(new String[]{StorageProperties.eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, BaseDirectory.LIB.toString() + File.separator + "disconnect_iscsitarget_sc.pl", System.getProperty("euca.home") + "," +  
						sanInfo.getSanHost() + "," + iqn + "," + encryptedPassword});
				if(returnValue.length() == 0) {
					throw new EucalyptusCloudException("Unable to disconnect target");
				}
			} catch (ExecutionException e) {
				throw new EucalyptusCloudException("Unable to connect to storage target");
			}				
		} catch(EucalyptusCloudException ex) {
			db.rollback();
			throw new EucalyptusCloudException(ex);
		}
	}

	public boolean volumeExists(String volumeId) throws EucalyptusCloudException {
		String returnValue = execCommand("stty hardwrap off\rvolume show " + volumeId + " \r");
		if((returnValue.split(VOLUME_SHOW_PATTERN.toString()).length > 1) && returnValue.contains(volumeId)) {
			return true;
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

	private void showFreeSpace() {
		try {
			String returnValue = execCommand("stty hardwrap off\rshow pool\r");
			String freeSpaceString = matchPattern(returnValue, SHOW_SPACE_PATTERN);
			SANInfo sanInfo = SANInfo.getStorageInfo();
			if(freeSpaceString != null && (freeSpaceString.length() > 0)) {
				LOG.info("Free Space on " + sanInfo.getSanHost() + " : " + freeSpaceString);
			}
		} catch (EucalyptusCloudException e) {
			LOG.error(e);
		}
	}

	@Override
	public int addInitiatorRule(String volumeId, String nodeIqn)
	throws EucalyptusCloudException {
		//nothing to do here yet.
		return -1;
	}

	@Override
	public void removeInitiatorRule(String volumeId, String nodeIqn)
	throws EucalyptusCloudException {
		//nothing to do here yet.
	}

	@Override
	public void getStorageProps(ArrayList<ComponentProperty> componentProperties) {
		//nothing to do here.		
	}

	@Override
	public void setStorageProps(ArrayList<ComponentProperty> storageProps) {
		this.configure();
	}

	@Override
	public void stop() throws EucalyptusCloudException {
		sessionManager.stop();
	}
}

