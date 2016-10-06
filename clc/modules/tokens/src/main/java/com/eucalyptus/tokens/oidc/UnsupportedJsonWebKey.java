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
import com.eucalyptus.util.Parameters;
import com.fasterxml.jackson.databind.JsonNode;
import javaslang.control.Option;

/**
 *
 */
class UnsupportedJsonWebKey extends JsonWebKey {

  private final String kty;

  static UnsupportedJsonWebKey fromJson( final JsonNode keyObject ) throws IOException {
    return new UnsupportedJsonWebKey(
        alg( keyObject ),
        kid( keyObject ),
        use( keyObject ),
        keyOps( keyObject ),
        x5c( keyObject ),
        kty( keyObject )
    );
  }

  UnsupportedJsonWebKey(
      final String alg,
      final Option<String> kid,
      final Option<String> use,
      final Option<List<String>> keyOps,
      final Option<List<String>> x5c,
      final String kty
  ) {
    super( alg, kid, use, keyOps, x5c );
    this.kty = Parameters.checkParamNotNull( "kty", kty );
  }

  @Override
  @Nonnull
  public String getKty( ) {
    return kty;
  }
}
