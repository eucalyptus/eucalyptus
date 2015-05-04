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

import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.entities.VolumeInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;

/**
 *
 */
public class BlockStorageStartupChecksTest {

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
    BlockStorageUnitTestSupport.flushSnapshotInfos();
    BlockStorageUnitTestSupport.flushVolumeInfos();
  }

  @AfterClass
  public static void teardownClass() {
    BlockStorageUnitTestSupport.tearDownBlockStoragePersistenceContext();
    BlockStorageUnitTestSupport.tearDownAuthPersistenceContext();
  }

  @Test
  public void cleanStuckSnapshotsBasicTest() throws Exception {
    SnapshotInfo good = new SnapshotInfo();
    good.setStatus(StorageProperties.Status.available.toString());
    good.setProgress("100");
    good.setSizeGb(new Integer(1));
    good.setShouldTransfer(Boolean.FALSE);
    good.setSnapPointId(null);
    good.setStartTime(new Date());
    good.setUserName("unittestuser0");
    good.setSnapshotLocation("http://osg.host/snaps/good");
    good.setVolumeId("vol-0000");
    good.setSnapshotId("snap-0000");

    SnapshotInfo failOne = new SnapshotInfo();
    failOne.setStatus(StorageProperties.Status.creating.toString());
    failOne.setProgress("0");
    failOne.setSizeGb(new Integer(1));
    failOne.setShouldTransfer(Boolean.FALSE);
    failOne.setSnapPointId(null);
    failOne.setStartTime(new Date());
    failOne.setUserName("unittestuser0");
    failOne.setSnapshotLocation("http://osg.host/snaps/failOne");
    failOne.setVolumeId("vol-0001");
    failOne.setSnapshotId("snap-0001");

    SnapshotInfo failTwo = new SnapshotInfo();
    failTwo.setStatus(StorageProperties.Status.failed.toString());
    failTwo.setProgress("0");
    failTwo.setSizeGb(new Integer(1));
    failTwo.setShouldTransfer(Boolean.FALSE);
    failTwo.setSnapPointId(null);
    failTwo.setStartTime(new Date());
    failTwo.setUserName("unittestuser0");
    failTwo.setSnapshotLocation("http://osg.host/snaps/failTwo");
    failTwo.setVolumeId("vol-0002");
    failTwo.setSnapshotId("snap-0002");

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      good = Entities.persist(good);
      failOne = Entities.persist(failOne);
      failTwo = Entities.persist(failTwo);
      tran.commit();
    }

    BlockStorageController.updateStuckSnapshots();

    List<SnapshotInfo> remaining;
    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      SnapshotInfo example = new SnapshotInfo();
      example.setStatus(StorageProperties.Status.failed.toString());
      remaining = Entities.query(example);
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    assertTrue("expected two SnapshotInfos to have " + StorageProperties.Status.failed.toString() + ", but " + remaining.size() + " exist",
        remaining.size() == 2);
  }

  @Test
  public void cleanStuckVolumesBasicTest() throws Exception {
    VolumeInfo good = new VolumeInfo();
    good.setStatus(StorageProperties.Status.available.toString());
    good.setSize(new Integer(1));
    good.setUserName("unittestuser0");
    good.setVolumeId("vol-0000");
    good.setSnapshotId("snap-0000");
    good.setCreateTime(new Date());
    good.setZone("eucalyptus");

    VolumeInfo failOne = new VolumeInfo();
    failOne.setStatus(StorageProperties.Status.creating.toString());
    failOne.setSize(new Integer(1));
    failOne.setUserName("unittestuser0");
    failOne.setVolumeId("vol-0001");
    failOne.setSnapshotId("snap-0001");
    failOne.setCreateTime(new Date());
    failOne.setZone("eucalyptus");

    VolumeInfo failTwo = new VolumeInfo();
    failTwo.setStatus(StorageProperties.Status.failed.toString());
    failTwo.setSize(new Integer(1));
    failTwo.setUserName("unittestuser0");
    failTwo.setVolumeId("vol-0002");
    failTwo.setSnapshotId("snap-0002");
    failTwo.setCreateTime(new Date());
    failTwo.setZone("eucalyptus");

    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      good = Entities.persist(good);
      failOne = Entities.persist(failOne);
      failTwo = Entities.persist(failTwo);
      tran.commit();
    }

    BlockStorageController.updateStuckVolumes();

    List<VolumeInfo> remaining;
    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      VolumeInfo example = new VolumeInfo();
      example.setStatus(StorageProperties.Status.failed.toString());
      remaining = Entities.query(example);
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    assertTrue("expected 2 VolumeInfos to still exist, but " + remaining.size() + " exist", remaining.size() == 2);
  }
}
