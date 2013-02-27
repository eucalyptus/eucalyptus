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
		List<String> instanceIds = null;
		String instanceId = null;
		try{
			instanceIds = 
					EucalyptusActivityTasks.getInstance().launchInstances(zoneToLaunch, 
							LOADBALANCER_EMI, LOADBALANCER_INSTANCE_TYPE, 1);
			LOG.info("new servo instance launched: "+instanceIds.get(0));
			instanceId = instanceIds.get(0);
		}catch(Exception ex){
			throw new EventHandlerException("failed to launch the servo instance", ex);
		}
		
		// save the servoinstance entry
		final EntityTransaction db = Entities.get( LoadBalancerServoInstance.class );
		// check the listener 
		try{
			Entities.uniqueResult(LoadBalancerServoInstance.named(instanceId));
		}catch(NoSuchElementException ex){
			final LoadBalancerServoInstance newInstance = 
					LoadBalancerServoInstance.named(instanceId, zoneToLaunch);
			newInstance.setLoadbalancer(lb);		
			Entities.persist(newInstance);
			db.commit();
		}catch(Exception ex){
			LOG.error("failed to persist the servo instance "+instanceId, ex);
			db.rollback();
			throw new EventHandlerException("failed to persist the servo instance", ex);
		}
	}
}
