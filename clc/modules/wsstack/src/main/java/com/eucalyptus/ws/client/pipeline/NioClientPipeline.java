package com.eucalyptus.ws.client.pipeline;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;

import com.eucalyptus.ws.BindingException;
import com.eucalyptus.ws.binding.Binding;
import com.eucalyptus.ws.binding.BindingManager;
import com.eucalyptus.ws.handlers.BindingHandler;
import com.eucalyptus.ws.handlers.NioHttpResponseDecoder;
import com.eucalyptus.ws.handlers.NioResponseHandler;
import com.eucalyptus.ws.handlers.SoapMarshallingHandler;
import com.eucalyptus.ws.handlers.http.NioHttpRequestEncoder;
import com.eucalyptus.ws.handlers.soap.SoapHandler;
import com.eucalyptus.ws.handlers.wssecurity.WsSecHandler;
import com.eucalyptus.ws.handlers.MessageStackHandler;

public class NioClientPipeline implements ChannelPipelineFactory {
  private static Logger            LOG = Logger.getLogger( NioClientPipeline.class );

  private final NioResponseHandler handler;
  private BindingHandler           bindingHandler;
  private final WsSecHandler       wssecHandler;
  
  public NioClientPipeline( final NioResponseHandler handler, final String clientBinding ) {
    this( handler, clientBinding, null );
  }

  public NioClientPipeline( final NioResponseHandler handler, final String clientBinding, final WsSecHandler wssecHandler ) {
    this.handler = handler;
    // TODO: Fix wrapping of the binding
    try {
      Binding binding = BindingManager.getBinding( clientBinding );
      this.bindingHandler = new BindingHandler( binding );
    } catch ( BindingException e ) {
      LOG.error( e, e );
    }
    this.wssecHandler = wssecHandler;
  }

  public ChannelPipeline getPipeline( ) throws Exception {
    ChannelPipeline pipeline = Channels.pipeline( );

    pipeline.addLast( "decoder", new NioHttpResponseDecoder( ) );
    pipeline.addLast( "aggregator", new HttpChunkAggregator( 1048576 ) );
    pipeline.addLast( "encoder", new NioHttpRequestEncoder( ) );
    pipeline.addLast( "serializer", new SoapMarshallingHandler( ) );
    if ( this.wssecHandler != null ) {
      pipeline.addLast( "wssec", this.wssecHandler );
    }
    pipeline.addLast( "soap", new SoapHandler( ) );
    pipeline.addLast( "binding", bindingHandler );
    pipeline.addLast( "handler", handler );
    return pipeline;
  }
  
  public BindingHandler getBindingHandler( ) {
    return bindingHandler;
  }

  public WsSecHandler getWssecHandler( ) {
    return wssecHandler;
  }

  public NioResponseHandler getHandler( ) {
    return handler;
  }
}
