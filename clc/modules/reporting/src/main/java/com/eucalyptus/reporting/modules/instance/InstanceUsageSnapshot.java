package com.eucalyptus.reporting.modules.instance;

import javax.persistence.*;

import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;

/**
 * <p>InstanceUsageSnapshot is a record of cumulative instance resource usage
 * at a certain point in time, keyed by instance uuid.
 * 
 * <p>InstanceUsageSnapshot allows null values for some of its fields.
 * 
 * @author tom.werges
 */
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="instance_usage_snapshot")
public class InstanceUsageSnapshot
	extends AbstractPersistent 
{
	@Column(name="uuid", nullable=false)
	protected String uuid;
	@Column(name="timestamp_ms", nullable=false)
	protected Long timestampMs;
	@Column(name="total_network_io_megs", nullable=true)
	protected Long networkIoMegs;
	@Column(name="total_disk_io_megs", nullable=true)
	protected Long diskIoMegs;


	protected InstanceUsageSnapshot()
	{
		//hibernate will override these thru reflection despite finality
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
		this.uuid = uuid;
		this.timestampMs = timestampMs;
		this.networkIoMegs = networkIoMegs;
		this.diskIoMegs = diskIoMegs;
	}

	public String getUuid()
	{
		return uuid;
	}
	
	public Long getTimestampMs()
	{
		return timestampMs;
	}
	
	/**
	 * @return Can return null, which indicates unknown usage and not zero usage.
	 */
	public Long getCumulativeNetworkIoMegs()
	{
		return networkIoMegs;
	}
	
	/**
	 * @return Can return null, which indicates unknown usage and not zero usage.
	 */
	public Long getCumulativeDiskIoMegs()
	{
		return diskIoMegs;
	}

	@Override
	public String toString()
	{
		return "[timestamp: " + this.timestampMs + " cumulIoMegs:" + this.networkIoMegs + " cumulDiskMegs:" + this.diskIoMegs + "]";
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((timestampMs == null) ? 0 : timestampMs.hashCode());
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		InstanceUsageSnapshot other = (InstanceUsageSnapshot) obj;
		if (timestampMs == null) {
			if (other.timestampMs != null)
				return false;
		} else if (!timestampMs.equals(other.timestampMs))
			return false;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}

  /**
   * NOTE:IMPORTANT: this method has default visibility (rather than public) only for the sake of
   * supporting currently hand-coded proxy classes. Don't share this value with the user.
   * 
   * TODO: remove this if possible.
   * @return
   * @see {@link AbstractPersistent#getId()}
   */
  public String getEntityId( ) {
    return this.getId( );
  }

}
