package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.transport.client.Client;
import org.apache.log4j.Logger;
import org.apache.axis2.AxisFault;

import java.util.NoSuchElementException;

public class StopNetworkCallback extends QueuedEventCallback<StopNetworkType> {

  private static Logger LOG = Logger.getLogger( StopNetworkCallback.class );

  private NetworkToken token;

  public StopNetworkCallback( final NetworkToken networkToken ) {
    this.token = networkToken;
  }

  public void process( final Client clusterClient, final StopNetworkType msg ) throws Exception {
    LOG.debug( "Sending stopNetwork for " + token.getName() + " on cluster " + token.getCluster() );
    try {
      for ( VmInstance v : VmInstances.getInstance().listValues() ) {
        if ( v.getNetworkNames().contains( token.getName() ) && v.getPlacement().equals( token.getCluster() ) ) return;
      }

      StopNetworkResponseType reply = ( StopNetworkResponseType ) clusterClient.send( msg );
      try {
        Network net = Networks.getInstance().lookup( token.getName() );
        Cluster cluster = Clusters.getInstance().lookup( token.getCluster() );
        LOG.debug( "Releasing network token back to cluster: " + token );
        cluster.getState().releaseNetworkAllocation( token );
        LOG.debug( "Removing network token: " + token );
        net.removeToken( token.getCluster() );
      } catch ( NoSuchElementException e1 ) {
        LOG.error( e1 );
      }
    } catch ( AxisFault axisFault ) {
      LOG.error( axisFault );
      LOG.debug( axisFault, axisFault );
    }
  }

}
