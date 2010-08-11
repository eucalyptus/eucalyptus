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

import java.io.IOException;
import java.security.PublicKey;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.auth.Authentication;
import com.eucalyptus.auth.X509Cert;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.BlockStorageUtil;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.ExecutionException;
import com.eucalyptus.util.StorageProperties;

import edu.ucsb.eucalyptus.cloud.entities.CHAPUserInfo;
import edu.ucsb.eucalyptus.cloud.entities.ISCSIMetaInfo;
import edu.ucsb.eucalyptus.cloud.entities.ISCSIVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.LVMVolumeInfo;

import edu.ucsb.eucalyptus.util.StreamConsumer;
import edu.ucsb.eucalyptus.util.SystemUtil;


public class ISCSIManager implements StorageExportManager {
	private static Logger LOG = Logger.getLogger(ISCSIManager.class);
	private static String ROOT_WRAP = BaseDirectory.HOME.toString() + StorageProperties.EUCA_ROOT_WRAPPER;
	@Override
	public void checkPreconditions() throws EucalyptusCloudException, ExecutionException {
		String returnValue;
		returnValue = SystemUtil.run(new String[]{ROOT_WRAP, "tgtadm", "--help"});
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("tgtadm not found: Is tgt installed?");
		}
		if(SystemUtil.runAndGetCode(new String[]{ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--mode", "target", "--op", "show"}) != 0) {
			LOG.warn("Unable to connect to tgt daemon. Is tgtd loaded?");
			LOG.info("Attempting to start tgtd ISCSI daemon");
			if(SystemUtil.runAndGetCode(new String[]{ROOT_WRAP, "tgtd"}) != 0) {
				throw new EucalyptusCloudException("Unable to start tgt daemon. Cannot proceed.");
			}
		}
	}

	public void addUser(String username, String password) throws ExecutionException {
		SystemUtil.run(new String[]{ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "new", "--mode", "account", "--user", username, "--password", password});
	}

	public void deleteUser(String username) throws ExecutionException {
		SystemUtil.run(new String[]{ROOT_WRAP , "tgtadm", "--lld", "iscsi", "--op", "delete", "--mode", "account", "--user", username});
	}

	public void exportTarget(int tid, String name, int lun, String path, String user) throws EucalyptusCloudException {
		try
		{
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(new String[]{ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "new", "--mode", "target", "--tid", String.valueOf(tid), "-T", name});
			StreamConsumer error = new StreamConsumer(proc.getErrorStream());
			StreamConsumer output = new StreamConsumer(proc.getInputStream());
			error.start();
			output.start();
			proc.waitFor();
			output.join();
			String errorValue = error.getReturnValue();
			if(errorValue.length() > 0)
				throw new EucalyptusCloudException(errorValue);

			proc = rt.exec(new String[]{ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "new", "--mode", "logicalunit", "--tid", String.valueOf(tid), "--lun", String.valueOf(lun), "-b", path});
			error = new StreamConsumer(proc.getErrorStream());
			output = new StreamConsumer(proc.getInputStream());
			error.start();
			output.start();
			proc.waitFor();
			output.join();
			errorValue = error.getReturnValue();
			if(errorValue.length() > 0)
				throw new EucalyptusCloudException(errorValue);

			proc = rt.exec(new String[]{ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "bind", "--mode", "account", "--tid", String.valueOf(tid), "--user", user});
			error = new StreamConsumer(proc.getErrorStream());
			output = new StreamConsumer(proc.getInputStream());
			error.start();
			output.start();
			proc.waitFor();
			output.join();
			errorValue = error.getReturnValue();
			if(errorValue.length() > 0)
				throw new EucalyptusCloudException(errorValue);

			proc = rt.exec(new String[]{ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "bind", "--mode", "target", "--tid" , String.valueOf(tid), "-I", "ALL"});
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
			if(SystemUtil.runAndGetCode(new String[]{ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "unbind", "--mode", "target", "--tid", String.valueOf(tid),  "-I", "ALL"}) != 0) {
				LOG.error("Unable to unbind tid: " + tid);
				return;
			}

			if(SystemUtil.runAndGetCode(new String[]{ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "delete", "--mode", "logicalunit", "--tid" , String.valueOf(tid), "--lun", String.valueOf(lun)}) != 0) {
				LOG.error("Unable to delete lun for tid: " + tid);
				return;
			}

			if(SystemUtil.runAndGetCode(new String[]{ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "delete", "--mode", "target", "--tid", String.valueOf(tid)}) != 0) {
				LOG.error("Unable to delete target: " + tid);
				return;
			}			
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
		EntityWrapper<ISCSIMetaInfo> db = StorageProperties.getEntityWrapper();
		ISCSIMetaInfo metaInfo = new ISCSIMetaInfo(StorageProperties.NAME);
		try {
			List<ISCSIMetaInfo> metaInfoList = db.query(metaInfo);
			if(metaInfoList.size() <= 0) {
				metaInfo.setStorePrefix(StorageProperties.STORE_PREFIX);
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
		EntityWrapper<CHAPUserInfo> dbUser = StorageProperties.getEntityWrapper();
		try {
			CHAPUserInfo userInfo = dbUser.getUnique(new CHAPUserInfo("eucalyptus"));
			//check if account actually exists, if not create it.			
			if(!checkUser("eucalyptus")) {
				try {
					addUser("eucalyptus", BlockStorageUtil.decryptSCTargetPassword(userInfo.getEncryptedPassword()));
				} catch (ExecutionException e1) {
					LOG.error(e1);					
					return;
				}
			}
		} catch(EucalyptusCloudException ex) {
			String password = Hashes.getRandom(20);
			try {
				addUser("eucalyptus", password);
			} catch (ExecutionException e1) {
				LOG.error(e1);
				dbUser.rollback();
				return;
			}
			CHAPUserInfo userInfo;
			try {
				userInfo = new CHAPUserInfo("eucalyptus", BlockStorageUtil.encryptSCTargetPassword(password));
				dbUser.add(userInfo);
			} catch (EucalyptusCloudException e) {
				LOG.error(e);
			}
		} finally {
			dbUser.commit();
		}
	}

	@Override
	public synchronized void allocateTarget(LVMVolumeInfo volumeInfo) {
		if(volumeInfo instanceof ISCSIVolumeInfo) {
			ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) volumeInfo;		
			EntityWrapper<ISCSIMetaInfo> db = StorageProperties.getEntityWrapper();
			List<ISCSIMetaInfo> metaInfoList = db.query(new ISCSIMetaInfo(StorageProperties.NAME));
			int tid = -1, storeNumber = -1;
			if(metaInfoList.size() > 0) {
				ISCSIMetaInfo foundMetaInfo = metaInfoList.get(0);
				storeNumber = foundMetaInfo.getStoreNumber();
				tid = foundMetaInfo.getTid();
			}			
			db.commit();
			//check if tid is in use
			int i = tid;
			do {
				try {
					String returnValue = SystemUtil.run(new String[]{ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "show", "--mode", "target", "--tid" , String.valueOf(i)});
					if(returnValue.length() == 0) {
						tid = i;
						break;
					}
				} catch (ExecutionException e) {
					LOG.error(e);
					iscsiVolumeInfo.setTid(-1);
					return;
				}
				i = (i + 1) % Integer.MAX_VALUE;
			} while(i != tid);
			if(tid > 0) {
				db = StorageProperties.getEntityWrapper();
				metaInfoList = db.query(new ISCSIMetaInfo(StorageProperties.NAME));
				if(metaInfoList.size() > 0) {
					ISCSIMetaInfo foundMetaInfo = metaInfoList.get(0);
					foundMetaInfo.setStoreNumber(++storeNumber);
					foundMetaInfo.setTid(tid + 1);
					iscsiVolumeInfo.setStoreName(foundMetaInfo.getStorePrefix() + StorageProperties.NAME + ":store" + storeNumber);
					iscsiVolumeInfo.setStoreUser(foundMetaInfo.getStoreUser());
					iscsiVolumeInfo.setTid(tid);
					//LUN cannot be 0 (some clients don't like that).
					iscsiVolumeInfo.setLun(1);
				}
				db.commit();
			} else {
				iscsiVolumeInfo.setTid(-1);
				LOG.fatal("Unable to allocate ISCSI target id.");
			}
		}
	}

	private boolean checkUser(String username) {
		Runtime rt = Runtime.getRuntime();
		Process proc;
		try {
			proc = rt.exec(new String[]{ROOT_WRAP, "tgtadm", "--op", "show", "--mode", "account"});
			StreamConsumer error = new StreamConsumer(proc.getErrorStream());
			StreamConsumer output = new StreamConsumer(proc.getInputStream());
			error.start();
			output.start();
			proc.waitFor();
			output.join();
			String returnValue = output.getReturnValue();
			if(returnValue.length() > 0) {
				Pattern p = Pattern.compile(username);
				Matcher m = p.matcher(returnValue);
				if(m.find())
					return true;
				else
					return false;
			}
		} catch (IOException e) {
			LOG.error(e);
			return false;
		} catch (InterruptedException e) {
			LOG.error(e);
			return false;
		}
		return false;		
	}

	public String getEncryptedPassword() throws EucalyptusCloudException {
		EntityWrapper<CHAPUserInfo> db = StorageProperties.getEntityWrapper();
		try {
			CHAPUserInfo userInfo = db.getUnique(new CHAPUserInfo("eucalyptus"));
			String encryptedPassword = userInfo.getEncryptedPassword();
			return BlockStorageUtil.encryptNodeTargetPassword(BlockStorageUtil.decryptSCTargetPassword(encryptedPassword));
		} catch(EucalyptusCloudException ex) {
			throw new EucalyptusCloudException("Unable to get CHAP password for: " + "eucalyptus");
		} finally {
			db.commit();
		}
	}
}
