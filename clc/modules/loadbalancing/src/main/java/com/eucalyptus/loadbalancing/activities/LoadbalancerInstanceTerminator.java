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

import org.apache.log4j.Logger;

import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park
 *
 */
public class LoadbalancerInstanceTerminator implements EventHandler<DeleteLoadbalancerEvent> {
	private static final Logger LOG = Logger.getLogger( LoadbalancerInstanceTerminator.class );

	private List<String> terminatedInstances = null;
	@Override
	public void apply(DeleteLoadbalancerEvent evt) throws EventHandlerException {
		// TODO Auto-generated method stub
		
		// find the servo instances for the affected LB
		final UserFullName ownerFullName = evt.getContext().getUserFullName( );
	  	LoadBalancer lb=null;
	  	 try{
	  		lb= LoadBalancers.getLoadbalancer(ownerFullName, evt.getLoadBalancer());
	  	 }catch(Exception ex){
	    	throw new EventHandlerException("failed to find the loadbalancer", ex);
	    }
	  	final Collection<LoadBalancerServoInstance> instances = lb.getServoInstances();
	  	List<String> toTerminate = Lists.newArrayList(Collections2.transform(instances, 
	  			new Function<LoadBalancerServoInstance, String>(){
	  		@Override
	  		public String apply(LoadBalancerServoInstance input){
	  			return input.getInstanceId();
	  		}
	  	}));
	  	
	  	List<String> terminated = null;
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
	public List<String> getTerminatedInstances(){
		return this.terminatedInstances == null ? Lists.<String>newArrayList() : this.terminatedInstances;
	}

}
