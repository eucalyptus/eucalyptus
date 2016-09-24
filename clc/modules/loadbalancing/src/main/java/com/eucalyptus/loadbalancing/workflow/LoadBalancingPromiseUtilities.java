/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.loadbalancing.workflow;

import java.util.List;
import java.util.Map;

import com.amazonaws.services.simpleworkflow.flow.core.AndPromise;
import com.amazonaws.services.simpleworkflow.flow.core.OrPromise;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.eucalyptus.loadbalancing.LoadBalancerZone;
import com.eucalyptus.loadbalancing.service.LoadBalancingServoCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
public class LoadBalancingPromiseUtilities {
  public static Promise<?> atLeastOneInstanceInZone(final Map<String, Promise<?>> instancePromises) {
    final Multimap<String, String> zoneToInstances =
        ArrayListMultimap.create();
    
    for (final String instance : instancePromises.keySet()) {
     try{
       final LoadBalancerZone zone = 
           LoadBalancingServoCache.getInstance().getLoadBalancerZone(instance);
       zoneToInstances.put(zone.getName(), instance);
     }catch(final Exception ex) {
       ;
     }
    }
    final List<Promise<?>> orPromises = Lists.newArrayList();
    for (final String zone : zoneToInstances.keys()) {
      Promise<?> orPromise = null;
      for (final String instance : zoneToInstances.get(zone)) {
        final Promise<?> p = instancePromises.get(instance);
        if(orPromise == null)
          orPromise = p;
        else
          orPromise = new OrPromise(orPromise, p);
      }
      if (orPromise!=null)
        orPromises.add(orPromise);
    }
    Promise<?> andPromise = null;
    for(Promise<?> p : orPromises) {
      if(andPromise==null)
        andPromise = p;
      else
        andPromise = new AndPromise(andPromise, p);
    }
    return andPromise;
  }
  
  public static Promise<?> allInstances(final Map<String, Promise<?>> instancePromises) {
    Promise<?> andPromise = null;
    for(final String instance : instancePromises.keySet()) {
      final Promise<?> p = instancePromises.get(instance);
      if (andPromise == null) {
        andPromise = p;
      }else {
        andPromise = new AndPromise(andPromise, p);
      }
    }
    return andPromise;
  }
}
