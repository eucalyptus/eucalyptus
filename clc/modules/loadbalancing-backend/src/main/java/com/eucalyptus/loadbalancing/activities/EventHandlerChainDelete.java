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
package com.eucalyptus.loadbalancing.activities;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup.LoadBalancerSecurityGroupCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup.LoadBalancerSecurityGroupEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerZone;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.eucalyptus.loadbalancing.common.LoadBalancingBackend;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup.LoadBalancerAutoScalingGroupCoreView;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup.LoadBalancerAutoScalingGroupEntityTransform;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceCoreView;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceEntityTransform;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
public class EventHandlerChainDelete extends EventHandlerChain<DeleteLoadbalancerEvent> {
	private static Logger LOG  = Logger.getLogger( EventHandlerChainDelete.class );

	@Override
	public EventHandlerChain<DeleteLoadbalancerEvent> build() {
		this.insert(new DnsARecordRemover(this));
		this.insert(new AutoScalingGroupRemover(this));
		this.insert(new InstanceProfileRemover(this));
		this.insert(new IAMRoleRemover(this));
		this.insert(new SecurityGroupRemover(this));  /// remove security group
		return this;
	}
	
	private static class DnsARecordRemover extends AbstractEventHandler<DeleteLoadbalancerEvent>{
		DnsARecordRemover(
				EventHandlerChain<DeleteLoadbalancerEvent> chain) {
			super(chain);
		}

		@Override
		public void apply(DeleteLoadbalancerEvent evt)
				throws EventHandlerException {
			LoadBalancer lb;
			List<LoadBalancerServoInstanceCoreView> servos = Lists.newArrayList();
			try{ 
				lb= LoadBalancers.getLoadbalancer(evt.getLoadBalancerAccountNumber(), evt.getLoadBalancer());
				
				if(lb.getZones()!=null){
					for(final LoadBalancerZoneCoreView zoneView : lb.getZones()){
						LoadBalancerZone zone;
						try{
							zone = LoadBalancerZoneEntityTransform.INSTANCE.apply(zoneView);
						}catch(final Exception ex){
							continue;
						}
						servos.addAll(zone.getServoInstances());
					}
				}
			}catch(NoSuchElementException ex){
				return;
			}catch(Exception ex){
				LOG.warn("Failed to find the loadbalancer", ex);
				return;
			}

			// find ServoInstance
			for(LoadBalancerServoInstanceCoreView instance: servos){
				String	address = instance.getAddress();
				if(address==null || address.length()<=0)
					continue;
				try{
				  try ( final TransactionResource db =
				      Entities.transactionFor(LoadBalancerServoInstance.class)){
				    try{
				      final LoadBalancerServoInstance entity = 
				          Entities.uniqueResult(LoadBalancerServoInstance.named(instance.getInstanceId()));
				      entity.setDnsState(LoadBalancerServoInstance.DNS_STATE.Deregistered);
				      Entities.persist(entity);
				      db.commit();
				    }catch(final Exception ex){
				      LOG.error(String.format("failed to set servo instance(%s)'s dns state to deregistered", 
				          instance.getInstanceId()), ex);
				    }
				  }
				}catch(Exception ex){
					LOG.error("Error updating DNS registration state for balancer " + evt.getLoadBalancer( ), ex);
				}
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
		}
	}
	
	public static class AutoScalingGroupRemover extends AbstractEventHandler<DeleteLoadbalancerEvent> {
		protected AutoScalingGroupRemover(
				EventHandlerChain<DeleteLoadbalancerEvent> chain) {
			super(chain);
		}

