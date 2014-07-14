package com.eucalyptus.blockstorage;

import com.eucalyptus.blockstorage.entities.VolumeInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.*;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created by wesw on 6/20/14.
 */
public class BlockStorageController_VolumeDeleterTaskTest {

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
        Calendar twoDaysAgo = Calendar.getInstance();
        twoDaysAgo.add(Calendar.HOUR, -49);

        VolumeInfo good = new VolumeInfo();
        good.setStatus(StorageProperties.Status.deleted.toString());
        good.setSize(new Integer(1));
        good.setUserName("unittestuser0");
        good.setVolumeId("vol-0000");
        good.setSnapshotId("snap-0000");
        good.setCreateTime(twoDaysAgo.getTime());
        good.setDeletionTime(twoDaysAgo.getTime());
        good.setZone("eucalyptus");

        VolumeInfo failOne = new VolumeInfo();
        failOne.setStatus(StorageProperties.Status.deleting.toString());
        failOne.setSize(new Integer(1));
        failOne.setUserName("unittestuser0");
        failOne.setVolumeId("vol-0001");
        failOne.setSnapshotId("snap-0001");
        failOne.setCreateTime(new Date());
        failOne.setZone("eucalyptus");

        VolumeInfo failTwo = new VolumeInfo();
        failTwo.setStatus(StorageProperties.Status.deleting.toString());
        failTwo.setSize(new Integer(1));
        failTwo.setUserName("unittestuser0");
        failTwo.setVolumeId("vol-0002");
        failTwo.setSnapshotId("snap-0002");
        failTwo.setCreateTime(new Date());
        failTwo.setZone("eucalyptus");

        try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
            Entities.persist(good);
            Entities.persist(failOne);
            Entities.persist(failTwo);
            tran.commit();
        }

        final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
        context.checking(new Expectations(){{
            exactly(2).of(storageManager).deleteVolume(with(any(String.class)));
        }});

        BlockStorageController bsc =
                new BlockStorageController( storageManager );
        BlockStorageController.VolumeDeleterTask bscvdt = new BlockStorageController.VolumeDeleterTask( );
        bscvdt.run();

        List<VolumeInfo> remaining ;
        try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
            remaining = Entities.query(new VolumeInfo());
            tran.commit();
        }

        assertTrue("expected to have a result set querying the eucalyptus_storage persistence context",
                remaining != null);
        assertTrue("expected two VolumeInfos to still exist",
                remaining.size() == 2);

        try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
            VolumeInfo example = new VolumeInfo();
            example.setStatus(StorageProperties.Status.deleted.toString());
            remaining = Entities.query(example);
            tran.commit();
        }

        assertTrue("expected to have a result set querying the eucalyptus_storage persistence context for deleted volumeinfos",
                remaining != null);
        assertTrue("expected two VolumeInfos to exist",
                remaining.size() == 2);

    }

}
