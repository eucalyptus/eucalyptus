/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.portal.BillingProperties;
import com.eucalyptus.reporting.event.SnapShotEvent;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;

public class SnapshotUsageEventListener extends SensorQueueEventListener<SnapShotEvent>  {
  private static final Logger LOG = Logger.getLogger(SnapshotUsageEventListener.class);

  public static void register() {
    Listeners.register(SnapShotEvent.class, new SnapshotUsageEventListener());
  }

  @Override
  public void fireEvent(@Nonnull final SnapShotEvent event) {
    // should run in the same host running swf activities
    if (!Bootstrap.isOperational() || !BillingProperties.ENABLED) {
      return;
    }
    if (event.getActionInfo() == null ||
            !SnapShotEvent.SnapShotAction.SNAPSHOTUSAGE.equals(event.getActionInfo().getAction())) {
      return;
    }

    transformAndQueue( LOG, event, QueuedEvents.FromSnapshotUsageEvent );
  }
}