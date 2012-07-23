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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.WalrusProperties;

@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_walrus")
@Table( name = "drbd_info" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@ConfigurableClass(root = "walrus", alias = "drbd", description = "DRBD configuration.", deferred = true)
public class DRBDInfo extends AbstractPersistent {
	@Column(name = "walrus_name", unique=true)
	private String name;
	@ConfigurableField( description = "DRBD block device", displayName = "Block Device" )
	@Column( name = "block_device" )
	private String blockDevice;
	@ConfigurableField( description = "DRBD resource name", displayName = "DRBD Resource" )
	@Column( name = "resource_name" )
	private String resource;

	public DRBDInfo() {}

	public DRBDInfo(final String name,
			final String blockDevice) {
		this.name = name;
		this.blockDevice = blockDevice;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBlockDevice() {
		return blockDevice;
	}

	public void setBlockDevice(String blockDevice) {
		this.blockDevice = blockDevice;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public static DRBDInfo getDRBDInfo() {
		EntityWrapper<DRBDInfo> db = EntityWrapper.get(DRBDInfo.class);
		DRBDInfo drbdInfo;
		try {
			drbdInfo = db.getUnique(new DRBDInfo());
		} catch(EucalyptusCloudException ex) {
			drbdInfo = new DRBDInfo(WalrusProperties.NAME, 
					"/dev/unknown");
			db.add(drbdInfo);     
		} finally {
			db.commit();
		}
		return drbdInfo;
	}
}
