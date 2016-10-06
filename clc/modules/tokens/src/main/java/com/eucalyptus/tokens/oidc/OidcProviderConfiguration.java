/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.tokens.oidc;

import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;
import com.eucalyptus.auth.euare.common.identity.OidcProvider;
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
