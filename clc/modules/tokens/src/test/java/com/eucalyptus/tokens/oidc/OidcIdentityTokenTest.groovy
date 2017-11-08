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
