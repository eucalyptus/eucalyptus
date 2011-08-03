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
import java.util.Map;
import java.util.NoSuchElementException;
import org.bouncycastle.util.encoders.Base64;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.auth.principal.FakePrincipals;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.blockstorage.Volume;
import com.eucalyptus.cloud.ResourceToken;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.cluster.ClusterNodeState;
import com.eucalyptus.cluster.ClusterState;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.component.Partition;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.images.Emis.BootableSet;
import com.eucalyptus.keys.SshKeyPair;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.network.NetworkToken;
import com.eucalyptus.network.Networks;
import com.eucalyptus.util.Counters;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.eucalyptus.vm.VmType;
import com.eucalyptus.vm.VmTypes;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.VmKeyInfo;
import edu.ucsb.eucalyptus.msgs.HasRequest;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class Allocations {
  public static class Allocation implements HasRequest {
    private final Context                  context;
    private final RunInstancesType         request;
    private final UserFullName             ownerFullName;
    private final List<ResourceToken>      allocationTokens  = Lists.newArrayList( );
    private final List<String>             addresses         = Lists.newArrayList( );
    private final List<Volume>             persistentVolumes = Lists.newArrayList( );
    private final List<Volume>             transientVolumes  = Lists.newArrayList( );
    private final List<Integer>            networkIndexList  = Lists.newArrayList( );
    private final String                   reservationId;
    private byte[]                         userData;
    private Partition                      partition;
    private Long                           reservationIndex;
    private SshKeyPair                     sshKeyPair;
    private BootableSet                    bootSet;
    private VmType                         vmType;
    private Map<String, NetworkGroup> networkRulesGroups;
    private final int minCount;
    private final int maxCount;
    
    private Allocation( RunInstancesType request ) {
      super( );
      this.context = Contexts.lookup( );
      this.request = request;
      this.minCount = request.getMinCount( );
      this.maxCount = request.getMaxCount( );
      UserFullName temp = FakePrincipals.nobodyFullName();
      try {
        temp = Contexts.lookup( request.getCorrelationId( ) ).getUserFullName( );
      } catch ( NoSuchContextException ex ) {}
      this.ownerFullName = temp;
      if ( this.request.getInstanceType( ) == null || "".equals( this.request.getInstanceType( ) ) ) {
        this.request.setInstanceType( VmTypes.defaultTypeName( ) );
      }
      this.reservationIndex = Counters.getIdBlock( request.getMaxCount( ) );
      this.reservationId = VmInstances.getId( this.reservationIndex, 0 ).replaceAll( "i-", "r-" );
      byte[] userData = new byte[0];
      if ( this.request.getUserData( ) != null ) {
        try {
          this.userData = Base64.decode( this.request.getUserData( ) );
        } catch ( Exception e ) {}
      }
      this.request.setUserData( new String( Base64.encode( userData ) ) );
    }
    
    public RunInstancesType getRequest( ) {
      return this.request;
    }
    
    public NetworkGroup getPrimaryNetwork( ) {
      if ( this.networkRulesGroups.size( ) < 1 ) {
        throw new IllegalArgumentException( "At least one network group must be specified." );
      } else {
        NetworkGroup firstRules = this.networkRulesGroups.values( ).iterator( ).next( );
        return firstRules;
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
    
    public List<NetworkGroup> getNetworkRulesGroups( ) {
      return Lists.newArrayList( this.networkRulesGroups.values( ) );
    }

    public ResourceToken requestResourceToken( ClusterNodeState state, String vmTypeName, int tryAmount, int maxAmount ) throws NotEnoughResourcesAvailable {
      ResourceToken rscToken = state.getResourceAllocation( this.request.getCorrelationId( ), this.ownerFullName, vmTypeName, tryAmount, maxAmount );
      this.allocationTokens.add( rscToken );
      return rscToken;
    }
    
    public void requestNetworkTokens( ) throws NotEnoughResourcesAvailable {
      NetworkGroup net = this.getPrimaryNetwork( );
      for ( ResourceToken rscToken : this.allocationTokens ) {
        ClusterState clusterState = Clusters.getInstance( ).lookup( rscToken.getCluster( ) ).getState( );
        NetworkToken networkToken = clusterState.getNetworkAllocation( this.ownerFullName, rscToken, net.getName( ) );
        rscToken.getNetworkTokens( ).add( networkToken );//TODO:GRZE:FIXME
      }
    }
    
    public void requestNetworkIndexes( ) throws NotEnoughResourcesAvailable {
      NetworkGroup net = this.getPrimaryNetwork( );
      for ( ResourceToken rscToken : this.allocationTokens ) {
        for ( int i = 0; i < rscToken.getAmount( ); i++ ) {
          Integer addrIndex = net.allocateNetworkIndex( rscToken.getCluster( ) );
          if ( addrIndex == null ) {
            throw new NotEnoughResourcesAvailable( "Not enough addresses left in the network subnet assigned to requested group: " + net );
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
    
    public VmType getVmType( ) {
      return this.vmType;
    }
    
    public Partition getPartition( ) {
      return this.partition;
    }
    
    public void setBootableSet( BootableSet bootSet ) {
      this.bootSet = bootSet;
    }
    
    public void setVmType( VmType vmType ) {
      this.vmType = vmType;
    }
    
    public UserFullName getOwnerFullName( ) {
      return this.ownerFullName;
    }
    
    public List<ResourceToken> getAllocationTokens( ) {
      return this.allocationTokens;
    }
    
    public List<String> getAddresses( ) {
      return this.addresses;
    }
    
    public List<Integer> getNetworkIndexList( ) {
      return this.networkIndexList;
    }
    
    public byte[] getUserData( ) {
      return this.userData;
    }
    
    public Long getReservationIndex( ) {
      return this.reservationIndex;
    }
    
    public String getReservationId( ) {
      return this.reservationId;
    }
    
    public BootableSet getBootSet( ) {
      return this.bootSet;
    }
    
    public Context getContext( ) {
      return this.context;
    }
    
    public void setPartition( Partition partition2 ) {
      this.partition = partition2;
    }
    
    public List<Volume> getPersistentVolumes( ) {
      return this.persistentVolumes;
    }
    
    public List<Volume> getTransientVolumes( ) {
      return this.transientVolumes;
    }
    
    public SshKeyPair getSshKeyPair( ) {
      return this.sshKeyPair;
    }
    
    public void setSshKeyPair( SshKeyPair sshKeyPair ) {
      this.sshKeyPair = sshKeyPair;
    }
    
    public void setNetworkRules( Map<String, NetworkGroup> networkRuleGroups ) {
      this.networkRulesGroups = networkRuleGroups;
    }
    
    public VmTypeInfo getVmTypeInfo( ) throws MetadataException {
      return this.bootSet.populateVirtualBootRecord( vmType );
    }

    public int getMinCount( ) {
      return this.minCount;
    }

    public int getMaxCount( ) {
      return this.maxCount;
    }
  }
  
  public static Allocation begin( RunInstancesType request ) {
    return new Allocation( request );
  }
}
