package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.msgs.RebootInstancesType;

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.ws.client.Client;
import org.apache.log4j.Logger;

public class RebootCallback extends QueuedEventCallback<RebootInstancesType> {

  private static Logger LOG = Logger.getLogger( RebootCallback.class );

  public RebootCallback( final ClusterConfiguration clusterConfig ) {
    super( clusterConfig );
  }

  public void process( final Client cluster, final RebootInstancesType msg ) throws Exception {
    cluster.send( msg );
  }

}
