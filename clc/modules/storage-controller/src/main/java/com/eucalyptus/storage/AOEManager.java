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
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package com.eucalyptus.storage;

import java.io.File;
import java.util.List;

import edu.ucsb.eucalyptus.cloud.entities.AOEMetaInfo;
import edu.ucsb.eucalyptus.cloud.entities.AOEVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.LVMVolumeInfo;
import edu.ucsb.eucalyptus.util.StreamConsumer;
import edu.ucsb.eucalyptus.util.SystemUtil;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.ExecutionException;
import com.eucalyptus.util.StorageProperties;

public class AOEManager implements StorageExportManager {
	private static Logger LOG = Logger.getLogger(AOEManager.class);
	public static final int MAX_MINOR_NUMBER = 16;
	public native int exportVolume(String iface, String lvName, int major, int minor);

	@Override
	public void checkPreconditions() throws EucalyptusCloudException, ExecutionException {
		String returnValue;
		returnValue = SystemUtil.run(new String[]{OverlayManager.eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "which", "vblade"});
		if(returnValue.length() == 0) {
			throw new EucalyptusCloudException("vblade not found: Is it installed?");
		} else {
			LOG.info(returnValue);
		}
	}

	public void unexportVolume(int vbladePid) {
		try
		{
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(new String[]{OverlayManager.eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "kill", String.valueOf(vbladePid)});
			StreamConsumer error = new StreamConsumer(proc.getErrorStream());
			StreamConsumer output = new StreamConsumer(proc.getInputStream());
			error.start();
			output.start();
			proc.waitFor();
			output.join();
		} catch (Throwable t) {
			LOG.error(t);
		}
	}

	public void loadModule() {
		try
		{
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(new String[]{OverlayManager.eucaHome + StorageProperties.EUCA_ROOT_WRAPPER, "modprobe", "aoe"});
			StreamConsumer error = new StreamConsumer(proc.getErrorStream());
			StreamConsumer output = new StreamConsumer(proc.getInputStream());
			error.start();
			output.start();
			proc.waitFor();
			output.join();
		} catch (Throwable t) {
			LOG.error(t);
		}
	}


	public AOEManager()  {
		loadModule();
	}

	@Override
	public void configure() {
		EntityWrapper<AOEMetaInfo> db = StorageProperties.getEntityWrapper();
		AOEMetaInfo metaInfo = new AOEMetaInfo(StorageProperties.NAME);
		try {
			List<AOEMetaInfo> metaInfoList = db.query(metaInfo);
			if(metaInfoList.size() <= 0) {
				metaInfo.setMajorNumber(0);
				metaInfo.setMinorNumber(0);
				db.add(metaInfo);
			}
		} catch(Exception e) {
			db.rollback();
			LOG.error(e);
		}
		db.commit();		
	}

	@Override
	public synchronized void allocateTarget(LVMVolumeInfo volumeInfo) {
		if(volumeInfo instanceof AOEVolumeInfo) {
			AOEVolumeInfo aoeVolumeInfo = (AOEVolumeInfo) volumeInfo;
			int majorNumber = -1;
			int minorNumber = -1;
			EntityWrapper<AOEMetaInfo> db = StorageProperties.getEntityWrapper();
			List<AOEMetaInfo> metaInfoList = db.query(new AOEMetaInfo(StorageProperties.NAME));
			if(metaInfoList.size() > 0) {
				AOEMetaInfo foundMetaInfo = metaInfoList.get(0);
				majorNumber = foundMetaInfo.getMajorNumber();
				minorNumber = foundMetaInfo.getMinorNumber();
				do {
					if(minorNumber >= MAX_MINOR_NUMBER - 1) {
						++majorNumber;
					}
					minorNumber = (minorNumber + 1) % MAX_MINOR_NUMBER;
					LOG.info("Trying e" + majorNumber + "." + minorNumber);
				} while(new File(StorageProperties.ETHERD_PREFIX + majorNumber + "." + minorNumber).exists());
				foundMetaInfo.setMajorNumber(majorNumber);
				foundMetaInfo.setMinorNumber(minorNumber);
			}
			aoeVolumeInfo.setMajorNumber(majorNumber);
			aoeVolumeInfo.setMinorNumber(minorNumber);
			db.commit();		
		}
	}
}
