package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.entities.VmType;
import edu.ucsb.eucalyptus.msgs.*;

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.ws.client.Client;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.log4j.Logger;

public class ResourceUpdateCallback extends QueuedEventCallback<DescribeResourcesType> implements Runnable {

  private static Logger LOG = Logger.getLogger( ResourceUpdateCallback.class );

  private static int SLEEP_TIMER = 3 * 1000;
  private boolean firstTime = true;

  public ResourceUpdateCallback( ClusterConfiguration config ) {
    super( config );
  }

  public void process( final Client cluster, final DescribeResourcesType msg ) throws Exception {
    DescribeResourcesResponseType reply = ( DescribeResourcesResponseType ) cluster.send( msg );
//TODO:    parent.getNodeState().update( reply.getResources() );
    LOG.debug("Adding node service tags: " + reply.getServiceTags() );
//TODO:    parent.updateNodeInfo( reply.getServiceTags() );
//    if ( !parent.getNodeTags().isEmpty() && this.firstTime ) {
//      this.firstTime = false;
//      this.parent.fireNodeThreads();
//    }
  }

  public void run() {
    do {
      Cluster cluster = Clusters.getInstance( ).lookup( this.getConfig( ).getName( ) );
      DescribeResourcesType drMsg = new DescribeResourcesType();
      drMsg.setUserId( EucalyptusProperties.NAME );
      drMsg.setEffectiveUserId( EucalyptusProperties.NAME );
      for ( VmType v : VmTypes.list() ) drMsg.getInstanceTypes().add( v.getAsVmTypeInfo() );

      cluster.getMessageQueue().enqueue( new QueuedEvent( this, drMsg ) );
      this.waitForEvent();
    } while ( !this.isStopped() && this.sleep( SLEEP_TIMER ) );

  }

}
