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
package com.eucalyptus.loadbalancing.activities
import com.eucalyptus.loadbalancing.common.backend.msgs.Instance
import com.eucalyptus.loadbalancing.common.backend.msgs.Listener
import com.eucalyptus.context.Context;
import com.eucalyptus.event.GenericEvent

/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
class LoadbalancingEvent extends GenericEvent<String> { 
	String loadBalancer="";
	Context context;
	
	@Override
	public String toString(){
		return "evt: "+this.getClass().toString();
	}
}

class LoadbalancingUserEvent extends LoadbalancingEvent{ }
class LoadbalancingAdminEvent extends LoadbalancingEvent{ }

class NewLoadbalancerEvent extends LoadbalancingUserEvent { 
	Collection<String> zones;
	
}
class DeleteLoadbalancerEvent extends LoadbalancingUserEvent { }
class CreateListenerEvent extends LoadbalancingUserEvent {
	Collection<Listener> listeners;
}
class DeleteListenerEvent extends LoadbalancingUserEvent {
	Collection<Integer> ports;
}
class RegisterInstancesEvent extends LoadbalancingUserEvent {
	Collection<Instance> instances;
}
class DeregisterInstancesEvent extends LoadbalancingUserEvent {
	Collection<Instance> instances;
}
class EnabledZoneEvent extends LoadbalancingUserEvent {
	Collection<String> zones;
}
class DisabledZoneEvent extends LoadbalancingUserEvent {
	Collection<String> zones;
}