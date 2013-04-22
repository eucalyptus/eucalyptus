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
import org.apache.log4j.Logger;

import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup;
import com.eucalyptus.loadbalancing.LoadBalancers;
/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
public class EventHandlerChainDeleteListeners extends EventHandlerChain<DeleteListenerEvent> {
	private static Logger LOG  = Logger.getLogger( EventHandlerChainDeleteListeners.class );

	@Override
	public EventHandlerChain<DeleteListenerEvent> build() {
		this.insert(new RevokeIngressRule(this));
		return this;
	}

	public static class RevokeIngressRule extends AbstractEventHandler<DeleteListenerEvent> {		
		protected RevokeIngressRule(
				EventHandlerChain<DeleteListenerEvent> chain) {
			super(chain);
		}

		@Override
		public void apply(DeleteListenerEvent evt) throws EventHandlerException {
			final Collection<Integer> ports = evt.getPorts();
			LoadBalancer lb = null;
			String groupName = null;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext().getUserFullName(), evt.getLoadBalancer());
				final LoadBalancerSecurityGroup group = lb.getGroup();
				if(group!=null)
					groupName = group.getName();
			}catch(Exception ex){
				throw new EventHandlerException("could not find the loadbalancer", ex);
			}
			if(groupName == null)
				throw new EventHandlerException("Group name is not found");
			
			String[] protocols = new String[]{"tcp"}; /// Loadbalancer listeners protocols: HTTP, HTTPS, TCP, SSL -> all tcp
			for(String protocol : protocols){
				for(Integer port : ports){
					try{
						EucalyptusActivityTasks.getInstance().revokeSecurityGroup(groupName, protocol, port);
					}catch(Exception ex){
						;
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
