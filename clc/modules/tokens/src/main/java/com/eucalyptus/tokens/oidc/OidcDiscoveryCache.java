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

import static java.lang.System.getProperty;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.primitives.Ints.tryParse;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.HttpsURLConnection;
import org.apache.commons.io.input.BoundedInputStream;
import com.eucalyptus.crypto.util.SslSetup;
import com.eucalyptus.util.Pair;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.net.HttpHeaders;
import io.vavr.control.Option;

/**
 *
 */
public class OidcDiscoveryCache {

  private static final int CONNECT_TIMEOUT =
      firstNonNull( tryParse( getProperty( "com.eucalyptus.tokens.oidc.connectTimeout", "" ) ), 20_000 );

  private static final int READ_TIMEOUT =
      firstNonNull( tryParse( getProperty( "com.eucalyptus.tokens.oidc.readTimeout", "" ) ), 30_000 );

  private static final int MAX_LENGTH =
      firstNonNull( tryParse( getProperty( "com.eucalyptus.tokens.oidc.maxLength", "" ) ), 128 * 1024 );

  private final AtomicReference<Pair<String,Cache<String,OidcDiscoveryCachedResource>>> cacheReference =
      new AtomicReference<>( );

  public Pair<String, Certificate[]> get(
      final String cacheSpec,
      final long minimumRefreshInterval,
      final long timeNow,
      final String url
  ) throws IOException {
    final Cache<String,OidcDiscoveryCachedResource> cache = cache( cacheSpec );
    final OidcDiscoveryCachedResource cachedResource = cache.getIfPresent( url );
    final OidcDiscoveryCachedResource resource;
    if ( cachedResource == null ) { // not cached
      resource = fetchResource( url, timeNow, null );
    } else if ( cachedResource.needsRefresh( minimumRefreshInterval, timeNow ) ) { // cache refresh expired, check if current
      resource = fetchResource( url, timeNow, cachedResource );
    } else { // use existing
      resource = cachedResource;
    }
    if ( resource != cachedResource ) {
      cache.put( url, resource );
    }
    return resource.contentPair( );
  }

  private OidcDiscoveryCachedResource fetchResource(
      final String url,
      final long timeNow,
      final OidcDiscoveryCachedResource cached
  ) throws IOException {
    final URL location = new URL( url );
    final OidcResource oidcResource;
    { // setup url connection and resolve
      final HttpURLConnection conn = (HttpURLConnection) location.openConnection( );
      conn.setAllowUserInteraction( false );
      conn.setInstanceFollowRedirects( false );
      conn.setConnectTimeout( CONNECT_TIMEOUT );
      conn.setReadTimeout( READ_TIMEOUT );
      conn.setUseCaches( false );
      if ( cached != null ) {
        if ( cached.lastModified.isDefined( ) ) {
          conn.setRequestProperty( HttpHeaders.IF_MODIFIED_SINCE, cached.lastModified.get( ) );
        }
        if ( cached.etag.isDefined( ) ) {
          conn.setRequestProperty( HttpHeaders.IF_NONE_MATCH, cached.etag.get( ) );
        }
      }
      oidcResource = resolve( conn );
    }

    // build cache entry from resource
    if ( oidcResource.statusCode == 304 ) {
      return new OidcDiscoveryCachedResource( timeNow, cached );
    } else {
      return new OidcDiscoveryCachedResource(
          timeNow,
          Option.of( oidcResource.lastModifiedHeader ),
          Option.of( oidcResource.etagHeader ),
          ImmutableList.copyOf( oidcResource.certs ),
          url,
          new String( oidcResource.content, StandardCharsets.UTF_8 )
      );
    }
  }

