/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
package com.eucalyptus.objectstorage;

import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.ResourceType.StorageWalrus;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.objectstorage.entities.ObjectStorageGlobalConfiguration;
import com.eucalyptus.objectstorage.policy.ObjectStorageQuotaUtil;
import com.eucalyptus.reporting.event.ResourceAvailabilityEvent;
import com.eucalyptus.reporting.event.ResourceAvailabilityEvent.Availability;
import com.eucalyptus.storage.config.ConfigurationCache;

/**
 * Event listener that fires ResourceAvailabilityEvents for the ObjectStorage.
 */
public class ObjectStorageAvailabilityEventListener implements EventListener<ClockTick> {
  private static Logger logger = Logger.getLogger(ObjectStorageAvailabilityEventListener.class);

  public static void register() {
    Listeners.register(ClockTick.class, new ObjectStorageAvailabilityEventListener());
  }

  @Override
  public void fireEvent(final ClockTick event) {
    if (BootstrapArgs.isCloudController() && Bootstrap.isOperational()) {
      try {
        long capacity = ConfigurationCache.getConfiguration(ObjectStorageGlobalConfiguration.class).getMax_total_reporting_capacity_gb();
        long used = 0;

        if ( capacity != Integer.MAX_VALUE ) {
          // only calculate usage if capacity is configured
          // with many objects usage calculation causes load
          used = (long) Math.ceil((double) ObjectStorageQuotaUtil.getTotalObjectSize() / FileUtils.ONE_GB);
        }

        ListenerRegistry.getInstance().fireEvent(
            new ResourceAvailabilityEvent(StorageWalrus, new Availability(
                capacity,
                Math.max(0, capacity - used))));
      } catch (Exception ex) {
        logger.error(ex, ex);
      }
    }
  }
}
