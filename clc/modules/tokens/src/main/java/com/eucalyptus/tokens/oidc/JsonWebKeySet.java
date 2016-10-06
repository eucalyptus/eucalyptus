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
import javaslang.collection.Seq;
import javaslang.collection.Stream;
import javaslang.control.Option;
import javaslang.control.Try;

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
    } catch ( final Try.NonFatalException e ) {
      throw new OidcParseException( "Invalid json web key set: " + e.getCause( ).getMessage( ), e.getCause( ) );
    } catch ( final IOException e ) {
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
