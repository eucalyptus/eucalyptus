/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.cluster;

import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.VmType;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.cloud.NetworkToken;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import com.eucalyptus.records.EventRecord;
import edu.ucsb.eucalyptus.msgs.ResourceType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class ClusterNodeState {
  private static Logger LOG = Logger.getLogger( ClusterNodeState.class );
  private ConcurrentNavigableMap<String, VmTypeAvailability> typeMap;
  private NavigableSet<ResourceToken> pendingTokens;
  private NavigableSet<ResourceToken> submittedTokens;
  private NavigableSet<ResourceToken> redeemedTokens;
  private int virtualTimer;
  private String clusterName;
  
  public ClusterNodeState( String clusterName ) {
    this.clusterName = clusterName;
    this.typeMap = new ConcurrentSkipListMap<String, VmTypeAvailability>();

    for ( VmType v : VmTypes.list() )
      this.typeMap.putIfAbsent( v.getName(), new VmTypeAvailability( v, 0, 0 ) );

    this.pendingTokens = new ConcurrentSkipListSet<ResourceToken>();
    this.submittedTokens = new ConcurrentSkipListSet<ResourceToken>();
    this.redeemedTokens = new ConcurrentSkipListSet<ResourceToken>();
  }

  public synchronized ResourceToken getResourceAllocation( String requestId, String userName, String vmTypeName, Integer quantity ) throws NotEnoughResourcesAvailable {
    VmTypeAvailability vmType = this.typeMap.get( vmTypeName );
    NavigableSet<VmTypeAvailability> sorted = this.sorted();

    LOG.debug( LogUtil.header("BEFORE ALLOCATE") );
    LOG.debug( sorted );
    //:: if not enough, then bail out :://
    if ( vmType.getAvailable() < quantity ) throw new NotEnoughResourcesAvailable("Not enough resources available: vm resources");

    Set<VmTypeAvailability> tailSet = sorted.tailSet( vmType );
    Set<VmTypeAvailability> headSet = sorted.headSet( vmType );
    LOG.debug( LogUtil.header("DURING ALLOCATE") );
    LOG.debug(LogUtil.subheader( "TAILSET: \n" + tailSet) );
    LOG.debug(LogUtil.subheader( "HEADSET: \n" + headSet) );
    //:: decrement available resources across the "active" partition :://
    for ( VmTypeAvailability v : tailSet )
      v.decrement( quantity );
    for ( VmTypeAvailability v : headSet )
      v.setAvailable( vmType.getAvailable() );
    LOG.debug( LogUtil.header("AFTER ALLOCATE") );
    LOG.debug( sorted );

    ResourceToken token = new ResourceToken( this.clusterName, requestId, userName, quantity, this.virtualTimer++, vmTypeName );
    EventRecord.caller( ResourceToken.class, EventType.TOKEN_RESERVED, token.toString( ) ).info( );
    this.pendingTokens.add( token );
    return token;
  }
  
  public synchronized List<ResourceToken> splitToken( ResourceToken token ) throws NoSuchTokenException {
    EventRecord.caller( ResourceToken.class, EventType.TOKEN_SPLIT, token.toString( ) ).info( );
    if( !this.pendingTokens.contains( token ) ) {
      throw new NoSuchTokenException( "Splitting the requested token is not possible since it is not pending: " + token );
    }
    List<ResourceToken> childTokens = Lists.newArrayList( );
    for( int index = 0; index < token.getAmount( ); index++ ) {
      NetworkToken primaryNet = token.getPrimaryNetwork( );
      ResourceToken childToken = new ResourceToken( token.getCluster( ), token.getCorrelationId( )+index, token.getUserName( ), 1, this.virtualTimer++, token.getVmType( ) );
      if( token.getAddresses( ).size( ) > index ) {
        childToken.getAddresses( ).add( token.getAddresses( ).get( index ) );
      }
      if( token.getInstanceIds( ).size( ) > index ) {
        childToken.getInstanceIds( ).add( token.getInstanceIds( ).get( index ) );
      }
      if( primaryNet != null ) {
        NetworkToken childNet = new NetworkToken( primaryNet.getCluster( ), primaryNet.getUserName( ), primaryNet.getNetworkName( ), primaryNet.getVlan( ) );
        childNet.getIndexes( ).add( primaryNet.getIndexes( ).pollFirst( ) );
        childToken.getNetworkTokens( ).add( childNet );
      }
      EventRecord.caller( ResourceToken.class, EventType.TOKEN_CHILD, childToken.toString( ) ).info( );
      childTokens.add( childToken );
    }
    this.pendingTokens.remove( token );
    this.pendingTokens.addAll( childTokens );
    return childTokens;
  }

  public synchronized void releaseToken( ResourceToken token ) {
    EventRecord.caller( ResourceToken.class, EventType.TOKEN_RETURNED, token.toString( ) ).info( );
    this.pendingTokens.remove( token );
    this.submittedTokens.remove( token );
    this.redeemedTokens.remove( token );
  }

  public synchronized void submitToken( ResourceToken token ) throws NoSuchTokenException {
    EventRecord.caller( ResourceToken.class, EventType.TOKEN_SUBMITTED, token.toString( ) ).info( );
    if ( this.pendingTokens.remove( token ) )
      this.submittedTokens.add( token );
    else
      throw new NoSuchTokenException();
  }

  public synchronized void redeemToken( ResourceToken token ) throws NoSuchTokenException {
    EventRecord.caller( ResourceToken.class, EventType.TOKEN_REDEEMED, token.toString( ) ).info( );
    if ( this.submittedTokens.remove( token ) || this.pendingTokens.remove( token ) )
      this.redeemedTokens.add( token );
    else
      throw new NoSuchTokenException();
  }


  public synchronized void update( List<ResourceType> rscUpdate ) {
    int outstandingCount = 0;
    int pending = 0, submitted = 0, redeemed = 0;
    for( ResourceToken t : this.pendingTokens )
      pending += t.getAmount();
    for( ResourceToken t : this.submittedTokens )
      submitted += t.getAmount();
    for( ResourceToken t : this.redeemedTokens )
      redeemed += t.getAmount();
    outstandingCount = pending + submitted;
    EventRecord.here( ClusterNodeState.class, EventType.CLUSTER_STATE_UPDATE, this.clusterName, String.format( "outstanding=%d:pending=%d:submitted=%d:redeemed=%d", outstandingCount, pending, submitted, redeemed ) ).info( );
    this.redeemedTokens.clear();

    StringBuffer before = new StringBuffer();
    StringBuffer after = new StringBuffer();
    for ( ResourceType rsc : rscUpdate ) {
      VmTypeAvailability vmAvailable = this.typeMap.get( rsc.getInstanceType().getName() );
      if ( vmAvailable == null ) continue;
      before.append( String.format( ":%s:%d/%d", vmAvailable.getType( ).getName( ), vmAvailable.getAvailable( ), vmAvailable.getMax( ) ) );
      vmAvailable.setAvailable( rsc.getAvailableInstances() );
      vmAvailable.decrement( outstandingCount );
      vmAvailable.setMax( rsc.getMaxInstances() );
      after.append( String.format( ":%s:%d/%d", vmAvailable.getType( ).getName( ), vmAvailable.getAvailable( ), vmAvailable.getMax( ) ) );
    }
    EventRecord.here( ClusterNodeState.class, EventType.CLUSTER_STATE_UPDATE, this.clusterName, "ANTE" + before.toString( ) ).info( );
    EventRecord.here( ClusterNodeState.class, EventType.CLUSTER_STATE_UPDATE, this.clusterName, "POST" + after.toString( ) ).info( );
  }

  private NavigableSet<VmTypeAvailability> sorted() {
    NavigableSet<VmTypeAvailability> available = new TreeSet<VmTypeAvailability>();
    for ( String typeName : this.typeMap.keySet() )
      available.add( this.typeMap.get( typeName ) );
    available.add( VmTypeAvailability.ZERO );
    LOG.debug("Resource information for " + this.clusterName );
    return available;
  }

  
  
  public VmTypeAvailability getAvailability( String vmTypeName ) {
    return this.typeMap.get( vmTypeName );
  }

  public static ResourceComparator getComparator( VmTypeInfo vmTypeInfo ) {
    return new ResourceComparator( vmTypeInfo );
  }

  public static class ResourceComparator implements Comparator<ClusterNodeState> {

    private VmTypeInfo vmTypeInfo;

    ResourceComparator( final VmTypeInfo vmTypeInfo ) {
      this.vmTypeInfo = vmTypeInfo;
    }

    public int compare( final ClusterNodeState o1, final ClusterNodeState o2 ) {
      return o1.getAvailability( this.vmTypeInfo.getName() ).getAvailable() - o2.getAvailability( this.vmTypeInfo.getName() ).getAvailable();
    }
  }

  @Override
  public String toString( ) {
    return String.format(
                          "ClusterNodeState [clusterName=%s, pendingTokens=%s, redeemedTokens=%s, submittedTokens=%s, typeMap=%s]",
                          this.clusterName, this.pendingTokens, this.redeemedTokens, this.submittedTokens, this.typeMap );
  }

}
