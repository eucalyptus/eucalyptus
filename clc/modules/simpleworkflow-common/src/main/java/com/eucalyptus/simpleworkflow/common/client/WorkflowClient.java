/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.simpleworkflow.common.client;

import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.flow.ActivityWorker;
import com.amazonaws.services.simpleworkflow.flow.WorkflowWorker;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.component.ComponentId;
import com.google.common.base.Supplier;

/**
 *
 */
public class WorkflowClient {

  private static final Logger logger = Logger.getLogger( WorkflowClient.class );

  private final boolean shutdownClient;
  private final AmazonSimpleWorkflow simpleWorkflow;
  private WorkflowWorker workflowWorker;
  private ActivityWorker activityWorker;

  public WorkflowClient( final Class<? extends ComponentId> componentIdClass,
                         final Supplier<User> user,
                         final String domain,
                         final String taskList,
                         final String workflowWorkerConfig,
                         final String activityWorkerConfig
  ) throws AuthException {
    this(
        componentIdClass,
        true,
        Config.buildClient( user ),
        domain,
        taskList,
        workflowWorkerConfig,
        activityWorkerConfig
    );
  }

  public WorkflowClient( final Class<? extends ComponentId> componentIdClass,
                         final AmazonSimpleWorkflow simpleWorkflow,
                         final String domain,
                         final String taskList,
                         final String workflowWorkerConfig,
                         final String activityWorkerConfig
  ) {
    this(
        componentIdClass,
        false,
        simpleWorkflow,
        domain,
        taskList,
        workflowWorkerConfig,
        activityWorkerConfig
    );
  }

  public WorkflowClient( final Class<? extends ComponentId> componentIdClass,
                         final boolean shutdownClient,
                         final AmazonSimpleWorkflow simpleWorkflow,
                         final String domain,
                         final String taskList,
                         final String workflowWorkerConfig,
                         final String activityWorkerConfig
  ) {
    this.shutdownClient = shutdownClient;
    this.simpleWorkflow = simpleWorkflow;
    try {
      this.workflowWorker = Config.buildWorkflowWorker(
          componentIdClass,
          this.simpleWorkflow,
          domain,
          taskList,
          workflowWorkerConfig
      );
      this.workflowWorker.setDisableServiceShutdownOnStop( !shutdownClient );
      this.activityWorker = Config.buildActivityWorker(
          componentIdClass,
          this.simpleWorkflow,
          domain,
          taskList,
          activityWorkerConfig
      );
      this.activityWorker.setDisableServiceShutdownOnStop( !shutdownClient );
    } catch( Throwable e ) {
      try {
        stop( );
      } catch ( InterruptedException e2 ) {
        logger.warn( "Interrupted during stop" );
      }
      throw e;
    }
  }

  public WorkflowClient(final Class<?>[] workflowImpl, final Class<?>[] activitiesImpl,
      final boolean shutdownClient,
      final AmazonSimpleWorkflow simpleWorkflow,
      final String domain,
      final String taskList,
      final String workflowWorkerConfig,
      final String activityWorkerConfig
      ) {
    this.shutdownClient = shutdownClient;
    this.simpleWorkflow = simpleWorkflow;
    try {
      if (workflowImpl != null && workflowImpl.length>0) {
        this.workflowWorker = Config.buildWorkflowWorker(
            workflowImpl,
            this.simpleWorkflow,
            domain,
            taskList,
            workflowWorkerConfig
            );
        this.workflowWorker.setDisableServiceShutdownOnStop( !shutdownClient );
      }
      if (activitiesImpl != null && activitiesImpl.length>0) {
        this.activityWorker = Config.buildActivityWorker(
            activitiesImpl,
            this.simpleWorkflow,
            domain,
            taskList,
            activityWorkerConfig
            );
        this.activityWorker.setDisableServiceShutdownOnStop( !shutdownClient );
      }
    } catch( Throwable e ) {
      try {
        stop( );
      } catch ( InterruptedException e2 ) {
        logger.warn( "Interrupted during stop" );
      }
      throw e;
    }
  }

  public AmazonSimpleWorkflow getAmazonSimpleWorkflow( ) {
    return simpleWorkflow;
  }

  public synchronized void start( ) {
    if(workflowWorker!=null )
      workflowWorker.start( );
    if(activityWorker!=null) {
      activityWorker.start();
    }
  }

  public synchronized void stop( ) throws InterruptedException {
    boolean waitForWorkflowWorker = false;
    if ( workflowWorker != null && workflowWorker.isRunning( ) ) {
      waitForWorkflowWorker = true;
      workflowWorker.shutdown( );
    }
    boolean waitForActivityWorker = false;
    if ( activityWorker != null && activityWorker.isRunning( ) ) {
      waitForActivityWorker = true;
      activityWorker.shutdown( );
    }
    if ( waitForWorkflowWorker &&  !workflowWorker.awaitTermination( 30, TimeUnit.SECONDS ) ) {
      if ( workflowWorker.isRunning( ) ) { // DecisionTaskPoller#awaitTermination is not implemented so we check again
        logger.warn( "Workflow worker not yet terminated." );
      }
    }
    if ( waitForActivityWorker && !activityWorker.awaitTermination( 30, TimeUnit.SECONDS ) ) {
      logger.warn( "Activity worker not yet terminated." );
    }
    if ( simpleWorkflow != null && shutdownClient ) {
      simpleWorkflow.shutdown( );
    }
  }

  public synchronized boolean isRunning( ) {
    if(activityWorker==null && workflowWorker==null) {
      return false;
    } else if (activityWorker!=null && !activityWorker.isRunning()) {
      return false;
    } else if (workflowWorker!=null && !workflowWorker.isRunning()) {
      return false;
    } else {
      return true;
    }
  }
}
