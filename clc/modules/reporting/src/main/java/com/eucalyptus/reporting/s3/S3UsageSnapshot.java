package com.eucalyptus.reporting.s3;

import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
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

	public String toString()
	{
		return String.format("[key:%s,usageData:%s]", this.key, this.usageData);
	}

}
