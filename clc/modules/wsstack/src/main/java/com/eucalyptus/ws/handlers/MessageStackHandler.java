package com.eucalyptus.ws.handlers;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;

public abstract class MessageStackHandler implements ChannelDownstreamHandler, ChannelUpstreamHandler {
  private static Logger LOG = Logger.getLogger( MessageStackHandler.class );

  public void handleDownstream( final ChannelHandlerContext channelHandlerContext, final ChannelEvent channelEvent ) throws Exception {
    MessageStackHandler.LOG.debug( this.getClass( ).getSimpleName( ) + "[outgoing]: " + channelEvent );
    if ( channelEvent instanceof MessageEvent ) {
      final MessageEvent msgEvent = ( MessageEvent ) channelEvent;
      this.outgoingMessage( channelHandlerContext, msgEvent );
    }
    channelHandlerContext.sendDownstream( channelEvent );
  }

  public abstract void outgoingMessage( final ChannelHandlerContext ctx, MessageEvent event ) throws Exception;

  public abstract void incomingMessage( final ChannelHandlerContext ctx, MessageEvent event ) throws Exception;

  public void exceptionCaught( final Throwable t ) throws Exception {

  }

  public void exceptionCaught( final ChannelHandlerContext channelHandlerContext, final ExceptionEvent exceptionEvent ) throws Exception {
    MessageStackHandler.LOG.debug( this.getClass( ).getSimpleName( ) + "[exception:" + exceptionEvent.getCause( ).getClass( ).getSimpleName( ) + "]: " + exceptionEvent.getCause( ).getMessage( ) );
  }

  public void handleUpstream( final ChannelHandlerContext channelHandlerContext, final ChannelEvent channelEvent ) throws Exception {
    MessageStackHandler.LOG.debug( this.getClass( ).getSimpleName( ) + "[incoming]: " + channelEvent );
    if ( channelEvent instanceof MessageEvent ) {
      final MessageEvent msgEvent = ( MessageEvent ) channelEvent;
      this.incomingMessage( channelHandlerContext, msgEvent );
    } else if ( channelEvent instanceof ExceptionEvent ) {
      this.exceptionCaught( channelHandlerContext, ( ExceptionEvent ) channelEvent );
    }
    channelHandlerContext.sendUpstream( channelEvent );
  }
}
