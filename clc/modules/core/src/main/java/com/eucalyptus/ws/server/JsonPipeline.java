/**
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
 * Pipeline support for JSON v1.1 services
 */
public class JsonPipeline extends FilteredPipeline {
  private final HmacUserAuthenticationStage auth;
  private final String name;
  private final String targetPrefix;
  private final Set<String> servicePathPrefixes;

  protected JsonPipeline( final String name,
                          final String targetPrefix,
                          final Set<String> servicePathPrefixes,
                          final Set<TemporaryKeyType> allowedTemporaryCredentials,
                          final Set<SignatureVersion> allowedSignatureVersions ) {
    this.auth = new HmacUserAuthenticationStage( allowedTemporaryCredentials, allowedSignatureVersions );
    this.name = name;
    this.targetPrefix = targetPrefix + ".";
    this.servicePathPrefixes = ImmutableSet.copyOf( servicePathPrefixes );
  }

  protected UnrollableStage getAuthenticationStage( ) {
    return auth;
  }

  @Override
  public String getName( ) {
    return name;
  }

  @Override
  public ChannelPipeline addHandlers( final ChannelPipeline pipeline ) {
    pipeline.addLast( "aggregator", Handlers.newQueryHttpChunkAggregator());
    getAuthenticationStage( ).unrollStage( pipeline );
    return pipeline;
  }

  @Override
  public boolean checkAccepts( final HttpRequest message ) {
    if ( message instanceof MappingHttpRequest ) {
      final boolean targetMatch = Strings.startsWith( targetPrefix ).apply( message.getHeader( "X-Amz-Target" ) );
      final boolean usesServicePath = Iterables.any( servicePathPrefixes, Strings.isPrefixOf( message.getUri() ) );
      final boolean noPath = message.getUri( ).isEmpty( ) || message.getUri( ).equals( "/" ) || message.getUri( ).startsWith( "/?" );
      return
          targetMatch &&
              ( usesServicePath ||
                  ( noPath && resolvesByHost( message.getHeader( HttpHeaders.Names.HOST ) ) ) );
    }
    return false;
  }
}