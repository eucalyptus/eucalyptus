/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

package com.eucalyptus.objectstorage.pipeline;

import java.nio.charset.Charset;

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
import com.eucalyptus.objectstorage.exceptions.s3.SignatureDoesNotMatchException;
import com.eucalyptus.objectstorage.msgs.ObjectStorageErrorMessageType;
import com.eucalyptus.ws.WebServicesException;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.Runnables;

public class ObjectStorageRESTExceptionHandler extends SimpleChannelUpstreamHandler {
  private static final Logger LOG = Logger.getLogger(ObjectStorageRESTExceptionHandler.class);
  private static final String CODE_UNKNOWN = "UNKNOWN";

  private static final Charset UTF_8 = Charset.forName("UTF-8");

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

    final ObjectStorageErrorMessageType errorResponse = new ObjectStorageErrorMessageType( );
    errorResponse.setResource( Strings.nullToEmpty( resource ) );
    errorResponse.setMessage( Strings.nullToEmpty( cause.getMessage() ) );
    errorResponse.setCode( Strings.nullToEmpty( code ) );
    errorResponse.setRequestId( Strings.nullToEmpty( requestId ) );
    errorResponse.setStatus( status );

    if ( cause instanceof InvalidAccessKeyIdException ) {
      errorResponse.setAccessKeyId( ( (InvalidAccessKeyIdException) cause ).getAccessKeyId( ) );
      errorResponse.setResource( null );
    }

    if (cause instanceof SignatureDoesNotMatchException) {
      SignatureDoesNotMatchException ex = (SignatureDoesNotMatchException) cause;
      errorResponse.setAccessKeyId( ex.getAccessKeyId( ) );
      errorResponse.setResource( null );
      errorResponse.setStringToSign( Strings.nullToEmpty( ex.getStringToSign( ) ) );
      errorResponse.setSignatureProvided( Strings.nullToEmpty( ex.getSignatureProvided( ) ) );
      if (ex.getStringToSign() != null)
        errorResponse.setStringToSignBytes( stringToByteString( ex.getStringToSign() ) );
      errorResponse.setCanonicalRequest( Strings.nullToEmpty( ex.getCanonicalRequest( ) ) );
      if (ex.getCanonicalRequest() != null)
        errorResponse.setCanonicalRequestBytes( stringToByteString( ex.getCanonicalRequest() ) );
    }

    if ( ctx.getChannel( ).isConnected( ) ) {
      final MappingHttpResponse response = new MappingHttpResponse( httpVersion );
      response.setStatus( status );
      response.setMessage( errorResponse );
      response.setCorrelationId( requestId );
      errorResponse.setCorrelationId( requestId );
      final ChannelFuture writeFuture = Channels.future(ctx.getChannel());
      writeFuture.addListener(ChannelFutureListener.CLOSE);
      response.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
      Channels.write( ctx, writeFuture, response );
      cleanup.run( );
    } else {
      ctx.sendDownstream( e );
    }
  }
  
  private static String stringToByteString(String in) {
    StringBuilder sb = new StringBuilder();
    byte[] b = in.getBytes(UTF_8);
    for(int i=0; i<b.length; i++)
      sb.append(b[i]).append(" ");
    sb.deleteCharAt(sb.length()-1);
    return sb.toString();
  }
}
