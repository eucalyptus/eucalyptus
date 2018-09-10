/**
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.ws.server;

import java.util.Set;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import com.eucalyptus.auth.principal.TemporaryAccessKey.TemporaryKeyType;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.util.Strings;
import com.eucalyptus.ws.Handlers;
import com.eucalyptus.ws.stages.HmacUserAuthenticationStage;
import com.eucalyptus.ws.stages.UnrollableStage;
import com.eucalyptus.ws.util.HmacUtils.SignatureVersion;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 *
 */
abstract class RestPipeline extends FilteredPipeline {
  private final HmacUserAuthenticationStage auth;
  private final String name;
  private final Set<String> servicePathPrefixes;

  protected RestPipeline( final String name,
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
    getAuthenticationStage( ).unrollStage( pipeline );
    return null;
  }

  @Override
  public boolean checkAccepts( final HttpRequest message ) {
    if ( message instanceof MappingHttpRequest && !message.getHeaderNames().contains( "SOAPAction" )) {
      final boolean usesServicePath = Iterables.any( servicePathPrefixes, Strings.isPrefixOf( message.getUri( ) ) );
      final boolean pathValidForService = validPathForService( message.getUri( ) );
      final boolean validAuthForPipeline = validAuthForPipeline( (MappingHttpRequest)message );
      if ( !validAuthForPipeline || (
          !usesServicePath && !( pathValidForService && resolvesByHost( message.getHeader( HttpHeaders.Names.HOST ) ) )
      ) ) {
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
    return true;
  }

  /**
   * Does the given request use a valid authentication method for this pipeline.
   */
  protected boolean validAuthForPipeline( final MappingHttpRequest request ) {
    return true;
  }
}
