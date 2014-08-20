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

package com.eucalyptus.blockstorage.ceph.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableIdentifier;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Transactions;

@Entity
@PersistenceContext(name = "eucalyptus_storage")
@Table(name = "ceph_info")
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@ConfigurableClass(root = "storage", alias = "ceph", description = "Basic Ceph configuration", singleton = false, deferred = true)
public class CephInfo extends AbstractPersistent {

	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(CephInfo.class);

	public static final String POOL_IMAGE_DELIMITER = "/";
	public static final String IMAGE_SNAPSHOT_DELIMITER = "@";

	private static final String DEFAULT_CEPH_CONFIG_FILE = "/etc/ceph/ceph.conf";
	private static final String DEFAULT_CEPH_USER = "admin";
	private static final String DEFAULT_POOL = "rbd";

	@ConfigurableIdentifier
	@Column(name = "cluster_name", unique = true)
	private String clusterName;
	@ConfigurableField(description = "Absolute path to Ceph config file. If no path is configured, Eucalyptus will default to /etc/ceph/ceph.conf", displayName = "Ceph Configuration File", initial = "/etc/ceph/ceph.conf")
	@Column(name = "ceph_config_file")
	private String cephConfigFile;
	@ConfigurableField(description = "Ceph username to be employed Eucalyptus operations. If no user is configured, Eucalyptus will default to admin", displayName = "Ceph Username", initial = "admin")
	@Column(name = "ceph_user")
	private String cephUser;
	@ConfigurableField(description = "Ceph storage pools made available to Eucalyptus for EBS operations. Use a CSV list for configuring multiple pools. "
			+ "If no pools is configured, Eucalyptus will default to rbd pool", displayName = "Ceph Pools", initial = "rbd", type = ConfigurableFieldType.KEYVALUE)
	@Column(name = "ceph_pools")
	private String cephPools;

	public CephInfo() {
		this.clusterName = StorageProperties.NAME;
	}

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public String getCephConfigFile() {
		return cephConfigFile;
	}

	public void setCephConfigFile(String cephConfigFile) {
		this.cephConfigFile = cephConfigFile;
	}

	public String getCephUser() {
		return cephUser;
	}

	public void setCephUser(String cephUser) {
		this.cephUser = cephUser;
	}

	public String getCephPools() {
		return cephPools;
	}

	public void setCephPools(String cephPools) {
		this.cephPools = cephPools;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((clusterName == null) ? 0 : clusterName.hashCode());
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
		CephInfo other = (CephInfo) obj;
		if (clusterName == null) {
			if (other.clusterName != null)
				return false;
		} else if (!clusterName.equals(other.clusterName))
			return false;
		return true;
	}

	private static CephInfo generateDefault() {
		CephInfo info = new CephInfo();
		info.setCephConfigFile(DEFAULT_CEPH_CONFIG_FILE);
		info.setCephUser(DEFAULT_CEPH_USER);
		info.setCephPools(DEFAULT_POOL);
		return info;
	}

	public static CephInfo getStorageInfo() {
		CephInfo info = null;

		try {
			info = Transactions.find(new CephInfo());
		} catch (Exception e) {
			LOG.warn("Failed to get Ceph storage info for: " + StorageProperties.NAME + ". Loading defaults.");
			try {
				info = Transactions.saveDirect(generateDefault());
			} catch (Exception e1) {
				try {
					info = Transactions.find(new CephInfo());
				} catch (Exception e2) {
					LOG.warn("Failed to retrieve->persist->retrieve storage info (CephInfo entity)");
				}
			}
		}

		if (info == null) {
			info = generateDefault();
		}

		return info;
	}
}
