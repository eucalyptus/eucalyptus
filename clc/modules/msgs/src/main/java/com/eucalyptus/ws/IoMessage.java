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
package com.eucalyptus.ws;

import java.net.URI;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import com.eucalyptus.util.Parameters;
import com.google.common.base.MoreObjects;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;

/**
 *
 */
public class IoMessage<HT extends FullHttpMessage> {

  private String correlationId;
  private SOAPEnvelope soapEnvelope;
  private OMElement omMessage;
  private Object message;
  private HT httpMessage;

  private IoMessage( ) {
  }

  /**
   * Create a new message for outbound http use.
   *
   * @param endpoint The http endpoint
   * @param message The message to be sent
   * @return The new message
   */
  @Nonnull
  public static IoMessage<FullHttpRequest> httpRequest(
      @Nonnull final URI endpoint,
      @Nonnull final BaseMessage message
  ) {
    Parameters.checkParamNotNull( "endpoint", endpoint );
    Parameters.checkParamNotNull( "message", message );
    final FullHttpRequest httpRequest =
        new DefaultFullHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, endpoint.getPath() );
    httpRequest.headers( ).add( HttpHeaders.Names.HOST, endpoint.getHost( ) + ":" + endpoint.getPort( ) );
    final IoMessage<FullHttpRequest> ioMessage = new IoMessage<>( );
    ioMessage.setHttpMessage( httpRequest );
    ioMessage.setMessage( message );
    ioMessage.setCorrelationId( message.getCorrelationId( ) );
    return ioMessage;
  }

  /**
   * Create a new message from an existing http message.
   *
   * @param httpMessage The http message to use
   * @param <HT> The type of the http message
   * @return The new message
   */
  @Nonnull
  public static <HT extends FullHttpMessage> IoMessage<HT> http(
      @Nonnull HT httpMessage
  ) {
    final IoMessage<HT> ioMessage = new IoMessage<>( );
    ioMessage.setHttpMessage( httpMessage );
    return ioMessage;
  }

  @Nonnull
  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId( @Nonnull final String correlationId ) {
    this.correlationId = Parameters.checkParamNotNull( "correlationId", correlationId );
  }

  @Nullable
  public Object getMessage() {
    return message;
  }

  public void setMessage( final Object message ) {
    this.message = message;
  }

  @Nonnull
  public HT getHttpMessage() {
    return httpMessage;
  }

  public void setHttpMessage( @Nonnull  final HT httpMessage ) {
    this.httpMessage = Parameters.checkParamNotNull( "httpMessage", httpMessage );
  }

  @Nullable
  public SOAPEnvelope getSoapEnvelope() {
    return soapEnvelope;
  }

  public void setSoapEnvelope( final SOAPEnvelope soapEnvelope ) {
    this.soapEnvelope = soapEnvelope;
  }

  @Nullable
  public OMElement getOmMessage() {
    return omMessage;
  }

  public void setOmMessage( final OMElement omMessage ) {
    this.omMessage = omMessage;
  }

  public boolean isRequest( ) {
    return getHttpMessage( ) instanceof HttpRequest;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "correlationId", getCorrelationId( ) )
        .toString( );
  }
}
