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

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.portal.BillingProperties;
import org.apache.log4j.Logger;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.event.InstanceUsageEvent;
import com.eucalyptus.system.Threads;

@ConfigurableClass( root = "cloud.billing", description = "Parameters controlling programmatic billing")
public class InstanceUsageEventListener implements
        EventListener<InstanceUsageEvent> {
  private static final Logger LOG = Logger
          .getLogger(InstanceUsageEventListener.class);

  private static final ScheduledExecutorService eventFlushTimer = Executors
          .newSingleThreadScheduledExecutor( Threads.threadFactory( "reporting-flush-pool-%d" ) );

  private static AtomicBoolean busy = new AtomicBoolean(false);

  private static LinkedBlockingQueue<InstanceUsageEvent> eventQueue = new LinkedBlockingQueue<InstanceUsageEvent>();

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
      final QueuedEvent qevt = QueuedEvents.FromInstanceUsageEvent.apply(event);
      final String msg = QueuedEvents.EventToMessage.apply(qevt);
      SimpleQueueClientManager.getInstance().sendMessage(BillingProperties.SENSOR_QUEUE_NAME,
              msg);
    } catch (final Exception ex) {
      LOG.error("Failed to send instance event message to queue", ex);
    }
  }
}