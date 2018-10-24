/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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

package com.eucalyptus.blockstorage.entities;

import static com.eucalyptus.upgrade.Upgrades.Version.v4_1_0;
import static com.eucalyptus.upgrade.Upgrades.Version.v4_2_0;
import static com.eucalyptus.upgrade.Upgrades.Version.v4_4_0;
import groovy.sql.GroovyRowResult;
import groovy.sql.Sql;

import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableIdentifier;
import com.eucalyptus.configurable.ConfigurableInit;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.upgrade.Upgrades.DatabaseFilters;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.upgrade.Upgrades.PreUpgrade;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Predicate;

@Entity
@PersistenceContext(name = "eucalyptus_storage")
@Table(name = "storage_info")
@ConfigurableClass(root = "storage", alias = "basic", description = "Basic storage controller configuration.", singleton = false, deferred = true)
public class StorageInfo extends AbstractPersistent {
  private static final String DEFAULT_SHOULD_TRANSFER_SNAPSHOTS_TXT = "true";
  private static final Boolean DEFAULT_SHOULD_TRANSFER_SNAPSHOTS = Boolean.valueOf( DEFAULT_SHOULD_TRANSFER_SNAPSHOTS_TXT );
  private static final Integer DEFAULT_SNAPSHOT_PART_SIZE_IN_MB = 100;
  private static final Integer DEFAULT_MAX_SNAPSHOT_PARTS_QUEUE_SIZE = 5;
  private static final Integer DEFAULT_MAX_SNAPSHOT_CONCURRENT_TRANSFERS = 3;
  private static final Integer DEFAULT_SNAPSHOT_TRANSFER_TIMEOUT = 48;
  private static final Integer DEFAULT_READ_BUFFER_SIZE_IN_MB = 1;
  private static final Integer DEFAULT_WRITE_BUFFER_SIZE_IN_MB = 100;
  private static final Integer DEFAULT_DELETED_VOL_EXPIRATION_TIME = 1440;// minutes
  private static final Integer DEFAULT_DELETED_SNAP_EXPIRATION_TIME = 60;// minutes
  private static final Integer MIN_RESOURCE_EXPIRATION_TIME = 10;// minutes
  private static final String DEFAULT_MAX_CONCURRENT_VOLUMES = "10";
  private static final String DEFAULT_MAX_CONCURRENT_SNAPSHOTS = "3";
  private static final String DEFAULT_MAX_SNAP_DELTAS = "0";

  private static Logger LOG = Logger.getLogger(StorageInfo.class);

  @ConfigurableIdentifier
  @Column(name = "storage_name", unique = true)
  private String name;

  @ConfigurableField(description = "Total disk space reserved for volumes",
      displayName = "Disk space reserved for volumes",
      initialInt = StorageProperties.MAX_TOTAL_VOLUME_SIZE )
  @Column(name = "system_storage_volume_size_gb")
  private Integer maxTotalVolumeSizeInGb;

  @ConfigurableField(description = "Max volume size", displayName = "Max volume size",
      initialInt = StorageProperties.MAX_VOLUME_SIZE )
  @Column(name = "system_storage_max_volume_size_gb")
  private Integer maxVolumeSizeInGB;

  @ConfigurableField(description = "Should transfer snapshots", displayName = "Transfer snapshots to ObjectStorage",
      type = ConfigurableFieldType.BOOLEAN, initial = DEFAULT_SHOULD_TRANSFER_SNAPSHOTS_TXT )
  @Column(name = "system_storage_transfer_snapshots")
  private Boolean shouldTransferSnapshots;

  @ConfigurableField(
      description = "Time interval in minutes after which Storage Controller metadata for volumes that have been physically removed from the block storage backend will be deleted",
      displayName = "Volume Metadata Expiration Time In Minutes", initial = "1440", changeListener = MinimumExpirationTimeChangeListener.class)
  @Column(name = "vol_expiration")
  private Integer volExpiration;

