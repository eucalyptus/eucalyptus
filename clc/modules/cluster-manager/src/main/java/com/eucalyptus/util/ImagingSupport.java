/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

package com.eucalyptus.util;

import com.google.common.base.Predicate;

import org.apache.log4j.Logger;

import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;

public class ImagingSupport {
  private static Logger LOG = Logger.getLogger( ImagingSupport.class );
  
  public static void terminateInstancesWaitingImageConversion(String imageId) {
    LOG.debug("Image that failed conversion: " + imageId);
    if ( imageId != null ) {
      for( VmInstance vm : VmInstances.list( new InstanceByImageId(imageId)) ) {
        if ( VmInstance.VmState.PENDING.apply(vm) ) {
          LOG.debug("Shutting down instance: " + vm.getInstanceId());
          VmInstances.setState(vm, VmInstance.VmState.SHUTTING_DOWN, VmInstance.Reason.FAILED );
        }
      }
    }
  }

  private static class InstanceByImageId implements Predicate<VmInstance> {
    private String imageId;
    public InstanceByImageId(String imageId) {
      this.imageId = imageId;
    }
    @Override
    public boolean apply( final VmInstance input ) {
      return imageId.equals(input.getImageId());
    }
  }
}
