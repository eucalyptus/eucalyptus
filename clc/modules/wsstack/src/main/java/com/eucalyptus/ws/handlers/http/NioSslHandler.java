package com.eucalyptus.ws.handlers.http;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.ssl.SslHandler;

public class NioSslHandler extends SslHandler {

  public NioSslHandler( SSLEngine engine ) {
    super( engine );
  }

  @Override
  public void channelBound( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
    super.channelBound( ctx, e );
  }

  @Override
  public void channelOpen( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
    super.channelOpen( ctx, e );
  }

  @Override
  public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    if( e instanceof MessageEvent ) {
      Object o = ((MessageEvent) e).getMessage( );      
      if( ! (o instanceof ChannelBuffer ) ) {
        ctx.getPipeline( ).removeFirst( );//this should be me.
        ctx.sendUpstream(e);
        return;
      } else { //we punt on HTTP and only delegate SSL.
      }
    }
    super.handleUpstream( ctx, e );
  }


}
