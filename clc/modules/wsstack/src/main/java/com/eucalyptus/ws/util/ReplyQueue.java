package com.eucalyptus.ws.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelLocal;
import org.jboss.netty.channel.Channels;
import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.message.ExceptionMessage;

import com.eucalyptus.ws.binding.BindingManager;

import edu.ucsb.eucalyptus.cloud.RequestTransactionScript;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

public class ReplyQueue {

  private static Logger                                 LOG                   = Logger.getLogger( ReplyQueue.class );
  //TODO: measure me
  private static int                                    MAP_CAPACITY          = 64;
  private static int                                    MAP_NUM_CONCURRENT    = MAP_CAPACITY / 2;
  private static float                                  MAP_BIN_AVG_THRESHOLD = 1.0f;
  private static long                                   MAP_GET_WAIT_MS       = 10;
  private static ConcurrentMap<String, ChannelHandlerContext> pending               = new ConcurrentHashMap<String, ChannelHandlerContext>( MAP_CAPACITY, MAP_BIN_AVG_THRESHOLD, MAP_NUM_CONCURRENT );

  public static void addReplyListener( String correlationId, ChannelHandlerContext ctx ) {
    pending.put( correlationId, ctx );
  }
  
  @SuppressWarnings( "unchecked" )
  public void handle( EucalyptusMessage responseMessage ) {
    String corrId = responseMessage.getCorrelationId( );
    ChannelHandlerContext ctx = pending.remove( corrId );
    if ( ctx == null ) {
      LOG.warn( "Received a reply for absent client:  No channel to write response message." );
      LOG.debug( responseMessage );
    } else {
      Channels.write( ctx.getChannel( ), responseMessage );
    }
  }

  public void handle( ExceptionMessage exMsg ) {
    Throwable exception = exMsg.getException( );
    Object payload = null;
    EucalyptusMessage msg = null;
    if ( exception instanceof MessagingException ) {
      MessagingException ex = ( MessagingException ) exception;
      MuleMessage muleMsg = ex.getUmoMessage( );

      if ( payload instanceof RequestTransactionScript ) {
        msg = ( ( RequestTransactionScript ) payload ).getRequestMessage( );
      } else {
        try {
          msg = parsePayload( muleMsg.getPayload( ) );
        } catch ( Exception e ) {
          LOG.error( "Bailing out of error handling: don't have the correlationId for the caller!" );
          LOG.error( e, e );
          return;
        }
      }
      EucalyptusErrorMessageType errMsg = getErrorMessageType( exMsg, msg );
      errMsg.setException( exception.getCause( ) );
      this.handle( errMsg );
//      replies.putMessage( errMsg );
    }
  }

  private EucalyptusErrorMessageType getErrorMessageType( final ExceptionMessage exMsg, final EucalyptusMessage msg ) {
    Throwable exception = exMsg.getException( );
    EucalyptusErrorMessageType errMsg = null;
    if ( exception != null ) {
      Throwable e = exMsg.getException( ).getCause( );
      if ( e != null ) {
        errMsg = new EucalyptusErrorMessageType( exMsg.getComponentName( ), msg, e.getMessage( ) );
      }
    }
    if ( errMsg == null ) {
      ByteArrayOutputStream exStream = new ByteArrayOutputStream( );
      exception.printStackTrace( new PrintStream( exStream ) );
      errMsg = new EucalyptusErrorMessageType( exMsg.getComponentName( ), msg, "Internal Error: \n" + exStream.toString( ) );
    }
    return errMsg;
  }

  private EucalyptusMessage parsePayload( Object payload ) throws Exception {
    if ( payload instanceof EucalyptusMessage ) {
      return ( EucalyptusMessage ) payload;
    } else if ( payload instanceof VmAllocationInfo ) {
      return ( ( VmAllocationInfo ) payload ).getRequest( );
    } else {
      return ( EucalyptusMessage ) BindingManager.getBinding( "msgs_eucalyptus_ucsb_edu" ).fromOM( ( String ) payload );
    }
  }

}
