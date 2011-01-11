package com.eucalyptus.reporting.storage;

import javax.persistence.*;

@Entity
@PersistenceContext(name="reporting")
@Table(name="storage_usage_snapshot")
class StorageUsageSnapshot
{
	@Id
	@GeneratedValue(strategy=GenerationType.TABLE)
	@Column(name="id", nullable=false)
	protected Long id;
	@Column(name="timestamp_ms", nullable=false)
	protected Long timestampMs;
	@Column(name="owner_id", nullable=false)
	protected String ownerId;
	@Column(name="account_id", nullable=false)
	protected String accountId;
	@Column(name="cluster_name", nullable=false)
	protected String clusterName;
	@Column(name="availability_zone", nullable=false)
	protected String availabilityZone;
	@Column(name="volumes_num", nullable=true)
	protected Long volumesNum;
	@Column(name="snapshot_num", nullable=true)
	protected Long snapshotsNum;
	@Column(name="volumes_gb", nullable=true)
	protected Long volumesGb; //Gigabytes, not gigabits; following java caps
	@Column(name="snapshot_gb", nullable=true)
	protected Long snapshotsGb;
	@Column(name="objects_num", nullable=true)
	protected Long objectsNum;
	@Column(name="objects_gb", nullable=true)
	protected Long objectsGb;

	public Long getId()
	{
		return id;
	}

	public Long getTimestampMs()
	{
		return timestampMs;
	}

	public String getOwnerId()
	{
		return ownerId;
	}

	public String getAccountId()
	{
		return accountId;
	}

	public String getClusterName()
	{
		return clusterName;
	}

	public String getAvailabilityZone()
	{
		return availabilityZone;
	}

	public Long getVolumesNum()
	{
		return volumesNum;
	}

	public Long getSnapshotsNum()
	{
		return snapshotsNum;
	}

	public Long getVolumesGb()
	{
		return volumesGb;
	}

	public Long getSnapshotsGb()
	{
		return snapshotsGb;
	}

	public Long getObjectsNum()
	{
		return objectsNum;
	}

	public Long getObjectsGb()
	{
		return objectsGb;
	}

}
