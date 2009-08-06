package com.eucalyptus.ws.handlers;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelLocal;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.MessageEvent;
import org.mule.MuleServer;
import org.mule.api.MuleContext;
import org.mule.api.registry.Registry;
import org.mule.config.spring.SpringRegistry;

import com.eucalyptus.ws.MappingHttpMessage;
import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.MappingHttpResponse;
import com.eucalyptus.ws.util.Messaging;
import com.eucalyptus.ws.util.ReplyQueue;

import edu.ucsb.eucalyptus.constants.EventType;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.EventRecord;
import edu.ucsb.eucalyptus.msgs.PutObjectType;
import edu.ucsb.eucalyptus.msgs.WalrusDataGetResponseType;
import edu.ucsb.eucalyptus.msgs.WalrusDeleteResponseType;

@ChannelPipelineCoverage( "one" )
public class ServiceSinkHandler implements ChannelDownstreamHandler, ChannelUpstreamHandler {
  private static Logger      LOG              = Logger.getLogger( ServiceSinkHandler.class );
  private long               QUEUE_TIMEOUT_MS = 2; //TODO: measure me
  private long               startTime;
  private ChannelLocal<MappingHttpMessage> requestLocal = new ChannelLocal<MappingHttpMessage>();

  @Override
  public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    LOG.debug( this.getClass( ).getSimpleName( ) + "[incoming]: " + e );
    if ( e instanceof MessageEvent ) {
      this.startTime = System.currentTimeMillis( );
      final MessageEvent event = ( MessageEvent ) e;
      if ( event.getMessage( ) instanceof MappingHttpMessage ) {
        MappingHttpMessage request = ( MappingHttpMessage ) event.getMessage( );
        requestLocal.set( ctx.getChannel( ), request );
        EucalyptusMessage msg = ( EucalyptusMessage ) request.getMessage( );
        LOG.info( EventRecord.create( this.getClass( ).getSimpleName( ), msg.getUserId( ), msg.getCorrelationId( ), EventType.MSG_RECEIVED, msg.getClass( ).getSimpleName( ) ) );
        ReplyQueue.addReplyListener( msg.getCorrelationId( ), ctx );
        Messaging.dispatch( "vm://RequestQueue", msg );
      }
    }
  }

  @SuppressWarnings( "unchecked" )
  @Override
  public void handleDownstream( final ChannelHandlerContext ctx, final ChannelEvent e ) throws Exception {
    if ( e instanceof MessageEvent && ( ( MessageEvent ) e ).getMessage( ) instanceof EucalyptusMessage ) {
      MappingHttpMessage request = requestLocal.get( ctx.getChannel( ) );
      EucalyptusMessage reply = ( EucalyptusMessage ) ( ( MessageEvent ) e ).getMessage( );
      if ( reply == null ) {
        // TODO: fix this error reporting
        LOG.warn( "Received a null response: " + request.getMessageString( ) );
        reply = new EucalyptusErrorMessageType( this.getClass( ).getSimpleName( ), ( EucalyptusMessage ) request.getMessage( ), "Received a NULL reply" );
      }
      LOG.info( EventRecord.create( this.getClass( ).getSimpleName( ), reply.getUserId( ), reply.getCorrelationId( ), EventType.MSG_SERVICED, ( System.currentTimeMillis( ) - startTime ) ) );      
      MappingHttpResponse response = new MappingHttpResponse( request.getProtocolVersion( ) );
      DownstreamMessageEvent newEvent = new DownstreamMessageEvent( ctx.getChannel( ), e.getFuture( ), response, null );
      response.setMessage( reply );
      if ( !( reply instanceof WalrusDataGetResponseType ) ) {
        ctx.sendDownstream( newEvent );
        newEvent.getFuture( ).addListener( ChannelFutureListener.CLOSE );
      }
    }
  }
}
