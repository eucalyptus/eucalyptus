/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.eucalyptus.blockstorage.entities.BlockStorageGlobalConfiguration;
import com.eucalyptus.blockstorage.entities.CHAPUserInfo;
import com.eucalyptus.blockstorage.entities.DASInfo;
import com.eucalyptus.blockstorage.entities.DirectStorageInfo;
import com.eucalyptus.blockstorage.entities.ISCSIMetaInfo;
import com.eucalyptus.blockstorage.entities.ISCSIVolumeInfo;
import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.entities.SnapshotPart;
import com.eucalyptus.blockstorage.entities.SnapshotTransferConfiguration;
import com.eucalyptus.blockstorage.entities.SnapshotUploadInfo;
import com.eucalyptus.blockstorage.entities.StorageInfo;
import com.eucalyptus.blockstorage.entities.VolumeExportRecord;
import com.eucalyptus.blockstorage.entities.VolumeInfo;
import com.eucalyptus.blockstorage.entities.VolumeToken;
import com.eucalyptus.blockstorage.exceptions.SnapshotTransferException;
import com.eucalyptus.blockstorage.san.common.entities.SANInfo;
import com.eucalyptus.blockstorage.san.common.entities.SANVolumeInfo;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.PersistenceContextConfiguration;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.storage.common.CheckerTask;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import edu.ucsb.eucalyptus.msgs.ComponentProperty;

public class BlockStorageUnitTestSupport {
  private static Map<String, List<String>> userMap = new HashMap<>();

  public static void setupBlockStoragePersistenceContext() {
    Map<String, String> props = Maps.newHashMap();
    props.put("hibernate.ejb.interceptor.session_scoped", "com.eucalyptus.entities.DelegatingInterceptor");
    props.put("hibernate.show_sql", "false");
    props.put("hibernate.format_sql", "false");
    props.put("hibernate.generate_statistics", "false");
    props.put("hibernate.bytecode.use_reflection_optimizer", "true");
    props.put("javax.persistence.jdbc.driver", "org.apache.derby.jdbc.EmbeddedDriver");
    props.put("javax.persistence.jdbc.user", "root");
    props.put("javax.persistence.jdbc.password", "root");
    props.put("hibernate.hbm2ddl.auto", "create");
    props.put("hibernate.cache.use_second_level_cache", "false");
    props.put("hibernate.dialect", "org.hibernate.dialect.DerbyDialect");
    props.put("hibernate.connection.url", "jdbc:derby:memory:test;create=true");

    PersistenceContextConfiguration config = new PersistenceContextConfiguration("eucalyptus_storage",
        ImmutableList.<Class<?>>builder().add(BlockStorageGlobalConfiguration.class).add(CHAPUserInfo.class).add(DASInfo.class)
            .add(DirectStorageInfo.class).add(ISCSIMetaInfo.class).add(ISCSIVolumeInfo.class).add(SnapshotInfo.class).add(SnapshotPart.class)
            .add(SnapshotTransferConfiguration.class).add(SnapshotUploadInfo.class).add(StorageInfo.class).add(VolumeExportRecord.class)
            .add(VolumeInfo.class).add(VolumeToken.class).add(SANVolumeInfo.class).add(SANInfo.class).build(),
        props);

    PersistenceContexts.registerPersistenceContext(config);
  }

  public static void tearDownBlockStoragePersistenceContext() {
    PersistenceContexts.shutdown();
  }

  public static void setupAuthPersistenceContext() {}

  public static void tearDownAuthPersistenceContext() {}

  /**
   * Create a set of accounts and users for use in test units
   * 
   * @param numAccounts
   * @param usersPerAccount
   * @throws Exception
   */
  public static void initializeAuth(int numAccounts, int usersPerAccount) throws Exception {}

