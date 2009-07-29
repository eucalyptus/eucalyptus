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

import edu.ucsb.eucalyptus.cloud.NetworkToken;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;
import org.apache.log4j.Logger;

import java.util.Comparator;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

public class ClusterState {

  private static Logger LOG = Logger.getLogger( ClusterState.class );

  private Cluster parent;
  private NavigableSet<Integer> availableVlans;

  public ClusterState( Cluster parent ) {
    this.parent = parent;

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


}

