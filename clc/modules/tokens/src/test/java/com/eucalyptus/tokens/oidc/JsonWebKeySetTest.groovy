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

import static org.junit.Assert.*

import io.vavr.control.Option
import org.junit.Test

/**
 *
 */
class JsonWebKeySetTest {

  private final String basicSet = '''\
    {
        "keys": [
            {
                "kty": "RSA",
                "n": "73l27Yp7WT2c0Ve7EoGJ13AuKzg-GHU7Mpgx0JKa_hO04gAXSVXRadQy7gmdLLtAK8uBVcV0fHGgsBl4J92t-I7hayiJSLbgbX-sZhI_OfegeOLcSNB9poPS9w60XGqR9buYOW2x-KXXitsmyHXNmg_-1u0uqfKHu9pmST8dcjUYXTM5F3oJpQKeJlSH8daMlDks4xb9Y83EEFRv-ppY965-WTm2NW4pwLlbgGTWFvZ6YS6GTb-mfGwGuzStI0lKZ7dOFx9ryYQ4wSoUVHtIrypT-gbuaT90Z2SkwOH-GaEZJkudctBeGpieOsyC7P40UXpwgGNFy3xoWL4vHpnHmQ==",
                "e": "AQAB",
                "alg": "RS512",
                "use": "sig"
            }
        ]
    }
    '''.stripIndent( )

  @Test
  void testBasicParse() {
    println JsonWebKeySet.parse( basicSet )
  }

  @Test( expected = OidcParseException )
  void testInvalidParse() {
    JsonWebKeySet.parse( basicSet.substring( 0, basicSet.length( ) - 5 ) )
  }

  @Test
  void testBasicLookup( ) {
    Option<RsaJsonWebKey> keyOption =
        JsonWebKeySet.parse( basicSet ).findKey( Option.none( ), RsaJsonWebKey.class, "sig", "validate" )
    assertNotNull( 'key by type', keyOption  )
    assertTrue( 'key present', keyOption.isDefined( )  )
    assertTrue( 'key single value', keyOption.isSingleValued( )  )
    assertEquals( 'key type', 'RSA', keyOption.get( ).getKty( )  )
    assertNotNull( 'key n', keyOption.get( ).getN( )  )
    assertNotNull( 'key e', keyOption.get( ).getE( )  )
    assertNotNull( 'key alg', keyOption.get( ).getAlg( )  )
  }

}