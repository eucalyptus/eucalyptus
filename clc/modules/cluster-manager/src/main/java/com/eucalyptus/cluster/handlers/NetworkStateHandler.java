package com.eucalyptus.cluster.handlers;

import java.net.SocketException;
import java.nio.channels.AlreadyConnectedException;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.Networks;
import com.eucalyptus.event.Event;
import com.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.NetworkUtil;
import com.eucalyptus.ws.BindingException;
import com.eucalyptus.ws.MappingHttpResponse;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.cloud.Network;
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
  public void downstreamMessage( ChannelHandlerContext ctx, MessageEvent e ) {
    ctx.sendDownstream( e );
  }

  @Override
  public void upstreamMessage( ChannelHandlerContext ctx, MessageEvent e ) {
    if ( e.getMessage( ) instanceof MappingHttpResponse ) {
      MappingHttpResponse resp = ( MappingHttpResponse ) e.getMessage( );
      DescribeNetworksResponseType reply = ( DescribeNetworksResponseType ) resp.getMessage( );
      ctx.getChannel( ).close( );
      for ( NetworkInfoType netInfo : reply.getActiveNetworks( ) ) {
        try {
          Network net = Networks.getInstance( ).lookup( netInfo.getUserName( ) + "-" + netInfo.getNetworkName( ) );
          net.getAvailableAddresses( ).removeAll( netInfo.getAllocatedAddresses( ) );
          net.getToken( this.getCluster( ).getName( ) ).setVlan( netInfo.getVlan( ) );
        } catch ( NoSuchElementException e1 ) {
          LOG.warn( "Got back network info for an extant allocation, but cant do anything with it." + LogUtil.dumpObject( netInfo ) );
        }
      }
      this.getCluster( ).getState( ).setAddressCapacity( reply.getAddrsPerNetwork( ) );
      this.getCluster( ).getState( ).setMode( reply.getMode( ) );
      List<Cluster> ccList = Clusters.getInstance( ).listValues( );
      int ccNum = ccList.size( );
      for ( Cluster c : ccList ) {
        ccNum -= c.getState( ).getMode( );
      }
      if ( ccNum != 0 ) {
        EucalyptusProperties.disableNetworking = true;
      } else {
        EucalyptusProperties.disableNetworking = false;
      }
      this.verified = true;
    } else {
      LOG.warn( "Received unknown message type. " + e.getMessage( ) );
      ctx.getChannel( ).close( );
    }
  }

  @Override
  public void advertiseEvent( Event event ) {}



}
