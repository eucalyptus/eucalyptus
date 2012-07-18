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
 ************************************************************************/

package edu.ucsb.eucalyptus.cloud.entities;

import javax.persistence.Column;
import org.hibernate.annotations.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableIdentifier;

@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_storage")
@Table( name = "ISCSIMetadata" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@ConfigurableClass(root = "storage", description = "Storage controller ISCSI meta info", singleton=false, deferred = true)
public class ISCSIMetaInfo extends LVMMetaInfo {
	@ConfigurableIdentifier
	@Column(name = "hostname")
	private String hostName;
	@ConfigurableField( description = "Prefix for ISCSI device", displayName = "ISCSI Prefix", type = ConfigurableFieldType.PRIVATE)
	@Column(name = "store_prefix")
	private String storePrefix;
	@Column(name = "store_number")
	private Integer storeNumber;
	@ConfigurableField( description = "Next Target ID for ISCSI device", displayName = "Next Target ID", type = ConfigurableFieldType.PRIVATE)
	@Column(name = "tid")
	private Integer tid;
	@Column(name = "store_user")
	private String storeUser;

	public ISCSIMetaInfo() {}

	public ISCSIMetaInfo(String hostName) {
		this.hostName = hostName;
	}

	public String getStorePrefix() {
		return storePrefix;
	}

	public void setStorePrefix(String store_prefix) {
		this.storePrefix = store_prefix;
	}

	public Integer getStoreNumber() {
		return storeNumber;
	}

	public void setStoreNumber(Integer storeNumber) {
		this.storeNumber = storeNumber;
	}

	public Integer getTid() {
		return tid;
	}

	public void setTid(Integer tid) {
		this.tid = tid;
	}

	public String getStoreUser() {
		return storeUser;
	}

	public void setStoreUser(String storeUser) {
		this.storeUser = storeUser;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
}
