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

import org.apache.log4j.Logger;
/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
public class EventHandlerChains {
	private static Logger LOG  = Logger.getLogger( EventHandlerChains.class );
	
	public static EventHandlerChain<NewLoadbalancerEvent> onNewLoadbalancer(){
		return (new EventHandlerChainNew()).build();
	}
	
	public static EventHandlerChain<DeleteLoadbalancerEvent> onDeleteLoadbalancer(){
		return (new EventHandlerChainDelete()).build();
	}
	
	public static EventHandlerChain<CreateListenerEvent> onCreateListener(){
		return (new EventHandlerChainNewListeners()).build();
	}
	
	private static  EventHandlerChain<DeleteListenerEvent> onDeleteListenerChain = null;
	public static EventHandlerChain<DeleteListenerEvent> onDeleteListener(){
		return (new EventHandlerChainDeleteListeners()).build();
	}
	
	public static EventHandlerChain<RegisterInstancesEvent> onRegisterInstances(){
		return new EventHandlerChain<RegisterInstancesEvent>(){
				@Override
				public EventHandlerChain<RegisterInstancesEvent> build() {
					// TODO Auto-generated method stub
					return this;
				}
			}.build();
	}
	
	private static EventHandlerChain<DeregisterInstancesEvent> onDeregisterInstancesChain = null;
	public static EventHandlerChain<DeregisterInstancesEvent> onDeregisterInstances(){
		return new EventHandlerChain<DeregisterInstancesEvent>(){
				@Override
				public EventHandlerChain<DeregisterInstancesEvent> build() {
					// TODO Auto-generated method stub
					return this;
				}
			}.build();
	}
		
}
