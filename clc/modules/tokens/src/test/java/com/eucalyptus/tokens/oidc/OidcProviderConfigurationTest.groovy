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
package com.eucalyptus.tokens.oidc

import org.junit.Test

/**
 *
 */
class OidcProviderConfigurationTest {

  @Test( expected = OidcParseException )
  void testNull( ) {
    OidcProviderConfiguration.parse( null )
  }

  @Test( expected = OidcParseException )
  void testEmpty( ) {
    OidcProviderConfiguration.parse( '' )
  }

  @Test( expected = OidcParseException )
  void testNotJson( ) {
    OidcProviderConfiguration.parse( '<html/>' )
  }

  @Test( expected = OidcParseException )
  void testNotJsonObject( ) {
    OidcProviderConfiguration.parse( '[ "text" ]' )
  }

  @Test
  void testBasicParse( ) {
    println OidcProviderConfiguration.parse( '''\
      {
        "issuer": "https://auth.globus.org",
        "authorization_endpoint": "https://auth.globus.org/v2/oauth2/authorize",
        "userinfo_endpoint": "https://auth.globus.org/v2/oauth2/userinfo",
        "token_endpoint": "https://auth.globus.org/v2/oauth2/token",
        "revocation_endpoint": "https://auth.globus.org/v2/oauth2/token/revoke",
        "jwks_uri": "https://auth.globus.org/jwk.json",
        "response_types_supported": [
          "code",
          "token",
          "token id_token",
          "id_token"
        ],
        "id_token_signing_alg_values_supported": [
          "RS512"
        ],
        "scopes_supported": [
          "openid",
          "email",
          "profile"
        ],
        "token_endpoint_auth_methods_supported": [
          "client_secret_basic"
        ],
        "claims_supported": [
          "at_hash",
          "aud",
          "email",
          "exp",
          "name",
          "nonce",
          "preferred_username",
          "iat",
          "iss",
          "sub"
        ],
        "subject_types_supported" : ["public"]
      }
      '''.stripIndent( ) )
  }

  @Test
  void testMinimumParse( ) {
    println OidcProviderConfiguration.parse( '''\
      {
        "issuer": "https://auth.globus.org",
        "authorization_endpoint": "https://auth.globus.org/v2/oauth2/authorize",
        "jwks_uri": "https://auth.globus.org/jwk.json",
        "response_types_supported": [
          "code",
          "token",
          "token id_token",
          "id_token"
        ],
        "id_token_signing_alg_values_supported": [
          "RS512"
        ],
        "subject_types_supported" : ["public"]
      }
      '''.stripIndent( ) )
  }

  @Test( expected = OidcParseException )
  void testMissingIssuer( ) {
    println OidcProviderConfiguration.parse( '''\
      {
        "authorization_endpoint": "https://auth.globus.org/v2/oauth2/authorize",
        "jwks_uri": "https://auth.globus.org/jwk.json",
        "response_types_supported": [
          "code",
          "token",
          "token id_token",
          "id_token"
        ],
        "id_token_signing_alg_values_supported": [
          "RS512"
        ],
        "subject_types_supported" : ["public"]
      }
      '''.stripIndent( ) )
  }

  @Test( expected = OidcParseException )
  void testMissingJwksUri( ) {
    println OidcProviderConfiguration.parse( '''\
      {
        "issuer": "https://auth.globus.org",
        "authorization_endpoint": "https://auth.globus.org/v2/oauth2/authorize",
        "response_types_supported": [
          "code",
          "token",
          "token id_token",
          "id_token"
        ],
        "id_token_signing_alg_values_supported": [
          "RS512"
        ],
        "subject_types_supported" : ["public"]
      }
      '''.stripIndent( ) )
  }

  @Test( expected = OidcParseException )
  void testMissingIdTokenSigning( ) {
    println OidcProviderConfiguration.parse( '''\
      {
        "issuer": "https://auth.globus.org",
        "authorization_endpoint": "https://auth.globus.org/v2/oauth2/authorize",
        "jwks_uri": "https://auth.globus.org/jwk.json",
        "response_types_supported": [
          "code",
          "token",
          "token id_token",
          "id_token"
        ],
        "subject_types_supported" : ["public"]
      }
      '''.stripIndent( ) )
  }
}
