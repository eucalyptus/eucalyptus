/**
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.ws.protocol;

import static com.eucalyptus.util.Json.JsonOption.IgnoreBaseMessageUpper;
import static com.eucalyptus.util.Json.JsonOption.OmitNullValues;
import static com.eucalyptus.util.Json.JsonOption.UpperCamelPropertyNaming;
import java.util.EnumSet;
import java.util.Map;
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
import com.eucalyptus.binding.HttpContent;
import com.eucalyptus.binding.RestBinding;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Beans;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Json;
import com.eucalyptus.util.UnsafeByteArrayOutputStream;
import com.eucalyptus.ws.handlers.ExceptionMarshallerHandler;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;
import io.vavr.control.Option;

/**
 *
 */
public abstract class BaseRestJsonBinding extends MessageStackHandler implements ExceptionMarshallerHandler {

  private static final ObjectMapper mapper =
      Json.mapper( EnumSet.of( IgnoreBaseMessageUpper, OmitNullValues, UpperCamelPropertyNaming ) )
          .addMixIn( EucalyptusData.class, MemberMixin.class );
  private static final ObjectReader reader = mapper.reader( );
  private static final ObjectWriter writer = mapper.writer( );

  private final String pathPrefix;

  public BaseRestJsonBinding( final String pathPrefix ) {
    this.pathPrefix = pathPrefix;
  }

  @Override
  public void incomingMessage( MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpRequest ) {
      final MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
      try {
        final BaseMessage msg = ( BaseMessage ) this.bind( httpRequest );
        httpRequest.setMessage( msg );
      } catch ( Exception e ) {
        if ( !( e instanceof BindingException ) ) {
          e = new BindingException( e );
        }
        throw e;
      }
    }
  }

  private Object bind( final MappingHttpRequest request ) throws Exception {
    final String path = request.getServicePath( );
    final String cleanedPath = path.startsWith( pathPrefix ) ? path.substring( pathPrefix.length( ) ) : path;
    final RestBinding restBinding = BindingManager.getRestBinding( getComponentClass( ) );
    if (!restBinding.hasRestClass( request.getMethod().getName( ),  cleanedPath )) {
      throw new BindingException( "Invalid request" );
    }
    return restBinding.fromRest(
        request.getMethod( ).getName( ),
        cleanedPath,
        request::getHeader,
        request.getParameters( )::get,
        request.getContentAsString( ),
        tm -> Json.readObject( reader, tm._1( ), tm._2( ) ) );
  }

  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpResponse ) {
      final MappingHttpResponse httpResponse = (MappingHttpResponse) event.getMessage( );
      final Object message = httpResponse.getMessage( );
      final Option<String> correlationId = getCorrelationId( event );
      if ( correlationId.isDefined( ) ) {
        httpResponse.addHeader( "x-amzn-requestid", correlationId.get( ) );
      }
      final UnsafeByteArrayOutputStream byteOut = new UnsafeByteArrayOutputStream( 8192 );
      if ( message instanceof EucalyptusErrorMessageType ) {
        httpResponse.setStatus( HttpResponseStatus.INTERNAL_SERVER_ERROR );
        httpResponse.addHeader( "x-amzn-errortype", "InternalFailure" );
        Json.writeObject( byteOut, ImmutableMap.of(
            "message", ((EucalyptusErrorMessageType)httpResponse.getMessage( )).getMessage() ) );
      } else if ( message instanceof ExceptionResponseType ) { //handle error case specially
        ExceptionResponseType msg = ( ExceptionResponseType ) httpResponse.getMessage( );
        httpResponse.setStatus( msg.getHttpStatus( ) );
        httpResponse.addHeader( "x-amzn-errortype", "InternalFailure" );
        Json.writeObject( byteOut, ImmutableMap.of(
            "message", ((ExceptionResponseType)httpResponse.getMessage( )).getMessage() ) );
      } else {
        final RestBinding.RestResponse restResponse = RestBinding.toRest( message );
        httpResponse.setStatus( HttpResponseStatus.valueOf( restResponse.getStatus( ) ) );
        for ( final Map.Entry<String,String> header : restResponse.getHeaders( ).entrySet( ) ) {
          httpResponse.setHeader( header.getKey( ), header.getValue( ) );
        }
        if ( !restResponse.hasBody( ) ) {
          httpResponse.addHeader( HttpHeaders.Names.CONTENT_LENGTH, "0" );
          return;
        }
        writer.writeValue( byteOut, writeReplace( message ) );
      }
      final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer( byteOut.getBuffer( ), 0, byteOut.getCount( ) );
      httpResponse.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes( ) ) );
      httpResponse.addHeader( HttpHeaders.Names.CONTENT_TYPE, "application/json; charset=UTF-8" );
      httpResponse.setContent( buffer );
    }
  }

  @Nonnull
  @Override
  public ExceptionResponse marshallException(
      @Nonnull final ChannelEvent event,
      @Nonnull final HttpResponseStatus status,
      @Nonnull final Throwable throwable
  ) throws Exception {
    HttpResponseStatus responseStatus = status;
    final Map<String,String> headers = Maps.newHashMap();
    headers.put( HttpHeaders.Names.CONTENT_TYPE, "application/json; charset=UTF-8" );
    final Option<String> correlationId = getCorrelationId( event );
    if ( correlationId.isDefined( ) ) {
      headers.put( "x-amzn-requestid", correlationId.get(  ) );
    }

    String type = "InternalFailure"; // code
    String message = throwable.getMessage( );
    if ( Exceptions.isCausedBy( throwable, InvalidAccessKeyAuthException.class ) ) {
      responseStatus = HttpResponseStatus.FORBIDDEN;
      type = "UnrecognizedClientException";
      message = "The security token included in the request is invalid.";
    } else if ( Exceptions.isCausedBy( throwable, InvalidSignatureAuthException.class ) ) {
      responseStatus = HttpResponseStatus.FORBIDDEN;
      type = "InvalidSignatureException";
      message = "The request signature we calculated does not match the signature you provided. Check your AWS Secret Access Key and signing method.";
    } else if ( throwable instanceof BindingException ) {
      responseStatus = HttpResponseStatus.BAD_REQUEST;
      type = "InvalidParameterValue";
    }
    headers.put( "x-amzn-errortype", type );

    final UnsafeByteArrayOutputStream byteOut = new UnsafeByteArrayOutputStream( 4096 );
    Json.writeObject( byteOut, ImmutableMap.of("message", message ) );

    return new ExceptionResponse(
        responseStatus,
        ChannelBuffers.wrappedBuffer( byteOut.getBuffer( ), 0, byteOut.getCount( ) ),
        ImmutableMap.copyOf( headers )
    );
  }

  protected Object writeReplace( @Nonnull final Object message ) {
    final Option<HttpContent> httpContentOption = Ats.from( message ).getOption( HttpContent.class );
    if ( httpContentOption.isDefined( ) ) {
      return Beans.getObjectProperty( message, httpContentOption.get( ).payload( ) );
    }
    return message;
  }

  protected Class<? extends ComponentId> getComponentClass( ) {
    final ComponentPart componentPart = Ats.from( getClass( ) ).get( ComponentPart.class );
    if ( componentPart == null ) {
      throw new IllegalArgumentException( "BaseRestJsonBinding must have @ComponentPart annotation: " + getClass( ) );
    }
    return componentPart.value( );
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

  private interface MemberMixin {
    @JsonAnyGetter
    Map<String,Object> getMapping( );

    @JsonAnySetter
    void setMapping( String key, String value );

    @JsonAnySetter
    void setMapping( String key, Object value );
  }
}
