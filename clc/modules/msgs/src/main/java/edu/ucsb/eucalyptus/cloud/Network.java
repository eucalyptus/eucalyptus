/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
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
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package edu.ucsb.eucalyptus.cloud;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.HasOwningAccount;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import edu.ucsb.eucalyptus.msgs.PacketFilterRule;
import edu.ucsb.eucalyptus.msgs.VmNetworkPeer;

public class Network implements HasFullName<Network>, HasOwningAccount {
  public String getNetworkName( ) {
    return this.networkName;
  }

  private static final Integer MIN_ADDR = 2;
  private static final Integer MAX_ADDR = 4096;
  private static Logger        LOG      = Logger.getLogger( Network.class );
  
  enum NetworkIndexState {
    ILLEGAL, FREE, USED, OUTLAW
  };
  
  private final String                              uuid;
  private final AtomicInteger                       vlan          = new AtomicInteger( 0 );
  private final String                              name;
  private final String                              networkName;
  private final AccountFullName                     account;
  private final List<PacketFilterRule>              rules         = new ArrayList<PacketFilterRule>( );
  private final ConcurrentMap<String, NetworkToken> clusterTokens = new ConcurrentHashMap<String, NetworkToken>( );
  
  private static class AddressRange {
    Integer min = 2;
    Integer max = 4096;
  }
  
  private static final AddressRange                                INITIAL_BOUNDS         = new AddressRange( );
  private final AtomicReference<AddressRange>                      addrsPerNet            = new AtomicReference<AddressRange>( INITIAL_BOUNDS );
  
  private final ConcurrentNavigableMap<Integer, NetworkIndexState> networkIndexes;
  private final SortedSetMultimap<String, NetworkToken>            volatileActiveNetworks = TreeMultimap.create( );
  private final SortedSetMultimap<String, NetworkToken>            activeNetworks         = Multimaps.synchronizedSortedSetMultimap( volatileActiveNetworks );
  private final FQDN                                               fullName;
  
  public Network( final AccountFullName owner, final String networkName, final String uuid ) {
    this.uuid = uuid;
    this.account = owner;
    this.fullName = new FQDN( owner );
    this.networkName = networkName;
    this.name = this.account.getAccountNumber( ) + "-" + this.networkName;
    this.networkIndexes = new ConcurrentSkipListMap<Integer, NetworkIndexState>( ) {
      {
        for ( int i = MIN_ADDR; i < MAX_ADDR; i++ ) {
          put( i, NetworkIndexState.FREE );
        }
      }
    };
  }
  public Network( final AccountFullName owner, final String networkName, final String uuid, List<PacketFilterRule> pfRules ) {
    this( owner, networkName, uuid );
    this.rules.addAll( pfRules );
  }
  
  public boolean initVlan( Integer i ) {
    return this.vlan.compareAndSet( Integer.valueOf( 0 ), i );
  }
  
  public Integer getVlan( ) {
    return this.vlan.get( );
  }
  
  public void extantNetworkIndex( String cluster, Integer index ) {
    NetworkIndexState ret = null;
    if ( this.networkIndexes.replace( index, NetworkIndexState.FREE, NetworkIndexState.USED )
         || ( ( ret = this.networkIndexes.putIfAbsent( index, NetworkIndexState.USED ) ) == null ) || ret == NetworkIndexState.FREE ) {
      EventRecord.caller( this.getClass( ), EventType.TOKEN_ALLOCATED, this.fullName.toString( ), "cluster", cluster, "networkIndex", index, "state",
                          this.networkIndexes.get( index ) ).debug( );
    } else if ( this.networkIndexes.putIfAbsent( index, NetworkIndexState.USED ) != NetworkIndexState.FREE ) {
      EventRecord.caller( this.getClass( ), EventType.TOKEN_ALLOCATED, this.fullName.toString( ), "cluster", cluster, "networkIndex", index, "state",
                          this.networkIndexes.get( index ) ).debug( );
    } else if ( this.networkIndexes.replace( index, NetworkIndexState.ILLEGAL, NetworkIndexState.OUTLAW ) ) {
      EventRecord.caller( this.getClass( ), EventType.TOKEN_ALLOCATED, "ERROR", this.fullName.toString( ), "cluster", cluster, "networkIndex", index, "state",
                          this.networkIndexes.get( index ), "Allocated when previous state was", NetworkIndexState.ILLEGAL ).debug( );
    } else {
      EventRecord.caller( this.getClass( ), EventType.TOKEN_ALLOCATED, "ERROR", this.fullName.toString( ), "cluster", cluster, "networkIndex", index, "state",
                          this.networkIndexes.get( index ), "No state information is availble for the index", NetworkIndexState.ILLEGAL ).debug( );
    }
  }
  
  public AccountFullName getAccount( ) {
    return this.account;
  }
  public String getAccountId( ) {
    return this.account.getAccountNumber( );
  }

  public ConcurrentNavigableMap<Integer, NetworkIndexState> getNetworkIndexes( ) {
    return this.networkIndexes;
  }

  private NetworkToken getClusterToken( String cluster ) {
    NetworkToken newToken = new NetworkToken( cluster, this.fullName.getAccountNumber( ), this.networkName, this.uuid, this.vlan.get( ) );
    NetworkToken token = this.clusterTokens.putIfAbsent( cluster, newToken );
    if ( token == null ) {
      return newToken;
    } else {
      return token;
    }
  }
  
  public NetworkToken createNetworkToken( String cluster ) {
    return getClusterToken( cluster );
  }
  
