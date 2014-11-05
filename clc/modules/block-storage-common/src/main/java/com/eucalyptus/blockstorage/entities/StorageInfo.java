/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.blockstorage.entities;

import static com.eucalyptus.upgrade.Upgrades.Version.v4_1_0;
import groovy.sql.Sql;

import java.util.concurrent.Callable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableIdentifier;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.upgrade.Upgrades.DatabaseFilters;
import com.eucalyptus.upgrade.Upgrades.PreUpgrade;

@Entity
@PersistenceContext(name = "eucalyptus_storage")
@Table(name = "storage_info")
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@ConfigurableClass(root = "storage", alias = "basic", description = "Basic storage controller configuration.", singleton = false, deferred = true)
public class StorageInfo extends AbstractPersistent {
	private static final Boolean DEFAULT_SHOULD_TRANSFER_SNAPSHOTS = Boolean.TRUE;
	private static final Integer DEFAULT_MAX_SNAP_TRANSFER_RETRIES = 50;
	private static final Integer DEFAULT_SNAPSHOT_PART_SIZE_IN_MB = 100;
	private static final Integer DEFAULT_MAX_SNAPSHOT_PARTS_QUEUE_SIZE = 5;
	private static final Integer DEFAULT_MAX_SNAPSHOT_CONCURRENT_TRANSFERS = 3;
	private static final Integer DEFAULT_SNAPSHOT_TRANSFER_TIMEOUT = 48;
	private static final Integer DEFAULT_READ_BUFFER_SIZE_IN_MB = 1;
	private static final Integer DEFAULT_WRITE_BUFFER_SIZE_IN_MB = 100;

	@Transient
	private static Logger LOG = Logger.getLogger(StorageInfo.class);

	@ConfigurableIdentifier
	@Column(name = "storage_name", unique = true)
	private String name;

	@ConfigurableField(description = "Total disk space reserved for volumes", displayName = "Disk space reserved for volumes")
	@Column(name = "system_storage_volume_size_gb")
	private Integer maxTotalVolumeSizeInGb;

	@ConfigurableField(description = "Max volume size", displayName = "Max volume size")
	@Column(name = "system_storage_max_volume_size_gb")
	private Integer maxVolumeSizeInGB;

	@ConfigurableField(description = "Should transfer snapshots", displayName = "Transfer snapshots to ObjectStorage", type = ConfigurableFieldType.BOOLEAN)
	@Column(name = "system_storage_transfer_snapshots")
	private Boolean shouldTransferSnapshots;

	@ConfigurableField(description = "Maximum retry count for snapshot transfer", displayName = "Max Snaphot Transfer Retries", initial = "50")
	@Column(name = "max_snap_transfer_retries")
	private Integer maxSnapTransferRetries;

	@ConfigurableField(description = "Expiration time for deleted volumes (hours)", displayName = "Deleted Volumes Expiration Time (Hours)")
	@Column(name = "deleted_vol_expiration")
	private Integer deletedVolExpiration;

	@ConfigurableField(description = "Snapshot part size in MB for snapshot transfers using multipart upload. Minimum part size is 5MB", displayName = "Snapshot Part Size", initial = "100", changeListener = MinimumPartSizeChangeListener.class)
	@Column(name = "snapshot_part_size_mb")
	private Integer snapshotPartSizeInMB;

	@ConfigurableField(description = "Maximum number of snapshot parts per snapshot that can be spooled on the disk", displayName = "Maximum Queue Size", initial = "5", changeListener = PositiveIntegerChangeListener.class)
	@Column(name = "max_snapshot_parts_queue_size")
	private Integer maxSnapshotPartsQueueSize;

	@ConfigurableField(description = "Maximum number of snapshots that can be uploaded concurrently", displayName = "Maximum Concurrent Snapshot Uploads", initial = "3", changeListener = PositiveIntegerChangeListener.class)
	@Column(name = "max_concurrent_snapshot_transfers")
	private Integer maxConcurrentSnapshotTransfers;

	@ConfigurableField(description = "Snapshot upload wait time in hours after which the upload will be cancelled", displayName = "Snapshot Upload Timeout", initial = "48", changeListener = PositiveIntegerChangeListener.class)
	@Column(name = "snapshot_transfer_timeout_hours")
	private Integer snapshotTransferTimeoutInHours;

	@ConfigurableField(description = "Buffer size in MB for reading data from snapshot when uploading snapshot to objectstorage gateway", displayName = "Read Buffer Size", initial = "1", changeListener = PositiveIntegerChangeListener.class)
	@Column(name = "read_buffer_size_mb")
	private Integer readBuffferSizeInMB;

	@ConfigurableField(description = "Buffer size in MB for writing data to snapshot when downloading snapshot from objectstorage gateway", displayName = "Write Buffer Size", initial = "100", changeListener = PositiveIntegerChangeListener.class)
	@Column(name = "write_buffer_size_mb")
	private Integer writeBufferSizeInMB;

	public StorageInfo() {
		this.name = StorageProperties.NAME;
	}

