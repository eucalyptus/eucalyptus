/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.google.common.base.Functions;

@Entity
@PersistenceContext(name = "eucalyptus_storage")
@Table(name = "snapshot_transfer_configuration")
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class SnapshotTransferConfiguration extends AbstractPersistent {

	private static final String DEFAULT_SINGLETON_ID = "singleton";
	public static final int DEFAULT_BUCKET_CREATION_RETRIES = 10;
	public static final String OSG = "objectstoragegateway";

	@Column(name = "singleton_id")
	private String singletonId;

	@Column(name = "snapshot_bucket")
	private String snapshotBucket;

	public SnapshotTransferConfiguration() {
		super();
		this.singletonId = DEFAULT_SINGLETON_ID;
	}

	public SnapshotTransferConfiguration(String snapshotBucket) {
		this();
		this.snapshotBucket = snapshotBucket;
	}

	public String getSnapshotBucket() {
		return snapshotBucket;
	}

	public void setSnapshotBucket(String snapshotBucket) {
		this.snapshotBucket = snapshotBucket;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((snapshotBucket == null) ? 0 : snapshotBucket.hashCode());
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
		SnapshotTransferConfiguration other = (SnapshotTransferConfiguration) obj;
		if (snapshotBucket == null) {
			if (other.snapshotBucket != null)
				return false;
		} else if (!snapshotBucket.equals(other.snapshotBucket))
			return false;
		return true;
	}

	synchronized public static SnapshotTransferConfiguration getInstance() throws TransactionException {
		return Transactions.one(new SnapshotTransferConfiguration(), Functions.<SnapshotTransferConfiguration> identity());
	}

	synchronized public static SnapshotTransferConfiguration updateBucketName(String snapshotBucket) {
		try (TransactionResource db = Entities.transactionFor(SnapshotTransferConfiguration.class)) {
			SnapshotTransferConfiguration conf = null;
			try {
				conf = Entities.uniqueResult(new SnapshotTransferConfiguration());
				conf.setSnapshotBucket(snapshotBucket);
			} catch (Exception ex) {
				conf = new SnapshotTransferConfiguration(snapshotBucket);
				Entities.persist(conf);
			} finally {
				db.commit();
			}
			return conf;
		}
	}
}
