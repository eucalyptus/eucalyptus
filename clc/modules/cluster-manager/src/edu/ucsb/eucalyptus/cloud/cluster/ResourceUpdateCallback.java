package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.entities.VmType;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.transport.client.Client;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.log4j.Logger;

public class ResourceUpdateCallback extends QueuedEventCallback<DescribeResourcesType> implements Runnable {

  private static Logger LOG = Logger.getLogger( ResourceUpdateCallback.class );

  private Cluster parent;
  private static int SLEEP_TIMER = 3 * 1000;
  private boolean firstTime = true;

  public ResourceUpdateCallback( final Cluster parent ) {
    this.parent = parent;
  }

  public void process( final Client cluster, final DescribeResourcesType msg ) throws Exception {
    DescribeResourcesResponseType reply = ( DescribeResourcesResponseType ) cluster.send( msg );
    parent.getState().update( reply.getResources() );
    LOG.debug("Adding node service tags: " + reply.getServiceTags() );
    parent.updateNodeInfo( reply.getServiceTags() );
//    if ( !parent.getNodeTags().isEmpty() && this.firstTime ) {
//      this.firstTime = false;
//      this.parent.fireNodeThreads();
//    }
  }

  public void run() {
    do {
      DescribeResourcesType drMsg = new DescribeResourcesType();
      drMsg.setUserId( EucalyptusProperties.NAME );
      drMsg.setEffectiveUserId( EucalyptusProperties.NAME );
      for ( VmType v : VmTypes.list() ) drMsg.getInstanceTypes().add( v.getAsVmTypeInfo() );

      this.parent.getMessageQueue().enqueue( new QueuedEvent( this, drMsg ) );
      this.waitForEvent();
    } while ( !this.isStopped() && this.sleep( SLEEP_TIMER ) );

  }

}
