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
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup;
import com.eucalyptus.loadbalancing.LoadBalancerZone;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.eucalyptus.loadbalancing.activities.EventHandlerChainNew.ServoInstancePendingToRunningChecker;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
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
		this.insert(new ServoInstanceFinder(this));
		this.insert(new LoadbalancerInstanceTerminator(this));
		this.insert(new SecurityGroupRemover(this));  /// remove security group
		/// delete the DB entry
		this.insert(new DatabaseUpdate(this));
		return this;
	}

	public static class ServoInstanceFinder extends AbstractEventHandler<DeleteLoadbalancerEvent> implements StoredResult<String>{
		private List<String> instances = Lists.newArrayList();
	
		ServoInstanceFinder(EventHandlerChain<DeleteLoadbalancerEvent> chain){
			super(chain);
		}
		@Override
		public void apply(DeleteLoadbalancerEvent evt)
				throws EventHandlerException {
			// find all servo instances that belong to the deleted loadbalancer 
			LoadBalancer lb = null;
			try{ 
				lb= LoadBalancers.getLoadbalancer(evt.getContext().getUserFullName(), evt.getLoadBalancer());
			}catch(NoSuchElementException ex){
				throw new EventHandlerException("No such loadbalancer found");
			}catch(Exception ex){
				throw new EventHandlerException("Failed to find the loadbalancer");
			}
			
			final EntityTransaction db = Entities.get( LoadBalancerZone.class );
			final List<LoadBalancerServoInstance> members = Lists.newArrayList();
			try{
				final Collection<LoadBalancerZone> zones = lb.getZones();
				if(zones == null || zones.size()==0)
					return;
				
				for(LoadBalancerZone z : zones){
					final LoadBalancerZone zone = Entities.uniqueResult(z);
					members.addAll(zone.getServoInstances());
				}
			}catch(Exception ex){
				throw new EventHandlerException("Failed to find the loadbalancer VMs", ex);
			}finally{
				db.commit();
			}
			
			/// disassociate servo from lbzone; update the status to OutOfService
			final EntityTransaction db2 = Entities.get( LoadBalancerServoInstance.class );
			try{
				for(final LoadBalancerServoInstance instance : members){
					LoadBalancerServoInstance exist = Entities.uniqueResult(instance);
					exist.leaveZone();
					exist.setState(LoadBalancerServoInstance.STATE.OutOfService);
					Entities.persist(exist);
				}
				db2.commit();
			}catch(Exception ex){
				db2.rollback();
			}
			
			instances.addAll(Collections2.transform(members, new Function<LoadBalancerServoInstance, String>(){
				@Override
				public String apply(@Nullable LoadBalancerServoInstance arg0) {
					return arg0.getInstanceId();
				}
			}));
			LOG.debug("found "+instances.size()+ " loadbalancer VMs to terminate");
		}

		@Override
		public void rollback() throws EventHandlerException {
			;
		}

		@Override
		public List<String> getResult() {
			// TODO Auto-generated method stub
			return instances;
		}
	}
	

	public static class LoadbalancerInstanceTerminator extends AbstractEventHandler<DeleteLoadbalancerEvent> implements StoredResult<String> {
		private StoredResult<String> finder = null;
		LoadbalancerInstanceTerminator(EventHandlerChain<DeleteLoadbalancerEvent> chain){
			super(chain);
			finder = chain.findHandler(ServoInstanceFinder.class);
		}
				
		private List<String> terminatedInstances = null;
		@Override
		public void apply(DeleteLoadbalancerEvent evt) throws EventHandlerException {
			// TODO Auto-generated method stub
			
			// find the servo instances for the affected LB
			List<String> terminated = null;
			List<String> toTerminate = this.finder.getResult();
		  	try{
		  		terminated = EucalyptusActivityTasks.getInstance().terminateInstances(toTerminate);
		  	}catch(Exception ex){
		  		throw new EventHandlerException("failed to terminate the instances", ex);
		  	}
		  	this.terminatedInstances = terminated;
		  	
		  	for (String gone : terminated){
		  		toTerminate.remove(gone);
		  	}
		  	if(toTerminate.size()>0){
		  		StringBuilder sb = new StringBuilder();
		  		for(String error : toTerminate)
		  			sb.append(error+" ");
		  		String strErrorInstances = new String(sb);
		  		LOG.error("Some instances were not terminated: "+strErrorInstances);
		  	}
		}
	
		@Override
		public void rollback() throws EventHandlerException {
			; // no rollback if termination fails..?
		}
	
		@Override
		public List<String> getResult() {
			// TODO Auto-generated method stub
			return this.terminatedInstances == null ? Lists.<String>newArrayList() : this.terminatedInstances;
		}
	}
	
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
				allGroups = Entities.query(LoadBalancerSecurityGroup.named());
				db.commit();
			}catch(NoSuchElementException ex){
				db.rollback();
			}catch(Exception ex){
				db.rollback();
			}
			if(allGroups==null || allGroups.size()<=0)
				return;
			final List<String> toDelete = Lists.newArrayList();
			for(LoadBalancerSecurityGroup group : allGroups){
				Collection<LoadBalancerServoInstance> instances = group.getServoInstances();
				if(instances == null || instances.size()<=0)
					toDelete.add(group.getName());
			}
			
			/// delete them from euca
			for(String groupName : toDelete){
				try{
					EucalyptusActivityTasks.getInstance().deleteSecurityGroup(groupName);
					LOG.info("deleted security group: "+groupName);
				}catch(Exception ex){
					LOG.warn("failed to delete the security group from eucalyptus",ex);
				}
			}
			final EntityTransaction db2 = Entities.get( LoadBalancerSecurityGroup.class );
			try{
				for(String groupName: toDelete){
					LoadBalancerSecurityGroup g = Entities.uniqueResult(LoadBalancerSecurityGroup.named(groupName));
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
	
	public static class SecurityGroupRemover extends AbstractEventHandler<DeleteLoadbalancerEvent>{
		SecurityGroupRemover(EventHandlerChain<DeleteLoadbalancerEvent> chain){
			super(chain);
		}

		@Override
		public void apply(DeleteLoadbalancerEvent evt)
				throws EventHandlerException {
			LoadBalancer lb = null;
			LoadBalancerSecurityGroup group = null;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext().getUserFullName(), evt.getLoadBalancer());
				if(lb.getGroups().size() > 0){
					List<LoadBalancerSecurityGroup> groups = Lists.newArrayList(lb.getGroups());
					group = groups.get(0);
				}
			}catch(NoSuchElementException ex){
				throw new EventHandlerException("Could not find the loadbalancer with name="+evt.getLoadBalancer(), ex);
			}catch(Exception ex){
				throw new EventHandlerException("Error while looking for loadbalancer with name="+evt.getLoadBalancer(), ex);
			}

			if(group!= null){
				final EntityTransaction db = Entities.get( LoadBalancerSecurityGroup.class );
				try{
					final LoadBalancerSecurityGroup exist = Entities.uniqueResult(group);
					exist.setLoadBalancer(null);	// this allows the loadbalancer to be deleted
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
			// TODO Auto-generated method stub
		}
		
	}

	public static class DatabaseUpdate extends AbstractEventHandler<DeleteLoadbalancerEvent>{
		DatabaseUpdate(EventHandlerChain<DeleteLoadbalancerEvent> chain){
			super(chain);
		}
		
		@Override
		public void apply(DeleteLoadbalancerEvent evt)
				throws EventHandlerException {
			;
		}

		@Override
		public void rollback() throws EventHandlerException {
			;
		}
	}
	
	/// checks the latest status of the servo instances and update the database accordingly
	public static class ServoInstanceRemover implements EventListener<ClockTick> {
		public static void register( ) {
	      Listeners.register( ClockTick.class, new ServoInstanceRemover() );
	    }

		@Override
		public void fireEvent(ClockTick event) {
			// find all OutOfService instances
			List<LoadBalancerServoInstance> outOfService=null;
			final EntityTransaction db = Entities.get( LoadBalancerServoInstance.class );
			try{
				LoadBalancerServoInstance sample = LoadBalancerServoInstance.withState(LoadBalancerServoInstance.STATE.OutOfService.name());
				outOfService = Entities.query(sample);
			}catch(NoSuchElementException ex){
				;
			}catch(Exception ex){
				LOG.warn("failed to query loadbalancer servo instance", ex);
			}finally{
				db.commit();
			}
			if(outOfService == null || outOfService.size()<=0)
				return;
			
			  /// for each:
			// describe instances
	    	final List<String> param = Lists.newArrayList();
	    	final Map<String, String> latestState = Maps.newHashMap();
	    	for(final LoadBalancerServoInstance instance : outOfService){
	    		/// 	call describe instance
		    	String instanceId = instance.getInstanceId();
	    		if(instanceId == null)
	    			continue;
	    		param.clear();
	    		param.add(instanceId);
	    		String instanceState = null;
	    		try{
	    			final List<RunningInstancesItemType> result = 
	    					  EucalyptusActivityTasks.getInstance().describeInstances(param);
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
