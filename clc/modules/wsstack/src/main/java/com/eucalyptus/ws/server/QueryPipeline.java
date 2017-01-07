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
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.ws.server;

import static com.eucalyptus.auth.principal.TemporaryAccessKey.TemporaryKeyType;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.eucalyptus.ws.Handlers;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.util.Strings;
import com.eucalyptus.ws.stages.HmacUserAuthenticationStage;
import com.eucalyptus.ws.stages.UnrollableStage;
import com.eucalyptus.ws.util.HmacUtils.SignatureVersion;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 *
 */
public abstract class QueryPipeline extends FilteredPipeline {
  private final HmacUserAuthenticationStage auth;
  private final String name;
  private final Set<String> servicePathPrefixes;

  protected QueryPipeline( final String name,
                           final String servicePathPrefix,
                           final Set<TemporaryKeyType> allowedTemporaryCredentials ) {
    this( name,
          ImmutableSet.of( servicePathPrefix ),
          allowedTemporaryCredentials );
  }

  protected QueryPipeline( final String name,
                           final String servicePathPrefix,
                           final Set<TemporaryKeyType> allowedTemporaryCredentials,
                           final Set<SignatureVersion> allowedSignatureVersions ) {
    this( name,
        ImmutableSet.of( servicePathPrefix ),
        allowedTemporaryCredentials,
        allowedSignatureVersions );
  }

  protected QueryPipeline( final String name,
                           final Set<String> servicePathPrefixes,
                           final Set<TemporaryKeyType> allowedTemporaryCredentials ) {
    this(
        name,
        servicePathPrefixes,
        allowedTemporaryCredentials,
        EnumSet.allOf( SignatureVersion.class ) );
  }

  protected QueryPipeline( final String name,
                           final Set<String> servicePathPrefixes,
                           final Set<TemporaryKeyType> allowedTemporaryCredentials,
                           final Set<SignatureVersion> allowedSignatureVersions ) {
    this.auth = new HmacUserAuthenticationStage( allowedTemporaryCredentials, allowedSignatureVersions );
    this.name = name;
    this.servicePathPrefixes = ImmutableSet.copyOf( servicePathPrefixes );
  }

  protected UnrollableStage getAuthenticationStage( ) {
    return auth;
  }

  @Override
  public ChannelPipeline addHandlers( final ChannelPipeline pipeline ) {
    pipeline.addLast( "aggregator", Handlers.newQueryHttpChunkAggregator());
    pipeline.addLast( "parse-query-parameters",new MessageStackHandler( ){
      @Override
      public void incomingMessage( final MessageEvent event ) throws Exception {
        if ( event.getMessage( ) instanceof  MappingHttpRequest ) {
          final MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
          if ( httpRequest.getMethod( ).equals( HttpMethod.POST ) ) {
            final Map<String, String> parameters = new HashMap<String, String>( httpRequest.getParameters( ) );
            final Set<String> nonQueryParameters = Sets.newHashSet( );
            final String query = httpRequest.getContentAsString( true );
            for ( String p : query.split( "&" ) ) {
              String[] splitParam = p.split( "=", 2 );
              String lhs = splitParam[ 0 ];
              String rhs = splitParam.length == 2 ? splitParam[ 1 ] : null;
              try {
                if ( lhs != null ) lhs = new URLCodec( ).decode( lhs );
              } catch ( DecoderException e ) {
              }
              try {
                if ( rhs != null ) rhs = new URLCodec( ).decode( rhs );
              } catch ( DecoderException e ) {
              }
              parameters.put( lhs, rhs );
              nonQueryParameters.add( lhs );
            }
            httpRequest.getParameters( ).putAll( parameters );
            httpRequest.addNonQueryParameterKeys( nonQueryParameters );
          }
        }
      }
    } );
    getAuthenticationStage( ).unrollStage( pipeline );
    return null;
  }

  @Override
  public boolean checkAccepts( final HttpRequest message ) {
    if ( message instanceof MappingHttpRequest && !message.getHeaderNames().contains( "SOAPAction" )) {
      final boolean usesServicePath = Iterables.any( servicePathPrefixes, Strings.isPrefixOf( message.getUri( ) ) );
      final boolean pathValidForService = validPathForService( message.getUri( ) );
      if ( !usesServicePath && !( pathValidForService && resolvesByHost( message.getHeader( HttpHeaders.Names.HOST ) ) ) ) {
        return false;
      }
      return true;
    }  else {
      return false;
    }
  }

  @Override
  public String getName( ) {
    return name;
  }

  /**
   * Is the non-prefixed path one handled by this service.
   *
   * Non-prefixed paths that the service recognises are handled as long as the
   * host is also valid.
   *
   * @param path The non-prefixed path to check.
   * @return True if this this path is valid for the service
   */
  protected boolean validPathForService( final String path ) {
    return path.isEmpty( ) || path.equals( "/" ) || path.startsWith( "/?" );
  }
}
