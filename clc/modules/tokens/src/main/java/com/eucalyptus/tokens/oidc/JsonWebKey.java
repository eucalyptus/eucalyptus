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
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import com.eucalyptus.util.Json;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.Strings;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.MoreObjects;
import io.vavr.control.Option;

/**
 * JSON Web Key (JWK)
 * https://tools.ietf.org/html/rfc7517
 */
public abstract class JsonWebKey {

  private final String alg;
  private final Option<String> kid;
  private final Option<String> use;
  private final Option<List<String>> keyOps;
  private final Option<List<String>> x5c; // base64 encoded certificate chain

  JsonWebKey(
      final String alg,
      final Option<String> kid,
      final Option<String> use,
      final Option<List<String>> keyOps,
      final Option<List<String>> x5c
  ) {
    this.alg = alg;
    this.kid = kid;
    this.use = use;
    this.keyOps = keyOps;
    this.x5c =x5c;
  }

  @Nonnull
  public abstract String getKty( );

  @Nonnull
  public String getAlg( ) {
    return alg;
  }

  @Nonnull
  public Option<String> getKid( ) {
    return kid;
  }

  @Nonnull
  public Option<String> getUse( ) {
    return use;
  }

  @Nonnull
  public Option<List<String>> getKeyOps( ) {
    return keyOps;
  }

  @Nonnull
  public Option<List<String>> getX5c( ) {
    return x5c;
  }

  public Pair<String,Option<String>> key( ) {
    return key( getKty( ), getKid( ) );
  }

  public static Pair<String,Option<String>> key( final Class<? extends JsonWebKey> type, final Option<String> kid ) {
    return Pair.pair( type( type ), kid );
  }

  public static Pair<String,Option<String>> key( final String type, final Option<String> kid ) {
    return Pair.pair( type, kid );
  }

  public String toString( ) {
    return MoreObjects.toStringHelper( JsonWebKey.class )
        .add( "kty", getKty( ) )
        .add( "kid", getKid( ) )
        .add( "alg", getAlg( ) )
        .add( "use", getUse( ) )
        .add( "keyOps", getKeyOps( ) )
        .add( "x5cSize", getX5c( ).map( Collection::size ) )
        .toString( );
  }

  static String alg( final JsonNode node ) throws IOException {
    return Json.text( node, "alg" );
  }

  static Option<List<String>> keyOps( final JsonNode node ) throws IOException {
    return Json.textListOption( node, "key_ops" );
  }

  static Option<List<String>> x5c( final JsonNode node ) throws IOException {
    return Json.textListOption( node, "x5c" );
  }

  static Option<String> kid( final JsonNode node ) throws IOException {
    return Json.textOption( node, "kid" );
  }

  static String kty( final JsonNode node ) throws IOException {
    return Json.text( node, "kty" );
  }

  static Option<String> use( final JsonNode node ) throws IOException {
    return Json.textOption( node, "use" );
  }

  static void assertKty( final JsonNode node, final String type ) throws IOException {
    if ( !type.equals( kty( node ) ) ) {
      throw new IOException( "Unexpected kty: " + type );
    }
  }

  protected static String type( Class<? extends JsonWebKey> type ) {
    return Strings.trimSuffix( JsonWebKey.class.getSimpleName( ), type.getSimpleName( ) ).toUpperCase( );
  }
}
