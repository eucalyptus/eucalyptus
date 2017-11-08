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
package com.eucalyptus.portal;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;

import com.eucalyptus.component.Topology;
import com.eucalyptus.portal.common.Portal;
import com.eucalyptus.simplequeue.SimpleQueue;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow;
import org.apache.log4j.Logger;

@Provides(Portal.class)
@RunDuring(Bootstrap.Stage.Final)
@DependsLocal(Portal.class)
public class PortalServiceBootstrapper extends Bootstrapper.Simple {
  private static Logger LOG = Logger.getLogger(PortalServiceBootstrapper.class);

  private static PortalServiceBootstrapper singleton;

  public static Bootstrapper getInstance() {
    synchronized (PortalServiceBootstrapper.class) {
      if (singleton == null) {
        singleton = new PortalServiceBootstrapper();
      }
    }
    return singleton;
  }
  @Override
  public boolean check() throws Exception {
    if (!super.check())
      return false;

    try {
      if (!Topology.isEnabled(SimpleQueue.class)) {
        return false;
      }
      createQueueIfNotExist();
    }catch (final Exception ex) {
      LOG.error("Failed to create SQS queues for billing", ex);
      return false;
    }

    try {
      if (Topology.isEnabled( SimpleWorkflow.class ))  {
        if(!WorkflowClientManager.isRunning()) {
          WorkflowClientManager.start();
        }
      } else {
        return false;
      }
    }catch(final Exception ex) {
      LOG.error("Failed to start SWF workers for billing", ex);
      return false;
    }

    return true;
  }

  @Override
  public boolean disable() throws Exception {
    try {
      WorkflowClientManager.stop();
    }catch(final Exception ex) {
      LOG.error("Failed to stop SWF workers for billing", ex);
      return false;
    }
    return true;
  }

  private void createQueueIfNotExist() throws Exception {
    final SimpleQueueClientManager client = SimpleQueueClientManager.getInstance();
    for (final String queue : new String[]{BillingProperties.SENSOR_QUEUE_NAME, BillingProperties.INSTANCE_HOUR_SENSOR_QUEUE_NAME}) {
      if (!client.queueExists(queue)) {
        client.createQueue(queue, BillingProperties.getQueueAttributes());
      } else {
        client.setQueueAttributes(queue,
                BillingProperties.getQueueAttributes());
      }
    }
  }
}