		@Override
		public void apply(DeleteLoadbalancerEvent evt) throws EventHandlerException{
			LoadBalancer lb;
			try{ 
				lb= LoadBalancers.getLoadbalancer(evt.getLoadBalancerAccountNumber(), evt.getLoadBalancer());
			}catch(NoSuchElementException ex){
				return;
			}catch(Exception ex){
				LOG.warn("Failed to find the loadbalancer named " + evt.getLoadBalancer(), ex);
				return;
			}	
			final LoadBalancerAutoScalingGroupCoreView group = lb.getAutoScaleGroup();
			if(group == null){
				LOG.warn(String.format("Loadbalancer %s had no autoscale group associated with it", lb.getDisplayName()));
				return;
			}
			
			final String groupName = group.getName();
			String launchConfigName = null;
			
			try{
				final DescribeAutoScalingGroupsResponseType resp = EucalyptusActivityTasks.getInstance().describeAutoScalingGroups(Lists.newArrayList(groupName));
				final AutoScalingGroupType asgType = resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember().get(0);
				launchConfigName = asgType.getLaunchConfigurationName();
			}catch(final Exception ex){
				LOG.warn(String.format("Unable to find the launch config associated with %s", groupName));
			}
			
			try{
			  EucalyptusActivityTasks.getInstance().updateAutoScalingGroup(groupName, null, 0);
			}catch(final Exception ex){
			  LOG.warn(String.format("Unable to set desired capacity for %s", groupName), ex);
			}
			
			boolean error;
			final int NUM_DELETE_ASG_RETRY = 4;
			for(int i=0; i<NUM_DELETE_ASG_RETRY; i++){
			  try{
			    EucalyptusActivityTasks.getInstance().deleteAutoScalingGroup(groupName, true);
			    error = false;
			    // willl terminate all instances
			  }catch(final Exception ex){
			    error = true;
			    LOG.warn(String.format("Failed to delete autoscale group (%d'th attempt): %s", (i+1), groupName));
			    try{
			      long sleepMs = (i+1) * 500;
			      Thread.sleep(sleepMs);
			    }catch(final Exception ex2){
			    }
	      }
			  if(!error)
			    break;
			}
			
			if(launchConfigName!=null){
				try{
					EucalyptusActivityTasks.getInstance().deleteLaunchConfiguration(launchConfigName);
				}catch(Exception ex){
					LOG.warn("Failed to delete launch configuration " + launchConfigName, ex);
				}
			}

			LoadBalancerAutoScalingGroup scaleGroup = null;
			try{
				final LoadBalancerAutoScalingGroupCoreView groupView = lb.getAutoScaleGroup();
				scaleGroup = LoadBalancerAutoScalingGroupEntityTransform.INSTANCE.apply(groupView);
			}catch(final Exception ex){
				LOG.error("falied to update servo instance record", ex);
			}
			
			if(scaleGroup==null)
				return;
			
			try ( TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
				for(final LoadBalancerServoInstanceCoreView instanceView : scaleGroup.getServos()){
					LoadBalancerServoInstance instance;
					try{
						instance=LoadBalancerServoInstanceEntityTransform.INSTANCE.apply(instanceView);
					}catch(final Exception ex){
						continue;
					}
					final LoadBalancerServoInstance found = Entities.uniqueResult(instance);
					found.setAvailabilityZone(null);
					found.setAutoScalingGroup(null);
					// InService --> Retired
					// Pending --> Retired
					// OutOfService --> Retired
					// Error --> Retired
					found.setState(LoadBalancerServoInstance.STATE.Retired); 
					Entities.persist(found);
				}
				db.commit();
			}catch(final Exception ex){
				LOG.error("Failed to update servo instance record", ex);
			}
		
			// AutoScalingGroup record will be deleted as a result of cascaded delete
		}

		@Override
		public void rollback() throws EventHandlerException {
		}
	}
	
	 public static class InstanceProfileRemover extends AbstractEventHandler<DeleteLoadbalancerEvent> {
	    protected InstanceProfileRemover(
	        EventHandlerChain<DeleteLoadbalancerEvent> chain) {
	      super(chain);
	    }

	    @Override
	    public void apply(DeleteLoadbalancerEvent evt) throws EventHandlerException{
	      // remove a role from the instance profile
	      final String instanceProfileName = String.format("%s-%s-%s", EventHandlerChainNew.InstanceProfileSetup.INSTANCE_PROFILE_NAME_PREFIX,
	          evt.getLoadBalancerAccountNumber(), evt.getLoadBalancer());
	      final String roleName = String.format("%s-%s-%s", EventHandlerChainNew.IAMRoleSetup.ROLE_NAME_PREFIX, evt.getLoadBalancerAccountNumber(), evt.getLoadBalancer());
	      
	      try{
	        EucalyptusActivityTasks.getInstance().removeRoleFromInstanceProfile(instanceProfileName, roleName);
	      }catch(final Exception ex){
	        LOG.error(String.format("Failed to remove role(%s) from the instance profile(%s)", roleName, instanceProfileName), ex);
	      }
	      
	      // remove instance profile
	      try{
	        EucalyptusActivityTasks.getInstance().deleteInstanceProfile(instanceProfileName);
	      }catch(final Exception ex){
	        LOG.error(String.format("Failed to delete instance profile (%s)", instanceProfileName), ex);
	      }
	    }

