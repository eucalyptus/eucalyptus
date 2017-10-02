/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.util

import groovy.transform.Immutable
import org.junit.Test
import static org.junit.Assert.*

import java.util.function.Function

/**
 *
 */
class LensTest {

  @Test
  void testBasic( ) {
    Bar bar = new Bar( 'baz value' )
    Lens<Bar,String> bazl = Lens.of(
        { Bar b -> b.baz } as Function<Bar,String>,
        { String bazVal -> { Bar b -> new Bar( bazVal ) } as Function<Bar,Bar> } as Function<String,Function<Bar,Bar>>
    )
    assertEquals( 'lens value', 'baz value', bazl.get( bar ) )
    assertEquals( 'bar updated', new Bar( 'baz updated' ), bazl.set( 'baz updated' ).apply( bar ) )
  }

  @Test
  void testIdentity( ) {
    String value = 'value'
    Lens<String,String> idl = Lens.identity( )
    assertEquals( 'lens value', 'value', idl.get( value ) )
    assertEquals( 'value updated', 'updated', idl.set( 'updated' ).apply( value ) )
  }

  @Test
  void testModify( ) {
    Bar bar = new Bar( 'baz value' )
    Lens<Bar,String> bazl = Lens.of(
        { Bar b -> b.baz } as Function<Bar,String>,
        { String bazVal -> { Bar b -> new Bar( bazVal ) } as Function<Bar,Bar> } as Function<String,Function<Bar,Bar>>
    )
    assertEquals( 'bar modified', new Bar( 'baz updated' ), bazl.modify( { 'baz updated' } ).apply( bar ) )
  }

  @Test
  void testCompose( ) {
    Bar bar = new Bar( 'baz value' )
    Foo foo = new Foo( bar )
    Lens<Bar,String> bazl = Lens.of(
        { Bar b -> b.baz } as Function<Bar,String>,
        { String bazVal -> { Bar b -> new Bar( bazVal ) } as Function<Bar,Bar> } as Function<String,Function<Bar,Bar>>
    )
    Lens<Foo,Bar> fool = Lens.of(
        { Foo f -> f.bar } as Function<Foo,Bar>,
        { Bar barVal -> { Foo f -> new Foo( barVal ) } as Function<Foo,Foo> } as Function<Bar,Function<Foo,Foo>>
    )
    Lens<Foo,String> compl = fool.compose(bazl)
    Foo foo2 =
    assertEquals( 'lens value', 'baz value', compl.get( foo ) )
    assertEquals( 'value updated', new Foo( new Bar( 'baz updated' ) ), compl.set( 'baz updated' ).apply( foo ) )
  }

  @Immutable
  static class Foo {
    Bar bar
  }

  @Immutable
  static class Bar {
    String baz
  }
}
