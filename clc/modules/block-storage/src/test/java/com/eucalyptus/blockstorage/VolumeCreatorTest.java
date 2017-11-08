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
import java.util.List;

import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.eucalyptus.blockstorage.async.VolumeCreator;
import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.entities.VolumeInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;

/**
 * Created by wesw on 6/20/14.
 */
public class VolumeCreatorTest {

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

    final int volSz = 1;
    SnapshotInfo goodSnap = new SnapshotInfo();
    goodSnap.setStatus(StorageProperties.Status.available.toString());
    goodSnap.setProgress("100");
    goodSnap.setSizeGb(new Integer(volSz));
    goodSnap.setShouldTransfer(Boolean.FALSE);
    goodSnap.setSnapPointId(null);
    goodSnap.setStartTime(new Date());
    goodSnap.setUserName("unittestuser0");
    goodSnap.setVolumeId("vol-0000");
    goodSnap.setSnapshotId("snap-0000");
    goodSnap.setSnapshotLocation("http://osg.host/snaps/goodSnap");

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      Entities.persist(goodSnap);
      tran.commit();
    }

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {
        oneOf(storageManager).createVolume("vol-0000", "snap-0000", 1);
        will(createVolume(volSz));
      }
    });

    VolumeCreator bscvc = new VolumeCreator("vol-0000", null, "snap-0000", null, 1, storageManager);
    bscvc.run();

    List<VolumeInfo> remaining;
    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      remaining = Entities.query(new VolumeInfo());
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    assertTrue("expected one VolumeInfo to exist, but there are " + remaining.size(), remaining.size() == 1);
    assertTrue("expected volumeinfo to be vol-0000 but was " + remaining.get(0).getVolumeId(), "vol-0000".equals(remaining.get(0).getVolumeId()));
  }

  @Test
  public void run_BasicFromParentVolumeTest() throws Exception {

    final int volSz = 1;
    VolumeInfo good = new VolumeInfo();
    good.setStatus(StorageProperties.Status.available.toString());
    good.setSize(new Integer(volSz));
    good.setUserName("unittestuser0");
    good.setVolumeId("vol-0000");
    good.setSnapshotId("snap-0000");
    good.setCreateTime(new Date());
    good.setZone("eucalyptus");

    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      Entities.persist(good);
      tran.commit();
    }

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {
        oneOf(storageManager).cloneVolume("vol-0001", "vol-0000");
        will(createVolume());
      }
    });

    VolumeCreator bscvc = new VolumeCreator("vol-0001", null, null, "vol-0000", 1, storageManager);
    bscvc.run();

    List<VolumeInfo> remaining;
    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      remaining = Entities.query(new VolumeInfo());
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    assertTrue("expected two VolumeInfos to exist, but there are " + remaining.size(), remaining.size() == 2);
  }

  @Test
  public void run_BasicFromNothingVolumeTest() throws Exception {

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {
        oneOf(storageManager).createVolume("vol-0001", 1);
        will(createVolume());
      }
    });

    VolumeCreator bscvc = new VolumeCreator("vol-0001", null, null, null, 1, storageManager);
    bscvc.run();

    List<VolumeInfo> remaining;
    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      remaining = Entities.query(new VolumeInfo());
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    assertTrue("expected one VolumeInfo to exist, but there are " + remaining.size(), remaining.size() == 1);
  }

  private Action createVolume(int size) {
    return new CreateVolumeAction(size);
  }

  private Action createVolume() {
    return new CreateVolumeAction();
  }

  private static class CreateVolumeAction implements Action {
    private int size = 0;

    private CreateVolumeAction(int size) {
      this.size = size;
    }

    private CreateVolumeAction() {}

    @Override
    public Object invoke(Invocation invocation) throws Throwable {
      String volId = (String) invocation.getParameter(0);
      VolumeInfo good = new VolumeInfo();
      good.setStatus(StorageProperties.Status.available.toString());
      good.setSize(new Integer(size));
      good.setUserName("unittestuser0");
      good.setVolumeId(volId);
      good.setCreateTime(new Date());
      good.setZone("eucalyptus");
      try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
        Entities.persist(good);
        tran.commit();
      }
      return size > 0 ? size : null;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("creates a VolumeInfo entity when called");
    }
  }

}
