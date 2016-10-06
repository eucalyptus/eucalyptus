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
package com.eucalyptus.tokens.oidc;

import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;
import com.eucalyptus.util.Json;
import com.eucalyptus.util.Parameters;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import javaslang.control.Option;

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
