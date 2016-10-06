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
import javaslang.control.Option;

/**
 *
 */
public class EcJsonWebKey extends JsonWebKey {

  public static final String TYPE = "EC";

  private final String crv;
  private final String x;
  private final String y;

  static EcJsonWebKey fromJson( final JsonNode keyObject ) throws IOException {
    assertKty( keyObject, TYPE );
    return new EcJsonWebKey(
        alg( keyObject ),
        kid( keyObject ),
        use( keyObject ),
        keyOps( keyObject ),
        x5c( keyObject ),
        Json.text( keyObject, "crv" ),
        Json.text( keyObject, "x" ),
        Json.text( keyObject, "y" )
    );
  }

  EcJsonWebKey( final String alg,
                final Option<String> kid,
                final Option<String> use,
                final Option<List<String>> keyOps,
                final Option<List<String>> x5c,
                final String crv,
                final String x,
                final String y ) {
    super( alg, kid, use, keyOps, x5c );
    this.crv = Parameters.checkParamNotNull( "crv", crv );
    this.x = Parameters.checkParamNotNull( "x", x );
    this.y = Parameters.checkParamNotNull( "y", y );
  }

  @Nonnull
  @Override
  public String getKty() {
    return TYPE;
  }

  @Nonnull
  public String getCrv( ) {
    return crv;
  }

  @Nonnull
  public String getX( ) {
    return x;
  }

  @Nonnull
  public String getY( ) {
    return y;
  }
}
