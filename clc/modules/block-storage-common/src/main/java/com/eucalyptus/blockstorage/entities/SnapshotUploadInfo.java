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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.EucalyptusCloudException;

@PersistenceContext(name = "eucalyptus_storage")
@Table(name = "snapshot_upload_info")
@Entity
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class SnapshotUploadInfo extends AbstractPersistent {

	public static final Long PURGE_INTERVAL = (long) (60 * 60 * 1000); // one hour

	public static enum SnapshotUploadState {
		creatingparts, createdparts, uploaded, aborted, cleaned
	}

	@Column(name = "sc_name")
	private String scName;

	@Column(name = "snapshot_id")
	private String snapshotId;

	@Column(name = "bucket_name")
	private String bucketName;

	@Column(name = "key_name")
	private String keyName;

	@Column(name = "upload_id")
	private String uploadId;

	@Column(name = "state")
	@Enumerated(EnumType.STRING)
	private SnapshotUploadState state;

	@Column(name = "total_parts")
	private Integer totalParts;

	@Column(name = "etag")
	private String etag;

	@Column(name = "purge_time")
	private Long purgeTime;

	public SnapshotUploadInfo() {
		this.scName = StorageProperties.NAME;
	}

	public SnapshotUploadInfo(String snapshotId, String bucketName, String keyName) {
		this();
		this.snapshotId = snapshotId;
		this.bucketName = bucketName;
		this.keyName = keyName;
	}

	public SnapshotUploadInfo(String snapshotId, String bucketName, String keyName, String uploadId) {
		this();
		this.snapshotId = snapshotId;
		this.bucketName = bucketName;
		this.keyName = keyName;
		this.uploadId = uploadId;
	}

	public SnapshotUploadInfo(String snapshotId, String bucketName, String keyName, SnapshotUploadState state) {
		this();
		this.snapshotId = snapshotId;
		this.bucketName = bucketName;
		this.keyName = keyName;
		this.state = state;
	}

	public String getScName() {
		return scName;
	}

	public void setScName(String scName) {
		this.scName = scName;
	}

	public String getSnapshotId() {
		return snapshotId;
	}

	public void setSnapshotId(String snapshotId) {
		this.snapshotId = snapshotId;
	}

	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getKeyName() {
		return keyName;
	}

	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}

	public String getUploadId() {
		return uploadId;
	}

	public void setUploadId(String uploadId) {
		this.uploadId = uploadId;
	}

	public SnapshotUploadState getState() {
		return state;
	}

	public void setState(SnapshotUploadState state) {
		this.state = state;
	}

	public SnapshotUploadInfo withState(SnapshotUploadState state) {
		this.state = state;
		return this;
	}

	public Integer getTotalParts() {
		return totalParts;
	}

	public void setTotalParts(Integer totalParts) {
		this.totalParts = totalParts;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public Long getPurgeTime() {
		return purgeTime;
	}

	public void setPurgeTime(Long purgeTime) {
		this.purgeTime = purgeTime;
	}

	@Override
	public String toString() {
		return "SnapshotMpuInfo [snapshotId=" + snapshotId + ", bucketName=" + bucketName + ", keyName=" + keyName + ", uploadId=" + uploadId + "]";
	}

	public static SnapshotUploadInfo create(String snapshotId, String bucketName, String keyName) throws EucalyptusCloudException {
		try (TransactionResource transaction = Entities.transactionFor(SnapshotUploadInfo.class)) {
			SnapshotUploadInfo snapUploadInfo = Entities.persist(new SnapshotUploadInfo(snapshotId, bucketName, keyName, SnapshotUploadState.creatingparts));
			transaction.commit();
			return snapUploadInfo;
		} catch (Exception ex) {
			throw new EucalyptusCloudException("Failed to create snapshot upload info enity. snapshot Id=" + snapshotId + ", bucket=" + bucketName + ", key="
					+ keyName, ex);
		}
	}

	public SnapshotUploadInfo updateUploadId(String uploadId) throws EucalyptusCloudException {
		try (TransactionResource transaction = Entities.transactionFor(SnapshotUploadInfo.class)) {
			SnapshotUploadInfo snapUploadInfo = Entities.uniqueResult(new SnapshotUploadInfo(this.snapshotId, this.bucketName, this.keyName));
			snapUploadInfo.setUploadId(uploadId);
			transaction.commit();
			return snapUploadInfo;
		} catch (Exception ex) {
			throw new EucalyptusCloudException("Failed to update upload ID for snapshot upload info enity " + this, ex);
		}
	}

	public SnapshotUploadInfo updateStateCreatedParts(Integer totalParts) throws EucalyptusCloudException {
		try (TransactionResource transaction = Entities.transactionFor(SnapshotUploadInfo.class)) {
			SnapshotUploadInfo snapUploadInfo = Entities.uniqueResult(new SnapshotUploadInfo(this.snapshotId, this.bucketName, this.keyName, this.uploadId));
			snapUploadInfo.setTotalParts(totalParts);
			snapUploadInfo.setState(SnapshotUploadState.createdparts);
			transaction.commit();
			return snapUploadInfo;
		} catch (Exception ex) {
			throw new EucalyptusCloudException("Failed to update state for snapshot upload info enity " + this + " to "
					+ SnapshotUploadState.createdparts.toString() + this, ex);
		}
	}

	public SnapshotUploadInfo updateStateUploaded(String etag) throws EucalyptusCloudException {
		try (TransactionResource transaction = Entities.transactionFor(SnapshotUploadInfo.class)) {
			SnapshotUploadInfo snapUploadInfo = Entities.uniqueResult(new SnapshotUploadInfo(this.snapshotId, this.bucketName, this.keyName, this.uploadId));
			snapUploadInfo.setState(SnapshotUploadState.uploaded);
			snapUploadInfo.setEtag(etag);
			snapUploadInfo.setPurgeTime(System.currentTimeMillis() + PURGE_INTERVAL);
			transaction.commit();
			return snapUploadInfo;
		} catch (Exception ex) {
			throw new EucalyptusCloudException("Failed to update state for snapshot upload info enity " + this + " to "
					+ SnapshotUploadState.uploaded.toString() + this, ex);
		}
	}

	public SnapshotUploadInfo updateStateAborted() throws EucalyptusCloudException {
		try (TransactionResource transaction = Entities.transactionFor(SnapshotUploadInfo.class)) {
			SnapshotUploadInfo snapUploadInfo = Entities.uniqueResult(new SnapshotUploadInfo(this.snapshotId, this.bucketName, this.keyName, this.uploadId));
			snapUploadInfo.setState(SnapshotUploadState.aborted);
			transaction.commit();
			return snapUploadInfo;
		} catch (Exception ex) {
			throw new EucalyptusCloudException("Failed to update state for snapshot upload info enity " + this + " to "
					+ SnapshotUploadState.aborted.toString() + this, ex);
		}
	}
}
