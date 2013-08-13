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
 * Author: Zach Hill zach@eucalyptus.com
 */

package com.eucalyptus.blockstorage;

import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.EucalyptusCloudException;

import edu.ucsb.eucalyptus.util.SystemUtil;

public class LVMWrapper {
	Logger LOG = Logger.getLogger(LVMWrapper.class);
	private static final String EUCA_ROOT_WRAPPER = BaseDirectory.LIBEXEC.toString() + "/euca_rootwrap";
	
	public static String getLvmVersion() throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "lvm", "version"});
	}

	public static String createPhysicalVolume(String loDevName) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "pvcreate", loDevName});
	}

	public static String getPhysicalVolume(String partition) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "pvdisplay", partition});
	}

	public static String getVolumeGroup(String volumeGroup) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "vgdisplay", volumeGroup});
	}

	public static String createVolumeGroup(String pvName, String vgName) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "vgcreate", vgName, pvName});
	}

	public static String getPhysicalVolumeVerbose(String pvName) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "pvdisplay", "-v", pvName});
	}

	/**
	 * 
	 * @param volumeId
	 * @param vgName
	 * @param lvName
	 * @param sizeMB - Volume size in MB
	 * @return
	 * @throws EucalyptusCloudException
	 */
	public static String createLogicalVolume(String volumeId, String vgName, String lvName, long sizeMB) throws EucalyptusCloudException {
		// return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "lvcreate", "-n", lvName, "-L", String.valueOf(size) + "G", vgName});
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "lvcreate", "--addtag", volumeId, "-n", lvName, "-L", String.valueOf(sizeMB) + "M", vgName});
	}

	/**
	 * 
	 * @param lvName
	 * @param snapLvName
	 * @param sizeMB - Volume size in MB
	 * @return
	 * @throws EucalyptusCloudException
	 */
	public static String createSnapshotLogicalVolume(String lvName, String snapLvName, long sizeMB) throws EucalyptusCloudException {
		// return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "lvcreate", "-n", snapLvName, "-s", "-L", String.valueOf(size) + "G", lvName});
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "lvcreate", "-n", snapLvName, "-s", "-L", String.valueOf(sizeMB) + "M", lvName});
	}

	public static String removeLogicalVolume(String lvName) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "lvremove", "-f", lvName});
	}

	public static String enableLogicalVolume(String lvName) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "lvchange", "-ay", lvName});
	}
	
	public static String extendVolumeGroup(String pvName, String vgName) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{StorageProperties.EUCA_ROOT_WRAPPER, "vgextend", vgName, pvName});
	}

	public static String scanVolumeGroups() throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{StorageProperties.EUCA_ROOT_WRAPPER, "vgscan"});
	}

	public static String createLogicalVolume(String volumeId, String vgName, String lvName) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{StorageProperties.EUCA_ROOT_WRAPPER, "lvcreate", "--addtag", volumeId, "-n", lvName, "-l", "100%FREE", vgName});
	}

	public static String createSnapshotLogicalVolume(String lvName, String snapLvName) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{StorageProperties.EUCA_ROOT_WRAPPER, "lvcreate", "-n", snapLvName, "-s", "-l", "100%FREE", lvName});
	}

	public static String removeVolumeGroup(String vgName) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{StorageProperties.EUCA_ROOT_WRAPPER, "vgremove", vgName});
	}

	public static String removePhysicalVolume(String loDevName) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{StorageProperties.EUCA_ROOT_WRAPPER, "pvremove", loDevName});
	}

	public static String reduceVolumeGroup(String vgName, String pvName) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{StorageProperties.EUCA_ROOT_WRAPPER, "vgreduce", vgName, pvName});
	}

	public static String disableLogicalVolume(String lvName) throws EucalyptusCloudException {
		return SystemUtil.run(new String[]{StorageProperties.EUCA_ROOT_WRAPPER, "lvchange", "-an", lvName});
	}

	public static boolean logicalVolumeExists(String lvName) {
		boolean success = false;
		String returnValue = SystemUtil.run(new String[]{StorageProperties.EUCA_ROOT_WRAPPER, "lvdisplay", lvName});
		if(returnValue.length() > 0) {
			success = true;
		}
		return success;
	}
	
	
}
