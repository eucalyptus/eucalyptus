/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.blockstorage;

import java.util.Date;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.eucalyptus.blockstorage.async.VolumesConvertor;
import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.entities.VolumeInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;

/**
 * Created by wesw on 6/20/14.
 */
public class BlockStorageController_VolumeConverterTest {

  @Rule
  public JUnitRuleMockery context = new JUnitRuleMockery();

  @BeforeClass
  public static void setupClass() {
    try {
      BlockStorageUnitTestSupport.setupBlockStoragePersistenceContext();
      BlockStorageUnitTestSupport.setupAuthPersistenceContext();
      BlockStorageUnitTestSupport.initializeAuth(1, 1);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @After
  public void teardown() {
    BlockStorageUnitTestSupport.flushBlockStorageEntities();
  }

  @AfterClass
  public static void teardownClass() {
    BlockStorageUnitTestSupport.tearDownBlockStoragePersistenceContext();
    BlockStorageUnitTestSupport.tearDownAuthPersistenceContext();
  }

  @Test
  public void run_BasicFromSnapshotTest() throws Exception {

    SnapshotInfo snapOne = new SnapshotInfo();
    snapOne.setStatus(StorageProperties.Status.available.toString());
    snapOne.setProgress("100");
    snapOne.setSizeGb(new Integer(1));
    snapOne.setShouldTransfer(Boolean.FALSE);
    snapOne.setSnapPointId(null);
    snapOne.setStartTime(new Date());
    snapOne.setUserName("unittestuser0");
    snapOne.setVolumeId("vol-0000");
    snapOne.setSnapshotId("snap-0000");
    snapOne.setSnapshotLocation("http://osg.host/snaps/snapOne");

    SnapshotInfo snapTwo = new SnapshotInfo();
    snapTwo.setStatus(StorageProperties.Status.available.toString());
    snapTwo.setProgress("100");
    snapTwo.setSizeGb(new Integer(1));
    snapTwo.setShouldTransfer(Boolean.FALSE);
    snapTwo.setSnapPointId(null);
    snapTwo.setStartTime(new Date());
    snapTwo.setUserName("unittestuser0");
    snapTwo.setVolumeId("vol-0001");
    snapTwo.setSnapshotId("snap-0001");
    snapTwo.setSnapshotLocation("http://osg.host/snaps/snapTwo");

    SnapshotInfo snapThree = new SnapshotInfo();
    snapThree.setStatus(StorageProperties.Status.available.toString());
    snapThree.setProgress("100");
    snapThree.setSizeGb(new Integer(1));
    snapThree.setShouldTransfer(Boolean.FALSE);
    snapThree.setSnapPointId(null);
    snapThree.setStartTime(new Date());
    snapThree.setUserName("unittestuser0");
    snapThree.setVolumeId("vol-0002");
    snapThree.setSnapshotId("snap-0002");
    snapThree.setSnapshotLocation("http://osg.host/snaps/snapThree");

    VolumeInfo volOne = new VolumeInfo();
    volOne.setStatus(StorageProperties.Status.available.toString());
    volOne.setSize(new Integer(1));
    volOne.setUserName("unittestuser0");
    volOne.setVolumeId("vol-0000");
    volOne.setSnapshotId("snap-0000");
    volOne.setCreateTime(new Date());
    volOne.setZone("eucalyptus");

    VolumeInfo volTwo = new VolumeInfo();
    volTwo.setStatus(StorageProperties.Status.available.toString());
    volTwo.setSize(new Integer(1));
    volTwo.setUserName("unittestuser0");
    volTwo.setVolumeId("vol-0001");
    volTwo.setSnapshotId("snap-0001");
    volTwo.setCreateTime(new Date());
    volTwo.setZone("eucalyptus");

    VolumeInfo volThree = new VolumeInfo();
    volThree.setStatus(StorageProperties.Status.available.toString());
    volThree.setSize(new Integer(1));
    volThree.setUserName("unittestuser0");
    volThree.setVolumeId("vol-0002");
    volThree.setSnapshotId("snap-0002");
    volThree.setCreateTime(new Date());
    volThree.setZone("eucalyptus");

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      Entities.persist(snapOne);
      Entities.persist(snapTwo);
      Entities.persist(snapThree);
      Entities.persist(volOne);
      Entities.persist(volTwo);
      Entities.persist(volThree);
      tran.commit();
    }

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {
        oneOf(storageManager).getVolumePath("vol-0000");
        will(returnValue(""));
        oneOf(storageManager).importVolume("vol-0000", "", 1);
        oneOf(storageManager).finishVolume("vol-0000");
        oneOf(storageManager).getVolumePath("vol-0001");
        will(returnValue(""));
        oneOf(storageManager).importVolume("vol-0001", "", 1);
        oneOf(storageManager).finishVolume("vol-0001");
        oneOf(storageManager).getVolumePath("vol-0002");
        will(returnValue(""));
        oneOf(storageManager).importVolume("vol-0002", "", 1);
        oneOf(storageManager).finishVolume("vol-0002");

        oneOf(storageManager).getSnapshotPath("snap-0000");
        will(returnValue(""));
        oneOf(storageManager).getSnapshotSize("snap-0000");
        will(returnValue(1));
        oneOf(storageManager).importSnapshot("snap-0000", "vol-0000", "", 1);
        oneOf(storageManager).finishVolume("snap-0000");
        oneOf(storageManager).getSnapshotPath("snap-0001");
        will(returnValue(""));
        oneOf(storageManager).getSnapshotSize("snap-0001");
        will(returnValue(1));
        oneOf(storageManager).importSnapshot("snap-0001", "vol-0001", "", 1);
        oneOf(storageManager).finishVolume("snap-0001");
        oneOf(storageManager).getSnapshotPath("snap-0002");
        will(returnValue(""));
        oneOf(storageManager).getSnapshotSize("snap-0002");
        will(returnValue(1));
        oneOf(storageManager).importSnapshot("snap-0002", "vol-0002", "", 1);
        oneOf(storageManager).finishVolume("snap-0002");
      }
    });

    VolumesConvertor bscvc = new VolumesConvertor(storageManager, storageManager);
    bscvc.run();
  }

}
