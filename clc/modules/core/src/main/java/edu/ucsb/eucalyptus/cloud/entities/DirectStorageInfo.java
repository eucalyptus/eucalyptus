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
@Table( name = "direct_storage_info" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@ConfigurableClass(root = "storage", alias = "direct", description = "Basic storage controller configuration.", singleton=false, deferred = true)
public class DirectStorageInfo extends AbstractPersistent {
	private static Logger LOG = Logger.getLogger( DirectStorageInfo.class );

	@ConfigurableIdentifier
	@Column( name = "storage_name", unique=true)
	private String name;
	@ConfigurableField( description = "Storage network interface.", displayName = "Storage Interface" )
	@Column( name = "storage_interface" )
	private String storageInterface;
	@ConfigurableField( description = "Storage volumes directory.", displayName = "Volumes path" )
	@Column( name = "system_storage_volumes_dir" )
	private String volumesDir;
	@ConfigurableField( description = "Should volumes be zero filled.", displayName = "Zero-fill volumes", type = ConfigurableFieldType.BOOLEAN )
	@Column(name = "zero_fill_volumes")
	private Boolean zeroFillVolumes;

	public DirectStorageInfo(){
		this.name = StorageProperties.NAME;
	}

	public DirectStorageInfo( final String name )
	{
		this.name = name;
	}

	public DirectStorageInfo(final String name, 
			final String storageInterface, 
			final String volumesDir,
			final Boolean zeroFillVolumes) {
		this.name = name;
		this.storageInterface = storageInterface;
		this.volumesDir = volumesDir;
		this.zeroFillVolumes = zeroFillVolumes;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStorageInterface() {
		return storageInterface;
	}

	public void setStorageInterface(String storageInterface) {
		this.storageInterface = storageInterface;
	}

	public String getVolumesDir() {
		return volumesDir;
	}

	public void setVolumesDir(String volumesDir) {
		this.volumesDir = volumesDir;
	}

	public Boolean getZeroFillVolumes() {
		return zeroFillVolumes;
	}

	public void setZeroFillVolumes(Boolean zeroFillVolumes) {
		this.zeroFillVolumes = zeroFillVolumes;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DirectStorageInfo other = (DirectStorageInfo) obj;
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

	public static DirectStorageInfo getStorageInfo() {
		EntityWrapper<DirectStorageInfo> storageDb = EntityWrapper.get(DirectStorageInfo.class);
		DirectStorageInfo conf = null;
		try {
			conf = storageDb.getUnique(new DirectStorageInfo(StorageProperties.NAME));
			storageDb.commit();
		}
		catch ( EucalyptusCloudException e ) {
			LOG.warn("Failed to get storage info for: " + StorageProperties.NAME + ". Loading defaults.");
			conf =  new DirectStorageInfo(StorageProperties.NAME, 
					StorageProperties.iface, 
					StorageProperties.storageRootDirectory,
					StorageProperties.zeroFillVolumes);
			storageDb.add(conf);
			storageDb.commit();
		}
		catch (Exception t) {
			LOG.error("Unable to get storage info for: " + StorageProperties.NAME);
			storageDb.rollback();
			return new DirectStorageInfo(StorageProperties.NAME, 
					StorageProperties.iface, 
					StorageProperties.storageRootDirectory,
					StorageProperties.zeroFillVolumes);
		}
		return conf;
	}
}
