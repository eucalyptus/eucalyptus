/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.util

import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.JsonNode
import io.vavr.Tuple as VTuple
import io.vavr.Tuple2 as VTuple2
import io.vavr.collection.List as VList
import org.junit.Assert
import org.junit.Test

/**
 *
 */
class YamlTest {

  @Test
  void testParseWithObject( ) {
    Yaml.parse( '{}' )
  }

  @Test
  void testParseWithText( ) {
    Yaml.parse( '"asfd"' )
  }

  @Test
  void testParseWithDocument( ) {
    Yaml.parse( '''\
    ---
    Key1: Value1
    Key2:
      - ListValue1
      - ListValue2
    ...
    '''.stripIndent( ) )
  }

  @Test
  void testParseWithTaggedDocument( ) {
    Yaml.parse( '''\
    ---
    Key1: !!str Value1
    Key2: !!seq
      - !!str ListValue1
      - !!str ListValue2
    ...
    '''.stripIndent( ) )
  }

  @Test
  void testParseVersion10Directive( ) {
    Yaml.parse( '''\
    %YAML 1.0
    ---
    Key1: Value1
    '''.stripIndent( ) )
  }

  @Test
  void testParseVersion11Directive( ) {
    Yaml.parse( '''\
    %YAML 1.1
    ---
    Key1: Value1
    '''.stripIndent( ) )
  }

  @Test
  void testParseVersion12Directive( ) {
    Yaml.parse( '''\
    %YAML 1.2
    ---
    Key1: Value1
    '''.stripIndent( ) )
  }

  @Test
  void testParseTagDirective( ) {
    Yaml.parse( '''\
    %TAG !yaml! tag:yaml.org,2002:
    ---
    !yaml!str "foo"
    '''.stripIndent( ) )
  }

  @Test
  void testRandomDirective( ) {
    Yaml.parse( '''\
    %BLAH YAML rulez
    ---
    a: b
    '''.stripIndent( ) )
  }

  @Test(expected = IOException)
  void testParseUnsafe( ) {
    Yaml.parse( '''\
    !!java.io.FileOutputStream [/tmp/unsafe.txt]
    a: b
    '''.stripIndent( ) )
  }

  @Test(expected = IOException)
  void testParseMultipleDocuments( ) {
    Yaml.parse( '''\
    ---
    a: b

    ---
    c: d
    ---
    e: f
    '''.stripIndent( ) )
  }

  @Test(expected = IOException)
  void testParseMultipleDocumentsExplicitEnd( ) {
    Yaml.parse( '''\
    ---
    a: b

    ...
    ---
    c: d
    ...
    ---
    e: f
    ...
    '''.stripIndent( ) )
  }

  @Test(expected = IOException)
  void testParseAlias( ) {
    Yaml.parse( '''\
    &A [ *A ]
    '''.stripIndent( ) )
  }

  @Test(expected = IOException)
  void testParseHashMerge( ) {
    Object foo = Yaml.parse( '''\
    defaults: &defaults
      a: b
    development:
      <<: *defaults
      c: d
    '''.stripIndent( ) )
    print foo
  }

  @Test(expected = IOException)
  void testParseTag( ) {
    Yaml.parse( '''\
    !TagGoesHere example
    '''.stripIndent( ) )
  }

  @Test
  void testBasicTags( ) {
    Yaml.parse( '''\
    explicit string: !!str abc
    explicit integer: !!int 12
    explicit float: !!float 12.1
    explicit bool: !!bool true
    '''.stripIndent( ) )
  }

  @Test
  void testBasic( ) {
    Yaml.parse( '''\
    implicit string: abc
    implicit integer: 12
    implicit float: 12.1
    implicit bool: false
    '''.stripIndent( ) )
  }

  @Test
  void testBasicCollectionsTag( ) {
    Yaml.parse( '''\
    list1: !!seq [ a, b, c ]
    list2: !!seq
      - a
      - b
      - c
    '''.stripIndent( ) )
  }

  @Test
  void testBasicCollections( ) {
    Yaml.parse( '''\
    list1: [ a, b, c ]
    list2:
      - a
      - b
      - c
    '''.stripIndent( ) )
  }

  @Test(expected = IOException)
  void testBinaryShortCanonical( ) {
    Yaml.parse( '''\
    canonical: !!binary "R0lGODlhDAAMAIQAAP//9/X17unp5WZmZgAAAOfn515eXvPz7Y6OjuDg4J+fn5OTk6enp56enmlpaWNjY6Ojo4SEhP/++f/++f/++f/++f/++f/++f/++f/++f/++f/++f/++f/++f/++f/++SH+Dk1hZGUgd2l0aCBHSU1QACwAAAAADAAMAAAFLCAgjoEwnuNAFOhpEMTRiggcz4BNJHrv/zCFcLiwMWYNG84BwwEeECcgggoBADs="
    description:
     The binary value above is a tiny arrow encoded as a gif image.
    '''.stripIndent( ) )
  }

  @Test(expected = IOException)
  void testBinaryShort( ) {
    Yaml.parse( '''\
    generic: !!binary |
     R0lGODlhDAAMAIQAAP//9/X17unp5WZmZgAAAOfn515eXvPz7Y6OjuDg4J+fn5
     OTk6enp56enmlpaWNjY6Ojo4SEhP/++f/++f/++f/++f/++f/++f/++f/++f/+
     +f/++f/++f/++f/++f/++SH+Dk1hZGUgd2l0aCBHSU1QACwAAAAADAAMAAAFLC
     AgjoEwnuNAFOhpEMTRiggcz4BNJHrv/zCFcLiwMWYNG84BwwEeECcgggoBADs=
    description:
     The binary value above is a tiny arrow encoded as a gif image.
    '''.stripIndent( ) )
  }

