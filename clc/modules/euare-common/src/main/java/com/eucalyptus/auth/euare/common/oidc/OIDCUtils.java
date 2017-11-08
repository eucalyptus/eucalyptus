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
package com.eucalyptus.auth.euare.common.oidc;

import java.net.URI;
import java.net.URISyntaxException;
import com.eucalyptus.auth.principal.OpenIdConnectProvider;
import com.google.common.base.Strings;


/**
 *
 */
public class OIDCUtils {

  /**
   * Parse a provider url and throw if invalid.
   *
   * @param issuerIdentifier The URL (must start with "https://")
   * @return A tuple of the host, port and path for the provider
   * @throws IllegalArgumentException If the url is invalid
   */
  public static OIDCIssuerIdentifier parseIssuerIdentifier(
      final String issuerIdentifier
  ) throws IllegalArgumentException {
    try {
      if ( issuerIdentifier == null ) {
        throw new IllegalArgumentException( "Null issuer identifier" );
      }
      final URI uri = new URI( issuerIdentifier );
      if ( !"https".equalsIgnoreCase( uri.getScheme( ) ) && !"http".equalsIgnoreCase( uri.getScheme( ) ) ) {
        throw new IllegalArgumentException(
            "Invalid scheme " + uri.getScheme( ) + " for issuer identifier: " + issuerIdentifier );
      }
      if ( uri.getQuery( ) != null || uri.getFragment( ) != null ) {
        throw new IllegalArgumentException( "Query or fragment not permitted: " + issuerIdentifier );
      }
      return new OIDCIssuerIdentifier( uri.getHost( ), uri.getPort( ), Strings.nullToEmpty( uri.getPath( ) ) ); //TODO:STEVE: what about only /?
    } catch ( URISyntaxException e ) {
      throw new IllegalArgumentException( e.getMessage( ), e );
    }
  }

  /**
   * Parse a provider url and use the given port.
   *
   * @param url The provider URL (i.e host/path)
   * @param port The optional port
   * @return A tuple of the host, port and path for the provider
   * @throws IllegalArgumentException If the url is invalid
   */
  public static OIDCIssuerIdentifier issuerIdentifierFromProviderUrl(
      final String url,
      final Integer port
  ) throws IllegalArgumentException {
    if ( url == null ) {
      throw new IllegalArgumentException( "Null url" );
    }
    final int pathStartIndex = url.indexOf( '/' );
    if ( pathStartIndex == 0 ) throw new IllegalArgumentException( "Invalid host in url: " + url );
    final String host = pathStartIndex > 0 ? url.substring( 0, pathStartIndex ) : url;
    final String path = pathStartIndex > 0 ? url.substring( pathStartIndex ) :  "";
    return new OIDCIssuerIdentifier( host, port == null || port < 1 ? -1 : port, path );
  }


  public static String buildIssuerIdentifier( final OpenIdConnectProvider issuerIdentifier ) {
    return buildIssuerIdentifier( new OIDCIssuerIdentifier( issuerIdentifier ) );
  }

  public static String buildIssuerIdentifier( final OIDCIssuerIdentifier issuerIdentifier ) {
    return buildIssuerIdentifier(
        issuerIdentifier.getHost( ),
        issuerIdentifier.getPort( ),
        issuerIdentifier.getPath( ),
        true );
  }

  public static String buildIssuerIdentifier(
      final String host,
      final Integer port,
      final String path,
      final boolean omitDefaultPort
  ) {
    return
        "https://" +
        host +
        (port > 0 && (!omitDefaultPort || port != 443) ? ":" + port : "" ) +
        path;
  }
}
