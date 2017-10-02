/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
