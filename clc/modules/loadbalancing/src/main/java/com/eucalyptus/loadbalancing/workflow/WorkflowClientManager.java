/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
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
