/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.cloud;

import edu.ucsb.eucalyptus.cloud.cluster.*;
import edu.ucsb.eucalyptus.cloud.entities.Address;
import edu.ucsb.eucalyptus.util.*;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import groovy.lang.*;
import org.apache.log4j.Logger;

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.ClusterNodeState;
import com.eucalyptus.cluster.ClusterState;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.Networks;
import com.eucalyptus.net.util.AddressUtil;
import com.eucalyptus.util.BaseDirectory;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.util.FailScriptFailException;
import com.eucalyptus.util.GroovyUtil;
import com.eucalyptus.util.NotEnoughResourcesAvailable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import javax.script.ScriptEngineManager;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

public class SLAs {
  
  private static Logger LOG                  = Logger.getLogger( SLAs.class );
  
  static String         RULES_DIR_NAME       = BaseDirectory.CONF.toString( ) + File.separator + "rules";
  static String         ALLOC_RULES_DIR_NAME = RULES_DIR_NAME + File.separator + "allocation";
  static String         TIMER_RULES_DIR_NAME = RULES_DIR_NAME + File.separator + "timer";
  static String         STATE_RULES_DIR_NAME = RULES_DIR_NAME + File.separator + "state";
  
  ScriptEngineManager   mgr                  = new ScriptEngineManager( );
  
  public List<ResourceToken> doVmAllocation( VmAllocationInfo vmAllocInfo ) throws FailScriptFailException, NotEnoughResourcesAvailable {
    RunInstancesType request = vmAllocInfo.getRequest( );
    String clusterName = request.getAvailabilityZone( );
    if ( clusterName != null && !"default".equals( clusterName ) ) {
      try {
        Cluster cluster = Clusters.getInstance( ).lookup( clusterName );
        ClusterNodeState clusterState = cluster.getNodeState( );
        int available = clusterState.getAvailability( request.getInstanceType( ) ).getAvailable( );
        if ( available < request.getMinCount( ) ) {
          throw new NotEnoughResourcesAvailable( "Not enough resources: vm resources in the requested cluster " + clusterName );
        }
        int count = available > request.getMaxCount( ) ? request.getMaxCount( ) : available;
        ResourceToken token = clusterState.getResourceAllocation( request.getCorrelationId( ), request.getUserId( ), request.getInstanceType( ), count );
        return Lists.newArrayList( token );
      } catch ( NoSuchElementException e ) {
        throw new NotEnoughResourcesAvailable( "Not enough resources: request cluster does not exist " + clusterName );
      }
    } else {
      SortedSet<ClusterNodeState> clusterStateList = new ConcurrentSkipListSet<ClusterNodeState>( ClusterNodeState.getComparator( vmAllocInfo.getVmTypeInfo( ) ) );
      for ( Cluster c : Clusters.getInstance( ).getEntries( ) )
        clusterStateList.add( c.getNodeState( ) );
      Allocator blah = this.getAllocator( );
      return Lists.newArrayList( blah.allocate( request.getCorrelationId( ), request.getUserId( ), vmAllocInfo.getVmTypeInfo( ).getName( ), request.getMinCount( ),
                                                request.getMaxCount( ), clusterStateList ) );
    }
  }
  
  public void doAddressAllocation( VmAllocationInfo vmAllocInfo ) throws NotEnoughResourcesAvailable {
    if( EucalyptusProperties.disableNetworking ) return;
    if ( "public".equals( vmAllocInfo.getRequest().getAddressingType() ) || vmAllocInfo.getRequest().getAddressingType() == null ) {
      List<ResourceToken> allocTokeList = vmAllocInfo.getAllocationTokens();
      List<Address> addressList = Lists.newArrayList();
      try {
        for ( ResourceToken token : allocTokeList ) {
          addressList.addAll( AddressUtil.tryAssignSystemAddresses( token ) );
        }
      } catch ( Exception e ) {
        throw new NotEnoughResourcesAvailable( e.getMessage( ), e );
      }
      Iterator<Address> iter = addressList.listIterator( );
      for ( ResourceToken token : allocTokeList ) {
        for ( int i = 0; i < token.getAmount(); i++ ) {
          Address next = iter.next( );
          token.getAddresses().add( next.getName( ) );
        }
      }
    }
  }

  public void doNetworkAllocation( VmAllocationInfo vmAllocInfo ) throws NotEnoughResourcesAvailable {
    String userId = vmAllocInfo.getRequest().getUserId();
    List<ResourceToken> rscTokens = vmAllocInfo.getAllocationTokens(); 
    List<Network> networks = vmAllocInfo.getNetworks();
    ResourceToken firstRscToken = rscTokens.get( 0 );
    if( networks.size() < 1 ) {
      throw new NotEnoughResourcesAvailable( "At least one network group must be specified." );
    }
    Network firstNet = networks.get( 0 );
    String networkName = firstNet.getName( );
    try {
      firstNet = Networks.getInstance( ).lookup( networkName );
    } catch ( NoSuchElementException e ) {
      Networks.getInstance( ).registerIfAbsent( firstNet, Networks.State.ACTIVE ); 
      firstNet = Networks.getInstance( ).lookup( networkName );      
    }
    for ( ResourceToken token : rscTokens ) {
      NetworkToken netToken = allocateClusterVlan( userId, token.getCluster( ), firstNet.getName( ) );
      token.getNetworkTokens( ).add( netToken );
      if( !EucalyptusProperties.disableNetworking ) {
        for ( int i = 0; i < token.getAmount( ); i++ ) {
          Integer addrIndex = firstNet.allocateNetworkIndex( token.getCluster( ) );
          if ( addrIndex == null ) {
            for( Integer index : token.getPrimaryNetwork( ).getIndexes( ) ) {
              firstNet.returnNetworkIndex( index );
            }
            token.getPrimaryNetwork( ).getIndexes( ).clear( );
            throw new NotEnoughResourcesAvailable( "Not enough addresses left in the network subnet assigned to requested group: " + firstNet.getNetworkName( ) );
          } else {
            token.getPrimaryNetwork( ).getIndexes().add( addrIndex );
          }
        }
      }
    }
  }
  
  private NetworkToken allocateClusterVlan( final String userId, final String clusterName, final String networkName ) throws NotEnoughResourcesAvailable {
    ClusterState clusterState = Clusters.getInstance( ).lookup( clusterName ).getState( );
    NetworkToken networkToken = clusterState.getNetworkAllocation( userId, networkName );
    LOG.info( String.format( EucalyptusProperties.DEBUG_FSTRING, EucalyptusProperties.TokenState.accepted, networkToken ) );
    return networkToken;
  }
  
  private Allocator getAllocator( ) throws FailScriptFailException {
    Object blah = null;
    try {
      blah = GroovyUtil.newInstance( ALLOC_RULES_DIR_NAME + File.separator + "default.groovy" );
    } catch ( FailScriptFailException e ) {
      LOG.error( e, e );
    }
    if ( !( blah instanceof Allocator ) ) throw new FailScriptFailException( blah.getClass( ) + " does not implement " + Allocator.class );
    return ( Allocator ) blah;
  }
  
}
