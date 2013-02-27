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

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.AccessPointNotFoundException;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancerZone;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park
 *
 */
@ConfigurableClass(root = "loadbalancing", description = "Parameters controlling Auto Scaling")
public class LoadbalancerInstanceLauncher implements EventHandler<NewLoadbalancerEvent> {
	private static final Logger LOG = Logger.getLogger( LoadbalancerInstanceLauncher.class );
	private List<String> launchedInstances = null;
	@ConfigurableField( displayName = "loadbalancer_emi", 
	                    description = "EMI containing haproxy and the controller",
	                    initial = "NULL", 
	                    readonly = false,
	                    type = ConfigurableFieldType.KEYVALUE )
	public static String LOADBALANCER_EMI = "NULL";
	
	@ConfigurableField( displayName = "loadbalancer_instance_type", 
            description = "instance type for loadbalancer instances",
            initial = "NULL", 
            readonly = false,
            type = ConfigurableFieldType.KEYVALUE )
public static String LOADBALANCER_INSTANCE_TYPE = "NULL";

	
	@Override
	public void apply(NewLoadbalancerEvent evt) throws EventHandlerException {
		final UserFullName ownerFullName = evt.getContext().getUserFullName( );
	  	LoadBalancer lb=null;
	  	 try{
	  		lb= LoadBalancers.getLoadbalancer(ownerFullName, evt.getLoadBalancer());
	  	 }catch(Exception ex){
	    	throw new EventHandlerException("failed to find the loadbalancer", ex);
	    }
	  	 
	  	 /// TODO: SPARK: implements multi-zone
	  	 /// Will need to launch instances per zone
		final Collection<String> zones = evt.getZones();
		String zoneToLaunch = null;
		if (zones.size()>0){
			zoneToLaunch = new ArrayList<String>(zones).get(0);
		}
		InstanceUserDataBuilder userDataBuilder = new InstanceUserDataBuilder();
		
		
		List<String> instanceIds = null;
		try{
			instanceIds = 
					EucalyptusActivityTasks.getInstance().launchInstances(zoneToLaunch, 
							LOADBALANCER_EMI, LOADBALANCER_INSTANCE_TYPE, 1);
			StringBuilder sb = new StringBuilder();
			for (String id : instanceIds)
				sb.append(id+" ");
			LOG.info("new servo instance launched: "+sb.toString());
		}catch(Exception ex){
			throw new EventHandlerException("failed to launch the servo instance", ex);
		}
		launchedInstances = instanceIds;
	}
	public List<String> getLaunchedInstances(){
		return  this.launchedInstances == null ? Lists.<String>newArrayList() : this.launchedInstances;
	}
	
	private static class InstanceUserDataBuilder {
		ConcurrentHashMap<String,String> dataDict= null;
		InstanceUserDataBuilder(){
			dataDict = new ConcurrentHashMap<String,String>();
		}
		void add(String name, String value){
			dataDict.put(name, value);
		}
		
		public String build(){
			StringBuilder sb  = new StringBuilder();
			for (String key : dataDict.keySet()){
				String value = dataDict.get(key);
				sb.append(String.format("%s:%s\n", key, value));
			}
			return sb.toString();
		}
	}
}
