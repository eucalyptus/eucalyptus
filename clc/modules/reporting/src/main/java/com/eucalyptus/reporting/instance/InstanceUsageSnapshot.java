package com.eucalyptus.reporting.instance;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Entity;

/**
 * <p>InstanceUsageSnapshot is a record of cumulative instance resource usage
 * at a certain point in time, keyed by instance uuid.
 * 
 * <p>InstanceUsageSnapshot allows null values for some of its fields. Null values
 * indicate unknown usage and not zero usage.
 * 
 * @author tom.werges
 */
@Entity
@PersistenceContext(name="reporting")
@Table(name="instance_usage_snapshot")
class InstanceUsageSnapshot
{
	//Hibernate can override final fields
	@Id
	@GeneratedValue(strategy=GenerationType.TABLE)
	@Column(name="id", nullable=false)
	protected final Long id;
	@Column(name="uuid", nullable=false)
	protected final String uuid;
	@Column(name="timestamp_ms", nullable=false)
	protected final Long timestampMs;
	@Column(name="total_network_io_megs", nullable=true)
	protected final Long networkIoMegs;
	@Column(name="total_disk_io_megs", nullable=true)
	protected final Long diskIoMegs;


	protected InstanceUsageSnapshot()
	{
		//hibernate will override these thru reflection despite finality
		this.id = null;
		this.uuid = null;
		this.timestampMs = null;
		this.networkIoMegs = null;
		this.diskIoMegs = null;
	}

	InstanceUsageSnapshot(String uuid, Long timestampMs,
			Long networkIoMegs, Long diskIoMegs)
	{
		if (timestampMs == null)
			throw new IllegalArgumentException("timestampMs can't be null");
		this.id = null;
		this.uuid = uuid;
		this.timestampMs = timestampMs;
		this.networkIoMegs = networkIoMegs;
		this.diskIoMegs = diskIoMegs;
	}

	String getUuid()
	{
		return uuid;
	}
	
	Long getTimestampMs()
	{
		return timestampMs;
	}
	
	/**
	 * @return Can return null, which indicates unknown usage and not zero usage.
	 */
	Long getCumulativeNetworkIoMegs()
	{
		return networkIoMegs;
	}
	
	/**
	 * @return Can return null, which indicates unknown usage and not zero usage.
	 */
	Long getCumulativeDiskIoMegs()
	{
		return diskIoMegs;
	}

	Long getId()
	{
		return id;
	}
}
