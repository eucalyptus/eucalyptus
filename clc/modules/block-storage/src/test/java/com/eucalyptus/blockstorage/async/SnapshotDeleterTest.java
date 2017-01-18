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

package com.eucalyptus.blockstorage.async;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.eucalyptus.blockstorage.BlockStorageUnitTestSupport;
import com.eucalyptus.blockstorage.LogicalStorageManager;
import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;

/**
 * Created by wesw on 6/20/14.
 */
public class SnapshotDeleterTest {

  private static Logger LOG = Logger.getLogger(SnapshotDeleterTest.class);

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
  public void cleanDeletingSnapshots() throws Exception {
    Calendar twoHoursAgo = Calendar.getInstance();
    twoHoursAgo.add(Calendar.HOUR, -2);

    SnapshotInfo deleted = new SnapshotInfo();
    deleted.setStatus(StorageProperties.Status.deleted.toString());
    deleted.setProgress("100");
    deleted.setSizeGb(new Integer(1));
    deleted.setStartTime(new Date());
    deleted.setVolumeId("vol-0000");
    deleted.setSnapshotId("snap-0000");
    deleted.setSnapshotLocation("http://osg.host/snaps/good");
    deleted.setDeletionTime(twoHoursAgo.getTime());
    deleted.setSnapPointId("snap-point-foo");
    deleted.setIsOrigin(Boolean.TRUE);

    SnapshotInfo good = new SnapshotInfo();
    good.setStatus(StorageProperties.Status.available.toString());
    good.setProgress("100");
    good.setSizeGb(new Integer(1));
    good.setStartTime(new Date());
    good.setVolumeId("vol-0000");
    good.setSnapshotId("snap-0001");
    good.setSnapshotLocation("http://osg.host/snaps/good");
    good.setSnapPointId("snap-point-foo");
    good.setIsOrigin(Boolean.TRUE);

    SnapshotInfo failOne = new SnapshotInfo();
    failOne.setStatus(StorageProperties.Status.deleting.toString());
    failOne.setProgress("0");
    failOne.setSizeGb(new Integer(1));
    failOne.setStartTime(new Date());
    failOne.setVolumeId("vol-0001");
    failOne.setSnapshotId("snap-0002");
    failOne.setSnapshotLocation("http://osg.host/snaps/failOne");
    failOne.setSnapPointId("snap-point-foo");
    failOne.setIsOrigin(Boolean.TRUE);

    SnapshotInfo failTwo = new SnapshotInfo();
    failTwo.setStatus(StorageProperties.Status.deleting.toString());
    failTwo.setProgress("0");
    failTwo.setSizeGb(new Integer(1));
    failTwo.setStartTime(new Date());
    failTwo.setVolumeId("vol-0002");
    failTwo.setSnapshotId("snap-0003");
    failTwo.setSnapshotLocation("http://osg.host/snaps/failTwo");
    failTwo.setSnapPointId("snap-point-foo");
    failTwo.setIsOrigin(Boolean.TRUE);

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      Entities.persist(deleted);
      Entities.persist(good);
      Entities.persist(failOne);
      Entities.persist(failTwo);
      tran.commit();
    }

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {
        exactly(2).of(storageManager).deleteSnapshot(with(any(String.class)), with(any(String.class)));
      }
    });

    // Test SnapshotDeleter
    SnapshotDeleter sct = new SnapshotDeleter(storageManager, BlockStorageUnitTestSupport.createMockS3SnapshotTransfer());
    sct.run();

    List<SnapshotInfo> remaining;
    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      remaining = Entities.query(new SnapshotInfo());
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    assertTrue("expected all 4 SnapshotInfos to still exist but found ", remaining.size() == 4);

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      SnapshotInfo example = new SnapshotInfo();
      example.setStatus(StorageProperties.Status.deleted.toString());
      remaining = Entities.query(example);
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context for deleted snapshotinfos", remaining != null);
    assertTrue("expected two SnapshotInfos with deleted status to exist but found " + remaining.size(), remaining.size() == 3);

    // Test ExpiredSnapshotCleaner
    ExpiredSnapshotCleaner esc = new ExpiredSnapshotCleaner();
    esc.run();

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      remaining = Entities.query(new SnapshotInfo());
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    assertTrue("expected all three SnapshotInfos to still exist but found ", remaining.size() == 3);

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      SnapshotInfo example = new SnapshotInfo();
      example.setStatus(StorageProperties.Status.deleted.toString());
      remaining = Entities.query(example);
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context for deleted snapshotinfos", remaining != null);
    assertTrue("expected two SnapshotInfos with deleted status to exist but found " + remaining.size(), remaining.size() == 2);
    assertTrue("expected a valid deletionTime and snapshot ID other than snap-0000",
        remaining.get(0).getDeletionTime() != null && !remaining.get(0).getSnapshotId().equals("snap-0000"));
    assertTrue("expected a valid deletionTime and snapshot ID other than snap-0000",
        remaining.get(1).getDeletionTime() != null && !remaining.get(0).getSnapshotId().equals("snap-0000"));

  }

  @Test
  public void cleanFailedSnapshotsTest() throws Exception {
    Calendar twoHoursAgo = Calendar.getInstance();
    twoHoursAgo.add(Calendar.HOUR, -2);

    SnapshotInfo good = new SnapshotInfo();
    good.setStatus(StorageProperties.Status.available.toString());
    good.setProgress("100");
    good.setSizeGb(new Integer(1));
    good.setStartTime(new Date());
    good.setSnapshotLocation("http://osg.host/snaps/good");
    good.setVolumeId("vol-0000");
    good.setSnapshotId("snap-0000");
    good.setSnapPointId("snap-point-foo");
    good.setIsOrigin(Boolean.TRUE);

    SnapshotInfo failOne = new SnapshotInfo();
    failOne.setStatus(StorageProperties.Status.failed.toString());
    failOne.setProgress("0");
    failOne.setSizeGb(new Integer(1));
    failOne.setStartTime(new Date());
    failOne.setSnapshotLocation("http://osg.host/snaps/failOne");
    failOne.setVolumeId("vol-0001");
    failOne.setSnapshotId("snap-0001");
    failOne.setSnapPointId("snap-point-foo");
    failOne.setIsOrigin(Boolean.TRUE);

    SnapshotInfo failTwo = new SnapshotInfo();
    failTwo.setStatus(StorageProperties.Status.failed.toString());
    failTwo.setProgress("0");
    failTwo.setSizeGb(new Integer(1));
    failTwo.setStartTime(new Date());
    failTwo.setSnapshotLocation("http://osg.host/snaps/failTwo");
    failTwo.setVolumeId("vol-0002");
    failTwo.setSnapshotId("snap-0002");
    failTwo.setDeletionTime(twoHoursAgo.getTime());
    failTwo.setSnapPointId("snap-point-foo");
    failTwo.setIsOrigin(Boolean.TRUE);

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      good = Entities.persist(good);
      failOne = Entities.persist(failOne);
      failTwo = Entities.persist(failTwo);
      tran.commit();
    }

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {
        oneOf(storageManager).finishVolume(with(any(String.class)));
      }
      {
        oneOf(storageManager).cleanSnapshot(with(any(String.class)), with(any(String.class)));
      }
    });

    // Test FailedSnapshotCleaner
    FailedSnapshotCleaner sct = new FailedSnapshotCleaner(storageManager, BlockStorageUnitTestSupport.createMockS3SnapshotTransfer());
    sct.run();

    List<SnapshotInfo> remaining;
    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      remaining = Entities.query(new SnapshotInfo());
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    assertTrue("expected 3 SnapshotInfos to still exist but found " + remaining.size(), remaining.size() == 3);

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      SnapshotInfo example = new SnapshotInfo();
      example.setStatus(StorageProperties.Status.failed.toString());
      remaining = Entities.query(example);
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context for deleted snapshotinfos", remaining != null);
    assertTrue("expected 2 SnapshotInfo with failed status to exist but found " + remaining.size(), remaining.size() == 2);
    assertTrue("expected a valid deletionTime but got null", remaining.get(0).getDeletionTime() != null);
    assertTrue("expected a valid deletionTime but got null", remaining.get(1).getDeletionTime() != null);

    // Test ExpiredSnapshotCleaner
    ExpiredSnapshotCleaner esc = new ExpiredSnapshotCleaner();
    esc.run();

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      remaining = Entities.query(new SnapshotInfo());
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    assertTrue("expected two SnapshotInfos to still exist but found " + remaining.size(), remaining.size() == 2);

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      SnapshotInfo example = new SnapshotInfo();
      example.setStatus(StorageProperties.Status.failed.toString());
      remaining = Entities.query(example);
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context for deleted snapshotinfos", remaining != null);
    assertTrue("expected one SnapshotInfo with failed status to exist but found " + remaining.size(), remaining.size() == 1);
    assertTrue("expected a valid deletionTime and snapshot ID snap-0001",
        remaining.get(0).getDeletionTime() != null && remaining.get(0).getSnapshotId().equals("snap-0001"));
  }

  @Test
  public void deleteDeltaSnapshot1() throws Exception {

    SnapshotInfo full = new SnapshotInfo();
    full.setStatus(StorageProperties.Status.available.toString());
    full.setProgress("100");
    full.setSizeGb(new Integer(1));
    full.setStartTime(new Date(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(30, TimeUnit.MINUTES)));
    full.setSnapshotLocation("http://osg.host/snaps/snap-000001a");
    full.setVolumeId("vol-0000001");
    full.setSnapshotId("snap-000001a");
    full.setSnapPointId("snap-point-id-1a");
    full.setIsOrigin(Boolean.TRUE);

    SnapshotInfo delta1 = new SnapshotInfo();
    delta1.setStatus(StorageProperties.Status.deleting.toString());
    delta1.setProgress("100");
    delta1.setSizeGb(new Integer(1));
    delta1.setStartTime(new Date(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(20, TimeUnit.MINUTES)));
    delta1.setSnapshotLocation("http://osg.host/snaps/snap-000001b");
    delta1.setVolumeId(full.getVolumeId());
    delta1.setSnapshotId("snap-000001b");
    delta1.setSnapPointId("snap-point-id-1b");
    delta1.setIsOrigin(Boolean.TRUE);
    delta1.setPreviousSnapshotId(full.getSnapshotId());

    SnapshotInfo delta2 = new SnapshotInfo();
    delta2.setStatus(StorageProperties.Status.available.toString());
    delta2.setProgress("100");
    delta2.setSizeGb(new Integer(1));
    delta2.setStartTime(new Date(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES)));
    delta2.setSnapshotLocation("http://osg.host/snaps/snap-000001c");
    delta2.setVolumeId(full.getVolumeId());
    delta2.setSnapshotId("snap-000001c");
    delta2.setSnapPointId("snap-point-id-1c");
    delta2.setIsOrigin(Boolean.TRUE);
    delta2.setPreviousSnapshotId(delta1.getSnapshotId());

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      Entities.persist(full);
      Entities.persist(delta1);
      Entities.persist(delta2);
      tran.commit();
    }

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {
        oneOf(storageManager).deleteSnapshot(delta1.getSnapshotId(), delta1.getSnapPointId());
      }
    });

    // Test SnapshotDeleter
    new SnapshotDeleter(storageManager, BlockStorageUnitTestSupport.createMockS3SnapshotTransfer()).run();

    context.assertIsSatisfied();

    SnapshotInfo processedDelta = Transactions.find(new SnapshotInfo(delta1.getSnapshotId()));
    assertTrue("expected to have a result  querying the eucalyptus_storage persistence context for " + delta1.getSnapshotId(),
        processedDelta != null);
    assertTrue("expected SnapshotInfo snapshotID to be " + delta1.getSnapshotId() + " but got " + processedDelta.getSnapshotId(),
        delta1.getSnapshotId().equals(processedDelta.getSnapshotId()));
    assertTrue("expected SnapshotInfo status to be " + StorageProperties.Status.deletedfromebs + " but got " + processedDelta.getStatus(),
        StorageProperties.Status.deletedfromebs.toString().equals(processedDelta.getStatus()));
    assertTrue("expected SnapshotInfo deletionTime to be unset but found " + processedDelta.getDeletionTime(),
        processedDelta.getDeletionTime() == null);
  }

  @Test
  public void deleteDeltaSnapshot2() throws Exception {

    SnapshotInfo full = new SnapshotInfo();
    full.setStatus(StorageProperties.Status.available.toString());
    full.setProgress("100");
    full.setSizeGb(new Integer(1));
    full.setStartTime(new Date(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(30, TimeUnit.MINUTES)));
    full.setSnapshotLocation("http://osg.host/snaps/snap-000001a");
    full.setVolumeId("vol-0000002");
    full.setSnapshotId("snap-000002a");
    full.setSnapPointId("snap-point-id-2a");
    full.setIsOrigin(Boolean.TRUE);

    SnapshotInfo delta1 = new SnapshotInfo();
    delta1.setStatus(StorageProperties.Status.deleting.toString());
    delta1.setProgress("100");
    delta1.setSizeGb(new Integer(1));
    delta1.setStartTime(new Date(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(20, TimeUnit.MINUTES)));
    delta1.setSnapshotLocation("http://osg.host/snaps/snap-000001b");
    delta1.setVolumeId(full.getVolumeId());
    delta1.setSnapshotId("snap-000002b");
    delta1.setSnapPointId("snap-point-id-2b");
    delta1.setIsOrigin(Boolean.TRUE);
    delta1.setPreviousSnapshotId(full.getSnapshotId());

    SnapshotInfo delta2 = new SnapshotInfo();
    delta2.setStatus(StorageProperties.Status.deleted.toString());
    delta2.setProgress("100");
    delta2.setSizeGb(new Integer(1));
    delta2.setStartTime(new Date(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES)));
    delta2.setSnapshotLocation("http://osg.host/snaps/snap-000001c");
    delta2.setVolumeId(full.getVolumeId());
    delta2.setSnapshotId("snap-000002c");
    delta2.setSnapPointId("snap-point-id-2c");
    delta2.setIsOrigin(Boolean.TRUE);
    delta2.setPreviousSnapshotId(delta1.getSnapshotId());
    delta2.setDeletionTime(new Date());

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      Entities.persist(full);
      Entities.persist(delta1);
      Entities.persist(delta2);
      tran.commit();
    }

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {
        oneOf(storageManager).deleteSnapshot(delta1.getSnapshotId(), delta1.getSnapPointId());
      }
    });

    // Test SnapshotDeleter
    new SnapshotDeleter(storageManager, BlockStorageUnitTestSupport.createMockS3SnapshotTransfer()).run();

    context.assertIsSatisfied();

    SnapshotInfo processedDelta = Transactions.find(new SnapshotInfo(delta1.getSnapshotId()));
    assertTrue("expected to have a result  querying the eucalyptus_storage persistence context for " + delta1.getSnapshotId(),
        processedDelta != null);
    assertTrue("expected SnapshotInfo snapshotID to be " + delta1.getSnapshotId() + " but got " + processedDelta.getSnapshotId(),
        delta1.getSnapshotId().equals(processedDelta.getSnapshotId()));
    assertTrue("expected SnapshotInfo status to be " + StorageProperties.Status.deleted + " but got " + processedDelta.getStatus(),
        StorageProperties.Status.deleted.toString().equals(processedDelta.getStatus()));
    assertTrue("expected SnapshotInfo deletionTime to be valid but found " + processedDelta.getDeletionTime(),
        processedDelta.getDeletionTime() != null);
  }

  @Test
  public void deleteDeltaSnapshot3() throws Exception {

    SnapshotInfo full = new SnapshotInfo();
    full.setStatus(StorageProperties.Status.available.toString());
    full.setProgress("100");
    full.setSizeGb(new Integer(1));
    full.setStartTime(new Date(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(30, TimeUnit.MINUTES)));
    full.setSnapshotLocation("http://osg.host/snaps/snap-000001a");
    full.setVolumeId("vol-0000003");
    full.setSnapshotId("snap-000003a");
    full.setSnapPointId("snap-point-id-3a");
    full.setIsOrigin(Boolean.TRUE);

    SnapshotInfo delta1 = new SnapshotInfo();
    delta1.setStatus(StorageProperties.Status.deletedfromebs.toString());
    delta1.setProgress("100");
    delta1.setSizeGb(new Integer(1));
    delta1.setStartTime(new Date(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(20, TimeUnit.MINUTES)));
    delta1.setSnapshotLocation("http://osg.host/snaps/snap-000001b");
    delta1.setVolumeId(full.getVolumeId());
    delta1.setSnapshotId("snap-000003b");
    delta1.setSnapPointId("snap-point-id-3b");
    delta1.setIsOrigin(Boolean.TRUE);
    delta1.setPreviousSnapshotId(full.getSnapshotId());

    SnapshotInfo delta2 = new SnapshotInfo();
    delta2.setStatus(StorageProperties.Status.deleting.toString());
    delta2.setProgress("100");
    delta2.setSizeGb(new Integer(1));
    delta2.setStartTime(new Date(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES)));
    delta2.setSnapshotLocation("http://osg.host/snaps/snap-000001c");
    delta2.setVolumeId(full.getVolumeId());
    delta2.setSnapshotId("snap-000003c");
    delta2.setSnapPointId("snap-point-id-3c");
    delta2.setIsOrigin(Boolean.TRUE);
    delta2.setPreviousSnapshotId(delta1.getSnapshotId());

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      Entities.persist(full);
      Entities.persist(delta1);
      Entities.persist(delta2);
      tran.commit();
    }

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {
        oneOf(storageManager).deleteSnapshot(delta2.getSnapshotId(), delta2.getSnapPointId());
      }
    });

    // Test SnapshotDeleter
    new SnapshotDeleter(storageManager, BlockStorageUnitTestSupport.createMockS3SnapshotTransfer()).run();

    SnapshotInfo processedDelta2 = Transactions.find(new SnapshotInfo(delta2.getSnapshotId()));
    assertTrue("expected to have a result  querying the eucalyptus_storage persistence context for " + delta2.getSnapshotId(),
        processedDelta2 != null);
    assertTrue("expected SnapshotInfo snapshotID to be " + delta2.getSnapshotId() + " but got " + processedDelta2.getSnapshotId(),
        delta2.getSnapshotId().equals(processedDelta2.getSnapshotId()));
    assertTrue("expected SnapshotInfo status to be " + StorageProperties.Status.deleted + " but got " + processedDelta2.getStatus(),
        StorageProperties.Status.deleted.toString().equals(processedDelta2.getStatus()));
    assertTrue("expected SnapshotInfo deletionTime to be valid but found " + processedDelta2.getDeletionTime(),
        processedDelta2.getDeletionTime() != null);

    SnapshotInfo processedDelta1 = Transactions.find(new SnapshotInfo(delta1.getSnapshotId()));
    assertTrue("expected to have a result  querying the eucalyptus_storage persistence context for " + delta1.getSnapshotId(),
        processedDelta1 != null);
    assertTrue("expected SnapshotInfo snapshotID to be " + delta1.getSnapshotId() + " but got " + processedDelta1.getSnapshotId(),
        delta1.getSnapshotId().equals(processedDelta1.getSnapshotId()));

    if (StorageProperties.Status.deleted.toString().equals(processedDelta1.getStatus())) {
      assertTrue("expected SnapshotInfo deletionTime to be valid but found " + processedDelta1.getDeletionTime(),
          processedDelta1.getDeletionTime() != null);

      context.assertIsSatisfied();
    } else if (StorageProperties.Status.deletedfromebs.toString().equals(processedDelta1.getStatus())) {
      assertTrue("expected SnapshotInfo deletionTime to be unset but found " + processedDelta1.getDeletionTime(),
          processedDelta1.getDeletionTime() == null);

      context.checking(new Expectations() {
        {

        }
      });

      // Test SnapshotDeleter
      new SnapshotDeleter(storageManager, BlockStorageUnitTestSupport.createMockS3SnapshotTransfer()).run();

      processedDelta1 = Transactions.find(new SnapshotInfo(delta1.getSnapshotId()));
      assertTrue("expected to have a result  querying the eucalyptus_storage persistence context for " + delta1.getSnapshotId(),
          processedDelta1 != null);
      assertTrue("expected SnapshotInfo snapshotID to be " + delta1.getSnapshotId() + " but got " + processedDelta1.getSnapshotId(),
          delta1.getSnapshotId().equals(processedDelta1.getSnapshotId()));
      assertTrue("expected SnapshotInfo status to be " + StorageProperties.Status.deleted + " but got " + processedDelta1.getStatus(),
          StorageProperties.Status.deleted.toString().equals(processedDelta1.getStatus()));
      assertTrue("expected SnapshotInfo deletionTime to be valid but found " + processedDelta1.getDeletionTime(),
          processedDelta1.getDeletionTime() != null);

      context.assertIsSatisfied();
    } else {
      fail("Found snap " + processedDelta1.getSnapshotId() + " in unexpected state " + processedDelta1.getStatus());
    }
  }

  @Test
  public void deleteDeltaSnapshot4() throws Exception {

    SnapshotInfo full = new SnapshotInfo();
    full.setStatus(StorageProperties.Status.available.toString());
    full.setProgress("100");
    full.setSizeGb(new Integer(1));
    full.setStartTime(new Date(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(30, TimeUnit.MINUTES)));
    full.setSnapshotLocation("http://osg.host/snaps/snap-000004a");
    full.setVolumeId("vol-0000004");
    full.setSnapshotId("snap-000004a");
    full.setSnapPointId("snap-point-id-4a");
    full.setIsOrigin(Boolean.TRUE);

    SnapshotInfo delta1 = new SnapshotInfo();
    delta1.setStatus(StorageProperties.Status.available.toString());
    delta1.setProgress("100");
    delta1.setSizeGb(new Integer(1));
    delta1.setStartTime(new Date(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(20, TimeUnit.MINUTES)));
    delta1.setSnapshotLocation("http://osg.host/snaps/snap-000004b");
    delta1.setVolumeId(full.getVolumeId());
    delta1.setSnapshotId("snap-000004b");
    delta1.setSnapPointId("snap-point-id-4b");
    delta1.setIsOrigin(Boolean.TRUE);
    delta1.setPreviousSnapshotId(full.getSnapshotId());

    SnapshotInfo delta2 = new SnapshotInfo();
    delta2.setStatus(StorageProperties.Status.deleting.toString());
    delta2.setProgress("100");
    delta2.setSizeGb(new Integer(1));
    delta2.setStartTime(new Date(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES)));
    delta2.setSnapshotLocation("http://osg.host/snaps/snap-000004c");
    delta2.setVolumeId(full.getVolumeId());
    delta2.setSnapshotId("snap-000004c");
    delta2.setSnapPointId("snap-point-id-4c");
    delta2.setIsOrigin(Boolean.TRUE);
    delta2.setPreviousSnapshotId(delta1.getSnapshotId());

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      Entities.persist(full);
      Entities.persist(delta1);
      Entities.persist(delta2);
      tran.commit();
    }

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {
        oneOf(storageManager).deleteSnapshot(delta2.getSnapshotId(), delta2.getSnapPointId());
      }
    });

    // Test SnapshotDeleter
    new SnapshotDeleter(storageManager, BlockStorageUnitTestSupport.createMockS3SnapshotTransfer()).run();

    context.assertIsSatisfied();

    SnapshotInfo processedDelta = Transactions.find(new SnapshotInfo(delta2.getSnapshotId()));
    assertTrue("expected to have a result  querying the eucalyptus_storage persistence context for " + delta2.getSnapshotId(),
        processedDelta != null);
    assertTrue("expected SnapshotInfo snapshotID to be " + delta2.getSnapshotId() + " but got " + processedDelta.getSnapshotId(),
        delta2.getSnapshotId().equals(processedDelta.getSnapshotId()));
    assertTrue("expected SnapshotInfo status to be " + StorageProperties.Status.deleted + " but got " + processedDelta.getStatus(),
        StorageProperties.Status.deleted.toString().equals(processedDelta.getStatus()));
    assertTrue("expected SnapshotInfo deletionTime to be valid but found " + processedDelta.getDeletionTime(),
        processedDelta.getDeletionTime() != null);
  }
}
