package com.eucalyptus.blockstorage.async;

import static org.junit.Assert.assertTrue;

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

import com.eucalyptus.blockstorage.BlockDeviceResource;
import com.eucalyptus.blockstorage.BlockStorageUnitTestSupport;
import com.eucalyptus.blockstorage.LogicalStorageManager;
import com.eucalyptus.blockstorage.StorageResource;
import com.eucalyptus.blockstorage.StorageResourceWithCallback;
import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.entities.StorageInfo;
import com.eucalyptus.blockstorage.entities.VolumeInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.google.common.base.Function;
import com.google.common.base.Strings;

public class SnapshotCreatorTest {

  private static Logger LOG = Logger.getLogger(SnapshotCreatorTest.class);

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
    BlockStorageUnitTestSupport.flushStorageInfos();
    BlockStorageUnitTestSupport.flushBlockStorageEntities();
  }

  @AfterClass
  public static void teardownClass() {
    BlockStorageUnitTestSupport.tearDownBlockStoragePersistenceContext();
    BlockStorageUnitTestSupport.tearDownAuthPersistenceContext();
  }

  /**
   * <li>snapshot transfer disabled</li>
   * <li>snapshot deltas enabled</li>
   * 
   * @throws Exception
   */
  @Test
  public void create_snapshot_test_1() throws Exception {
    StorageInfo storageInfo = new StorageInfo();
    storageInfo.setDefaults();
    storageInfo.setShouldTransferSnapshots(Boolean.FALSE);
    storageInfo.setMaxSnapshotDeltas(10);
    try (TransactionResource tran = Entities.transactionFor(StorageInfo.class)) {
      Entities.persist(storageInfo);
      tran.commit();
    }

    VolumeInfo vol = new VolumeInfo("vol-0000001");
    vol.setCreateTime(new Date(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS)));
    vol.setSize(1);
    vol.setStatus(StorageProperties.Status.available.toString());

    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      Entities.persist(vol);
      tran.commit();
    }

    SnapshotInfo snap = new SnapshotInfo("snap-0000001");
    snap.setIsOrigin(Boolean.TRUE);
    snap.setProgress("0");
    snap.setSizeGb(vol.getSize());
    snap.setSnapPointId("snap-point-id-1");
    snap.setStartTime(new Date(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(20, TimeUnit.MINUTES)));
    snap.setStatus(StorageProperties.Status.creating.toString());
    snap.setVolumeId(vol.getVolumeId());

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      Entities.persist(snap);
      tran.commit();
    }

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {
        oneOf(storageManager).createSnapshot(vol.getVolumeId(), snap.getSnapshotId(), snap.getSnapPointId());

        oneOf(storageManager).finishVolume(snap.getSnapshotId());
      }
    });

    new SnapshotCreator(vol.getVolumeId(), snap.getSnapshotId(), snap.getSnapPointId(), storageManager,
        BlockStorageUnitTestSupport.createMockS3SnapshotTransfer(), BlockStorageUnitTestSupport.createMockSnapshotProgressCallback()).run();

    context.assertIsSatisfied();

    List<SnapshotInfo> snaps;
    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      snaps = Entities.query(new SnapshotInfo());
      tran.commit();
    }

    assertTrue("expected to have a result set querying the snapshots table in eucalyptus_storage persistence context", snaps != null);
    assertTrue("expected 1 SnapshotInfo entity to exist, but found " + snaps.size(), snaps.size() == 1);
    assertTrue("expected SnapshotInfo entity for " + snap.getSnapshotId() + " but found entity for " + snaps.get(0).getSnapshotId(),
        snap.getSnapshotId().equals(snaps.get(0).getSnapshotId()));
    assertTrue("expected SnapshotInfo entity status to be " + StorageProperties.Status.available.toString() + " but found it to be "
        + snaps.get(0).getStatus(), StorageProperties.Status.available.toString().equals(snaps.get(0).getStatus()));
    assertTrue("expected SnapshotInfo entity progress to be 100% but found it to be " + snaps.get(0).getProgress(),
        "100".equals(snaps.get(0).getProgress()));
    assertTrue("expected SnapshotInfo entity to contain a null reference for snapshotLocation but found " + snaps.get(0).getSnapshotLocation(),
        null == snaps.get(0).getSnapshotLocation());
    assertTrue("expected SnapshotInfo entity to contain null reference for previousSnapshotID but found " + snaps.get(0).getPreviousSnapshotId(),
        null == snaps.get(0).getPreviousSnapshotId());
  }

  /**
   * <li>snapshot transfer enabled</li>
   * <li>snapshot deltas enabled</li>
   * <li>backend deltas disabled</li>
   * 
   * @throws Exception
   */
  @Test
  public void create_snapshot_test_2() throws Exception {
    StorageInfo storageInfo = new StorageInfo();
    storageInfo.setDefaults();
    storageInfo.setMaxSnapshotDeltas(10);
    try (TransactionResource tran = Entities.transactionFor(StorageInfo.class)) {
      Entities.persist(storageInfo);
      tran.commit();
    }

    VolumeInfo vol = new VolumeInfo("vol-0000002");
    vol.setCreateTime(new Date(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS)));
    vol.setSize(2);
    vol.setStatus(StorageProperties.Status.available.toString());

    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      Entities.persist(vol);
      tran.commit();
    }

    SnapshotInfo snap = new SnapshotInfo("snap-0000002");
    snap.setIsOrigin(Boolean.TRUE);
    snap.setProgress("0");
    snap.setSizeGb(vol.getSize());
    snap.setSnapPointId("snap-point-id-2");
    snap.setStartTime(new Date());
    snap.setStatus(StorageProperties.Status.creating.toString());
    snap.setVolumeId(vol.getVolumeId());

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      Entities.persist(snap);
      tran.commit();
    }

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {
        oneOf(storageManager).createSnapshot(vol.getVolumeId(), snap.getSnapshotId(), snap.getSnapPointId());

        oneOf(storageManager).supportsIncrementalSnapshots();
        will(returnValue(false));

        oneOf(storageManager).prepSnapshotForUpload(vol.getVolumeId(), snap.getSnapshotId(), snap.getSnapPointId());
        will(returnValue(new BlockDeviceResource(snap.getSnapshotId(), "path-to-snap")));

        oneOf(storageManager).finishVolume(snap.getSnapshotId());
      }
    });

    new SnapshotCreator(vol.getVolumeId(), snap.getSnapshotId(), snap.getSnapPointId(), storageManager,
        BlockStorageUnitTestSupport.createMockS3SnapshotTransfer(), BlockStorageUnitTestSupport.createMockSnapshotProgressCallback()).run();

    context.assertIsSatisfied();

    List<SnapshotInfo> snaps;
    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      snaps = Entities.query(new SnapshotInfo());
      tran.commit();
    }

    assertTrue("expected to have a result set querying the snapshots table in eucalyptus_storage persistence context", snaps != null);
    assertTrue("expected 1 SnapshotInfo to exist, but found " + snaps.size(), snaps.size() == 1);
    assertTrue("expected SnapshotInfo entity for " + snap.getSnapshotId() + " but found entity for " + snaps.get(0).getSnapshotId(),
        snap.getSnapshotId().equals(snaps.get(0).getSnapshotId()));
    assertTrue("expected SnapshotInfo entity status to be " + StorageProperties.Status.available.toString() + " but found it to be "
        + snaps.get(0).getStatus(), StorageProperties.Status.available.toString().equals(snaps.get(0).getStatus()));
    assertTrue("expected SnapshotInfo entity progress to be 100% but found it to be " + snaps.get(0).getProgress(),
        "100".equals(snaps.get(0).getProgress()));
    assertTrue("expected SnapshotInfo entity to contain a valid reference for snapshotLocation but found " + snaps.get(0).getSnapshotLocation(),
        !Strings.isNullOrEmpty(snaps.get(0).getSnapshotLocation()));
    assertTrue("expected SnapshotInfo entity to contain null reference for previousSnapshotID but found " + snaps.get(0).getPreviousSnapshotId(),
        null == snaps.get(0).getPreviousSnapshotId());
  }

  /**
   * <li>snapshot transfer enabled</li>
   * <li>snapshot deltas enabled</li>
   * <li>backend deltas enabled</li>
   * <li>first snapshot on volume</li>
   * 
   * @throws Exception
   */
  @Test
  public void create_snapshot_test_3() throws Exception {
    StorageInfo storageInfo = new StorageInfo();
    storageInfo.setDefaults();
    storageInfo.setMaxSnapshotDeltas(10);
    try (TransactionResource tran = Entities.transactionFor(StorageInfo.class)) {
      Entities.persist(storageInfo);
      tran.commit();
    }

    VolumeInfo vol = new VolumeInfo("vol-0000003");
    vol.setCreateTime(new Date(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS)));
    vol.setSize(2);
    vol.setStatus(StorageProperties.Status.available.toString());

    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      Entities.persist(vol);
      tran.commit();
    }

    SnapshotInfo snap = new SnapshotInfo("snap-0000003");
    snap.setIsOrigin(Boolean.TRUE);
    snap.setProgress("0");
    snap.setSizeGb(vol.getSize());
    snap.setSnapPointId("snap-point-id-3");
    snap.setStartTime(new Date());
    snap.setStatus(StorageProperties.Status.creating.toString());
    snap.setVolumeId(vol.getVolumeId());

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      Entities.persist(snap);
      tran.commit();
    }

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {
        oneOf(storageManager).createSnapshot(vol.getVolumeId(), snap.getSnapshotId(), snap.getSnapPointId());

        oneOf(storageManager).supportsIncrementalSnapshots();
        will(returnValue(true));

        oneOf(storageManager).prepSnapshotForUpload(vol.getVolumeId(), snap.getSnapshotId(), snap.getSnapPointId());
        will(returnValue(new BlockDeviceResource(snap.getSnapshotId(), "path-to-snap")));

        oneOf(storageManager).finishVolume(snap.getSnapshotId());
      }
    });

    new SnapshotCreator(vol.getVolumeId(), snap.getSnapshotId(), snap.getSnapPointId(), storageManager,
        BlockStorageUnitTestSupport.createMockS3SnapshotTransfer(), BlockStorageUnitTestSupport.createMockSnapshotProgressCallback()).run();

    context.assertIsSatisfied();

    List<SnapshotInfo> snaps;
    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      snaps = Entities.query(new SnapshotInfo());
      tran.commit();
    }

    assertTrue("expected to have a result set querying the snapshots table in eucalyptus_storage persistence context", snaps != null);
    assertTrue("expected 1 SnapshotInfo to exist, but found " + snaps.size(), snaps.size() == 1);
    assertTrue("expected SnapshotInfo entity for " + snap.getSnapshotId() + " but found entity for " + snaps.get(0).getSnapshotId(),
        snap.getSnapshotId().equals(snaps.get(0).getSnapshotId()));
    assertTrue("expected SnapshotInfo entity status to be " + StorageProperties.Status.available.toString() + " but found it to be "
        + snaps.get(0).getStatus(), StorageProperties.Status.available.toString().equals(snaps.get(0).getStatus()));
    assertTrue("expected SnapshotInfo entity progress to be 100% but found it to be " + snaps.get(0).getProgress(),
        "100".equals(snaps.get(0).getProgress()));
    assertTrue("expected SnapshotInfo entity to contain a valid reference for snapshotLocation but found " + snaps.get(0).getSnapshotLocation(),
        !Strings.isNullOrEmpty(snaps.get(0).getSnapshotLocation()));
    assertTrue("expected SnapshotInfo entity to contain null reference for previousSnapshotID but found " + snaps.get(0).getPreviousSnapshotId(),
        null == snaps.get(0).getPreviousSnapshotId());
  }

  /**
   * <li>snapshot transfer enabled</li>
   * <li>snapshot deltas enabled</li>
   * <li>backend deltas enabled</li>
   * <li>non-first snapshot on volume</li>
   * 
   * @throws Exception
   */
  @Test
  public void create_snapshot_test_4() throws Exception {
    StorageInfo storageInfo = new StorageInfo();
    storageInfo.setDefaults();
    storageInfo.setMaxSnapshotDeltas(10);
    try (TransactionResource tran = Entities.transactionFor(StorageInfo.class)) {
      Entities.persist(storageInfo);
      tran.commit();
    }

    VolumeInfo vol = new VolumeInfo("vol-0000004");
    vol.setCreateTime(new Date(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS)));
    vol.setSize(2);
    vol.setStatus(StorageProperties.Status.available.toString());

    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      Entities.persist(vol);
      tran.commit();
    }

    SnapshotInfo snapa = new SnapshotInfo("snap-000004a");
    snapa.setIsOrigin(Boolean.TRUE);
    snapa.setProgress("100");
    snapa.setSizeGb(vol.getSize());
    snapa.setSnapPointId("snap-point-id-4a");
    snapa.setStartTime(new Date(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES)));
    snapa.setStatus(StorageProperties.Status.available.toString());
    snapa.setVolumeId(vol.getVolumeId());
    snapa.setSnapshotLocation("snapshots://objectstoragegateway/bucket-for-upload/snap-000004a");

    SnapshotInfo snapb = new SnapshotInfo("snap-000004b");
    snapb.setIsOrigin(Boolean.TRUE);
    snapb.setProgress("0");
    snapb.setSizeGb(vol.getSize());
    snapb.setSnapPointId("snap-point-id-4b");
    snapb.setStartTime(new Date());
    snapb.setStatus(StorageProperties.Status.creating.toString());
    snapb.setVolumeId(vol.getVolumeId());

    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      Entities.persist(snapa);
      Entities.persist(snapb);
      tran.commit();
    }

    final Function<StorageResource, String> callback = new Function<StorageResource, String>() {

      @Override
      public String apply(StorageResource arg0) {
        return "blah";
      }
    };

    final BlockDeviceResource bdr = new BlockDeviceResource(snapb.getSnapshotId(), "path-to-snap");

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {
        oneOf(storageManager).createSnapshot(vol.getVolumeId(), snapb.getSnapshotId(), snapb.getSnapPointId());

        oneOf(storageManager).supportsIncrementalSnapshots();
        will(returnValue(true));

        oneOf(storageManager).prepIncrementalSnapshotForUpload(vol.getVolumeId(), snapb.getSnapshotId(), snapb.getSnapPointId(),
            snapa.getSnapshotId(), snapa.getSnapPointId());
        will(returnValue(new StorageResourceWithCallback(bdr, callback)));

        oneOf(storageManager).executeCallback(callback, bdr);
        will(returnValue("crap"));

        oneOf(storageManager).finishVolume(snapb.getSnapshotId());
      }
    });

    new SnapshotCreator(vol.getVolumeId(), snapb.getSnapshotId(), snapb.getSnapPointId(), storageManager,
        BlockStorageUnitTestSupport.createMockS3SnapshotTransfer(), BlockStorageUnitTestSupport.createMockSnapshotProgressCallback()).run();

    context.assertIsSatisfied();

    List<SnapshotInfo> snaps;
    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      snaps = Entities.query(new SnapshotInfo());
      tran.commit();
    }

    assertTrue("expected to have a result set querying the snapshots table in eucalyptus_storage persistence context", snaps != null);
    assertTrue("expected 2 SnapshotInfo entities to exist, but found " + snaps.size(), snaps.size() == 2);
    for (SnapshotInfo snap : snaps) {
      assertTrue("expected SnapshotInfo entity for " + snapa.getSnapshotId() + " or " + snapb.getSnapshotId() + " but found entity for "
          + snap.getSnapshotId(), (snapa.getSnapshotId().equals(snap.getSnapshotId()) || snapb.getSnapshotId().equals(snap.getSnapshotId())));
      assertTrue(
          "expected SnapshotInfo entity status to be " + StorageProperties.Status.available.toString() + " but found it to be " + snap.getStatus(),
          StorageProperties.Status.available.toString().equals(snap.getStatus()));
      assertTrue("expected SnapshotInfo entity progress to be 100% but found it to be " + snap.getProgress(), "100".equals(snap.getProgress()));
      assertTrue("expected SnapshotInfo entity to contain a valid reference for snapshotLocation but found " + snap.getSnapshotLocation(),
          !Strings.isNullOrEmpty(snap.getSnapshotLocation()));
      if (snapa.getSnapshotId().equals(snap.getSnapshotId())) {
        assertTrue("expected SnapshotInfo entity to contain null reference for previousSnapshotID but found " + snap.getPreviousSnapshotId(),
            null == snap.getPreviousSnapshotId());
      } else {
        assertTrue("expected previousSnapshotID of SnapshotInfo entity to be " + snapa.getSnapshotId() + " but found " + snap.getPreviousSnapshotId(),
            snapa.getSnapshotId().equals(snap.getPreviousSnapshotId()));
      }
    }
  }
}
