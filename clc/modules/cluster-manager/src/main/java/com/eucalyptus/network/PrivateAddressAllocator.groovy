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
package com.eucalyptus.network

import com.eucalyptus.compute.common.internal.util.NotEnoughResourcesException
import com.eucalyptus.compute.common.internal.util.ResourceAllocationException
import com.eucalyptus.compute.common.internal.vm.VmInstance
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface as VpcNetworkInterface
import groovy.transform.CompileStatic

/**
 *
 */
@CompileStatic
interface PrivateAddressAllocator {

  /**
   * Allocate an address
   *
   * @param scope The scope (namespace) for the allocation
   * @param tag The tag for the address, returned on release
   * @param addresses The list of all addresses
   * @param addressCount The number of addresses
   * @param allocatedCount The number of allocated addresses or -1 for unknown
   * @return The allocated address
   * @throws NotEnoughResourcesException If an address cannot be allocated.
   */
  String allocate( String scope, String tag, Iterable<Integer> addresses, int addressCount, int allocatedCount ) throws NotEnoughResourcesException

  void associate( String address, VmInstance instance ) throws ResourceAllocationException

  void associate( String address, VpcNetworkInterface networkInterface ) throws ResourceAllocationException

  /**
   * Release an address
   *
   * @param scope The scope (namespace) for the allocation
   * @param address The address to release
   * @param ownerId The address owner (the owning resource identifier)
   * @return The tag for the address if any
   */
  String release( String scope, String address, String ownerId )

  boolean verify( String scope, String address, String ownerId )

  boolean releasing( Iterable<String> activeAddresses, String partition )

}
