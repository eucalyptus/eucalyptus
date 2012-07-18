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

@PersistenceContext(name="eucalyptus_storage")
@Table( name = "AOEMetaInfo" )
@Entity @javax.persistence.Entity
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@ConfigurableClass(root = "storage", description = "Storage controller AOE meta info", singleton=false, deferred = true)
public class AOEMetaInfo extends LVMMetaInfo {
	@ConfigurableIdentifier
	@Column(name = "hostname")
	private String hostName;
	@ConfigurableField( description = "AOE Major Number", displayName = "AOE Major Number", type = ConfigurableFieldType.PRIVATE)
	@Column(name = "major_number")
	private Integer majorNumber;
	@ConfigurableField( description = "AOE Minor Number", displayName = "AOE Minor Number", type = ConfigurableFieldType.PRIVATE)
	@Column(name = "minor_number")
	private Integer minorNumber;

	public AOEMetaInfo() {}

	public AOEMetaInfo(String hostName) {
		this.hostName = hostName;
	}

	public Integer getMajorNumber() {
		return majorNumber;
	}

	public void setMajorNumber(Integer majorNumber) {
		this.majorNumber = majorNumber;
	}

	public Integer getMinorNumber() {
		return minorNumber;
	}

	public void setMinorNumber(Integer minorNumber) {
		this.minorNumber = minorNumber;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
}
