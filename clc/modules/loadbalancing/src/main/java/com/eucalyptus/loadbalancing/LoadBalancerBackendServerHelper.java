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

import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerBackendServerDescription;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancer;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park
 *
 */
public class LoadBalancerBackendServerHelper {
  private static Logger    LOG     = Logger.getLogger( LoadBalancerBackendServerHelper.class );

  public static List<LoadBalancerBackendServerDescription> getLoadBalancerBackendServerDescription(final LoadBalancer lb) {
      return Lists.newArrayList( lb.getBackendServers( ) );
  }

  /**
   * Caller must have transaction for load balancer
   */
  public static boolean hasBackendServerDescription(final LoadBalancer lb, int instancePort) {
    boolean found = false;
    for(final LoadBalancerBackendServerDescription backendServerDescription : lb.getBackendServers()) {
      if(backendServerDescription.getInstancePort() == instancePort) {
        found =true;
        break;
      }
    }
    return found;
  }

  /**
   * Caller must have transaction for load balancer
   */
  public static LoadBalancerBackendServerDescription getBackendServerDescription(final LoadBalancer lb, final int instancePort) {
    // look for existing back-end server description; if none, create one
    LoadBalancerBackendServerDescription backend = null;
    for(final LoadBalancerBackendServerDescription backendServerDescription : lb.getBackendServers()){
      if(backendServerDescription.getInstancePort() == instancePort) {
        backend = backendServerDescription;
        break;
      }
    }
    return backend; 
  }
}
