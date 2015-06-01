/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.eucalyptus.loadbalancing.common.msgs.Instance;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Sang-Min Park
 *
 */
public class EventHandlerChainRegisterInstances extends EventHandlerChain<RegisterInstancesEvent> {

	@Override
	public EventHandlerChain<RegisterInstancesEvent> build() {
		this.insert(new RequestVerifier(this));
		return this;
	}

	private static class RequestVerifier extends  AbstractEventHandler<RegisterInstancesEvent> {

		protected RequestVerifier(
				EventHandlerChain<RegisterInstancesEvent> chain) {
			super(chain);
		}

		@Override
		public void apply(RegisterInstancesEvent evt)
				throws EventHandlerException {
			if(evt.getInstances()==null || evt.getInstances().size()<=0)
				return;
			
			final List<Instance> instances = Lists.newArrayList(evt.getInstances());
			try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
				for(final Instance instance : instances){
					// make sure the instance is not a servo VM
					try{
						LoadBalancerServoInstance servo = Entities.uniqueResult(LoadBalancerServoInstance.named(instance.getInstanceId()));
						if(servo!=null)
							throw new EventHandlerException("Loadbalancer VM cannot be registered");
					}catch(EventHandlerException ex){
						throw ex;
					}catch(NoSuchElementException ex){
						// pass
					}catch(Exception ex){
						break;
					}
				}
			}
			
			LoadBalancer lb;
      try{
        lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
      }catch(NoSuchElementException ex){
        throw new EventHandlerException("Could not find the loadbalancer with name="+evt.getLoadBalancer(), ex);
      }catch(Exception ex){
        throw new EventHandlerException("Error while looking for loadbalancer with name="+evt.getLoadBalancer(), ex);
      } 

      final Set<String> lbZones = Sets.newHashSet(Collections2.transform(lb.getZones(), new Function<LoadBalancerZoneCoreView, String>(){
        @Override
        public String apply(LoadBalancerZoneCoreView arg0) {
          return arg0.getName();
        }
      }));
      
			final String acctNumber = evt.getContext().getAccountNumber();
			final List<String> instanceIds = Lists.newArrayList(Collections2.transform(evt.getInstances(), new Function<Instance,String>() {
        @Override
        public String apply(Instance arg0) {
          return arg0.getInstanceId();
        }
			}));
			
			List<RunningInstancesItemType> eucaInstances = Lists.newArrayList();
		  try{
		    eucaInstances = EucalyptusActivityTasks.getInstance().describeUserInstances(acctNumber, instanceIds);
		  }catch(final Exception ex) {
		    throw new EventHandlerException("Failed to query instances: "+ex.getMessage());
		  }
		  
		  for(final RunningInstancesItemType instance : eucaInstances) {
		    if(! lbZones.contains(instance.getPlacement())) {
		      throw new EventHandlerException("Instance "+instance.getInstanceId()+"'s availaibility zone is not enabled for the loadbalancer");
		    }
		  }
		}

		@Override
		public void rollback() throws EventHandlerException {
			;
		}
		
	}

}
