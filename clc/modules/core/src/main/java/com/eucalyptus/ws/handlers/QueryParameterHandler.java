/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.ws.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import com.eucalyptus.http.MappingHttpRequest;
import com.google.common.collect.Sets;

/**
 *
 */
@ChannelHandler.Sharable
public class QueryParameterHandler extends MessageStackHandler {

  @Override
  public void incomingMessage( final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpRequest) {
      final MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
      if ( httpRequest.getParameters().isEmpty() &&
          httpRequest.getMethod( ).equals( HttpMethod.POST ) &&
          ( !httpRequest.containsHeader(HttpHeaders.Names.CONTENT_ENCODING) ||
              HttpHeaders.Values.IDENTITY.equals(httpRequest.getHeader(HttpHeaders.Names.CONTENT_ENCODING)) ) ) {
        final Map<String, String> parameters = new HashMap<>( httpRequest.getParameters( ) );
        final Set<String> nonQueryParameters = Sets.newHashSet( );
        final String query = httpRequest.getContentAsString( true );
        for ( String p : query.split( "&" ) ) {
          String[] splitParam = p.split( "=", 2 );
          String lhs = splitParam[ 0 ];
          String rhs = splitParam.length == 2 ? splitParam[ 1 ] : null;
          try {
            if ( lhs != null ) lhs = new URLCodec( ).decode( lhs );
          } catch ( DecoderException ignore ) {
          }
          try {
            if ( rhs != null ) rhs = new URLCodec( ).decode( rhs );
          } catch ( DecoderException ignore ) {
          }
          parameters.put( lhs, rhs );
          nonQueryParameters.add( lhs );
        }
        httpRequest.getParameters( ).putAll( parameters );
        httpRequest.addNonQueryParameterKeys( nonQueryParameters );
      }
    }
  }
}
