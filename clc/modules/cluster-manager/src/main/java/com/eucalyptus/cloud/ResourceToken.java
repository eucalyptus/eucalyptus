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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.cloud;

import static com.eucalyptus.cloud.VmInstanceLifecycleHelpers.NetworkResourceVmInstanceLifecycleHelper;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.Volume;
import com.eucalyptus.compute.common.CloudMetadata.VmInstanceMetadata;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.ResourceState.NoSuchTokenException;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.compute.common.network.Networking;
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesType;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypedContext;
import com.eucalyptus.util.TypedKey;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vmtypes.VmTypes;
import com.google.common.collect.Maps;

public class ResourceToken implements VmInstanceMetadata, Comparable<ResourceToken> {
  private static Logger       LOG    = Logger.getLogger( ResourceToken.class );
  private final Allocation    allocation;
  private final Integer       launchIndex;
  private final String        instanceId;
  private final String        instanceUuid;
  @Nullable
  private Volume              rootVolume;
  @Nullable
  private Map<String, Volume> ebsVolumes;
  @Nullable
  private Map<String, String> ephemeralDisks;
  private final TypedContext  resourceContext = TypedContext.newTypedContext( );
  private final Date          creationTime;
  @Nullable
  private VmInstance          vmInst;
  private final Cluster       cluster;
  private boolean             aborted;
  private final boolean       unorderedType;

  public ResourceToken( final Allocation allocInfo, final int launchIndex ) {
    this.allocation = allocInfo;
    this.launchIndex = launchIndex;
    this.instanceId = allocInfo.getInstanceId( launchIndex );
    this.instanceUuid = allocInfo.getInstanceUuid( launchIndex );
    if ( this.instanceId == null || this.instanceUuid == null ) {
      throw new IllegalArgumentException( "Cannot create resource token with null instance id or uuid: " + allocInfo );
    }
    this.creationTime = Calendar.getInstance( ).getTime( );
    ServiceConfiguration config = Topology.lookup( ClusterController.class, this.getAllocationInfo( ).getPartition( ) );
    this.cluster = Clusters.lookup( config );
    this.unorderedType = VmTypes.isUnorderedType( allocInfo.getVmType( ) );
  }
  
  public Allocation getAllocationInfo( ) {
    return this.allocation;
  }
  
  public String getInstanceId( ) {
    return this.instanceId;
  }
  
  public Integer getAmount( ) {
    return 1;
  }
  
  public Date getCreationTime( ) {
    return this.creationTime;
  }
  
  @Override
  public int compareTo( final ResourceToken that ) {
    return this.instanceId.compareTo( that.instanceId );
  }
  
  public String getInstanceUuid( ) {
    return this.instanceUuid;
  }
  
  public Integer getLaunchIndex( ) {
    return this.launchIndex;
  }

  public <T> T getAttribute( final TypedKey<T> key ) {
    return resourceContext.get( key );
  }

  public <T> T setAttribute( final TypedKey<T> key, final T value ) {
    return resourceContext.put( key, value );
  }

  public <T> T removeAttribute( final TypedKey<T> key ) {
    return resourceContext.remove( key );
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
        releaseNetworkResourcesType.getResources( ).addAll( getAttribute( NetworkResourceVmInstanceLifecycleHelper.NetworkResourcesKey ) );
        Networking.getInstance( ).release( releaseNetworkResourcesType );
      } catch ( final Exception ex ) {
        LOG.error( ex, ex );
      }

      if ( this.vmInst != null ) {
        try {
          this.vmInst.release( );
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
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + this.instanceId.hashCode( );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( obj == null ) {
      return false;
    }
    if ( getClass( ) != obj.getClass( ) ) {
      return false;
    }
    ResourceToken other = ( ResourceToken ) obj;
    if ( !this.instanceId.equals( other.instanceId ) ) {
      return false;
    }
    return true;
  }
  
  static Logger getLOG( ) {
    return LOG;
  }
  
  Allocation getAllocation( ) {
    return this.allocation;
  }
  
  public void submit( ) throws NoSuchTokenException {
    this.cluster.getNodeState( ).submitToken( this );
  }
  
  public void redeem( ) throws NoSuchTokenException {
    this.cluster.getNodeState( ).redeemToken( this );
  }
  
  public void release( ) throws NoSuchTokenException {
    this.cluster.getNodeState( ).releaseToken( this );
  }

  public boolean isPending( ) {
    return this.cluster.getNodeState( ).isPending( this );
  }
  
  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( "ResourceToken:" );
    if ( this.instanceId != null ) {
      builder.append( this.instanceId ).append( ":" );
    }
    builder.append( "resources=" ).append( resourceContext );
    return builder.toString( );
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

  public boolean isUnorderedType( ) {
    return this.unorderedType;
  }

  public Cluster getCluster( ) {
    return this.cluster;
  }

}
