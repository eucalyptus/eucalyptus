/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * 
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.imaging;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.CloudControllerColocatingBootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.FaultLogPrefix;
import com.eucalyptus.component.annotation.PolicyVendor;
import com.eucalyptus.component.annotation.PublicService;

/**
 * @author Chris Grzegorczyk <grze@eucalyptus.com>
 */
@PublicService
@FaultLogPrefix( "cloud" )
@PolicyVendor("euimager")
public class Imaging extends ComponentId {
  /**
   * This forces the service to be co-located with the ENABLED cloud controller.
   */
  @RunDuring( Bootstrap.Stage.RemoteServicesInit )
  @Provides( Imaging.class )
  public static class ColocationBootstrapper extends CloudControllerColocatingBootstrapper {
    public ColocationBootstrapper( ) {
      super( Imaging.class );
    }    
  }
}
