package com.eucalyptus.cluster;

import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.log4j.Logger;

import edu.ucsb.eucalyptus.cloud.NetworkToken;
import edu.ucsb.eucalyptus.cloud.cluster.NetworkAlreadyExistsException;
import edu.ucsb.eucalyptus.cloud.cluster.NotEnoughResourcesAvailable;

public class ClusterState {
  private static Logger LOG = Logger.getLogger( ClusterState.class );
  private String clusterName;
  private NavigableSet<Integer> availableVlans;

  public ClusterState( String clusterName ) {
    this.clusterName = clusterName;
    this.availableVlans = new ConcurrentSkipListSet<Integer>();
    for ( int i = 10; i < 4096; i++ ) this.availableVlans.add( i );
  }


  public NetworkToken extantAllocation( String userName, String networkName, int vlan ) throws NetworkAlreadyExistsException {
    NetworkToken netToken = new NetworkToken( this.clusterName, userName, networkName, vlan );
    if ( !this.availableVlans.remove( vlan ) ) {
      throw new NetworkAlreadyExistsException();
    }
    return netToken;
  }

  public NetworkToken getNetworkAllocation( String userName, String networkName ) throws NotEnoughResourcesAvailable {
    if ( this.availableVlans.isEmpty() ) throw new NotEnoughResourcesAvailable();
    int vlan = this.availableVlans.first();
    this.availableVlans.remove( vlan );
    NetworkToken token = new NetworkToken( this.clusterName, userName, networkName, vlan );
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

  

}
