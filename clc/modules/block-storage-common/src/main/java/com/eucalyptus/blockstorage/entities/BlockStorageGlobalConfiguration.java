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

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.walrus.entities.WalrusInfo;
import com.google.common.base.Predicate;

/**
 * Global configuration information that is common to all SC instances, regardless of cluster or instance
 * 
 */
@Entity
@PersistenceContext(name = "eucalyptus_storage")
@Table(name = "blockstorage_global_configuration")
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@ConfigurableClass(root = "storage", description = "Basic storage controller configuration.", singleton = true, deferred = true)
public class BlockStorageGlobalConfiguration extends AbstractPersistent {
	private static Logger LOG = Logger.getLogger(BlockStorageGlobalConfiguration.class);
	private static final int DEFAULT_GLOBAL_TOTAL_SNAPSHOT_SIZE_GB = 50;

	@ConfigurableField(description = "Maximum total snapshot capacity (GB)", displayName = "Maximum total size allowed for snapshots", initial = "50")
	@Column(name = "global_total_snapshot_size_limit_gb")
	private Integer global_total_snapshot_size_limit_gb;

	public Integer getGlobal_total_snapshot_size_limit_gb() {
		return global_total_snapshot_size_limit_gb;
	}

	public void setGlobal_total_snapshot_size_limit_gb(Integer global_total_snapshot_size_limit_gb) {
		this.global_total_snapshot_size_limit_gb = global_total_snapshot_size_limit_gb;
	}

	public BlockStorageGlobalConfiguration() {
	}

	@PrePersist
	private void initalize() {
		if (this.global_total_snapshot_size_limit_gb == null) {
			this.global_total_snapshot_size_limit_gb = DEFAULT_GLOBAL_TOTAL_SNAPSHOT_SIZE_GB;
		}
	}

	public static BlockStorageGlobalConfiguration getInstance() {
		BlockStorageGlobalConfiguration config = null;
		try {
			config = Transactions.find(new BlockStorageGlobalConfiguration());
		} catch (Exception e) {
			try {
				config = Transactions.save(new BlockStorageGlobalConfiguration());
			} catch (Exception e1) {
				LOG.warn("Failed to load and save block storage global configuration");
				config = new BlockStorageGlobalConfiguration();
				config.initalize();
			}
		}
		return config;
	}

	@EntityUpgrade(entities = { BlockStorageGlobalConfiguration.class }, since = Upgrades.Version.v4_0_0, value = Storage.class)
	public static enum BSGC400Upgrade implements Predicate<Class> { // Set the max snapshot size from Walrus in 3.4.x
		INSATANCE;
		private static final Logger LOG = Logger.getLogger(BlockStorageGlobalConfiguration.BSGC400Upgrade.class);

		@Override
		public boolean apply(@Nullable Class arg0) {
			EntityTransaction tran = Entities.get(BlockStorageGlobalConfiguration.class);
			try {
				WalrusInfo walrusConfig = WalrusInfo.getWalrusInfo();
				BlockStorageGlobalConfiguration config = null;
				try {
					config = Entities.uniqueResult(new BlockStorageGlobalConfiguration());
					config.setGlobal_total_snapshot_size_limit_gb(walrusConfig.getStorageMaxTotalSnapshotSizeInGb());
				} catch (Exception e) {
					config = new BlockStorageGlobalConfiguration();
					config.setGlobal_total_snapshot_size_limit_gb(walrusConfig.getStorageMaxTotalSnapshotSizeInGb());
					Entities.persist(config);
				}
				tran.commit();
			} catch (Exception e) {
				LOG.error("Error upgrading blockstorage global configuration", e);
				tran.rollback();
				Exceptions.toUndeclared("Error upgrading blockstorage global configuration", e);
			} finally {
				if (tran.isActive()) {
					tran.rollback();
				}
			}
			return true;
		}
	}
}
