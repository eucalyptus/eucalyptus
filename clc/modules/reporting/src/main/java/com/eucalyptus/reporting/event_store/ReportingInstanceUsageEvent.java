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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Set;
import javax.persistence.*;

import org.hibernate.annotations.Entity;

@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_instance_usage_events")
public class ReportingInstanceUsageEvent
	extends ReportingEventSupport
{
	private static final long serialVersionUID = 1L;
	
	@Column(name="uuid", nullable=false)
	protected String uuid;
	@Column(name="metric", nullable=false)
	protected String metric;
	@Column(name="sequenceNum", nullable=false)
	protected Long sequenceNum;
	@Column(name="dimension", nullable=false)
	protected String dimension;
	@Column(name="value", nullable=false)
	protected Double value;

	protected ReportingInstanceUsageEvent()
	{
	}

	ReportingInstanceUsageEvent( final String uuid, final String metric, final Long sequenceNum,
		    final String dimension, final Double value, final Long valueTimestamp ) {

		assertThat(uuid, notNullValue());
		assertThat(metric, notNullValue());
		assertThat(sequenceNum, notNullValue());
		assertThat(dimension, notNullValue());
		assertThat(value, notNullValue());
		assertThat(valueTimestamp, notNullValue());
		this.uuid = uuid;
		this.timestampMs = valueTimestamp;
		this.metric = metric;
		this.sequenceNum = sequenceNum;
		this.dimension = dimension;
		this.value = value;
	}

	public ReportingInstanceUsageEvent zero( final Long timestamp ) {
		final ReportingInstanceUsageEvent event = new ReportingInstanceUsageEvent();
		event.uuid = uuid;
		event.metric = metric;
		event.dimension = dimension;
		event.value = 0d;
		event.timestampMs = timestamp;
		event.sequenceNum = -1L;
		return event;
	}

	@Override
	public Set<EventDependency> getDependencies() {
		return withDependencies()
				.relation( ReportingInstanceCreateEvent.class, "uuid", uuid )
				.set();
	}

	public String getUuid() {
	    return uuid;
	}

	public String getMetric() {
	    return metric;
	}

	public Long getSequenceNum() {
	    return sequenceNum;
	}

	public String getDimension() {
	    return dimension;
	}

	public Double getValue() {
	    return value;
	}

	@Override
	public String toString() {
			return "ReportingInstanceUsageEvent [uuid=" + uuid
				+ ", metric=" + metric
				+ ", sequenceNum=" + sequenceNum + ", dimension="
				+ dimension + ", value=" + value + ", timestamp="
				+ timestampMs + "]";
	}

}
