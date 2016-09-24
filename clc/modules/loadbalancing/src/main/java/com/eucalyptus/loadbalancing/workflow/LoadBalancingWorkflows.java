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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionAlreadyStartedException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneEntityTransform;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
import com.eucalyptus.loadbalancing.common.msgs.Listener;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerAttributes;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;

import com.google.common.base.Supplier;


/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
/// facade for workflows
public class LoadBalancingWorkflows {
  private static Logger LOG  = Logger.getLogger( LoadBalancingWorkflows.class );

  public static boolean createLoadBalancerSync(final String accountId, final String loadbalancer, final List<String> availabilityZones) {
    final  Future<Boolean> task = Threads.enqueue(LoadBalancing.class, LoadBalancingWorkflows.class,  createLoadBalancerImpl(accountId, loadbalancer, availabilityZones ));
    try{ 
      return task.get();
    }catch(final Exception ex) {
      return false;
    }
  }

  public static void createLoadBalancerAsync(final String accountId, final String loadbalancer, final List<String> availabilityZones) {
   Threads.enqueue(LoadBalancing.class, LoadBalancingWorkflows.class,  createLoadBalancerImpl(accountId, loadbalancer, availabilityZones ));
  }
  
  private static Callable<Boolean> createLoadBalancerImpl(final String accountId, final String loadbalancer, final List<String> availabilityZones) { 
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        final CreateLoadBalancerWorkflowClientExternal workflow =
            WorkflowClients.getCreateLbWorkflow();
        workflow.createLoadBalancer(accountId, loadbalancer, availabilityZones.toArray(new String[availabilityZones.size()]));
        return waitFor(new Supplier<ElbWorkflowState>() {
          @Override
          public ElbWorkflowState get() {
            return workflow.getState();
          } 
        });
      }
    };
  }
  
  public static boolean deleteLoadBalancerSync(final String accountId, final String loadbalancer) {
    final  Future<Boolean> task = Threads.enqueue(LoadBalancing.class, LoadBalancingWorkflows.class,  deleteLoadBalancerImpl(accountId, loadbalancer));
    try{ 
      return task.get();
    }catch(final Exception ex) {
      return false;
    }
  }
  
  public static void deleteLoadBalancerAsync(final String accountId, final String loadbalancer) {
    Threads.enqueue(LoadBalancing.class, LoadBalancingWorkflows.class,  deleteLoadBalancerImpl(accountId, loadbalancer));
  }
  
  private static Callable<Boolean> deleteLoadBalancerImpl(final String accountId, final String loadbalancer) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        final DeleteLoadBalancerWorkflowClientExternal workflow =
            WorkflowClients.getDeleteLbWorkflow(accountId, loadbalancer);
        workflow.deleteLoadBalancer(accountId, loadbalancer);
        return waitFor(new Supplier<ElbWorkflowState>() {
          @Override
          public ElbWorkflowState get() {
            return workflow.getState();
          } 
        });
      }
    };
  }
  
  public static boolean createListenersSync(final String accountId, final String loadbalancer, final List<Listener> listeners) {
    final  Future<Boolean> task = Threads.enqueue(LoadBalancing.class, LoadBalancingWorkflows.class,  createListenersImpl(accountId, loadbalancer, listeners ));
    try{ 
      return task.get();
    }catch(final Exception ex) {
      return false;
    }
  }
  
  public static void createListenersAsync(final String accountId, final String loadbalancer, final List<Listener> listeners) {
    Threads.enqueue(LoadBalancing.class, LoadBalancingWorkflows.class,  createListenersImpl(accountId, loadbalancer, listeners ));
  }

  private static Callable<Boolean> createListenersImpl(final String accountId, final String loadbalancer, final List<Listener> listeners) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        final CreateLoadBalancerListenersWorkflowClientExternal  workflow =  
            WorkflowClients.getCreateListenersWorkflow(accountId, loadbalancer);
        workflow.createLoadBalancerListeners(accountId, loadbalancer, 
            listeners.toArray(new Listener[listeners.size()]));
        return waitFor(new Supplier<ElbWorkflowState>() {
          @Override
          public ElbWorkflowState get() {
            return workflow.getState();
          } 
        });
      }
    };
  }
  
  public static Boolean deleteListenersSync(final String accountId, final String loadbalancer, final List<Integer> ports) {
    final  Future<Boolean> task = Threads.enqueue(LoadBalancing.class, LoadBalancingWorkflows.class,  
        deleteListenersImpl(accountId, loadbalancer, ports ));
    try{ 
      return task.get();
    }catch(final Exception ex) {
      return false;
    }
  }
  
  public static void deleteListenersAsync(final String accountId, final String loadbalancer, final List<Integer> ports) {
    Threads.enqueue(LoadBalancing.class, LoadBalancingWorkflows.class,
        deleteListenersImpl(accountId, loadbalancer, ports ));
  }

  private static Callable<Boolean> deleteListenersImpl(final String accountId, final String loadbalancer, final List<Integer> ports) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        final DeleteLoadBalancerListenersWorkflowClientExternal  workflow =  
            WorkflowClients.getDeleteListenersWorkflow(accountId, loadbalancer);
        workflow.deleteLoadBalancerListeners(accountId, loadbalancer, ports.toArray(new Integer[ports.size()]));
        return waitFor(new Supplier<ElbWorkflowState>() {
          @Override
          public ElbWorkflowState get() {
            return workflow.getState();
          } 
        }); 
      }
    };
  }

  public static boolean enableZonesSync(final String accountId, final String loadbalancer, 
      final List<String> availabilityZones, final Map<String,String> zoneToSubnetIdMap) {
    final  Future<Boolean> task = Threads.enqueue(LoadBalancing.class, LoadBalancingWorkflows.class,  enableZonesImpl(accountId, loadbalancer, availabilityZones, zoneToSubnetIdMap ));
    try{ 
      return task.get();
    }catch(final Exception ex) {
      return false;
    }
  }
  
  public static void enableZonesAsync(final String accountId, final String loadbalancer, 
      final List<String> availabilityZones, final Map<String,String> zoneToSubnetIdMap) {
    Threads.enqueue(LoadBalancing.class, LoadBalancingWorkflows.class,  enableZonesImpl(accountId, loadbalancer, availabilityZones, zoneToSubnetIdMap ));
  }
  
  private static Callable<Boolean> enableZonesImpl(final String accountId, final String loadbalancer, 
      final List<String> availabilityZones, final Map<String,String> zoneToSubnetIdMap) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        // TODO Auto-generated method stub            
        final EnableAvailabilityZoneWorkflowClientExternal workflow = 
            WorkflowClients.getEnableAvailabilityZoneClient(accountId,loadbalancer);
        workflow.enableAvailabilityZone(accountId, loadbalancer, 
            availabilityZones, 
            zoneToSubnetIdMap);
        return waitFor(new Supplier<ElbWorkflowState>() {
          @Override
          public ElbWorkflowState get() {
            return workflow.getState();
          } 
        });
      }
    };
  }
  
  public static Boolean disableZonesSync(final String accountId, final String loadbalancer,
      final List<String> availabilityZones) {
    final  Future<Boolean> task = Threads.enqueue(LoadBalancing.class, LoadBalancingWorkflows.class,  disableZonesImpl(accountId, loadbalancer, availabilityZones ));
    try{ 
      return task.get();
    }catch(final Exception ex) {
      return false;
    }
  }
  
  public static void disableZonesAsync(final String accountId, final String loadbalancer,
      final List<String> availabilityZones) {
    Threads.enqueue(LoadBalancing.class, LoadBalancingWorkflows.class,  disableZonesImpl(accountId, loadbalancer, availabilityZones ));
  }
  
  private static Callable<Boolean> disableZonesImpl(final String accountId, final String loadbalancer, 
      final List<String> availabilityZones) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        final DisableAvailabilityZoneWorkflowClientExternal workflow = 
            WorkflowClients.getDisableAvailabilityZoneClient(accountId, loadbalancer);
        workflow.disableAvailabilityZone(accountId, loadbalancer, availabilityZones);
        return waitFor(new Supplier<ElbWorkflowState>() {
          @Override
          public ElbWorkflowState get() {
            return workflow.getState();
          } 
        });
      }
    };
  }
  
  public static Boolean modifyLoadBalancerAttributesSync(final String accountId, final String loadbalancer, 
      final LoadBalancerAttributes attributes) {
    final  Future<Boolean> task = 
        Threads.enqueue(LoadBalancing.class, LoadBalancingWorkflows.class,  modifyLoadBalancerAttributesImpl(accountId, loadbalancer, attributes ));
    try{ 
      return task.get();
    }catch(final Exception ex) {
      return false;
    }
  }
  
  public static void modifyLoadBalancerAttributesASync(final String accountId, final String loadbalancer, 
      final LoadBalancerAttributes attributes) {
    Threads.enqueue(LoadBalancing.class, LoadBalancingWorkflows.class,  modifyLoadBalancerAttributesImpl(accountId, loadbalancer, attributes ));
  }
  
  private static Callable<Boolean> modifyLoadBalancerAttributesImpl(final String accountId, final String loadbalancer, 
      final LoadBalancerAttributes attributes) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        final ModifyLoadBalancerAttributesWorkflowClientExternal workflow = 
            WorkflowClients.getModifyLoadBalancerAttributesClient(accountId, loadbalancer);
        workflow.modifyLoadBalancerAttributes(accountId, loadbalancer, attributes);
        return waitFor(new Supplier<ElbWorkflowState>() {
          @Override
          public ElbWorkflowState get() {
            return workflow.getState();
          } 
        });
      }
    };
  }
  
  public static Boolean applySecurityGroupsSync(final String accountId, final String loadbalancer,
      final Map<String,String> groupIdToNameMap) {
    final  Future<Boolean> task = 
        Threads.enqueue(LoadBalancing.class, LoadBalancingWorkflows.class,  
            applySecurityGroupsImpl(accountId, loadbalancer, groupIdToNameMap ));
    try{ 
      return task.get();
    }catch(final Exception ex) {
      return false;
    }
  }
  
  public static void applySecurityGroupsAsync(final String accountId, final String loadbalancer,
      final Map<String,String> groupIdToNameMap) {
    Threads.enqueue(LoadBalancing.class, LoadBalancingWorkflows.class,  
        applySecurityGroupsImpl(accountId, loadbalancer, groupIdToNameMap ));
  }

  private static Callable<Boolean> applySecurityGroupsImpl(final String accountId, final String loadbalancer,
      final Map<String,String> groupIdToNameMap) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        final ApplySecurityGroupsWorkflowClientExternal workflow =
            WorkflowClients.getApplySecurityGroupsClient(accountId, loadbalancer);
        workflow.applySecurityGroups(accountId,  loadbalancer,  groupIdToNameMap);
        return waitFor(new Supplier<ElbWorkflowState>() {
          @Override
          public ElbWorkflowState get() {
            return workflow.getState();
          } 
        });
      }
    };
  }
  
  public static Boolean modifyServicePropertiesSync(final String emi, final String instanceType,
      final String keyname, final String initScript ) {
    final  Future<Boolean> task = 
        Threads.enqueue(LoadBalancing.class, LoadBalancingWorkflows.class,  
            modifyServicePropertiesImpl( emi, instanceType, keyname, initScript ));
    try{ 
      return task.get();
    }catch(final Exception ex) {
      LOG.error("Failed run property modification workflow", ex);
      return false;
    }
  }
  
  public static void modifyServicePropertiesAsync( final String emi, final String instanceType,
      final String keyname, final String initScript ) {
    Threads.enqueue(LoadBalancing.class, LoadBalancingWorkflows.class,  
        modifyServicePropertiesImpl( emi, instanceType, keyname, initScript ));
  }
      
  private static Callable<Boolean> modifyServicePropertiesImpl( final String emi, final String instanceType,
      final String keyname, final String initScript ) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        final ModifyServicePropertiesWorkflowClientExternal workflow =
            WorkflowClients.getModifyServicePropertiesClient();
        workflow.modifyServiceProperties(emi, instanceType, keyname, initScript);
        return waitFor(new Supplier<ElbWorkflowState>() {
          @Override
          public ElbWorkflowState get() {
            return workflow.getState();
          } 
        });
      }
    };
  }
  
  private static Boolean waitFor(Supplier<ElbWorkflowState> supplier) {
    ElbWorkflowState state = ElbWorkflowState.WORKFLOW_RUNNING;
    do {
      try{
        Thread.sleep(500);
      }catch(final Exception ex){
        ;
      }
      state = supplier.get();
    } while (state == null || state == ElbWorkflowState.WORKFLOW_RUNNING );
    return state == ElbWorkflowState.WORKFLOW_SUCCESS;
  }
  
  private static String getUpdateLoadBalancerWorkflowId(final String accountId, final String loadbalancer) {
    return String.format("update-loadbalancer-%s-%s", accountId, loadbalancer);
  }
  public static void runUpdateLoadBalancer(final String accountId, final String loadbalancer) {
    final String workflowId = getUpdateLoadBalancerWorkflowId(accountId, loadbalancer);
    try{
      final UpdateLoadBalancerWorkflowClientExternal workflow = 
          WorkflowClients.getUpdateLoadBalancerWorkflowClient(accountId, loadbalancer, workflowId);  
      workflow.updateLoadBalancer(accountId, loadbalancer);  
    }catch(final WorkflowExecutionAlreadyStartedException ex ) {
      ;
    }catch(final Exception ex) {
      throw Exceptions.toUndeclared("Failed to start update-loadbalancer workflow", ex);
    }
  }
  
  public static void cancelUpdateLoadBalancer(final String accountId, final String loadbalancer) {
    final String workflowId = getUpdateLoadBalancerWorkflowId(accountId, loadbalancer);
    try{
      final UpdateLoadBalancerWorkflowClientExternal workflow =
          WorkflowClients.getUpdateLoadBalancerWorkflowClient(accountId, loadbalancer, workflowId);
      workflow.requestCancelWorkflowExecution();
      LOG.debug(String.format("Cancelled update-loadbalancer workflow for %s-%s", 
          accountId, loadbalancer));
    }catch(final Exception ex) {
      LOG.error("Failed to cance update-loadbalancer workflow", ex);
    }
  }

  public static void updateLoadBalancer(final String accountId, final String loadbalancer) {
    final String workflowId = getUpdateLoadBalancerWorkflowId(accountId, loadbalancer);
    try{
      final UpdateLoadBalancerWorkflowClientExternal workflow =
          WorkflowClients.getUpdateLoadBalancerWorkflowClient(accountId, loadbalancer, workflowId);
      workflow.updateImmediately();
    }catch(final Exception ex) {
      LOG.error("Failed to signal update-loadbalancer workflow", ex);
    }
  }

  /// instanceId: ID of ELB VM
  public static void updateLoadBalancer(final String instanceId) {
    String accountId = null;
    String loadbalancer = null;
    try ( final TransactionResource db = 
        Entities.transactionFor( LoadBalancerServoInstance.class )) {
      final LoadBalancerServoInstance sample = LoadBalancerServoInstance.named(instanceId);
      final LoadBalancerServoInstance entity = Entities.uniqueResult(sample);
      final LoadBalancerZoneCoreView zoneView = entity.getAvailabilityZone();
      final LoadBalancerZone zone = LoadBalancerZoneEntityTransform.INSTANCE.apply(zoneView);
      final LoadBalancerCoreView lbView = zone.getLoadbalancer();
      accountId = lbView.getOwnerAccountNumber();
      loadbalancer = lbView.getDisplayName();
    }catch (final NoSuchElementException ex) {
      throw Exceptions.toUndeclared(ex);
    }catch (final Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
    updateLoadBalancer(accountId, loadbalancer);
  }
  
  private static String getInstanceStatusWorkflowId(final String accountId, final String loadbalancer) {
    return String.format("instance-status-%s-%s", accountId, loadbalancer);
  }
  
  public static void runInstanceStatusPolling(final String accountId, final String loadbalancer) {
    final String workflowId = getInstanceStatusWorkflowId(accountId, loadbalancer);
    try{
      final InstanceStatusWorkflowClientExternal workflow =
          WorkflowClients.getinstanceStatusWorkflowClient(accountId, loadbalancer, workflowId);
      workflow.pollInstanceStatus(accountId, loadbalancer); // instances);
    }catch(final WorkflowExecutionAlreadyStartedException ex ) {
      ;
    }catch(final Exception ex) {
      throw Exceptions.toUndeclared("Failed to start instance status polling workflow", ex);
    }
  }

  public static void cancelInstanceStatusPolling(final String accountId, final String loadbalancer) {
    final String workflowId = getInstanceStatusWorkflowId(accountId, loadbalancer);
    try{
      final InstanceStatusWorkflowClientExternal workflow =
          WorkflowClients.getinstanceStatusWorkflowClient(accountId, loadbalancer, workflowId);
      workflow.requestCancelWorkflowExecution();
      LOG.debug(String.format("Cancelled instance polling workflow for %s-%s", 
          accountId, loadbalancer));
    }catch(final Exception ex) {
      LOG.error("Failed to cancel instance-polling workflow", ex);
    }
  }

  private static String getCloudWatchPutMetricWorkflowId(final String accountId, final String loadbalancer) {
    return String.format("cloudwatch-put-metric-%s-%s", accountId, loadbalancer);
  }

  public static void runCloudWatchPutMetric(final String accountId, final String loadbalancer) {
    final String workflowId = getCloudWatchPutMetricWorkflowId(accountId, loadbalancer);
    try{
      final CloudWatchPutMetricWorkflowClientExternal workflow =
          WorkflowClients.getPutMetricWorkflowClient(accountId, loadbalancer, workflowId);
      workflow.putCloudWatchMetric(accountId, loadbalancer);
    }catch(final WorkflowExecutionAlreadyStartedException ex ) {
      ;
    }catch(final Exception ex) {
      throw Exceptions.toUndeclared("Failed to start cloud-watch put-metric workflow", ex);
    }
  }

  public static void cancelCloudWatchPutMetric(final String accountId, final String loadbalancer) {
    final String workflowId = getCloudWatchPutMetricWorkflowId(accountId, loadbalancer);
    if(workflowId != null) {
      try{
        final CloudWatchPutMetricWorkflowClientExternal workflow =
            WorkflowClients.getPutMetricWorkflowClient(accountId, loadbalancer, workflowId);
        workflow.requestCancelWorkflowExecution();
        LOG.debug(String.format("Cancelled put-metric workflow for %s-%s", 
            accountId, loadbalancer));
      }catch(final Exception ex) {
        LOG.error("Failed to cancel put-metric workflow", ex);
      }
    }
  }
  
  public static void runServiceStateWorkflow(final String workflowId) {
    try{
      final LoadBalancingServiceHealthCheckWorkflowClientExternal workflow =
          WorkflowClients.getServiceStateWorkflowClient(workflowId);
      workflow.performServiceHealthCheck();
    }catch(final WorkflowExecutionAlreadyStartedException ex ) {
      ;
    }catch(final Exception ex) {
      throw Exceptions.toUndeclared("Failed to start loadbalancing service state workflow", ex);
    }
  }
}
