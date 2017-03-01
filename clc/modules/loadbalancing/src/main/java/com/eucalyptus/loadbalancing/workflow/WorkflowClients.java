/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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

import java.util.List;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.flow.WorkflowClientExternal;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.loadbalancing.LoadBalancingServiceProperties;
import com.eucalyptus.simpleworkflow.common.client.Config;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
public class WorkflowClients {
  private static final String TASK_LIST =
      LoadBalancingServiceProperties.SWF_TASKLIST;
  private static final String DOMAIN =
      LoadBalancingServiceProperties.SWF_DOMAIN;
  
  private static AmazonSimpleWorkflow getSimpleWorkflow() {
    AmazonSimpleWorkflow swfService = null;
    try{
      swfService = Config.buildClient(
          LoadBalancingAWSCredentialsProvider.LoadBalancingUserSupplier.INSTANCE
      );
      return swfService;
    }catch(final AuthException ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }
  
  static class WorkflowOptionsBuilder {
    WorkflowClientExternal client = null;
    WorkflowOptionsBuilder(final WorkflowClientExternal client) {
      this.client = client;
    }
    
    WorkflowOptionsBuilder withTag(final String account, final String loadbalancer) {
      final List<String> tagList = Lists.newArrayList();
      if(account!=null)
        tagList.add("account:"+account);
      if(loadbalancer!=null)
        tagList.add("loadbalancer:"+loadbalancer);
      this.client.getSchedulingOptions().setTagList(tagList);
      return this;
    }
   
    WorkflowOptionsBuilder withWorkflowTimeout(final long seconds) {
     this.client.getSchedulingOptions().setExecutionStartToCloseTimeoutSeconds(seconds);
      return this;
    }
    
    WorkflowOptionsBuilder withTaskTimeout(final long seconds) {
     this.client.getSchedulingOptions().setTaskStartToCloseTimeoutSeconds(seconds);
     return this;
    }
    
    WorkflowClientExternal build() {
      return this.client;
    }
  }
  
  public static CreateLoadBalancerWorkflowClientExternal getCreateLbWorkflow() {
    final CreateLoadBalancerWorkflowClientExternalFactory factory = 
        new CreateLoadBalancerWorkflowClientExternalFactoryImpl(getSimpleWorkflow(),  DOMAIN);
    
    return (CreateLoadBalancerWorkflowClientExternal) 
        new WorkflowOptionsBuilder(factory.getClient()).build();
  }

  public static DeleteLoadBalancerWorkflowClientExternal getDeleteLbWorkflow(final String account, final String loadbalancer) {
    final DeleteLoadBalancerWorkflowClientExternalFactory factory = 
        new DeleteLoadBalancerWorkflowClientExternalFactoryImpl(getSimpleWorkflow(),  DOMAIN);
    return (DeleteLoadBalancerWorkflowClientExternal)
        new WorkflowOptionsBuilder(factory.getClient())
        .withTag(account, loadbalancer)
        .build();       
  }
  
  public static CreateLoadBalancerListenersWorkflowClientExternal  getCreateListenersWorkflow(final String account, final String loadbalancer) {
    final CreateLoadBalancerListenersWorkflowClientExternalFactory factory =
        new CreateLoadBalancerListenersWorkflowClientExternalFactoryImpl(getSimpleWorkflow(), 
            DOMAIN);
    return (CreateLoadBalancerListenersWorkflowClientExternal)
        new WorkflowOptionsBuilder(factory.getClient())
        .withTag(account, loadbalancer)
        .build();
  }

  public static DeleteLoadBalancerListenersWorkflowClientExternal getDeleteListenersWorkflow(final String account, final String loadbalancer) {
    final DeleteLoadBalancerListenersWorkflowClientExternalFactory factory =
        new DeleteLoadBalancerListenersWorkflowClientExternalFactoryImpl(getSimpleWorkflow(),
            DOMAIN);
    return (DeleteLoadBalancerListenersWorkflowClientExternal) 
        new WorkflowOptionsBuilder(factory.getClient())
        .withTag(account, loadbalancer)
        .build();
  }
  
  public static EnableAvailabilityZoneWorkflowClientExternal getEnableAvailabilityZoneClient(final String account, final String loadbalancer) {
    final EnableAvailabilityZoneWorkflowClientExternalFactory factory =
        new EnableAvailabilityZoneWorkflowClientExternalFactoryImpl(getSimpleWorkflow(), DOMAIN);
    return (EnableAvailabilityZoneWorkflowClientExternal)
        new WorkflowOptionsBuilder(factory.getClient())
        .withTag(account, loadbalancer)
        .build();
  }
  
  public static DisableAvailabilityZoneWorkflowClientExternal getDisableAvailabilityZoneClient(final String account, final String loadbalancer) {
    final DisableAvailabilityZoneWorkflowClientExternalFactory factory =
        new DisableAvailabilityZoneWorkflowClientExternalFactoryImpl(getSimpleWorkflow(), DOMAIN);
    return (DisableAvailabilityZoneWorkflowClientExternal)
        new WorkflowOptionsBuilder(factory.getClient())
        .withTag(account, loadbalancer)
        .build();
  }
  
  public static ModifyLoadBalancerAttributesWorkflowClientExternal getModifyLoadBalancerAttributesClient(final String account, final String loadbalancer) {
    final ModifyLoadBalancerAttributesWorkflowClientExternalFactory factory =
        new ModifyLoadBalancerAttributesWorkflowClientExternalFactoryImpl(getSimpleWorkflow(), DOMAIN);
    return (ModifyLoadBalancerAttributesWorkflowClientExternal)
        new WorkflowOptionsBuilder(factory.getClient())
        .withTag(account,  loadbalancer)
        .build();
  }

  public static UpgradeLoadBalancerWorkflowClientExternal getUpgradeLoadBalancerClient(final String workflowId) {
    final UpgradeLoadBalancerWorkflowClientExternalFactory factory =
            new UpgradeLoadBalancerWorkflowClientExternalFactoryImpl(getSimpleWorkflow(), DOMAIN);
    return factory.getClient(workflowId);
  }
  
  public static ApplySecurityGroupsWorkflowClientExternal getApplySecurityGroupsClient(final String accountId, final String loadbalancer) {
    final ApplySecurityGroupsWorkflowClientExternalFactory factory =
        new ApplySecurityGroupsWorkflowClientExternalFactoryImpl(getSimpleWorkflow(), DOMAIN);
    return (ApplySecurityGroupsWorkflowClientExternal) 
        new WorkflowOptionsBuilder(factory.getClient())
        .withTag(accountId, loadbalancer)
        .build();
  }
  
  public static ModifyServicePropertiesWorkflowClientExternal getModifyServicePropertiesClient() {
    final ModifyServicePropertiesWorkflowClientExternalFactory factory =
        new ModifyServicePropertiesWorkflowClientExternalFactoryImpl(getSimpleWorkflow(), DOMAIN);
    return (ModifyServicePropertiesWorkflowClientExternal)
        new WorkflowOptionsBuilder(factory.getClient())
        .build();
  }
  
  public static UpdateLoadBalancerWorkflowClientExternal getUpdateLoadBalancerWorkflowClient(final String account, final String loadbalancer, final String workflowId) {
    final UpdateLoadBalancerWorkflowClientExternalFactory factory =
        new UpdateLoadBalancerWorkflowClientExternalFactoryImpl(getSimpleWorkflow(), DOMAIN);
    
    return (UpdateLoadBalancerWorkflowClientExternal)
        new WorkflowOptionsBuilder( factory.getClient(workflowId))
        .withTag(account, loadbalancer)
        .build();
  }
  
  public static InstanceStatusWorkflowClientExternal getinstanceStatusWorkflowClient(final String account, final String loadbalancer, final String workflowId) {
    final InstanceStatusWorkflowClientExternalFactory factory =
        new InstanceStatusWorkflowClientExternalFactoryImpl(getSimpleWorkflow(), DOMAIN);
    return (InstanceStatusWorkflowClientExternal)
        new WorkflowOptionsBuilder(factory.getClient(workflowId))
        .withTag(account, loadbalancer)
        .build();
  }
  
  public static CloudWatchPutMetricWorkflowClientExternal getPutMetricWorkflowClient(final String account, final String loadbalancer, final String workflowId) {
    final CloudWatchPutMetricWorkflowClientExternalFactory factory =
        new CloudWatchPutMetricWorkflowClientExternalFactoryImpl(getSimpleWorkflow(), DOMAIN);
    return (CloudWatchPutMetricWorkflowClientExternal)
        new WorkflowOptionsBuilder(factory.getClient(workflowId))
        .withTag(account, loadbalancer)
        .build();
  }
  
  public static LoadBalancingServiceHealthCheckWorkflowClientExternal getServiceStateWorkflowClient(final String workflowId) {
    final LoadBalancingServiceHealthCheckWorkflowClientExternalFactory factory =
        new LoadBalancingServiceHealthCheckWorkflowClientExternalFactoryImpl(getSimpleWorkflow(), DOMAIN);
    return factory.getClient(workflowId);
  }
}
