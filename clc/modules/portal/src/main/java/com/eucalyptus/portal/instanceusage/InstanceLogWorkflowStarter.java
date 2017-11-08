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
package com.eucalyptus.portal.instanceusage;

import com.amazonaws.services.simpleworkflow.flow.StartWorkflowOptions;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionAlreadyStartedException;
import com.eucalyptus.portal.WorkflowClients;
import com.eucalyptus.portal.workflow.InstanceLogWorkflowClientExternal;
import com.eucalyptus.simpleworkflow.common.client.WorkflowStarter;
import org.apache.log4j.Logger;

public class InstanceLogWorkflowStarter implements WorkflowStarter{
  private static Logger LOG     =
          Logger.getLogger(  InstanceLogWorkflowStarter.class );

  public static final String BILLING_INSTANCE_LOG_HOURLY_WORKFLOW_ID =
          "billing-instance-log-hourly-workflow-01";
  @Override
  public void start() {
    try {
      final InstanceLogWorkflowClientExternal workflow =
              WorkflowClients.getInstanceLogWorkflow(BILLING_INSTANCE_LOG_HOURLY_WORKFLOW_ID);
      final StartWorkflowOptions options = new StartWorkflowOptions();
      options.setExecutionStartToCloseTimeoutSeconds(720L);
      options.setTaskStartToCloseTimeoutSeconds(60L);
      workflow.logInstanceHourly(options);
    } catch (final WorkflowExecutionAlreadyStartedException ex) {
      ;
    } catch (final Exception ex) {
      LOG.error("Failed to start the workflow that aggregates instance hour reports");
    }
  }

  @Override
  public String name() {
    return "INSTANCE_LOG_HOURLY_WORKFLOW";
  }
}
