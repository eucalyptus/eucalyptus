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
@SqlResultSetMapping(name="instanceUsageEventMap",
        entities=@EntityResult(entityClass=ReportingInstanceUsageEvent.class))
@NamedNativeQuery(name="scanInstanceUsageEvents",
     query="select * from reporting_instance_usage_events order by uuid,timestamp_ms",
     resultSetMapping="instanceUsageEventMap")
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_instance_usage_events")
public class ReportingInstanceUsageEvent
	extends ReportingEventSupport
{
	private static final long serialVersionUID = 1L;
	
	@Column(name="uuid", nullable=false)
	protected String uuid;
	@Column(name="resource_name", nullable=true)
	protected String resourceName;
	@Column(name="metric", nullable=true)
	protected String metric;
	@Column(name="sequenceNum", nullable=true)
	protected int sequenceNum;
	@Column(name="dimension", nullable=true)
	protected String dimension;
	@Column(name="value", nullable=true)
	protected Double value;
	@Column(name="valueTimestamp", nullable=true)
	protected Long valueTimestamp;
	


	protected ReportingInstanceUsageEvent()
	{
	  
	}

	ReportingInstanceUsageEvent(final String uuid, final long timestamp, final String resourceName,
		    final String metric, final int sequenceNum, final String dimension,
		    final Double value, final long valueTimestamp) {

		assertThat(uuid, notNullValue());
		assertThat(timestamp, notNullValue());
		assertThat(resourceName, notNullValue());
		assertThat(metric, notNullValue());
		assertThat(sequenceNum, notNullValue());
		assertThat(dimension, notNullValue());
		assertThat(value, notNullValue());
		assertThat(valueTimestamp, notNullValue());
		this.uuid = uuid;
		this.timestampMs = timestamp;
		this.resourceName = resourceName;
		this.metric = metric;
		this.sequenceNum = sequenceNum;
		this.dimension = dimension;
		this.value = value;
		this.valueTimestamp = valueTimestamp;
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

	public String getResourceName() {
	    return resourceName;
	}

	public String getMetric() {
	    return metric;
	}

	public int getSequenceNum() {
	    return sequenceNum;
	}

	public String getDimension() {
	    return dimension;
	}

	public Double getValue() {
	    return value;
	}

	public Long getValueTimestamp() {
	    return valueTimestamp;
	}

	@Override
	public String toString() {
	    return "ReportingInstanceUsageEvent [uuid=" + uuid
		    + ", resourceName=" + resourceName + ", metric=" + metric
		    + ", sequenceNum=" + sequenceNum + ", dimension="
		    + dimension + ", value=" + value + ", valueTimestamp="
		    + valueTimestamp + "]";
	}

}
