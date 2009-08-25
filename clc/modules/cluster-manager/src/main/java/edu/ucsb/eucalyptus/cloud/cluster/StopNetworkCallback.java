package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.msgs.*;

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.ws.client.Client;
import org.apache.log4j.Logger;


import java.util.NoSuchElementException;

public class StopNetworkCallback extends QueuedEventCallback<StopNetworkType> {

  private static Logger LOG = Logger.getLogger( StopNetworkCallback.class );

  private NetworkToken token;

  public StopNetworkCallback( final ClusterConfiguration clusterConfig, final NetworkToken networkToken ) {
    super(clusterConfig);
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
    } catch ( Exception e ) {
      LOG.error( e );
      LOG.debug( e, e );
    }
  }

}
