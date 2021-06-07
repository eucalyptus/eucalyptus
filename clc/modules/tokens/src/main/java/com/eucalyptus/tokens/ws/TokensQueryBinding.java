/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
package com.eucalyptus.tokens.ws;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.tokens.common.msgs.TokensErrorResponseType;
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
