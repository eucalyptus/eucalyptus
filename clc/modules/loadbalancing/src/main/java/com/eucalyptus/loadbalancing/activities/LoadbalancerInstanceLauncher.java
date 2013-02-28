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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancerZone;
import com.eucalyptus.loadbalancing.LoadBalancers;
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
	public void apply(NewLoadbalancerEvent evt) {
		// TODO Auto-generated method stub
		
		final UserFullName ownerFullName = evt.getContext().getUserFullName( );
	  	LoadBalancer lb=null;
	  	 try{
	  		lb= LoadBalancers.getLoadbalancer(ownerFullName, evt.getLoadBalancer());
		  	Entities.refresh(lb);
	  	 }catch(Exception ex){
	    	LOG.warn("No loadbalancer is found with name="+evt.getLoadBalancer());    
	    }
		final Collection<LoadBalancerZone> zones = lb.getZones();
		String zoneToLaunch = "PARTI00";
		if (zones.size()>0){
			zoneToLaunch = new ArrayList<LoadBalancerZone>(zones).get(0).getName();
		}
		try{
			EucalyptusActivityTasks.getInstnace().launchInstance(zoneToLaunch, LOADBALANCER_EMI, LOADBALANCER_INSTANCE_TYPE);
		}catch(Exception e){
			LOG.error("failed to launch instance", e);
		}
	}
}
