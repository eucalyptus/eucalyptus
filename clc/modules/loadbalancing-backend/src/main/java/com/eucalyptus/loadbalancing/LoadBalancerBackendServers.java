/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.loadbalancing;

import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.LoadBalancerBackendServerDescription.LoadBalancerBackendServerDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerBackendServerDescription.LoadBalancerBackendServerDescriptionEntityTransform;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park
 *
 */
public class LoadBalancerBackendServers {
  private static Logger    LOG     = Logger.getLogger( LoadBalancerBackendServers.class );

  public static List<LoadBalancerBackendServerDescription> getLoadBalancerBackendServerDescription(final LoadBalancer lb) {
      final List<LoadBalancerBackendServerDescription> backends = Lists.newArrayList();
      for(final LoadBalancerBackendServerDescriptionCoreView view : lb.getBackendServers()) {
        backends.add(LoadBalancerBackendServerDescriptionEntityTransform.INSTANCE.apply(view));
      }
      return backends;
  }
  
  public static boolean hasBackendServerDescription(final LoadBalancer lb, int instancePort) {
    boolean found = false;
    for(final LoadBalancerBackendServerDescriptionCoreView view : lb.getBackendServers()) {
      if(view.getInstancePort().intValue() == instancePort) {
        found =true;
        break;
      }
    }
    return found;
  }
  
  public static LoadBalancerBackendServerDescription createBackendServerDescription(final LoadBalancer lb, final int instancePort) {
    LoadBalancerBackendServerDescription backend = null;
    try ( final TransactionResource db = 
        Entities.transactionFor( LoadBalancerBackendServerDescription.class ) ) {
      backend = LoadBalancerBackendServerDescription.named(lb, instancePort);
      Entities.persist(backend);
      db.commit();
    }
    
    try ( final TransactionResource db = 
        Entities.transactionFor( LoadBalancerBackendServerDescription.class ) ) {
      try{
        backend = Entities.uniqueResult(backend);
      }catch(final Exception ex) {
        backend = null;
      }
    }
    return backend;
  }
  
  public static LoadBalancerBackendServerDescription getBackendServerDescription(final LoadBalancer lb, final int instancePort) {
    // look for existing back-end server description; if none, create one
    LoadBalancerBackendServerDescription backend = null;
    for(final LoadBalancerBackendServerDescriptionCoreView backendView : lb.getBackendServers()){
      if(backendView.getInstancePort().intValue() == instancePort) {
        try{
          backend = LoadBalancerBackendServerDescriptionEntityTransform.INSTANCE.apply(backendView);
        }catch(final Exception ex){
          LOG.error("Failed to load loadbalancer backend server description", ex);
          return null;
        }
        break;
      }
    }
    return backend; 
  }
}
