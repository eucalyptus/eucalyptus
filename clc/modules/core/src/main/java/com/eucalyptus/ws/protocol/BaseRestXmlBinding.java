/**
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.ws.protocol;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.binding.HttpContent;
import com.eucalyptus.binding.RestBinding;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Beans;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.UnsafeByteArrayOutputStream;
import com.eucalyptus.ws.WebServiceError;
import com.eucalyptus.ws.handlers.ExceptionMarshallerHandler;
import com.eucalyptus.ws.handlers.RestfulMarshallingHandler;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;
import io.vavr.control.Option;

/**
 *
 */
public abstract class BaseRestXmlBinding<ERROR_RESPONSE extends BaseMessage & WebServiceError> extends RestfulMarshallingHandler implements ExceptionMarshallerHandler {
  private static final Pattern versionPattern = Pattern.compile( "^/(\\d\\d\\d\\d-\\d\\d-\\d\\d)/" );

  private final String pathPrefix;
  private final boolean responseDefaultNs;

  public BaseRestXmlBinding( final String namespacePattern,
                             final String defaultVersion,
                             final String pathPrefix ) {
    this( namespacePattern, defaultVersion, pathPrefix, false );
  }

  public BaseRestXmlBinding( final String namespacePattern,
                             final String defaultVersion,
                             final String pathPrefix,
                             final boolean responseDefaultNs ) {
    super( namespacePattern, defaultVersion );
    this.pathPrefix = pathPrefix;
    this.responseDefaultNs = responseDefaultNs;
  }

  @Override
  protected String getVersionFromRequest( final MappingHttpRequest request ) {
    final String path = request.getServicePath( );
    final String cleanedPath = path.startsWith( pathPrefix ) ? path.substring( pathPrefix.length( ) ) : path;
    final Matcher matcher = versionPattern.matcher( cleanedPath );
    return matcher.matches( ) ? matcher.group( 1 ) : null;
  }

  @Override
  public Object bind( final MappingHttpRequest request ) throws Exception {
    final String path = request.getServicePath( );
    final String cleanedPath = path.startsWith( pathPrefix ) ? path.substring( pathPrefix.length( ) ) : path;
    final Binding binding = getBindingWithHttpRequestClass(
        request.getMethod().getName( ),
        cleanedPath );
    final RestBinding restBinding = binding.getRestBinding( );
    return restBinding.fromRest(
        request.getMethod( ).getName( ),
        cleanedPath,
        request::getHeader,
        request.getParameters( )::get,
        request.getContentAsString( ),
        tm -> binding.fromOM( tm._2() ) );
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
      if ( message instanceof EucalyptusErrorMessageType ) {
        httpResponse.setStatus( HttpResponseStatus.INTERNAL_SERVER_ERROR );
        httpResponse.setMessage( errorResponse(
            correlationId.getOrNull( ),
            "Receiver",
            "InternalFailure",
            ((EucalyptusErrorMessageType)httpResponse.getMessage( )).getMessage() ) );
      } else if ( message instanceof ExceptionResponseType ) { //handle error case specially
        ExceptionResponseType msg = (ExceptionResponseType) httpResponse.getMessage( );
        httpResponse.setStatus( msg.getHttpStatus( ) );
        httpResponse.setMessage( errorResponse(
            correlationId.getOrNull( ),
            "Receiver",
            "InternalFailure",
            ((ExceptionResponseType)httpResponse.getMessage( )).getMessage() ) );
      } else {
        final RestBinding.RestResponse restResponse = RestBinding.toRest( message );
        if (httpResponse.getStatus() == null || httpResponse.getStatus()==HttpResponseStatus.OK) {
          httpResponse.setStatus( HttpResponseStatus.valueOf( restResponse.getStatus( ) ) );
        }
        for ( final Map.Entry<String, String> header : restResponse.getHeaders( ).entrySet( ) ) {
          httpResponse.setHeader( header.getKey( ), header.getValue( ) );
        }
        if ( !restResponse.hasBody( ) ) {
          httpResponse.addHeader( HttpHeaders.Names.CONTENT_LENGTH, "0" );
          return;
        }
      }
    }
    super.outgoingMessage( ctx, event );
  }

  @Nonnull
  @Override
  public ExceptionResponse marshallException(
      @Nonnull final ChannelEvent event,
      @Nonnull final HttpResponseStatus status,
      @Nonnull final Throwable throwable
  ) throws Exception {
    final Map<String,String> headers = Maps.newHashMap();
    headers.put( HttpHeaders.Names.CONTENT_TYPE, "application/json; charset=UTF-8" );
    final Option<String> correlationId = getCorrelationId( event );
    if ( correlationId.isDefined( ) ) {
      headers.put( "x-amzn-requestid", correlationId.get(  ) );
    }
    HttpResponseStatus responseStatus = status;
    String type = "Receiver";
    String code = "InternalFailure"; // code
    String message = throwable.getMessage( );
    if ( Exceptions.isCausedBy( throwable, InvalidAccessKeyAuthException.class ) ) {
      responseStatus = HttpResponseStatus.FORBIDDEN;
      type = "Sender";
      code = "UnrecognizedClientException";
      message = "The security token included in the request is invalid.";
    } else if ( Exceptions.isCausedBy( throwable, InvalidSignatureAuthException.class ) ) {
      responseStatus = HttpResponseStatus.FORBIDDEN;
      type = "Sender";
      code = "InvalidSignatureException";
      message = "The request signature we calculated does not match the signature you provided. Check your AWS Secret Access Key and signing method.";
    } else if ( throwable instanceof BindingException ) {
      responseStatus = HttpResponseStatus.BAD_REQUEST;
      type = "Sender";
      code = "InvalidParameterValue";
    }

    final UnsafeByteArrayOutputStream byteOut = new UnsafeByteArrayOutputStream( 4096 );
    final BaseMessage errorResponse = errorResponse( correlationId.getOrNull( ),  type, code, message );
    getDefaultBinding( ).toStream( byteOut, writeReplace( errorResponse ), getNamespaceOverride( errorResponse, null ) );
    final ChannelBuffer content = ChannelBuffers.wrappedBuffer( byteOut.getBuffer( ), 0, byteOut.getCount( ) );

    return new ExceptionResponse( responseStatus, content, headers );
  }

  protected abstract ERROR_RESPONSE errorResponse(
               String requestId,
      @Nonnull String type,
      @Nonnull String code,
      @Nonnull String message
  );

  @Override
  protected String getNamespaceOverride( @Nonnull final Object message, @Nullable final String namespace ) {
    return responseDefaultNs && !(message instanceof WebServiceError) ? "" : namespace;
  }

  protected Object writeReplace( @Nonnull final Object message ) {
    final Option<HttpContent> httpContentOption = Ats.from( message ).getOption( HttpContent.class );
    if ( httpContentOption.isDefined( ) ) {
      return Beans.getObjectProperty( message, httpContentOption.get( ).payload( ) );
    }
    return super.writeReplace( message );
  }

  protected Binding getBindingWithHttpRequestClass(
      final String method,
      final String path
  ) throws BindingException {
    Binding binding = null;
    if ( this.getBinding( ).getRestBinding( ).hasRestClass( method, path ) ) {
      binding = this.getBinding( );
    } else if ( this.getDefaultBinding() != null &&
        this.getDefaultBinding().getRestBinding().hasRestClass( method, path ) ) {
      binding = this.getDefaultBinding();
    }
    return binding;
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
}
