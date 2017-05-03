/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.portal.awsusage

class QueuedEvent {
    String eventType = null;
    String operation = null;
    String resourceId = null;
    String accountId = null;
    String userId = null;
    String availabilityZone = null;
    String usageValue = null;
    String any = null; // fields reserved for any data that's useful for aggregation
    Date timestamp = null;

    QueuedEvent() { }
    QueuedEvent( final QueuedEvent other ) {
        this.eventType = other.eventType;
        this.operation = other.operation;
        this.resourceId = other.resourceId;
        this.accountId = other.accountId;
        this.userId = other.userId;
        this.availabilityZone = other.availabilityZone;
        this.usageValue = other.usageValue;
        this.any = other.any;
        this.timestamp = other.timestamp;
    }

    @Override
    public String toString() {
        return String.format("%s:%s:%s:%s:%s:%s:%s:%s:%s",
        eventType, operation, resourceId, accountId, userId, availabilityZone, usageValue, any, timestamp);
    }
}
