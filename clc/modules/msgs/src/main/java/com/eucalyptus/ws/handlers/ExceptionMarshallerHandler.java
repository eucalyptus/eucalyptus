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
