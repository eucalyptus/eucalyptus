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
				try{
					EventHandlerChains.onEnableZones().execute((EnabledZoneEvent) event);
				}catch(EventHandlerChainException ex){
					throw Exceptions.toUndeclared(ex);
				}
			}
		},
		DisabledZone(DisabledZoneEvent.class){
			@Override
			public void fireEvent(LoadbalancingEvent event) {
				try{
					EventHandlerChains.onDisableZones().execute((DisabledZoneEvent) event);
				}catch(EventHandlerChainException ex){
					throw Exceptions.toUndeclared(ex);
				}
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
