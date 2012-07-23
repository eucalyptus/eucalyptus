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

package com.eucalyptus.bootstrap;


import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.storage.BlockStorageManagerFactory;
import com.eucalyptus.storage.LogicalStorageManager;
import com.eucalyptus.util.EucalyptusCloudException;

import edu.ucsb.eucalyptus.cloud.ws.BlockStorage;

@Provides(Storage.class)
@RunDuring(Bootstrap.Stage.DatabaseInit)
@DependsLocal(Storage.class)
public class BlockStorageBootstrapper extends Bootstrapper {
	private static Logger LOG = Logger.getLogger( BlockStorageBootstrapper.class );

	private static BlockStorageBootstrapper singleton;

	public static Bootstrapper getInstance( ) {
		synchronized ( BlockStorageBootstrapper.class ) {
			if ( singleton == null ) {
				singleton = new BlockStorageBootstrapper( );
				LOG.info( "Creating Block Storage Bootstrapper instance." );
			} else {
				LOG.info( "Returning Block Storage Bootstrapper instance." );
			}
		}
		return singleton;
	}

	@Override
	public boolean load() throws Exception {
		//privileged context (loads modules if necessary, etc).
		LogicalStorageManager blockStorageManager = BlockStorageManagerFactory.getBlockStorageManager();
		blockStorageManager.checkPreconditions();
		return true;
	}

	@Override
	public boolean start( ) throws Exception {
		BlockStorage.configure();
		return true;
	}

	/**
	 * @see com.eucalyptus.bootstrap.Bootstrapper#enable()
	 */
	@Override
	public boolean enable( ) throws Exception {
		BlockStorage.enable();
		return true;
	}

	/**
	 * @see com.eucalyptus.bootstrap.Bootstrapper#stop()
	 */
	@Override
	public boolean stop( ) throws Exception {
		BlockStorage.stop();
		return true;
	}

	/**
	 * @see com.eucalyptus.bootstrap.Bootstrapper#destroy()
	 */
	@Override
	public void destroy( ) throws Exception {
		BlockStorage.stop();
	}

	/**
	 * @see com.eucalyptus.bootstrap.Bootstrapper#disable()
	 */
	@Override
	public boolean disable( ) throws Exception {
		BlockStorage.disable();
		return true;
	}

	/**
	 * @see com.eucalyptus.bootstrap.Bootstrapper#check()
	 */
	@Override
	public boolean check( ) throws Exception {
		BlockStorage.check();
		return true;
	}
}
