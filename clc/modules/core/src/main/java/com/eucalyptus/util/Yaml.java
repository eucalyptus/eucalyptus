/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.util;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.collection.Stream;
import io.vavr.control.Option;


/**
 * YAML 1.1 utilities
 *
 * http://yaml.org/type/index.html
 * http://yaml.org/spec/current.html (i.e. v1.1)
 */
public class Yaml {
  private static final TaggedTokenFilter noopFilter = ( tag, token ) -> null;
  private static final ObjectMapper mapper = mapper( );
  private static final ObjectReader reader = mapper.reader( );

  /**
   * Types for yaml.org, e.g. tag:yaml.org,2002:str
   *
   *   !!map
   *   !!omap
   *   !!pairs
   *   !!set
   *   !!seq
   *
   *   !!binary
   *   !!bool
   *   !!float
   *   !!int
   *   !!merge
   *   !!null
   *   !!str
   *   !!timestamp
   *   !!value
   *   !!yaml
   *
   * we allow a subset of the above.
   */
  private static final Set<String> PERMITTED_TAGS = Stream.of(
      "map",
      "seq",
      "bool",
      "float",
      "int",
      "str"
  ).map( Strings.prepend( "tag:yaml.org,2002:" ) ).toSet( );

  public static JsonNode parse( final InputStream yamlStream ) throws IOException {
    return parse( reader, yamlStream );
  }

  public static JsonNode parse( final ObjectReader reader, final InputStream yamlStream ) throws IOException {
    if ( yamlStream == null ) throw new IOException( "Null" );
    final JsonParser parser = reader.getFactory( ).createParser( yamlStream );
    return parse( parser );
  }

  public static JsonNode parse( final String yamlText ) throws IOException {
    return parse( reader, yamlText );
  }