  @Test(expected = IOException)
  void testBinaryCanonical( ) {
    Yaml.parse( '''\
    canonical: !!binary "R0lGODlhDAAMAIQAAP//9/X17unp5WZmZgAAAOfn515eXvPz7Y6OjuDg4J+fn5OTk6enp56enmlpaWNjY6Ojo4SEhP/++f/++f/++f/++f/++f/++f/++f/++f/++f/++f/++f/++f/++f/++SH+Dk1hZGUgd2l0aCBHSU1QACwAAAAADAAMAAAFLCAgjoEwnuNAFOhpEMTRiggcz4BNJHrv/zCFcLiwMWYNG84BwwEeECcgggoBADs="
    description:
     The binary value above is a tiny arrow encoded as a gif image.
    '''.stripIndent( ) )
  }

  @Test(expected = IOException)
  void testBinary( ) {
    Yaml.parse( '''\
    generic: !!binary |
     R0lGODlhDAAMAIQAAP//9/X17unp5WZmZgAAAOfn515eXvPz7Y6OjuDg4J+fn5
     OTk6enp56enmlpaWNjY6Ojo4SEhP/++f/++f/++f/++f/++f/++f/++f/++f/+
     +f/++f/++f/++f/++f/++SH+Dk1hZGUgd2l0aCBHSU1QACwAAAAADAAMAAAFLC
     AgjoEwnuNAFOhpEMTRiggcz4BNJHrv/zCFcLiwMWYNG84BwwEeECcgggoBADs=
    description:
     The binary value above is a tiny arrow encoded as a gif image.
    '''.stripIndent( ) )
  }

  @Test
  void testTimestampsAsStrings( ) {
    Yaml.parse( '''\
    canonical: 2001-12-15T02:59:43.1Z
    iso8601: 2001-12-14t21:59:43.10-05:00
    spaced: 2001-12-14 21:59:43.10 -5
    date: 2002-12-14
    '''.stripIndent( ) )
  }

  @Test(expected = IOException)
  void testTimestamps( ) {
    Yaml.parse( '''\
    canonical: !!timestamp 2001-12-15T02:59:43.1Z
    iso8601: !!timestamp 2001-12-14t21:59:43.10-05:00
    spaced: !!timestamp 2001-12-14 21:59:43.10 -5
    date: !!timestamp 2002-12-14
    '''.stripIndent( ) )
  }

  @Test
  void testFilter( ) {
    JsonNode node1 = Yaml.parse( '''\
    a:
      foo: b
    '''.stripIndent( ) )

    final Yaml.TaggedTokenFilter filter = new Yaml.TaggedTokenFilter() {
      @Override
      VTuple2<VList<VTuple2<JsonToken,String>>, VList<VTuple2<JsonToken,String>>> filterTag(final String tag, final JsonToken token) {
        return tag == 'foo' ? VTuple.of(
            VList.of( VTuple.of( JsonToken.START_OBJECT, '' ), VTuple.of( JsonToken.FIELD_NAME, 'foo' ) ),
            VList.of( VTuple.of( JsonToken.END_OBJECT, '' ) ) ) :
            null
      }
    }
    JsonNode node2 = Yaml.parse( Yaml.mapper( filter ).reader( ), '''\
    a: !foo b
    '''.stripIndent( ) )

    Assert.assertEquals( 'Nodes', node1, node2 )
  }

  @Test
  void testFilterArray( ) {
    JsonNode node1 = Yaml.parse( '''\
    a:
      foo: [ b, c, d ]
    '''.stripIndent( ) )

    final Yaml.TaggedTokenFilter filter = new Yaml.TaggedTokenFilter() {
      @Override
      VTuple2<VList<VTuple2<JsonToken,String>>, VList<VTuple2<JsonToken,String>>> filterTag(final String tag, final JsonToken token) {
        return tag == 'foo' ? io.vavr.Tuple.of(
            VList.of( VTuple.of( JsonToken.START_OBJECT, '' ), VTuple.of( JsonToken.FIELD_NAME, 'foo' ) ),
            VList.of( VTuple.of( JsonToken.END_OBJECT, '' ) ) ) :
            null
      }
    }
    JsonNode node2 = Yaml.parse( Yaml.mapper( filter ).reader( ), '''\
    a: !foo [ b, c, d ]
    '''.stripIndent( ) )

    Assert.assertEquals( 'Nodes', node1, node2 )
  }

  @Test
  void testFilterArrayNesting( ) {
    JsonNode node1 = Yaml.parse( '''\
    a:
      foo:
        - b
        - foo: c
        - [ d ]
    '''.stripIndent( ) )

    println node1

    final Yaml.TaggedTokenFilter filter = new Yaml.TaggedTokenFilter() {
      @Override
      VTuple2<VList<VTuple2<JsonToken,String>>, VList<VTuple2<JsonToken,String>>> filterTag(final String tag, final JsonToken token) {
        return tag == 'foo' ? io.vavr.Tuple.of(
            VList.of( VTuple.of( JsonToken.START_OBJECT, '' ), VTuple.of( JsonToken.FIELD_NAME, 'foo' ) ),
            VList.of( VTuple.of( JsonToken.END_OBJECT, '' ) ) ) :
            null
      }
    }
    JsonNode node2 = Yaml.parse( Yaml.mapper( filter ).reader( ), '''\
    a: !foo [ b, !foo c, [ d ] ]
    '''.stripIndent( ) )

    println node2

    Assert.assertEquals( 'Nodes', node1, node2 )
  }
}
