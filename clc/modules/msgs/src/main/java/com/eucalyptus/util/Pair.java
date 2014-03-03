/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.util;

import static org.hamcrest.Matchers.notNullValue;
import static com.eucalyptus.util.Parameters.checkParam;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 * @see com.google.common.base.Optional Optional - for null support
 */
public class Pair<L,R> {

  private final L left;
  private final R right;

  /**
   * @see #pair(Object, Object)
   * @see #opair(Object, Object)
   * @see #lopair(Object, Object)
   * @see #ropair(Object, Object)
   */
  public Pair( @Nonnull final L left, @Nonnull final R right ) {
    this.left = checkParam( "left", left, notNullValue() );
    this.right = checkParam( "right", right, notNullValue() );
  }

  /**
   * Convenience constructor
   *
   * @param left The left value
   * @param right The right value
   * @param <L> The left type
   * @param <R> The right type
   * @return The new pair
   */
  public static <L,R> Pair<L,R> pair( @Nonnull final L left, @Nonnull final R right ) {
    return new Pair<>( left, right );
  }

  /**
   * Convenience constructor for an optional pair.
   *
   * @param left The left value
   * @param right The right value
   * @param <L> The left type
   * @param <R> The right type
   * @return The new pair
   */
  public static <L,R> Pair<Optional<L>,Optional<R>> opair( @Nullable final L left, @Nullable final R right ) {
    return new Pair<>(
        Optional.fromNullable( left ),
        Optional.fromNullable( right ) );
  }

  /**
   * Convenience constructor for a left optional pair.
   *
   * @param left The left value
   * @param right The right value
   * @param <L> The left type
   * @param <R> The right type
   * @return The new pair
   */
  public static <L,R> Pair<Optional<L>,R> lopair( @Nullable final L left, @Nonnull final R right ) {
    return new Pair<>(
        Optional.fromNullable( left ),
        right );
  }

  /**
   * Convenience constructor for a right optional pair.
   *
   * @param left The left value
   * @param right The right value
   * @param <L> The left type
   * @param <R> The right type
   * @return The new pair
   */
  public static <L,R> Pair<L,Optional<R>> ropair( @Nonnull final L left, @Nullable final R right ) {
    return new Pair<>(
        left,
        Optional.fromNullable( right ) );
  }

  @Nonnull
  public L getLeft( ) {
    return left;
  }

  @Nonnull
  public R getRight( ) {
    return right;
  }

  @Nonnull
  public static <L,R> Function<Pair<L,R>,L> left( ) {
    return new PairLeftExtractor<>( );
  }

  @Nonnull
  public static <L,R> Function<Pair<L,R>,R> right( ) {
    return new PairRightExtractor<>( );
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    final Pair pair = (Pair) o;

    if ( !left.equals( pair.left ) ) return false;
    if ( !right.equals( pair.right ) ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = left.hashCode();
    result = 31 * result + right.hashCode();
    return result;
  }

  @Override
  public String toString( ) {
    return Objects.toStringHelper( this )
        .add( "left", left )
        .add( "right", right )
        .toString( );
  }

  private static class PairLeftExtractor<L,R> implements Function<Pair<L,R>,L> {
    @Override
    public L apply( final Pair<L, R> pair ) {
      return pair.getLeft();
    }
  }

  private static class PairRightExtractor<L,R> implements Function<Pair<L,R>,R> {
    @Override
    public R apply( final Pair<L, R> pair ) {
      return pair.getRight( );
    }
  }
}
