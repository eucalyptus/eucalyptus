package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.ws.SystemState;
import edu.ucsb.eucalyptus.transport.client.Client;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.log4j.Logger;

public class VmUpdateCallback extends QueuedEventCallback<VmDescribeType> implements Runnable {

  private static Logger LOG = Logger.getLogger( VmUpdateCallback.class );

  private Cluster parent;
  private static int SLEEP_TIMER = 5 * 1000;

  public VmUpdateCallback( final Cluster parent ) {
    this.parent = parent;
  }

  public void process( final Client cluster, final VmDescribeType msg ) throws Exception {
    VmDescribeResponseType reply = ( VmDescribeResponseType ) cluster.send( msg );
    if ( reply != null ) reply.setOriginCluster( this.parent.getName() );
    for ( VmInfo vmInfo : reply.getVms() )
      vmInfo.setPlacement( this.parent.getName() );
    SystemState.handle( reply );
  }

  public void run() {
    do {
      VmDescribeType msg = new VmDescribeType();
      msg.setUserId( EucalyptusProperties.NAME );
      msg.setEffectiveUserId( EucalyptusProperties.NAME );

      this.parent.getMessageQueue().enqueue( new QueuedEvent( this, msg ) );
      this.waitForEvent();
    } while ( !this.isStopped() && this.sleep( SLEEP_TIMER ) );
  }
}
