package com.eucalyptus.reporting.s3;

import javax.persistence.*;
import org.hibernate.annotations.Entity;

/**
 * <p>S3UsageSnapshot is a snapshot of S3 data usage at some point in
 * time.
 * 
 * @author tom.werges
 */
@Entity @javax.persistence.Entity
@PersistenceContext(name="reporting")
@Table(name="s3_usage_snapshot")
class S3UsageSnapshot
{
	@EmbeddedId
	protected S3SnapshotKey key;
	@Embedded
	protected S3UsageData usageData;
	@Column(name="is_all_snapshot", nullable=false)
	protected Boolean allSnapshot = false;
	
	
	protected S3UsageSnapshot()
	{
		
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
		return String.format("[key:%s,usageData:%s]", this.key, this.usageData);
	}

}
