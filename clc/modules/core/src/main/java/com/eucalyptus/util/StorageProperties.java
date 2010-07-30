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

package com.eucalyptus.util;

import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.config.StorageControllerConfiguration;
import com.eucalyptus.config.WalrusConfiguration;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.scripting.groovy.GroovyUtil;
import com.eucalyptus.system.BaseDirectory;

import edu.ucsb.eucalyptus.util.SystemUtil;

public class StorageProperties {

	private static Logger LOG = Logger.getLogger( StorageProperties.class );

	public static final String SERVICE_NAME = "StorageController";
	public static final String SC_LOCAL_NAME = "StorageController-local";
	public static final String DB_NAME             = "eucalyptus_storage";
	public static final String EUCALYPTUS_OPERATION = "EucaOperation";
	public static final String EUCALYPTUS_HEADER = "EucaHeader";
	public static final String storageRootDirectory = BaseDirectory.VAR.toString() + "/volumes";
	public static final long GB = 1024*1024*1024;
	public static final long MB = 1024*1024;
	public static final long KB = 1024;
	public static final String ETHERD_PREFIX = "/dev/etherd/e";
	public static final String iface = "eth0";
	public static final int MAX_TOTAL_VOLUME_SIZE = 50;
	public static final int MAX_VOLUME_SIZE = 10;
	public static final boolean zeroFillVolumes = false;

	public static int TRANSFER_CHUNK_SIZE = 8192;
	public static boolean enableSnapshots = false;
	public static boolean enableStorage = false;
	public static boolean shouldEnforceUsageLimits = true;
	public static String STORE_PREFIX = "iqn.2009-06.com.eucalyptus.";
	public static String WALRUS_URL = "http://localhost:"+System.getProperty("euca.ws.port")+"/services/Walrus";
	public static String NAME = "unregistered";
	public static Integer ISCSI_LUN = 1;
	public static boolean trackUsageStatistics = true;
	public static String STORAGE_HOST = "127.0.0.1";

	static { GroovyUtil.loadConfig("storageprops.groovy"); }

	public static void updateName() {
		if(!Component.eucalyptus.isLocal()) {
			String scName = System.getProperty("euca.storage.name");
			if(scName != null) {
				StorageProperties.NAME = scName;
			} else {
				SystemUtil.shutdownWithError("Storage controller name cannot be determined. Shutting down.");
			}
		} else {
			try {
				List<StorageControllerConfiguration> configs = Configuration.getStorageControllerConfigurations();
				for(StorageControllerConfiguration config : configs) {
					if(NetworkUtil.testLocal(config.getHostName())) {
						StorageProperties.NAME = config.getName();
						return;
					}
				}
			} catch (EucalyptusCloudException e) {
				LOG.error(e);
			}
		}
	}

	public static void updateStorageHost() {
		try {
			if(!"unregistered".equals(StorageProperties.NAME)) {
				StorageControllerConfiguration config = Configuration.getStorageControllerConfiguration(StorageProperties.NAME);
				STORAGE_HOST = config.getHostName();
			} else {
				LOG.info("Storage Controller not registered yet.");
			}
		} catch (EucalyptusCloudException e) {
			LOG.error(e);
		}
	}

	public static void updateStorageHost(String hostName) {
		STORAGE_HOST = hostName;
	}

	public static void updateWalrusUrl() {
		List<WalrusConfiguration> walrusConfigs;
		try {
			walrusConfigs = Configuration.getWalrusConfigurations();
			if(walrusConfigs.size() > 0) {
				WalrusConfiguration walrusConfig = walrusConfigs.get(0);
				WALRUS_URL = walrusConfig.getUri();
				StorageProperties.enableSnapshots = true;
				LOG.info("Setting WALRUS_URL to: " + WALRUS_URL);
			} else {
				LOG.warn("Could not obtain walrus information. Snapshot functionality may be unavailable. Have you registered Walrus?");
				StorageProperties.enableSnapshots = false;
			}
		} catch (EucalyptusCloudException e) {
			LOG.warn("Could not obtain walrus information. Snapshot functionality may be unavailable. Have you registered Walrus?");
			StorageProperties.enableSnapshots = false;
		}		
	}

	public enum Status {
		creating, available, pending, completed, failed
	}

	public enum StorageParameters {
		EucaSignature, EucaSnapSize, EucaCert, EucaEffectiveUserId
	}

	public static final String EUCA_ROOT_WRAPPER = "/usr/lib/eucalyptus/euca_rootwrap";

	public static final String blockSize = "1M";

	public static <T> EntityWrapper<T> getEntityWrapper( ) {
		return new EntityWrapper<T>( StorageProperties.DB_NAME );
	}
}
