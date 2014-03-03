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
package com.eucalyptus.loadbalancing.common;

import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.CloudControllerColocatingBootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.FaultLogPrefix;
import com.eucalyptus.component.annotation.Partition;
import com.eucalyptus.component.annotation.PolicyVendor;
import com.eucalyptus.component.id.Eucalyptus;

/**
 * @author Chris Grzegorczyk <grze@eucalyptus.com>
 */
@PolicyVendor( PolicySpec.VENDOR_LOADBALANCING )
@Partition( Eucalyptus.class )
@FaultLogPrefix( "cloud" )
public class LoadBalancingBackend extends ComponentId {
  private static final long serialVersionUID = 1L;

  @Override
  public Boolean isCloudLocal() {
    return Boolean.TRUE;
  }

  @Override
  public boolean isDistributedService() {
    return true;
  }

  @Override
  public boolean isRegisterable() {
    return false;
  }

  @Override
  public boolean isImpersonationSupported( ) {
    return true;
  }

  /**
   * This forces the service to be co-located with the ENABLED cloud controller.
   */
  @RunDuring( Bootstrap.Stage.RemoteServicesInit )
  @Provides( LoadBalancingBackend.class )
  public static class ColocationBootstrapper extends CloudControllerColocatingBootstrapper {
    public ColocationBootstrapper( ) {
      super( LoadBalancingBackend.class );
    }
  }
}

