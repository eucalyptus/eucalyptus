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

import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;
import com.eucalyptus.util.Json;
import com.eucalyptus.util.Parameters;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Option;

/**
 * The body for an OIDC id token.
 */
public class OidcIdentityToken {

  @Nonnull private final String iss;
  @Nonnull private final List<String> aud;
  @Nonnull private final String sub;
  @Nonnull private final Long iat; // seconds since epoch
  @Nonnull private final Long exp; // seconds since epoch
  @Nonnull private final Option<Long> nbf; // seconds since epoch

  OidcIdentityToken(
      @Nonnull final String iss,
      @Nonnull final List<String> aud,
      @Nonnull final String sub,
      @Nonnull final Long iat,
      @Nonnull final Long exp,
      @Nonnull final Option<Long> nbf
  ) {
    this.iss = Parameters.checkParamNotNull( "iss", iss );
    this.aud = ImmutableList.copyOf( Parameters.checkParamNotNull( "aud", aud ) );
    this.sub = Parameters.checkParamNotNull( "sub", sub );
    this.iat = Parameters.checkParamNotNull( "iat", iat );
    this.exp = Parameters.checkParamNotNull( "exp", exp );
    this.nbf = Parameters.checkParamNotNull( "nbf", nbf );
  }

  @Nonnull
  public String getIss( ) {
    return iss;
  }

  @Nonnull
  public List<String> getAud( ) {
    return aud;
  }

  @Nonnull
  public String getSub( ) {
    return sub;
  }

  @Nonnull
  public Long getIat( ) {
    return iat;
  }

  @Nonnull
  public Long getExp( ) {
    return exp;
  }

  @Nonnull
  public Option<Long> getNbf( ) {
    return nbf;
  }

  public String toString( ) {
    return MoreObjects.toStringHelper( OidcIdentityToken.class )
        .add( "iss", iss )
        .add( "aud", aud )
        .add( "sub", sub )
        .add( "iat", iat )
        .add( "exp", exp )
        .add( "nbf", nbf )
        .toString( );
  }

  public static OidcIdentityToken parse( final String tokenJson ) throws OidcParseException {
    try {
      final JsonNode config = Json.parseObject( tokenJson );
      return new OidcIdentityToken(
          Json.text( config, "iss" ),
          Json.isText( config, "aud" ) ?
              ImmutableList.of( Json.text( config, "aud" ) ) :
              Json.textList( config, "aud" ),
          length( Json.text( config, "sub" ), "sub", 255 ),
          Json.longInt( config, "iat" ),
          Json.longInt( config, "exp" ),
          Json.longIntOption( config, "nbf" )
      );
    } catch ( final IOException e ) {
      throw new OidcParseException( "Oidc id token error: " + e.getMessage( ), e );
    }
  }

  private static String length( String text, String desc, int length ) throws OidcParseException {
    if ( text.length( ) > length  ) {
      throw new OidcParseException( "Length limit exceeded for " + desc + " " + text.length( ) + "/" + length );
    }
    return text;
  }
}
