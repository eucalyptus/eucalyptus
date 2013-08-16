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
package com.eucalyptus.ws.server;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.ws.protocol.RequiredQueryParams;
import com.eucalyptus.ws.stages.HmacUserAuthenticationStage;
import com.eucalyptus.ws.stages.UnrollableStage;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 *
 */
public abstract class QueryPipeline extends FilteredPipeline {
  private final HmacUserAuthenticationStage auth;
  private final String name;
  private final String servicePathPrefix;
  private final Set<RequiredQueryParams> requiredQueryParams;

  protected QueryPipeline( final String name,
                           final String servicePathPrefix,
                           final boolean allowTemporaryCredentials) {
    this( name,
          servicePathPrefix,
          allowTemporaryCredentials,
          EnumSet.allOf( RequiredQueryParams.class ) );
  }

  protected QueryPipeline( final String name,
                           final String servicePathPrefix,
                           final boolean allowTemporaryCredentials,
                           final Set<RequiredQueryParams> requiredQueryParams ) {
    this.auth = new HmacUserAuthenticationStage( allowTemporaryCredentials );
    this.name = name;
    this.servicePathPrefix = servicePathPrefix;
    this.requiredQueryParams = ImmutableSet.copyOf( requiredQueryParams );
  }

  protected UnrollableStage getAuthenticationStage( ) {
    return auth;
  }

  @Override
  public ChannelPipeline addHandlers( final ChannelPipeline pipeline ) {
    getAuthenticationStage( ).unrollStage( pipeline );
    return null;
  }

  @Override
  public boolean checkAccepts( final HttpRequest message ) {
    if ( message instanceof MappingHttpRequest) {
      MappingHttpRequest httpRequest = ( MappingHttpRequest ) message;
      if ( httpRequest.getMethod( ).equals( HttpMethod.POST ) ) {
        Map<String,String> parameters = new HashMap<String,String>( httpRequest.getParameters( ) );
        Set<String> nonQueryParameters = Sets.newHashSet();
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
          nonQueryParameters.add( lhs );
        }
        for ( RequiredQueryParams p : requiredQueryParams ) {
          if ( !parameters.containsKey( p.toString( ) ) ) {
            return false;
          }
        }
        httpRequest.getParameters( ).putAll( parameters );
        httpRequest.addNonQueryParameterKeys( nonQueryParameters );
      } else {
        for ( RequiredQueryParams p : requiredQueryParams ) {
          if ( !httpRequest.getParameters( ).containsKey( p.toString( ) ) ) {
            return false;
          }
        }
      }
      final boolean usesServicePath = message.getUri( ).startsWith( servicePathPrefix );
      final boolean noPath = message.getUri( ).isEmpty( ) || message.getUri( ).equals( "/" );
      return
          usesServicePath ||
          ( noPath && resolvesByHost( message.getHeader( HttpHeaders.Names.HOST ) ) );
    }
    return false;
  }

  @Override
  public String getName( ) {
    return name;
  }
}