  public static JsonNode parse( final ObjectReader reader, final String yamlText ) throws IOException {
    if ( yamlText == null ) throw new IOException( "Null" );
    final JsonParser parser = reader.getFactory( ).createParser( new StringReader( yamlText ) {
      @Override public String toString() { return "yaml"; } // overridden for better source in error message
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

  /**
   * Get a new mapper
   */
  public static ObjectMapper mapper( ) {
    return mapper(noopFilter);
  }

  /**
   * Get a new mapper
   */
  public static ObjectMapper mapper( final TaggedTokenFilter filter ) {
    return new ObjectMapper(new EucaYAMLFactory(filter));
  }

  /**
   * Filter a token stream during document parse.
   */
  public interface TaggedTokenFilter {
    /**
     * Filter a tagged token by adding tokens/names to the stream before or after a tagged token.
     *
     * @param tag The application specific tag to process
     * @param token The JsonToken with the tag
     * @return null if tag not supported, empty lists for no changes
     */
    Tuple2<List<Tuple2<JsonToken,String>>, List<Tuple2<JsonToken,String>>> filterTag( String tag, JsonToken token );
  }

  /**
   * Factory allowing customization of yaml parser configuration
   */
  private static class EucaYAMLFactory extends YAMLFactory {
    private static final long serialVersionUID = 1L;
    private final TaggedTokenFilter tokenFilter;

    private EucaYAMLFactory( final TaggedTokenFilter tokenFilter ) {
      this.tokenFilter = tokenFilter;
    }

    @Override
    protected YAMLParser _createParser( final InputStream in, final IOContext ctxt ) throws IOException {
      return new EucaYAMLParser(tokenFilter, ctxt, _getBufferRecycler(), _parserFeatures,
          _yamlParserFeatures, _objectCodec, _createReader(in, null, ctxt));
    }

    @Override
    protected YAMLParser _createParser( final Reader r, final IOContext ctxt ) {
      return new EucaYAMLParser(tokenFilter, ctxt, _getBufferRecycler(), _parserFeatures,
          _yamlParserFeatures, _objectCodec, r);
    }

    @Override
    protected YAMLParser _createParser( final char[] data, final int offset, final int len, final IOContext ctxt,
                                        final boolean recyclable ) {
      return new EucaYAMLParser(tokenFilter, ctxt, _getBufferRecycler(), _parserFeatures,
          _yamlParserFeatures, _objectCodec, new CharArrayReader(data, offset, len));
    }

    @Override
    protected YAMLParser _createParser( final byte[] data, final int offset, final int len,
                                        final IOContext ctxt ) throws IOException {
      return new EucaYAMLParser(tokenFilter, ctxt, _getBufferRecycler(), _parserFeatures,
          _yamlParserFeatures, _objectCodec, _createReader(data, offset, len, null, ctxt));
    }
  }

  /**
   * Parser that disallows aliases and unsafe yaml functionality
   */
  private static class EucaYAMLParser extends YAMLParser {
    private final Deque<List<Tuple2<JsonToken,String>>> tokensStack = new ArrayDeque<>( );
    private final Deque<Tuple2<JsonToken,String>> tokenStack = new ArrayDeque<>( );
    private final TaggedTokenFilter tokenFilter;
    private Option<String> currentNameOverride = Option.none();

    public EucaYAMLParser( final TaggedTokenFilter tokenFilter, final IOContext ctxt, final BufferRecycler br,
                           final int parserFeatures, final int formatFeatures, final ObjectCodec codec,
                           final Reader reader ) {
      super( ctxt, br, parserFeatures, formatFeatures, codec, reader );
      this.tokenFilter = tokenFilter;
    }

    @Override
    public String getCurrentName( ) throws IOException {
      return currentNameOverride.isDefined( ) ? currentNameOverride.get( ) : super.getCurrentName( );
    }

    @Override
    public JsonToken nextToken( ) throws IOException {
      JsonToken token;

      if( tokenStack.isEmpty( ) ) {
        currentNameOverride = Option.none( );
        token = super.nextToken( );
        if ( isCurrentAlias( ) ) {
          throw new IOException( "Alias not supported" );
        }
        final JsonToken processingToken = token;
        final String typeId = getTypeId( );
        List<Tuple2<JsonToken,String>> pushTokens = List.empty( );
        if ( typeId != null && !typeId.startsWith( "tag:" ) ) {
          final Tuple2<List<Tuple2<JsonToken,String>>, List<Tuple2<JsonToken,String>>> filterResult =
              tokenFilter.filterTag( typeId, token );
          if ( filterResult == null ) {
            throw new IOException( "Unsupported tag: " + typeId );
          }
          if ( !filterResult._1( ).isEmpty( ) ) {
            final Tuple2<JsonToken,String> append = Tuple.of(token, getCurrentName());
            token = filterResult._1( ).get( 0 )._1( );
            currentNameOverride = Option.some( filterResult._1( ).get( 0 )._2( ) );
            filterResult._1( ).slice( 1, filterResult._1( ).size( ) ).forEach( tokenStack::addLast );
            tokenStack.addLast( append );
          }
          if ( !filterResult._2( ).isEmpty( ) ) {
            if ( processingToken.isStructStart( ) ) {
              pushTokens = filterResult._2( );
            } else {
              filterResult._2( ).forEach( tokenStack::addLast );
            }
          }
        } else if ( typeId != null && !isPermittedTag( typeId ) ) {
          throw new IOException( "Unsupported type: " + typeId );
        }
        if ( processingToken !=null && processingToken.isStructStart( ) &&
            (!pushTokens.isEmpty( ) || !tokensStack.isEmpty( ) ) ) {
          tokensStack.addFirst( pushTokens );
        } else if ( token != null && token.isStructEnd( ) && !tokensStack.isEmpty() ) {
          tokensStack.pop( ).forEach( tokenStack::addLast );
        }
      } else {
        final Tuple2<JsonToken,String> tokenAndName = tokenStack.pop( );
        token = tokenAndName._1( );
        currentNameOverride = Option.some( tokenAndName._2( ) );
      }
      return token;
    }

    private boolean isPermittedTag( final String tag ) {
      return PERMITTED_TAGS.contains( tag );
    }
  }
}
