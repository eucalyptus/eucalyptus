package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.VmDescribeResponseType;
import edu.ucsb.eucalyptus.cloud.VmDescribeType;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.entities.VmType;
import edu.ucsb.eucalyptus.cloud.ws.SystemState;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.ws.client.Client;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.log4j.Logger;

public class VmUpdateCallback extends QueuedEventCallback<VmDescribeType> implements Runnable {
  private static Logger LOG = Logger.getLogger( VmUpdateCallback.class );
  private static int SLEEP_TIMER = 5 * 1000;

  public VmUpdateCallback( ClusterConfiguration config ) {
    super( config );
  }

  public void process( final Client cluster, final VmDescribeType msg ) throws Exception {
    VmDescribeResponseType reply = ( VmDescribeResponseType ) cluster.send( msg );
    if ( reply != null ) reply.setOriginCluster( super.getConfig( ).getName() );
    for ( VmInfo vmInfo : reply.getVms() ) {
      vmInfo.setPlacement( super.getConfig( ).getName() );
      VmTypeInfo typeInfo = vmInfo.getInstanceType();
      if( typeInfo.getName() == null || "".equals( typeInfo.getName() ) ) {
        for( VmType t : VmTypes.list() ) {
          if( t.getCpu().equals( typeInfo.getCores() ) && t.getDisk().equals( typeInfo.getDisk() ) && t.getMemory().equals( typeInfo.getMemory() ) ) {
            typeInfo.setName( t.getName() );
          }
        }
      }
    }
    SystemState.handle( reply );
  }

  public void run() {
    do {
      Cluster cluster = Clusters.getInstance( ).lookup( this.getConfig( ).getName( ) );
      VmDescribeType msg = new VmDescribeType();
      msg.setUserId( Component.eucalyptus.name( ) );
      msg.setEffectiveUserId( Component.eucalyptus.name( ) );
      cluster.getMessageQueue().enqueue( new QueuedEvent( this, msg ) );
      this.waitForEvent();
    } while ( !this.isStopped() && this.sleep( SLEEP_TIMER ) );
  }
}
