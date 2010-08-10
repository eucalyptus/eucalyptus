/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.cloud.entities;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableIdentifier;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.StorageProperties;

import javax.persistence.*;

@Entity
@PersistenceContext(name="eucalyptus_storage")
@Table( name = "storage_info" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
@ConfigurableClass(root = "storage", alias="basic", description = "Basic storage controller configuration.", singleton=false, deferred = true)
public class
StorageInfo {
	private static Logger LOG = Logger.getLogger( StorageInfo.class );

	@Id
	@GeneratedValue
	@Column( name = "storage_id" )
	private Long id = -1l;
	@ConfigurableIdentifier
	@Column( name = "storage_name", unique=true)
	private String name;
	@ConfigurableField( description = "Total disk space reserved for volumes", displayName = "Disk space reserved for volumes" )
	@Column( name = "system_storage_volume_size_gb" )
	private Integer maxTotalVolumeSizeInGb;
	@ConfigurableField( description = "Max volume size", displayName = "Max volume size" )
	@Column( name = "system_storage_max_volume_size_gb")
	private Integer maxVolumeSizeInGB;

	public StorageInfo(){
		this.name = StorageProperties.NAME;
	}

	public StorageInfo( final String name )
	{
		this.name = name;
	}

	public StorageInfo(final String name, 
			final Integer maxTotalVolumeSizeInGb,
			final Integer maxVolumeSizeInGB) {
		this.name = name;
		this.maxTotalVolumeSizeInGb = maxTotalVolumeSizeInGb;
		this.maxVolumeSizeInGB = maxVolumeSizeInGB;
	}

	public Long getId()
	{
		return id;
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
		EntityWrapper<StorageInfo> storageDb = new EntityWrapper<StorageInfo>(StorageProperties.DB_NAME);
		StorageInfo conf = null;
		try {
			conf = storageDb.getUnique(new StorageInfo(StorageProperties.NAME));
			storageDb.commit();
		}
		catch ( EucalyptusCloudException e ) {
			LOG.warn("Failed to get storage info for: " + StorageProperties.NAME + ". Loading defaults.");
			conf =  new StorageInfo(StorageProperties.NAME, 
					StorageProperties.MAX_TOTAL_VOLUME_SIZE, 
					StorageProperties.MAX_VOLUME_SIZE);
			storageDb.add(conf);
			storageDb.commit();
		}
		catch (Throwable t) {
			LOG.error("Unable to get storage info for: " + StorageProperties.NAME);
			storageDb.rollback();
			return new StorageInfo(StorageProperties.NAME, 
					StorageProperties.MAX_TOTAL_VOLUME_SIZE, 
					StorageProperties.MAX_VOLUME_SIZE);
		}
		return conf;
	}
}