  public static void flushSnapshotInfos() {
    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      Entities.deleteAll(SnapshotInfo.class);
      tran.commit();
    } catch (Throwable t) {
      throw new RuntimeException("error deleting remaining snapshot infos - " + t.getMessage(), t);
    }
  }

  public static void flushVolumeInfos() {
    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      Entities.deleteAll(VolumeInfo.class);
      tran.commit();
    } catch (Throwable t) {
      throw new RuntimeException("error deleting remaining volume infos - " + t.getMessage(), t);
    }
  }

  public static void flushISCSIMetaInfos() {
    try (TransactionResource tran = Entities.transactionFor(ISCSIMetaInfo.class)) {
      Entities.deleteAll(ISCSIMetaInfo.class);
      tran.commit();
    } catch (Throwable t) {
      throw new RuntimeException("error deleting remaining ISCSIMetaInfos - " + t.getMessage(), t);
    }
  }

  public static void flushCHAPUserInfos() {
    try (TransactionResource tran = Entities.transactionFor(CHAPUserInfo.class)) {
      Entities.deleteAll(CHAPUserInfo.class);
      tran.commit();
    } catch (Throwable t) {
      throw new RuntimeException("error deleting remaining CHAPUserInfos - " + t.getMessage(), t);
    }
  }

  public static void flushSANVolumeInfos() {
    try (TransactionResource tran = Entities.transactionFor(SANVolumeInfo.class)) {
      Entities.deleteAll(SANVolumeInfo.class);
      tran.commit();
    } catch (Throwable t) {
      throw new RuntimeException("error deleting remaining SAN volume infos - " + t.getMessage(), t);
    }
  }

  public static void flushSANInfos() {
    try (TransactionResource tran = Entities.transactionFor(SANInfo.class)) {
      Entities.deleteAll(SANInfo.class);
      tran.commit();
    } catch (Throwable t) {
      throw new RuntimeException("error deleting remaining SANInfos - " + t.getMessage(), t);
    }
  }
  
  public static void flushStorageInfos() {
    try (TransactionResource tran = Entities.transactionFor(StorageInfo.class)) {
      Entities.deleteAll(StorageInfo.class);
      tran.commit();
    } catch (Throwable t) {
      throw new RuntimeException("error deleting remaining StorageInfos - " + t.getMessage(), t);
    }
  }

  public static void flushBlockStorageEntities() {
    flushSnapshotInfos();
    flushVolumeInfos();
    flushCHAPUserInfos();
    flushISCSIMetaInfos();
    flushSANVolumeInfos();
    flushSANInfos();
  }

  public static S3SnapshotTransfer createMockS3SnapshotTransfer() {
    S3SnapshotTransfer mock = new S3SnapshotTransfer(true) {

      @Override
      public String getSnapshotId() {
        return super.getSnapshotId();
      }

      @Override
      public void setSnapshotId(String snapshotId) {
        super.setSnapshotId(snapshotId);
      }

      @Override
      public String getBucketName() {
        return super.getBucketName();
      }

      @Override
      public void setBucketName(String bucketName) {
        super.setBucketName(bucketName);
      }

      @Override
      public String getKeyName() {
        return super.getKeyName();
      }

      @Override
      public void setKeyName(String keyName) {
        super.setKeyName(keyName);
      }

      @Override
      public String getUploadId() {
        return super.getUploadId();
      }

      @Override
      public void setUploadId(String uploadId) {
        super.setUploadId(uploadId);
      }

      @Override
      public String prepareForUpload() throws SnapshotTransferException {
        return "bucket-for-upload";
      }

      @Override
      public Future<String> upload(StorageResource storageResource, SnapshotProgressCallback progressCallback) throws SnapshotTransferException {
        return null;
      }

      @Override
      public void cancelUpload() throws SnapshotTransferException {

      }

      @Override
      public void download(StorageResource storageResource) throws SnapshotTransferException {

      }

      @Override
      public void delete() throws SnapshotTransferException {

      }

      @Override
      public Long getSizeInBytes() throws SnapshotTransferException {
        return super.getSizeInBytes();
      }
    };
    return mock;
  }

  public static LogicalStorageManager createMockLogicalStorageManager() {
    return new LogicalStorageManager() {
      @Override
      public void initialize() throws EucalyptusCloudException {

      }

      @Override
      public void configure() throws EucalyptusCloudException {

      }

      @Override
      public void checkPreconditions() throws EucalyptusCloudException {

      }

      @Override
      public void reload() {

      }

      @Override
      public void startupChecks() throws EucalyptusCloudException {

      }

      @Override
      public void cleanVolume(String volumeId) {
        try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
          VolumeInfo example = new VolumeInfo(volumeId);
          Entities.delete(Entities.uniqueResult(example));
          tran.commit();
        } catch (TransactionException e) {
          System.err.println("exception caught looking for VolumeInfo with volumeId - " + volumeId);
          e.printStackTrace();
        }
      }

      @Override
      public void cleanSnapshot(String snapshotId, String snapshotPointId) {

      }

      @Override
      public void createSnapshot(String volumeId, String snapshotId, String snapshotPointId) throws EucalyptusCloudException {

      }

      @Override
      public List<String> prepareForTransfer(String snapshotId) throws EucalyptusCloudException {
        return null;
      }

      @Override
      public void createVolume(String volumeId, int size) throws EucalyptusCloudException {

      }

      @Override
      public int createVolume(String volumeId, String snapshotId, int size) throws EucalyptusCloudException {
        return 0;
      }

      @Override
      public void cloneVolume(String volumeId, String parentVolumeId) throws EucalyptusCloudException {

      }

      @Override
      public void addSnapshot(String snapshotId) throws EucalyptusCloudException {

      }

      @Override
      public void deleteVolume(String volumeId) throws EucalyptusCloudException {

      }

      @Override
      public void deleteSnapshot(String snapshotId, String snapshotPointId) throws EucalyptusCloudException {

      }

      @Override
      public String getVolumeConnectionString(String volumeId) throws EucalyptusCloudException {
        return null;
      }

      @Override
      public void loadSnapshots(List<String> snapshotSet, List<String> snapshotFileNames) throws EucalyptusCloudException {

      }

      @Override
      public int getSnapshotSize(String snapshotId) throws EucalyptusCloudException {
        return 0;
      }

      @Override
      public void finishVolume(String snapshotId) throws EucalyptusCloudException {

      }

      @Override
      public StorageResourceWithCallback prepSnapshotForDownload(String snapshotId, int sizeExpected, long actualSizeInMB)
          throws EucalyptusCloudException {
        return null;
      }

      @Override
      public ArrayList<ComponentProperty> getStorageProps() {
        return null;
      }

      @Override
      public void setStorageProps(ArrayList<ComponentProperty> storageParams) {

      }

      @Override
      public String getStorageRootDirectory() {
        return null;
      }

      @Override
      public String getVolumePath(String volumeId) throws EucalyptusCloudException {
        return null;
      }

      @Override
      public void importVolume(String volumeId, String volumePath, int size) throws EucalyptusCloudException {

      }

      @Override
      public String getSnapshotPath(String snapshotId) throws EucalyptusCloudException {
        return null;
      }

      @Override
      public void importSnapshot(String snapshotId, String snapPath, String volumeId, int size) throws EucalyptusCloudException {

      }

      @Override
      public String exportVolume(String volumeId, String nodeIqn) throws EucalyptusCloudException {
        return null;
      }

      @Override
      public void unexportVolume(String volumeId, String nodeIqn) throws EucalyptusCloudException, UnsupportedOperationException {

      }

      @Override
      public void unexportVolumeFromAll(String volumeId) throws EucalyptusCloudException {

      }

      @Override
      public String createSnapshotPoint(String parentVolumeId, String volumeId) throws EucalyptusCloudException {
        return null;
      }

      @Override
      public void deleteSnapshotPoint(String parentVolumeId, String volumeId, String snapshotPointId) throws EucalyptusCloudException {

      }

      @Override
      public void checkReady() throws EucalyptusCloudException {

      }

      @Override
      public void stop() throws EucalyptusCloudException {

      }

      @Override
      public void enable() throws EucalyptusCloudException {

      }

      @Override
      public void disable() throws EucalyptusCloudException {

      }

      @Override
      public boolean getFromBackend(String snapshotId, int size) throws EucalyptusCloudException {
        return false;
      }

      @Override
      public void checkVolume(String volumeId) throws EucalyptusCloudException {

      }

      @Override
      public List<CheckerTask> getCheckers() {
        return null;
      }

      @Override
      public boolean supportsIncrementalSnapshots() throws EucalyptusCloudException {
        return false;
      }

      @Override
      public StorageResourceWithCallback prepIncrementalSnapshotForUpload(String volumeId, String snapshotId, String snapPointId,
          String prevSnapshotId, String prevSnapPointId) throws EucalyptusCloudException {
        return null;
      }

      @Override
      public StorageResource prepSnapshotForUpload(String volumeId, String snapshotId, String snapPointId) throws EucalyptusCloudException {
        return null;
      }

      @Override
      public StorageResourceWithCallback prepSnapshotBaseForRestore(String snapshotId, int size, String snapshotPointId)
          throws EucalyptusCloudException {
        return null;
      }

      @Override
      public void restoreSnapshotDelta(String currentSnapId, String prevSnapId, String baseId, StorageResource sr) throws EucalyptusCloudException {}

      @Override
      public void completeSnapshotRestorationFromDeltas(String snapshotId) throws EucalyptusCloudException {}

      @Override
      public <F, T> T executeCallback(Function<F, T> callback, F input) throws EucalyptusCloudException {
        return null;
      }

    };
  }

  public static SnapshotProgressCallback createMockSnapshotProgressCallback() {
    return new MockSnapshotProgressCallback();
  }
}
