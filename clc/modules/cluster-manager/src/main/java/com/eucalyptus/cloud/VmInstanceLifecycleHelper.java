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
package com.eucalyptus.cloud;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.cluster.common.ResourceToken;
import com.eucalyptus.cluster.common.msgs.VmRunType.Builder;
import com.eucalyptus.compute.common.internal.util.MetadataException;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmInstance.VmState;
import com.eucalyptus.compute.common.network.NetworkResource;
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesResultType;
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesType;
import com.eucalyptus.util.TypedKey;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;

/**
 * Helper for instance lifecycle tasks.
 */
public interface VmInstanceLifecycleHelper {

  TypedKey<Set<NetworkResource>> NetworkResourcesKey =
      TypedKey.create( "NetworkResources", Sets::newHashSet );

  TypedKey<String> DefaultVpcIdKey =
      TypedKey.create( "DefaultVpcId", Suppliers.<String>ofInstance( null ) );

  /**
   * Verify that the allocation is valid.
   *
   * Populate verified references in the allocation but do not allocate
   * resources.
   */
  void verifyAllocation( Allocation allocation ) throws MetadataException;

  /**
   * Prepare the network resource request for the allocation.
   *
   * Request resource allocations, typically for each token.
   */
  void prepareNetworkAllocation( Allocation allocation, PrepareNetworkResourcesType prepareNetworkResourcesType );

  /**
   * Verify network resource allocation successful.
   *
   * Tokens will have been updated prior to this call.
   */
  void verifyNetworkAllocation( Allocation allocation, PrepareNetworkResourcesResultType prepareNetworkResourcesResultType );

  void prepareVmRunType( ResourceToken resourceToken, Builder builder );

  /**
   * Build the instance based on information from the corresponding token.
   *
   * @see VmInstances.Builder#onBuild
   */
  void prepareVmInstance( ResourceToken resourceToken, VmInstances.Builder builder );

  void prepareAllocation( VmInstance instance, Allocation allocation );

  void startVmInstance( ResourceToken resourceToken, VmInstance instance );

  /**
   * Can be called multiple times during clean with various states.
   *
   * <p>Guaranteed to be called with a persistent VmInstance in the TERMINATED
   * state.</p>
   *
   * @param instance The instance being cleaned up, may not be persistent.
   * @param state    The instance state
   */
  void cleanUpInstance( VmInstance instance, VmState state );

  static VmInstanceLifecycleHelper get( ) {
    return Registry.helper.get( );
  }

  class Registry {
    public static AtomicReference<VmInstanceLifecycleHelper> helper = new AtomicReference<>(  );
  }
}
