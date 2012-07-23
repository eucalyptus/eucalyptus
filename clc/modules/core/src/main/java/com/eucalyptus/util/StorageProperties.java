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
 ************************************************************************/

package com.eucalyptus.util;

import java.util.List;
import java.util.NoSuchElementException;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.component.id.Walrus;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.system.BaseDirectory;
import edu.ucsb.eucalyptus.cloud.entities.VolumeInfo;

public class StorageProperties {

	private static Logger LOG = Logger.getLogger( StorageProperties.class );

	public static final String SERVICE_NAME = "StorageController";
	public static final String DB_NAME             = "eucalyptus_storage";
	public static final String EUCALYPTUS_OPERATION = "EucaOperation";
	public static final String EUCALYPTUS_HEADER = "EucaHeader";
	public static final String storageRootDirectory = BaseDirectory.VAR.getChildPath( "volumes" );
	public static final long GB = 1024*1024*1024;
	public static final long MB = 1024*1024;
	public static final long KB = 1024;
	public static final String ETHERD_PREFIX = "/dev/etherd/e";
	public static final String iface = "eth0";
	public static final int MAX_TOTAL_VOLUME_SIZE = 100;
	public static final int MAX_VOLUME_SIZE = 15;
	public static int TRANSFER_CHUNK_SIZE = 8192;
	public static final boolean zeroFillVolumes = false;

	public static boolean enableSnapshots = false;
	public static boolean enableStorage = false;
	public static boolean shouldEnforceUsageLimits = true;
	public static String STORE_PREFIX = "iqn.2009-06.com.eucalyptus.";
	public static String WALRUS_URL = "http://localhost:8773/services/Walrus";
	public static String NAME = "unregistered";
	public static Integer ISCSI_LUN = 1;
	public static boolean trackUsageStatistics = true;
	public static String STORAGE_HOST = "127.0.0.1";

	public static String eucaHome = BaseDirectory.HOME.toString( );
	public static final String EUCA_ROOT_WRAPPER = "/usr/lib/eucalyptus/euca_rootwrap";
	public static final String blockSize = "1M";

	static { Groovyness.loadConfig("storageprops.groovy"); }

	public static void updateName() {
		try {
			StorageProperties.NAME = Components.lookup( Storage.class ).getLocalServiceConfiguration( ).getPartition( );
		} catch ( NoSuchElementException ex ) {
			LOG.error( ex , ex );
			LOG.error( "Failed to configure Storage Controller NAME." );
			throw ex;
		}
	}

	public static void updateStorageHost() {
		try {
			STORAGE_HOST = Components.lookup( Storage.class ).getLocalServiceConfiguration( ).getHostName( );
		} catch ( NoSuchElementException ex ) {
			LOG.error( ex , ex );
			LOG.error( "Failed to configure Storage Controller HOST (given the name " + StorageProperties.NAME + "." );
		}
	}

	public static void updateStorageHost(String hostName) {
		STORAGE_HOST = hostName;
	}

	public static void updateWalrusUrl() {
		try {
			ServiceConfiguration walrusConfig = Topology.lookup(Walrus.class);
			WALRUS_URL = ServiceUris.remote( walrusConfig ).toASCIIString( );
			StorageProperties.enableSnapshots = true;
			LOG.info("Setting WALRUS_URL to: " + WALRUS_URL);
		} catch (Exception e) {
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

	public static <T> EntityWrapper<T> getEntityWrapper( ) {
		return ( EntityWrapper<T> ) EntityWrapper.get( VolumeInfo.class );
	}

}
