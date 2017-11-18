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
package com.eucalyptus.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import io.vavr.Function1;
import io.vavr.Predicates;
import io.vavr.Value;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

/**
 * Utility class for JSON processing with Jackson.
 */
public class Json {
  private static final ObjectMapper mapper = mapper( );
  private static final ObjectReader reader = mapper.reader( );
  private static final ObjectWriter writer = mapper.writer( );
  private static final Option<Stream<String>> optionStreamEmpty = Option.of( Stream.empty( ) );
  private static final Option<Stream<JsonNode>> optionStreamObjEmpty = Option.of( Stream.empty( ) );
  private static final Function1<String,Supplier<IOException>> missingExceptionSupplierFunc =
      fieldName -> () -> new IOException( "Missing required value " + fieldName );

  public enum JsonOption {
    IgnoreGroovy {
      @Override
      ObjectMapper config( final ObjectMapper objectMapper ) {
        objectMapper.addMixIn( GroovyObject.class, GroovyMixin.class );
        return objectMapper;
      }
    },
    IgnoreBaseMinimalMessage {
      @Override
      ObjectMapper config( final ObjectMapper objectMapper ) {
        objectMapper.addMixIn( BaseMessage.class, BaseMessageMinimalMixin.class );
        return objectMapper;
      }
    },
    IgnoreBaseMessage {
      @Override
      ObjectMapper config( final ObjectMapper objectMapper ) {
        objectMapper.addMixIn( BaseMessage.class, BaseMessageMixin.class );
        return objectMapper;
      }
    },
    IgnoreVavr {
      @Override
      ObjectMapper config( final ObjectMapper objectMapper ) {
        objectMapper.addMixIn( Value.class, VavrMixin.class );
        return objectMapper;
      }
    },
    ;

    abstract ObjectMapper config( final ObjectMapper mapper );

    static ObjectMapper config( final Iterable<JsonOption> options, final ObjectMapper objectMapper ) {
      ObjectMapper configured = objectMapper;
      for ( final JsonOption option : options ) {
        configured = option.config( configured );
      }
      return configured;
    }
  }

  public static JsonNode parse( final InputStream jsonStream ) throws IOException {
    if ( jsonStream == null ) throw new IOException( "Null" );
    final JsonParser parser = reader.getFactory( ).createParser( jsonStream );
    return parse( parser );
  }

  public static JsonNode parse( final String jsonText ) throws IOException {
    if ( jsonText == null ) throw new IOException( "Null" );
    final JsonParser parser = reader.getFactory( ).createParser( new StringReader( jsonText ) {
      @Override public String toString() { return "json"; } // overridden for better source in error message
    } );
    return parse( parser );
  }

  public static JsonNode parse( final JsonParser parser ) throws IOException {
    if ( parser == null ) throw new IOException( "Null" );
    final JsonNode node = reader.readTree( parser );
    if ( node == null ) {
      throw new IOException( "No content at " + parser.getCurrentLocation( ) );
    }
    boolean trailingContent;
    try {
      trailingContent = parser.nextToken( ) != null;
    } catch ( IOException e ) {
      trailingContent = true;
    }
    if ( trailingContent ) {
      throw new IOException( "Unexpected trailing content at " + parser.getCurrentLocation( ) );
    }
    return node;
  }

  public static ObjectNode parseObject( final String jsonText ) throws IOException {
    final JsonNode node = parse( jsonText );
    if ( !node.isObject( ) ) {
      throw new IOException( "Invalid object" );
    }
    return (ObjectNode) node;
  }

  public static void writeObject( final OutputStream out, final Object object ) throws IOException {
    writer.writeValue( out, object );
  }

  public static String writeObjectAsString( final Object object ) throws IOException {
    return writer.writeValueAsString( object );
  }

  /**
   * Get a mapper configured to ignore groovy object properties.
   */
  public static ObjectMapper mapper( ) {
    return JsonOption.IgnoreGroovy.config( new ObjectMapper( ) );
  }

  /**
   * Get a mapper configured with the given options.
   */
  public static ObjectMapper mapper( final Iterable<JsonOption> options ) {
    return JsonOption.config( options, new ObjectMapper( ) );
  }

  /**
   * Get a mapper configured with the given options.
   */
  public static <O extends ObjectMapper> O mapper( final O mapper, JsonOption... options ) {
    JsonOption.config( Arrays.asList( options ), mapper );
    return mapper;
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

  @JsonIgnoreProperties( "reply" )
  private interface BaseMessageMinimalMixin {
  }

  @JsonIgnoreProperties( { "correlationId", "effectiveUserId", "reply", "statusMessage", "userId",
      "_disabledServices", "_notreadyServices", "_stoppedServices", "_epoch", "_services", "_return",
      "callerContext" } )
  private interface BaseMessageMixin {
  }

  @JsonIgnoreProperties( { "async", "defined", "distinct", "empty", "lazy", "memoized", "ordered",
      "sequential", "singleValued", "traversableAgain" } )
  private interface VavrMixin {
  }
}
