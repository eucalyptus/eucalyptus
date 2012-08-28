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

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.id.Reporting;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.ws.protocol.RequiredQueryParams;
import com.eucalyptus.ws.server.FilteredPipeline;
import com.eucalyptus.ws.stages.HmacUserAuthenticationStage;

/**
 * TODO:STEVE: Change hierarchy to extend new QueryPipeline class when Tokens changes merged to testing
 */
@ComponentId.ComponentPart(Reporting.class)
public class ReportingQueryPipeline extends FilteredPipeline {
  private final HmacUserAuthenticationStage auth = new HmacUserAuthenticationStage( );

  @Override
  public ChannelPipeline addHandlers( ChannelPipeline pipeline ) {
    auth.unrollStage( pipeline );
    pipeline.addLast( "reporting-query-binding", new ReportingQueryBinding( ) );
    return null;
  }

  @Override
  public boolean checkAccepts( HttpRequest message ) {
    if ( message instanceof MappingHttpRequest) {
      MappingHttpRequest httpRequest = ( MappingHttpRequest ) message;
      if ( httpRequest.getMethod( ).equals( HttpMethod.POST ) ) {
        Map<String,String> parameters = new HashMap<String,String>( httpRequest.getParameters( ) );
        ChannelBuffer buffer = httpRequest.getContent( );
        buffer.markReaderIndex( );
        byte[] read = new byte[buffer.readableBytes( )];
        buffer.readBytes( read );
        String query = new String( read );
        buffer.resetReaderIndex( );
        for ( String p : query.split( "&" ) ) {
          String[] splitParam = p.split( "=" );
          String lhs = splitParam[0];
          String rhs = splitParam.length == 2 ? splitParam[1] : null;
          try {
            if( lhs != null ) lhs = new URLCodec().decode(lhs);
          } catch ( DecoderException e ) {}
          try {
            if( rhs != null ) rhs = new URLCodec().decode(rhs);
          } catch ( DecoderException e ) {}
          parameters.put( lhs, rhs );
        }
        for ( RequiredQueryParams p : RequiredQueryParams.values( ) ) {
          if ( !parameters.containsKey( p.toString( ) ) ) {
            return false;
          }
        }
        httpRequest.getParameters( ).putAll( parameters );
      } else {
        for ( RequiredQueryParams p : RequiredQueryParams.values( ) ) {
          if ( !httpRequest.getParameters( ).containsKey( p.toString( ) ) ) {
            return false;
          }
        }
      }
      return message.getUri( ).startsWith( "/services/Reporting" );
    }
    return false;
  }

  @Override
  public String getName( ) {
    return "reporting-query-pipeline";
  }
}
