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
package com.eucalyptus.portal.awsusage;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.portal.BillingProperties;
import com.eucalyptus.portal.SimpleQueueClientManager;
import com.eucalyptus.reporting.event.S3ObjectEvent;
import org.apache.log4j.Logger;
import javax.annotation.Nonnull;

public class S3ObjectEventListener  implements
        EventListener<S3ObjectEvent> {
  private static final Logger LOG = Logger
          .getLogger(S3ObjectEventListener.class);

  public static void register() {
    Listeners.register(S3ObjectEvent.class,
            new S3ObjectEventListener());
  }

  @Override
  public void fireEvent(@Nonnull final S3ObjectEvent event) {
    // should run in the same host running swf activities
    if (!Bootstrap.isOperational() || !BillingProperties.ENABLED) {
      return;
    }

    if (event.getAction() == null ||
            !S3ObjectEvent.S3ObjectAction.OBJECTUSAGE.equals(event.getAction())) {
      return;
    }

    try {
      final QueuedEvent qevt = QueuedEvents.FromS3ObjectUsageEvent.apply(event);
      final String msg = QueuedEvents.EventToMessage.apply(qevt);
      SimpleQueueClientManager.getInstance().sendMessage(BillingProperties.SENSOR_QUEUE_NAME,
              msg);
    } catch (final Exception ex) {
      LOG.error("Failed to send s3 object event message to queue", ex);
    }
  }
}