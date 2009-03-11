package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.transport.client.Client;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.log4j.Logger;
import org.apache.axis2.AxisFault;

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
          VmInstances.getInstance().lookup( vmInfo.getInstanceId() ).getNetworkConfig().setIpAddress( vmInfo.getNetParams().getIpAddress() );
          VmInstances.getInstance().lookup( vmInfo.getInstanceId() ).getNetworkConfig().setIgnoredPublicIp( vmInfo.getNetParams().getIgnoredPublicIp() );
        }
      } else {
        this.parent.getRollback().lazySet( true );
      }
    } catch ( AxisFault axisFault ) { throw axisFault; }
  }

}
