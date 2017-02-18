/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.portal.awsusage;

import com.eucalyptus.reporting.event.AddressEvent;
import com.eucalyptus.reporting.event.InstanceUsageEvent;
import com.eucalyptus.reporting.event.SnapShotEvent;
import com.eucalyptus.reporting.event.VolumeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

public class QueuedEvents {
  private static final Logger LOG = Logger
          .getLogger(QueuedEvents.class);
  public static Function<QueuedEvent, String> EventToMessage = (event) -> {
    final ObjectMapper mapper = new ObjectMapper();
    try {
      final String jsonObj = mapper.writeValueAsString(event);
      return new String(Base64.getEncoder().encode(jsonObj.getBytes()));
    }catch (final IOException ex) {
      LOG.debug("Failed to serialize QueuedEvent", ex);
      return null;
    }
  };

  public static Function<String, QueuedEvent> MessageToEvent = (message) -> {
    final ObjectMapper mapper = new ObjectMapper();
    try {
      final String jsonObj = new String(Base64.getDecoder().decode(message.getBytes()));
      final QueuedEvent event =
              mapper.readValue(jsonObj, QueuedEvent.class);
      return event;
    }catch (final IOException ex) {
      LOG.debug("Failed to deserialize QueuedEvent", ex);
      return null;
    }
  };

  public static Function<InstanceUsageEvent, QueuedEvent> FromInstanceUsageEvent = (event) -> {
    final QueuedEvent q = new QueuedEvent();
    q.setEventType("InstanceUsage");
    q.setResourceId(event.getInstanceId());
    q.setAccountId( null );
    q.setUserId( null );
    q.setTimestamp( new Date(System.currentTimeMillis()));
    return q;
  };

  private static final long GigaByte = 1073741824;
  public static Function<VolumeEvent, QueuedEvent> FromVolumeUsageEvent = (event) -> {
    final QueuedEvent q = new QueuedEvent();
    q.setEventType("VolumeUsage");
    q.setResourceId(event.getVolumeId());
    q.setAccountId(event.getAccountNumber());
    q.setUserId(event.getUserId());
    q.setAvailabilityZone(event.getAvailabilityZone());
    q.setUsageValue(String.format("%d", event.getSizeGB() * GigaByte));
    q.setTimestamp( new Date(System.currentTimeMillis()));
    return q;
  };

  public static Function<SnapShotEvent, QueuedEvent> FromSnapshotUsageEvent = (event) -> {
    final QueuedEvent q = new QueuedEvent();
    q.setEventType("SnapshotUsage");
    q.setResourceId(event.getSnapshotId());
    q.setAccountId(event.getAccountNumber());
    q.setUserId(event.getUserId());
    q.setUsageValue(String.format("%d", event.getVolumeSizeGB() * GigaByte));
    q.setTimestamp(new Date(System.currentTimeMillis()));
    return q;
  };

  public static Function<AddressEvent, QueuedEvent> FromAddressUsageEvent = (event) -> {
    final QueuedEvent q = new QueuedEvent();
    q.setEventType("AddressUsage");
    q.setResourceId(event.getAddress());
    q.setAccountId(event.getAccountId());
    q.setUserId(event.getUserId());
    q.setUsageValue(event.getActionInfo().getAction().toString()); // ALLOCATE or ASSOCIATE
    q.setTimestamp(new Date(System.currentTimeMillis()));
    return q;
  };
}
