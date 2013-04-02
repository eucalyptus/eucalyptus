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

import com.eucalyptus.event.EventFailedException;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.util.Exceptions;
/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
public class ActivityManager {

	/* CONSIDERATIONS
	 * 	1. LB vm launching event
	 * 	   -- create load balancer? probably not 
	 * 	   -- listener attached to the loadbalancer? maybe
	 * 	   -- there exist an instance to the loadbalancer: how to deal with frequent update?
	 * 	2. LB vm terminating event
	 *	   -- load balancer deleted? probably not
	 *     -- no more listener attached? maybe
	 *     -- no instance in the pool?
	 * 	3. LB VM-to-loadbalancer relation registry
	 * 
	 * 	4. LB VM caching
	 *     
	 * 	5. EMI backing the servo VM
	 * 
	 * 	6. event-to-action logic should be abstract and be part of activities package
	 * 
	 * 	7. multi zones
	 * 
	 * 	8. scaling and HA
	 * 
	 * 	9. admin configurable properties
	 * 
	 * 	10. DNS registry
	 * 
	 * INTERFACE FROM LOADBALANCING SERVICE TO ACTIVITY MGR
	 *  1. Operation check
	 *     -- should create-lb, add-listener, add-instances, etc return ok?
	 *     
	 *  2. Event listing
	 *     -- create lb, listener, add instances
	 *     
	 *  3. Event publication
	 *  
	 *  4. LB status check
	 *     -- are the LBs running ok? 
	 *     -- any mgmt issues? multi-zones, ha, scaling, etc
	 *     
	 *  5. Health check callback
	 *  
	 *  6. CloudWatch callback
	 *  
	 *   
	 *  INTERFACE FROM ADMIN TO ACTIVITY MGR
	 *  1. Management events
	 *     -- max # LB Vms, LB Vms per user, etc 
	 *     
	 *  2. Service enabling/disabling
	 *  
	 *  3. Changing EMIs backing LB VMs
	 *  
	 */
    private static final Logger LOG = Logger.getLogger( ActivityManager.class );

	private ActivityManager(){
		LoadbalancerEventListener.register();
	}
	private static ActivityManager _instance = new ActivityManager();
	public static ActivityManager getInstance(){
		return _instance;
	}
	
	public void fire(LoadbalancingEvent evt) throws EventFailedException {
		ListenerRegistry.getInstance().fireThrowableEvent(evt);
	}
	
	enum LoadbalancerEventListener implements EventListener<LoadbalancingEvent>{
		NewLoadbalancer(NewLoadbalancerEvent.class) {
			@Override
			public void fireEvent(LoadbalancingEvent event) {
				try{
					EventHandlerChains.onNewLoadbalancer().execute((NewLoadbalancerEvent)event);
				}catch(EventHandlerChainException ex){
					throw Exceptions.toUndeclared(ex);
				}
			}
		},
		DeleteLoadbalancer(DeleteLoadbalancerEvent.class) {
			@Override
			public void fireEvent(LoadbalancingEvent event) {
				try{
					EventHandlerChains.onDeleteLoadbalancer().execute((DeleteLoadbalancerEvent) event);
				}catch(EventHandlerChainException ex){
					throw Exceptions.toUndeclared(ex);
				}
			}
		},
		CreateListener(CreateListenerEvent.class) {
			@Override
			public void fireEvent(LoadbalancingEvent event) {
				try{
					EventHandlerChains.onCreateListener().execute((CreateListenerEvent)event);
				}catch(EventHandlerChainException ex){
					throw Exceptions.toUndeclared(ex);
				}
			}
		}, 
		DeleteListener(DeleteListenerEvent.class) {
			@Override
			public void fireEvent(LoadbalancingEvent event) {
				try{
					EventHandlerChains.onDeleteListener().execute((DeleteListenerEvent)event);
				}catch(EventHandlerChainException ex){
					throw Exceptions.toUndeclared(ex);
				}
			}
		},
		RegisterInstances(RegisterInstancesEvent.class) {
			@Override
			public void fireEvent(LoadbalancingEvent event) {
				try{
					EventHandlerChains.onRegisterInstances().execute((RegisterInstancesEvent)event);
				}catch(EventHandlerChainException ex){
					throw Exceptions.toUndeclared(ex);
				}
			}
		},
		DeregisterInstances(DeregisterInstancesEvent.class) {
			@Override
			public void fireEvent(LoadbalancingEvent event) {
				try{
					EventHandlerChains.onDeregisterInstances().execute((DeregisterInstancesEvent) event);
				}catch(EventHandlerChainException ex){
					throw Exceptions.toUndeclared(ex);
				}
			}
		},
		EnabledZone(EnabledZoneEvent.class){
			@Override
			public void fireEvent(LoadbalancingEvent event) {
				// NULL-OP
				// TODO: SPARK: Should validate the zone name
				return;
			}
		},
		DisabledZone(DisabledZoneEvent.class){
			@Override
			public void fireEvent(LoadbalancingEvent event) {
				// NULL-OP event sink
				return;
			}
		};
		
		private final Class<? extends LoadbalancingEvent> evtType;
		
		LoadbalancerEventListener(Class<? extends LoadbalancingEvent> type){
			evtType = type;
		}
		
		public static void register(){
			for (LoadbalancerEventListener listener : LoadbalancerEventListener.values()){
				Listeners.register(listener.evtType, listener);
			}
		}
	}
}
