/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

package com.eucalyptus.objectstorage.pipeline;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.objectstorage.exceptions.ObjectStorageException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidAccessKeyIdException;
import com.eucalyptus.objectstorage.msgs.ObjectStorageErrorMessageType;
import com.eucalyptus.ws.WebServicesException;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.Runnables;

public class ObjectStorageRESTExceptionHandler extends SimpleChannelUpstreamHandler {
  private static final Logger LOG = Logger.getLogger(ObjectStorageRESTExceptionHandler.class);
  private static final String CODE_UNKNOWN = "UNKNOWN";

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) throws Exception {
    final Channel ch = e.getChannel();
    Throwable cause = e.getCause();
    if (cause.getCause() != null) {
      // wrapped exception
      cause = cause.getCause();
    }

    // Get the request ID from the context and clear the context. If you cant log an exception and move on
    String requestId = null;
    HttpVersion httpVersion = HttpVersion.HTTP_1_0;
    Runnable cleanup = Runnables.doNothing( );
    try {
      if (ch != null) {
        Context context = Contexts.lookup(ch);
        requestId = context.getCorrelationId();
        MappingHttpRequest request = context.getHttpRequest( );
        httpVersion = request != null ? request.getProtocolVersion( ) : httpVersion;
        cleanup = () -> Contexts.clear( context );
      }
    } catch (Exception ex) {
      LOG.trace("Error getting request ID or clearing context", ex);
    }

    // Populate the error response fields
    final HttpResponseStatus status;
    final String code;
    final String resource;
    final String message;
    if (cause instanceof ObjectStorageException) {
      ObjectStorageException walrusEx = (ObjectStorageException) cause;
      status = walrusEx.getStatus();
      code = walrusEx.getCode();
      resource = walrusEx.getResource();
    } else if (cause instanceof WebServicesException) {
      WebServicesException webEx = (WebServicesException) cause;
      status = webEx.getStatus();
      code = CODE_UNKNOWN;
      resource = null;
    } else {
      status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
      code = CODE_UNKNOWN;
      resource = null;
    }
    message = cause.getMessage();

    final ObjectStorageErrorMessageType errorResponse = new ObjectStorageErrorMessageType( );
    errorResponse.setResource( Strings.nullToEmpty( resource ) );
    errorResponse.setMessage( Strings.nullToEmpty( message ) );
    errorResponse.setCode( Strings.nullToEmpty( code ) );
    errorResponse.setRequestId( Strings.nullToEmpty( requestId ) );
    errorResponse.setStatus( status );

    if ( cause instanceof InvalidAccessKeyIdException ) {
      errorResponse.setAccessKeyId( ( (InvalidAccessKeyIdException) cause ).getAccessKeyId( ) );
      errorResponse.setResource( null );
    }

    if ( ctx.getChannel( ).isConnected( ) ) {
      final MappingHttpResponse response = new MappingHttpResponse( httpVersion );
      response.setStatus( status );
      response.setMessage( errorResponse );
      response.setCorrelationId( requestId );
      errorResponse.setCorrelationId( requestId );
      final ChannelFuture writeFuture = Channels.future(ctx.getChannel());
      writeFuture.addListener(ChannelFutureListener.CLOSE);
      response.addHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
      Channels.write( ctx, writeFuture, response );
      cleanup.run( );
    } else {
      ctx.sendDownstream( e );
    }
  }
}
