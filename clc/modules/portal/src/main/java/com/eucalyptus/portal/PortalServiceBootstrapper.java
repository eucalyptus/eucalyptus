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
