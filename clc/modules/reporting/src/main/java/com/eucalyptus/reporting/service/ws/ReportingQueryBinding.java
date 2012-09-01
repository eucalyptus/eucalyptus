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