	public StorageInfo(final String name) {
		this.name = name;
	}

	public StorageInfo(final String name, final Integer maxTotalVolumeSizeInGb, final Integer maxVolumeSizeInGB, final Boolean shouldTransferSnapshots) {
		this.name = name;
		this.maxTotalVolumeSizeInGb = maxTotalVolumeSizeInGb;
		this.maxVolumeSizeInGB = maxVolumeSizeInGB;
		this.shouldTransferSnapshots = shouldTransferSnapshots;
		this.deletedVolExpiration = StorageProperties.DELETED_VOLUME_EXPIRATION_TIME;
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

	public Integer getMaxSnapTransferRetries() {
		return maxSnapTransferRetries;
	}

	public void setMaxSnapTransferRetries(Integer maxSnapTransferRetries) {
		this.maxSnapTransferRetries = maxSnapTransferRetries;
	}

	public Integer getDeletedVolExpiration() {
		return deletedVolExpiration == null ? StorageProperties.DELETED_VOLUME_EXPIRATION_TIME : deletedVolExpiration;
	}

	public void setDeletedVolExpiration(Integer deletedVolExpiration) {
		this.deletedVolExpiration = deletedVolExpiration;
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

	public Integer getReadBuffferSizeInMB() {
		return readBuffferSizeInMB;
	}

	public void setReadBuffferSizeInMB(Integer readBuffferSizeInMB) {
		this.readBuffferSizeInMB = readBuffferSizeInMB;
	}

	public Integer getWriteBufferSizeInMB() {
		return writeBufferSizeInMB;
	}

	public void setWriteBufferSizeInMB(Integer writeBufferSizeInMB) {
		this.writeBufferSizeInMB = writeBufferSizeInMB;
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
			maxVolumeSizeInGB = StorageProperties.MAX_TOTAL_VOLUME_SIZE;
		}
		if (shouldTransferSnapshots == null) {
			shouldTransferSnapshots = DEFAULT_SHOULD_TRANSFER_SNAPSHOTS;
		}
		if (maxSnapTransferRetries == null) {
			maxSnapTransferRetries = DEFAULT_MAX_SNAP_TRANSFER_RETRIES;
		}
		if (deletedVolExpiration == null) {
			deletedVolExpiration = StorageProperties.DELETED_VOLUME_EXPIRATION_TIME;
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
		if (readBuffferSizeInMB == null) {
			readBuffferSizeInMB = DEFAULT_READ_BUFFER_SIZE_IN_MB;
		}
		if (writeBufferSizeInMB == null) {
			writeBufferSizeInMB = DEFAULT_WRITE_BUFFER_SIZE_IN_MB;
		}
	}

	private static StorageInfo getDefaultInstance() {
		StorageInfo info = new StorageInfo(StorageProperties.NAME);
		info.setMaxTotalVolumeSizeInGb(StorageProperties.MAX_TOTAL_VOLUME_SIZE);
		info.setMaxVolumeSizeInGB(StorageProperties.MAX_VOLUME_SIZE);
		info.setShouldTransferSnapshots(DEFAULT_SHOULD_TRANSFER_SNAPSHOTS);
		info.setDeletedVolExpiration(StorageProperties.DELETED_VOLUME_EXPIRATION_TIME);
		info.setMaxSnapTransferRetries(DEFAULT_MAX_SNAP_TRANSFER_RETRIES);
		info.setSnapshotPartSizeInMB(DEFAULT_SNAPSHOT_PART_SIZE_IN_MB);
		info.setMaxSnapshotPartsQueueSize(DEFAULT_MAX_SNAPSHOT_PARTS_QUEUE_SIZE);
		info.setMaxConcurrentSnapshotTransfers(DEFAULT_MAX_SNAPSHOT_CONCURRENT_TRANSFERS);
		info.setSnapshotTransferTimeoutInHours(DEFAULT_SNAPSHOT_TRANSFER_TIMEOUT);
		info.setReadBuffferSizeInMB(DEFAULT_READ_BUFFER_SIZE_IN_MB);
		info.setWriteBufferSizeInMB(DEFAULT_WRITE_BUFFER_SIZE_IN_MB);
		return info;
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
					throw new ConfigurablePropertyException(t.getFieldName() + " cannot be modified to " + newValue + ". It must be greater than or equal to 5");
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

	@PreUpgrade(since = v4_1_0, value = Storage.class)
	public static class RenameColumns implements Callable<Boolean> {

		private static final Logger LOG = Logger.getLogger(RenameColumns.class);

		@Override
		public Boolean call() throws Exception {

			LOG.info("Renaming columns in table storage_info");
			Sql sql = null;

			try {
				sql = DatabaseFilters.NEWVERSION.getConnection("eucalyptus_storage");
				sql.execute("alter table storage_info rename column max_concurrent_snapshot_uploads to max_concurrent_snapshot_transfers");
				sql.execute("alter table storage_info rename column snapshot_upload_timeout_hours to snapshot_transfer_timeout_hours");

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
}
