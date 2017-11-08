/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.blockstorage.san.common;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.eucalyptus.blockstorage.BlockStorageUnitTestSupport;
import com.eucalyptus.blockstorage.StorageResource;
import com.eucalyptus.blockstorage.san.common.entities.SANInfo;
import com.eucalyptus.blockstorage.san.common.entities.SANVolumeInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.EucalyptusCloudException;

/**
 * Created by wesw on 8/7/14.
 */
public class SANManagerTest {

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
  public void cleanSnapshot_BasicTest() throws Exception {
    final String snapId = "foo";
    final String sanVolId = "bar";
    final String iqn = "foo-iqn";
    final String snapshotPointId = "snap-point-foo";
    SANVolumeInfo volInfo = new SANVolumeInfo(snapId);
    volInfo.setSanVolumeId(sanVolId);
    volInfo.setIqn(iqn);
    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      Entities.persist(volInfo);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        oneOf(sanProvider).deleteSnapshot(sanVolId, iqn, snapshotPointId);
        will(returnValue(Boolean.TRUE));
      }
    });

    SANManager test = new SANManager(sanProvider);
    test.cleanSnapshot(snapId, snapshotPointId);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      List<SANVolumeInfo> results = Entities.query(new SANVolumeInfo());
      assertTrue("expected no SANVolumeInfos to exist", results == null || results.size() == 0);
    }
  }

  @Test
  public void cleanSnapshot_NoVolTest() throws Exception {
    final String snapId = "foo";
    final String snapshotPointId = "snap-point-foo";
    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations());

    SANManager test = new SANManager(sanProvider);
    test.cleanSnapshot(snapId, snapshotPointId);
    assertTrue("expected to reach this statement without an exception", true);
  }

  @Test
  public void cleanVolume_BasicTest() throws Exception {
    final String volId = "foo";
    final String sanVolId = "bar";
    final String iqn = "iqn";

    SANVolumeInfo volInfo = new SANVolumeInfo(volId);
    volInfo.setSanVolumeId(sanVolId);
    volInfo.setIqn(iqn);
    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      Entities.persist(volInfo);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        oneOf(sanProvider).deleteVolume(sanVolId, iqn);
        will(returnValue(Boolean.TRUE));
      }
    });

    SANManager test = new SANManager(sanProvider);
    test.cleanVolume(volId);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      List<SANVolumeInfo> results = Entities.query(new SANVolumeInfo());
      assertTrue("expected no SANVolumeInfos to exist", results == null || results.size() == 0);
    }
  }

  @Test
  public void cleanVolume_NoVolTest() throws Exception {
    final String snapId = "foo";
    final String snapshotPointId = "snap-point-foo";
    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations());

    SANManager test = new SANManager(sanProvider);
    test.cleanSnapshot(snapId, snapshotPointId);
    assertTrue("expected to reach this statement without an exception", true);
  }

  @Test
  public void createVolume_BasicTest() throws Exception {
    SANInfo sanInfo = new SANInfo(StorageProperties.NAME, "foohost", "foouser", "foopassword");
    sanInfo.setResourcePrefix("fooprefix");
    sanInfo.setResourceSuffix("foosuffix");

    try (TransactionResource tran = Entities.transactionFor(SANInfo.class)) {
      Entities.persist(sanInfo);
      tran.commit();
    }

    final String volId = "foo";
    final int volSz = 5;

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        // oneOf(sanProvider).volumeExists(volId); will(returnValue(Boolean.FALSE));
        oneOf(sanProvider).createVolume("fooprefix" + volId + "foosuffix", volSz);
        will(returnValue("foo-iqn"));
      }
    });

    SANManager test = new SANManager(sanProvider);
    test.createVolume(volId, volSz);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      List<SANVolumeInfo> results = Entities.query(new SANVolumeInfo());
      assertTrue("expected one SANVolumeInfo to exist", results != null && results.size() == 1);
      SANVolumeInfo created = results.get(0);
      assertTrue("expected volume id to be " + volId + ", but was " + created.getVolumeId(), volId.equals(created.getVolumeId()));
      assertTrue(
          "expected SAN volume id to " + sanInfo.getResourcePrefix() + volId + sanInfo.getResourceSuffix() + ", but was " + created.getSanVolumeId(),
          created.getSanVolumeId().equals(sanInfo.getResourcePrefix() + volId + sanInfo.getResourceSuffix()));
      assertTrue("expected iqn to be foo-iqn, but was " + created.getIqn(), "foo-iqn".equals(created.getIqn()));
    }
  }

  @Test
  public void createVolume_DbRecExistsTest() throws Exception {
    SANInfo sanInfo = new SANInfo(StorageProperties.NAME, "foohost", "foouser", "foopassword");
    sanInfo.setResourcePrefix("fooprefix");
    sanInfo.setResourceSuffix("foosuffix");

    final String volId = "foo";
    final String iqn = "iqn";
    final SANVolumeInfo existing = new SANVolumeInfo(volId).withSanVolumeId("fooprefix" + volId + "foosuffix");
    existing.setIqn(iqn);

    try (TransactionResource tran = Entities.transactionFor(SANInfo.class)) {
      Entities.persist(sanInfo);
      Entities.persist(existing);
      tran.commit();
    }

    final int volSz = 5;

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        oneOf(sanProvider).volumeExists(existing.getSanVolumeId(), iqn);
        will(returnValue(Boolean.FALSE));
        oneOf(sanProvider).createVolume("fooprefix" + volId + "foosuffix", volSz);
        will(returnValue("foo-iqn"));
      }
    });

    SANManager test = new SANManager(sanProvider);
    test.createVolume(volId, volSz);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      List<SANVolumeInfo> results = Entities.query(new SANVolumeInfo());
      assertTrue("expected one SANVolumeInfo to exist", results != null && results.size() == 1);
      SANVolumeInfo created = results.get(0);
      assertTrue("expected volume id to be " + volId + ", but was " + created.getVolumeId(), volId.equals(created.getVolumeId()));
      assertTrue(
          "expected SAN volume id to " + sanInfo.getResourcePrefix() + volId + sanInfo.getResourceSuffix() + ", but was " + created.getSanVolumeId(),
          created.getSanVolumeId().equals(sanInfo.getResourcePrefix() + volId + sanInfo.getResourceSuffix()));
      assertTrue("expected iqn to be foo-iqn, but was " + created.getIqn(), "foo-iqn".equals(created.getIqn()));
    }
  }

  @Test
  public void deleteSnapshot_BasicTest() throws Exception {
    final String volId = "foo";
    final String iqn = "foo-iqn";
    final String snapshotPointId = "snap-point-foo";

    final SANVolumeInfo existing = new SANVolumeInfo(volId).withSanVolumeId("fooprefix" + volId + "foosuffix");
    existing.setIqn(iqn);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      Entities.persist(existing);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        oneOf(sanProvider).deleteSnapshot(existing.getSanVolumeId(), iqn, snapshotPointId);
        will(returnValue(Boolean.TRUE));
      }
    });

    SANManager test = new SANManager(sanProvider);
    test.deleteSnapshot(volId, snapshotPointId);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      List<SANVolumeInfo> results = Entities.query(new SANVolumeInfo());
      assertTrue("expected no SANVolumeInfos to exist", results == null || results.size() == 0);
    }
  }

  @Test
  public void deleteSnapshot_NoSANSnapshot() throws Exception {
    final String volId = "foo";
    final String iqn = "foo-iqn";
    final String snapshotPointId = "snap-point-foo";
    final SANVolumeInfo existing = new SANVolumeInfo(volId).withSanVolumeId("fooprefix" + volId + "foosuffix");
    existing.setIqn(iqn);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      Entities.persist(existing);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        oneOf(sanProvider).deleteSnapshot(existing.getSanVolumeId(), iqn, snapshotPointId);
        will(returnValue(Boolean.FALSE));
        oneOf(sanProvider).snapshotExists(existing.getSanVolumeId(), iqn);
        will(returnValue(Boolean.FALSE));
      }
    });

    SANManager test = new SANManager(sanProvider);
    test.deleteSnapshot(volId, snapshotPointId);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      List<SANVolumeInfo> results = Entities.query(new SANVolumeInfo());
      assertTrue("expected no SANVolumeInfos to exist", results == null || results.size() == 0);
    }
  }

  @Test
  public void getSnapshotSize_BasicTest() throws Exception {
    final String snapId = "foo";
    final String sanVolId = "bar";
    Integer size = new Integer(5);
    SANVolumeInfo volInfo = new SANVolumeInfo(snapId).withSanVolumeId(sanVolId).withSize(size);
    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      Entities.persist(volInfo);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    SANManager test = new SANManager(sanProvider);
    int sizeReturned = test.getSnapshotSize(snapId);
    assertTrue("expected the size returned, " + sizeReturned + ", to match the size specified, " + size.intValue(), size.intValue() == sizeReturned);
  }

  @Test(expected = EucalyptusCloudException.class)
  public void getSnapshotSize_NoSnapshotTest() throws Exception {
    final SANProvider sanProvider = context.mock(SANProvider.class);
    SANManager test = new SANManager(sanProvider);
    test.getSnapshotSize("foo");
  }

  @Test
  public void deleteVolume_BasicTest() throws Exception {
    final String volId = "foo";
    final SANVolumeInfo existing = new SANVolumeInfo(volId).withSanVolumeId("fooprefix" + volId + "foosuffix");

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      Entities.persist(existing);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        oneOf(sanProvider).deleteVolume(existing.getSanVolumeId(), null);
        will(returnValue(Boolean.TRUE));
      }
    });

    SANManager test = new SANManager(sanProvider);
    test.deleteVolume(volId);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      List<SANVolumeInfo> results = Entities.query(new SANVolumeInfo());
      assertTrue("expected no SANVolumeInfos to exist", results == null || results.size() == 0);
    }
  }

  @Test
  public void finishVolume_BasicTest() throws Exception {
    final String volId = "foo";
    final String sanVolId = "fooprefix" + volId + "foosuffix";
    final String volIqn = "foo-iqn";
    final String lun = "1";
    final SANVolumeInfo existing = new SANVolumeInfo(volId);
    existing.setSanVolumeId(sanVolId);
    existing.setIqn(volIqn + ',' + lun);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      Entities.persist(existing);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        oneOf(sanProvider).disconnectTarget(sanVolId, volIqn, lun);
        oneOf(sanProvider).unexportResource(sanVolId, "sc-foo-iqn");
        oneOf(sanProvider).waitAndComplete(sanVolId, existing.getIqn());
      }
    });

    SANManager test = new SANManager(sanProvider);
    test.finishVolume(volId);

  }

  @Test
  public void finishVolume_NoVolumeTest() throws Exception {
    final SANProvider sanProvider = context.mock(SANProvider.class);
    SANManager test = new SANManager(sanProvider);
    test.finishVolume("foo");
  }

  @Test
  public void getVolumePath_BasicTest() throws Exception {
    final String volId = "foo";
    final String sanVolId = "fooprefix" + volId + "foosuffix";
    final String volIqn = "foo-iqn";
    final SANVolumeInfo existing = new SANVolumeInfo(volId);
    existing.setSanVolumeId(sanVolId);
    existing.setIqn(volIqn);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      Entities.persist(existing);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        oneOf(sanProvider).connectTarget(volIqn, null);
        will(returnValue(new StorageResource(volId, "foopath", StorageResource.Type.FILE) {
          @Override
          public Long getSize() throws Exception {
            return null;
          }

          @Override
          public InputStream getInputStream() throws Exception {
            return null;
          }

          @Override
          public OutputStream getOutputStream() throws Exception {
            return null;
          }

          @Override
          public Boolean isDownloadSynchronous() {
            return Boolean.TRUE;
          }
        }));
      }
    });

    SANManager test = new SANManager(sanProvider);
    String path = test.getVolumePath(volId);
    assertTrue("expected the returned value to be 'foopath', but it was '" + path + "'", "foopath".equals(path));

  }

  @Test(expected = EucalyptusCloudException.class)
  public void getVolumePath_NoVolumeTest() throws Exception {
    final SANProvider sanProvider = context.mock(SANProvider.class);
    SANManager test = new SANManager(sanProvider);
    test.getVolumePath("foo");
  }

  // @Test
  public void importVolume_BasicTest() throws Exception {
    final String volPath = File.createTempFile("blockstoragetest-", ".tmp").getCanonicalPath();
    final String devName = File.createTempFile("blockstoragetest-", ".tmp").getCanonicalPath();
    final String volId = "foo";
    final String sanVolId = "fooprefix" + volId + "foosuffix";
    final String volIqn = "foo-iqn";
    final Integer volSz = new Integer(5);

    SANInfo sanInfo = new SANInfo(StorageProperties.NAME, "foohost", "foouser", "foopassword");
    sanInfo.setResourcePrefix("fooprefix");
    sanInfo.setResourceSuffix("foosuffix");

    try (TransactionResource tran = Entities.transactionFor(SANInfo.class)) {
      Entities.persist(sanInfo);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        oneOf(sanProvider).createVolume(sanVolId, volSz);
        will(returnValue(volIqn));
        oneOf(sanProvider).connectTarget(volIqn, null);
        will(returnValue(new StorageResource(volId, devName, StorageResource.Type.FILE) {
          @Override
          public Long getSize() throws Exception {
            return null;
          }

          @Override
          public InputStream getInputStream() throws Exception {
            return null;
          }

          @Override
          public OutputStream getOutputStream() throws Exception {
            return null;
          }

          @Override
          public Boolean isDownloadSynchronous() {
            return Boolean.TRUE;
          }
        }));
        oneOf(sanProvider).disconnectTarget(sanVolId, volIqn, null);
      }
    });

    SANManager test = new SANManager(sanProvider);
    test.importVolume(volId, volPath, volSz);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      List<SANVolumeInfo> results = Entities.query(new SANVolumeInfo());
      assertTrue("expected one SANVolumeInfo to exist", results != null && results.size() == 1);
      SANVolumeInfo created = results.get(0);
      assertTrue("expected volume id to be " + volId + ", but was " + created.getVolumeId(), volId.equals(created.getVolumeId()));
      assertTrue(
          "expected SAN volume id to " + sanInfo.getResourcePrefix() + volId + sanInfo.getResourceSuffix() + ", but was " + created.getSanVolumeId(),
          created.getSanVolumeId().equals(sanInfo.getResourcePrefix() + volId + sanInfo.getResourceSuffix()));
      assertTrue("expected iqn to be foo-iqn, but was " + created.getIqn(), "foo-iqn".equals(created.getIqn()));
    }

    new File(volPath).delete();
    new File(devName).delete();

  }

  // @Test(expected = EucalyptusCloudException.class)
  public void importVolume_VolumeExistsTest() throws Exception {
    final String volId = "foo";
    final String sanVolId = "fooprefix" + volId + "foosuffix";
    final String volIqn = "foo-iqn";

    final SANVolumeInfo existing = new SANVolumeInfo(volId);
    existing.setSanVolumeId(sanVolId);
    existing.setIqn(volIqn);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      Entities.persist(existing);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    SANManager test = new SANManager(sanProvider);
    test.importVolume(volId, "foo", new Integer(5));
  }

  // @Test
  public void importSnapshot_BasicTest() throws Exception {
    final String snapPath = File.createTempFile("blockstoragetest-", ".tmp").getCanonicalPath();
    final String devName = File.createTempFile("blockstoragetest-", ".tmp").getCanonicalPath();
    final String snapId = "foo";
    final String sanVolId = "fooprefix" + snapId + "foosuffix";
    final String fromVolId = "fromfoo";
    final String volIqn = "foo-iqn";
    final Integer volSz = new Integer(5);

    SANInfo sanInfo = new SANInfo(StorageProperties.NAME, "foohost", "foouser", "foopassword");
    sanInfo.setResourcePrefix("fooprefix");
    sanInfo.setResourceSuffix("foosuffix");

    try (TransactionResource tran = Entities.transactionFor(SANInfo.class)) {
      Entities.persist(sanInfo);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        oneOf(sanProvider).createVolume(sanVolId, volSz);
        will(returnValue(volIqn));
        oneOf(sanProvider).connectTarget(volIqn, null);
        will(returnValue(new StorageResource(snapId, devName, StorageResource.Type.FILE) {
          @Override
          public Long getSize() throws Exception {
            return null;
          }

          @Override
          public InputStream getInputStream() throws Exception {
            return null;
          }

          @Override
          public OutputStream getOutputStream() throws Exception {
            return null;
          }

          @Override
          public Boolean isDownloadSynchronous() {
            return Boolean.TRUE;
          }
        }));
        oneOf(sanProvider).disconnectTarget(sanVolId, volIqn, null);
      }
    });

    SANManager test = new SANManager(sanProvider);
    test.importSnapshot(snapId, fromVolId, snapPath, volSz);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      List<SANVolumeInfo> results = Entities.query(new SANVolumeInfo());
      assertTrue("expected one SANVolumeInfo to exist", results != null && results.size() == 1);
      SANVolumeInfo created = results.get(0);
      assertTrue("expected volume id to be " + snapId + ", but was " + created.getVolumeId(), snapId.equals(created.getVolumeId()));
      assertTrue(
          "expected SAN volume id to " + sanInfo.getResourcePrefix() + snapId + sanInfo.getResourceSuffix() + ", but was " + created.getSanVolumeId(),
          created.getSanVolumeId().equals(sanInfo.getResourcePrefix() + snapId + sanInfo.getResourceSuffix()));
      assertTrue("expected iqn to be foo-iqn, but was " + created.getIqn(), "foo-iqn".equals(created.getIqn()));
    }

    new File(snapPath).delete();
    new File(devName).delete();

  }

  // @Test(expected = EucalyptusCloudException.class)
  public void importSnapshot_VolumeExistsTest() throws Exception {
    final String snapId = "foo";
    final String sanVolId = "fooprefix" + snapId + "foosuffix";
    final String volIqn = "foo-iqn";
    final String fromVolId = "fromfoo";

    final SANVolumeInfo existing = new SANVolumeInfo(snapId);
    existing.setSanVolumeId(sanVolId);
    existing.setIqn(volIqn);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      Entities.persist(existing);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    SANManager test = new SANManager(sanProvider);
    test.importSnapshot(snapId, fromVolId, "foo", new Integer(5));
  }

  @Test
  public void exportVolume_BasicTest() throws Exception {
    final String volId = "foo";
    final String sanVolId = "fooprefix" + volId + "foosuffix";
    final String nodeIqn = "node-iqn";
    final String volIqn = "foo-iqn";
    final SANVolumeInfo existing = new SANVolumeInfo(volId);
    existing.setSanVolumeId(sanVolId);
    existing.setIqn(volIqn);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      Entities.persist(existing);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        oneOf(sanProvider).getProtocol();
        will(returnValue("fooprotocol"));
        oneOf(sanProvider).getProviderName();
        will(returnValue("fooprovider"));
        oneOf(sanProvider).exportResource(sanVolId, nodeIqn, volIqn);
        will(returnValue(new String("1")));
        oneOf(sanProvider).getVolumeConnectionString(volId);
        will(returnValue("fooconnectionstring"));
        oneOf(sanProvider).getAuthType();
        will(returnValue("fooauthtype"));
        oneOf(sanProvider).getOptionalChapUser();
        will(returnValue("foooptchapuser"));
      }
    });

    String expected = "fooprotocol,fooprovider,foooptchapuser,fooauthtype,1,fooconnectionstring";
    SANManager test = new SANManager(sanProvider);
    String result = test.exportVolume(volId, nodeIqn);
    assertTrue("expected result to be '" + expected + "' but was '" + result + "'", expected.equals(result));
  }

  @Test(expected = EucalyptusCloudException.class)
  public void exportVolume_VolumeDoesNotExistTest() throws Exception {
    final SANProvider sanProvider = context.mock(SANProvider.class);
    SANManager test = new SANManager(sanProvider);
    test.exportVolume("foo", "foo-iqn");
  }

  @Test
  public void unexportVolumeFromAll_BasicTest() throws Exception {
    final String volId = "foo";
    final String sanVolId = "fooprefix" + volId + "foosuffix";
    final String volIqn = "foo-iqn";
    final SANVolumeInfo existing = new SANVolumeInfo(volId);
    existing.setSanVolumeId(sanVolId);
    existing.setIqn(volIqn);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      Entities.persist(existing);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        oneOf(sanProvider).unexportResourceFromAll(sanVolId);
      }
    });

    SANManager test = new SANManager(sanProvider);
    test.unexportVolumeFromAll(volId);
  }

  @Test(expected = EucalyptusCloudException.class)
  public void unexportVolumeFromAll_VolumeDoesNotExistTest() throws Exception {
    final SANProvider sanProvider = context.mock(SANProvider.class);
    SANManager test = new SANManager(sanProvider);
    test.unexportVolumeFromAll("foo");
  }

  @Test
  public void unexportVolume_BasicTest() throws Exception {
    final String volId = "foo";
    final String sanVolId = "fooprefix" + volId + "foosuffix";
    final String volIqn = "foo-iqn";
    final SANVolumeInfo existing = new SANVolumeInfo(volId);
    existing.setSanVolumeId(sanVolId);
    existing.setIqn(volIqn);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      Entities.persist(existing);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        oneOf(sanProvider).unexportResource(sanVolId, volIqn);
      }
    });

    SANManager test = new SANManager(sanProvider);
    test.unexportVolume(volId, volIqn);
  }

  @Test(expected = EucalyptusCloudException.class)
  public void unexportVolume_VolumeDoesNotExistTest() throws Exception {
    final SANProvider sanProvider = context.mock(SANProvider.class);
    SANManager test = new SANManager(sanProvider);
    test.unexportVolume("foo", "foo-iqn");
  }

  @Test
  public void getFromBackend_BasicTest() throws Exception {
    final String volId = "foo";
    final SANVolumeInfo existing = new SANVolumeInfo(volId).withSanVolumeId("fooprefix" + volId + "foosuffix");

    try (TransactionResource tran = Entities.transactionFor(SANInfo.class)) {
      Entities.persist(existing);
      tran.commit();
    }

    final int volSz = 5;

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        oneOf(sanProvider).snapshotExists(existing.getSanVolumeId(), null);
        will(returnValue(Boolean.TRUE));
      }
    });

    SANManager test = new SANManager(sanProvider);
    boolean result = test.getFromBackend(volId, volSz);

    assertTrue("expected result to be true", result);
  }

  @Test
  public void getFromBackend_SnapExistsOnOtherPartitionTest() throws Exception {
    final String volId = "foo";
    final SANVolumeInfo existing = new SANVolumeInfo(volId).withSanVolumeId("fooprefix" + volId + "foosuffix");
    existing.setScName("other");
    existing.setIqn("foo-iqn");
    existing.setSize(new Integer(5));
    existing.setSnapshotOf("foo-vol");

    try (TransactionResource tran = Entities.transactionFor(SANInfo.class)) {
      Entities.persist(existing);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        oneOf(sanProvider).snapshotExists(existing.getSanVolumeId(), existing.getIqn());
        will(returnValue(Boolean.TRUE));
      }
    });

    SANManager test = new SANManager(sanProvider);
    boolean result = test.getFromBackend(volId, new Integer(5));

    assertTrue("expected result to be true", result);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      List<SANVolumeInfo> results = Entities.query(new SANVolumeInfo());
      assertTrue("expected one SANVolumeInfo to exist", results != null && results.size() == 1);
    }
  }

  @Test
  public void getFromBackend_NoSnapTest() throws Exception {
    final String volId = "foo";

    final int volSz = 5;

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {

      }
    });

    SANManager test = new SANManager(sanProvider);
    boolean result = test.getFromBackend(volId, volSz);

    assertTrue("expected result to be false", !result);
  }

  @Test
  public void createSnapshotPoint_BasicTest() throws Exception {
    final String volId = "testparentvol";
    final String rezPrefix = "fooprefix";
    final String rezSuffix = "foosuffix";
    final String iqn = "foo-iqn";
    final SANVolumeInfo existing = new SANVolumeInfo(volId).withSanVolumeId(rezPrefix + volId + rezSuffix);
    existing.setIqn(iqn);
    existing.setSize(new Integer(5));
    existing.setSnapshotOf("foo-vol");

    SANInfo sanInfo = new SANInfo(StorageProperties.NAME, "foohost", "foouser", "foopassword");
    sanInfo.setResourcePrefix(rezPrefix);
    sanInfo.setResourceSuffix(rezSuffix);

    try (TransactionResource tran = Entities.transactionFor(SANInfo.class)) {
      Entities.persist(existing);
      Entities.persist(sanInfo);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        oneOf(sanProvider).createSnapshotPoint(existing.getSanVolumeId(), rezPrefix + "foo" + rezSuffix, iqn);
        will(returnValue("result"));
      }
    });

    SANManager test = new SANManager(sanProvider);
    String result = test.createSnapshotPoint(volId, "foo");

    assertTrue("expected result to be 'result', but was '" + result + "'", "result".equals(result));

  }

  @Test(expected = EucalyptusCloudException.class)
  public void createSnapshotPoint_VolumeDoesNotExistTest() throws Exception {
    final String rezPrefix = "fooprefix";
    final String rezSuffix = "foosuffix";
    SANInfo sanInfo = new SANInfo(StorageProperties.NAME, "foohost", "foouser", "foopassword");
    sanInfo.setResourcePrefix(rezPrefix);
    sanInfo.setResourceSuffix(rezSuffix);

    try (TransactionResource tran = Entities.transactionFor(SANInfo.class)) {
      Entities.persist(sanInfo);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    SANManager test = new SANManager(sanProvider);
    test.createSnapshotPoint("fooparent", "foo");
  }

  @Test
  public void cloneVolume_BasicTest() throws Exception {
    final String volId = "testparentvol";
    final String rezPrefix = "fooprefix";
    final String rezSuffix = "foosuffix";
    final SANVolumeInfo existing = new SANVolumeInfo(volId).withSanVolumeId(rezPrefix + volId + rezSuffix);
    existing.setIqn("foo-parent-iqn");
    existing.setSize(new Integer(5));
    existing.setSnapshotOf("foo-vol");

    SANInfo sanInfo = new SANInfo(StorageProperties.NAME, "foohost", "foouser", "foopassword");
    sanInfo.setResourcePrefix(rezPrefix);
    sanInfo.setResourceSuffix(rezSuffix);

    try (TransactionResource tran = Entities.transactionFor(SANInfo.class)) {
      Entities.persist(existing);
      Entities.persist(sanInfo);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        // oneOf(sanProvider).snapshotExists(existing.getSanVolumeId()); will(returnValue(Boolean.FALSE));
        oneOf(sanProvider).cloneVolume(rezPrefix + "foo" + rezSuffix, existing.getSanVolumeId(), existing.getIqn());
        will(returnValue("foo-iqn"));
      }
    });

    SANManager test = new SANManager(sanProvider);
    test.cloneVolume("foo", existing.getVolumeId());

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      List<SANVolumeInfo> results = Entities.query(new SANVolumeInfo());
      assertTrue("expected two SANVolumeInfo to exist", results != null && results.size() == 2);
    }
  }

  @Test
  public void cloneVolume_CloneDbRecExistsTest() throws Exception {
    final String volId = "testparentvol";
    final String rezPrefix = "fooprefix";
    final String rezSuffix = "foosuffix";
    final SANVolumeInfo existing = new SANVolumeInfo(volId).withSanVolumeId(rezPrefix + volId + rezSuffix);
    existing.setIqn("foo-parent-iqn");
    existing.setSize(new Integer(5));
    existing.setSnapshotOf("foo-vol");
    final SANVolumeInfo existingClone = new SANVolumeInfo("testvol").withSanVolumeId(rezPrefix + "testvol" + rezSuffix);
    existingClone.setIqn("foo-iqn");
    existingClone.setSize(new Integer(5));

    SANInfo sanInfo = new SANInfo(StorageProperties.NAME, "foohost", "foouser", "foopassword");
    sanInfo.setResourcePrefix(rezPrefix);
    sanInfo.setResourceSuffix(rezSuffix);

    try (TransactionResource tran = Entities.transactionFor(SANInfo.class)) {
      Entities.persist(existing);
      Entities.persist(existingClone);
      Entities.persist(sanInfo);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        oneOf(sanProvider).snapshotExists(existingClone.getSanVolumeId(), existingClone.getIqn());
        will(returnValue(Boolean.FALSE));
        oneOf(sanProvider).cloneVolume(rezPrefix + "testvol" + rezSuffix, existing.getSanVolumeId(), existing.getIqn());
        will(returnValue("foo-iqn"));
      }
    });

    SANManager test = new SANManager(sanProvider);
    test.cloneVolume("testvol", existing.getVolumeId());

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      List<SANVolumeInfo> results = Entities.query(new SANVolumeInfo());
      assertTrue("expected two SANVolumeInfo to exist", results != null && results.size() == 2);
    }
  }

  @Test
  public void createSnapshot_BasicTest() throws Exception {
    final String volId = "testparentvol";
    final String rezPrefix = "fooprefix";
    final String rezSuffix = "foosuffix";
    final String parentIqn = "foo-parent-iqn";
    final SANVolumeInfo existing = new SANVolumeInfo(volId).withSanVolumeId(rezPrefix + volId + rezSuffix);
    existing.setIqn(parentIqn);
    existing.setSize(new Integer(5));
    existing.setSnapshotOf("foo-vol");

    SANInfo sanInfo = new SANInfo(StorageProperties.NAME, "foohost", "foouser", "foopassword");
    sanInfo.setResourcePrefix(rezPrefix);
    sanInfo.setResourceSuffix(rezSuffix);

    try (TransactionResource tran = Entities.transactionFor(SANInfo.class)) {
      Entities.persist(existing);
      Entities.persist(sanInfo);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        // oneOf(sanProvider).snapshotExists(existing.getSanVolumeId()); will(returnValue(Boolean.FALSE));
        oneOf(sanProvider).createSnapshot(existing.getSanVolumeId(), rezPrefix + "foo" + rezSuffix, "bar");
        will(returnValue("foo-iqn"));
      }
    });

    SANManager test = new SANManager(sanProvider);
    StorageResource result = test.createSnapshot(volId, "foo", "bar", false);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      List<SANVolumeInfo> results = Entities.query(new SANVolumeInfo());
      assertTrue("expected two SANVolumeInfo to exist", results != null && results.size() == 2);
    }
  }

  @Test
  public void createSnapshot_CloneDbRecExistsTest() throws Exception {
    final String volId = "testparentvol";
    final String rezPrefix = "fooprefix";
    final String rezSuffix = "foosuffix";
    final SANVolumeInfo existing = new SANVolumeInfo(volId).withSanVolumeId(rezPrefix + volId + rezSuffix);
    existing.setIqn("foo-parent-iqn");
    existing.setSize(new Integer(5));
    existing.setSnapshotOf("foo-vol");
    final SANVolumeInfo existingClone = new SANVolumeInfo("testvol").withSanVolumeId(rezPrefix + "testvol" + rezSuffix);
    existingClone.setIqn("foo-iqn");
    existingClone.setSize(new Integer(5));

    SANInfo sanInfo = new SANInfo(StorageProperties.NAME, "foohost", "foouser", "foopassword");
    sanInfo.setResourcePrefix(rezPrefix);
    sanInfo.setResourceSuffix(rezSuffix);

    try (TransactionResource tran = Entities.transactionFor(SANInfo.class)) {
      Entities.persist(existing);
      Entities.persist(existingClone);
      Entities.persist(sanInfo);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        oneOf(sanProvider).snapshotExists(existingClone.getSanVolumeId(), existingClone.getIqn());
        will(returnValue(Boolean.FALSE));
        oneOf(sanProvider).createSnapshot(existing.getSanVolumeId(), rezPrefix + "testvol" + rezSuffix, "bar");
        will(returnValue("foo-iqn"));
      }
    });

    SANManager test = new SANManager(sanProvider);
    StorageResource result = test.createSnapshot(volId, "testvol", "bar", false);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      List<SANVolumeInfo> results = Entities.query(new SANVolumeInfo());
      assertTrue("expected two SANVolumeInfo to exist", results != null && results.size() == 2);
    }
  }

  @Test
  public void createVolume_FromSnap_BasicTest() throws Exception {
    final String volId = "testparentvol";
    final String rezPrefix = "fooprefix";
    final String rezSuffix = "foosuffix";
    final Integer snapSz = new Integer(5);
    final SANVolumeInfo existing = new SANVolumeInfo(volId).withSanVolumeId(rezPrefix + volId + rezSuffix);
    existing.setIqn("foo-parent-iqn");
    existing.setSize(snapSz);
    existing.setSnapshotOf("foo-vol");

    SANInfo sanInfo = new SANInfo(StorageProperties.NAME, "foohost", "foouser", "foopassword");
    sanInfo.setResourcePrefix(rezPrefix);
    sanInfo.setResourceSuffix(rezSuffix);

    try (TransactionResource tran = Entities.transactionFor(SANInfo.class)) {
      Entities.persist(existing);
      Entities.persist(sanInfo);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        // oneOf(sanProvider).snapshotExists(existing.getSanVolumeId()); will(returnValue(Boolean.FALSE));
        oneOf(sanProvider).createVolume(rezPrefix + "foo" + rezSuffix, existing.getSanVolumeId(), snapSz.intValue(), snapSz.intValue(),
            existing.getIqn());
        will(returnValue("foo-iqn"));
      }
    });

    SANManager test = new SANManager(sanProvider);
    test.createVolume("foo", volId, snapSz.intValue());

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      List<SANVolumeInfo> results = Entities.query(new SANVolumeInfo());
      assertTrue("expected two SANVolumeInfo to exist", results != null && results.size() == 2);
    }
  }

  @Test
  public void createVolume_FromSnap_CloneDbRecExistsTest() throws Exception {
    final String volId = "testparentvol";
    final String rezPrefix = "fooprefix";
    final String rezSuffix = "foosuffix";
    final Integer snapSz = new Integer(5);
    final SANVolumeInfo existing = new SANVolumeInfo(volId).withSanVolumeId(rezPrefix + volId + rezSuffix);
    existing.setIqn("foo-parent-iqn");
    existing.setSize(new Integer(5));
    existing.setSnapshotOf("foo-vol");
    final SANVolumeInfo existingClone = new SANVolumeInfo("testvol").withSanVolumeId(rezPrefix + "testvol" + rezSuffix);
    existingClone.setIqn("foo-iqn");
    existingClone.setSize(new Integer(5));

    SANInfo sanInfo = new SANInfo(StorageProperties.NAME, "foohost", "foouser", "foopassword");
    sanInfo.setResourcePrefix(rezPrefix);
    sanInfo.setResourceSuffix(rezSuffix);

    try (TransactionResource tran = Entities.transactionFor(SANInfo.class)) {
      Entities.persist(existing);
      Entities.persist(existingClone);
      Entities.persist(sanInfo);
      tran.commit();
    }

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        oneOf(sanProvider).volumeExists(existingClone.getSanVolumeId(), existingClone.getIqn());
        will(returnValue(Boolean.FALSE));
        oneOf(sanProvider).createVolume(rezPrefix + "testvol" + rezSuffix, existingClone.getSanVolumeId(), snapSz.intValue(), snapSz.intValue(),
            existingClone.getIqn());
        will(returnValue("foo-iqn"));
      }
    });

    SANManager test = new SANManager(sanProvider);
    test.createVolume("testvol", existingClone.getVolumeId(), snapSz.intValue());

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      List<SANVolumeInfo> results = Entities.query(new SANVolumeInfo());
      assertTrue("expected two SANVolumeInfo to exist", results != null && results.size() == 2);
    }
  }

  @Test
  public void prepareSnapshot_BasicTest() throws Exception {
    final String volId = "testsnap";
    final String rezPrefix = "fooprefix";
    final String rezSuffix = "foosuffix";
    final Integer snapSz = new Integer(5);
    // final SANVolumeInfo existing = new SANVolumeInfo(volId).withSanVolumeId(rezPrefix + volId + rezSuffix);
    // existing.setIqn("foo-parent-iqn");
    // existing.setSize(snapSz);
    // existing.setSnapshotOf("foo-vol");

    SANInfo sanInfo = new SANInfo(StorageProperties.NAME, "foohost", "foouser", "foopassword");
    sanInfo.setResourcePrefix(rezPrefix);
    sanInfo.setResourceSuffix(rezSuffix);

    try (TransactionResource tran = Entities.transactionFor(SANInfo.class)) {
      // Entities.persist(existing);
      Entities.persist(sanInfo);
      tran.commit();
    }

    StorageProperties.SC_INITIATOR_IQN = "sc-foo-iqn";

    final SANProvider sanProvider = context.mock(SANProvider.class);
    context.checking(new Expectations() {
      {
        oneOf(sanProvider).createSnapshotHolder(rezPrefix + volId + rezSuffix, snapSz * 1024l);
        will(returnValue("foo-iqn"));
        oneOf(sanProvider).exportResource(rezPrefix + volId + rezSuffix, "sc-foo-iqn", "foo-iqn");
        will(returnValue(new String("1")));
        oneOf(sanProvider).connectTarget("foo-iqn", "1");
        will(returnValue(new StorageResource(volId, "foopath", StorageResource.Type.FILE) {
          @Override
          public Long getSize() throws Exception {
            return null;
          }

          @Override
          public InputStream getInputStream() throws Exception {
            return null;
          }

          @Override
          public OutputStream getOutputStream() throws Exception {
            return null;
          }

          @Override
          public Boolean isDownloadSynchronous() {
            return Boolean.TRUE;
          }
        }));
      }
    });

    SANManager test = new SANManager(sanProvider);
    test.prepSnapshotForDownload(volId, snapSz.intValue(), snapSz.intValue() * 1024l);

    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      List<SANVolumeInfo> results = Entities.query(new SANVolumeInfo());
      assertTrue("expected one SANVolumeInfo to exist", results != null && results.size() == 1);
    }
  }
}
