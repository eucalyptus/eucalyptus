/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
 * Please contact Eucalyptus Systems, Inc., 6750 Navigator Way, Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.ws.handlers.http;

import java.lang.Exception;
import java.lang.Override;
import java.util.Date;
import java.util.Optional;

import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.ws.StackConfiguration;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;


public class HttpResponseHeaderHandler extends MessageStackHandler {

  /**
   * This is for adding default/standard headers to *outbound* HTTP Responses.
   */
  @Override
  public void outgoingMessage(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
    if (event.getMessage() instanceof HttpResponse) {
      HttpResponse resp = (HttpResponse) event.getMessage();

      // Add headers

      // If previous handler has already set the Date header, leave it as is.
      if (!resp.containsHeader(HttpHeaders.Names.DATE)) {
        resp.setHeader(HttpHeaders.Names.DATE, Timestamps.formatRfc822Timestamp(new Date()));
      }

      // If server already set then do not overwrite
      final Optional<String> header = StackConfiguration.getServerHeader( );
      if ( !resp.containsHeader(HttpHeaders.Names.SERVER) && header.isPresent( ) ) {
        resp.setHeader( HttpHeaders.Names.SERVER, header.get( ) );
      }
    }
  }
}
