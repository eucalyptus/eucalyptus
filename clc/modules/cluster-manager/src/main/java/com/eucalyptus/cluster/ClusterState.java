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

import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.log4j.Logger;

import edu.ucsb.eucalyptus.cloud.NetworkToken;
import edu.ucsb.eucalyptus.cloud.Network;
import edu.ucsb.eucalyptus.cloud.cluster.NetworkAlreadyExistsException;
import edu.ucsb.eucalyptus.cloud.cluster.NotEnoughResourcesAvailable;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;

public class ClusterState {
  private static Logger LOG = Logger.getLogger( ClusterState.class );
  private String clusterName;
  private static NavigableSet<Integer> availableVlans;
  private Integer mode = 1;
  private Integer addressCapacity;
  
  public ClusterState( String clusterName ) {
    this.clusterName = clusterName;
    this.availableVlans = new ConcurrentSkipListSet<Integer>();
    for ( int i = 10; i < 4096; i++ ) this.availableVlans.add( i );
  }


  public NetworkToken extantAllocation( String userName, String networkName, int vlan ) throws NetworkAlreadyExistsException {
    NetworkToken netToken = new NetworkToken( this.clusterName, userName, networkName, vlan );
    if ( !ClusterState.availableVlans.remove( vlan ) ) {
      throw new NetworkAlreadyExistsException();
    }
    return netToken;
  }

  public NetworkToken getNetworkAllocation( String userName, String networkName ) throws NotEnoughResourcesAvailable {
    return ClusterState.getNetworkAllocation( userName, clusterName, networkName );
  }
  
  private static NetworkToken getNetworkAllocation( String userName, String clusterName, String networkName ) throws NotEnoughResourcesAvailable {
    Network network = null;
    try {
      network = Networks.getInstance( ).lookup( networkName );
      Integer vlan = network.getVlan( );
      if( vlan == null ) {
        vlan = ClusterState.availableVlans.pollFirst();
        if( vlan == null ) throw new NotEnoughResourcesAvailable( "Not enough resources available: vlan tags" );
        network.setVlan( vlan );
      }
      NetworkToken token = new NetworkToken( clusterName, userName, network.getNetworkName( ), network.getVlan( ) );
      LOG.debug( String.format( EucalyptusProperties.DEBUG_FSTRING, EucalyptusProperties.TokenState.preallocate, token ) );
      network.addTokenIfAbsent( token );
      return token;
    } catch ( NoSuchElementException e ) {
      LOG.debug( e, e );
      throw new NotEnoughResourcesAvailable( "Failed to create registry entry for network named: " + networkName );
    }
  }

  public void releaseNetworkAllocation( String networkName ) {
    Network existingNet = Networks.getInstance( ).lookup( networkName );
    if( !existingNet.hasTokens() ) {
      ClusterState.availableVlans.add( existingNet.getVlan( ) );
    }
  }
  public void releaseNetworkAllocation( NetworkToken token ) {
    LOG.debug( String.format( EucalyptusProperties.DEBUG_FSTRING, EucalyptusProperties.TokenState.returned, token ) );
    try {
      Network existingNet = Networks.getInstance( ).lookup( token.getName( ) );
      this.releaseNetworkAllocation( token.getName( ) );
    } catch ( NoSuchElementException e ) {
    }
  }


  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    ClusterState cluster = ( ClusterState ) o;

    if ( !this.getClusterName( ).equals( cluster.getClusterName( ) ) ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return this.getClusterName( ).hashCode();
  }


  public String getClusterName( ) {
    return clusterName;
  }


  public Integer getMode( ) {
    return mode;
  }


  public void setMode( Integer mode ) {
    this.mode = mode;
  }


  public Integer getAddressCapacity( ) {
    return addressCapacity;
  }


  public void setAddressCapacity( Integer addressCapacity ) {
    this.addressCapacity = addressCapacity;
  }

  

}
