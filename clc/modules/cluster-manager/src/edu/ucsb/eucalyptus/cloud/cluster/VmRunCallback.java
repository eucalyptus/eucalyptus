package edu.ucsb.eucalyptus.cloud.cluster;

import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.VmRunResponseType;
import edu.ucsb.eucalyptus.cloud.VmRunType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesType;
import edu.ucsb.eucalyptus.transport.client.Client;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.axis2.AxisFault;
import org.apache.log4j.Logger;

class VmRunCallback extends QueuedEventCallback<VmRunType> {

  private static Logger LOG = Logger.getLogger( VmRunCallback.class );

  private ClusterAllocator parent;
  private ResourceToken token;

  public VmRunCallback( final ClusterAllocator parent, final ResourceToken token ) {
    this.parent = parent;
    this.token = token;
  }

  public void process( final Client clusterClient, final VmRunType msg ) throws Exception {
    LOG.info( String.format( EucalyptusProperties.DEBUG_FSTRING, EucalyptusProperties.TokenState.submitted, token ) );
    Clusters.getInstance().lookup( token.getCluster() ).getNodeState().submitToken( token );
    for ( String vmId : msg.getInstanceIds() )
      parent.msgMap.put( ClusterAllocator.State.ROLLBACK,
                         new QueuedEvent<TerminateInstancesType>(
                             new TerminateCallback( ),
                             new TerminateInstancesType( vmId, msg ) ) );
    VmRunResponseType reply = null;
    try {
      reply = ( VmRunResponseType ) clusterClient.send( msg );
      Clusters.getInstance().lookup( token.getCluster() ).getNodeState().redeemToken( token );
      LOG.info( String.format( EucalyptusProperties.DEBUG_FSTRING, EucalyptusProperties.TokenState.redeemed, token ) );
      if ( reply.get_return() ) {
        for ( VmInfo vmInfo : reply.getVms() ) {
          VmInstance vm = VmInstances.getInstance().lookup( vmInfo.getInstanceId() );
          vm.getNetworkConfig().setIpAddress( vmInfo.getNetParams().getIpAddress() );
          vm.getNetworkConfig().setIgnoredPublicIp( vmInfo.getNetParams().getIgnoredPublicIp() );
        }
        this.parent.setupAddressMessages( Lists.newArrayList( this.token.getAddresses() ), Lists.newArrayList( reply.getVms() ) );
      } else {
        this.parent.getRollback().lazySet( true );
      }
    } catch ( AxisFault axisFault ) { throw axisFault; }
  }

}
