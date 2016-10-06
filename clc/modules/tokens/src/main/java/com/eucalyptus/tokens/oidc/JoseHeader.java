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
import javaslang.control.Option;

/**
 *  The JOSE (JSON Object Signing and Encryption) Header is comprised of a set of Header Parameters.
 */
public class JoseHeader {

  @Nonnull private final String alg;
  @Nonnull private final Option<String> kid;
  @Nonnull private final Option<String> typ;
  @Nonnull private final Option<List<String>> crit;

  JoseHeader(
      @Nonnull final String alg,
      @Nonnull final Option<String> kid,
      @Nonnull final Option<String> typ,
      @Nonnull final Option<List<String>> crit
  ) {
    this.alg = Parameters.checkParamNotNull( "alg", alg );
    this.kid = Parameters.checkParamNotNull( "kid", kid );
    this.typ = Parameters.checkParamNotNull( "typ", typ );
    this.crit = Parameters.checkParamNotNull( "crit", crit );
  }

  @Nonnull
  public String getAlg( ) {
    return alg;
  }

  @Nonnull
  public Option<String> getKid( ) {
    return kid;
  }

  @Nonnull
  public Option<String> getTyp( ) {
    return typ;
  }

  @Nonnull
  public Option<List<String>> getCrit( ) {
    return crit;
  }

  public String toString( ) {
    return MoreObjects.toStringHelper( JoseHeader.class )
        .add( "alg", alg )
        .add( "kid", kid )
        .add( "typ", typ )
        .add( "crit", crit )
        .toString( );
  }

  public static JoseHeader parse( final String headerJson ) throws OidcParseException {
    try {
      final JsonNode header = Json.parseObject( headerJson );
      return new JoseHeader(
          Json.text( header, "alg" ),
          Json.textOption( header, "kid" ),
          Json.textOption( header, "typ" ),
          Json.textListOption( header, "crit" )
      );
    } catch ( final IOException e ) {
      throw new OidcParseException( "Jose header invalid: " + e.getMessage( ), e );
    }
  }
}
