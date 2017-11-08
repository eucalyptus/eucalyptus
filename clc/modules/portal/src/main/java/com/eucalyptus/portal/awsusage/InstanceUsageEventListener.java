/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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

      // pick up InstanceDataTransfer, PublicIpTransfer, and LoadBalancerDataTransfer
      QueuedEvents.FromPublicIpTransfer.apply(event).stream()
              .map( QueuedEvents.EventToMessage )
              .forEach( sensorSender );
    } catch (final Exception ex) {
      LOG.error("Failed to send instance event message to queue", ex);
    }
  }
}