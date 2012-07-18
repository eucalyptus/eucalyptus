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
import com.eucalyptus.entities.AbstractPersistent;

@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_storage")
@Table( name = "storage_stats_info" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class StorageStatsInfo extends AbstractPersistent {
	@Column(name = "storage_name")
	private String name;
	@Column( name = "number_volumes" )
	private Integer numberOfVolumes;
	@Column( name = "total_space_used" )
	private Long totalSpaceUsed;

	public StorageStatsInfo() {}
	
	public StorageStatsInfo(final String name) {
		this.name = name;
	}
	
	public StorageStatsInfo(final String name, 
			Integer numberOfVolumes,
			Long totalSpaceUsed) {
		this.name = name;
		this.numberOfVolumes = numberOfVolumes;
		this.totalSpaceUsed = totalSpaceUsed;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getNumberOfVolumes() {
		return numberOfVolumes;
	}

	public void setNumberOfVolumes(Integer numberOfVolumes) {
		this.numberOfVolumes = numberOfVolumes;
	}

	public Long getTotalSpaceUsed() {
		return totalSpaceUsed;
	}

	public void setTotalSpaceUsed(Long totalSpaceUsed) {
		this.totalSpaceUsed = totalSpaceUsed;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StorageStatsInfo other = (StorageStatsInfo) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	
}
