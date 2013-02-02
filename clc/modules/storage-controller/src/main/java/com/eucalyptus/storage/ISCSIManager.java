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

package com.eucalyptus.storage;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.BlockStorageUtil;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.StorageProperties;
import com.google.common.base.Joiner;

import edu.ucsb.eucalyptus.cloud.entities.CHAPUserInfo;
import edu.ucsb.eucalyptus.cloud.entities.DirectStorageInfo;
import edu.ucsb.eucalyptus.cloud.entities.ISCSIMetaInfo;
import edu.ucsb.eucalyptus.cloud.entities.ISCSIVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.LVMVolumeInfo;
import edu.ucsb.eucalyptus.util.StreamConsumer;
import edu.ucsb.eucalyptus.util.SystemUtil.CommandOutput;


public class ISCSIManager implements StorageExportManager {
  public static String  TGT_SERVICE_NAME = "tgtd";
  private static Logger LOG = Logger.getLogger(ISCSIManager.class);
	private static String ROOT_WRAP = StorageProperties.EUCA_ROOT_WRAPPER;

	private static ExecutorService service = Executors.newFixedThreadPool(10);

	// TODO define fault IDs in a enum
	private static final int TGT_HOSED = 2000;
	private static final int TGT_CORRUPTED = 2002;

	@Override
	public void checkPreconditions() throws EucalyptusCloudException {		
		Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();

		CommandOutput output = execute(new String[] { ROOT_WRAP, "tgtadm", "--help" }, timeout);
		if (output.returnValue != 0 || StringUtils.isNotBlank(output.error)) {
			Faults.forComponent(Storage.class).havingId(TGT_CORRUPTED).withVar("component", "Storage Controller").withVar("operation", "tgtadm --help").withVar("error", output.error).log();
			throw new EucalyptusCloudException("tgtadm not found: Is tgt installed?");
		}
		
		output = execute (new String[]{ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--mode", "target", "--op", "show"}, timeout);
		if (output.returnValue != 0 || StringUtils.isNotBlank(output.error)) {
			LOG.warn("Unable to connect to tgt daemon. Is tgtd loaded?");
			LOG.info("Attempting to start tgtd ISCSI daemon");
			output = execute(new String[] { ROOT_WRAP, "service", TGT_SERVICE_NAME, "status" }, timeout);
			if (output.returnValue != 0 || StringUtils.isNotBlank(output.error)) {
				output = execute(new String[] { ROOT_WRAP, "service", TGT_SERVICE_NAME, "start" }, timeout);
				if (output.returnValue != 0 || StringUtils.isNotBlank(output.error)) {
					Faults.forComponent(Storage.class).havingId(TGT_CORRUPTED).withVar("component", "Storage Controller").withVar("operation", "service tgt start").withVar("error", output.error).log();
					throw new EucalyptusCloudException("Unable to start tgt daemon. Cannot proceed.");
				}
			} else {
				output = execute(new String[] { ROOT_WRAP, "service", "tgt", "start" } ,timeout);
				if (output.returnValue != 0 || StringUtils.isNotBlank(output.error)) {
					Faults.forComponent(Storage.class).havingId(TGT_CORRUPTED).withVar("component", "Storage Controller").withVar("operation", "service tgt start").withVar("error", output.error).log();
					throw new EucalyptusCloudException("Unable to start tgt daemon. Cannot proceed.");
				}
			}
		}
	}

	@Override
	public void check() throws EucalyptusCloudException {
		Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();

		CommandOutput output = execute(new String[] { ROOT_WRAP, "service", TGT_SERVICE_NAME, "status" }, timeout);
		if (StringUtils.isNotBlank(output.error)) {
			Faults.forComponent(Storage.class).havingId(TGT_CORRUPTED).withVar("component", "Storage Controller").withVar("operation", "service tgt status").withVar("error", output.error).log();
			throw new EucalyptusCloudException("tgt service check failed with error: " + output.error);
		}
	}

	public void addUser(String username, String password) throws EucalyptusCloudException {
		Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
		execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "new", "--mode", "account", "--user", username, "--password", password }, timeout);
	}

