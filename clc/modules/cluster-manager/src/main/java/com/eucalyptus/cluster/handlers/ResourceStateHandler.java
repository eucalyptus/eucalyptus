package com.eucalyptus.cluster.handlers;

import java.nio.channels.AlreadyConnectedException;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.event.Event;
import com.eucalyptus.ws.BindingException;

public class ResourceStateHandler extends AbstractClusterMessageDispatcher {
  private static Logger LOG = Logger.getLogger( NetworkStateHandler.class );
  public ResourceStateHandler( Cluster cluster ) throws BindingException {
    super( cluster );
  }

  @Override
  public void trigger( ) {}

  @Override
  public void fireEvent( Event event ) {}

  @Override
  public void downstreamMessage( ChannelHandlerContext ctx, MessageEvent e ) {
    ctx.sendDownstream( e );
  }

  @Override
  public void upstreamMessage( ChannelHandlerContext ctx, MessageEvent e ) {
    ctx.sendUpstream( e );
    ctx.getChannel( ).close( );
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