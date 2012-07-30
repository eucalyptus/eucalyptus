package com.eucalyptus.reporting.event_store;

import javax.persistence.*;

import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;

/**
 * @author tom.werges
 */
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_volume_usage_events")
public class ReportingVolumeUsageEvent
	extends AbstractPersistent 
{
	@Column(name="uuid", nullable=false)
	protected String uuid;
	@Column(name="timestamp_ms", nullable=false)
	protected Long timestampMs;
	@Column(name="cumulative_megs_read", nullable=true)
	protected Long cumulativeMegsRead;
	@Column(name="cumulative_megs_written", nullable=true)
	protected Long cumulativeMegsWritten;


	protected ReportingVolumeUsageEvent()
	{
		//hibernate will override these thru reflection despite finality
		this.uuid = null;
		this.timestampMs = null;
		this.cumulativeMegsRead = null;
		this.cumulativeMegsWritten = null;
	}

	ReportingVolumeUsageEvent(String uuid, Long timestampMs,
			Long cumulativeMegsRead, Long cumulativeMegsWritten)
	{
		if (timestampMs == null)
			throw new IllegalArgumentException("timestampMs can't be null");
		if (uuid == null)
			throw new IllegalArgumentException("volumeId can't be null");
		this.uuid = uuid;
		this.timestampMs = timestampMs;
		this.cumulativeMegsRead = cumulativeMegsRead;
		this.cumulativeMegsWritten = cumulativeMegsWritten;
	}

	public String getUuId()
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
	public Long getCumulativeMegsRead()
	{
		return cumulativeMegsRead;
	}
	
	/**
	 * @return Can return null, which indicates unknown usage and not zero usage.
	 */
	public Long getCumulativeMegsWritten()
	{
		return cumulativeMegsWritten;
	}

	@Override
	public String toString()
	{
		return "[timestamp: " + this.timestampMs + " uuid:" + this.uuid + " cumulMegsRead:"
			+ this.cumulativeMegsRead + " cumulMegsWritten:" + this.cumulativeMegsWritten + "]";
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
		ReportingVolumeUsageEvent other = (ReportingVolumeUsageEvent) obj;
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
