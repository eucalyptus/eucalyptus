/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.ejb.Ejb3Configuration;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.euare.persist.entities.AccessKeyEntity;
import com.eucalyptus.auth.euare.persist.entities.AccountEntity;
import com.eucalyptus.auth.euare.persist.entities.CertificateEntity;
import com.eucalyptus.auth.euare.persist.entities.GroupEntity;
import com.eucalyptus.auth.euare.persist.entities.InstanceProfileEntity;
import com.eucalyptus.auth.euare.persist.entities.PolicyEntity;
import com.eucalyptus.auth.euare.persist.entities.RoleEntity;
import com.eucalyptus.auth.euare.persist.entities.ServerCertificateEntity;
import com.eucalyptus.auth.euare.persist.entities.UserEntity;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
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
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.storage.common.CheckerTask;
import com.eucalyptus.util.EucalyptusCloudException;

import edu.ucsb.eucalyptus.msgs.ComponentProperty;

public class BlockStorageUnitTestSupport {
  private static Map<String, List<String>> userMap = new HashMap<>();

  public static void setupBlockStoragePersistenceContext() {
    Properties props = new Properties();
    props.put("hibernate.archive.autodetection", "jar, class, hbm");
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

    Ejb3Configuration config =
        (new Ejb3Configuration()).configure(props).addAnnotatedClass(BlockStorageGlobalConfiguration.class).addAnnotatedClass(CHAPUserInfo.class)
            .addAnnotatedClass(DASInfo.class).addAnnotatedClass(DirectStorageInfo.class).addAnnotatedClass(ISCSIMetaInfo.class)
            .addAnnotatedClass(ISCSIVolumeInfo.class).addAnnotatedClass(SnapshotInfo.class).addAnnotatedClass(SnapshotPart.class)
            .addAnnotatedClass(SnapshotTransferConfiguration.class).addAnnotatedClass(SnapshotUploadInfo.class).addAnnotatedClass(StorageInfo.class)
            .addAnnotatedClass(VolumeExportRecord.class).addAnnotatedClass(VolumeInfo.class).addAnnotatedClass(VolumeToken.class)
            .addAnnotatedClass(SANVolumeInfo.class).addAnnotatedClass(SANInfo.class);

    PersistenceContexts.registerPersistenceContext("eucalyptus_storage", config);
  }

  public static void tearDownBlockStoragePersistenceContext() {
    PersistenceContexts.shutdown();
  }

  public static void setupAuthPersistenceContext() {
    Properties props = new Properties();
    props.put("hibernate.archive.autodetection", "jar, class, hbm");
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

    Ejb3Configuration config =
        (new Ejb3Configuration()).configure( props ).addAnnotatedClass( AccessKeyEntity.class ).addAnnotatedClass( AccountEntity.class )
            .addAnnotatedClass( InstanceProfileEntity.class ).addAnnotatedClass( GroupEntity.class )
            .addAnnotatedClass( PolicyEntity.class ).addAnnotatedClass( UserEntity.class )
            .addAnnotatedClass( RoleEntity.class ).addAnnotatedClass( CertificateEntity.class )
            .addAnnotatedClass( ServerCertificateEntity.class );

    PersistenceContexts.registerPersistenceContext( "eucalyptus_auth", config );
  }

  public static void tearDownAuthPersistenceContext() {
    PersistenceContexts.shutdown();
  }

  /**
   * Create a set of accounts and users for use in test units
   * 
   * @param numAccounts
   * @param usersPerAccount
   * @throws Exception
   */
  public static void initializeAuth(int numAccounts, int usersPerAccount) throws Exception {
    String accountName;
    String userName;
    Account accnt;
    HashMap<String, String> props = null;
    for (int i = 0; i < numAccounts; i++) {
      accountName = "unittestaccount" + i;
      userMap.put(accountName, new ArrayList<String>());
      accnt = Accounts.addAccount(accountName);
      for (int j = 0; j < usersPerAccount; j++) {
        props = new HashMap<>();
        userName = "unittestuser" + j;
        props.put("email", userName + "@unit-test.com");
        User usr = accnt.addUser(userName, "/", true, props);
        userMap.get(accountName).add(usr.getUserId());
      }
    }
  }

  public static Set<String> getTestAccounts() {
    return userMap.keySet();
  }

  public static List<String> getUsersByAccountName(String accountName) {
    return userMap.get(accountName);
  }

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
        return null;
      }

      @Override
      public void upload(StorageResource storageResource) throws SnapshotTransferException {

      }

      @Override
      public void cancelUpload() throws SnapshotTransferException {

      }

      @Override
      public void resumeUpload(StorageResource storageResource) throws SnapshotTransferException {

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
      public void cleanSnapshot(String snapshotId) {

      }

      @Override
      public StorageResource createSnapshot(String volumeId, String snapshotId, String snapshotPointId, Boolean shouldTransferSnapshots)
          throws EucalyptusCloudException {
        return null;
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
      public void deleteSnapshot(String snapshotId) throws EucalyptusCloudException {

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
      public StorageResource prepareSnapshot(String snapshotId, int sizeExpected, long actualSizeInMB) throws EucalyptusCloudException {
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
    };
  }

}
