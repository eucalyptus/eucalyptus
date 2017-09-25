/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.tokens.oidc;

import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;
import com.eucalyptus.auth.euare.common.identity.msgs.OidcProvider;
import com.eucalyptus.auth.euare.common.oidc.OIDCUtils;
import com.eucalyptus.auth.principal.OpenIdConnectProvider;
import com.eucalyptus.util.Json;
import com.eucalyptus.util.Parameters;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

/**
 *
 */
public class OidcProviderConfiguration {

  public static final String DISCOVERY_URL_SUFFIX = ".well-known/openid-configuration";

  @Nonnull private final String issuer;
  @Nonnull private final String authorizationEndpoint;
  @Nonnull private final String jwksUri;
  @Nonnull private final List<String> responseTypesSupported;
  @Nonnull private final List<String> subjectTypesSupported;
  @Nonnull private final List<String> idTokenSigningAlgValuesSupported;

  public OidcProviderConfiguration(
      @Nonnull final String issuer,
      @Nonnull final String authorizationEndpoint,
      @Nonnull final String jwksUri,
      @Nonnull final List<String> responseTypesSupported,
      @Nonnull final List<String> subjectTypesSupported,
      @Nonnull final List<String> idTokenSigningAlgValuesSupported
  ) {
    this.issuer = Parameters.checkParamNotNull( "issuer", issuer );
    this.authorizationEndpoint = Parameters.checkParamNotNull( "authorizationEndpoint", authorizationEndpoint );
    this.jwksUri = Parameters.checkParamNotNull( "jwksUri", jwksUri );
    this.responseTypesSupported = ImmutableList.copyOf( responseTypesSupported );
    this.subjectTypesSupported = ImmutableList.copyOf( subjectTypesSupported );
    this.idTokenSigningAlgValuesSupported = ImmutableList.copyOf( idTokenSigningAlgValuesSupported );
  }

  @Nonnull
  public String getIssuer( ) {
    return issuer;
  }

  @Nonnull
  public String getJwksUri( ) {
    return jwksUri;
  }

  @Nonnull
  public List<String> getIdTokenSigningAlgValuesSupported( ) {
    return idTokenSigningAlgValuesSupported;
  }

  public String toString() {
    return MoreObjects.toStringHelper( OidcProvider.class )
        .add( "issuer", issuer )
        .add( "jwksUri", jwksUri )
        .add( "idTokenSigningAlgValuesSupported", idTokenSigningAlgValuesSupported )
        .toString( );
  }

  public static String buildDiscoveryUrl( final OpenIdConnectProvider provider ) {
    final String baseUrl = OIDCUtils.buildIssuerIdentifier( provider );
    final String joiner = baseUrl.endsWith( "/" ) ?
        "" :
        "/";
    return baseUrl + joiner + DISCOVERY_URL_SUFFIX;
  }

  public static OidcProviderConfiguration parse( final String configJson ) throws OidcParseException {
    try {
      final JsonNode config = Json.parseObject( configJson );
      return new OidcProviderConfiguration(
          Json.text( config, "issuer" ),
          Json.text( config, "authorization_endpoint" ),
          Json.text( config, "jwks_uri" ),
          Json.textList( config, "response_types_supported" ),
          Json.textList( config, "subject_types_supported" ),
          Json.textList( config, "id_token_signing_alg_values_supported" )
      );
    } catch ( final IOException e ) {
      throw new OidcParseException( "Oidc provider configuration error: " + e.getMessage( ), e );
    }
  }
}
