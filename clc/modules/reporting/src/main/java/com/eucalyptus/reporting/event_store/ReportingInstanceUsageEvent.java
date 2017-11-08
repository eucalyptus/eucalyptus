/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.reporting.event_store;

import static org.hamcrest.Matchers.notNullValue;
import static com.eucalyptus.util.Parameters.checkParam;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import com.eucalyptus.component.annotation.RemotablePersistence;

@Entity
@PersistenceContext(name="eucalyptus_reporting_backend")
@RemotablePersistence
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

		checkParam( uuid, notNullValue() );
		checkParam( metric, notNullValue() );
		checkParam( sequenceNum, notNullValue() );
		checkParam( dimension, notNullValue() );
		checkParam( value, notNullValue() );
		checkParam( valueTimestamp, notNullValue() );
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
