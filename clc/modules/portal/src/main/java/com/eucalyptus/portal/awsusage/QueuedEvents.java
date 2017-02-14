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

import com.eucalyptus.reporting.event.InstanceUsageEvent;
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
}
