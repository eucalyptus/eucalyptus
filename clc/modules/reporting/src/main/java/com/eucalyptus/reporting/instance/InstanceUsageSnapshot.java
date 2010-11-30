package com.eucalyptus.reporting.instance;

import javax.persistence.*;

/**
 * <p>InstanceUsageSnapshot is a record of cumulative instance resource usage
 * at a certain point in time, keyed by instance uuid.
 * 
 * @author tom.werges
 */
@Entity
@PersistenceContext(name="reporting")
@Table(name="instance_usage_snapshot")
class InstanceUsageSnapshot
{
	@Id
	@Column(name="uuid", nullable=false)
	protected String uuid;
	@Embedded
	protected UsageSnapshot usageSnapshot;

	protected InstanceUsageSnapshot(String uuid, UsageSnapshot usageSnapshot)
	{
		this.uuid = uuid;
		this.usageSnapshot = usageSnapshot;
	}

	public InstanceUsageSnapshot(InstanceUsageSnapshot copyFromThis)
	{
		this.uuid = copyFromThis.uuid;
		this.usageSnapshot = copyFromThis.usageSnapshot;
	}

	public String getUuid()
	{
		return this.uuid;
	}

	public UsageSnapshot getUsageSnapshot()
	{
		return this.usageSnapshot;
	}

}
