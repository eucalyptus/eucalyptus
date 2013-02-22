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
 * @author Sang-Min Park
 *
 */
public class EventHandlerChains {
	private static Logger LOG  = Logger.getLogger( EventHandlerChains.class );
	
	private static EventHandlerChain<NewLoadbalancerEvent> onNewLoadbalancerChain = null;
	public static EventHandlerChain<NewLoadbalancerEvent> onNewLoadbalancer(){
		if(onNewLoadbalancerChain==null){
			onNewLoadbalancerChain= new EventHandlerChain<NewLoadbalancerEvent>(){
				@Override
				public EventHandlerChain<NewLoadbalancerEvent> build() {
					// TODO Auto-generated method stub
					this.insert(new EventHandler<NewLoadbalancerEvent>(){
						@Override
						public void apply(NewLoadbalancerEvent evt) {
							// TODO Auto-generated method stub
							LOG.info("onNewLoadbalancer");
						}
					});
					this.insert(new LoadbalancerInstanceLauncher());
					return this;
				}
			}.build();
		}
		return onNewLoadbalancerChain;
	}
	
	private static  EventHandlerChain<DeleteLoadbalancerEvent> onDeleteLoadbalancerChain = null;
	public static EventHandlerChain<DeleteLoadbalancerEvent> onDeleteLoadbalancer(){
		if(onDeleteLoadbalancerChain==null){
			onDeleteLoadbalancerChain= new EventHandlerChain<DeleteLoadbalancerEvent>(){
				@Override
				public EventHandlerChain<DeleteLoadbalancerEvent> build() {
				// TODO Auto-generated method stub
					this.insert(new EventHandler<DeleteLoadbalancerEvent>(){
					@Override
						public void apply(DeleteLoadbalancerEvent evt) {
						// TODO Auto-generated method stub
							LOG.info("onDeleteLoadbalancer");
						}
					});
					return this;
				}
			}.build();
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
					this.insert(new EventHandler<CreateListenerEvent>(){
						@Override
						public void apply(CreateListenerEvent evt) {
							// TODO Auto-generated method stub
							LOG.info("onCreateListener");
						}
					});
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
					this.insert(new EventHandler<DeleteListenerEvent>(){
						@Override
						public void apply(DeleteListenerEvent evt) {
							// TODO Auto-generated method stub
							LOG.info("onDeleteListener");
						}
					});
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
					this.insert(new EventHandler<RegisterInstancesEvent>(){
						@Override
						public void apply(RegisterInstancesEvent evt) {
							// TODO Auto-generated method stub
							LOG.info("onRegisterInstances");
						}
					});
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
					this.insert(new EventHandler<DeregisterInstancesEvent>(){
						@Override
						public void apply(DeregisterInstancesEvent evt) {
							// TODO Auto-generated method stub
							LOG.info("onDeregisterInstances");
						}
					});
					return this;
				}
			}.build();
		}
		return onDeregisterInstancesChain;
	}
		
}
