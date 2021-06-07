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
import io.vavr.control.Option;

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
