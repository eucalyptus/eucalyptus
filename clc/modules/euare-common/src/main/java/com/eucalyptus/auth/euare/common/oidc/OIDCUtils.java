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
