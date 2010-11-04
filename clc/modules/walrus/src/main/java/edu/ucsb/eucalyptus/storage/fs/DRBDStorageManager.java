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

package edu.ucsb.eucalyptus.storage.fs;

import org.apache.log4j.Logger;

import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.ExecutionException;
import com.eucalyptus.util.WalrusProperties;

import edu.ucsb.eucalyptus.cloud.entities.DRBDInfo;
import edu.ucsb.eucalyptus.cloud.entities.WalrusInfo;
import edu.ucsb.eucalyptus.storage.fs.FileSystemStorageManager;
import edu.ucsb.eucalyptus.util.SystemUtil;

public class DRBDStorageManager extends FileSystemStorageManager {

	private static Logger LOG = Logger.getLogger( DRBDStorageManager.class );

	public DRBDStorageManager(String rootDirectory) {
		super(rootDirectory);
	}

	private String getConnectionStatus() throws ExecutionException, EucalyptusCloudException {
		String returnValue = SystemUtil.run(new String[]{WalrusProperties.eucaHome + WalrusProperties.EUCA_ROOT_WRAPPER, "drbdadm", "cstate", DRBDInfo.getDRBDInfo().getResource()});
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("Unable to get connection status for resource: " + DRBDInfo.getDRBDInfo().getResource());
		}
		return returnValue;
	}

	private String getDataStatus() throws ExecutionException, EucalyptusCloudException {
		String returnValue = SystemUtil.run(new String[]{WalrusProperties.eucaHome + WalrusProperties.EUCA_ROOT_WRAPPER, "drbdadm", "dstate", DRBDInfo.getDRBDInfo().getResource()});
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("Unable to get data status for resource: " + DRBDInfo.getDRBDInfo().getResource());
		}
		return returnValue;
	}

	private String getRole() throws ExecutionException, EucalyptusCloudException {
		String returnValue = SystemUtil.run(new String[]{WalrusProperties.eucaHome + WalrusProperties.EUCA_ROOT_WRAPPER, "drbdadm", "role", DRBDInfo.getDRBDInfo().getResource()});
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("Unable to get role for resource: " + DRBDInfo.getDRBDInfo().getResource());
		}
		return returnValue;
	}

	private void makePrimary() throws ExecutionException, EucalyptusCloudException {
		//TODO: check if is already primary
		if(SystemUtil.runAndGetCode(new String[]{WalrusProperties.eucaHome + WalrusProperties.EUCA_ROOT_WRAPPER, "drbdadm", "primary", DRBDInfo.getDRBDInfo().getResource()}) != 0) {
			throw new EucalyptusCloudException("Unable to make resource " + DRBDInfo.getDRBDInfo().getResource() + " primary");
		}
	}

	private void makeSecondary() throws ExecutionException, EucalyptusCloudException {
		//TODO: check if is already secondary
		if(SystemUtil.runAndGetCode(new String[]{WalrusProperties.eucaHome + WalrusProperties.EUCA_ROOT_WRAPPER, "drbdadm", "secondary", DRBDInfo.getDRBDInfo().getResource()}) != 0) {
			throw new EucalyptusCloudException("Unable to make resource " + DRBDInfo.getDRBDInfo().getResource() + " secondary");
		}
	}

	private void connectResource() throws ExecutionException, EucalyptusCloudException {
		if(SystemUtil.runAndGetCode(new String[]{WalrusProperties.eucaHome + WalrusProperties.EUCA_ROOT_WRAPPER, "drbdadm", "connect", DRBDInfo.getDRBDInfo().getResource()}) != 0) {
			throw new EucalyptusCloudException("Unable to connect resource: " + DRBDInfo.getDRBDInfo().getResource());
		}
	}

	private void disconnectResource() throws ExecutionException, EucalyptusCloudException {
		if(SystemUtil.runAndGetCode(new String[]{WalrusProperties.eucaHome + WalrusProperties.EUCA_ROOT_WRAPPER, "drbdadm", "disconnect", DRBDInfo.getDRBDInfo().getResource()}) != 0) {
			throw new EucalyptusCloudException("Unable to disconnect resource: " + DRBDInfo.getDRBDInfo().getResource());
		}
	}

	//create config
	private void generateConfig() {

	}

	private void mountPrimary() throws ExecutionException, EucalyptusCloudException {
		if(SystemUtil.runAndGetCode(new String[]{WalrusProperties.eucaHome + WalrusProperties.EUCA_MOUNT_WRAPPER, "mount", DRBDInfo.getDRBDInfo().getBlockDevice(), WalrusInfo.getWalrusInfo().getStorageDir()}) != 0) {
			throw new EucalyptusCloudException("Unable to mount " + DRBDInfo.getDRBDInfo().getBlockDevice() + " as " + WalrusInfo.getWalrusInfo().getStorageDir());
		}
	}

	private void unmountPrimary() throws ExecutionException, EucalyptusCloudException {
		if(SystemUtil.runAndGetCode(new String[]{WalrusProperties.eucaHome + WalrusProperties.EUCA_MOUNT_WRAPPER, "umount", DRBDInfo.getDRBDInfo().getBlockDevice()}) != 0) {
			throw new EucalyptusCloudException("Unable to unmount " + DRBDInfo.getDRBDInfo().getBlockDevice());
		}
	}

	private boolean isMounted() throws ExecutionException {
		String returnValue = SystemUtil.run(new String[]{WalrusProperties.eucaHome + WalrusProperties.EUCA_ROOT_WRAPPER, "mount"});
		if(returnValue.length() > 0) {
			if(returnValue.contains(DRBDInfo.getDRBDInfo().getBlockDevice())) {
				return true;
			}
		}
		return false;
	}
	
	public void becomeMaster() {
		//check mount point, block device, role, cstate, dstate
		//make primary
		//mount
		//verify state
	}

	public void becomeSlave() {
		//check mount point, block device, role, cstate, dstate
		//make primary
		//mount
		//verify state
	}
	//check status

	//verify consistency
}
