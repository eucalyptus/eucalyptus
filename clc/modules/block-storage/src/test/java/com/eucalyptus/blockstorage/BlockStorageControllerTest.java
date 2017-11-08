/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.blockstorage;

import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.entities.VolumeInfo;
import com.eucalyptus.blockstorage.msgs.DeleteStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.DeleteStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageSnapshotsResponseType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageSnapshotsType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageVolumesResponseType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageVolumesType;
import com.eucalyptus.blockstorage.msgs.GetStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.GetStorageVolumeType;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;

/**
 * Created by wesw on 6/20/14.
 */
public class BlockStorageControllerTest {

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
  public void GetStorageVolume_BasicTest() throws Exception {

    VolumeInfo volOne = new VolumeInfo();
    volOne.setStatus(StorageProperties.Status.available.toString());
    volOne.setSize(new Integer(1));
    volOne.setUserName("unittestuser0");
    volOne.setVolumeId("vol-0000");
    volOne.setSnapshotId("snap-0000");
    volOne.setCreateTime(new Date());
    volOne.setZone("eucalyptus");

    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      Entities.persist(volOne);
      tran.commit();
    }

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {
        oneOf(storageManager).getVolumeConnectionString("vol-0000");
        will(returnValue("foo"));
      }
    });

    StorageProperties.enableStorage = true;
    BlockStorageController bsc = new BlockStorageController(storageManager);

    GetStorageVolumeType request = new GetStorageVolumeType();
    request.setVolumeId("vol-0000");
    GetStorageVolumeResponseType response = bsc.GetStorageVolume(request);

    assertTrue("expected to receive proper volumeId in response", "vol-0000".equals(response.getVolumeId()));
    assertTrue("expected to receive correct size in response", "1".equals(response.getSize()));
    assertTrue("expected to receive correct status in response", StorageProperties.Status.available.toString().equals(response.getStatus()));
    assertTrue("expected to receive correct snapshotId in response", "snap-0000".equals(response.getSnapshotId()));
    assertTrue("expected to receive correct device name in response", "foo".equals(response.getActualDeviceName()));
  }

  @Test
  public void DeleteStorageVolume_BasicTest() throws Exception {

    VolumeInfo volOne = new VolumeInfo();
    volOne.setStatus(StorageProperties.Status.available.toString());
    volOne.setSize(new Integer(1));
    volOne.setUserName("unittestuser0");
    volOne.setVolumeId("vol-0000");
    volOne.setSnapshotId("snap-0000");
    volOne.setCreateTime(new Date());
    volOne.setZone("eucalyptus");

    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      Entities.persist(volOne);
      tran.commit();
    }

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {

      }
    });

    StorageProperties.enableStorage = true;
    BlockStorageController bsc = new BlockStorageController(storageManager);

    DeleteStorageVolumeType request = new DeleteStorageVolumeType();
    request.setVolumeId("vol-0000");
    DeleteStorageVolumeResponseType response = bsc.DeleteStorageVolume(request);

    VolumeInfo retrieved;
    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      retrieved = Entities.uniqueResult(new VolumeInfo("vol-0000"));
      tran.commit();
    }

    assertTrue("expected to find volume after execution", "vol-0000".equals(retrieved.getVolumeId()));
    assertTrue("expected retrieved volume to be 'deleting'", StorageProperties.Status.deleting.toString().equals(retrieved.getStatus()));
  }

  @Test
  public void DescribeStorageSnapshots_BasicTest() throws Exception {

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

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      Entities.persist(good);
      tran.commit();
    }

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {

      }
    });

    BlockStorageController bsc = new BlockStorageController(storageManager);

    DescribeStorageSnapshotsType request = new DescribeStorageSnapshotsType();
    DescribeStorageSnapshotsResponseType response = bsc.DescribeStorageSnapshots(request);

    assertTrue("expected at least one snapshot in the response", response != null && response.getSnapshotSet() != null
        && response.getSnapshotSet().size() > 0);
    assertTrue("expected to find manually created snapshot in the response", response.getSnapshotSet().get(0).getSnapshotId().equals("snap-0000"));
  }

  @Test
  public void DescribeStorageVolumes_BasicTest() throws Exception {

    VolumeInfo volOne = new VolumeInfo();
    volOne.setStatus(StorageProperties.Status.available.toString());
    volOne.setSize(new Integer(1));
    volOne.setUserName("unittestuser0");
    volOne.setVolumeId("vol-0000");
    volOne.setSnapshotId("snap-0000");
    volOne.setCreateTime(new Date());
    volOne.setZone("eucalyptus");

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      Entities.persist(volOne);
      tran.commit();
    }

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {

      }
    });

    BlockStorageController bsc = new BlockStorageController(storageManager);

    DescribeStorageVolumesType request = new DescribeStorageVolumesType();
    DescribeStorageVolumesResponseType response = bsc.DescribeStorageVolumes(request);

    assertTrue("expected at least one volume in the response", response != null && response.getVolumeSet() != null
        && response.getVolumeSet().size() > 0);
    assertTrue("expected to find manually created snapshot in the response", response.getVolumeSet().get(0).getVolumeId().equals("vol-0000"));
  }

  // @Test
  // public void CreateStorageVolume_BasicTest() throws Exception {
  //
  // StorageProperties.enableStorage = true;
  // StorageProperties.shouldEnforceUsageLimits = true;
  //
  // final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
  // context.checking(new Expectations() {
  // {
  //
  // }
  // });
  // final List<BlockStorageController.VolumeTask> holder = Lists.newArrayList();
  //
  // BlockStorageController bsc = new BlockStorageController(storageManager);
  // bsc.volumeService = new VolumeService() {
  // @Override
  // public void add(BlockStorageController.VolumeTask creator) {
  // holder.add(creator);
  // }
  // };
  //
  // CreateStorageVolumeType request = new CreateStorageVolumeType();
  // request.setVolumeId("vol-" + Hashes.getRandom(10));
  // request.setSize("5");
  // CreateStorageVolumeResponseType response = bsc.CreateStorageVolume(request);
  //
  // assertTrue("expected to get a volume id", response.getVolumeId() != null && !"".equals(response.getVolumeId()));
  // assertTrue("expected size to match the request, but was - '" + response.getSize() + "'",
  // response.getSize() != null && "5".equals(response.getSize()));
  // assertTrue("expected status to be 'creating' but was - " + response.getStatus(), response.getStatus() != null
  // && StorageProperties.Status.creating.toString().equals(response.getStatus()));
  // assertTrue("expected the controller to submit the task ", holder.size() > 0 && holder.get(0) instanceof BlockStorageController.VolumeCreator);
  // }
}
