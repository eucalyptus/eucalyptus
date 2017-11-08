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
import static org.junit.Assert.fail;

import java.util.List;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.eucalyptus.blockstorage.entities.CHAPUserInfo;
import com.eucalyptus.blockstorage.entities.DirectStorageInfo;
import com.eucalyptus.blockstorage.entities.ISCSIMetaInfo;
import com.eucalyptus.blockstorage.entities.ISCSIVolumeInfo;
import com.eucalyptus.blockstorage.util.BlockStorageUtilSvc;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;

/**
 *
 */
public class ISCSIManagerTest {

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
  public void allocateTarget_BasicTest() throws Exception {
    final String volId = "vol-0000";
    final ISCSIVolumeInfo iscsiVolumeInfo = new ISCSIVolumeInfo();
    iscsiVolumeInfo.setVolumeId(volId);

    final TGTService tgtService = context.mock(TGTService.class);
    final BlockStorageUtilSvc blockStorageUtilSvc = context.mock(BlockStorageUtilSvc.class);
    context.checking(new Expectations() {
      {
        oneOf(tgtService).targetExists(volId, 1, null, DirectStorageInfo.getStorageInfo().getTimeoutInMillis());
        will(returnValue(Boolean.FALSE));
      }
    });

    ISCSIMetaInfo metaInfo = new ISCSIMetaInfo(StorageProperties.NAME);
    metaInfo.setStoreNumber(new Integer(1));
    metaInfo.setTid(new Integer(1));
    metaInfo.setStorePrefix("foo:");
    metaInfo.setStoreUser("unittestuser0");

    try (TransactionResource tran = Entities.transactionFor(ISCSIMetaInfo.class)) {
      metaInfo = Entities.persist(metaInfo);
      tran.commit();
    }

    ISCSIManager iscsiManager = new ISCSIManager(tgtService, blockStorageUtilSvc);
    iscsiManager.allocateTarget(iscsiVolumeInfo);

    List<ISCSIMetaInfo> remaining;
    try (TransactionResource tran = Entities.transactionFor(ISCSIMetaInfo.class)) {
      remaining = Entities.query(new ISCSIMetaInfo());
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    ISCSIMetaInfo retrieved = remaining.get(0);
    assertTrue("expected store number to be 2 but was " + retrieved.getStoreNumber(), retrieved.getStoreNumber().intValue() == 2);
    assertTrue("expected tid to be 2", retrieved.getTid().intValue() == 2);
    String storeName = "foo:" + StorageProperties.NAME + ":store2";
    assertTrue("expected iscsiVolumeInfo store name to be " + storeName, storeName.equals(iscsiVolumeInfo.getStoreName()));
    assertTrue("expected iscsiVolumeInfo store user to be unittestuser0 but was " + iscsiVolumeInfo.getStoreUser(),
        "unittestuser0".equals(iscsiVolumeInfo.getStoreUser()));
    assertTrue("expected iscsiVolumeInfo tid to be 1 and lun to be 1", iscsiVolumeInfo.getTid().intValue() == 1
        && iscsiVolumeInfo.getLun().intValue() == 1);
  }

  @Test
  public void configure_BasicTest() throws Exception {

    final Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
    final TGTService tgtService = context.mock(TGTService.class);
    final BlockStorageUtilSvc blockStorageUtilSvc = context.mock(BlockStorageUtilSvc.class);
    context.checking(new Expectations() {
      {
        oneOf(tgtService).userExists("eucalyptus", timeout);
        will(returnValue(Boolean.FALSE));
        oneOf(tgtService).addUser(with(equal("eucalyptus")), with(any(String.class)), with(equal(timeout)));
        oneOf(blockStorageUtilSvc).encryptSCTargetPassword(with(any(String.class)));
        will(returnValue("foo"));
      }
    });

    ISCSIManager iscsiManager = new ISCSIManager(tgtService, blockStorageUtilSvc);
    iscsiManager.configure();

    List<ISCSIMetaInfo> remaining;
    try (TransactionResource tran = Entities.transactionFor(ISCSIMetaInfo.class)) {
      remaining = Entities.query(new ISCSIMetaInfo());
      tran.commit();
    }

    // make sure the meta info was created
    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    ISCSIMetaInfo retrieved = remaining.get(0);
    assertTrue("expected store prefix to be " + StorageProperties.STORE_PREFIX + " but it was " + retrieved.getStorePrefix(),
        StorageProperties.STORE_PREFIX.equals(retrieved.getStorePrefix()));
    assertTrue("expected store number to be 0 but it was " + retrieved.getStoreNumber().intValue(), retrieved.getStoreNumber().intValue() == 0);
    assertTrue("expected store user to be eucalyptus, but it was " + retrieved.getStoreUser(), "eucalyptus".equals(retrieved.getStoreUser()));
    assertTrue("expected tid to be 1, but it was " + retrieved.getTid().intValue(), 1 == retrieved.getTid().intValue());

    // make sure chap user was created
    CHAPUserInfo example = new CHAPUserInfo("eucalyptus");
    example.setScName(StorageProperties.NAME);
    try (TransactionResource tran = Entities.transactionFor(CHAPUserInfo.class)) {
      example = Entities.uniqueResult(example);
      tran.commit();
    } catch (Exception ex) {
      fail("exception caught while looking for CHAPUserInfo record - " + ex.getMessage());
      ex.printStackTrace();
    }
    assertTrue("expected chap user info to be non-null", example != null);
    assertTrue("expected eucalyptus chap user info to be created", "eucalyptus".equals(example.getUser()));
  }

  @Test
  public void configure_NoDbUserButYesTGTUserTest() throws Exception {

    final Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
    final TGTService tgtService = context.mock(TGTService.class);
    final BlockStorageUtilSvc blockStorageUtilSvc = context.mock(BlockStorageUtilSvc.class);
    context.checking(new Expectations() {
      {
        oneOf(tgtService).userExists("eucalyptus", timeout);
        will(returnValue(Boolean.TRUE));
        oneOf(tgtService).deleteUser("eucalyptus", timeout);
        oneOf(tgtService).addUser(with(equal("eucalyptus")), with(any(String.class)), with(equal(timeout)));
        oneOf(blockStorageUtilSvc).encryptSCTargetPassword(with(any(String.class)));
        will(returnValue("foo"));
      }
    });

    ISCSIManager iscsiManager = new ISCSIManager(tgtService, blockStorageUtilSvc);
    iscsiManager.configure();

    List<ISCSIMetaInfo> remaining;
    try (TransactionResource tran = Entities.transactionFor(ISCSIMetaInfo.class)) {
      remaining = Entities.query(new ISCSIMetaInfo());
      tran.commit();
    }

    // make sure the meta info was created
    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    ISCSIMetaInfo retrieved = remaining.get(0);
    assertTrue("expected store prefix to be " + StorageProperties.STORE_PREFIX + " but it was " + retrieved.getStorePrefix(),
        StorageProperties.STORE_PREFIX.equals(retrieved.getStorePrefix()));
    assertTrue("expected store number to be 0 but it was " + retrieved.getStoreNumber().intValue(), retrieved.getStoreNumber().intValue() == 0);
    assertTrue("expected store user to be eucalyptus, but it was " + retrieved.getStoreUser(), "eucalyptus".equals(retrieved.getStoreUser()));
    assertTrue("expected tid to be 1, but it was " + retrieved.getTid().intValue(), 1 == retrieved.getTid().intValue());

    // make sure chap user was created
    CHAPUserInfo example = new CHAPUserInfo("eucalyptus");
    example.setScName(StorageProperties.NAME);
    try (TransactionResource tran = Entities.transactionFor(CHAPUserInfo.class)) {
      example = Entities.uniqueResult(example);
      tran.commit();
    } catch (Exception ex) {
      fail("exception caught while looking for CHAPUserInfo record - " + ex.getMessage());
      ex.printStackTrace();
    }
    assertTrue("expected chap user info to be non-null", example != null);
    assertTrue("expected eucalyptus chap user info to be created", "eucalyptus".equals(example.getUser()));
  }

  @Test
  public void configure_YesDbUserButNoTGTUserTest() throws Exception {

    final Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
    final TGTService tgtService = context.mock(TGTService.class);
    final BlockStorageUtilSvc blockStorageUtilSvc = context.mock(BlockStorageUtilSvc.class);
    context.checking(new Expectations() {
      {
        oneOf(tgtService).userExists("eucalyptus", timeout);
        will(returnValue(Boolean.FALSE));
        oneOf(blockStorageUtilSvc).decryptSCTargetPassword("foo");
        will(returnValue("foo"));
        oneOf(tgtService).addUser("eucalyptus", "foo", timeout);
      }
    });

    try (TransactionResource tran = Entities.transactionFor(CHAPUserInfo.class)) {
      Entities.persist(new CHAPUserInfo("eucalyptus", "foo"));
      tran.commit();
    }

    ISCSIManager iscsiManager = new ISCSIManager(tgtService, blockStorageUtilSvc);
    iscsiManager.configure();

    List<ISCSIMetaInfo> remaining;
    try (TransactionResource tran = Entities.transactionFor(ISCSIMetaInfo.class)) {
      remaining = Entities.query(new ISCSIMetaInfo());
      tran.commit();
    }

    // make sure the meta info was created
    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    ISCSIMetaInfo retrieved = remaining.get(0);
    assertTrue("expected store prefix to be " + StorageProperties.STORE_PREFIX + " but it was " + retrieved.getStorePrefix(),
        StorageProperties.STORE_PREFIX.equals(retrieved.getStorePrefix()));
    assertTrue("expected store number to be 0 but it was " + retrieved.getStoreNumber().intValue(), retrieved.getStoreNumber().intValue() == 0);
    assertTrue("expected store user to be eucalyptus, but it was " + retrieved.getStoreUser(), "eucalyptus".equals(retrieved.getStoreUser()));
    assertTrue("expected tid to be 1, but it was " + retrieved.getTid().intValue(), 1 == retrieved.getTid().intValue());

    // make sure chap user was created
    CHAPUserInfo example = new CHAPUserInfo("eucalyptus");
    example.setScName(StorageProperties.NAME);
    try (TransactionResource tran = Entities.transactionFor(CHAPUserInfo.class)) {
      example = Entities.uniqueResult(example);
      tran.commit();
    } catch (Exception ex) {
      fail("exception caught while looking for CHAPUserInfo record - " + ex.getMessage());
      ex.printStackTrace();
    }
    assertTrue("expected chap user info to be non-null", example != null);
    assertTrue("expected eucalyptus chap user info to be created", "eucalyptus".equals(example.getUser()));
  }

  @Test
  public void configure_YesDbUserAndYesTGTUserTest() throws Exception {

    final Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
    final TGTService tgtService = context.mock(TGTService.class);
    final BlockStorageUtilSvc blockStorageUtilSvc = context.mock(BlockStorageUtilSvc.class);
    context.checking(new Expectations() {
      {
        oneOf(tgtService).userExists("eucalyptus", timeout);
        will(returnValue(Boolean.TRUE));
      }
    });

    try (TransactionResource tran = Entities.transactionFor(CHAPUserInfo.class)) {
      Entities.persist(new CHAPUserInfo("eucalyptus", "foo"));
      tran.commit();
    }

    ISCSIManager iscsiManager = new ISCSIManager(tgtService, blockStorageUtilSvc);
    iscsiManager.configure();

    List<ISCSIMetaInfo> remaining;
    try (TransactionResource tran = Entities.transactionFor(ISCSIMetaInfo.class)) {
      remaining = Entities.query(new ISCSIMetaInfo());
      tran.commit();
    }

    // make sure the meta info was created
    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    ISCSIMetaInfo retrieved = remaining.get(0);
    assertTrue("expected store prefix to be " + StorageProperties.STORE_PREFIX + " but it was " + retrieved.getStorePrefix(),
        StorageProperties.STORE_PREFIX.equals(retrieved.getStorePrefix()));
    assertTrue("expected store number to be 0 but it was " + retrieved.getStoreNumber().intValue(), retrieved.getStoreNumber().intValue() == 0);
    assertTrue("expected store user to be eucalyptus, but it was " + retrieved.getStoreUser(), "eucalyptus".equals(retrieved.getStoreUser()));
    assertTrue("expected tid to be 1, but it was " + retrieved.getTid().intValue(), 1 == retrieved.getTid().intValue());

    // make sure chap user was created
    CHAPUserInfo example = new CHAPUserInfo("eucalyptus");
    example.setScName(StorageProperties.NAME);
    try (TransactionResource tran = Entities.transactionFor(CHAPUserInfo.class)) {
      example = Entities.uniqueResult(example);
      tran.commit();
    } catch (Exception ex) {
      fail("exception caught while looking for CHAPUserInfo record - " + ex.getMessage());
      ex.printStackTrace();
    }
    assertTrue("expected chap user info to be non-null", example != null);
    assertTrue("expected eucalyptus chap user info to be created", "eucalyptus".equals(example.getUser()));
  }

  @Test
  public void cleanup_BasicTest() throws Exception {
    final String volId = "vol-0000";
    final ISCSIVolumeInfo iscsiVolumeInfo = new ISCSIVolumeInfo();
    iscsiVolumeInfo.setVolumeId(volId);
    iscsiVolumeInfo.setTid(new Integer(1));
    iscsiVolumeInfo.setVgName("foo");
    iscsiVolumeInfo.setLvName("foo");
    iscsiVolumeInfo.setLun(new Integer(1));

    final TGTService tgtService = context.mock(TGTService.class);
    final BlockStorageUtilSvc blockStorageUtilSvc = context.mock(BlockStorageUtilSvc.class);
    final Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
    context.checking(new Expectations() {
      {
        oneOf(tgtService).targetExists(volId, 1, iscsiVolumeInfo.getAbsoluteLVPath(), timeout);
        will(returnValue(Boolean.TRUE));
        oneOf(tgtService).targetExists(volId, 1, iscsiVolumeInfo.getAbsoluteLVPath(), timeout);
        will(returnValue(Boolean.TRUE));
        oneOf(tgtService).unbindTarget(volId, 1, timeout);
        oneOf(tgtService).deleteLun(volId, 1, 1, timeout);
        oneOf(tgtService).deleteTarget(volId, 1, timeout, false);
        oneOf(tgtService).targetExists(volId, 1, null, timeout);
        will(returnValue(Boolean.FALSE));
        oneOf(tgtService).targetExists(volId, 1, iscsiVolumeInfo.getAbsoluteLVPath(), timeout);
        will(returnValue(Boolean.FALSE));
      }
    });

    try (TransactionResource tran = Entities.transactionFor(ISCSIVolumeInfo.class)) {
      Entities.persist(iscsiVolumeInfo);
      tran.commit();
    }

    ISCSIManager iscsiManager = new ISCSIManager(tgtService, blockStorageUtilSvc);
    iscsiManager.cleanup(iscsiVolumeInfo);

    List<ISCSIVolumeInfo> remaining;
    try (TransactionResource tran = Entities.transactionFor(ISCSIVolumeInfo.class)) {
      remaining = Entities.query(new ISCSIVolumeInfo());
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    ISCSIVolumeInfo retrieved = remaining.get(0);
    assertTrue("expected store name to be null but was " + retrieved.getStoreName(), retrieved.getStoreName() == null);
    assertTrue("expected tid to be -1", retrieved.getTid().intValue() == -1);
    assertTrue("expected iscsiVolumeInfo lun to be -1 but was " + retrieved.getLun().intValue(), retrieved.getLun().intValue() == -1);
  }

  @Test
  public void cleanup_NotExportedTest() throws Exception {
    final String volId = "vol-0000";
    final ISCSIVolumeInfo iscsiVolumeInfo = new ISCSIVolumeInfo();
    iscsiVolumeInfo.setVolumeId(volId);
    iscsiVolumeInfo.setTid(new Integer(1));
    iscsiVolumeInfo.setVgName("foo");
    iscsiVolumeInfo.setLvName("foo");
    iscsiVolumeInfo.setLun(new Integer(1));

    final TGTService tgtService = context.mock(TGTService.class);
    final BlockStorageUtilSvc blockStorageUtilSvc = context.mock(BlockStorageUtilSvc.class);
    final Long timeout = DirectStorageInfo.getStorageInfo().getTimeoutInMillis();
    context.checking(new Expectations() {
      {
        oneOf(tgtService).targetExists(volId, 1, iscsiVolumeInfo.getAbsoluteLVPath(), timeout);
        will(returnValue(Boolean.FALSE));
      }
    });

    try (TransactionResource tran = Entities.transactionFor(ISCSIVolumeInfo.class)) {
      Entities.persist(iscsiVolumeInfo);
      tran.commit();
    }

    ISCSIManager iscsiManager = new ISCSIManager(tgtService, blockStorageUtilSvc);
    iscsiManager.cleanup(iscsiVolumeInfo);

    List<ISCSIVolumeInfo> remaining;
    try (TransactionResource tran = Entities.transactionFor(ISCSIVolumeInfo.class)) {
      remaining = Entities.query(new ISCSIVolumeInfo());
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    ISCSIVolumeInfo retrieved = remaining.get(0);
    assertTrue("expected store name to be null but was " + retrieved.getStoreName(), retrieved.getStoreName() == null);
    assertTrue("expected tid to be -1", retrieved.getTid().intValue() == -1);
    assertTrue("expected iscsiVolumeInfo lun to be -1 but was " + retrieved.getLun().intValue(), retrieved.getLun().intValue() == -1);
  }

  @Test
  public void getEncryptedPassword_BasicTest() throws Exception {

    final TGTService tgtService = context.mock(TGTService.class);
    final BlockStorageUtilSvc blockStorageUtilSvc = context.mock(BlockStorageUtilSvc.class);
    context.checking(new Expectations() {
      {
        oneOf(blockStorageUtilSvc).decryptSCTargetPassword("foo");
        will(returnValue("foo"));
        oneOf(blockStorageUtilSvc).getPartitionForLocalService(Storage.class);
        will(returnValue(null));
        oneOf(blockStorageUtilSvc).encryptNodeTargetPassword("foo", null);
        will(returnValue("foo"));
      }
    });

    try (TransactionResource tran = Entities.transactionFor(CHAPUserInfo.class)) {
      Entities.persist(new CHAPUserInfo("eucalyptus", "foo"));
      tran.commit();
    }

    ISCSIManager iscsiManager = new ISCSIManager(tgtService, blockStorageUtilSvc);
    String foo = iscsiManager.getEncryptedPassword();

    assertTrue("expected result to be 'foo' but was - " + foo, foo != null && "foo".equals(foo));

  }

}
