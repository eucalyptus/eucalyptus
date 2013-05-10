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
 * @author zhill
 *
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
