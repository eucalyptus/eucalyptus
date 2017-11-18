/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

import static org.hamcrest.Matchers.notNullValue;
import static com.eucalyptus.util.Parameters.checkParam;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;

/**
 * @see com.google.common.base.Optional Optional - for null support
 */
public class Pair<L,R> {

  private final L left;
  private final R right;

  /**
   * @see #of(Object, Object)
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
   * Curried pair constructor function.
   *
   * @param <L> The left type
   * @param <R> The right type
   * @return The pair function
   */
  @Nonnull
  public static <L,R> NonNullFunction<L,Function<R, Pair<L,R>>> pair( ) {
    return new NonNullFunction<L,Function<R, Pair<L,R>>>( ){
      @Override
      public Function<R, Pair<L, R>> apply( final L left ) {
        return new Function<R, Pair<L, R>>( ){
          @Override
          public Pair<L, R> apply( final R right ) {
            return pair( left, right );
          }
        };
      }
    };
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
  public static <L,R> Pair<L,R> of( @Nonnull final L left, @Nonnull final R right ) {
    return new Pair<>( left, right );
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

  @Nonnull
  public static <T,L,R> NonNullFunction<T,Pair<L,R>> builder( @Nonnull final Function<? super T, L> leftFunction,
                                                              @Nonnull final Function<? super T, R> rightFunction ) {
    return new NonNullFunction<T,Pair<L,R>>( ) {
      @SuppressWarnings( "ConstantConditions" )
      @Nonnull
      @Override
      public Pair<L,R> apply( final T value ) {
        return Pair.pair( leftFunction.apply( value ), rightFunction.apply( value ) );
      }
    };
  }

  @Nonnull
  public static <T,L,R> NonNullFunction<T,Pair<L,Optional<R>>> robuilder(
      @Nonnull final Function<? super T, L> leftFunction,
      @Nonnull final Function<? super T, R> rightFunction ) {
    return new NonNullFunction<T,Pair<L,Optional<R>>>( ) {
      @SuppressWarnings( "ConstantConditions" )
      @Nonnull
      @Override
      public Pair<L,Optional<R>> apply( final T value ) {
        return Pair.pair( leftFunction.apply( value ), Optional.fromNullable( rightFunction.apply( value ) ) );
      }
    };
  }

  public static <L,R> NonNullFunction<R,Iterable<Pair<L,R>>> explodeLeft( final Iterable<L> leftValues ) {
    return new NonNullFunction<R,Iterable<Pair<L,R>>>( ) {
      @SuppressWarnings( "ConstantConditions" )
      @Nonnull
      @Override
      public Iterable<Pair<L,R>> apply( final R rightValue ) {
        return Iterables.transform( leftValues, CollectionUtils.flipCurried( Pair.<L,R>pair( ) ).apply( rightValue ) );
      }
    };
  }

  public static <L,R> NonNullFunction<L,Iterable<Pair<L,R>>> explodeRight( final Iterable<R> rightValues ) {
    return new NonNullFunction<L,Iterable<Pair<L,R>>>( ) {
      @SuppressWarnings( "ConstantConditions" )
      @Nonnull
      @Override
      public Iterable<Pair<L,R>> apply( final L leftValue ) {
        return Iterables.transform( rightValues, Pair.<L,R>pair().apply( leftValue ) );
      }
    };
  }

  public static <L,R,V> NonNullFunction<Pair<L,R>,V> transformer(
      final Function<L,? extends Function<R,V>> pairTransform
  ) {
    return new NonNullFunction<Pair<L, R>, V>( ) {
      @SuppressWarnings( "ConstantConditions" )
      @Nonnull
      @Override
      public V apply( final Pair<L, R> pair ) {
        return pairTransform.apply( pair.getLeft( ) ).apply( pair.getRight( ) );
      }
    };
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
