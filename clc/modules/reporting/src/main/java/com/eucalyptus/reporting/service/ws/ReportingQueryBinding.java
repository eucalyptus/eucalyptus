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
package com.eucalyptus.reporting.service.ws;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.reporting.service.ReportingErrorResponseType;
import com.eucalyptus.ws.protocol.BaseQueryBinding;
import com.eucalyptus.ws.protocol.OperationParameter;

/**
 *
 */
public class ReportingQueryBinding extends BaseQueryBinding<OperationParameter> {

  static final String REPORTING_NAMESPACE_PATTERN = "http://www.eucalyptus.com/ns/reporting/%s/";
  static final String REPORTING_DEFAULT_VERSION = "2012-08-24";
  static final String REPORTING_DEFAULT_NAMESPACE = String.format( REPORTING_NAMESPACE_PATTERN, REPORTING_DEFAULT_VERSION );

  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx,
                               final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpResponse &&
        ( ( MappingHttpResponse ) event.getMessage( ) ).getMessage( ) instanceof ReportingErrorResponseType) {
      final MappingHttpResponse httpResponse = ( MappingHttpResponse )event.getMessage( );
      final ReportingErrorResponseType errorResponse = (ReportingErrorResponseType) httpResponse.getMessage( );
      httpResponse.setStatus( errorResponse.getHttpStatus( ) );
    }

    super.outgoingMessage( ctx, event );
  }

  public ReportingQueryBinding() {
    super( REPORTING_NAMESPACE_PATTERN, REPORTING_DEFAULT_VERSION, OperationParameter.Action );
  }
}
