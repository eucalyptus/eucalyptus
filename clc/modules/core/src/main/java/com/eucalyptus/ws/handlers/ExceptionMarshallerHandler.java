/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.ws.handlers;

import static org.hamcrest.Matchers.notNullValue;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.util.Parameters;
import com.google.common.base.Optional;

/**
 *
 */
public interface ExceptionMarshallerHandler extends ChannelHandler {

  /**
   * Marshall an exception into a binding appropriate format.
   *
   * @param event The event related to the exception
   * @param status The rest HTTP status
   * @param throwable The throwable to marshall
   * @return The result
   * @throws Exception if an error occurs while marshalling
   */
  @Nonnull
  ExceptionResponse marshallException( @Nonnull ChannelEvent event,
                                       @Nonnull HttpResponseStatus status,
                                       @Nonnull Throwable throwable ) throws Exception;

  static class ExceptionResponse {
    private final HttpResponseStatus status;
    private final ChannelBuffer content;
    private final Map<String,String> headers;

    public ExceptionResponse( @Nonnull final HttpResponseStatus status,
                              @Nonnull final ChannelBuffer content,
                              @Nonnull final Map<String,String> headers ) {
      Parameters.checkParam( "status", status, notNullValue( ) );
      Parameters.checkParam( "content", content, notNullValue( ) );
      Parameters.checkParam( "headers", headers, notNullValue( ) );
      this.status = status;
      this.content = content;
      this.headers = headers;
    }

    @Nonnull
    public HttpResponseStatus getStatus( ) {
      return status;
    }

    @Nonnull
    public ChannelBuffer getContent( ) {
      return content;
    }

    public Map<String, String> getHeaders( ) {
      return headers;
    }
  }
}
