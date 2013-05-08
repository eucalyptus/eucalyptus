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

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.Listener;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup.LoadBalancerSecurityGroupCoreView;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
public class EventHandlerChainNewListeners extends EventHandlerChain<CreateListenerEvent> {
	private static Logger LOG  = Logger.getLogger( EventHandlerChainNewListeners.class );

	@Override
	public EventHandlerChain<CreateListenerEvent> build() {
		this.insert(new AuthorizeIngressRule(this));
		this.insert(new UpdateHealthCheckConfig(this));
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
				lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
				final LoadBalancerSecurityGroupCoreView group = lb.getGroup();
				if(group!=null)
					groupName = group.getName();
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
				lb = LoadBalancers.getLoadbalancer(this.event.getContext(), this.event.getLoadBalancer());
				final LoadBalancerSecurityGroupCoreView group = lb.getGroup();
				if(group!=null)
					groupName = group.getName();
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

	static class UpdateHealthCheckConfig extends AbstractEventHandler<CreateListenerEvent> {
		private static final int DEFAULT_HEALTHY_THRESHOLD = 3;
		private static final int DEFAULT_INTERVAL = 30;
		private static final int DEFAULT_TIMEOUT = 5;
		private static final int DEFAULT_UNHEALTHY_THRESHOLD = 2;
		
		protected UpdateHealthCheckConfig(
				EventHandlerChain<CreateListenerEvent> chain) {
			super(chain);
		}

		@Override
		public void apply(CreateListenerEvent evt)
				throws EventHandlerException {
			LoadBalancer lb = null;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
			}catch(NoSuchElementException ex){
				throw new EventHandlerException("Could not find the loadbalancer with name="+evt.getLoadBalancer(), ex);
			}catch(Exception ex){
				throw new EventHandlerException("Error while looking for loadbalancer with name="+evt.getLoadBalancer(), ex);
			}
			/* default setting in AWS
			"HealthyThreshold": 10, 
			"Interval": 30, 
			"Target": "TCP:8000", 
			"Timeout": 5, 
			"UnhealthyThreshold": 2 */
			try{
				lb.getHealthCheckTarget();
				lb.getHealthCheckInterval();
				lb.getHealthCheckTimeout();
				lb.getHealthCheckUnhealthyThreshold();
				lb.getHealthyThreshold();
			}catch(final IllegalStateException ex){ /// only when the health check is not previously configured
				Listener firstListener = null;
				if(evt.getListeners()==null || evt.getListeners().size()<=0)
					throw new EventHandlerException("No listener requested");
				
				final List<Listener> listeners = Lists.newArrayList(evt.getListeners());
				firstListener = listeners.get(0);
				final String target = String.format("TCP:%d", firstListener.getInstancePort());
				final EntityTransaction db = Entities.get( LoadBalancer.class );
				try{
					final LoadBalancer update = Entities.uniqueResult(lb);
			    	update.setHealthCheck(DEFAULT_HEALTHY_THRESHOLD, DEFAULT_INTERVAL, target, DEFAULT_TIMEOUT, DEFAULT_UNHEALTHY_THRESHOLD);
					Entities.persist(update);
					db.commit();
				}catch(final NoSuchElementException exx){
					db.rollback();
					LOG.warn("Loadbalancer not found in the database");
				}catch(final Exception exx){
					db.rollback();
					LOG.warn("Unable to query the loadbalancer", ex);
				}finally {
					if(db.isActive())
						db.rollback();
				}
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
			;
		}
	}
		


}
