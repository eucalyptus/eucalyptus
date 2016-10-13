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
