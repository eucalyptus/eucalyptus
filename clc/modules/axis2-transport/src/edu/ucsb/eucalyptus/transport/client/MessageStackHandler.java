package edu.ucsb.eucalyptus.transport.client;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;

public abstract class MessageStackHandler implements ChannelDownstreamHandler, ChannelUpstreamHandler {
  private static Logger LOG = Logger.getLogger( MessageStackHandler.class );

  public void handleDownstream( final ChannelHandlerContext channelHandlerContext, final ChannelEvent channelEvent ) throws Exception {
    LOG.fatal(this.getClass().getSimpleName() + "[outgoing]: " + channelEvent);
    if( channelEvent instanceof MessageEvent ) {
      MessageEvent msgEvent = (MessageEvent) channelEvent;
      this.outgoingMessage( msgEvent );
    }
    channelHandlerContext.sendDownstream( channelEvent );
  }

  public abstract void outgoingMessage( MessageEvent event ) throws Exception;
  public abstract void incomingMessage( MessageEvent event ) throws Exception;

  public void handleUpstream( final ChannelHandlerContext channelHandlerContext, final ChannelEvent channelEvent ) throws Exception {
    LOG.fatal(this.getClass().getSimpleName() + "[incoming]: " + channelEvent);
    if( channelEvent instanceof MessageEvent ) {
      MessageEvent msgEvent = (MessageEvent) channelEvent;
      this.incomingMessage( msgEvent );
    }
    channelHandlerContext.sendDownstream( channelEvent );
  }
}
