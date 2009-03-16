package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.entities.Address;
import edu.ucsb.eucalyptus.cloud.net.Addresses;
import edu.ucsb.eucalyptus.cloud.ws.AddressManager;
import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.msgs.ReleaseAddressType;
import edu.ucsb.eucalyptus.msgs.UnassignAddressResponseType;
import edu.ucsb.eucalyptus.msgs.UnassignAddressType;
import edu.ucsb.eucalyptus.transport.client.Client;
import edu.ucsb.eucalyptus.util.Admin;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
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
    String addr = msg.getSource();
    try {
      Address a = Addresses.getInstance().lookup( addr );
      if( EucalyptusProperties.NAME.equals( a.getUserId() ) ) {
        new AddressManager().ReleaseAddress( Admin.makeMsg( ReleaseAddressType.class, addr ) );
      }
    } catch ( NoSuchElementException e1 ) {
      LOG.error( e1 );
    } catch ( EucalyptusCloudException e1 ) {
      LOG.error( e1 );
    }
  }

}
