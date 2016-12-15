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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.eucalyptus.blockstorage.async.ExpiredSnapshotCleaner;
import com.eucalyptus.blockstorage.async.FailedSnapshotCleaner;
import com.eucalyptus.blockstorage.async.SnapshotDeleter;
import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;

/**
 * Created by wesw on 6/20/14.
 */
public class SnapshotCleanupTest {

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
    deleted.setShouldTransfer(Boolean.FALSE);
    deleted.setSnapPointId(null);
    deleted.setStartTime(new Date());
    deleted.setUserName("unittestuser0");
    deleted.setVolumeId("vol-0000");
    deleted.setSnapshotId("snap-0000");
    deleted.setSnapshotLocation("http://osg.host/snaps/good");
    deleted.setDeletionTime(twoHoursAgo.getTime());
    deleted.setSnapPointId("snap-point-foo");

    SnapshotInfo good = new SnapshotInfo();
    good.setStatus(StorageProperties.Status.available.toString());
    good.setProgress("100");
    good.setSizeGb(new Integer(1));
    good.setShouldTransfer(Boolean.FALSE);
    good.setSnapPointId(null);
    good.setStartTime(new Date());
    good.setUserName("unittestuser0");
    good.setVolumeId("vol-0000");
    good.setSnapshotId("snap-0001");
    good.setSnapshotLocation("http://osg.host/snaps/good");
    good.setSnapPointId("snap-point-foo");

    SnapshotInfo failOne = new SnapshotInfo();
    failOne.setStatus(StorageProperties.Status.deleting.toString());
    failOne.setProgress("0");
    failOne.setSizeGb(new Integer(1));
    failOne.setShouldTransfer(Boolean.FALSE);
    failOne.setSnapPointId(null);
    failOne.setStartTime(new Date());
    failOne.setUserName("unittestuser0");
    failOne.setVolumeId("vol-0001");
    failOne.setSnapshotId("snap-0002");
    failOne.setSnapshotLocation("http://osg.host/snaps/failOne");
    failOne.setSnapPointId("snap-point-foo");

    SnapshotInfo failTwo = new SnapshotInfo();
    failTwo.setStatus(StorageProperties.Status.deleting.toString());
    failTwo.setProgress("0");
    failTwo.setSizeGb(new Integer(1));
    failTwo.setShouldTransfer(Boolean.FALSE);
    failTwo.setSnapPointId(null);
    failTwo.setStartTime(new Date());
    failTwo.setUserName("unittestuser0");
    failTwo.setVolumeId("vol-0002");
    failTwo.setSnapshotId("snap-0003");
    failTwo.setSnapshotLocation("http://osg.host/snaps/failTwo");
    failTwo.setSnapPointId("snap-point-foo");

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
    good.setShouldTransfer(Boolean.FALSE);
    good.setSnapPointId(null);
    good.setStartTime(new Date());
    good.setUserName("unittestuser0");
    good.setSnapshotLocation("http://osg.host/snaps/good");
    good.setVolumeId("vol-0000");
    good.setSnapshotId("snap-0000");
    good.setSnapPointId("snap-point-foo");
    

    SnapshotInfo failOne = new SnapshotInfo();
    failOne.setStatus(StorageProperties.Status.failed.toString());
    failOne.setProgress("0");
    failOne.setSizeGb(new Integer(1));
    failOne.setShouldTransfer(Boolean.FALSE);
    failOne.setSnapPointId(null);
    failOne.setStartTime(new Date());
    failOne.setUserName("unittestuser0");
    failOne.setSnapshotLocation("http://osg.host/snaps/failOne");
    failOne.setVolumeId("vol-0001");
    failOne.setSnapshotId("snap-0001");
    failOne.setSnapPointId("snap-point-foo");
    
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
    failTwo.setDeletionTime(twoHoursAgo.getTime());
    failTwo.setSnapPointId("snap-point-foo");

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
}
