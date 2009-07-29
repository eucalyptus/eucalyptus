package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.msgs.*;
import com.eucalyptus.ws.client.Client;

import org.apache.log4j.Logger;

public class AssignAddressCallback extends QueuedEventCallback<AssignAddressType> {

  private static Logger LOG = Logger.getLogger( AssignAddressCallback.class );

  private VmInstance parent;

  public AssignAddressCallback( final VmInstance parent ) {
    this.parent = parent;
  }

  public void process( final Client clusterClient, final AssignAddressType msg ) throws Exception {
    AssignAddressResponseType reply = null;
    try {
      reply = ( AssignAddressResponseType ) clusterClient.send( msg );
      LOG.debug( "Assign [" + msg.getSource() + "]  => [" + msg.getDestination() + "]" );
      this.parent.getNetworkConfig().setIgnoredPublicIp( msg.getSource() );
    } catch ( Exception e ) {}
  }

}
