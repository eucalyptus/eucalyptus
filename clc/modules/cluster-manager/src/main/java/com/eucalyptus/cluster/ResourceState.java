/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.cluster;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import com.eucalyptus.cloud.ResourceToken;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.cloud.util.NotEnoughResourcesException;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.vmtypes.VmType;
import com.eucalyptus.vmtypes.VmTypes;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.ResourceType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class ResourceState {
  private static Logger                                      LOG = Logger.getLogger( ResourceState.class );
  private ConcurrentNavigableMap<String, VmTypeAvailability> typeMap;
  private NavigableSet<ResourceToken>                        pendingTokens;
  private NavigableSet<ResourceToken>                        submittedTokens;
  private NavigableSet<ResourceToken>                        redeemedTokens;
  private String                                             clusterName;
  public static class NoSuchTokenException extends Exception {

    public NoSuchTokenException( String message ) {
      super( message );
    }

  }

  public ResourceState( String clusterName ) {
    this.clusterName = clusterName;
    this.typeMap = new ConcurrentSkipListMap<String, VmTypeAvailability>( );
    
    for ( VmType v : VmTypes.list( ) )
      this.typeMap.putIfAbsent( v.getName( ), new VmTypeAvailability( v, 0, 0 ) );
    
    this.pendingTokens = new ConcurrentSkipListSet<ResourceToken>( );
    this.submittedTokens = new ConcurrentSkipListSet<ResourceToken>( );
    this.redeemedTokens = new ConcurrentSkipListSet<ResourceToken>( );
  }
  
  public boolean hasUnorderedTokens( ) {
    return Iterables.any( this.pendingTokens, new Predicate<ResourceToken>( ) {
      
      @Override
      public boolean apply( ResourceToken arg0 ) {
        return arg0.isUnorderedType( );
      }
    } );
  }
  
  public synchronized List<ResourceToken> requestResourceAllocation( Allocation allocInfo, int minAmount, int maxAmount ) throws NotEnoughResourcesException {
    VmTypeAvailability vmTypeStatus = this.typeMap.get( allocInfo.getVmType( ).getName( ) );
    Integer available = vmTypeStatus.getAvailable( );
    NavigableSet<VmTypeAvailability> sorted = this.sorted( );
    LOG.debug( LogUtil.header( "BEFORE ALLOCATE" ) );
    LOG.debug( sorted );
    //:: if not enough, then bail out :://
    Integer quantity = minAmount;
    if ( vmTypeStatus.getAvailable( ) < minAmount ) {
      throw new NotEnoughResourcesException( "Not enough resources (" + available + " < " + minAmount + ": vm instances." );
    } else {
      quantity = ( maxAmount < available
                                        ? maxAmount
                                        : available );
    }
    
    Set<VmTypeAvailability> tailSet = sorted.tailSet( vmTypeStatus );
    Set<VmTypeAvailability> headSet = sorted.headSet( vmTypeStatus );
    LOG.debug( LogUtil.header( "DURING ALLOCATE" ) );
    LOG.debug( LogUtil.subheader( "TAILSET: \n" + tailSet ) );
    LOG.debug( LogUtil.subheader( "HEADSET: \n" + headSet ) );
    //:: decrement available resources across the "active" partition :://
    for ( VmTypeAvailability v : tailSet )
      v.decrement( quantity );
    for ( VmTypeAvailability v : headSet )
      v.setAvailable( vmTypeStatus.getAvailable( ) );
    LOG.debug( LogUtil.header( "AFTER ALLOCATE" ) );
    LOG.debug( sorted );
    List<ResourceToken> tokenList = Lists.newArrayList( );
    for ( int i = 0; i < quantity; i++ ) {
      try {
        ResourceToken token = new ResourceToken( allocInfo, i );
        LOG.debug( EventType.TOKEN_RESERVED.name( ) + ": " + token.toString( ) );
        this.pendingTokens.add( token );
        tokenList.add( token );
      } catch ( Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
        for ( ResourceToken token : tokenList ) {
          this.pendingTokens.remove( token );
        }
      }
    }
    return tokenList;
  }
  
  public synchronized void releaseToken( ResourceToken token ) {
    LOG.debug( EventType.TOKEN_RELEASED.name( ) + ": " + token.toString( ) );
    this.pendingTokens.remove( token );
    this.submittedTokens.remove( token );
    this.redeemedTokens.remove( token );
  }
  
  public synchronized void submitToken( ResourceToken token ) throws NoSuchTokenException {
    LOG.debug( EventType.TOKEN_SUBMITTED.name( ) + ": " + token.toString( ) );
    if ( this.pendingTokens.remove( token ) ) {
      this.submittedTokens.add( token );
    } else {
      throw new NoSuchTokenException( token.toString( ) );
    }
  }
  
  public synchronized void redeemToken( ResourceToken token ) throws NoSuchTokenException {
    LOG.debug( EventType.TOKEN_REDEEMED.name( ) + ": " + token.toString( ) );
    if ( this.submittedTokens.remove( token ) || this.pendingTokens.remove( token ) ) {
      this.redeemedTokens.add( token );
    } else {
      LOG.error(
        "Failed to find token: "
            + token
            + "\n"
            + Joiner.on( "\n" ).join( "pending", this.pendingTokens, "submitted", this.submittedTokens, "redeemed", this.redeemedTokens ),
        new NoSuchTokenException( token.toString( ) ) );
    }
  }

  public synchronized boolean isPending( final ResourceToken token ) {
    return this.pendingTokens.contains( token );
  }

  public synchronized void update( List<ResourceType> rscUpdate ) {
    long expiryAge = System.currentTimeMillis( ) - TimeUnit.MINUTES.toMillis( getExpiryMinutes( 15 ) );
    expirePendingTokens( expiryAge );

    int outstandingCount = 0;
    int pending = 0, submitted = 0, redeemed = 0;
    for ( ResourceToken t : this.pendingTokens )
      pending += t.getAmount( );
    for ( ResourceToken t : this.submittedTokens )
      submitted += t.getAmount( );
    for ( ResourceToken t : this.redeemedTokens )
      redeemed += t.getAmount( );
    outstandingCount = pending + submitted;
    EventRecord.here( ResourceState.class, EventType.CLUSTER_STATE_UPDATE, this.clusterName,
                      String.format( "outstanding=%d:pending=%d:submitted=%d:redeemed=%d", outstandingCount, pending, submitted, redeemed ) ).info( );
    this.redeemedTokens.clear( );
    
    StringBuilder before = new StringBuilder( );
    StringBuilder after = new StringBuilder( );
    for ( ResourceType rsc : rscUpdate ) {
      VmTypeAvailability vmAvailable = this.typeMap.get( rsc.getInstanceType( ).getName( ) );
      if ( vmAvailable == null ) continue;
      before.append( String.format( ":%s:%d/%d", vmAvailable.getType( ).getName( ), vmAvailable.getAvailable( ), vmAvailable.getMax( ) ) );
      vmAvailable.setAvailable( rsc.getAvailableInstances( ) );
      vmAvailable.decrement( outstandingCount );
      vmAvailable.setMax( rsc.getMaxInstances( ) );
      after.append( String.format( ":%s:%d/%d", vmAvailable.getType( ).getName( ), vmAvailable.getAvailable( ), vmAvailable.getMax( ) ) );
    }
    EventRecord.here( ResourceState.class, EventType.CLUSTER_STATE_UPDATE, this.clusterName, "ANTE" + before.toString( ) ).info( );
    EventRecord.here( ResourceState.class, EventType.CLUSTER_STATE_UPDATE, this.clusterName, "POST" + after.toString( ) ).info( );
  }

  private int getExpiryMinutes( final int defaultValue ) {
    try {
      return Integer.parseInt( System.getProperty(
          "com.eucalyptus.cluster.pendingTokenTimeout",
          String.valueOf( defaultValue ) ) );
    } catch ( final NumberFormatException e ) {
      return defaultValue;
    }
  }

  private void expirePendingTokens( final long expireBefore ) {
    final Date oldestDate = new Date( expireBefore );
    for ( final ResourceToken token : pendingTokens ) {
      if ( token.getCreationTime( ).before( oldestDate ) ) {
        LOG.error( "Expiring pending token: " + token );
        pendingTokens.remove( token );
      }
    }
  }

  private NavigableSet<VmTypeAvailability> sorted( ) {
    NavigableSet<VmTypeAvailability> available = new TreeSet<VmTypeAvailability>( );
    for ( String typeName : this.typeMap.keySet( ) )
      available.add( this.typeMap.get( typeName ) );
    available.add( VmTypeAvailability.ZERO );
    LOG.debug( "Resource information for " + this.clusterName );
    return available;
  }
  
  public VmTypeAvailability getAvailability( String vmTypeName ) {
    return this.typeMap.get( vmTypeName );
  }
  
  public static ResourceComparator getComparator( VmTypeInfo vmTypeInfo ) {
    return new ResourceComparator( vmTypeInfo );
  }
  
  public static class ResourceComparator implements Comparator<ResourceState> {
    
    private VmTypeInfo vmTypeInfo;
    
    ResourceComparator( final VmTypeInfo vmTypeInfo ) {
      this.vmTypeInfo = vmTypeInfo;
    }
    
    public int compare( final ResourceState o1, final ResourceState o2 ) {
      return o1.getAvailability( this.vmTypeInfo.getName( ) ).getAvailable( ) - o2.getAvailability( this.vmTypeInfo.getName( ) ).getAvailable( );
    }
  }
  
  @Override
  public String toString( ) {
    return String.format( "ClusterNodeState pending=%s redeemed=%s submitted=%s",
                          this.pendingTokens, this.redeemedTokens, this.submittedTokens );
  }
  
  public static class VmTypeAvailability implements Comparable {
    private VmType type;
    private int    max;
    private int    available;
    
    public VmTypeAvailability( final VmType type, final int max, final int available ) {
      this.type = type;
      this.max = max;
      this.available = available;
    }
    
    public VmType getType( ) {
      return type;
    }
    
    public void decrement( int quantity ) {
      this.available -= quantity;
      this.available = ( this.available < 0 )
                                             ? 0
                                             : this.available;
    }
    
    public int getMax( ) {
      return max;
    }
    
    public void setMax( final int max ) {
      this.max = max;
    }
    
    public int getAvailable( ) {
      return available;
    }
    
    public void setAvailable( final int available ) {
      this.available = available;
    }
    
    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( !( o instanceof VmTypeAvailability ) ) return false;
      
      VmTypeAvailability that = ( VmTypeAvailability ) o;
      
      if ( !type.equals( that.type ) ) return false;
      
      return true;
    }
    
    @Override
    public int hashCode( ) {
      return type.hashCode( );
    }
    
    public int compareTo( final Object o ) {
      VmTypeAvailability v = ( VmTypeAvailability ) o;
      if ( v.getAvailable( ) == this.getAvailable( ) ) return this.type.compareTo( v.getType( ) );
      return v.getAvailable( ) - this.getAvailable( );
    }
    
    @Override
    public String toString( ) {
      return "VmTypeAvailability " +
             " " + type +
             " " + available +
             " / " + max;
    }
    
    public static VmTypeAvailability ZERO = new ZeroTypeAvailability( );
    
    static class ZeroTypeAvailability extends VmTypeAvailability {
      ZeroTypeAvailability( ) {
        super( VmType.create( "ZERO", -1, -1, -1 ), 0, 0 );
      }
      
      @Override
      public int compareTo( final Object o ) {
        VmTypeAvailability v = ( VmTypeAvailability ) o;
        if ( v == ZERO ) return 0;
        if ( v.getAvailable( ) > 0 )
          return 1;
        else return -1;
      }
      
      @Override
      public void setAvailable( final int available ) {}
      
      @Override
      public void decrement( final int quantity ) {}
      
      @Override
      public boolean equals( final Object o ) {
        if ( this == o ) return true;
        return false;
      }
      
    }
    
  }

}
