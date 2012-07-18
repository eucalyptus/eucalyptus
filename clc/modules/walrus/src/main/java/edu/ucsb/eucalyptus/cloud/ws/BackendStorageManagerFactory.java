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

package edu.ucsb.eucalyptus.cloud.ws;

import org.apache.log4j.Logger;

import edu.ucsb.eucalyptus.storage.StorageManager;

public class BackendStorageManagerFactory {

	private static Logger LOG = Logger.getLogger( BackendStorageManagerFactory.class );

	public static StorageManager getStorageManager() throws Exception {
		String storageManager = "FileSystemStorageManager";
		if(System.getProperty("walrus.storage.manager") != null) {
			storageManager = System.getProperty("walrus.storage.manager");
		}
		try {
			storageManager = "edu.ucsb.eucalyptus.storage.fs." + storageManager;
			return (StorageManager) ClassLoader.getSystemClassLoader().loadClass(storageManager).newInstance();
		} catch (InstantiationException e) {
			LOG.error(e, e);
                        throw e;
		} catch (IllegalAccessException e) {
			LOG.error(e, e);
    			throw e; 
		} catch (ClassNotFoundException e) {
			LOG.error(e, e); 
			throw e;
		}
	}
}
