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

import java.util.Iterator;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;

public class ReportingInstanceEventStore extends EventStoreSupport {
    private static final ReportingInstanceEventStore instance = new ReportingInstanceEventStore();

    public static ReportingInstanceEventStore getInstance() {
	return instance;
    }

    protected ReportingInstanceEventStore() {
    }

    public void insertCreateEvent(@Nonnull final String uuid,
	    @Nonnull final long timestampMs, @Nonnull final String instanceId,
	    @Nonnull final String instanceType, @Nonnull final String userId,
	    @Nonnull final String userName, @Nonnull final String accountName,
	    @Nonnull final String accountId,
	    @Nonnull final String availabilityZone) {
	Preconditions.checkNotNull(uuid, "Uuid is required");
	Preconditions.checkNotNull(instanceId, "InstanceId is required");
	Preconditions.checkNotNull(instanceType, "InstanceType is required");
	Preconditions.checkNotNull(userId, "UserId is required");
	Preconditions.checkNotNull(userName, "User Name is required");
	Preconditions.checkNotNull(accountName, "Account Name is required");
	Preconditions.checkNotNull(accountId, "Account ID is required");
	Preconditions.checkNotNull(availabilityZone,
		"AvailabilityZone is required");

	persist(new ReportingInstanceCreateEvent(uuid, timestampMs, instanceId,
		instanceType, userId, userName, accountName, accountId,
		availabilityZone));
    }

    public static class InstanceEventTuple {
	private final ReportingInstanceAttributeEvent attributeEvent;
	private final ReportingInstanceUsageEvent usageEvent;

	public InstanceEventTuple(
		ReportingInstanceAttributeEvent attributeEvent,
		ReportingInstanceUsageEvent usageEvent) {
	    this.attributeEvent = attributeEvent;
	    this.usageEvent = usageEvent;
	}

	public ReportingInstanceAttributeEvent getAttributeEvent() {
	    return attributeEvent;
	}

	public ReportingInstanceUsageEvent getUsageEvent() {
	    return usageEvent;
	}
    }

    private static class InstanceEventIterator implements
	    Iterator<InstanceEventTuple> {

	@Override
	public boolean hasNext() {
	    return false;
	}

	@Override
	public InstanceEventTuple next() {
	    return null;
	}

	@Override
	public void remove() {
	}

    }

    /**
     * Read through all events, in order, starting from the first event.
     */
    public Iterator<InstanceEventTuple> scanEvents() {
	// final EntityTransaction db = Entities.get(event);
	return null;
    }

    public void insertUsageEvent(@Nonnull final String uuid,
	    @Nonnull final long timestamp, @Nonnull final String resourceName,
	    @Nonnull final String metric, @Nonnull final int sequenceNum,
	    @Nonnull final String dimension, @Nonnull final Double value,
	    @Nonnull final long valueTimestamp) {

	Preconditions.checkNotNull(uuid, "Uuid is required");
	Preconditions.checkNotNull(timestamp, "Timestamp is required");
	Preconditions.checkNotNull(resourceName, "ResourceName is required");
	Preconditions.checkNotNull(metric, "Metric is required");
	Preconditions.checkNotNull(sequenceNum, "SequenceNum is required");
	Preconditions.checkNotNull(dimension, "Dimension is required");
	Preconditions.checkNotNull(value, "value is required");
	Preconditions
		.checkNotNull(valueTimestamp, "ValueTimeStamp is required");

	persist(new ReportingInstanceUsageEvent(uuid, timestamp, resourceName,
		metric, sequenceNum, dimension, value, valueTimestamp));

    }
}
