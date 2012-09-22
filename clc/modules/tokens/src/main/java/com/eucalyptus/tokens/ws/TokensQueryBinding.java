/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
package com.eucalyptus.tokens.ws;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.tokens.TokensErrorResponseType;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.ws.protocol.BaseQueryBinding;
import com.eucalyptus.ws.protocol.OperationParameter;

/**
 *
 */
public class TokensQueryBinding extends BaseQueryBinding<OperationParameter> {

  static final String STS_NAMESPACE_PATTERN = "https://sts.amazonaws.com/doc/%s/";
  static final String STS_DEFAULT_VERSION = "2011-06-15";
  static final String STS_DEFAULT_NAMESPACE = String.format( STS_NAMESPACE_PATTERN, STS_DEFAULT_VERSION );

  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx,
                               final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpResponse &&
        ( ( MappingHttpResponse ) event.getMessage( ) ).getMessage( ) instanceof TokensErrorResponseType) {
      final MappingHttpResponse httpResponse = ( MappingHttpResponse )event.getMessage( );
      final TokensErrorResponseType errorResponse = (TokensErrorResponseType) httpResponse.getMessage( );
      httpResponse.setStatus( errorResponse.getHttpStatus( ) );
    }

    super.outgoingMessage( ctx, event );
  }

  public TokensQueryBinding() {
    super( STS_NAMESPACE_PATTERN, STS_DEFAULT_VERSION, OperationParameter.Action );
  }

}
