/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.loadbalancing.workflow;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.model.DomainInfo;
import com.amazonaws.services.simpleworkflow.model.DomainInfos;
import com.amazonaws.services.simpleworkflow.model.ListDomainsRequest;
import com.amazonaws.services.simpleworkflow.model.RegisterDomainRequest;
import com.amazonaws.services.simpleworkflow.model.RegistrationStatus;

import com.eucalyptus.loadbalancing.LoadBalancingServiceProperties;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
import com.eucalyptus.simpleworkflow.common.client.Config;
import com.eucalyptus.simpleworkflow.common.client.WorkflowClient;
import org.apache.log4j.Logger;

/**
 *
 */
public class WorkflowClientManager {
  private static Logger LOG  = Logger.getLogger( WorkflowClientManager.class );

  private static volatile WorkflowClient workflowClient;
  public static synchronized AmazonSimpleWorkflow getSimpleWorkflowClient( ) {
    return workflowClient.getAmazonSimpleWorkflow( );
  }

  private static final String TASK_LIST = LoadBalancingServiceProperties.SWF_TASKLIST;
  private static final String DOMAIN = LoadBalancingServiceProperties.SWF_DOMAIN;

  public static boolean isRunning() {
    return workflowClient != null && workflowClient.isRunning();
  }

  public static void start( ) throws Exception {
    //// FIXME: temporary config for only testing
     final AmazonSimpleWorkflow simpleWorkflowClient;
      simpleWorkflowClient = Config.buildClient(
          LoadBalancingAWSCredentialsProvider.LoadBalancingUserSupplier.INSTANCE,
          LoadBalancingServiceProperties.SWF_CLIENT_CONFIG );

    workflowClient = new WorkflowClient(
        LoadBalancing.class,
        simpleWorkflowClient,
        DOMAIN,
        TASK_LIST,
        LoadBalancingServiceProperties.SWF_WORKFLOW_WORKER_CONFIG,
        LoadBalancingServiceProperties.SWF_ACTIVITY_WORKER_CONFIG );
    workflowClient.start( );
    LOG.debug("LoadBalancing SWF client has started");
  }

  public static void stop( ) throws Exception {
    if ( workflowClient != null ) {
      workflowClient.stop( );
    }
  }

  private static boolean isDomainRegistered(final AmazonSimpleWorkflow client) {
    final ListDomainsRequest req = new ListDomainsRequest();
    req.setRegistrationStatus(RegistrationStatus.REGISTERED);
    final DomainInfos domains = client.listDomains(req);
    if (domains == null || domains.getDomainInfos() == null) {
      return false;
    }
    for (final DomainInfo dom : domains.getDomainInfos()) {
      if (DOMAIN.equals(dom.getName()) && "REGISTERED".equals(dom.getStatus())) {
        return true;
      }
    }
    return false;
  }

  private static void registerDomain(final AmazonSimpleWorkflow client) {
    final RegisterDomainRequest req = new RegisterDomainRequest();
    req.setName(DOMAIN);
    req.setDescription("SWF Domain for Loadbalancing service");
    req.setWorkflowExecutionRetentionPeriodInDays("1");
    client.registerDomain(req);
  }
}
