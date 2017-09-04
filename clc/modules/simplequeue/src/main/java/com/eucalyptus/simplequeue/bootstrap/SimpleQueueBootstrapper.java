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
    throwIfNotEnabled( SimpleWorkflow.class );
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

