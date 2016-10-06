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
import org.junit.Test

/**
 *
 */
class OidcIdentityTokenTest {

  @Test( expected = OidcParseException )
  void testInvalid( ) {
    // invalid due to trailing comma on exp
    OidcIdentityToken.parse('''\
    {
      "iss": "https://auth.example.com",
      "aud": "7c933f7f-8d9f-460d-bcf0-8ae5c3715371",
      "sub": "43b5b769-8581-4908-81b4-9ce0b6642d78",
      "iat": 1475859450,
      "exp": 1475859750,
    }
    '''.stripIndent( ) )
  }

  @Test
  void testBasicParse( ) {
    OidcIdentityToken token = OidcIdentityToken.parse( '''\
    {
      "iss": "https://auth.example.com",
      "aud": "7c933f7f-8d9f-460d-bcf0-8ae5c3715371",
      "sub": "43b5b769-8581-4908-81b4-9ce0b6642d78",
      "iat": 1475859450,
      "exp": 1475859750
    }
    '''.stripIndent( ) )
    println token
    assertEquals( "iss", 'https://auth.example.com', token.iss )
    assertEquals( "aud", ['7c933f7f-8d9f-460d-bcf0-8ae5c3715371'], token.aud )
    assertEquals( "sub", '43b5b769-8581-4908-81b4-9ce0b6642d78', token.sub )
    assertEquals( "iat", 1475859450L, token.iat )
    assertEquals( "exp", 1475859750L, token.exp )
  }
}