  @ConfigurableField(
      description = "Time interval in minutes after which Storage Controller metadata for snapshots that have been physically removed from the block storage backend will be deleted",
      displayName = "Snapshot Metadata Expiration Time in Minutes", initial = "60", changeListener = MinimumExpirationTimeChangeListener.class)
  @Column(name = "snap_expiration")
  private Integer snapExpiration;

  @ConfigurableField(description = "Snapshot part size in MB for snapshot transfers using multipart upload. Minimum part size is 5MB",
      displayName = "Snapshot Part Size", initial = "100", changeListener = MinimumPartSizeChangeListener.class)
  @Column(name = "snapshot_part_size_mb")
  private Integer snapshotPartSizeInMB;

  @ConfigurableField(description = "Maximum number of snapshot parts per snapshot that can be spooled on the disk",
      displayName = "Maximum Queue Size", initial = "5", changeListener = PositiveIntegerChangeListener.class)
  @Column(name = "max_snapshot_parts_queue_size")
  private Integer maxSnapshotPartsQueueSize;

  @ConfigurableField(description = "Maximum number of snapshots that can be uploaded to or downloaded from objectstorage gateway at a given time",
      displayName = "Maximum Concurrent Snapshot Transfers", initial = "3", changeListener = PositiveIntegerChangeListener.class)
  @Column(name = "max_concurrent_snapshot_transfers")
  private Integer maxConcurrentSnapshotTransfers;

  @ConfigurableField(description = "Snapshot upload wait time in hours after which the upload will be cancelled",
      displayName = "Snapshot Upload Timeout", initial = "48", changeListener = PositiveIntegerChangeListener.class)
  @Column(name = "snapshot_transfer_timeout_hours")
  private Integer snapshotTransferTimeoutInHours;

  @ConfigurableField(description = "Buffer size in MB for reading data from snapshot when uploading snapshot to objectstorage gateway",
      displayName = "Read Buffer Size", initial = "1", changeListener = PositiveIntegerChangeListener.class)
  @Column(name = "read_buffer_size_mb")
  private Integer readBufferSizeInMB;

  @ConfigurableField(description = "Buffer size in MB for writing data to snapshot when downloading snapshot from objectstorage gateway",
      displayName = "Write Buffer Size", initial = "100", changeListener = PositiveIntegerChangeListener.class)
  @Column(name = "write_buffer_size_mb")
  private Integer writeBufferSizeInMB;

  @ConfigurableField(description = "Maximum number of volumes processed on the block storage backend at a given time",
      displayName = "Maximum Concurrent Volumes", initial = DEFAULT_MAX_CONCURRENT_VOLUMES, changeListener = PositiveIntegerChangeListener.class)
  @Column(name = "max_concurrent_volumes_processed")
  private Integer maxConcurrentVolumes;

  @ConfigurableField(description = "Maximum number of snapshots processed on the block storage backend at a given time",
      displayName = "Maximum Concurrent Snapshots", initial = DEFAULT_MAX_CONCURRENT_SNAPSHOTS, changeListener = PositiveIntegerChangeListener.class)
  @Column(name = "max_concurrent_snapshots")
  private Integer maxConcurrentSnapshots;

  @ConfigurableField(
      description = "Maximum number of snapshots deltas allowed before triggering a complete upload of the snapshot content for a given volume",
      displayName = "Maximum Snapshot Deltas", initial = DEFAULT_MAX_SNAP_DELTAS, changeListener = NonNegativeIntegerChangeListener.class)
  @Column(name = "max_snapshot_deltas")
  private Integer maxSnapshotDeltas;

  public StorageInfo() {
    this.name = StorageProperties.NAME;
  }

