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
 *
 *  This file may incorporate work covered under the following copyright and permission notice:
 *
 *   Copyright 2010-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *    http://aws.amazon.com/apache2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 ************************************************************************/
package com.eucalyptus.simplequeue.bootstrap;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.Topology;
import com.eucalyptus.simplequeue.SimpleQueue;
import com.eucalyptus.simplequeue.workflow.SimpleQueueWorkflows;
import com.eucalyptus.simplequeue.workflow.WorkflowClientManager;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow;
import org.apache.log4j.Logger;

/**
 * Created by ethomas on 7/30/14.
 */
@Provides(SimpleQueue.class)
@RunDuring(Bootstrap.Stage.Final)
@DependsLocal(SimpleQueue.class)
public class SimpleQueueBootstrapper extends Bootstrapper.Simple {


  private static final Logger LOG = Logger.getLogger(SimpleQueueBootstrapper.class);
  private static final String SQS_CLOUD_WATCH_WORKFLOW_ID = "simplequeue-cloudwatch-workflow-01";

  @Override
  public boolean check() throws Exception {
    if (!super.check())
      return false;
    if (Topology.isEnabled( SimpleWorkflow.class ))  {
      try {
        if(!WorkflowClientManager.isRunning()) {
          WorkflowClientManager.start();
        }
      }catch(final Exception ex) {
        LOG.error("Failed to start SWF workers for Simple Queue Service", ex);
        return false;
      }
    } else {
      return false;
    }
    if (!runCloudWatchWorkflow())
      return false;
    return true;
  }

  private boolean runCloudWatchWorkflow() {
    try{
      SimpleQueueWorkflows.runCloudWatchWorkflow(SQS_CLOUD_WATCH_WORKFLOW_ID);
    }catch(final Exception ex) {
      LOG.error("Failed to run cloud watch workflow", ex);
      return false;
    }
    return true;
  }

  @Override
  public boolean disable() throws Exception {
    try {
      WorkflowClientManager.stop();
    }catch(final Exception ex) {
      LOG.error("Failed to stop SWF workers for ELB", ex);
      return false;
    }
    return true;
  }
}