      @Override
      public void rollback() throws EventHandlerException {
      }
	 }
	 
	 public static class IAMRoleRemover extends AbstractEventHandler<DeleteLoadbalancerEvent> {
	   protected IAMRoleRemover(
         EventHandlerChain<DeleteLoadbalancerEvent> chain) {
	     super(chain);
	   }

    @Override
    public void apply(DeleteLoadbalancerEvent evt) throws EventHandlerException {
      final String roleName = String.format("%s-%s-%s", EventHandlerChainNew.IAMRoleSetup.ROLE_NAME_PREFIX, evt.getLoadBalancerAccountNumber(), evt.getLoadBalancer());
      // delete role policy
      try{
        EucalyptusActivityTasks.getInstance().deleteRolePolicy(roleName, 
            EventHandlerChainNew.IAMPolicySetup.SERVO_ROLE_POLICY_NAME);
      }catch(final Exception ex){
        LOG.error("failed to delete role policy", ex);
      }
      
      // delete role
      try{
        EucalyptusActivityTasks.getInstance().deleteRole(roleName);
      }catch(final Exception ex){
        LOG.error("failed to delete role", ex);
      }
    }

    @Override
    public void rollback() throws EventHandlerException {
    }
	 }

	
	private static class SecurityGroupRemover extends AbstractEventHandler<DeleteLoadbalancerEvent>{
		SecurityGroupRemover(EventHandlerChain<DeleteLoadbalancerEvent> chain){
			super(chain);
		}

		@Override
		public void apply(DeleteLoadbalancerEvent evt)
				throws EventHandlerException {
			LoadBalancer lb;
			LoadBalancerSecurityGroupCoreView groupView = null;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getLoadBalancerAccountNumber(), evt.getLoadBalancer());
				if(lb.getGroup()!=null){
					groupView = lb.getGroup();
				}
			}catch(NoSuchElementException ex){
				return;
			}catch(Exception ex){
				LOG.error("Error while looking for loadbalancer with name="+evt.getLoadBalancer(), ex);
				return;
			}

			if ( lb.getVpcId( ) == null ) {
				LoadBalancerSecurityGroup group;
				try {
					group = LoadBalancerSecurityGroupEntityTransform.INSTANCE.apply( groupView );
				} catch ( final Exception ex ) {
					LOG.error( "Erorr while looking for loadbalancer group", ex );
					return;
				}

				try ( TransactionResource db = Entities.transactionFor( LoadBalancerSecurityGroup.class ) ) {
					final LoadBalancerSecurityGroup exist = Entities.uniqueResult( group );
					exist.setLoadBalancer( null );  // this allows the loadbalancer to be deleted
					exist.setState( LoadBalancerSecurityGroup.STATE.OutOfService );
					Entities.persist( exist );
					db.commit();
				} catch ( Exception ex ) {
					LOG.warn( "Could not disassociate the group from loadbalancer" );
				}
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
		}
	}
	
	/// delete security group if all its instances are terminated; delete from db
	public static class SecurityGroupCleanup implements EventListener<ClockTick> {
		public static void register( ) {
		      Listeners.register( ClockTick.class, new SecurityGroupCleanup() );
		    }

