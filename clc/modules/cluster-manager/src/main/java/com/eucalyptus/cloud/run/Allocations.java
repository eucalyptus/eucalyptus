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
 *    THE REGENTS DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.cloud.run;

import java.util.List;
import java.util.NoSuchElementException;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.auth.principal.FakePrincipals;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cluster.ClusterNodeState;
import com.eucalyptus.cluster.ClusterState;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.Networks;
import com.eucalyptus.component.Partition;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.images.Emis.BootableSet;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.Network;
import edu.ucsb.eucalyptus.cloud.NetworkToken;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.VmKeyInfo;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class Allocations {
  public static class Allocation {
    private final RunInstancesType    request;
    private final UserFullName        ownerFullName;
    private final List<Network>       networks         = Lists.newArrayList( );
    private final List<ResourceToken> allocationTokens = Lists.newArrayList( );
    private final List<String>        addresses        = Lists.newArrayList( );
    private final List<Integer>       networkIndexList = Lists.newArrayList( );
    private byte[]                    userData;
    private Partition                 partition;
    private Long                      reservationIndex;
    private String                    reservationId;
    private VmKeyInfo                 keyInfo;
    private VmTypeInfo                vmTypeInfo;
    private BootableSet               bootSet;
    
    private Allocation( RunInstancesType request ) {
      super( );
      this.request = request;
      UserFullName temp = FakePrincipals.NOBODY_USER_ERN; 
      try {
        temp = Contexts.lookup( request.getCorrelationId( ) ).getUserFullName( );
      } catch ( NoSuchContextException ex ) {}
      this.ownerFullName = temp;
    }
    
    public RunInstancesType getRequest( ) {
      return this.request;
    }
    
    public Network getPrimaryNetwork( ) {
      if ( this.networks.size( ) < 1 ) {
        throw new IllegalArgumentException( "At least one network group must be specified." );
      } else {
        Network firstNet = this.networks.get( 0 );
        try {
          firstNet = Networks.getInstance( ).lookup( firstNet.getName( ) );
        } catch ( NoSuchElementException e ) {
          Networks.getInstance( ).registerIfAbsent( firstNet, Networks.State.ACTIVE );
          firstNet = Networks.getInstance( ).lookup( firstNet.getName( ) );
        }
        return firstNet;
      }
    }
    
    public void releaseNetworkAllocationTokens( ) {
      for ( ResourceToken token : this.allocationTokens ) {
        if ( token.getPrimaryNetwork( ) != null ) {
          for ( NetworkToken networkToken : token.getNetworkTokens( ) ) {
            Clusters.getInstance( ).lookup( token.getCluster( ) ).getState( ).releaseNetworkAllocation( networkToken );
          }
        }
      }
    }
    
    public void releaseNetworkIndexes( ) {
      for ( ResourceToken token : this.allocationTokens ) {
        if ( token.getPrimaryNetwork( ) != null ) {
          for ( Integer net : token.getPrimaryNetwork( ).getIndexes( ) ) {
            this.getPrimaryNetwork( ).returnNetworkIndex( net );
          }
          token.getPrimaryNetwork( ).getIndexes( ).clear( );
        }
      }
    }
    
    public void releaseAllocationTokens( ) {
      for ( ResourceToken token : this.allocationTokens ) {
        Clusters.getInstance( ).lookup( token.getCluster( ) ).getNodeState( ).releaseToken( token );
      }
    }
    
    public List<Network> getNetworks( ) {
      return this.networks;
    }
    
    public ResourceToken requestResourceToken( ClusterNodeState state, String vmTypeName, int tryAmount, int maxAmount ) throws NotEnoughResourcesAvailable {
      ResourceToken rscToken = state.getResourceAllocation( this.request.getCorrelationId( ), this.ownerFullName, vmTypeName, tryAmount, maxAmount );
      this.allocationTokens.add( rscToken );
      return rscToken;
    }
    
    public void requestNetworkTokens( ) throws NotEnoughResourcesAvailable {
      Network net = this.getPrimaryNetwork( );
      for ( ResourceToken rscToken : this.allocationTokens ) {
        ClusterState clusterState = Clusters.getInstance( ).lookup( rscToken.getCluster( ) ).getState( );
        NetworkToken networkToken = clusterState.getNetworkAllocation( this.ownerFullName, rscToken, net.getName( ) );
        rscToken.getNetworkTokens( ).add( networkToken );//TODO:GRZE:FIXME
      }
    }
    
    public void requestNetworkIndexes( ) throws NotEnoughResourcesAvailable {
      Network net = this.getPrimaryNetwork( );
      for ( ResourceToken rscToken : this.allocationTokens ) {
        for( int i = 0; i < rscToken.getAmount( ); i ++ ) {
          Integer addrIndex = net.allocateNetworkIndex( rscToken.getCluster( ) );
          if ( addrIndex == null ) {
            throw new NotEnoughResourcesAvailable( "Not enough addresses left in the network subnet assigned to requested group: " + net.getNetworkName( ) );
          }
          rscToken.getPrimaryNetwork( ).getIndexes( ).add( addrIndex );
        }
        ClusterState clusterState = Clusters.getInstance( ).lookup( rscToken.getCluster( ) ).getState( );
        NetworkToken networkToken = clusterState.getNetworkAllocation( this.ownerFullName, rscToken, net.getName( ) );
        rscToken.getNetworkTokens( ).add( networkToken );//TODO:GRZE:FIXME
      }
    }
    
    public void requestAddressTokens( ) throws NotEnoughResourcesAvailable {
      for ( ResourceToken toke : this.allocationTokens ) {
        for ( Address addr : Addresses.allocateSystemAddresses( toke.getCluster( ), toke.getAmount( ) ) ) {
          toke.getAddresses( ).add( addr.getDisplayName( ) );
        }
      }
    }
    
    public void releaseAddressTokens( ) {
      for ( ResourceToken toke : this.allocationTokens ) {
        for ( String addr : toke.getAddresses( ) ) {
          Addresses.release( Addresses.getInstance( ).lookup( addr ) );
        }
      }
    }

    public VmTypeInfo getVmTypeInfo( ) {
      return this.vmTypeInfo;
    }

    public Partition getPartition( ) {
      return this.partition;
    }

    public void setBootableSet( BootableSet bootSet ) {
      this.bootSet = bootSet;
    }
  }
  
  public static Allocation begin( RunInstancesType request ) {
    return new Allocation( request );
  }
}
