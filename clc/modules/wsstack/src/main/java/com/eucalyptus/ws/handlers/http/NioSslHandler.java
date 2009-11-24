package com.eucalyptus.ws.handlers.http;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.ssl.SslHandler;

import com.eucalyptus.auth.util.SslSetup;
import com.eucalyptus.ws.util.HttpUtils;

@ChannelPipelineCoverage("one")
public class NioSslHandler extends SslHandler {
  private AtomicBoolean first = new AtomicBoolean( true );
  
  public NioSslHandler( ) {
    super( SslSetup.getServerEngine( ) );
  }
    
  @Override
  public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    Object o = null;
    if ( e instanceof MessageEvent
        && first.compareAndSet( true, false )
        && ( o = ( ( MessageEvent ) e ).getMessage( ) ) instanceof ChannelBuffer 
        && !HttpUtils.maybeSsl( ( ChannelBuffer ) o ) ) {
      ctx.getPipeline( ).removeFirst( );
      ctx.sendUpstream( e );
    } else {
      super.handleUpstream( ctx, e );
    }
  }
  
}
