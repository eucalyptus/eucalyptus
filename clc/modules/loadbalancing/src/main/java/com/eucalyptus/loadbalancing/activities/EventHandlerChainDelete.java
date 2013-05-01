/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancerDnsRecord;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup;
import com.eucalyptus.loadbalancing.LoadBalancerZone;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;

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
			LoadBalancer lb = null;
			List<LoadBalancerServoInstance> servos = Lists.newArrayList();
			try{ 
				lb= LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
				if(lb.getZones()!=null){
					for(LoadBalancerZone zone : lb.getZones()){
						servos.addAll(zone.getServoInstances());
					}
				}
			}catch(NoSuchElementException ex){
				return;
			}catch(Exception ex){
				LOG.warn("Failed to find the loadbalancer", ex);
				return;
			}
			final LoadBalancerDnsRecord dns = lb.getDns();
			
			// find ServoInstance
			for(LoadBalancerServoInstance instance: servos){
				String	address = instance.getAddress();
				if(address==null || address.length()<=0)
					continue;
				try{
					EucalyptusActivityTasks.getInstance().removeARecord(dns.getZone(), dns.getName(), address);
				}catch(Exception ex){
					LOG.error(String.format("failed to remove dns a record (zone=%s, name=%s, address=%s)", 
							dns.getZone(), dns.getName(), address), ex);
				}
			}
			
			try{
				EucalyptusActivityTasks.getInstance().removeMultiARecord(dns.getZone(), dns.getName());
			}catch(Exception ex){
				LOG.error(String.format("failed to remove dns a record (zone=%s,  name=%s)", dns.getZone(), dns.getName()), ex);
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
			;
		}
	}
	
	public static class AutoScalingGroupRemover extends AbstractEventHandler<DeleteLoadbalancerEvent> {
		protected AutoScalingGroupRemover(
				EventHandlerChain<DeleteLoadbalancerEvent> chain) {
			super(chain);
		}

		@Override
		public void apply(DeleteLoadbalancerEvent evt) throws EventHandlerException{
			LoadBalancer lb = null;
			try{ 
				lb= LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
			}catch(NoSuchElementException ex){
				return;
			}catch(Exception ex){
				LOG.warn("Failed to find the loadbalancer named " + evt.getLoadBalancer(), ex);
				return;
			}	
			LoadBalancerAutoScalingGroup group = lb.getAutoScaleGroup();
			if(group == null){
				LOG.warn(String.format("Loadbalancer %s had no autoscale group associated with it", lb.getDisplayName()));
				return;
			}
			
			final String groupName = group.getName();
			final String launchConfigName = group.getLaunchConfigName();
			boolean error = false;
			try{
				EucalyptusActivityTasks.getInstance().deleteAutoScalingGroup(groupName, true);
				// willl terminate all instances
			}catch(Exception ex){
				LOG.warn("Failed to delete autoscale group "+groupName, ex);
				error = true;
			}
			
			try{
				EucalyptusActivityTasks.getInstance().deleteLaunchConfiguration(launchConfigName);
			}catch(Exception ex){
				LOG.warn("Failed to delete launch configuration " + launchConfigName, ex);
				error = true;
			}

			EntityTransaction db = Entities.get( LoadBalancerServoInstance.class );
			try{
				for(LoadBalancerServoInstance instance : lb.getAutoScaleGroup().getServos()){
					final LoadBalancerServoInstance found = Entities.uniqueResult(instance);
					found.setDns(null);
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
			}catch(Exception ex){
				db.rollback();
				LOG.error("Failed to update servo instance record", ex);
			}
		
			// AutoScalingGroup record will be deleted as a result of cascaded delete
		}

		@Override
		public void rollback() throws EventHandlerException {
			;
		}
	}
	
	private static class SecurityGroupRemover extends AbstractEventHandler<DeleteLoadbalancerEvent>{
		SecurityGroupRemover(EventHandlerChain<DeleteLoadbalancerEvent> chain){
			super(chain);
		}

		@Override
		public void apply(DeleteLoadbalancerEvent evt)
				throws EventHandlerException {
			LoadBalancer lb = null;
			LoadBalancerSecurityGroup group = null;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
				if(lb.getGroup()!=null){
					group = lb.getGroup();
				}
			}catch(NoSuchElementException ex){
				return;
			}catch(Exception ex){
				LOG.error("Error while looking for loadbalancer with name="+evt.getLoadBalancer(), ex);
				return;
			}

			if(group!= null){
				final EntityTransaction db = Entities.get( LoadBalancerSecurityGroup.class );
				try{
					final LoadBalancerSecurityGroup exist = Entities.uniqueResult(group);
					exist.setLoadBalancer(null);	// this allows the loadbalancer to be deleted
					exist.setState(LoadBalancerSecurityGroup.STATE.OutOfService);
					Entities.persist(exist);
					db.commit();
				}catch(Exception ex){
					db.rollback();
					LOG.warn("Could not disassociate the group from loadbalancer");
				}
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
			;
		}
	}
	
	/// delete security group if all its instances are terminated; delete from db
	public static class SecurityGroupCleanup implements EventListener<ClockTick> {
		public static void register( ) {
		      Listeners.register( ClockTick.class, new SecurityGroupCleanup() );
		    }

		@Override
		public void fireEvent(ClockTick event) {
			/// find all security group whose member instances are empty
			final EntityTransaction db = Entities.get( LoadBalancerSecurityGroup.class );
			List<LoadBalancerSecurityGroup> allGroups = null;
			try{
				allGroups = Entities.query(LoadBalancerSecurityGroup.withState(LoadBalancerSecurityGroup.STATE.OutOfService));
				db.commit();
			}catch(NoSuchElementException ex){
				db.rollback();
			}catch(Exception ex){
				db.rollback();
			}
			if(allGroups==null || allGroups.size()<=0)
				return;
			final List<LoadBalancerSecurityGroup> toDelete = Lists.newArrayList();
			for(LoadBalancerSecurityGroup group : allGroups){
				Collection<LoadBalancerServoInstance> instances = group.getServoInstances();
				if(instances == null || instances.size()<=0)
					toDelete.add(group);
			}
			
			/// delete them from euca
			for(LoadBalancerSecurityGroup group : toDelete){
				try{
					EucalyptusActivityTasks.getInstance().deleteSecurityGroup(group.getName());
					LOG.info("deleted security group: "+group.getName());
				}catch(Exception ex){
					LOG.warn("failed to delete the security group from eucalyptus",ex);
				}
			}
			final EntityTransaction db2 = Entities.get( LoadBalancerSecurityGroup.class );
			try{
				for(LoadBalancerSecurityGroup group: toDelete){
					LoadBalancerSecurityGroup g = 
							Entities.uniqueResult(group);
					Entities.delete(g);
				}
				db2.commit();
			}catch(NoSuchElementException ex){
				db2.rollback();
			}catch(Exception ex){
				LOG.warn("failed to delete the securty group from entity", ex);
				db2.rollback();
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
			// find all OutOfService instances
			List<LoadBalancerServoInstance> retired=null;
			final EntityTransaction db = Entities.get( LoadBalancerServoInstance.class );
			try{
				LoadBalancerServoInstance sample = 
						LoadBalancerServoInstance.withState(LoadBalancerServoInstance.STATE.Retired.name());
				retired = Entities.query(sample);
				db.commit();
			}catch(NoSuchElementException ex){
				db.rollback();
			}catch(Exception ex){
				db.rollback();
				LOG.warn("failed to query loadbalancer servo instance", ex);
			}

			if(retired == null || retired.size()<=0)
				return;
			
			  /// for each:
			// describe instances
	    	final List<String> param = Lists.newArrayList();
	    	final Map<String, String> latestState = Maps.newHashMap();
	    	for(final LoadBalancerServoInstance instance : retired){
	    		/// 	call describe instance
		    	String instanceId = instance.getInstanceId();
	    		if(instanceId == null)
	    			continue;
	    		param.clear();
	    		param.add(instanceId);
	    		String instanceState = null;
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
	    			final EntityTransaction db2 = Entities.get( LoadBalancerServoInstance.class );
	    			try{
	    				LoadBalancerServoInstance toDelete = Entities.uniqueResult(LoadBalancerServoInstance.named(instanceId));
	    				Entities.delete(toDelete);
	    				db2.commit();
	    			}catch(Exception ex){
	    				db2.rollback();
	    			}
	    		}
	    	}
		}
	}
}
