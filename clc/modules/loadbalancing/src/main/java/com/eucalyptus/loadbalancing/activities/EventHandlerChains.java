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
	
	private static EventHandlerChain<NewLoadbalancerEvent> onNewLoadbalancerChain = null;
	public static EventHandlerChain<NewLoadbalancerEvent> onNewLoadbalancer(){
		if(onNewLoadbalancerChain==null){
			onNewLoadbalancerChain= (new EventHandlerChainNew()).build();
		}
		return onNewLoadbalancerChain;
	}
	
	private static  EventHandlerChain<DeleteLoadbalancerEvent> onDeleteLoadbalancerChain = null;
	public static EventHandlerChain<DeleteLoadbalancerEvent> onDeleteLoadbalancer(){
		if(onDeleteLoadbalancerChain==null){
			onDeleteLoadbalancerChain= (new EventHandlerChainDelete()).build();
		}
		return onDeleteLoadbalancerChain;
	}
	
	private static EventHandlerChain<CreateListenerEvent> onCreateListenerChain = null;
	public static EventHandlerChain<CreateListenerEvent> onCreateListener(){
		if (onCreateListenerChain==null){
			onCreateListenerChain = new EventHandlerChain<CreateListenerEvent>(){
				@Override
				public EventHandlerChain<CreateListenerEvent> build() {
					// TODO Auto-generated method stub
					return this;
				}
			}.build();
		}
		return onCreateListenerChain;
	}
	
	private static  EventHandlerChain<DeleteListenerEvent> onDeleteListenerChain = null;
	public static EventHandlerChain<DeleteListenerEvent> onDeleteListener(){
		if(onDeleteListenerChain==null){
			onDeleteListenerChain = new EventHandlerChain<DeleteListenerEvent>(){
				@Override
				public EventHandlerChain<DeleteListenerEvent> build() {
					// TODO Auto-generated method stub
					return this;
				}
			}.build();
		}
		return onDeleteListenerChain;
	}
	
	private static EventHandlerChain<RegisterInstancesEvent> onRegisterInstancesChain=null;
	public static EventHandlerChain<RegisterInstancesEvent> onRegisterInstances(){
		if(onRegisterInstancesChain==null){
			onRegisterInstancesChain= new EventHandlerChain<RegisterInstancesEvent>(){
				@Override
				public EventHandlerChain<RegisterInstancesEvent> build() {
					// TODO Auto-generated method stub
					return this;
				}
			}.build();
		}
		return onRegisterInstancesChain;
	}
	
	private static EventHandlerChain<DeregisterInstancesEvent> onDeregisterInstancesChain = null;
	public static EventHandlerChain<DeregisterInstancesEvent> onDeregisterInstances(){
		if(onDeregisterInstancesChain==null){
			onDeregisterInstancesChain= new EventHandlerChain<DeregisterInstancesEvent>(){
				@Override
				public EventHandlerChain<DeregisterInstancesEvent> build() {
					// TODO Auto-generated method stub
					return this;
				}
			}.build();
		}
		return onDeregisterInstancesChain;
	}
		
}
