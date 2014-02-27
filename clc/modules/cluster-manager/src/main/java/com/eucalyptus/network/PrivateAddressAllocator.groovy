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
package com.eucalyptus.network

import com.eucalyptus.cloud.util.NotEnoughResourcesException
import com.eucalyptus.cloud.util.ResourceAllocationException
import com.eucalyptus.vm.VmInstance
import groovy.transform.CompileStatic

/**
 *
 */
@CompileStatic
interface PrivateAddressAllocator {

  String allocate( Iterable<Integer> addresses ) throws NotEnoughResourcesException

  void associate( String address, VmInstance instance ) throws ResourceAllocationException

  void release( String address, String ownerId )

  boolean verify( String address, String ownerId )

  void releasing( Iterable<String> activeAddresses, String partition )

}
