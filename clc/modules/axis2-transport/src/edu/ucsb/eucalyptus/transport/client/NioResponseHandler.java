package edu.ucsb.eucalyptus.transport.client;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.HttpResponse;

@ChannelPipelineCoverage("all")
public class NioResponseHandler extends SimpleChannelHandler {
  private static Logger LOG = Logger.getLogger( NioResponseHandler.class );

  @Override
  public void messageReceived( ChannelHandlerContext ctx, MessageEvent e ) throws Exception {
    HttpResponse response = ( HttpResponse ) e.getMessage();

    LOG.fatal( "STATUS: " + response.getStatus() );
    LOG.fatal( "VERSION: " + response.getProtocolVersion() );

    if ( !response.getHeaderNames().isEmpty() ) {
      for ( String name : response.getHeaderNames() ) {
        for ( String value : response.getHeaders( name ) ) {
          LOG.fatal( "HEADER: " + name + " = " + value );
        }
      }
    }

    if ( response.getStatus().getCode() == 200 ) {
      ChannelBuffer content = response.getContent();
      if ( content.readable() ) {
        LOG.fatal( "CONTENT:" );
        LOG.fatal( content.toString( "UTF-8" ) );
      }
    }
  }

  @Override
  public void exceptionCaught( final ChannelHandlerContext channelHandlerContext, final ExceptionEvent exceptionEvent ) throws Exception {
    LOG.fatal( exceptionEvent );
    LOG.fatal( exceptionEvent.getCause(), exceptionEvent.getCause() );
  }

}
