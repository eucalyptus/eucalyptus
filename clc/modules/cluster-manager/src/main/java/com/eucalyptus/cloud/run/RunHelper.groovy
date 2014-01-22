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
package com.eucalyptus.cloud.run

import com.eucalyptus.cloud.ResourceToken
import com.eucalyptus.cloud.VmRunType.Builder as VmRunBuilder
import com.eucalyptus.cloud.util.MetadataException
import com.eucalyptus.network.PrepareNetworkResourcesType
import com.eucalyptus.vm.VmInstance
import com.eucalyptus.vm.VmInstance.Builder as VmInstanceBuilder
import edu.ucsb.eucalyptus.cloud.VmInfo
import groovy.transform.CompileStatic

/**
 * Helper for running instances
 */
@CompileStatic
interface RunHelper {

  void verifyAllocation( Allocations.Allocation allocation ) throws MetadataException

  void prepareNetworkAllocation( Allocations.Allocation allocation,
                                 PrepareNetworkResourcesType prepareNetworkResourcesType )

  void prepareVmRunType( ResourceToken resourceToken,
                         VmRunBuilder builder );


  void prepareVmInstance( ResourceToken resourceToken,
                          VmInstanceBuilder builder)

  void prepareAllocation( VmInstance instance,
                          Allocations.Allocation allocation )

  void prepareAllocation( VmInfo vmInfo,
                          Allocations.Allocation allocation )

  void startVmInstance( ResourceToken resourceToken,
                        VmInstance instance )
}
