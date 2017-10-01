/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudformation.ws;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.Collections;
import java.util.List;
import javax.security.auth.Subject;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.euare.DelegatingUserPrincipal;
import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.auth.principal.PolicyVersion;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.cloudformation.config.CloudFormationProperties;
import com.eucalyptus.cloudformation.util.CfnIdentityDocumentCredential;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.Signatures;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.Json;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.io.BaseEncoding;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;

/**
 *
 */
public class CloudFormationCfnAuthenticationHandler extends MessageStackHandler {
  private static final String AUTH_SCHEME_CFNV1 = "CFN_V1";

  private static final String DEFAULT_INSTANCE_AUTH_CACHE_SPEC = "maximumSize=1000, expireAfterWrite=5m";

  private static final CompatFunction<String,Cache<Tuple2<String,String>,Option<Tuple2<String,String>>>>
      MEMOIZED_CACHE_BUILDER = FUtils.memoizeLast( spec -> CacheBuilder.from( CacheBuilderSpec.parse( spec ) ).build( ) );

  static boolean usesCfnV1Authentication( final MappingHttpRequest request ) {
    final String authHeader = request.getHeader( HttpHeaders.Names.AUTHORIZATION );
    return authHeader != null && authHeader.startsWith( AUTH_SCHEME_CFNV1 );
  }

  @Override
  public void incomingMessage( final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpRequest ) {
      final MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
      final String authHeader = httpRequest.getHeader( HttpHeaders.Names.AUTHORIZATION );
      if ( authHeader == null ) {
        throw new AuthenticationException( "Invalid or missing authorization header" );
      }
      final int signaturePartIndex =  authHeader.indexOf( ':' );
      if ( signaturePartIndex < 100 ) {
        throw new AuthenticationException( "Invalid or missing authorization header" );
      }
      final String instanceId;
      final String accountNumber;
      try {
        final String documentPart = authHeader.substring( AUTH_SCHEME_CFNV1.length( ) + 1, signaturePartIndex );
        final String signaturePart = authHeader.substring( signaturePartIndex + 1 );
        final byte[] documentBytes = BaseEncoding.base64( ).decode( documentPart );
        final String document = new String( documentBytes, StandardCharsets.UTF_8 );
        final byte[] signature = BaseEncoding.base64( ).decode( signaturePart );
        final Option<Tuple2<String,String>> instanceAndAccountNumber =
            cache( ).get( Tuple.of( documentPart, signaturePart ), ( ) -> {
          final SystemCredentials.Credentials credentials = SystemCredentials.lookup( Eucalyptus.class );
          final Signature sig = Signatures.SHA1WithRSA.getInstance( );
          sig.initVerify( credentials.getCertificate( ) );
          sig.update( documentBytes );
          if ( sig.verify( signature ) ) {
            final ObjectNode documentObject = Json.parseObject( document );
            return Option.of( Tuple.of(
                documentObject.get( "instanceId" ).asText( ),
                documentObject.get( "accountId" ).asText( )
            ) );
          } else {
            return Option.none( );
          }
        } );

        if ( instanceAndAccountNumber.isEmpty( ) ) {
          throw new AuthenticationException( "Invalid signature" );
        } else {
          instanceId = instanceAndAccountNumber.get( )._1;
          accountNumber = instanceAndAccountNumber.get( )._2;
        }
      } catch ( IllegalArgumentException e ) {
        throw new AuthenticationException( "Invalid or missing authorization header" );
      }

      // Login as account admin but without any permissions
      // CfnIdentityDocumentCredential identifies the requesting instance
      final Context context = Contexts.lookup( httpRequest.getCorrelationId( ) );
      final Subject subject = new Subject( );
      final UserPrincipal principal =
          new DelegatingUserPrincipal( Accounts.lookupCachedPrincipalByAccountNumber( accountNumber ) ) {
            @Override public List<PolicyVersion> getPrincipalPolicies( ) { return Collections.emptyList( ); }
            @Override public boolean isAccountAdmin( ) { return false; }
            @Override public boolean isSystemAdmin( ) { return false; }
            @Override public boolean isSystemUser( ) { return false; }
          };
      subject.getPrincipals( ).add( principal );
      subject.getPublicCredentials( ).add( CfnIdentityDocumentCredential.of( instanceId ) );
      context.setUser( principal );
      context.setSubject( subject );
    }
  }

  private static Cache<Tuple2<String,String>,Option<Tuple2<String,String>>> cache( ) {
    return MEMOIZED_CACHE_BUILDER.apply( MoreObjects.firstNonNull(
        Strings.emptyToNull( CloudFormationProperties.CFN_INSTANCE_AUTH_CACHE ),
        DEFAULT_INSTANCE_AUTH_CACHE_SPEC
    ) );
  }
}
