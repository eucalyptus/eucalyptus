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

import com.eucalyptus.loadbalancing.Listener;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup;
import com.eucalyptus.loadbalancing.LoadBalancers;

/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
public class EventHandlerChainNewListeners extends EventHandlerChain<CreateListenerEvent> {
	private static Logger LOG  = Logger.getLogger( EventHandlerChainNewListeners.class );

	@Override
	public EventHandlerChain<CreateListenerEvent> build() {
		this.insert(new AuthorizeIngressRule(this));
		return this;
	}

	public static class AuthorizeIngressRule extends AbstractEventHandler<CreateListenerEvent> {
		private CreateListenerEvent event = null;
		
		protected AuthorizeIngressRule(
				EventHandlerChain<CreateListenerEvent> chain) {
			super(chain);
		}

		@Override
		public void apply(CreateListenerEvent evt) throws EventHandlerException {
			this.event = evt;
			final Collection<Listener> listeners = evt.getListeners();
			LoadBalancer lb = null;
			String groupName = null;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext().getUserFullName(), evt.getLoadBalancer());
				Collection<LoadBalancerSecurityGroup> groups = lb.getGroups();
				if(groups.size()>0)
					groupName = groups.toArray(new LoadBalancerSecurityGroup[groups.size()])[0].getName();
			}catch(Exception ex){
				throw new EventHandlerException("could not find the loadbalancer", ex);
			}
			if(groupName == null)
				throw new EventHandlerException("Group name is not found");
			
			for(Listener listener : listeners){
				int port = listener.getLoadBalancerPort();
				String protocol = "tcp"; /// Loadbalancer listeners protocols: HTTP, HTTPS, TCP, SSL -> all tcp
				try{
					EucalyptusActivityTasks.getInstance().authorizeSecurityGroup(groupName, protocol, port);
				}catch(Exception ex){
					throw new EventHandlerException(String.format("failed to authorize %s, %s, %d", groupName, protocol, port), ex);
				}
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
			if(this.event == null)
				return;
			
			final Collection<Listener> listeners = this.event.getListeners();
			LoadBalancer lb = null;
			String groupName = null;
			try{
				lb = LoadBalancers.getLoadbalancer(this.event.getContext().getUserFullName(), this.event.getLoadBalancer());
				Collection<LoadBalancerSecurityGroup> groups = lb.getGroups();
				if(groups.size()>0)
					groupName = groups.toArray(new LoadBalancerSecurityGroup[groups.size()])[0].getName();
			}catch(Exception ex){
				;
			}
			if(groupName == null)
				return;
			
			for(Listener listener : listeners){
				int port = listener.getLoadBalancerPort();
				String protocol = listener.getProtocol();
				protocol = protocol.toLowerCase();
				
				try{
					EucalyptusActivityTasks.getInstance().revokeSecurityGroup(groupName, protocol, port);
				}catch(Exception ex){
					;
				}
			}
		}
	}

}
