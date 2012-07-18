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
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableIdentifier;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.StorageProperties;

@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_storage")
@Table( name = "storage_info" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@ConfigurableClass(root = "storage", alias="basic", description = "Basic storage controller configuration.", singleton=false, deferred = true)
public class StorageInfo extends AbstractPersistent {
	private static Logger LOG = Logger.getLogger( StorageInfo.class );

	@ConfigurableIdentifier
	@Column( name = "storage_name", unique=true)
	private String name;
	@ConfigurableField( description = "Total disk space reserved for volumes", displayName = "Disk space reserved for volumes" )
	@Column( name = "system_storage_volume_size_gb" )
	private Integer maxTotalVolumeSizeInGb;
	@ConfigurableField( description = "Max volume size", displayName = "Max volume size" )
	@Column( name = "system_storage_max_volume_size_gb")
	private Integer maxVolumeSizeInGB;
	@ConfigurableField( description = "Should transfer snapshots", displayName = "Transfer snapshots to Walrus", type = ConfigurableFieldType.BOOLEAN )
	@Column( name = "system_storage_transfer_snapshots")
	private Boolean shouldTransferSnapshots;

	public StorageInfo() {
		this.name = StorageProperties.NAME;
	}

	public StorageInfo( final String name )
	{
		this.name = name;
	}

	public StorageInfo(final String name, 
			final Integer maxTotalVolumeSizeInGb,
			final Integer maxVolumeSizeInGB,
			final Boolean shouldTransferSnapshots) {
		this.name = name;
		this.maxTotalVolumeSizeInGb = maxTotalVolumeSizeInGb;
		this.maxVolumeSizeInGB = maxVolumeSizeInGB;
		this.shouldTransferSnapshots = shouldTransferSnapshots;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getMaxTotalVolumeSizeInGb() {
		return maxTotalVolumeSizeInGb;
	}

	public void setMaxTotalVolumeSizeInGb(Integer maxTotalVolumeSizeInGb) {
		this.maxTotalVolumeSizeInGb = maxTotalVolumeSizeInGb;
	}

	public Integer getMaxVolumeSizeInGB() {
		return maxVolumeSizeInGB;
	}

	public void setMaxVolumeSizeInGB(Integer maxVolumeSizeInGB) {
		this.maxVolumeSizeInGB = maxVolumeSizeInGB;
	}

	public Boolean getShouldTransferSnapshots() {
		return shouldTransferSnapshots;
	}

	public void setShouldTransferSnapshots(Boolean shouldTransferSnapshots) {
		this.shouldTransferSnapshots = shouldTransferSnapshots;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StorageInfo other = (StorageInfo) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public String toString()
	{
		return this.name;
	}

	public static StorageInfo getStorageInfo() {
		EntityWrapper<StorageInfo> storageDb = EntityWrapper.get(StorageInfo.class);
		StorageInfo conf = null;
		try {
			conf = storageDb.getUnique(new StorageInfo(StorageProperties.NAME));
			storageDb.commit();
		}
		catch ( EucalyptusCloudException e ) {
			LOG.warn("Failed to get storage info for: " + StorageProperties.NAME + ". Loading defaults.");
			conf =  new StorageInfo(StorageProperties.NAME, 
					StorageProperties.MAX_TOTAL_VOLUME_SIZE, 
					StorageProperties.MAX_VOLUME_SIZE,
					true);
			storageDb.add(conf);
			storageDb.commit();
		}
		catch (Exception t) {
			LOG.error("Unable to get storage info for: " + StorageProperties.NAME);
			storageDb.rollback();
			return new StorageInfo(StorageProperties.NAME, 
					StorageProperties.MAX_TOTAL_VOLUME_SIZE, 
					StorageProperties.MAX_VOLUME_SIZE,
					true);
		}
		return conf;
	}
}
