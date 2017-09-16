/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.ws.protocol;

import static com.eucalyptus.util.Json.JsonOption.IgnoreBaseMessage;
import static com.eucalyptus.util.Json.JsonOption.IgnoreGroovy;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
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
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Json;
import com.eucalyptus.util.UnsafeByteArrayOutputStream;
import com.eucalyptus.ws.handlers.ExceptionMarshallerHandler;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;
import javaslang.control.Option;

/**
 *
 */
public class BaseQueryJsonBinding<T extends Enum<T>> extends BaseQueryBinding<T> implements ExceptionMarshallerHandler {

  private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/x-amz-json-1.0";

  private static final String CODE_ERROR = "InternalFailure";

  private static final String HEADER_REQUEST_ID = "x-amzn-RequestId";

  private static final ObjectWriter writer = Json.mapper( EnumSet.of( IgnoreBaseMessage, IgnoreGroovy ) ).writer( );

  private final Class<? extends BaseMessage> messageType;
  private final String responseContentType;

  @SuppressWarnings( "unchecked" )
  public BaseQueryJsonBinding(
      final Class<? extends BaseMessage> messageType,
      final Option<String> responseContentType,
      final UnknownParameterStrategy unknownParameterStrategy,
      final T operationParam,
      final T... alternativeOperationParam
  ) {
    super(
        "http://www.eucalyptus.com/ns/msgs/2017-05-04/",
        null,
        unknownParameterStrategy,
        operationParam,
        alternativeOperationParam
    );
    this.messageType = messageType;
    this.responseContentType = responseContentType.getOrElse( DEFAULT_RESPONSE_CONTENT_TYPE );
  }

  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpResponse ) {
      Option<String> correlationId = getCorrelationId( event );
      MappingHttpResponse httpResponse = ( MappingHttpResponse ) event.getMessage( );
      UnsafeByteArrayOutputStream byteOut = new UnsafeByteArrayOutputStream( 8192 );
      if ( httpResponse.getMessage( ) instanceof EucalyptusErrorMessageType ) {
        httpResponse.setStatus( HttpResponseStatus.BAD_REQUEST );
        writer.writeValue( byteOut, ImmutableMap.of(
            "__type", CODE_ERROR,
            "message", ((EucalyptusErrorMessageType)httpResponse.getMessage( )).getMessage() ) );
      } else if ( httpResponse.getMessage( ) instanceof ExceptionResponseType ) { //handle error case specially
        ExceptionResponseType msg = ( ExceptionResponseType ) httpResponse.getMessage( );
        httpResponse.setStatus( msg.getHttpStatus( ) );
        writer.writeValue( byteOut, ImmutableMap.of(
            "__type", CODE_ERROR,
            "message", ((ExceptionResponseType)httpResponse.getMessage( )).getMessage() ) );
      } else if ( httpResponse.getMessage( ) != null ){ //actually try to bind response
        final Object message = httpResponse.getMessage( );
        if ( shouldWrite( message ) ) {
          correlationId = Option.some( ( (BaseMessage) message ).getCorrelationId( ) );
          writeMessage( byteOut, (BaseMessage) message );
        }
      }
      ChannelBuffer buffer = ChannelBuffers.wrappedBuffer( byteOut.getBuffer( ), 0, byteOut.getCount( ) );
      httpResponse.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes( ) ) );
      httpResponse.addHeader( HttpHeaders.Names.CONTENT_TYPE, responseContentType );
      if ( correlationId.isDefined( ) ) {
        httpResponse.addHeader( HEADER_REQUEST_ID, correlationId.get( ) );
      }
      httpResponse.setContent( buffer );
    }
  }

  @Nonnull
  @Override
  public ExceptionResponse marshallException( @Nonnull ChannelEvent event,
                                              @Nonnull final HttpResponseStatus status,
                                              @Nonnull final Throwable throwable ) throws Exception {
    final Map<String,String> headers = Maps.newHashMap();
    headers.put( HttpHeaders.Names.CONTENT_TYPE, responseContentType );
    final Option<String> correlationId = getCorrelationId( event );
    if ( correlationId.isDefined( ) ) {
      headers.put( HEADER_REQUEST_ID, correlationId.get(  ) );
    }
    final ExceptionResponseDetail details = getExceptionResponseDetail( throwable ).getOrElse(
        ExceptionResponseDetail.of( status, CODE_ERROR, Objects.toString( throwable.getMessage( ), "" ) )
    );
    final UnsafeByteArrayOutputStream byteOut = new UnsafeByteArrayOutputStream( 8192 );
    writer.writeValue( byteOut, ImmutableMap.of( "__type", details.getType( ), "message", details.getMessage( ) ) );

    return new ExceptionResponse(
        details.getStatus( ),
        ChannelBuffers.wrappedBuffer( byteOut.getBuffer( ), 0, byteOut.getCount( ) ),
        ImmutableMap.copyOf( headers )
    );
  }

  public String marshall( final BaseMessage message ) throws IOException {
    final UnsafeByteArrayOutputStream byteOut = new UnsafeByteArrayOutputStream( 4096 );
    if ( shouldWrite( message ) ) {
      writer.writeValue( byteOut, message );
    }
    return new String( byteOut.getBuffer( ), 0, byteOut.getCount(), StandardCharsets.UTF_8 );
  }

  protected final boolean shouldWrite( final Object message ) {
    return messageType.isInstance( message );
  }

  protected final void writeMessage( final OutputStream out, final BaseMessage message ) throws IOException {
    writer.writeValue( out, message );
  }

  protected final Option<ExceptionResponseDetail> details( HttpResponseStatus status, String type, String message ) {
    return Option.of( ExceptionResponseDetail.of( status, type, message ) );
  }

  protected Option<ExceptionResponseDetail> getExceptionResponseDetail( final Throwable throwable ) {
    Option<ExceptionResponseDetail> details = Option.none( );
    if ( Exceptions.isCausedBy( throwable, InvalidAccessKeyAuthException.class ) ) {
      details = details(
          HttpResponseStatus.FORBIDDEN,
          "InvalidClientTokenId",
          "The security token included in the request is invalid."
      );
    } else if ( Exceptions.isCausedBy( throwable, InvalidSignatureAuthException.class ) ) {
      details = details(
          HttpResponseStatus.FORBIDDEN,
          "InvalidSignatureException",
          "The request signature we calculated does not match the signature you provided. Check your AWS Secret Access Key and signing method."
      );
    } else if ( throwable instanceof BindingException ) {
      details = details(
          HttpResponseStatus.BAD_REQUEST,
          "InvalidParameterValue",
          Objects.toString( throwable.getMessage( ), "" )
      );
    }
    return details;
  }


  private Option<String> getCorrelationId( final ChannelEvent event ) {
    return getCorrelationId( event.getChannel( ) );
  }

  private Option<String> getCorrelationId( final Channel channel ) {
    try {
      final Context context = Contexts.lookup( channel );
      return Option.of( context.getCorrelationId( ) );
    } catch ( NoSuchContextException e ) {
      return Option.none( );
    }
  }

  public static final class ExceptionResponseDetail {
    private final HttpResponseStatus status;
    private final String type;
    private final String message;

    private ExceptionResponseDetail( final HttpResponseStatus status, final String type, final String message ) {
      this.status = status;
      this.type = type;
      this.message = message;
    }

    public static ExceptionResponseDetail of(
        final HttpResponseStatus status,
        final String type,
        final String message
    ) {
      return new ExceptionResponseDetail( status, type, message );
    }

    public HttpResponseStatus getStatus( ) {
      return status;
    }

    public String getType( ) {
      return type;
    }

    public String getMessage( ) {
      return message;
    }
  }
}
