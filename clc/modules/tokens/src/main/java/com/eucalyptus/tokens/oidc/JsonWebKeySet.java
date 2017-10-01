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
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Json;
import com.eucalyptus.util.Pair;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.vavr.collection.Seq;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;

/**
 * JSON Web Key (JWK)
 * https://tools.ietf.org/html/rfc7517
 */
public class JsonWebKeySet {

  @Nonnull
  private final Map<Pair<String,Option<String>>,JsonWebKey> keysByTypeAndId;

  public static JsonWebKeySet parse( final String keysJson ) throws OidcParseException {
    try {
      final JsonNode keys = Json.parseObject( keysJson );
      return new JsonWebKeySet(
          Try.sequence( Stream.ofAll( Json.objectList( keys, "keys" ) ).map( JsonWebKeySet::fromJson ) ).get( )
      );
//TODO: restore this once vavr change reverted : https://github.com/vavr-io/vavr/issues/2049
//    } catch ( final Try.NonFatalException e ) {
//      throw new OidcParseException( "Invalid json web key set: " + e.getCause( ).getMessage( ), e.getCause( ) );
//    } catch ( final IOException e ) {
    } catch ( final Throwable e ) {
      throw new OidcParseException( "Invalid json web key set: " + e.getMessage( ), e );
    }
  }

  public <KT extends JsonWebKey> Option<KT> findKey(
      final Option<String> kid,
      final Class<KT> keyType,
      final String use,
      final String keyOp
  ) {
    final Pair<String,Option<String>> key = JsonWebKey.key( keyType, kid );
    final Predicate<JsonWebKey> usage = jsonWebKey ->
        jsonWebKey.getUse( ).getOrElse( use ).equals( use ) &&
        jsonWebKey.getKeyOps( ).getOrElse( Lists.newArrayList( keyOp ) ).contains( keyOp );
    final JsonWebKey jsonWebKey = keysByTypeAndId.get( key );
    if ( keyType.isInstance( jsonWebKey ) ) {
      return Option.of( keyType.cast( jsonWebKey ) ).filter( usage );
    } else if ( !kid.isDefined( ) && keysByTypeAndId.size( ) == 1 ) {
      return Stream.ofAll( keysByTypeAndId.values( ) ).find( keyType::isInstance ).map( keyType::cast ).filter( usage );
    }
    return Option.none( );
  }

  public String toString( ) {
    return MoreObjects.toStringHelper( JsonWebKeySet.class )
        .add( "keys", keysByTypeAndId.values( ) )
        .toString( );
  }

  private JsonWebKeySet( final Seq<JsonWebKey> keys ) throws OidcParseException {
    keysByTypeAndId = CollectionUtils.reduce(
        keys,
        Try.success( Maps.<Pair<String,Option<String>>,JsonWebKey>newHashMap( ) ),
        ( tryMap, key ) -> tryMap.mapTry( map -> {
          final Pair<String,Option<String>> keyPair = key.key( );
          if ( map.containsKey( keyPair ) ) {
            throw new OidcParseException( "Duplicate key identifier: " + key.getKid( ) );
          }
          map.put( keyPair, key );
          return map;
        } )
    )
    .getOrElseThrow( cause -> new OidcParseException( cause.getMessage( ) ) );
  }

  private static Try<JsonWebKey> fromJson( final JsonNode keyObject ) {
    final Try<String> kty = Try.of( () -> Json.text( keyObject, "kty" ) );
    if ( kty.isFailure( ) ) {
      return Try.failure( kty.getCause( ) );
    }
    final Try<JsonNode> nodeTry = Try.success( keyObject );
    switch ( kty.get( ) ) {
      case EcJsonWebKey.TYPE:
        return nodeTry.mapTry( EcJsonWebKey::fromJson );
      case RsaJsonWebKey.TYPE:
        return nodeTry.mapTry( RsaJsonWebKey::fromJson );
      default:
        return nodeTry.mapTry( UnsupportedJsonWebKey::fromJson );
    }
  }
}
