/*************************************************************************
 * Copyright 2013 Eucalyptus Systems, Inc.
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

package com.eucalyptus.storage.tests;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.eucalyptus.storage.ISCSIManager;
import com.eucalyptus.storage.StorageExportManager;

/**
 * Test unit for StorageExportManagers.
 * Should discover all implementing classes and test each one.
 * 
 * TODO: NOT FINISHED
 */
public class StorageExportManagerTests {
	private static StorageExportManager manager = null;
	
	@BeforeClass
	public static void setUp() {
		manager = new ISCSIManager();
	}
	
	@AfterClass
	public static void tearDown() {
		
	}
	
	@Test
	public void testConfigure() {
		try {
			manager.checkPreconditions();		
			manager.configure();
			manager.check();
		} catch(Exception e) {
			Assert.fail("Configuratio failed");
		}
	}
}
