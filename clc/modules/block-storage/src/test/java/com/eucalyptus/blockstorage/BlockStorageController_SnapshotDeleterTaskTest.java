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

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;

/**
 * Created by wesw on 6/20/14.
 */
public class BlockStorageController_SnapshotDeleterTaskTest {

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
  public void run_BasicTest() throws Exception {
    SnapshotInfo good = new SnapshotInfo();
    good.setStatus(StorageProperties.Status.available.toString());
    good.setProgress("100");
    good.setSizeGb(new Integer(1));
    good.setShouldTransfer(Boolean.FALSE);
    good.setSnapPointId(null);
    good.setStartTime(new Date());
    good.setUserName("unittestuser0");
    good.setVolumeId("vol-0000");
    good.setSnapshotId("snap-0000");
    good.setSnapshotLocation("http://osg.host/snaps/good");

    SnapshotInfo failOne = new SnapshotInfo();
    failOne.setStatus(StorageProperties.Status.deleting.toString());
    failOne.setProgress("0");
    failOne.setSizeGb(new Integer(1));
    failOne.setShouldTransfer(Boolean.FALSE);
    failOne.setSnapPointId(null);
    failOne.setStartTime(new Date());
    failOne.setUserName("unittestuser0");
    failOne.setVolumeId("vol-0001");
    failOne.setSnapshotId("snap-0001");
    failOne.setSnapshotLocation("http://osg.host/snaps/failOne");

    SnapshotInfo failTwo = new SnapshotInfo();
    failTwo.setStatus(StorageProperties.Status.deleting.toString());
    failTwo.setProgress("0");
    failTwo.setSizeGb(new Integer(1));
    failTwo.setShouldTransfer(Boolean.FALSE);
    failTwo.setSnapPointId(null);
    failTwo.setStartTime(new Date());
    failTwo.setUserName("unittestuser0");
    failTwo.setVolumeId("vol-0002");
    failTwo.setSnapshotId("snap-0002");
    failTwo.setSnapshotLocation("http://osg.host/snaps/failTwo");

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      Entities.persist(good);
      Entities.persist(failOne);
      Entities.persist(failTwo);
      tran.commit();
    }

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {
        exactly(2).of(storageManager).deleteSnapshot(with(any(String.class)));
      }
    });

    BlockStorageController bsc = new BlockStorageController(storageManager);
    BlockStorageController.SnapshotDeleterTask bscsdt =
        new BlockStorageController.SnapshotDeleterTask(BlockStorageUnitTestSupport.createMockS3SnapshotTransfer());
    bscsdt.run();

    List<SnapshotInfo> remaining;
    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      remaining = Entities.query(new SnapshotInfo());
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    assertTrue("expected all three SnapshotInfos to still exist", remaining.size() == 3);

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      SnapshotInfo example = new SnapshotInfo();
      example.setStatus(StorageProperties.Status.deleted.toString());
      remaining = Entities.query(example);
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context for deleted snapshotinfos", remaining != null);
    assertTrue("expected two SnapshotInfos to exist", remaining.size() == 2);

  }

}
