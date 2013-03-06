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
import java.util.NoSuchElementException;

import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancerZone;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
public class EventHandlerChainDelete extends EventHandlerChain<DeleteLoadbalancerEvent> {
	private static Logger LOG  = Logger.getLogger( EventHandlerChainDelete.class );

	@Override
	public EventHandlerChain<DeleteLoadbalancerEvent> build() {
	// TODO Auto-generated method stub
		this.insert(new ServoInstanceFinder(this));
		this.insert(new LoadbalancerInstanceTerminator(this));
		/// delete the DB entry
		this.insert(new DatabaseUpdate(this));
		return this;
	}

	private class ServoInstanceFinder extends AbstractEventHandler<DeleteLoadbalancerEvent> implements StoredResult<String>{
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
	

private class LoadbalancerInstanceTerminator extends AbstractEventHandler<DeleteLoadbalancerEvent> implements StoredResult<String> {
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

	private class DatabaseUpdate extends AbstractEventHandler<DeleteLoadbalancerEvent>{
		private StoredResult<String> terminator = null;
		DatabaseUpdate(EventHandlerChain<DeleteLoadbalancerEvent> chain){
			super(chain);
			terminator = chain.findHandler(LoadbalancerInstanceTerminator.class);
		}
		
		@Override
		public void apply(DeleteLoadbalancerEvent evt)
				throws EventHandlerException {
			final EntityTransaction db = Entities.get( LoadBalancerServoInstance.class );
			try{
				for (String terminated : this.terminator.getResult()){
					final LoadBalancerServoInstance instance = Entities.uniqueResult( 
							LoadBalancerServoInstance.named(terminated));	
					Entities.delete(instance);
				}
				db.commit();
			}catch (NoSuchElementException e){
				db.rollback();
				throw new EventHandlerException("No servo instance found in DB", e);
			}catch (Exception e){
				db.rollback();
				throw new EventHandlerException("Failed to delete the servo instance", e);
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
			;
		}
		
	}
}