  protected OidcResource resolve( final HttpURLConnection conn ) throws IOException {
    SslSetup.configureHttpsUrlConnection( conn );
    try ( final InputStream istr = conn.getInputStream( ) ) {
      final int statusCode = conn.getResponseCode( );
      if ( statusCode == 304 ) {
        return new OidcResource( statusCode );
      } else {
        Certificate[] certs = new Certificate[0];
        if (conn instanceof HttpsURLConnection ) {
          certs = ((HttpsURLConnection)conn).getServerCertificates();
        }
        final long contentLength = conn.getContentLengthLong( );
        if ( contentLength > MAX_LENGTH) {
          throw new IOException( conn.getURL( ) + " content exceeds maximum size, " + MAX_LENGTH );
        }
        final byte[] content = ByteStreams.toByteArray( new BoundedInputStream( istr, MAX_LENGTH + 1 ) );
        if ( content.length > MAX_LENGTH) {
          throw new IOException( conn.getURL( ) + " content exceeds maximum size, " + MAX_LENGTH );
        }
        return new OidcResource(
            statusCode,
            conn.getHeaderField( HttpHeaders.LAST_MODIFIED ),
            conn.getHeaderField( HttpHeaders.ETAG ),
            certs,
            content
        );
      }
    }
  }

  private Cache<String,OidcDiscoveryCachedResource> cache( final String cacheSpec ) {
    Cache<String,OidcDiscoveryCachedResource> cache;
    final Pair<String,Cache<String,OidcDiscoveryCachedResource>> cachePair = cacheReference.get( );
    if ( cachePair == null || !cacheSpec.equals( cachePair.getLeft( ) ) ) {
      final Pair<String,Cache<String,OidcDiscoveryCachedResource>> newCachePair = Pair.pair(
          cacheSpec,
          CacheBuilder.from( CacheBuilderSpec.parse( cacheSpec ) ).build( ) );
      if ( cacheReference.compareAndSet( cachePair, newCachePair ) || cachePair == null ) {
        cache = newCachePair.getRight( );
      } else {
        cache = cachePair.getRight( );
      }
    } else {
      cache = cachePair.getRight( );
    }
    return cache;
  }

  protected static class OidcResource {
    private final int statusCode;
    private final String lastModifiedHeader;
    private final String etagHeader;
    private final Certificate[] certs;
    private final byte[] content;

    protected OidcResource(
        final int statusCode
    ) {
      this( statusCode, null, null, null, null );
    }

    protected OidcResource(
        final int statusCode,
        final String lastModifiedHeader,
        final String etagHeader,
        final Certificate[] certs,
        final byte[] content
    ) {
      this.statusCode = statusCode;
      this.lastModifiedHeader = lastModifiedHeader;
      this.etagHeader = etagHeader;
      this.certs = certs;
      this.content = content;
    }
  }

  private static class OidcDiscoveryCachedResource {
    private final long cached;
    private final Option<String> lastModified;
    private final Option<String> etag;
    private final ImmutableList<Certificate> certificateChain;
    private final String url;
    private final String resource;

    private OidcDiscoveryCachedResource(
        final long timeNow,
        final OidcDiscoveryCachedResource from
    ) {
      this.cached = timeNow;
      this.lastModified = from.lastModified;
      this.etag = from.etag;
      this.certificateChain = from.certificateChain;
      this.url = from.url;
      this.resource = from.resource;
    }

    private OidcDiscoveryCachedResource(
        final long cached,
        final Option<String> lastModified,
        final Option<String> etag,
        final ImmutableList<Certificate> certificateChain,
        final String url,
        final String resource
    ) {
      this.cached = cached;
      this.lastModified = lastModified;
      this.etag = etag;
      this.certificateChain = certificateChain;
      this.url = url;
      this.resource = resource;
    }

    private boolean needsRefresh(
        final long minimumRefreshInterval,
        final long timeNow
    ) {
      return timeNow > ( cached + minimumRefreshInterval );
    }

    private Pair<String, Certificate[]> contentPair( ) {
      return Pair.pair( resource, certificateChain.toArray( new Certificate[ certificateChain.size( ) ] ) );
    }
  }
}
