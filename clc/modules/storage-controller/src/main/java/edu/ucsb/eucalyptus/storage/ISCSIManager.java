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

package edu.ucsb.eucalyptus.storage;

import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.ExecutionException;
import com.eucalyptus.util.StorageProperties;

import edu.ucsb.eucalyptus.cloud.entities.ISCSIMetaInfo;
import edu.ucsb.eucalyptus.cloud.entities.ISCSIVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.LVMVolumeInfo;
import edu.ucsb.eucalyptus.ic.StorageController;

import edu.ucsb.eucalyptus.util.StreamConsumer;
import edu.ucsb.eucalyptus.util.SystemUtil;


public class ISCSIManager implements StorageExportManager {
	private static Logger LOG = Logger.getLogger(ISCSIManager.class);
	private static final String STORE_PREFIX = "iqn.2009-06." + StorageProperties.SC_LOCAL_NAME + ":store";

	@Override
	public void checkPreconditions() throws EucalyptusCloudException, ExecutionException {
		String returnValue;
		returnValue = SystemUtil.run(new String[]{LVM2Manager.eucaHome + LVM2Manager.EUCA_ROOT_WRAPPER, "ietadm", "--version"});
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("ietadm not found: Is iscsitarget installed?");
		} else {
			LOG.info(returnValue);
		}		
	}

	public void exportTarget(int tid, String name, int lun, String path, String user, String password) throws EucalyptusCloudException {
		try
		{
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(new String[]{LVM2Manager.eucaHome + LVM2Manager.EUCA_ROOT_WRAPPER, "ietadm", "--op", "new", "--tid=" + tid, "--params", "Name=" + name});
			StreamConsumer error = new StreamConsumer(proc.getErrorStream());
			StreamConsumer output = new StreamConsumer(proc.getInputStream());
			error.start();
			output.start();
			proc.waitFor();
			output.join();
			String errorValue = error.getReturnValue();
			if(errorValue.length() > 0)
				throw new EucalyptusCloudException(errorValue);

			proc = rt.exec(new String[]{LVM2Manager.eucaHome + LVM2Manager.EUCA_ROOT_WRAPPER, "ietadm", "--op", "new", "--tid=" + tid, "--user", "--params", "IncomingUser=" + user + ",Password=" + password});
			error = new StreamConsumer(proc.getErrorStream());
			output = new StreamConsumer(proc.getInputStream());
			error.start();
			output.start();
			proc.waitFor();
			output.join();
			errorValue = error.getReturnValue();
			if(errorValue.length() > 0)
				throw new EucalyptusCloudException(errorValue);

			proc = rt.exec(new String[]{LVM2Manager.eucaHome + LVM2Manager.EUCA_ROOT_WRAPPER, "ietadm", "--op", "new", "--tid=" + tid, "--lun=" + lun, "--params", "Path=" + path});
			error = new StreamConsumer(proc.getErrorStream());
			output = new StreamConsumer(proc.getInputStream());
			error.start();
			output.start();
			proc.waitFor();
			output.join();
			errorValue = error.getReturnValue();
			if(errorValue.length() > 0)
				throw new EucalyptusCloudException(errorValue);
		} catch (Throwable t) {
			LOG.error(t);
		}
	}

	public void unexportTarget(int tid, int lun) {
		try
		{
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(new String[]{LVM2Manager.eucaHome + LVM2Manager.EUCA_ROOT_WRAPPER, "ietadm", "--op", "delete", "--tid=" + tid, "--lun=" + lun});
			StreamConsumer error = new StreamConsumer(proc.getErrorStream());
			StreamConsumer output = new StreamConsumer(proc.getInputStream());
			error.start();
			output.start();
			proc.waitFor();

			proc = rt.exec(new String[]{LVM2Manager.eucaHome + LVM2Manager.EUCA_ROOT_WRAPPER, "ietadm", "--op", "delete", "--tid=" + tid});
			error = new StreamConsumer(proc.getErrorStream());
			output = new StreamConsumer(proc.getInputStream());
			error.start();
			output.start();
			proc.waitFor();
		} catch (Throwable t) {
			LOG.error(t);
		}
	}

	public native int exportVolume(String iface, String lvName, int major, int minor);

	public native void unexportVolume(int vbladePid);

	public native void loadModule();

	public ISCSIManager()  {}

	@Override
	public void configure() {
		EntityWrapper<ISCSIMetaInfo> db = StorageController.getEntityWrapper();
		ISCSIMetaInfo metaInfo = new ISCSIMetaInfo();
		try {
			List<ISCSIMetaInfo> metaInfoList = db.query(metaInfo);
			if(metaInfoList.size() <= 0) {
				metaInfo.setStore_prefix(STORE_PREFIX);
				metaInfo.setStoreNumber(0);
				metaInfo.setStoreUser("eucalyptus");
				metaInfo.setTid(1);
				db.add(metaInfo);
				db.commit();		
			}
		} catch(Exception e) {
			db.rollback();
			LOG.error(e);
		}
	}

	@Override
	public synchronized void allocateTarget(LVMVolumeInfo volumeInfo) {
		if(volumeInfo instanceof ISCSIVolumeInfo) {
			ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) volumeInfo;		
			ISCSIMetaInfo metaInfo = new ISCSIMetaInfo();
			EntityWrapper<ISCSIMetaInfo> db = StorageController.getEntityWrapper();
			List<ISCSIMetaInfo> metaInfoList = db.query(metaInfo);
			if(metaInfoList.size() > 0) {
				ISCSIMetaInfo foundMetaInfo = metaInfoList.get(0);
				int storeNumber = foundMetaInfo.getStoreNumber();
				int tid = foundMetaInfo.getTid();
				iscsiVolumeInfo.setStoreName(foundMetaInfo.getStore_prefix() + storeNumber);
				iscsiVolumeInfo.setStoreUser(foundMetaInfo.getStoreUser());
				iscsiVolumeInfo.setTid(tid);
				iscsiVolumeInfo.setLun(0);
				foundMetaInfo.setStoreNumber(++storeNumber);
				foundMetaInfo.setTid(++tid);
			}
			db.commit();
		}
	}
}