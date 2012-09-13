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
package com.eucalyptus.reporting.event_store;

import java.util.Set;
import javax.persistence.*;

import org.hibernate.annotations.Entity;

@Entity @javax.persistence.Entity
@SqlResultSetMapping(name="usageEventMap",
        entities=@EntityResult(entityClass=ReportingInstanceUsageEvent.class))
@NamedNativeQuery(name="scanInstanceUsageEvents",
     query="select * from reporting_instance_usage_events order by uuid,timestamp_ms",
     resultSetMapping="usageEventMap")
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_instance_usage_events")
public class ReportingInstanceUsageEvent
	extends ReportingEventSupport
{
	private static final long serialVersionUID = 1L;

	@Column(name="uuid", nullable=false)
	protected String uuid;
	@Column(name="cumulative_net_incoming_megs_within_zone", nullable=true)
	protected Long cumulativeNetIncomingMegsWithinZone;
	@Column(name="cumulative_net_incoming_megs_between_zones", nullable=true)
	protected Long cumulativeNetIncomingMegsBetweenZones;
	@Column(name="cumulative_net_incoming_megs_public_ip", nullable=true)
	protected Long cumulativeNetIncomingMegsPublic;
	@Column(name="cumulative_net_outgoing_megs_within_zone", nullable=true)
	protected Long cumulativeNetOutgoingMegsWithinZone;
	@Column(name="cumulative_net_outgoing_megs_between_zones", nullable=true)
	protected Long cumulativeNetOutgoingMegsBetweenZones;
	@Column(name="cumulative_net_outgoing_megs_public_ip", nullable=true)
	protected Long cumulativeNetOutgoingMegsPublic;
	@Column(name="cumulative_disk_io_megs", nullable=true)
	protected Long cumulativeDiskIoMegs;
	@Column(name="cpu_utilization_percent", nullable=true)
	protected Integer cpuUtilizationPercent;


	protected ReportingInstanceUsageEvent()
	{
	}

	ReportingInstanceUsageEvent(String uuid, Long timestampMs, Long cumulativeDiskIoMegs,
			Integer cpuUtilizationPercent, Long cumulativeNetIncomingMegsBetweenZones,
			Long cumulativeNetIncomingMegsWithinZone, Long cumulativeNetIncomingMegsPublicIp,
			Long cumulativeNetOutgoingMegsBetweenZones,	Long cumulativeNetOutgoingMegsWithinZone,
			Long cumulativeNetOutgoingMegsPublicIp)
	{
		if (timestampMs == null)
			throw new IllegalArgumentException("timestampMs can't be null");
		if (uuid == null)
			throw new IllegalArgumentException("uuid can't be null");
		this.uuid = uuid;
		this.timestampMs = timestampMs;
		this.cumulativeDiskIoMegs = cumulativeDiskIoMegs;
		this.cpuUtilizationPercent = cpuUtilizationPercent;
		this.cumulativeNetIncomingMegsBetweenZones = cumulativeNetIncomingMegsBetweenZones;
		this.cumulativeNetIncomingMegsWithinZone = cumulativeNetIncomingMegsWithinZone;
		this.cumulativeNetIncomingMegsPublic = cumulativeNetIncomingMegsPublicIp;
		this.cumulativeNetOutgoingMegsBetweenZones = cumulativeNetOutgoingMegsBetweenZones;
		this.cumulativeNetOutgoingMegsWithinZone = cumulativeNetOutgoingMegsWithinZone;
		this.cumulativeNetOutgoingMegsPublic = cumulativeNetOutgoingMegsPublicIp;
	}

	public String getUuid()
	{
		return uuid;
	}
	
	/**
	 * @return Can return null, which indicates unknown usage and not zero usage.
	 */
	public Long getCumulativeNetIncomingMegsWithinZone()
	{
		return cumulativeNetIncomingMegsWithinZone;
	}

	/**
	 * @return Can return null, which indicates unknown usage and not zero usage.
	 */
	public Long getCumulativeNetIncomingMegsBetweenZones()
	{
		return cumulativeNetIncomingMegsBetweenZones;
	}

	/**
	 * @return Can return null, which indicates unknown usage and not zero usage.
	 */
	public Long getCumulativeNetIncomingMegsPublic()
	{
		return cumulativeNetIncomingMegsPublic;
	}

	/**
	 * @return Can return null, which indicates unknown usage and not zero usage.
	 */
	public Long getCumulativeNetOutgoingMegsWithinZone()
	{
		return cumulativeNetOutgoingMegsWithinZone;
	}

	/**
	 * @return Can return null, which indicates unknown usage and not zero usage.
	 */
	public Long getCumulativeNetOutgoingMegsBetweenZones()
	{
		return cumulativeNetOutgoingMegsBetweenZones;
	}

	
	/**
	 * @return Can return null, which indicates unknown usage and not zero usage.
	 */
	public Long getCumulativeNetOutgoingMegsPublic()
	{
		return cumulativeNetOutgoingMegsPublic;
	}

	/**
	 * @return Can return null, which indicates unknown usage and not zero usage.
	 */
	public Long getCumulativeDiskIoMegs()
	{
		return cumulativeDiskIoMegs;
	}

	/**
	 * @return Can return null, which indicates unknown usage and not zero usage.
	 */
	public Integer getCpuUtilizationPercent()
	{
		return cpuUtilizationPercent;
	}

	@Override
	public Set<EventDependency> getDependencies() {
		return withDependencies()
				.relation( ReportingInstanceCreateEvent.class, "uuid", uuid )
				.set();
	}

	@Override
	public String toString()
	{
		return "[timestamp: " + this.timestampMs + " cumulDiskIoMegs:" + this.cumulativeDiskIoMegs + " cpuUtilization%:" + cpuUtilizationPercent + "]";
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
		ReportingInstanceUsageEvent other = (ReportingInstanceUsageEvent) obj;
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


}
