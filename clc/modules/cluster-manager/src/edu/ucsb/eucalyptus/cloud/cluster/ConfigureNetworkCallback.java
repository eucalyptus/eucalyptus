package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.transport.client.Client;
import org.apache.log4j.Logger;

class ConfigureNetworkCallback extends QueuedEventCallback<ConfigureNetworkType> {

  private static Logger LOG = Logger.getLogger( ConfigureNetworkCallback.class );

  public ConfigureNetworkCallback() {}

  public void process( final Client clusterClient, final ConfigureNetworkType msg ) throws Exception {
    clusterClient.send( msg );
  }

}