	public void deleteUser(String username) throws EucalyptusCloudException {
		Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
		execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "delete", "--mode", "account", "--user", username }, timeout);
	}

	/**
	 * Separate thread to wait for {@link java.lang.Process Process} to complete and return its exit value
	 * 
	 */
	public static class ProcessMonitor implements Callable<Integer> {
		private Process process;

		ProcessMonitor(Process process) {
			this.process = process;
		}

		public Integer call() throws Exception {
			process.waitFor();
			return process.exitValue();
		}
	}

	private static final Joiner JOINER = Joiner.on(" ").skipNulls();
	// Implementation of EUCA-3597
	/**
	 * Executes the specified command in a separate process. A {@link DirectStorageInfo#timeoutInMillis timeout} is enforced on the process using
	 * {@link java.util.concurrent.ExecutorService ExecutorService} framework. If the process does not complete with in the timeout, it is cancelled.
	 * 
	 * @param command
	 * @param timeout
	 * @return CommandOutput
	 * @throws EucalyptusCloudException
	 */
	private CommandOutput execute(String[] command, Long timeout) throws EucalyptusCloudException {
		try {
			Integer returnValue = -999999;
			Runtime runtime = Runtime.getRuntime();
			Process process = runtime.exec(command);
			StreamConsumer error = new StreamConsumer(process.getErrorStream());
			StreamConsumer output = new StreamConsumer(process.getInputStream());
			error.start();
			output.start();
			Callable<Integer> processMonitor = new ProcessMonitor(process);
			Future<Integer> processController = service.submit(processMonitor);
			try {
				returnValue = processController.get(timeout, TimeUnit.MILLISECONDS);
			} catch (TimeoutException tex) {
				String commandStr = buildCommand(command);
				LOG.error(commandStr + " timed out. Cancelling the process, logging a fault and exceptioning out");
				processController.cancel(true);
				Faults.forComponent(Storage.class).havingId(TGT_HOSED).withVar("component", "Storage Controller").withVar("timeout", Long.toString(timeout)).log();
				throw new EucalyptusCloudException("No response from the command " + commandStr + ". Process timed out after waiting for " + timeout + " milliseconds");
			}
			output.join();
			error.join();
			LOG.debug("ISCSIManager executed: " + JOINER.join(command) + "\n return=" + returnValue + "\n stdout=" + output.getReturnValue() + "\n stderr=" + error.getReturnValue());
			return new CommandOutput(returnValue, output.getReturnValue(), error.getReturnValue());
		} catch (Exception ex) {
			if (ex instanceof EucalyptusCloudException) {
				throw (EucalyptusCloudException) ex;
			} else {
				throw new EucalyptusCloudException(ex);
			}
		}
	}

	private String buildCommand(String[] command) {
		StringBuilder builder = new StringBuilder();
		for (String part : command) {
			builder.append(part).append(' ');
		}
		return builder.toString();
	}

	// Modified logic for implementing EUCA-3597
	public void exportTarget(int tid, String name, int lun, String path, String user) throws EucalyptusCloudException {
	  LOG.debug("Exporting target: " + tid + "," + name + "," + lun + "," + path + "," + user);
		checkAndAddUser();

		Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
		CommandOutput output;		
		// Check to see if the target is already exported. If it is dont do anything further
		output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "show", "--mode", "target", "--tid", String.valueOf(tid) }, timeout);
		// Return value 0 in this case indicates that the target exists
		if (StringUtils.isBlank(output.error)) {
			LOG.info("Target: " + tid + " already exported");
			return;
		}
		
		output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "new", "--mode", "target", "--tid", String.valueOf(tid), "-T", name }, timeout);
		if (StringUtils.isNotBlank(output.error)) {
			throw new EucalyptusCloudException(output.error);
		}

		output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "new", "--mode", "logicalunit", "--tid", String.valueOf(tid), "--lun", String.valueOf(lun), "-b", path }, timeout);
		if (StringUtils.isNotBlank(output.error)) {
			throw new EucalyptusCloudException(output.error);
		}

		output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "bind", "--mode", "account", "--tid", String.valueOf(tid), "--user", user }, timeout);
		if (StringUtils.isNotBlank(output.error)) {
			throw new EucalyptusCloudException(output.error);
		}

		output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "bind", "--mode", "target", "--tid", String.valueOf(tid), "-I", "ALL" }, timeout);
		if (StringUtils.isNotBlank(output.error)) {
			throw new EucalyptusCloudException(output.error);
		}
	}

	public void unexportTarget(int tid, int lun) {
		try
		{
			Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
			LOG.debug("Unexport target: tid=" + tid + ",lun=" + lun);
			CommandOutput output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "show", "--mode", "target", "--tid", String.valueOf(tid) }, timeout);
			if (StringUtils.isBlank(output.error)) {
				LOG.info("Attempting to unexport target: " + tid);
			} else {
				LOG.info("Target: " + tid + " not found");
				return;
			}
			
			output = execute (new String[]{ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "unbind", "--mode", "target", "--tid", String.valueOf(tid),  "-I", "ALL"} ,timeout);
			if (StringUtils.isNotBlank(output.error)) {
				LOG.error("Unable to unbind tid: " + tid);
			}
			int retryCount = 0;
			do {
				output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "delete", "--mode", "logicalunit", "--tid", String.valueOf(tid), "--lun", String.valueOf(lun) }, timeout);
				if(StringUtils.isNotBlank(output.error)) {
					LOG.warn("Unable to delete lun for: " + tid);
					Thread.sleep(1000); //FIXME: clean this up async 
					continue;
				} else {
					break;
				}	
			} while (retryCount++ < 30);
			if (retryCount>=30){
				LOG.error("Gave up deleting the lun for: " + tid);
			}

			retryCount = 0;
			do {
				output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "delete", "--mode", "target", "--tid", String.valueOf(tid) }, timeout);
				if (StringUtils.isNotBlank(output.error)) {
					LOG.warn("Unable to delete target: " + tid);
					Thread.sleep(1000); //FIXME: clean this up async 
					continue;
				}
				
				output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "show", "--mode", "target", "--tid", String.valueOf(tid) }, timeout);
				if (StringUtils.isBlank(output.error)) {
					LOG.warn("Target: " + tid + " still exists...");
					Thread.sleep(1000); //FIXME: clean this up async
				} else {
					break;
				}
			} while (retryCount++ < 30);
			if (retryCount>=30){
				LOG.error("Gave up deleting the target: " + tid);
			}
		} catch (Exception t) {
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
		checkAndAddUser();
	}

	private void checkAndAddUser() {
		EntityWrapper<CHAPUserInfo> dbUser = StorageProperties.getEntityWrapper();
		try {
			CHAPUserInfo userInfo = dbUser.getUnique(new CHAPUserInfo("eucalyptus"));
			//check if account actually exists, if not create it.			
			if(!checkUser("eucalyptus")) {
				try {
					addUser("eucalyptus", BlockStorageUtil.decryptSCTargetPassword(userInfo.getEncryptedPassword()));
				} catch (EucalyptusCloudException e1) {
					LOG.error(e1);					
					return;
				}
			}
		} catch(EucalyptusCloudException ex) {
			boolean addUser = true;
			String encryptedPassword = null; 
			
			if (checkUser("eucalyptus"))
			{
				try {
					LOG.debug("No DB record found for chapuser although a eucalyptus account exists on SC. Looking for all records with chapuser eucalyptus");
					CHAPUserInfo uesrInfo = new CHAPUserInfo("eucalyptus");
					uesrInfo.setScName(null);
					CHAPUserInfo currentUserInfo = dbUser.getUnique(uesrInfo);
					if (null != currentUserInfo && null != currentUserInfo.getEncryptedPassword()) {
						LOG.debug("Found a DB record, copying the password to the new record");
						addUser = false;
						encryptedPassword = currentUserInfo.getEncryptedPassword();
					}
				} catch (Exception e1) {
					LOG.debug("No old DB records found. The only way is to delete the chapuser and create a fresh account");
					try {
						deleteUser("eucalyptus");
					} catch (Exception e) {
						LOG.error("Failed to delete chapuser", e);
					}
				}
			} 
			
			if (addUser) {
				// Windows iscsi initiator requires the password length to be 12-16 bytes
				String password = Hashes.getRandom(16);
				password = password.substring(0,16);
				try {
					addUser("eucalyptus", password);
					encryptedPassword = BlockStorageUtil.encryptSCTargetPassword(password);
				} catch (Exception e) {
					LOG.error("Failed to add chapuser to SC", e);
					return;
				}
			}
			
			try{
				dbUser.add(new CHAPUserInfo("eucalyptus", encryptedPassword));
			} catch (Exception e) {
				dbUser.rollback();
				LOG.error(e);
			}
		} finally {
			dbUser.commit();
		}
	}

	@Override
	public synchronized void allocateTarget(LVMVolumeInfo volumeInfo) throws EucalyptusCloudException {
		if(volumeInfo instanceof ISCSIVolumeInfo) {
			ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) volumeInfo;
			LOG.debug("Allocate target: " + iscsiVolumeInfo);
			if(iscsiVolumeInfo.getTid() > -1) {
				LOG.info("Volume already associated with a tid: " + iscsiVolumeInfo.getTid());
				return;
			}
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

			Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
			do {
				CommandOutput output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "show", "--mode", "target", "--tid", String.valueOf(i) }, timeout);
				if(StringUtils.isNotBlank(output.error)) {
					tid = i;
					break;
				}
				i = (i + 1) % Integer.MAX_VALUE;
			} while(i != tid);
			LOG.debug("Target allocation found tid: " + tid);
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
		try {
			Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();

			CommandOutput output = execute(new String[] { ROOT_WRAP, "tgtadm", "--op", "show", "--mode", "account" }, timeout);
			String returnValue = output.output;

			if (returnValue.length() > 0) {
				Pattern p = Pattern.compile(username);
				Matcher m = p.matcher(returnValue);
				if (m.find())
					return true;
				else
					return false;
			}
		} catch (EucalyptusCloudException e) {
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

	public void cleanup(LVMVolumeInfo volumeInfo) throws EucalyptusCloudException {
		if(volumeInfo instanceof ISCSIVolumeInfo) {
			if(((ISCSIVolumeInfo) volumeInfo).getTid() > -1) {
				ISCSIVolumeInfo iscsiVolumeInfo = (ISCSIVolumeInfo) volumeInfo;
				unexportTarget(iscsiVolumeInfo.getTid(), iscsiVolumeInfo.getLun());
				Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
				CommandOutput output = execute(new String[] { ROOT_WRAP, "tgtadm", "--lld", "iscsi", "--op", "show", "--mode", "target", "--tid", String.valueOf(iscsiVolumeInfo.getTid()) }, timeout);
				if (StringUtils.isNotBlank(output.error)) {
					iscsiVolumeInfo.setTid(-1);
				} else {
					throw new EucalyptusCloudException("Unable to remove tid: " + iscsiVolumeInfo.getTid());
				}
			}
		}
	}

	@Override
	public void stop() {
		try {
			service.shutdownNow();
		} catch (Exception e) {
			LOG.warn("Unable to shutdown thread pool", e);
		} finally {
			service = null;
		}
	}
}
