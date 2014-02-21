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

import java.util.concurrent.Callable;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.storage.common.CallBack;
import com.eucalyptus.system.Threads;


/**
 * Callback that updates snapshot upload info in the db after
 * at least N percent upload change. Currently set at 3%
 * 
 * All updates are asynchronous so actual db update may be delayed.
 * Uses a single queue to ensure that updates are in-order.
 * 
 * The finish and fail operations are synchronous.
 *
 */
public class SnapshotProgressCallback implements CallBack{
	private static Logger LOG = Logger.getLogger(SnapshotProgressCallback.class);
	private String snapshotId;
	private static final int PROGRESS_TICK = 3; //Percent between updates
	private ServiceConfiguration myConfig;
	private long uploadSize;
	private int lastProgress;

	public SnapshotProgressCallback(String snapshotId, long size, int chunkSize) {
		this.snapshotId = snapshotId;
		uploadSize = size;
		myConfig = Components.lookup(Storage.class).getLocalServiceConfiguration();
	}

	public void update(final long bytesSoFar) {
		final int progress = (int)((bytesSoFar * 100) / uploadSize);
		if(progress - lastProgress < PROGRESS_TICK) {
			//Don't update, not enough change.			
			return;
		} else {
			lastProgress = progress;
			LOG.debug("Queueing snap update for progress: " + String.valueOf(progress));
			Threads.enqueue(myConfig, 1, new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					LOG.debug("Executing snap update for progress: " + String.valueOf(progress));
					
					EntityTransaction db = Entities.get(SnapshotInfo.class);		
					SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
					try {
						SnapshotInfo foundSnapshotInfo = Entities.uniqueResult(snapshotInfo);
						StorageProperties.Status snapStatus = StorageProperties.Status.valueOf(foundSnapshotInfo.getStatus());						

						if(StorageProperties.Status.pending.equals(snapStatus) ||
								StorageProperties.Status.creating.equals(snapStatus)) {
							//Only update in 'pending' or 'creating' state.
							foundSnapshotInfo.setProgress(String.valueOf(progress));
						}
						db.commit();
						return true;
					} catch (Exception ex) {
						failed();
						LOG.error("Error updating snapshot upload progress in DB.", ex);
						return false;
					} finally {
						if(db != null && db.isActive()) {
						db.rollback();			
						}
					}
				}
			});
		}		
	}

	public void finish() {
		EntityTransaction db = Entities.get(SnapshotInfo.class);		
		SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
		try {
			SnapshotInfo foundSnapshotInfo = Entities.uniqueResult(snapshotInfo);
			foundSnapshotInfo.setProgress("100");					
			foundSnapshotInfo.setShouldTransfer(false);
			db.commit();
		} catch (Exception ex) {			
			failed();
			LOG.error("Error updating snapshot upload progress in DB.", ex);
		} finally {
			if(db != null && db.isActive()) {
						db.rollback();			
			}
		}
	}

	public void failed() {
		EntityTransaction db = Entities.get(SnapshotInfo.class);		
		SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
		try {
			SnapshotInfo foundSnapshotInfo = Entities.uniqueResult(snapshotInfo);
			foundSnapshotInfo.setProgress("0");
			db.commit();			
		} catch (Exception ex) {			
			LOG.error("Error updating snapshot upload progress in DB.", ex);			
		} finally {
			if(db != null && db.isActive()) {
				db.rollback();			
			}
		}
	}
}
