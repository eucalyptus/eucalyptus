/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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

import java.util.Map;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.cloud.run.Allocations;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.common.ResourceToken;
import com.eucalyptus.cluster.common.ClusterController;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.internal.blockstorage.Volume;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.network.Networking;
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesType;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.vmtypes.VmTypes;
import com.google.common.collect.Maps;

/**
 *
 */
public class VmInstanceToken extends ResourceToken implements CloudMetadata.VmInstanceMetadata {
  private static Logger LOG    = Logger.getLogger( ResourceToken.class );
  private final Allocations.Allocation allocation;
  private final Integer       launchIndex;
  private final String        instanceId;
  private final String        instanceUuid;
  @Nullable
  private Volume rootVolume;
  @Nullable
  private Map<String, Volume> ebsVolumes;
  @Nullable
  private Map<String, String> ephemeralDisks;
  @Nullable
  private VmInstance vmInst;
  private boolean             aborted;
  private boolean             zombie;

  public VmInstanceToken( final Allocations.Allocation allocInfo, final int launchIndex ) {
    super(
        Clusters.lookupAny( Topology.lookup( ClusterController.class, allocInfo.getPartition( ) ) ),
        allocInfo.getVmType( ),
        VmTypes.isUnorderedType( allocInfo.getVmType( ) ),
        allocInfo.getInstanceId( launchIndex )
    );
    this.allocation = allocInfo;
    this.launchIndex = launchIndex;
    this.instanceId = allocInfo.getInstanceId( launchIndex );
    this.instanceUuid = allocInfo.getInstanceUuid( launchIndex );
    if ( this.instanceId == null || this.instanceUuid == null ) {
      throw new IllegalArgumentException( "Cannot create resource token with null instance id or uuid: " + allocInfo );
    }
  }

  public Allocations.Allocation getAllocationInfo( ) {
    return this.allocation;
  }

  public String getInstanceId( ) {
    return this.instanceId;
  }

  public Integer getAmount( ) {
    return 1;
  }

  public String getInstanceUuid( ) {
    return this.instanceUuid;
  }

  public Integer getLaunchIndex( ) {
    return this.launchIndex;
  }

  public void abort( ) {
    if ( aborted ) return;
    aborted = true;

    LOG.debug( this );
    if ( isPending( ) ) { // release unused resources
      try {
        this.release( );
      } catch ( final Exception ex ) {
        LOG.error( ex, ex );
      }

      try {
        final ReleaseNetworkResourcesType releaseNetworkResourcesType = new ReleaseNetworkResourcesType( );
        releaseNetworkResourcesType.setVpc( allocation.getSubnet( ) == null ?
            null :
            CloudMetadatas.toDisplayName( ).apply( allocation.getSubnet( ).getVpc( ) ) );
        releaseNetworkResourcesType.getResources( ).addAll( getAttribute( VmInstanceLifecycleHelper.NetworkResourcesKey ) );
        Networking.getInstance( ).release( releaseNetworkResourcesType );
      } catch ( final Exception ex ) {
        LOG.error( ex, ex );
      }

      if ( this.vmInst != null ) {
        try {
          VmInstances.terminated( this.vmInst );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    } else { // redeem and release later (if not used)
      try {
        this.redeem( );
      } catch ( final Exception ex ) {
        LOG.error( ex, ex );
      }
    }
  }

  static Logger getLOG( ) {
    return LOG;
  }

  Allocations.Allocation getAllocation( ) {
    return this.allocation;
  }

  public void setVmInstance( VmInstance vmInst ) {
    this.vmInst = vmInst;
  }

  public VmInstance getVmInstance( ) {
    return this.vmInst;
  }

  @Override
  public String getDisplayName( ) {
    return this.getInstanceId( );
  }

  public boolean isCommitted( ) {
    return this.allocation.isCommitted( );
  }

  @Override
  public OwnerFullName getOwner( ) {
    return this.allocation.getOwnerFullName( );
  }

  public Volume getRootVolume( ) {
    return this.rootVolume;
  }

  public void setRootVolume( Volume rootVolume ) {
    this.rootVolume = rootVolume;
  }

  public Map<String, Volume> getEbsVolumes() {
    if (this.ebsVolumes == null) {
      this.ebsVolumes = Maps.newHashMap();
    }
    return ebsVolumes;
  }

  public void setEbsVolumes(Map<String, Volume> ebsVolumes) {
    this.ebsVolumes = ebsVolumes;
  }

  public Map<String, String> getEphemeralDisks() {
    if (this.ephemeralDisks == null){
      this.ephemeralDisks = Maps.newHashMap();
    }
    return ephemeralDisks;
  }

  public void setEphemeralDisks(Map<String, String> ephemeralDisks) {
    this.ephemeralDisks = ephemeralDisks;
  }

  public boolean isZombie() {
    return zombie;
  }

  public void setZombie( final boolean zombie ) {
    this.zombie = zombie;
  }
}
