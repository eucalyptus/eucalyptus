/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