  public void trim( Integer max ) {
    AddressRange addrRange = new AddressRange( ) {
      {
        this.max = max;
      }
    };
    AddressRange oldRange = this.addrsPerNet.getAndSet( addrRange );
    if ( oldRange.max > addrRange.max ) {
      for ( int i = addrRange.max; i < oldRange.max; i++ ) {
        if ( this.networkIndexes.remove( i, NetworkIndexState.ILLEGAL ) || this.networkIndexes.remove( i, NetworkIndexState.FREE ) ) {
          EventRecord.caller( this.getClass( ), EventType.TOKEN_RETURNED, this.fullName.toString( ), "networkIndex", i, "state",
                              this.networkIndexes.get( i ) ).debug( );
        }
      }
    }
    this.networkIndexes.keySet( ).tailSet( this.addrsPerNet.get( ).max - 1, true ).clear( );
    this.networkIndexes.keySet( ).headSet( this.addrsPerNet.get( ).min ).clear( );
  }
  
  private final Predicate<Map.Entry<Integer, NetworkIndexState>> FIND_FREE_ADDR = new Predicate<Map.Entry<Integer, NetworkIndexState>>( ) {
                                                                                         @Override
                                                                                         public boolean apply( Entry<Integer, NetworkIndexState> t ) {
                                                                                           AddressRange addrRange = Network.this.addrsPerNet.get( );
                                                                                           return NetworkIndexState.FREE.equals( t.getValue( ) )
                                                                                                  && t.getKey( ) > addrRange.min
                                                                                                  && t.getKey( ) < addrRange.max
                                                                                                  && Network.this.networkIndexes.replace( t.getKey( ),
                                                                                                                                          NetworkIndexState.FREE,
                                                                                                                                          NetworkIndexState.USED );
                                                                                         }
                                                                                       };
  
  public Integer allocateNetworkIndex( String cluster ) {
    Integer nextIndex = null;
    try {
      nextIndex = Iterables.find( this.networkIndexes.entrySet( ), FIND_FREE_ADDR ).getKey( );
      EventRecord.caller( this.getClass( ), EventType.TOKEN_RESERVED, this.fullName.toString( ), "cluster", cluster, "networkIndex", nextIndex ).debug( );
    } catch ( NoSuchElementException ex ) {
      EventRecord.caller( this.getClass( ), EventType.TOKEN_RESERVED, "ERROR", this.fullName.toString( ), "cluster", cluster, "networkIndex", nextIndex, ex.getMessage( ) ).debug( );
    }
    this.getClusterToken( cluster ).allocateIndex( nextIndex );
    return nextIndex;
  }
  
  public void returnNetworkIndex( Integer index ) {
    if ( index < 2 ) return;
    for ( NetworkToken t : this.clusterTokens.values( ) ) {
      t.removeIndex( index );
    }
    if ( this.networkIndexes.replace( index, NetworkIndexState.USED, NetworkIndexState.FREE ) ) {
      EventRecord.caller( this.getClass( ), EventType.TOKEN_RETURNED, this.fullName.toString( ), "networkIndex", index ).debug( );
    } else if ( this.networkIndexes.remove( index, NetworkIndexState.OUTLAW ) || this.networkIndexes.remove( index, NetworkIndexState.ILLEGAL ) ) {
      EventRecord.caller( this.getClass( ), EventType.TOKEN_RETURNED, this.fullName.toString( ), "networkIndex", index ).debug( );
    }
  }
  
  public NetworkToken addTokenIfAbsent( NetworkToken token ) {
    if ( this.vlan.get( ) == 0 ) {
      this.vlan.compareAndSet( 0, token.getVlan( ) );
    }
    NetworkToken clusterToken = this.clusterTokens.putIfAbsent( token.getCluster( ), token );
    if ( clusterToken == null ) {
      clusterToken = this.clusterTokens.get( token.getCluster( ) );
    }
    return clusterToken;
  }
  
  public boolean hasToken( String cluster ) {
    return this.clusterTokens.containsKey( cluster );
  }
  
  public boolean hasTokens() {
    
    for( NetworkToken it : this.clusterTokens.values() ) {
      if(it.isEmpty()) {
        this.removeToken(it.getCluster());
      }
    }    
    return !this.clusterTokens.values( ).isEmpty( );
  }
  
  public void removeToken( String cluster ) {
    this.clusterTokens.remove( cluster );
  }
  
  public boolean isPeer( String peerName, String peerNetworkName ) {
    VmNetworkPeer peer = new VmNetworkPeer( peerName, peerNetworkName );
    for( PacketFilterRule pf : this.rules ) {
      if( pf.getPeers( ).contains( peer ) ) {
        return true;
      }
    }
    return false;
  }
  
  public String getName( ) {
    return this.name;
  }
  
  class FQDN extends AccountFullName implements FullName {
    FQDN( AccountFullName owner ) {
      super( owner, "security-group", Network.this.networkName );
    }
  }
  
  @Override
  public String getPartition( ) {
    return "";
  }

  @Override
  public FullName getFullName( ) {
    return this.fullName;
  }
  
  @Override
  public int compareTo( Network that ) {
    return this.vlan.get( ) - that.vlan.get( );
  }
  
  @Override
  public FullName getOwner( ) {
    return this.account;
  }
  public String getUuid( ) {
    return this.uuid;
  }
  public List<PacketFilterRule> getRules( ) {
    return this.rules;
  }
  @Override
  public String getOwnerAccountId( ) {
    return this.account.getAccountNumber( );
  }

}
