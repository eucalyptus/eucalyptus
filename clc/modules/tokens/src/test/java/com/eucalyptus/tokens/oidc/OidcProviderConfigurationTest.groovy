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
