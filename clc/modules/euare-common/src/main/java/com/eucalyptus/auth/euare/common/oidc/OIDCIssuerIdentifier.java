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

import static org.hamcrest.Matchers.*;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import javax.annotation.Nonnull;
import com.eucalyptus.auth.principal.OpenIdConnectProvider;
import com.eucalyptus.util.Parameters;

/**
 * Immutable representation for components of a parsed issuer identifier.
 *
 * The issuer identifier is a case sensitive URL using the https scheme that
 * contains scheme, host, and optionally, port number and path components and
 * no query or fragment components.
 *
 * This class is immutable.
 */
public class OIDCIssuerIdentifier {

  private final String host;
  private final int port;
  private final String path;

  public OIDCIssuerIdentifier(
      @Nonnull final String host,
               final int port,
      @Nonnull final String path ) {
    Parameters.checkParam( "host", host, not( isEmptyOrNullString( ) ) );
    Parameters.checkParam( "port", port, anyOf( equalTo( -1 ), greaterThan( 0 ) ) );
    Parameters.checkParam( "path", path, notNullValue( ) );
    this.host = host;
    this.port = port;
    this.path = path;
  }

  public OIDCIssuerIdentifier( @Nonnull final OpenIdConnectProvider openIdConnectProvider ) {
    this(
        openIdConnectProvider.getHost( ),
        openIdConnectProvider.getPort( ),
        openIdConnectProvider.getPath( ) );
  }

  public String getHost( ) {
    return host;
  }

  public int getPort( ) {
    return port;
  }

  public String getPath( ) {
    return path;
  }
}
