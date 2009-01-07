/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.entities.VmType;
import edu.ucsb.eucalyptus.msgs.*;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

public class ClusterState {

  private static Logger LOG = Logger.getLogger( ClusterState.class );

  private Cluster parent;
  private int virtualTimer;
  private ConcurrentNavigableMap<String, VmTypeAvailability> typeMap;

  private NavigableSet<ResourceToken> pendingTokens;
  private NavigableSet<ResourceToken> submittedTokens;
  private NavigableSet<Integer> availableVlans;

  public ClusterState( Cluster parent ) {
    this.parent = parent;
    this.virtualTimer = 0;
    this.typeMap = new ConcurrentSkipListMap<String, VmTypeAvailability>();

    for ( VmType v : VmTypes.list() )
      this.typeMap.putIfAbsent( v.getName(), new VmTypeAvailability( v, 0, 0 ) );

    this.pendingTokens = new ConcurrentSkipListSet<ResourceToken>();
    this.submittedTokens = new ConcurrentSkipListSet<ResourceToken>();

    this.availableVlans = new ConcurrentSkipListSet<Integer>();
    for ( int i = 10; i < 4096; i++ ) this.availableVlans.add( i );
  }

  public NetworkToken extantAllocation( String userName, String networkName, int vlan ) throws NetworkAlreadyExistsException {
    NetworkToken netToken = new NetworkToken( this.parent.getName(), userName, networkName, vlan );
    if ( !this.availableVlans.remove( vlan ) )
      throw new NetworkAlreadyExistsException();
    return netToken;
  }

  public NetworkToken getNetworkAllocation( String userName, String networkName ) throws NotEnoughResourcesAvailable {
    if ( this.availableVlans.isEmpty() ) throw new NotEnoughResourcesAvailable();
    int vlan = this.availableVlans.first();
    this.availableVlans.remove( vlan );
    NetworkToken token = new NetworkToken( this.parent.getName(), userName, networkName, vlan );
    return token;
  }

  public void releaseNetworkAllocation( NetworkToken token ) {
    this.availableVlans.add( token.getVlan() );
  }

  public void releaseResourceToken( ResourceToken token ) {
    this.pendingTokens.remove( token );
  }

  public int getAvailable( String vmTypeName ) {
    VmTypeAvailability vmType = this.typeMap.get( vmTypeName );
    NavigableSet<VmTypeAvailability> sorted = this.sorted();
    if ( sorted.headSet( VmTypeAvailability.ZERO ).contains( vmType ) )
      return sorted.higher( VmTypeAvailability.ZERO ).getAvailable();
    return vmType.getAvailable();
  }

  public int getMax( String vmTypeName ) {
    VmTypeAvailability vmType = this.typeMap.get( vmTypeName );
    return vmType.getMax();
  }

  public ResourceToken getResourceAllocation( String requestId, String userName, String vmTypeName, Integer quantity ) throws NotEnoughResourcesAvailable {
    VmTypeAvailability vmType = this.typeMap.get( vmTypeName );
    NavigableSet<VmTypeAvailability> sorted = this.sorted();

    //:: figure out how many resources are actually available :://
    int available = 0;
    if ( sorted.headSet( VmTypeAvailability.ZERO ).contains( vmType ) )
      available = sorted.higher( VmTypeAvailability.ZERO ).getAvailable();
    else
      available = vmType.getAvailable();

    //:: if not enough, then bail out :://
    if ( available < quantity ) throw new NotEnoughResourcesAvailable();

    //:: decrement available resources across the "active" partition :://
    for ( VmTypeAvailability v : sorted.tailSet( VmTypeAvailability.ZERO ) )
      v.decrement( quantity );

    //:: if vmType is IN the "active" partition, make it the first entry in the partition by disabling its tailSet:://
    if ( !sorted.headSet( VmTypeAvailability.ZERO ).contains( vmType ) )
      for ( VmTypeAvailability v : sorted.headSet( vmType ) )
        v.setDisabled( true );

    ResourceToken token = new ResourceToken( this.parent.getName(), requestId, userName, quantity, this.virtualTimer++, vmTypeName );
    this.pendingTokens.add( token );
    return token;
  }

  public void submitResourceAllocation( ResourceToken token ) throws NoSuchTokenException {
    if ( this.pendingTokens.remove( token ) )
      this.submittedTokens.add( token );
    else
      throw new NoSuchTokenException();
  }

  public void redeemToken( ResourceToken token ) throws NoSuchTokenException {
    if ( this.submittedTokens.remove( token ) ) ;
    else
      throw new NoSuchTokenException();
  }

  public void update( List<ResourceType> rscUpdate ) {
    for ( ResourceType rsc : rscUpdate ) {
      VmTypeAvailability vmAvailable = this.typeMap.get( rsc.getInstanceType().getName() );
      if ( vmAvailable == null ) continue;
      int outstandingCount = 0;
      for ( ResourceToken token : this.pendingTokens )
        if ( token.getVmType().equals( rsc.getInstanceType().getName() ) )
          outstandingCount++;
      for ( ResourceToken token : this.submittedTokens )
        if ( token.getVmType().equals( rsc.getInstanceType().getName() ) )
          outstandingCount++;
      vmAvailable.setAvailable( rsc.getAvailableInstances() - outstandingCount );
      vmAvailable.setMax( rsc.getMaxInstances() );
    }
  }

  public static ResourceComparator getComparator( VmTypeInfo vmTypeInfo ) {
    return new ResourceComparator( vmTypeInfo );
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    ClusterState cluster = ( ClusterState ) o;

    if ( !this.parent.getName().equals( cluster.parent.getName() ) ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return this.parent.getClusterInfo().hashCode();
  }

  private NavigableSet<VmTypeAvailability> sorted() {
    NavigableSet<VmTypeAvailability> available = new TreeSet<VmTypeAvailability>();
    for ( String typeName : this.typeMap.keySet() )
      available.add( this.typeMap.get( typeName ) );
    available.add( VmTypeAvailability.ZERO );
    return available;
  }

  public VmTypeAvailability getAvailability( String vmTypeName ) {
    return this.typeMap.get( vmTypeName );
  }

  public static class ResourceComparator implements Comparator<ClusterState> {

    private VmTypeInfo vmTypeInfo;

    ResourceComparator( final VmTypeInfo vmTypeInfo ) {
      this.vmTypeInfo = vmTypeInfo;
    }

    public int compare( final ClusterState o1, final ClusterState o2 ) {
      return o1.getAvailable( this.vmTypeInfo.getName() ) - o2.getAvailable( this.vmTypeInfo.getName() );
    }
  }

  @Override
  public String toString() {
    return "ClusterState{" +
           "typeMap=" + typeMap +
           ", pendingTokens=" + pendingTokens +
           ", submittedTokens=" + submittedTokens +
           ", availableVlans=" + availableVlans.size() +
           '}';
  }
}

