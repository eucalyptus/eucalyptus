package edu.ucsb.eucalyptus.cloud.cluster;

import java.net.SocketException;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.util.NetworkUtil;
import com.eucalyptus.ws.client.Client;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.cloud.entities.VmType;
import edu.ucsb.eucalyptus.msgs.DescribeNetworksType;
import edu.ucsb.eucalyptus.msgs.DescribeNetworksResponseType;

public class NetworkUpdateCallback extends QueuedEventCallback<DescribeNetworksType> implements Runnable {
  private static Logger LOG = Logger.getLogger( NetworkUpdateCallback.class );
  private static int SLEEP_TIMER = 3 * 1000;
  private boolean firstTime = true;

  public NetworkUpdateCallback ( ClusterConfiguration config ) {
    super( config );
  }

  public void process( final Client cluster, final DescribeNetworksType msg ) throws Exception {
    DescribeNetworksResponseType reply = ( DescribeNetworksResponseType ) cluster.send( msg );
    reply.getActiveNetworks( );//TODO: restore active networks after restart.
    Cluster parent = Clusters.getInstance( ).lookup( this.getConfig( ).getName( ) );
    parent.getState( ).setAddressCapacity( reply.getAddrsPerNetwork( ) );
    parent.getState( ).setMode( reply.getMode( ) );
    this.notifyHandler( );
  }

  public void run() {
    do {
      Cluster cluster = Clusters.getInstance( ).lookup( this.getConfig( ).getName( ) );
      DescribeNetworksType descNetMsg = new DescribeNetworksType();
      descNetMsg.setUserId( Component.eucalyptus.name() );
      descNetMsg.setEffectiveUserId( Component.eucalyptus.name() );
      descNetMsg.setClusterControllers( Lists.newArrayList( Clusters.getInstance( ).getClusterAddresses( ) ) );
      try {
        descNetMsg.setNameserver( NetworkUtil.getAllAddresses( ).get( 0 ) );//TODO: get the canonical address here !!!
      } catch ( SocketException e ) {
        LOG.error( e, e);
      }
      cluster.getMessageQueue().enqueue( new QueuedEvent( this, descNetMsg ) );
      this.waitForEvent();
    } while ( !this.isStopped() && this.sleep( SLEEP_TIMER ) );

  }

}
