package com.eucalyptus.ws.server;

import static org.jboss.netty.channel.Channels.pipeline;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

import com.eucalyptus.ws.handlers.http.NioHttpDecoder;

public class NioServerPipelineFactory implements ChannelPipelineFactory {
  public ChannelPipeline getPipeline( ) throws Exception {
    ChannelPipeline pipeline = pipeline( );
    // SSLEngine engine = SecureChatSslContextFactory.getServerContext().createSSLEngine();
    // engine.setUseClientMode(false);
    // pipeline.addLast("ssl", new SslHandler(engine));
    pipeline.addLast( "decoder", new NioHttpDecoder( ) );
    pipeline.addLast( "encoder", new HttpResponseEncoder( ) );
    pipeline.addLast( "chunkedWriter", new ChunkedWriteHandler( ) );
    pipeline.addLast( "handler", new NioServerHandler( ) );
    return pipeline;
  }
}
