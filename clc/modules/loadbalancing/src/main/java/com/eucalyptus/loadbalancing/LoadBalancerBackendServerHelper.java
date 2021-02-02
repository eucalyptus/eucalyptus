/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.loadbalancing;

import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerBackendServerDescription;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerBackendServerDescription.LoadBalancerBackendServerDescriptionCoreView;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerBackendServerDescription.LoadBalancerBackendServerDescriptionEntityTransform;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancer;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park
 *
 */
public class LoadBalancerBackendServerHelper {
  private static Logger    LOG     = Logger.getLogger( LoadBalancerBackendServerHelper.class );

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
