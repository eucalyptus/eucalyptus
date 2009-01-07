package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.entities.Address;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.transport.client.Client;
import org.apache.log4j.Logger;

import java.util.NoSuchElementException;

public class UnassignAddressCallback extends QueuedEventCallback<UnassignAddressType> {

  private static Logger LOG = Logger.getLogger( UnassignAddressCallback.class );

  private String pubIp;
  private String vmIp;
  private String vmId;

  public UnassignAddressCallback( final Address parent ) {
    this.vmId = parent.getInstanceId();
    this.pubIp = parent.getName();
    this.vmIp = parent.getInstanceAddress();
  }

  public void process( final Client clusterClient, final UnassignAddressType msg ) throws Exception {
    UnassignAddressResponseType reply = ( UnassignAddressResponseType ) clusterClient.send( msg );

    VmInstance vm = null;
    try {
      vm = VmInstances.getInstance().lookup( vmId );
      LOG.debug( "Unassign [" + pubIp + "] clearing VM " + vmId + ":" + vmIp );
      vm.getNetworkConfig().setIgnoredPublicIp( VmInstance.DEFAULT_IP );
    } catch ( NoSuchElementException e1 ) {}
  }

}
