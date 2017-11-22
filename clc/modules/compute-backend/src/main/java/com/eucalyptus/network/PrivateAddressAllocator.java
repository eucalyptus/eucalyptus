/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.network;

import com.eucalyptus.compute.common.internal.util.NotEnoughResourcesException;
import com.eucalyptus.compute.common.internal.util.ResourceAllocationException;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;

/**
 *
 */
public interface PrivateAddressAllocator {

  /**
   * Allocate an address
   *
   * @param scope          The scope (namespace) for the allocation
   * @param tag            The tag for the address, returned on release
   * @param addresses      The list of all addresses
   * @param addressCount   The number of addresses
   * @param allocatedCount The number of allocated addresses or -1 for unknown
   * @return The allocated address
   * @throws NotEnoughResourcesException If an address cannot be allocated.
   */
  String allocate( String scope, String tag, Iterable<Integer> addresses, int addressCount, int allocatedCount ) throws NotEnoughResourcesException;

  void associate( String address, VmInstance instance ) throws ResourceAllocationException;

  void associate( String address, NetworkInterface networkInterface ) throws ResourceAllocationException;

  /**
   * Release an address
   *
   * @param scope   The scope (namespace) for the allocation
   * @param address The address to release
   * @param ownerId The address owner (the owning resource identifier)
   * @return The tag for the address if any
   */
  String release( String scope, String address, String ownerId );

  boolean verify( String scope, String address, String ownerId );

  boolean releasing( Iterable<String> activeAddresses, String partition );
}
