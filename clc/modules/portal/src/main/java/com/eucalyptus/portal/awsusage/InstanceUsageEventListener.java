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
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.event.InstanceUsageEvent;
import java.util.Optional;

public class InstanceUsageEventListener implements
        EventListener<InstanceUsageEvent> {
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
      String msg = null;
      final Optional<QueuedEvent> instanceUsageEvent = QueuedEvents.FromInstanceUsageEvent.apply(event);
      if (instanceUsageEvent.isPresent()) {
        msg = QueuedEvents.EventToMessage.apply(instanceUsageEvent.get());
        SimpleQueueClientManager.getInstance().sendMessage(BillingProperties.SENSOR_QUEUE_NAME,
                msg);

      }
      // pick up VolumeIOUsage
      final Optional<QueuedEvent> volIoEvent = QueuedEvents.FromVolumeIoUsage.apply(event);
      if (volIoEvent.isPresent()) {
        msg = QueuedEvents.EventToMessage.apply(volIoEvent.get());
        SimpleQueueClientManager.getInstance().sendMessage(BillingProperties.SENSOR_QUEUE_NAME,
                msg);
      }

      // pick up InstanceDataTransfer
      final Optional<QueuedEvent> instanceDataTransferEvent = QueuedEvents.FromInstanceDataTransfer.apply(event);
      if (instanceDataTransferEvent.isPresent()) {
        msg = QueuedEvents.EventToMessage.apply(instanceDataTransferEvent.get());
        SimpleQueueClientManager.getInstance().sendMessage(BillingProperties.SENSOR_QUEUE_NAME,
                msg);
      }

      // pick up PublicIp transfer
      final Optional<QueuedEvent> publicIpTransferEvent = QueuedEvents.FromPublicIpTransfer.apply(event);
      if (publicIpTransferEvent.isPresent()) {
        msg = QueuedEvents.EventToMessage.apply(publicIpTransferEvent.get());
        SimpleQueueClientManager.getInstance().sendMessage(BillingProperties.SENSOR_QUEUE_NAME,
                msg);
      }

      // pick up ELB data transfer event
      final Optional<QueuedEvent> elbTransferEvent = QueuedEvents.FromLoadBalancerDataTransfer.apply(event);
      if (elbTransferEvent.isPresent()) {
        msg = QueuedEvents.EventToMessage.apply(elbTransferEvent.get());
        SimpleQueueClientManager.getInstance().sendMessage(BillingProperties.SENSOR_QUEUE_NAME,
                msg);
      }
    } catch (final Exception ex) {
      LOG.error("Failed to send instance event message to queue", ex);
    }
  }
}