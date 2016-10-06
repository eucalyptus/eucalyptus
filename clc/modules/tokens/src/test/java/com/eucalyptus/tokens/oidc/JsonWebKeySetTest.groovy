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

import static org.junit.Assert.*

import javaslang.control.Option
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