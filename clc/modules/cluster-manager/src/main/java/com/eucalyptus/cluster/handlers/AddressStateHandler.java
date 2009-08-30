package com.eucalyptus.cluster.handlers;

import java.nio.channels.AlreadyConnectedException;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.event.NewClusterEvent;
import com.eucalyptus.cluster.event.TeardownClusterEvent;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.GenericEvent;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.BindingException;
import com.eucalyptus.ws.MappingHttpResponse;

import edu.ucsb.eucalyptus.cloud.Pair;
import edu.ucsb.eucalyptus.cloud.entities.Address;
import edu.ucsb.eucalyptus.cloud.net.Addresses;
import edu.ucsb.eucalyptus.msgs.DescribeAddressesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribePublicAddressesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribePublicAddressesType;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;

@ChannelPipelineCoverage("one")
public class AddressStateHandler extends AbstractClusterMessageDispatcher {
  private static Logger LOG = Logger.getLogger( NetworkStateHandler.class );
  public AddressStateHandler( Cluster cluster ) throws BindingException {
    super( cluster );
  }

  @Override
  public void trigger( ) {
    DescribePublicAddressesType drMsg = new DescribePublicAddressesType( );
    drMsg.setUserId( Component.eucalyptus.name() );
    drMsg.setEffectiveUserId( Component.eucalyptus.name() );
    this.write( drMsg );
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
      LOG.trace( "Ignoring unknown event: " + LogUtil.dumpObject( event ) );
    }
  }

  @Override
  public void downstreamMessage( ChannelHandlerContext ctx, MessageEvent e ) {
    ctx.sendDownstream( e );
  }

  @Override
  public void upstreamMessage( ChannelHandlerContext ctx, MessageEvent e ) {
    if( e.getMessage( ) instanceof MappingHttpResponse ) {
      MappingHttpResponse resp = (MappingHttpResponse) e.getMessage( );
      DescribePublicAddressesResponseType reply = (DescribePublicAddressesResponseType) resp.getMessage( );   
      if ( reply.get_return( ) ) {
        EucalyptusProperties.disableNetworking = false;
        for ( Pair p : Pair.getPaired( reply.getAddresses( ), reply.getMapping( ) ) )
          try {
            Address blah = Addresses.getInstance( ).lookup( p.getLeft( ) );
            blah.setInstanceAddress( p.getRight( ) );
          } catch ( NoSuchElementException ex ) {
            Addresses.getInstance( ).registerDisabled( new Address( p.getLeft( ), this.getCluster().getName( ) ) );
          }
      } else {
        if ( !EucalyptusProperties.disableNetworking ) {
          LOG.warn( "Response from cluster [" + this.getCluster( ).getName( ) + "]: " + reply.getStatusMessage( ) );
        }
        EucalyptusProperties.disableNetworking = true;
      }
      this.verified = true;
      ctx.getChannel( ).close( );
    }
  }

  @Override
  public void advertiseEvent( Event event ) {}

  @Override
  public void exceptionCaught( ChannelHandlerContext ctx, ExceptionEvent e ) throws Exception {
    if( e.getCause( ) instanceof AlreadyConnectedException ) {
    } else {
      this.exceptionCaught( e.getCause( ) );
    }
  }

  @Override
  public void exceptionCaught( Throwable cause ) {
    LOG.info( cause, cause );
  }
}

