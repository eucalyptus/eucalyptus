/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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

import javax.annotation.Nonnull;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.portal.BillingProperties;
import org.apache.log4j.Logger;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.event.InstanceUsageEvent;
import com.eucalyptus.util.Consumer;

public class InstanceUsageEventListener extends
    SensorQueueEventListener<InstanceUsageEvent> {
  private static final Logger LOG = Logger
          .getLogger(InstanceUsageEventListener.class);

  public static void register() {
    Listeners.register(InstanceUsageEvent.class,
            new InstanceUsageEventListener());
  }

  @Override
  public void fireEvent(@Nonnull final InstanceUsageEvent event) {
    // Instance usage event is only delivered in CLC
    if (!Bootstrap.isOperational() || !BootstrapArgs.isCloudController() || !BillingProperties.ENABLED) {
      return;
    }

    try {
      final Consumer<String> sensorSender =
          queueConsumer( LOG, BillingProperties.SENSOR_QUEUE_NAME );

      QueuedEvents.FromInstanceUsageEvent.apply( event )
          .map( QueuedEvents.EventToMessage )
          .ifPresent( sensorSender.andThen(
              // instance usage event is used for instance hour reports
             queueConsumer( LOG, BillingProperties.INSTANCE_HOUR_SENSOR_QUEUE_NAME)
          ) );

      // pick up VolumeIOUsage
      QueuedEvents.FromVolumeIoUsage.apply( event )
          .map( QueuedEvents.EventToMessage )
          .ifPresent( sensorSender );

      // pick up InstanceDataTransfer
      QueuedEvents.FromInstanceDataTransfer.apply(event)
          .map( QueuedEvents.EventToMessage )
          .ifPresent( sensorSender );

      // pick up PublicIp transfer
      QueuedEvents.FromPublicIpTransfer.apply(event)
          .map( QueuedEvents.EventToMessage )
          .ifPresent( sensorSender );

      // pick up ELB data transfer event
      QueuedEvents.FromLoadBalancerDataTransfer.apply(event)
          .map( QueuedEvents.EventToMessage )
          .ifPresent( sensorSender );
    } catch (final Exception ex) {
      LOG.error("Failed to send instance event message to queue", ex);
    }
  }
}