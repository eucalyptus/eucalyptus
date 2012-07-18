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

package com.eucalyptus.reporting.modules.storage;

import javax.persistence.*;

import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;

/**
 * <p>StorageUsageSnapshot is a snapshot of disk data usage at some point in
 * time.
 */
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="storage_usage_snapshot")
public class StorageUsageSnapshot
	extends AbstractPersistent
{
	@Embedded
	protected StorageSnapshotKey key;
	@Embedded
	protected StorageUsageData usageData;
	@Column(name="is_all_snapshot", nullable=false)
	protected Boolean allSnapshot = false;
	
	protected StorageUsageSnapshot()
	{
		
	}

	public StorageUsageSnapshot(StorageSnapshotKey key, StorageUsageData usageData)
	{
		this.key = key;
		this.usageData = usageData;
	}

	public StorageSnapshotKey getSnapshotKey()
	{
		return key;
	}

	public StorageUsageData getUsageData()
	{
		return usageData;
	}
	
	public Boolean getAllSnapshot()
	{
		return allSnapshot;
	}

	public void setAllSnapshot(Boolean allSnapshot)
	{
		this.allSnapshot = allSnapshot;
	}
	
	public String toString()
	{
		return String.format("[key:%s,usageData:%s,allSnapshot:%b]",
				this.key, this.usageData, this.allSnapshot);
	}

	@Override
	public int hashCode()
	{
		return key.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		StorageUsageSnapshot other = (StorageUsageSnapshot) obj;
		return key.equals(other.key);
	}

}