		@Override
		public void fireEvent(ClockTick event) {
			if (!( Bootstrap.isFinished() &&
			          Topology.isEnabledLocally( LoadBalancingBackend.class ) &&
			          Topology.isEnabled( Eucalyptus.class ) )) 
				return;
		
			/// find all security group whose member instances are empty
			List<LoadBalancerSecurityGroup> allGroups = null;
			try ( TransactionResource db = Entities.transactionFor( LoadBalancerSecurityGroup.class ) ) {
				allGroups = Entities.query(LoadBalancerSecurityGroup.withState(LoadBalancerSecurityGroup.STATE.OutOfService));
				db.commit();
			}catch(Exception ex){ /* retry later */ }
			if(allGroups==null || allGroups.size()<=0)
				return;
			final List<LoadBalancerSecurityGroup> toDelete = Lists.newArrayList();
			for(LoadBalancerSecurityGroup group : allGroups){
				Collection<LoadBalancerServoInstanceCoreView> instances = group.getServoInstances();
				if(instances == null || instances.size()<=0)
					toDelete.add(group);
			}
			
			/// delete them from euca
			for(LoadBalancerSecurityGroup group : toDelete){
				try{
					EucalyptusActivityTasks.getInstance().deleteSystemSecurityGroup( group.getName() );
					LOG.info("deleted security group: "+group.getName());
				}catch(Exception ex){
					LOG.warn("failed to delete the security group from eucalyptus",ex);
				}
			}
			try ( final TransactionResource db2 = Entities.transactionFor( LoadBalancerSecurityGroup.class ) ) {
				for(LoadBalancerSecurityGroup group: toDelete){
					LoadBalancerSecurityGroup g = 
							Entities.uniqueResult(group);
					Entities.delete(g);
				}
				db2.commit();
			}catch(NoSuchElementException ex){
				// nothing to delete
			}catch(Exception ex){
				LOG.warn("failed to delete the securty group from entity", ex);
			}
		}
	}
	
	/// checks the latest status of the servo instances and delete from db if terminated
	public static class ServoInstanceCleanup implements EventListener<ClockTick> {
		public static void register( ) {
	      Listeners.register( ClockTick.class, new ServoInstanceCleanup() );
	    }

		@Override
		public void fireEvent(ClockTick event) {
			if (!( Bootstrap.isFinished() &&
			          Topology.isEnabledLocally( LoadBalancingBackend.class ) &&
			          Topology.isEnabled( Eucalyptus.class ) )) 
				return;
	
			// find all OutOfService instances
			List<LoadBalancerServoInstance> retired=null;
			try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
				LoadBalancerServoInstance sample = 
						LoadBalancerServoInstance.withState(LoadBalancerServoInstance.STATE.Retired.name());
				retired = Entities.query(sample);
				sample =  LoadBalancerServoInstance.withState(LoadBalancerServoInstance.STATE.Error.name());
				retired.addAll(Entities.query(sample));
				db.commit();
			}catch(Exception ex){
				LOG.warn("failed to query loadbalancer servo instance", ex);
			}

			if(retired == null || retired.size()<=0)
				return;
			final List<LoadBalancerServoInstance> retiredAndDnsClean = Lists.newArrayList();
			for(final LoadBalancerServoInstance instance: retired){
  		/// make sure DNS is deregistered
        if(! LoadBalancerServoInstance.DNS_STATE.Registered.equals(instance.getDnsState()))
          retiredAndDnsClean.add(instance);
			}
			/// for each:
			// describe instances
			final List<String> param = Lists.newArrayList();
			final Map<String, String> latestState = Maps.newHashMap();
			for(final LoadBalancerServoInstance instance : retiredAndDnsClean){
			  /// 	call describe instance
			  String instanceId = instance.getInstanceId();
			  if(instanceId == null)
			    continue;
			  param.clear();
			  param.add(instanceId);
			  String instanceState;
			  try{
			    final List<RunningInstancesItemType> result =
			        EucalyptusActivityTasks.getInstance().describeSystemInstances(param);
			    if (result.isEmpty())
			      instanceState= "terminated";
			    else
			      instanceState = result.get(0).getStateName();
			  }catch(final Exception ex){
			    LOG.warn("failed to query instances", ex);
			    continue;
			  }
			  latestState.put(instanceId, instanceState);
			}
			
			// if state==terminated or describe instances return no result,
			//    delete the database record
			for(String instanceId : latestState.keySet()){
			  String state = latestState.get(instanceId);
			  if(state.equals("terminated")){
			    try ( final TransactionResource db2 = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
			      LoadBalancerServoInstance toDelete = Entities.uniqueResult(LoadBalancerServoInstance.named(instanceId));
			      Entities.delete(toDelete);
			      db2.commit();
			    }catch(Exception ex){
			      LOG.trace( "Unable to delete load balancer servo instance: " + ex );
			    }
			  }
			}
		}
	}
}
