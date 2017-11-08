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

import com.eucalyptus.tokens.oidc.OidcDiscoveryCache.OidcResource as Res
import com.eucalyptus.util.Pair
import com.google.common.net.HttpHeaders
import org.junit.Test

import java.nio.charset.StandardCharsets
import java.security.cert.Certificate

import static org.junit.Assert.*

/**
 *
 */
class OidcDiscoveryCacheTest {

  @Test
  void testGet( ) {
    OidcDiscoveryCache cache = new OidcDiscoveryCache( ) {
      @Override
      protected Res resolve( final HttpURLConnection conn ) throws IOException {
        return new Res( 200, null, null, new Certificate[0], 'content'.getBytes( StandardCharsets.UTF_8 ) )
      }
    }
    Pair<String,Certificate[]> resourcePair =
        cache.get( 'maximumSize=20, expireAfterWrite=1m', 60_000, System.currentTimeMillis( ), 'http://test.com/test' )
    assertNotNull( 'resource pair', resourcePair )
    assertNotNull( 'resource content', resourcePair.left )
    assertNotNull( 'resource certs', resourcePair.right )
    assertEquals( 'resource contents', 'content', resourcePair.left )
  }

  @Test
  void testGetFromCache( ) {
    int resolveCount = 0
    long startTime = System.currentTimeMillis( )
    OidcDiscoveryCache cache = new OidcDiscoveryCache( ) {
      @Override
      protected Res resolve( final HttpURLConnection conn ) throws IOException {
        resolveCount += 1
        return new Res( 200, null, null, new Certificate[0], 'content'.getBytes( StandardCharsets.UTF_8 ) )
      }
    }
    cache.get( 'maximumSize=20, expireAfterWrite=1m', 60_000, startTime, 'http://test.com/test' )
    assertEquals( 'resolve count', 1, resolveCount )

    Pair<String,Certificate[]> resourcePair =
        cache.get( 'maximumSize=20, expireAfterWrite=1m', 60_000, startTime + 2, 'http://test.com/test' )
    assertNotNull( 'resource pair', resourcePair )
    assertEquals( 'resource contents', 'content', resourcePair.left )
    assertEquals( 'resolve count', 1, resolveCount )

    Pair<String,Certificate[]> resourcePair2 =
        cache.get( 'maximumSize=20, expireAfterWrite=1m', 60_000, startTime + 60_001, 'http://test.com/test' )
    assertNotNull( 'resource pair', resourcePair2 )
    assertEquals( 'resource contents', 'content', resourcePair2.left )
    assertEquals( 'resolve count', 2, resolveCount )
  }

  @Test
  void testGetIfModified( ) {
    int resolveCount = 0
    String lastResolvedEtag = null
    long startTime = System.currentTimeMillis( )
    OidcDiscoveryCache cache = new OidcDiscoveryCache( ) {
      @Override
      protected Res resolve( final HttpURLConnection conn ) throws IOException {
        resolveCount += 1
        lastResolvedEtag = conn.getRequestProperty( HttpHeaders.IF_NONE_MATCH )
        return lastResolvedEtag == null ?
            new Res( 200, null, 'etag-value-1', new Certificate[0], 'content'.getBytes( StandardCharsets.UTF_8 ) ) :
            new Res( 304 )
      }
    }
    cache.get( 'maximumSize=20, expireAfterWrite=1m', 60_000, startTime, 'http://test.com/test' )
    assertEquals( 'resolve count', 1, resolveCount )
    assertNull( 'etag', lastResolvedEtag )

    Pair<String,Certificate[]> resourcePair2 =
        cache.get( 'maximumSize=20, expireAfterWrite=1m', 60_000, startTime + 60_001, 'http://test.com/test' )
    assertNotNull( 'resource pair', resourcePair2 )
    assertEquals( 'resource contents', 'content', resourcePair2.left )
    assertEquals( 'resolve count', 2, resolveCount )
    assertEquals( 'etag', 'etag-value-1', lastResolvedEtag )
  }

  @Test
  void testGetMultipleResources( ) {
    int resolveCount = 0
    long startTime = System.currentTimeMillis( )
    OidcDiscoveryCache cache = new OidcDiscoveryCache( ) {
      @Override
      protected Res resolve( final HttpURLConnection conn ) throws IOException {
        resolveCount += 1
        return new Res( 200, null, null, new Certificate[0], 'content'.getBytes( StandardCharsets.UTF_8 ) )
      }
    }
    cache.get( 'maximumSize=20, expireAfterWrite=1m', 60_000, startTime, 'http://test.com/test/1' )
    assertEquals( 'resolve count', 1, resolveCount )

    cache.get( 'maximumSize=20, expireAfterWrite=1m', 60_000, startTime + 1, 'http://test.com/test/2' )
    assertEquals( 'resolve count', 2, resolveCount )

    cache.get( 'maximumSize=20, expireAfterWrite=1m', 60_000, startTime + 2, 'http://test.com/test/2' )
    assertEquals( 'resolve count', 2, resolveCount )
  }

}
