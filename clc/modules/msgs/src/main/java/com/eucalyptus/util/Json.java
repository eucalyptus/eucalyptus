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
package com.eucalyptus.util;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import javaslang.Function1;
import javaslang.Predicates;
import javaslang.collection.Stream;
import javaslang.control.Option;

/**
 * Utility class for JSON processing with Jackson.
 */
public class Json {
  private static final ObjectMapper mapper = new ObjectMapper( );
  private static final ObjectReader reader = mapper.reader( );
  private static final Option<Stream<String>> optionStreamEmpty = Option.of( Stream.empty( ) );
  private static final Option<Stream<JsonNode>> optionStreamObjEmpty = Option.of( Stream.empty( ) );
  private static final Function1<String,Supplier<IOException>> missingExceptionSupplierFunc =
      fieldName -> () -> new IOException( "Missing required value " + fieldName );



  public static JsonNode parse( final String jsonText ) throws IOException {
    if ( jsonText == null ) throw new IOException( "Null" );
    return reader.readTree( new StringReader( jsonText ) {
      @Override public String toString() { return "json"; } // overridden for better source in error message
    } );
  }

  public static JsonNode parseObject( final String jsonText ) throws IOException {
    final JsonNode node = parse( jsonText );
    if ( !node.isObject( ) ) {
      throw new IOException( "Invalid object" );
    }
    return node;
  }

  /**
   * Get a mapper configured to ignore groovy object properties.
   */
  public static ObjectMapper mapper( ) {
    final ObjectMapper objectMapper = new ObjectMapper( );
    objectMapper.addMixInAnnotations( GroovyObject.class, GroovyMixin.class );
    return objectMapper;
  }

  public static boolean isText( final JsonNode parent, final String fieldName ) throws IOException {
    final JsonNode node = parent.get( fieldName );
    return node != null && node.isTextual( );
  }

  public static String text( final JsonNode parent, final String fieldName ) throws IOException {
    return textOption( parent, fieldName ).getOrElseThrow( ifMissing( fieldName ) );
  }

  public static Option<String> textOption( final JsonNode parent, final String fieldName ) throws IOException {
    return tOption( parent, fieldName, JsonNode::isTextual, JsonNode::asText );
  }

  public static Integer integer( final JsonNode parent, final String fieldName ) throws IOException {
    return integerOption( parent, fieldName ).getOrElseThrow( ifMissing( fieldName ) );
  }

  public static Option<Integer> integerOption( final JsonNode parent, final String fieldName ) throws IOException {
    return tOption( parent, fieldName,
        Predicates.allOf( JsonNode::isIntegralNumber, JsonNode::canConvertToInt ),
        JsonNode::asInt );
  }

  public static Long longInt( final JsonNode parent, final String fieldName ) throws IOException {
    return longIntOption( parent, fieldName ).getOrElseThrow( ifMissing( fieldName ) );
  }

  public static Option<Long> longIntOption( final JsonNode parent, final String fieldName ) throws IOException {
    return tOption( parent, fieldName,
        Predicates.allOf( JsonNode::isIntegralNumber, JsonNode::canConvertToLong ),
        JsonNode::asLong );
  }

  public static Option<List<JsonNode>> objectListOption(
      final JsonNode parent,
      final String fieldName
  ) throws IOException {
    return tListOption( parent, fieldName, optionStreamObjEmpty, ( streamOption, item ) ->
        streamOption.flatMap( stream -> item.isObject( ) ?
            Option.of( stream.append( item ) ) :
            Option.<Stream<JsonNode>>none( ) )
    );
  }

  public static List<JsonNode> objectList( final JsonNode parent, final String fieldName ) throws IOException {
    return objectListOption( parent, fieldName ).getOrElseThrow( ifMissing( fieldName ) );
  }

  public static Option<List<String>> textListOption(
      final JsonNode parent,
      final String fieldName
  ) throws IOException {
    return tListOption( parent, fieldName, optionStreamEmpty, ( streamOption, item ) ->
        streamOption.flatMap( stream -> item.isTextual( ) ?
            Option.of( stream.append( item.asText( ) ) ) :
            Option.<Stream<String>>none( ) )
    );
  }

  public static List<String> textList( final JsonNode parent, final String fieldName ) throws IOException {
    return textListOption( parent, fieldName ).getOrElseThrow( ifMissing( fieldName ) );
  }

  private static <T> Option<T> tOption(
      final JsonNode parent,
      final String fieldName,
      final Predicate<? super JsonNode> test,
      final Function<? super JsonNode,? extends T> mapper
  ) throws IOException {
    final JsonNode node = parent.get( fieldName );
    if ( node == null ) {
      return Option.none( );
    }
    else if ( test.test( node ) ) {
      return Option.of( mapper.apply( node ) );
    } else {
      throw new IOException( "Invalid content for " + fieldName );
    }
  }

  private static <T> Option<List<T>> tListOption(
      final JsonNode parent,
      final String fieldName,
      final Option<Stream<T>> emptyStream,
      final BiFunction<Option<Stream<T>>,JsonNode,Option<Stream<T>>> reducer
  ) throws IOException {
    final JsonNode node = parent.get( fieldName );
    if ( node == null ) {
      return Option.none( );
    } else if ( node.isArray( ) ) {
      return Option.of( CollectionUtils.reduce( node, emptyStream, reducer )
          .getOrElseThrow( ( ) -> new IOException( "Invalid array content for " + fieldName ) )
          .toJavaList( ) );
    } else {
      throw new IOException( "Invalid content for " + fieldName );
    }
  }

  private static Supplier<IOException> ifMissing( final String fieldName ) {
    return missingExceptionSupplierFunc.apply( fieldName );
  }

  private interface GroovyMixin {
    @JsonIgnore
    void setMetaClass( MetaClass var1);
    @JsonIgnore MetaClass getMetaClass( );
  }
}
