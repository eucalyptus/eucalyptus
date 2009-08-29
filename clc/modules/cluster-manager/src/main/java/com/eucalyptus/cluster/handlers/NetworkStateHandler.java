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
import com.eucalyptus.cluster.event.NewClusterEvent;
import com.eucalyptus.cluster.event.TeardownClusterEvent;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.GenericEvent;
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
    if ( this.timedTrigger( event ) ) {
      this.trigger( );
    } else if ( event instanceof GenericEvent ) {
      GenericEvent<Cluster> g = ( GenericEvent<Cluster> ) event;
      if ( !g.matches( this.getCluster( ) ) ) { return; }
      if ( g instanceof NewClusterEvent ) {
        this.trigger( );
      } else if ( event instanceof TeardownClusterEvent ) {
        this.cleanup( );
      }
    } else {
      LOG.debug( "Ignoring event which doesn't belong to me: " + LogUtil.dumpObject( event ) );
    }
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
    } else {
      LOG.info( "Received unknown message type. " + e.getMessage( ) );
    }
    ctx.getChannel( ).close( );
  }

  @Override
  public void advertiseEvent( Event event ) {
  }

  @Override
  public void exceptionCaught( ChannelHandlerContext ctx, ExceptionEvent e ) throws Exception {
    if ( e.getCause( ) instanceof AlreadyConnectedException ) {
    } else {
      this.exceptionCaught( e.getCause( ) );
    }
  }

  @Override
  public void exceptionCaught( Throwable cause ) {
    LOG.info( cause, cause );
  }

}
