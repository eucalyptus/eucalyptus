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
@Table(name = "snapshot_part")
@Entity
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class SnapshotPart extends AbstractPersistent {

	public static enum SnapshotPartState {
		creating, created, uploading, uploaded, failed, cleaned
	};

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

	@Column(name = "file_name")
	private String fileName;

	@Column(name = "part_number")
	private Integer partNumber;

	@Column(name = "size")
	private Long size;

	@Column(name = "input_file_read_offset")
	private Long inputFileReadOffset;

	@Column(name = "input_file_bytes_read")
	private Long inputFileBytesRead;

	@Column(name = "is_last")
	private Boolean isLast;

	@Column(name = "state")
	@Enumerated(EnumType.STRING)
	private SnapshotPartState state;

	@Column(name = "etag")
	private String etag;

	public SnapshotPart() {
		this.scName = StorageProperties.NAME;
	}

	public SnapshotPart(String snapshotId, String bucketName, String keyName, String uploadId) {
		this();
		this.snapshotId = snapshotId;
		this.bucketName = bucketName;
		this.keyName = keyName;
		this.uploadId = uploadId;
	}

	public SnapshotPart(String snapshotId, String bucketName, String keyName, String uploadId, Integer partNumber) {
		this();
		this.snapshotId = snapshotId;
		this.bucketName = bucketName;
		this.keyName = keyName;
		this.uploadId = uploadId;
		this.partNumber = partNumber;
	}

	public SnapshotPart(String snapshotId, String bucketName, String keyName, String uploadId, String fileName, Integer partNumber, Long inputFileReadOffset,
			SnapshotPartState state) {
		this();
		this.snapshotId = snapshotId;
		this.bucketName = bucketName;
		this.keyName = keyName;
		this.uploadId = uploadId;
		this.fileName = fileName;
		this.partNumber = partNumber;
		this.inputFileReadOffset = inputFileReadOffset;
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

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public Integer getPartNumber() {
		return partNumber;
	}

	public void setPartNumber(Integer partNumber) {
		this.partNumber = partNumber;
	}

	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	public Long getInputFileReadOffset() {
		return inputFileReadOffset;
	}

	public void setInputFileReadOffset(Long inputFileReadOffset) {
		this.inputFileReadOffset = inputFileReadOffset;
	}

	public Long getInputFileBytesRead() {
		return inputFileBytesRead;
	}

	public void setInputFileBytesRead(Long inputFileBytesRead) {
		this.inputFileBytesRead = inputFileBytesRead;
	}

	public Boolean getIsLast() {
		return isLast;
	}

	public void setIsLast(Boolean isLast) {
		this.isLast = isLast;
	}

	public SnapshotPartState getState() {
		return state;
	}

	public void setState(SnapshotPartState state) {
		this.state = state;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	@Override
	public String toString() {
		return "SnapshotPart [snapshotId=" + snapshotId + ", bucketName=" + bucketName + ", keyName=" + keyName + ", uploadId=" + uploadId + ", partNumber="
				+ partNumber + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((snapshotId == null) ? 0 : snapshotId.hashCode());
		result = prime * result + ((bucketName == null) ? 0 : bucketName.hashCode());
		result = prime * result + ((keyName == null) ? 0 : keyName.hashCode());
		result = prime * result + ((uploadId == null) ? 0 : uploadId.hashCode());
		result = prime * result + ((partNumber == null) ? 0 : partNumber.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SnapshotPart other = (SnapshotPart) obj;
		if (snapshotId == null) {
			if (other.snapshotId != null)
				return false;
		} else if (!snapshotId.equals(other.snapshotId))
			return false;
		if (bucketName == null) {
			if (other.bucketName != null)
				return false;
		} else if (!bucketName.equals(other.bucketName))
			return false;
		if (keyName == null) {
			if (other.keyName != null)
				return false;
		} else if (!keyName.equals(other.keyName))
			return false;
		if (uploadId == null) {
			if (other.uploadId != null)
				return false;
		} else if (!uploadId.equals(other.uploadId))
			return false;
		if (partNumber == null) {
			if (other.partNumber != null)
				return false;
		} else if (!partNumber.equals(other.partNumber))
			return false;
		return true;
	}

	public static SnapshotPart createPart(SnapshotUploadInfo snapUploadInfo, String fileName, Integer partNumber, Long inputFileReadOffset)
			throws EucalyptusCloudException {
		try (TransactionResource transaction = Entities.transactionFor(SnapshotPart.class)) {
			SnapshotPart part = Entities.persist(new SnapshotPart(snapUploadInfo.getSnapshotId(), snapUploadInfo.getBucketName(), snapUploadInfo.getKeyName(),
					snapUploadInfo.getUploadId(), fileName, partNumber, inputFileReadOffset, SnapshotPartState.creating));
			transaction.commit();
			return part;
		} catch (Exception ex) {
			throw new EucalyptusCloudException("Failed to create part entity. snapshotId=" + snapUploadInfo.getSnapshotId() + ", bucket="
					+ snapUploadInfo.getBucketName() + ", key=" + snapUploadInfo.getKeyName() + ", upload ID=" + snapUploadInfo.getUploadId()
					+ ", part number=" + partNumber, ex);

		}
	}

	public SnapshotPart updateStateCreated(String uploadId, Long size, Long inputFileBytesRead, Boolean isLast) throws EucalyptusCloudException {
		try (TransactionResource transaction = Entities.transactionFor(SnapshotPart.class)) {
			SnapshotPart part = Entities.uniqueResult(new SnapshotPart(this.snapshotId, this.bucketName, this.keyName, this.uploadId, this.partNumber));
			part.setUploadId(uploadId);
			part.setSize(size);
			part.setInputFileBytesRead(inputFileBytesRead);
			part.setIsLast(isLast);
			part.setState(SnapshotPartState.created);
			transaction.commit();
			return part;
		} catch (Exception ex) {
			throw new EucalyptusCloudException("Failed to update state for part entity " + this + " to " + SnapshotPartState.created.toString(), ex);
		}
	}

	public SnapshotPart updateStateCreated(Long size, Long inputFileBytesRead, Boolean isLast) throws EucalyptusCloudException {
		try (TransactionResource transaction = Entities.transactionFor(SnapshotPart.class)) {
			SnapshotPart part = Entities.uniqueResult(new SnapshotPart(this.snapshotId, this.bucketName, this.keyName, this.uploadId, this.partNumber));
			part.setSize(size);
			part.setInputFileBytesRead(inputFileBytesRead);
			part.setIsLast(isLast);
			part.setState(SnapshotPartState.created);
			transaction.commit();
			return part;
		} catch (Exception ex) {
			throw new EucalyptusCloudException("Failed to update state for part entity " + this + " to " + SnapshotPartState.created.toString(), ex);
		}
	}

	public SnapshotPart updateStateUploading() throws EucalyptusCloudException {
		try (TransactionResource transaction = Entities.transactionFor(SnapshotPart.class)) {
			SnapshotPart part = Entities.uniqueResult(new SnapshotPart(this.snapshotId, this.bucketName, this.keyName, this.uploadId, this.partNumber));
			part.setState(SnapshotPartState.uploading);
			transaction.commit();
			return part;
		} catch (Exception ex) {
			throw new EucalyptusCloudException("Failed to update state for part entity " + this + " to " + SnapshotPartState.uploading.toString(), ex);
		}
	}

	public SnapshotPart updateStateUploaded(String etag) throws EucalyptusCloudException {
		try (TransactionResource transaction = Entities.transactionFor(SnapshotPart.class)) {
			SnapshotPart part = Entities.uniqueResult(new SnapshotPart(this.snapshotId, this.bucketName, this.keyName, this.uploadId, this.partNumber));
			part.setEtag(etag);
			part.setState(SnapshotPartState.uploaded);
			transaction.commit();
			return part;
		} catch (Exception ex) {
			throw new EucalyptusCloudException("Failed to update state for part entity " + this + " to " + SnapshotPartState.uploaded.toString(), ex);
		}
	}

	public SnapshotPart updateStateFailed() throws EucalyptusCloudException {
		try (TransactionResource transaction = Entities.transactionFor(SnapshotPart.class)) {
			SnapshotPart part = Entities.uniqueResult(new SnapshotPart(this.snapshotId, this.bucketName, this.keyName, this.uploadId, this.partNumber));
			part.setState(SnapshotPartState.failed);
			transaction.commit();
			return part;
		} catch (Exception ex) {
			throw new EucalyptusCloudException("Failed to update state for part entity " + this + " to " + SnapshotPartState.failed.toString(), ex);
		}
	}

}