  public StorageInfo(final String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getMaxTotalVolumeSizeInGb() {
    return maxTotalVolumeSizeInGb;
  }

  public void setMaxTotalVolumeSizeInGb(Integer maxTotalVolumeSizeInGb) {
    this.maxTotalVolumeSizeInGb = maxTotalVolumeSizeInGb;
  }

  public Integer getMaxVolumeSizeInGB() {
    return maxVolumeSizeInGB;
  }

  public void setMaxVolumeSizeInGB(Integer maxVolumeSizeInGB) {
    this.maxVolumeSizeInGB = maxVolumeSizeInGB;
  }

  public Boolean getShouldTransferSnapshots() {
    return shouldTransferSnapshots;
  }

  public void setShouldTransferSnapshots(Boolean shouldTransferSnapshots) {
    this.shouldTransferSnapshots = shouldTransferSnapshots;
  }

  public Integer getVolExpiration() {
    return volExpiration;
  }

  public void setVolExpiration(Integer volExpiration) {
    this.volExpiration = volExpiration;
  }

  public Integer getSnapExpiration() {
    return snapExpiration;
  }

  public void setSnapExpiration(Integer snapExpiration) {
    this.snapExpiration = snapExpiration;
  }

  public Integer getSnapshotPartSizeInMB() {
    return snapshotPartSizeInMB;
  }

  public void setSnapshotPartSizeInMB(Integer snapshotPartSizeInMB) {
    this.snapshotPartSizeInMB = snapshotPartSizeInMB;
  }

  public Integer getMaxSnapshotPartsQueueSize() {
    return maxSnapshotPartsQueueSize;
  }

  public void setMaxSnapshotPartsQueueSize(Integer maxSnapshotPartsQueueSize) {
    this.maxSnapshotPartsQueueSize = maxSnapshotPartsQueueSize;
  }

  public Integer getMaxConcurrentSnapshotTransfers() {
    return maxConcurrentSnapshotTransfers;
  }

  public void setMaxConcurrentSnapshotTransfers(Integer maxConcurrentSnapshotTransfers) {
    this.maxConcurrentSnapshotTransfers = maxConcurrentSnapshotTransfers;
  }

  public Integer getSnapshotTransferTimeoutInHours() {
    return snapshotTransferTimeoutInHours;
  }

  public void setSnapshotTransferTimeoutInHours(Integer snapshotTransferTimeoutInHours) {
    this.snapshotTransferTimeoutInHours = snapshotTransferTimeoutInHours;
  }

  public Integer getReadBufferSizeInMB() {
    return readBufferSizeInMB;
  }

  public void setReadBufferSizeInMB(Integer readBufferSizeInMB) {
    this.readBufferSizeInMB = readBufferSizeInMB;
  }

  public Integer getWriteBufferSizeInMB() {
    return writeBufferSizeInMB;
  }

  public void setWriteBufferSizeInMB(Integer writeBufferSizeInMB) {
    this.writeBufferSizeInMB = writeBufferSizeInMB;
  }

  public Integer getMaxConcurrentVolumes() {
    return maxConcurrentVolumes;
  }

  public void setMaxConcurrentVolumes(Integer maxConcurrentVolumesProcessed) {
    this.maxConcurrentVolumes = maxConcurrentVolumesProcessed;
  }

  public Integer getMaxConcurrentSnapshots() {
    return maxConcurrentSnapshots;
  }

  public void setMaxConcurrentSnapshots(Integer maxConcurrentSnapshotsProcessed) {
    this.maxConcurrentSnapshots = maxConcurrentSnapshotsProcessed;
  }

  public Integer getMaxSnapshotDeltas() {
    return maxSnapshotDeltas;
  }

  public void setMaxSnapshotDeltas(Integer maxSnapshotDeltas) {
    this.maxSnapshotDeltas = maxSnapshotDeltas;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    StorageInfo other = (StorageInfo) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return this.name;
  }

  @PreUpdate
  @PostLoad
  public void setDefaults() {
    if (maxTotalVolumeSizeInGb == null) {
      maxTotalVolumeSizeInGb = StorageProperties.MAX_TOTAL_VOLUME_SIZE;
    }
    if (maxVolumeSizeInGB == null) {
      maxVolumeSizeInGB = StorageProperties.MAX_VOLUME_SIZE;
    }
    if (shouldTransferSnapshots == null) {
      shouldTransferSnapshots = DEFAULT_SHOULD_TRANSFER_SNAPSHOTS;
    }
    if (volExpiration == null) {
      volExpiration = DEFAULT_DELETED_VOL_EXPIRATION_TIME;
    }
    if (snapExpiration == null) {
      snapExpiration = DEFAULT_DELETED_SNAP_EXPIRATION_TIME;
    }
    if (snapshotPartSizeInMB == null) {
      snapshotPartSizeInMB = DEFAULT_SNAPSHOT_PART_SIZE_IN_MB;
    }
    if (maxSnapshotPartsQueueSize == null) {
      maxSnapshotPartsQueueSize = DEFAULT_MAX_SNAPSHOT_PARTS_QUEUE_SIZE;
    }
    if (maxConcurrentSnapshotTransfers == null) {
      maxConcurrentSnapshotTransfers = DEFAULT_MAX_SNAPSHOT_CONCURRENT_TRANSFERS;
    }
    if (snapshotTransferTimeoutInHours == null) {
      snapshotTransferTimeoutInHours = DEFAULT_SNAPSHOT_TRANSFER_TIMEOUT;
    }
    if (readBufferSizeInMB == null) {
      readBufferSizeInMB = DEFAULT_READ_BUFFER_SIZE_IN_MB;
    }
    if (writeBufferSizeInMB == null) {
      writeBufferSizeInMB = DEFAULT_WRITE_BUFFER_SIZE_IN_MB;
    }
    if (maxConcurrentVolumes == null) {
      maxConcurrentVolumes = Integer.valueOf(DEFAULT_MAX_CONCURRENT_VOLUMES);
    }
    if (maxConcurrentSnapshots == null) {
      maxConcurrentSnapshots = Integer.valueOf(DEFAULT_MAX_CONCURRENT_SNAPSHOTS);
    }
    if (maxSnapshotDeltas == null) {
      maxSnapshotDeltas = Integer.valueOf(DEFAULT_MAX_SNAP_DELTAS);
    }
  }

  @ConfigurableInit
  public StorageInfo init( ) {
    setDefaults( );
    return this;
  }

  private static StorageInfo getDefaultInstance() {
    return new StorageInfo( ).init( );
  }

  public static StorageInfo getStorageInfo() {
    StorageInfo conf = null;

    try {
      conf = Transactions.find(new StorageInfo());
    } catch (Exception e) {
      LOG.warn("Storage controller properties for " + StorageProperties.NAME + " not found. Loading defaults.");
      try {
        conf = Transactions.saveDirect(getDefaultInstance());
      } catch (Exception e1) {
        try {
          conf = Transactions.find(new StorageInfo());
        } catch (Exception e2) {
          LOG.warn("Failed to persist and retrieve StorageInfo entity");
        }
      }
    }

    if (conf == null) {
      conf = getDefaultInstance();
    }

    return conf;
  }

  public static class MinimumPartSizeChangeListener implements PropertyChangeListener<Integer> {

    @Override
    public void fireChange(ConfigurableProperty t, Integer newValue) throws ConfigurablePropertyException {
      try {
        if (newValue == null) {
          LOG.error("Invalid value for " + t.getFieldName());
          throw new ConfigurablePropertyException("Invalid value for " + t.getFieldName());
        } else if (newValue.intValue() < 5) {
          LOG.error(t.getFieldName() + " cannot be modified to " + newValue + ". It must be greater than or equal to 5");
          throw new ConfigurablePropertyException(
              t.getFieldName() + " cannot be modified to " + newValue + ". It must be greater than or equal to 5");
        }
      } catch (IllegalArgumentException e) {
        throw new ConfigurablePropertyException("Invalid paths: " + e, e);
      }

    }
  }

  public static class PositiveIntegerChangeListener implements PropertyChangeListener<Integer> {

    @Override
    public void fireChange(ConfigurableProperty t, Integer newValue) throws ConfigurablePropertyException {
      if (newValue == null) {
        LOG.error("Invalid value for " + t.getFieldName());
        throw new ConfigurablePropertyException("Invalid value for " + t.getFieldName());
      } else if (newValue.intValue() <= 0) {
        LOG.error(t.getFieldName() + " cannot be modified to " + newValue + ". It must be an integer greater than 0");
        throw new ConfigurablePropertyException(t.getFieldName() + " cannot be modified to " + newValue + ". It must be an integer greater than 0");
      }
    }
  }

  public static class NonNegativeIntegerChangeListener implements PropertyChangeListener<Integer> {

    @Override
    public void fireChange(ConfigurableProperty t, Integer newValue) throws ConfigurablePropertyException {
      if (newValue == null) {
        LOG.error("Invalid value for " + t.getFieldName());
        throw new ConfigurablePropertyException("Invalid value for " + t.getFieldName());
      } else if (newValue.intValue() < 0) {
        LOG.error(t.getFieldName() + " cannot be modified to " + newValue + ". It must be an integer greater than or equal to 0");
        throw new ConfigurablePropertyException(
            t.getFieldName() + " cannot be modified to " + newValue + ". It must be an integer greater than or equal to 0");
      }
    }
  }

  public static class MinimumExpirationTimeChangeListener implements PropertyChangeListener<Integer> {

    @Override
    public void fireChange(ConfigurableProperty t, Integer newValue) throws ConfigurablePropertyException {
      if (newValue == null) {
        LOG.warn("Invalid value for " + t.getFieldName());
        throw new ConfigurablePropertyException("Invalid value for " + t.getFieldName());
      } else if (newValue.intValue() < MIN_RESOURCE_EXPIRATION_TIME) {
        LOG.warn(t.getFieldName() + " cannot be modified to " + newValue + ". It must be an integer greater than or equal to "
            + MIN_RESOURCE_EXPIRATION_TIME);
        throw new ConfigurablePropertyException(t.getFieldName() + " cannot be modified to " + newValue
            + ". It must be an integer greater than or equal to " + MIN_RESOURCE_EXPIRATION_TIME);
      }
    }
  }

  @PreUpgrade(since = v4_1_0, value = Storage.class)
  public static class RenameColumns implements Callable<Boolean> {

    private static final Logger LOG = Logger.getLogger(RenameColumns.class);

    @Override
    public Boolean call() throws Exception {

      LOG.info("Renaming columns in table storage_info");
      Sql sql = null;

      try {

        sql = DatabaseFilters.NEWVERSION.getConnection("eucalyptus_storage");

        String table = "storage_info";

        // check if the old column exists before renaming it
        String oldName = "max_concurrent_snapshot_uploads";
        String newName = "max_concurrent_snapshot_transfers";
        List<GroovyRowResult> result =
            sql.rows(String.format("select column_name from information_schema.columns where table_name='%s' and column_name='%s'", table, oldName));
        if (result != null && !result.isEmpty()) {
          // drop new column if it exists
          LOG.info("Dropping column if it exists " + newName);
          sql.execute(String.format("alter table %s drop column if exists %s", table, newName));
          // rename the new column
          LOG.info("Renaming column " + oldName + " to " + newName);
          sql.execute(String.format("alter table %s rename column %s to %s", table, oldName, newName));
        } else {
          LOG.debug("Column " + oldName + " not found, nothing to rename");
        }

        // check if the old column exists before renaming it
        oldName = "snapshot_upload_timeout_hours";
        newName = "snapshot_transfer_timeout_hours";
        result =
            sql.rows(String.format("select column_name from information_schema.columns where table_name='%s' and column_name='%s'", table, oldName));
        if (result != null && !result.isEmpty()) {
          // drop new column if it exists
          LOG.info("Dropping column if it exists " + newName);
          sql.execute(String.format("alter table %s drop column if exists %s", table, newName));
          // rename the new column
          LOG.info("Renaming column " + oldName + " to " + newName);
          sql.execute(String.format("alter table %s rename column %s to %s", table, oldName, newName));
        } else {
          LOG.debug("Column " + oldName + " not found, nothing to rename");
        }

        return Boolean.TRUE;
      } catch (Exception e) {
        LOG.warn("Failed to rename columns in table storage_info", e);
        return Boolean.TRUE;
      } finally {
        if (sql != null) {
          sql.close();
        }
      }
    }
  }

  @PreUpgrade(since = v4_2_0, value = Storage.class)
  public static class RenameColumnDeletedVolExpiration implements Callable<Boolean> {

    private static final Logger LOG = Logger.getLogger(RenameColumns.class);

    @Override
    public Boolean call() throws Exception {

      LOG.info("Renaming column deleted_vol_expiration to vol_expiration in table storage_info");
      Sql sql = null;

      try {

        sql = DatabaseFilters.NEWVERSION.getConnection("eucalyptus_storage");

        String table = "storage_info";

        // check if the old column exists before renaming it
        String oldName = "deleted_vol_expiration";
        String newName = "vol_expiration";
        List<GroovyRowResult> result =
            sql.rows(String.format("select column_name from information_schema.columns where table_name='%s' and column_name='%s'", table, oldName));
        if (result != null && !result.isEmpty()) {
          // drop new column if it exists
          LOG.info("Dropping column if it exists " + newName);
          sql.execute(String.format("alter table %s drop column if exists %s", table, newName));
          // rename the new column
          LOG.info("Renaming column " + oldName + " to " + newName);
          sql.execute(String.format("alter table %s rename column %s to %s", table, oldName, newName));
        } else {
          LOG.debug("Column " + oldName + " not found, nothing to rename");
        }

        return Boolean.TRUE;
      } catch (Exception e) {
        LOG.warn("Failed to rename column deleted_vol_expiration to vol_expiration in table storage_info", e);
        return Boolean.TRUE;
      } finally {
        if (sql != null) {
          sql.close();
        }
      }
    }
  }

  @EntityUpgrade(entities = {StorageInfo.class}, since = v4_2_0, value = Storage.class)
  public static enum ConvertExpirationTime implements Predicate<Class> {
    INSTANCE;
    private static final Logger LOG = Logger.getLogger(StorageInfo.ConvertExpirationTime.class);

    @Override
    public boolean apply(@Nullable Class arg0) {
      LOG.info("Entity upgrade for StorageInfo entities - converting deletionTime column from hours to minutes");
      try (TransactionResource tr = Entities.transactionFor(StorageInfo.class)) {
        List<StorageInfo> storageInfoList = Entities.query(new StorageInfo(null));
        if (storageInfoList != null && !storageInfoList.isEmpty()) {
          for (StorageInfo storageInfo : storageInfoList) {
            if (storageInfo.getVolExpiration() != null) {
              storageInfo.setVolExpiration(storageInfo.getVolExpiration() * 1440);
            }
          }
        }
        tr.commit();
      } catch (Exception e) {
        LOG.warn("Failed to perform entity upgrade for StorageInfo entities", e);
        Exceptions.toUndeclared("Failed to perform entity upgrade for StorageInfo entities", e);
      }
      return true;
    }
  }

  @PreUpgrade(since = v4_4_0, value = Storage.class)
  public static class RemoveMaxSnapTransferRetries implements Callable<Boolean> {

    private static final Logger LOG = Logger.getLogger(RemoveMaxSnapTransferRetries.class);

    @Override
    public Boolean call() throws Exception {
      Sql sql = null;
      try {
        sql = DatabaseFilters.NEWVERSION.getConnection("eucalyptus_storage");
        String table = "storage_info";
        // check if the old column exists before renaming it
        String column = "max_snap_transfer_retries";
        List<GroovyRowResult> result =
            sql.rows(String.format("select column_name from information_schema.columns where table_name='%s' and column_name='%s'", table, column));
        if (result != null && !result.isEmpty()) {
          // drop column if it exists
          LOG.info("Dropping column if it exists " + column);
          sql.execute(String.format("alter table %s drop column if exists %s", table, column));
        } else {
          LOG.debug("Column " + column + " not found, nothing to drop");
        }
        return Boolean.TRUE;
      } catch (Exception e) {
        LOG.warn("Failed to drop columns in table storage_info", e);
        return Boolean.TRUE;
      } finally {
        if (sql != null) {
          sql.close();
        }
      }
    }
  }
}
