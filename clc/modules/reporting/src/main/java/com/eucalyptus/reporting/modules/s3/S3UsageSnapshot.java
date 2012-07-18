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

package com.eucalyptus.reporting.modules.s3;

import javax.persistence.*;

import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;

/**
 * <p>S3UsageSnapshot is a snapshot of S3 data usage at some point in time.
 */
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="s3_usage_snapshot")
public class S3UsageSnapshot
	extends AbstractPersistent
{
	@Embedded
	protected S3SnapshotKey key;
	@Embedded
	protected S3UsageData usageData;
	@Column(name="is_all_snapshot", nullable=false)
	protected Boolean allSnapshot = false;


	protected S3UsageSnapshot()
	{
	}

	/**
	 * Copy constructor to create a non-attached hibernate-less object.
	 */
	public S3UsageSnapshot(S3UsageSnapshot snapshot)
	{
		this(new S3SnapshotKey(snapshot.getSnapshotKey()),
				new S3UsageData(snapshot.getUsageData()));
	}
	
	public S3UsageSnapshot(S3SnapshotKey key, S3UsageData usageData)
	{
		this.key = key;
		this.usageData = usageData;
	}

	public S3SnapshotKey getSnapshotKey()
	{
		return key;
	}

	public S3UsageData getUsageData()
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
		return String.format("[key:%s,usageData:%s,allSnapshot:%b]", this.key,
				this.usageData, this.allSnapshot);
	}

}
