package com.eucalyptus.ws.handlers;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelLocal;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;

import com.eucalyptus.auth.User;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.ws.MappingHttpMessage;
import com.eucalyptus.ws.MappingHttpResponse;
import com.eucalyptus.ws.client.NioMessageReceiver;
import com.eucalyptus.ws.util.Messaging;
import com.eucalyptus.ws.util.ReplyQueue;

import edu.ucsb.eucalyptus.constants.EventType;
import edu.ucsb.eucalyptus.constants.IsData;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.EventRecord;
import edu.ucsb.eucalyptus.msgs.GetObjectResponseType;
import edu.ucsb.eucalyptus.msgs.WalrusDataGetResponseType;

@ChannelPipelineCoverage( "one" )
public class ServiceSinkHandler extends SimpleChannelHandler {
  private static Logger                    LOG              = Logger.getLogger( ServiceSinkHandler.class );
  private long                             QUEUE_TIMEOUT_MS = 2;                                           //TODO: measure me
  private long                             startTime;
  private ChannelLocal<MappingHttpMessage> requestLocal     = new ChannelLocal<MappingHttpMessage>( );

  private NioMessageReceiver msgReceiver;
  
  public ServiceSinkHandler( ) {
    super( );
  }

  public ServiceSinkHandler( NioMessageReceiver msgReceiver ) {
    super( );
    this.msgReceiver = msgReceiver;
  }



  @Override
  public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    LOG.debug( this.getClass( ).getSimpleName( ) + "[incoming]: " + e );
    if( e instanceof ExceptionEvent) {
      this.exceptionCaught( ctx, (ExceptionEvent)e );
    } else if ( e instanceof MessageEvent ) {
      this.startTime = System.currentTimeMillis( );
      final MessageEvent event = ( MessageEvent ) e;
      if ( event.getMessage( ) instanceof MappingHttpMessage ) {
        MappingHttpMessage request = ( MappingHttpMessage ) event.getMessage( );
        User user = request.getUser( );
        requestLocal.set( ctx.getChannel( ), request );
        EucalyptusMessage msg = (EucalyptusMessage) request.getMessage( );
        if( user != null && msgReceiver == null) {
          msg.setUserId( user.getUserName( ) );
          msg.setEffectiveUserId( user.getIsAdministrator( )?Component.eucalyptus.name():user.getUserName( ) );
        }
        LOG.info( EventRecord.create( this.getClass( ).getSimpleName( ), msg.getUserId( ), msg.getCorrelationId( ), EventType.MSG_RECEIVED, msg.getClass( ).getSimpleName( ) ) );
        ReplyQueue.addReplyListener( msg.getCorrelationId( ), ctx );
        if( this.msgReceiver == null ) {
          Messaging.dispatch( "vm://RequestQueue", msg );
        } else if (user == null||user.getIsAdministrator( )){
          MuleMessage reply = this.msgReceiver.routeMessage( new DefaultMuleMessage( this.msgReceiver.getConnector().getMessageAdapter( msg ) ) );
          ctx.getChannel( ).write( reply.getPayload( ) );
        }
      }
    }
  }

  @SuppressWarnings( "unchecked" )
  @Override
  public void handleDownstream( final ChannelHandlerContext ctx, final ChannelEvent e ) throws Exception {
    if ( e instanceof MessageEvent ) {
      MessageEvent msge = ( MessageEvent ) e;
      if ( msge.getMessage( ) instanceof IsData ) {//Pass through for chunked messaging
        ctx.sendDownstream( msge );
      } else if ( msge.getMessage( ) instanceof EucalyptusMessage ) {//Handle single request-response MEP
        MappingHttpMessage request = requestLocal.get( ctx.getChannel( ) );
        EucalyptusMessage reply = ( EucalyptusMessage ) ( ( MessageEvent ) e ).getMessage( );
        if ( reply == null ) {// TODO: fix this error reporting
          LOG.warn( "Received a null response for request: " + request.getMessageString( ) );
          reply = new EucalyptusErrorMessageType( this.getClass( ).getSimpleName( ), ( EucalyptusMessage ) request.getMessage( ), "Received a NULL reply" );
        }
        LOG.info( EventRecord.create( this.getClass( ).getSimpleName( ), reply.getUserId( ), reply.getCorrelationId( ), EventType.MSG_SERVICED, ( System.currentTimeMillis( ) - startTime ) ) );
        if ( reply instanceof WalrusDataGetResponseType ) {
          if ( reply instanceof GetObjectResponseType ) {
            GetObjectResponseType getObjectResponse = ( GetObjectResponseType ) reply;
            if ( getObjectResponse.getBase64Data( ) == null ) return;
          } else {
            return;
          }
        }
        MappingHttpResponse response = new MappingHttpResponse( request.getProtocolVersion( ) );
        DownstreamMessageEvent newEvent = new DownstreamMessageEvent( ctx.getChannel( ), e.getFuture( ), response, null );
        response.setMessage( reply );
        ctx.sendDownstream( newEvent );
        newEvent.getFuture( ).addListener( ChannelFutureListener.CLOSE );
      } else {
        
      }
    }
  }
}
