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

import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.entities.VolumeInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.*;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created by wesw on 6/20/14.
 */
public class BlockStorageController_VolumeCreatorTaskTest {

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
        context.checking(new Expectations(){{
            oneOf(storageManager).createVolume("vol-0000", "snap-0000", 1); will(createVolume(volSz ));
        }});

        BlockStorageController bsc =
                new BlockStorageController( storageManager );
        BlockStorageController.VolumeCreator bscvc = new BlockStorageController.VolumeCreator("vol-0000", null, "snap-0000", null, 1 );
        bscvc.run();

        List<VolumeInfo> remaining ;
        try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
            remaining = Entities.query(new VolumeInfo());
            tran.commit();
        }

        assertTrue("expected to have a result set querying the eucalyptus_storage persistence context",
                remaining != null);
        assertTrue("expected one VolumeInfo to exist, but there are " + remaining.size(),
                remaining.size() == 1);
        assertTrue("expected volumeinfo to be vol-0000 but was " + remaining.get(0).getVolumeId(),
                "vol-0000".equals(remaining.get(0).getVolumeId()));
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
        context.checking(new Expectations(){{
            oneOf(storageManager).cloneVolume("vol-0001", "vol-0000"); will(createVolume());
        }});

        BlockStorageController bsc =
                new BlockStorageController( storageManager );
        BlockStorageController.VolumeCreator bscvc = new BlockStorageController.VolumeCreator("vol-0001", null, null, "vol-0000", 1 );
        bscvc.run();

        List<VolumeInfo> remaining ;
        try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
            remaining = Entities.query(new VolumeInfo());
            tran.commit();
        }

        assertTrue("expected to have a result set querying the eucalyptus_storage persistence context",
                remaining != null);
        assertTrue("expected two VolumeInfos to exist, but there are " + remaining.size(),
                remaining.size() == 2);
    }

    @Test
    public void run_BasicFromNothingVolumeTest() throws Exception {

        final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
        context.checking(new Expectations(){{
            oneOf(storageManager).createVolume("vol-0001", 1); will(createVolume());
        }});

        BlockStorageController bsc =
                new BlockStorageController( storageManager );
        BlockStorageController.VolumeCreator bscvc = new BlockStorageController.VolumeCreator("vol-0001", null, null, null, 1 );
        bscvc.run();

        List<VolumeInfo> remaining ;
        try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
            remaining = Entities.query(new VolumeInfo());
            tran.commit();
        }

        assertTrue("expected to have a result set querying the eucalyptus_storage persistence context",
                remaining != null);
        assertTrue("expected one VolumeInfo to exist, but there are " + remaining.size(),
                remaining.size() == 1);
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

        private CreateVolumeAction() { }

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
