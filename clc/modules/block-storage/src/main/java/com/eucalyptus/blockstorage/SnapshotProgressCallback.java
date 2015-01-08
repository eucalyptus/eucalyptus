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

package com.eucalyptus.blockstorage;

import java.io.File;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.exceptions.UnknownFileSizeException;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.storage.common.CallBack;
import com.eucalyptus.system.Threads;

import edu.ucsb.eucalyptus.util.SystemUtil;
import edu.ucsb.eucalyptus.util.SystemUtil.CommandOutput;

/**
 * Callback that updates snapshot upload info in the db after at least N percent upload change. Currently set at 3%
 * 
 * All updates are asynchronous so actual db update may be delayed. Uses a single queue to ensure that updates are in-order.
 * 
 * The finish and fail operations are no-ops.
 * 
 */
public class SnapshotProgressCallback implements CallBack {
	private static Logger LOG = Logger.getLogger(SnapshotProgressCallback.class);
	private static final int PROGRESS_TICK = 3; // Percent between updates

	private String snapshotId;
	private long uploadSize;
	private long bytesTransferred;
	private int lastProgress;
	private ServiceConfiguration scConfig;

	public SnapshotProgressCallback(String snapshotId) {
		this.snapshotId = snapshotId;
		this.uploadSize = 0L;
		this.bytesTransferred = 0L;
		this.scConfig = Components.lookup(Storage.class).getLocalServiceConfiguration();
		this.lastProgress = 1; // to indicate that some amount of snapshot in in progress
		Threads.enqueue(scConfig, SnapshotProgressCallback.class, 1, new ProgressSetter(this.snapshotId, this.lastProgress));
	}
	
	// Set the size before calling update()
	public void setUploadSize(long uploadSize) {
		this.uploadSize = uploadSize;
	}

	@Override
	public void update(final long bytesTransferred) {
		if (this.uploadSize > 0) {
			this.bytesTransferred += bytesTransferred;
			int progress = (int) ((this.bytesTransferred * 100) / uploadSize);
			if (progress >= 100 || (progress - this.lastProgress < PROGRESS_TICK)) {
				// Don't update. Either not enough change or snapshot is 100% complete
				return;
			} else {
				this.lastProgress = progress;
				Threads.enqueue(scConfig, SnapshotProgressCallback.class, 1, new ProgressSetter(this.snapshotId, this.lastProgress));
			}
		}
	}

	@Override
	public void finish() {
		// Nothing to do here
	}

	@Override
	public void failed() {
		// Nothing to do here
	}

	class ProgressSetter implements Callable<Boolean> {
		private String snapshotId;
		private int progress;

		public ProgressSetter(String snapshotId, int progress) {
			this.snapshotId = snapshotId;
			this.progress = progress;
		}

		@Override
		public Boolean call() throws Exception {
			try (TransactionResource db = Entities.transactionFor(SnapshotInfo.class)) {
				SnapshotInfo snap = Entities.uniqueResult(new SnapshotInfo(this.snapshotId));
				StorageProperties.Status snapStatus = StorageProperties.Status.valueOf(snap.getStatus());
				if (StorageProperties.Status.pending.equals(snapStatus) || StorageProperties.Status.creating.equals(snapStatus)) {
					// Only update in 'pending' or 'creating' state.
					snap.setProgress(String.valueOf(this.progress));
				}
				db.commit();
				return true;
			} catch (Exception e) {
				LOG.debug("Could not update snapshot progress in DB for " + snapshotId + " to " + lastProgress + "% due to " + e.getMessage());
				return false;
			}
		}
	}
}
