/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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

import java.util.function.Function;
import org.apache.log4j.Logger;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.portal.BillingProperties;
import com.eucalyptus.portal.SimpleQueueClientManager;
import com.eucalyptus.util.Consumer;

/**
 *
 */
abstract class SensorQueueEventListener<T extends Event> implements EventListener<T> {

  void transformAndQueue( final Logger log, T event, Function<T, QueuedEvent> transform ) {
    try {
      queueConsumer( log, BillingProperties.SENSOR_QUEUE_NAME ).accept(
          transform.andThen( QueuedEvents.EventToMessage ).apply( event )
      );
    } catch (final Exception ex) {
      log.error("Failed to send event message to queue", ex);
    }
  }

  Consumer<String> queueConsumer( final Logger log, final String queueName ) {
    return ( msg ) -> {
      try {
        SimpleQueueClientManager.getInstance( ).sendMessage( queueName, msg );
      } catch ( final Exception ex ) {
        log.error("Failed to send event message to queue", ex);
      }
    };
  }
}
