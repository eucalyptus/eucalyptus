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

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.common.msgs.Instance;
import com.google.common.collect.Lists;

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
		}

		@Override
		public void rollback() throws EventHandlerException {
			;
		}
		
	}

}
