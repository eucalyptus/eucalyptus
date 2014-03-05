/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

package com.eucalyptus.objectstorage;

import com.eucalyptus.objectstorage.metadata.BucketLifecycleManager;
import com.eucalyptus.objectstorage.metadata.DbBucketLifecycleManagerImpl;

/**
 * Manager factory for bucket lifecycle metadata handler. Returns an instance
 * for the configured manager.
 *
 */
public class BucketLifecycleManagers {
	private static BucketLifecycleManager manager = new DbBucketLifecycleManagerImpl();
    private static BucketLifecycleManager mocked;

	public static BucketLifecycleManager getInstance() {
		if(mocked != null) {
            return mocked;
        } else {
            return manager;
        }
	}

    /**
     * this allows a mock BucketLifecycleManager to be injected (for unit testing purposes)
     * visibility is package, so tests must live in the same package
     *
     * @param mock
     */
    static void setInstance(BucketLifecycleManager mock) {
        mocked = mock;
    }

}
