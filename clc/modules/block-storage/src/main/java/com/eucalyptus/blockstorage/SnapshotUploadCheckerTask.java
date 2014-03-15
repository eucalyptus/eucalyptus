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

package com.eucalyptus.blockstorage;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.blockstorage.entities.SnapshotPart;
import com.eucalyptus.blockstorage.entities.SnapshotPart.SnapshotPartState;
import com.eucalyptus.blockstorage.entities.SnapshotUploadInfo;
import com.eucalyptus.blockstorage.entities.SnapshotUploadInfo.SnapshotUploadState;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.storage.common.CheckerTask;
import com.google.common.collect.Maps;

/**
 * Checker task for cleaning up aborted uploads and removing the expired uploads from the database. Gets initialized in BlockStorageController and runs
 * periodically
 * 
 * @author Swathi Gangisetty
 */
public class SnapshotUploadCheckerTask extends CheckerTask {

	private Logger LOG = Logger.getLogger(SnapshotUploadCheckerTask.class);

	public SnapshotUploadCheckerTask() {
		this.name = SnapshotUploadCheckerTask.class.getName();
	}

	@Override
	public void run() {
		// Clean up aborted uploads
		cleanupAbortedUploads();
		// Remove expired entities
		deleteExpiredUploads();
	}

	private void cleanupAbortedUploads() {
		try (TransactionResource snapTran = Entities.transactionFor(SnapshotUploadInfo.class)) {
			List<SnapshotUploadInfo> snapUploadInfoList = Entities.query(new SnapshotUploadInfo().withState(SnapshotUploadState.aborted));
			for (SnapshotUploadInfo snapUploadInfo : snapUploadInfoList) {
				LOG.debug("Cleaning aborted entity " + snapUploadInfo);
				try (TransactionResource partTran = Entities.transactionFor(SnapshotPart.class)) {
					List<SnapshotPart> parts = Entities.query(new SnapshotPart(snapUploadInfo.getSnapshotId(), snapUploadInfo.getBucketName(), snapUploadInfo
							.getKeyName(), snapUploadInfo.getUploadId()));
					for (SnapshotPart part : parts) {
						if (StringUtils.isNotBlank(part.getFileName())) {
							LOG.debug("Deleting snapshot part from disk: " + part.getFileName());
							if (!Files.deleteIfExists(Paths.get(part.getFileName()))) {
								LOG.warn("Could not delete snapshot part from disk: " + part.getFileName());
							}
						}
						part.setState(SnapshotPartState.cleaned);
					}
					partTran.commit();
				}
				snapUploadInfo.setPurgeTime(System.currentTimeMillis() + SnapshotUploadInfo.PURGE_INTERVAL);
				snapUploadInfo.setState(SnapshotUploadState.cleaned);
			}
			snapTran.commit();
		} catch (Exception e) {
			LOG.debug("Error updating snapshot upload state during clean up" + e);
		}
	}

	private void deleteExpiredUploads() {
		try (TransactionResource snapTran = Entities.transactionFor(SnapshotUploadInfo.class)) {
			Criterion criterion = Restrictions.and(
					Restrictions.or(Restrictions.like("state", SnapshotUploadState.cleaned), Restrictions.like("state", SnapshotUploadState.uploaded)),
					Restrictions.le("purgeTime", System.currentTimeMillis()));
			List<SnapshotUploadInfo> snapshotUploadInfoList = Entities.query(new SnapshotUploadInfo(), Boolean.FALSE, criterion, Collections.EMPTY_MAP);
			for (SnapshotUploadInfo snapUploadInfo : snapshotUploadInfoList) {
				LOG.debug("Deleting expired entity from DB " + snapUploadInfo);
				Map<String, String> parameters = Maps.newHashMap();
				parameters.put("snapshotId", snapUploadInfo.getSnapshotId());
				parameters.put("bucketName", snapUploadInfo.getBucketName());
				parameters.put("keyName", snapUploadInfo.getKeyName());
				if (snapUploadInfo.getUploadId() != null) {
					parameters.put("uploadId", snapUploadInfo.getUploadId());
					Entities.deleteAllMatching(SnapshotPart.class,
							"WHERE snapshot_id = :snapshotId AND bucket_name = :bucketName AND key_name = :keyName AND upload_id = :uploadId", parameters);
				} else {
					Entities.deleteAllMatching(SnapshotPart.class, "WHERE snapshot_id = :snapshotId AND bucket_name = :bucketName AND key_name = :keyName",
							parameters);
				}
				Entities.delete(snapUploadInfo);
			}
			snapTran.commit();
		} catch (Exception e) {
			LOG.debug("Error deleting expired snapshot upload info entities" + e);
		}
	}
}
