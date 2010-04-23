package com.eucalyptus.cluster.handlers;

import java.net.SocketException;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.Networks;
import com.eucalyptus.event.Event;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.util.NetworkUtil;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.Network;
import edu.ucsb.eucalyptus.cloud.NetworkToken;
import edu.ucsb.eucalyptus.msgs.DescribeNetworksResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeNetworksType;
import edu.ucsb.eucalyptus.msgs.NetworkInfoType;

@ChannelPipelineCoverage( "one" )
public class NetworkStateHandler extends AbstractClusterMessageDispatcher {
  private static Logger LOG = Logger.getLogger( NetworkStateHandler.class );

  public NetworkStateHandler( Cluster cluster ) throws BindingException {
    super( cluster );
  }

  @Override
  public void trigger( ) {
    DescribeNetworksType descNetMsg = new DescribeNetworksType( );
    descNetMsg.setUserId( Component.eucalyptus.name( ) );
    descNetMsg.setEffectiveUserId( Component.eucalyptus.name( ) );
    descNetMsg.setClusterControllers( Lists.newArrayList( Clusters.getInstance( ).getClusterAddresses( ) ) );
    try {
      descNetMsg.setNameserver( NetworkUtil.getAllAddresses( ).get( 0 ) );
    } catch ( SocketException e ) {
      LOG.error( e, e );
    }
    this.write( descNetMsg );
  }

  @Override
  public void fireEvent( Event event ) {
    super.fireTimedStatefulTrigger( event );
  }

  @Override
  public void upstreamMessage( ChannelHandlerContext ctx, MessageEvent e ) {
    if ( e.getMessage( ) instanceof MappingHttpResponse ) {
      MappingHttpResponse resp = ( MappingHttpResponse ) e.getMessage( );
      DescribeNetworksResponseType reply = ( DescribeNetworksResponseType ) resp.getMessage( );
      LOG.trace( reply.toString( "eucalyptus_ucsb_edu") );
      for( Network net : Networks.getInstance( ).listValues( ) ) {
        net.trim( reply.getAddrsPerNetwork( ) );
      }
      this.getCluster( ).getState( ).setAddressCapacity( reply.getAddrsPerNetwork( ) );
      this.getCluster( ).getState( ).setMode( reply.getMode( ) );
      List<String> active = Lists.newArrayList( );
      for ( NetworkInfoType netInfo : reply.getActiveNetworks( ) ) {
        Network net = null;
        try {
          net = Networks.getInstance( ).lookup( netInfo.getUserName( ) + "-" + netInfo.getNetworkName( ) );
        } catch ( NoSuchElementException e1 ) {
          net = new Network( netInfo.getUserName( ), netInfo.getNetworkName( ) );
        }
        active.add( net.getName( ) );
        if( net.getVlan().equals( Integer.valueOf(0) ) && net.initVlan( netInfo.getVlan() ) ) {
          NetworkToken netToken = new NetworkToken( this.getCluster( ).getName( ), netInfo.getUserName( ), netInfo.getNetworkName( ), netInfo.getVlan( ) );
          netToken = net.addTokenIfAbsent( netToken );
        }
      }
      
      for( Network net : Networks.getInstance( ).listValues( Networks.State.ACTIVE ) ) {
        net.trim( reply.getAddrsPerNetwork( ) );
//TODO: update the network index/token state here.  ultimately needed for failure modes.
      }
      List<Cluster> ccList = Clusters.getInstance( ).listValues( );
      int ccNum = ccList.size( );
      for ( Cluster c : ccList ) {
        ccNum -= c.getState( ).getMode( );
      }
      this.verified = true;
    } else {
      LOG.warn( "Received unknown message type. " + e.getMessage( ) );
    }
  }
  
  @Override
  public void advertiseEvent( Event event ) {}



}
