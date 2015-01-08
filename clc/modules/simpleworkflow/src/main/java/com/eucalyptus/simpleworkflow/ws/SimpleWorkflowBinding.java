/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.simpleworkflow.ws;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.codehaus.jackson.JsonProcessingException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.auth.InvalidAccessKeyAuthException;
import com.eucalyptus.auth.InvalidSignatureAuthException;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.simpleworkflow.SwfJsonUtils;
import com.eucalyptus.simpleworkflow.common.model.SimpleWorkflowMessage;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.handlers.ExceptionMarshallerHandler;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;

/**
 *
 */
public class SimpleWorkflowBinding extends MessageStackHandler implements ExceptionMarshallerHandler {

  @Override
  public void incomingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
      try {
        BaseMessage msg = bind( httpRequest );
        httpRequest.setMessage( msg );
      } catch ( Exception e ) {
        if ( !( e instanceof BindingException ) ) {
          e = new BindingException( e );
        }
        throw e;
      }
    }
  }

  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpResponse ) {
      MappingHttpResponse httpResponse = ( MappingHttpResponse ) event.getMessage( );
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream( 8192 );
      if ( httpResponse.getMessage( ) instanceof EucalyptusErrorMessageType ) {
        httpResponse.setStatus( HttpResponseStatus.BAD_REQUEST );
        final Optional<String> correlationId = getCorrelationId( event );
        if ( correlationId.isPresent( ) ) httpResponse.addHeader( "x-amzn-RequestId", correlationId.get( ) );
        SwfJsonUtils.writeObject( byteOut, ImmutableMap.of(
            "__type", "InternalFailure",
            "message", ((EucalyptusErrorMessageType)httpResponse.getMessage( )).getMessage() ) );
      } else if ( httpResponse.getMessage( ) instanceof ExceptionResponseType ) { //handle error case specially
        ExceptionResponseType msg = ( ExceptionResponseType ) httpResponse.getMessage( );
        httpResponse.setStatus( msg.getHttpStatus( ) );
        final Optional<String> correlationId = getCorrelationId( event );
        if ( correlationId.isPresent( ) ) httpResponse.addHeader( "x-amzn-RequestId", correlationId.get( ) );
        SwfJsonUtils.writeObject( byteOut, ImmutableMap.of(
            "__type", "InternalFailure",
            "message", ((ExceptionResponseType)httpResponse.getMessage( )).getMessage() ) );
      } else if ( httpResponse.getMessage( ) != null ){ //actually try to bind response
        final Object message = httpResponse.getMessage( );
        if ( message instanceof SimpleWorkflowMessage ) {
          httpResponse.addHeader( "x-amzn-RequestId", ( (SimpleWorkflowMessage) message ).getCorrelationId() );
          if ( !SimpleWorkflowMessage.class.equals( message.getClass() ) ) {
            SwfJsonUtils.writeObject( byteOut, message );
          }
        }
      }
      byte[] req = byteOut.toByteArray( );
      ChannelBuffer buffer = ChannelBuffers.wrappedBuffer( req );
      httpResponse.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes( ) ) );
      httpResponse.addHeader( HttpHeaders.Names.CONTENT_TYPE, "application/x-amz-json-1.0" );
      httpResponse.setContent( buffer );
    }
  }

  @Nonnull
  @Override
  public ExceptionResponse marshallException( @Nonnull ChannelEvent event,
                                              @Nonnull final HttpResponseStatus status,
                                              @Nonnull final Throwable throwable ) throws Exception {
    HttpResponseStatus responseStatus = status;
    final Map<String,String> headers = Maps.newHashMap();
    headers.put( HttpHeaders.Names.CONTENT_TYPE, "application/x-amz-json-1.0" );
    final Optional<String> correlationId = getCorrelationId( event );
    if ( correlationId.isPresent( ) ) {
      headers.put( "x-amzn-RequestId", correlationId.get(  ) );
    }

    String type = "InternalFailure"; // code
    String message = throwable.getMessage( );
    if ( Exceptions.isCausedBy( throwable, InvalidAccessKeyAuthException.class ) ) {
      responseStatus = HttpResponseStatus.FORBIDDEN;
      type = "InvalidClientTokenId";
      message = "The security token included in the request is invalid.";
    } else if ( Exceptions.isCausedBy( throwable, InvalidSignatureAuthException.class ) ) {
      responseStatus = HttpResponseStatus.FORBIDDEN;
      type = "InvalidSignatureException";
      message = "The request signature we calculated does not match the signature you provided. Check your AWS Secret Access Key and signing method.";
    } else if ( throwable instanceof BindingException ) {
      responseStatus = HttpResponseStatus.BAD_REQUEST;
      type = "InvalidParameterValue";
    }

    final ByteArrayOutputStream byteOut = new ByteArrayOutputStream( 8192 );
    SwfJsonUtils.writeObject( byteOut, ImmutableMap.of( "__type", type, "message", message ) );

    return new ExceptionResponse(
        responseStatus,
        ChannelBuffers.wrappedBuffer( byteOut.toByteArray( ) ),
        ImmutableMap.copyOf( headers )
    );
  }

  @SuppressWarnings( "unchecked" )
  private BaseMessage bind( final MappingHttpRequest httpRequest ) throws BindingException, IOException {
    // find action
    final String target = Objects.toString( httpRequest.getHeader( "X-Amz-Target" ), "" );
    final String simpleClassName;
    if ( target.startsWith( "SimpleWorkflowService." ) ) {
      simpleClassName = target.substring( 22 ) + "Request";
    } else {
      throw new BindingException( "Unable to get action from target header: " + target );
    }

    // parse message
    final ChannelBuffer buffer = httpRequest.getContent( );
    buffer.markReaderIndex( );
    byte[] read = new byte[ buffer.readableBytes( ) ];
    buffer.readBytes( read );
    final String content = new String( read, Charsets.UTF_8 );
    buffer.resetReaderIndex( );

    final BaseMessage message;
    try {
      message = SwfJsonUtils.readObject(
          content,
          (Class<? extends BaseMessage>) Class.forName( SimpleWorkflowMessage.class.getPackage( ).getName( ) + "." + simpleClassName ) );
    } catch ( JsonProcessingException e ) {
      throw new BindingException( e.getMessage( ) );
    } catch ( ClassNotFoundException e ) {
      throw new BindingException( "Binding not found for target: " + target );
    }

    return message;
  }

  private Optional<String> getCorrelationId( final ChannelEvent event ) {
    return getCorrelationId( event.getChannel( ) );
  }

  private Optional<String> getCorrelationId( final Channel channel ) {
    try {
      final Context context = Contexts.lookup( channel );
      return Optional.fromNullable( context.getCorrelationId( ) );
    } catch ( NoSuchContextException e ) {
      return Optional.absent( );
    }
  }
}
