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
package com.eucalyptus.util;

import java.util.function.Function;
import javax.annotation.Nonnull;

/**
 * Composable lens for view/update of object hierarchy
 */
public class Lens<T,P> {

  private final Function<T,P> get;
  private final Function<P,Function<T,T>> set;

  private Lens( final Function<T, P> get, final Function<P, Function<T, T>> set ) {
    this.get = Assert.notNull( get, "get" );
    this.set = Assert.notNull( set, "set" );
  }

  @Nonnull
  public static <T,P> Lens<T,P> of(
      @Nonnull final Function<T, P> get,
      @Nonnull final Function<P, Function<T, T>> set
  ) {
    return new Lens<>( get, set );
  }

  @Nonnull
  public static <T> Lens<T,T> identity( ) {
    return of( Function.identity( ), p -> t -> p );
  }

  @Nonnull
  public P get( @Nonnull final T t ) {
    return get.apply( t );
  }

  @Nonnull
  public Function<T,T> set( @Nonnull final P p ) {
    return set.apply( p );
  }

  @Nonnull
  public Function<T,T> modify( @Nonnull final Function<P,P> f ) {
    return t -> set( f.apply( get( t ) ) ).apply( t );
  }

  @Nonnull
  public <Z> Lens<T,Z> compose( @Nonnull final Lens<P,Z> lens ) {
    return of(
        lens.get.compose( get ),
        z -> t -> set( lens.set( z ).apply( get( t ) ) ).apply( t )
    );
  }
}
