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
