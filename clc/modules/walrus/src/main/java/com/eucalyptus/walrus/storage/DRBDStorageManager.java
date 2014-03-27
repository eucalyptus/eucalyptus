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

package com.eucalyptus.walrus.storage;

import java.io.File;

import org.apache.log4j.Logger;

import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.walrus.WalrusBackend;
import com.eucalyptus.walrus.entities.DRBDInfo;
import com.eucalyptus.walrus.entities.WalrusInfo;
import com.eucalyptus.walrus.util.WalrusProperties;

import java.util.concurrent.ExecutionException;

import edu.ucsb.eucalyptus.util.SystemUtil;

public class DRBDStorageManager extends FileSystemStorageManager {

	private static Logger LOG = Logger.getLogger( DRBDStorageManager.class );
	private static final String PRIMARY_ROLE = "Primary";
	private static final String SECONDARY_ROLE = "Secondary";
	private static final String DSTATE_UPTODATE = "UpToDate";
	private static final String DSTATE_UNKNOWN = "Unknown";
	private static final String CSTATE_WFCONNECTION = "WFConnection";
	private static final String CSTATE_CONNECTED = "Connected";

	public DRBDStorageManager() {
	}

	private String getConnectionStatus() throws ExecutionException, EucalyptusCloudException {
		String returnValue = SystemUtil.run(new String[]{WalrusProperties.EUCA_ROOT_WRAPPER, "drbdadm", "cstate", DRBDInfo.getDRBDInfo().getResource()});
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("Unable to get connection status for resource: " + DRBDInfo.getDRBDInfo().getResource());
		}
		return returnValue;
	}

	private String getDataStatus() throws ExecutionException, EucalyptusCloudException {
		String returnValue = SystemUtil.run(new String[]{WalrusProperties.EUCA_ROOT_WRAPPER, "drbdadm", "dstate", DRBDInfo.getDRBDInfo().getResource()});
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("Unable to get data status for resource: " + DRBDInfo.getDRBDInfo().getResource());
		}
		return returnValue;
	}

	/**
	 * @return
	 * @throws ExecutionException
	 * @throws EucalyptusCloudException
	 */
	private String getRole() throws ExecutionException, EucalyptusCloudException {
		String returnValue = SystemUtil.run(new String[]{WalrusProperties.EUCA_ROOT_WRAPPER, "drbdadm", "role", DRBDInfo.getDRBDInfo().getResource()});
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("Unable to get role for resource: " + DRBDInfo.getDRBDInfo().getResource());
		}
		return returnValue;
	}

	/*This method does not check if resource is already primary*/
	/**
	 * @throws ExecutionException
	 * @throws EucalyptusCloudException
	 */
	private void makePrimary() throws ExecutionException, EucalyptusCloudException {
		if(SystemUtil.runAndGetCode(new String[]{WalrusProperties.EUCA_ROOT_WRAPPER, "drbdadm", "primary", DRBDInfo.getDRBDInfo().getResource()}) != 0) {
			throw new EucalyptusCloudException("Unable to make resource " + DRBDInfo.getDRBDInfo().getResource() + " primary");
		}
	}

	/*This method does not check if resource is already secondary*/
	/**
	 * @throws ExecutionException
	 * @throws EucalyptusCloudException
	 */
	private void makeSecondary() throws ExecutionException, EucalyptusCloudException {		
		if(SystemUtil.runAndGetCode(new String[]{WalrusProperties.EUCA_ROOT_WRAPPER, "drbdadm", "secondary", DRBDInfo.getDRBDInfo().getResource()}) != 0) {
			throw new EucalyptusCloudException("Unable to make resource " + DRBDInfo.getDRBDInfo().getResource() + " secondary");
		}
	}

	private void connectResource() throws ExecutionException, EucalyptusCloudException {
		if(SystemUtil.runAndGetCode(new String[]{WalrusProperties.EUCA_ROOT_WRAPPER, "drbdadm", "connect", DRBDInfo.getDRBDInfo().getResource()}) != 0) {
			throw new EucalyptusCloudException("Unable to connect resource: " + DRBDInfo.getDRBDInfo().getResource());
		}
	}

	private void disconnectResource() throws ExecutionException, EucalyptusCloudException {
		if(SystemUtil.runAndGetCode(new String[]{WalrusProperties.EUCA_ROOT_WRAPPER, "drbdadm", "disconnect", DRBDInfo.getDRBDInfo().getResource()}) != 0) {
			throw new EucalyptusCloudException("Unable to disconnect resource: " + DRBDInfo.getDRBDInfo().getResource());
		}
	}

	//create config
	private void generateConfig() {

	}

	private void mountPrimary() throws ExecutionException, EucalyptusCloudException {
		if(SystemUtil.runAndGetCode(new String[]{"mount", DRBDInfo.getDRBDInfo().getBlockDevice()}) != 0) {
			throw new EucalyptusCloudException("Unable to mount " + DRBDInfo.getDRBDInfo().getBlockDevice() + " as " + WalrusInfo.getWalrusInfo().getStorageDir());
		}
		if(SystemUtil.runAndGetCode(new String[]{WalrusProperties.EUCA_ROOT_WRAPPER, "chown", "eucalyptus:eucalyptus", WalrusInfo.getWalrusInfo().getStorageDir()}) != 0) {
                        throw new EucalyptusCloudException("Unable to change perms on: " + WalrusInfo.getWalrusInfo().getStorageDir());
                }
		SystemUtil.setEucaReadWriteOnly(WalrusInfo.getWalrusInfo().getStorageDir());
	}

	private void unmountPrimary() throws ExecutionException, EucalyptusCloudException {
		if(SystemUtil.runAndGetCode(new String[]{"umount", DRBDInfo.getDRBDInfo().getBlockDevice()}) != 0) {
			throw new EucalyptusCloudException("Unable to unmount " + DRBDInfo.getDRBDInfo().getBlockDevice());
		}
	}

	/**
	 * We use /proc/mounts because EUCA_ROOT_WRAPPER uses a syscall and does not update /etc/mtab
	 * @return
	 * @throws ExecutionException
	 */
	private boolean isMounted() throws EucalyptusCloudException {
		String returnValue = SystemUtil.run(new String[]{WalrusProperties.EUCA_ROOT_WRAPPER, "cat", "/proc/mounts"});
		if(returnValue.length() > 0) {
			if(returnValue.contains(DRBDInfo.getDRBDInfo().getBlockDevice())) {
				return true;
			}
		}
		return false;
	}

	private boolean isPrimary() throws EucalyptusCloudException, ExecutionException {
		String roleString = getRole();
		String[] roleParts = roleString.split("/");
		if(roleParts.length > 1) {
			if(roleParts[0].startsWith(PRIMARY_ROLE)) {
				return true;
			} else {
				return false;
			}			 
		} else {
			throw new EucalyptusCloudException("Unable to parse role.");
		}
	}

	private boolean isSecondary() throws EucalyptusCloudException, ExecutionException {
		String roleString = getRole();
		String[] roleParts = roleString.split("/");
		if(roleParts.length > 1) {
			if(roleParts[0].startsWith(SECONDARY_ROLE)) {
				return true;
			} else {
				return false;
			}			 
		} else {
			throw new EucalyptusCloudException("Unable to parse role.");
		}
	}

	private boolean isPeerPrimary() throws EucalyptusCloudException, ExecutionException {
		String roleString = getRole();
		String[] roleParts = roleString.split("/");
		if(roleParts.length > 1) {
			if(roleParts[1].startsWith(PRIMARY_ROLE)) {
				return true;
			} else {
				return false;
			}			 
		} else {
			throw new EucalyptusCloudException("Unable to parse role.");
		}
	}

	private boolean isPeerSecondary() throws EucalyptusCloudException, ExecutionException {
		String roleString = getRole();
		String[] roleParts = roleString.split("/");
		if(roleParts.length > 1) {
			if(roleParts[1].startsWith(SECONDARY_ROLE)) {
				return true;
			} else {
				return false;
			}			 
		} else {
			throw new EucalyptusCloudException("Unable to parse role.");
		}
	}

	private boolean isConnected() throws ExecutionException, EucalyptusCloudException {
		String cstateString = getConnectionStatus();
		if((cstateString != null) && cstateString.startsWith(CSTATE_CONNECTED)) {
			return true;
		} else {
			return false;
		}
	}

	private boolean isUpToDate() throws EucalyptusCloudException, ExecutionException {
		String dstateString = getDataStatus();
		String[] dstateParts = dstateString.split("/");
		if(dstateParts.length > 1) {
			if(dstateParts[0].startsWith(DSTATE_UPTODATE) && (dstateParts[1].startsWith(DSTATE_UPTODATE))) {
				return true;
			} else {
				return false;
			}
		} else {
			throw new EucalyptusCloudException("Unable to get resource dstate.");
		}		
	}

	private void checkLocalDisk() throws EucalyptusCloudException {		
		String blockDevice = DRBDInfo.getDRBDInfo().getBlockDevice();
		File mount = new File(blockDevice);
		if(!mount.exists()) {
			throw new EucalyptusCloudException("Block device " + blockDevice + " not found."); 
		}
		String storageDir = WalrusInfo.getWalrusInfo().getStorageDir();
		File root = new File(storageDir);
		if(!root.exists()) {
			throw new EucalyptusCloudException("Storage directory " + storageDir + " not found."); 			
		}
	}

	public void becomeMaster() throws EucalyptusCloudException, ExecutionException {		
		checkLocalDisk();
		//role, cstate, dstate
		if(!isPrimary()) {
			//make primary
			if(isPeerPrimary()) {
				throw new EucalyptusCloudException("Peer is primary and I am supposed to be master! Unable to proceed!");
			}
			makePrimary();
		}
		if(!isConnected()) {
			try {
				connectResource();
			} catch (Exception e) {
				LOG.error(e);
			}
		}
		//mount
		if(!isMounted()) {
			try {
				mountPrimary();
			} catch(Exception e) {
				//undo
				LOG.error(e, e);
				if(isPrimary()) {
					makeSecondary();
				}
				throw new EucalyptusCloudException("Unable to mount primary: " + e);
			}
		}
		//verify state
		if(!isPrimary()) {
			throw new EucalyptusCloudException("Unable to make resource primary.");
		}
	}

	public void becomeSlave() throws EucalyptusCloudException, ExecutionException {
		checkLocalDisk();
		//check mount point, block device, role, cstate, dstate
		if(isMounted()) {
			unmountPrimary();
		}
		if(!isSecondary()) {
			//make secondary
			makeSecondary();
		}
		if(!isConnected()) {
			try {
				connectResource();
			} catch(Exception e) {
				LOG.error(e);
			}
		}
		//verify state
		if(!isSecondary()) {
			throw new EucalyptusCloudException("Unable to make resource secondary.");
		}
		if(!isPeerPrimary()) {
			LOG.warn("Warning! Peer is not primary. No usable component?");
		}
	}
	//check status

	public void secondaryDrasticRecovery() throws ExecutionException, EucalyptusCloudException {
		if(SystemUtil.runAndGetCode(new String[]{WalrusProperties.EUCA_ROOT_WRAPPER, "drbdadm", "--", "--discard-my-data", "connect", DRBDInfo.getDRBDInfo().getResource()}) != 0) {
			throw new EucalyptusCloudException("Unable to recover from split brain for resource: " + DRBDInfo.getDRBDInfo().getResource());
		}
	}

	@Override
	public void enable() throws EucalyptusCloudException {
		try {
			becomeMaster();
		} catch (ExecutionException e) {
			throw new EucalyptusCloudException(e);
		}
	}

	@Override
	public void disable() throws EucalyptusCloudException {
		try {
			becomeSlave();
		} catch (ExecutionException e) {
			throw new EucalyptusCloudException(e);
		}
	}
	//verify consistency

	@Override
	public void check() throws EucalyptusCloudException {
		try {
			boolean notConnected = false;
			if(!isConnected()) {
				try {
					connectResource();
				} catch(Exception e) {
					LOG.error(e);
				}
			}
			if (Component.State.ENABLED.equals(Components.lookup(WalrusBackend.class).getState())) {
				if(!isPrimary()  || !isMounted()) {
					throw new EucalyptusCloudException("I am the master, but not DRBD primary. Please make me primary. Aborting!");
				}
			} else {
				if((isConnected()) && (!isUpToDate())) {
					throw new EucalyptusCloudException("Resource connected but not up to date!");
				}
				if (Component.State.DISABLED.equals(Components.lookup(WalrusBackend.class).getState())) {
					if(!isSecondary()) {
						LOG.warn("I am the slave, but not DRBD secondary. Trying to become secondary...");
						if(isMounted()) {
							unmountPrimary();
						}
						//make secondary
						makeSecondary();
						if(!isSecondary()) {
							throw new EucalyptusCloudException("Attempt to set secondary failed. Unable to proceed!");
						}
					}
				}
			}
		} catch(ExecutionException ex) {
			throw new EucalyptusCloudException(ex);
		}
	}

	@Override
	public void start() throws EucalyptusCloudException {
		try {
			becomeSlave();
		} catch (ExecutionException e) {
			throw new EucalyptusCloudException(e);
		}
	}

	@Override
	public void stop() throws EucalyptusCloudException {
		try {
			if(isMounted()) {
				unmountPrimary();
			}
			if(!isSecondary()) {
				//make secondary
				makeSecondary();
			}
		} catch(ExecutionException ex) {
			throw new EucalyptusCloudException(ex);
		}
	}

}
