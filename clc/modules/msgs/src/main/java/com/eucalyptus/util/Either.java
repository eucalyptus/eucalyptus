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
package com.eucalyptus.util;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import javax.annotation.Nonnull;
import com.google.common.base.Optional;

/**
 *
 */
public abstract class Either<L,R> {

  @Nonnull
  public static <L,R> Either<L,R> left( @Nonnull final L left ) {
    return new Left<L,R>( left );
  }

  @Nonnull
  public static <L,R> Either<L,R> right( @Nonnull final R right ) {
    return new Right<L,R>( right );
  }

  public abstract boolean isRight( );

  public final boolean isLeft( ) {
    return !isRight( );
  }

  @Nonnull
  public abstract L getLeft( );

  @Nonnull
  public abstract R getRight( );

  public Optional<L> getLeftOption( ) {
    return isLeft( ) ?
        Optional.of( getLeft( ) ) :
        Optional.<L>absent( );
  }

  public Optional<R> getRightOption( ) {
    return isRight() ?
        Optional.of( getRight() ) :
        Optional.<R>absent( );
  }

  public static <L,R> NonNullFunction<Either<L,R>,Optional<L>> leftOption( ) {
    return new NonNullFunction<Either<L, R>, Optional<L>>() {
      @Nonnull
      @Override
      public Optional<L> apply( final Either<L, R> either ) {
        return either.getLeftOption( );
      }
    };
  }

  public static <L,R> NonNullFunction<Either<L,R>,Optional<R>> rightOption( ) {
    return new NonNullFunction<Either<L, R>, Optional<R>>() {
      @Nonnull
      @Override
      public Optional<R> apply( final Either<L, R> either ) {
        return either.getRightOption();
      }
    };
  }

  private static final class Right<L,R> extends Either<L,R> {
    @Nonnull private final R right;

    private Right( final @Nonnull R right ) {
      Parameters.checkParam( "right", right, notNullValue( ) );
      this.right = right;
    }

    @Override
    public boolean isRight( ) {
      return true;
    }

    @Nonnull
    @Override
    public L getLeft( ) {
      throw new IllegalStateException( "Left value not present" );
    }

    @Nonnull
    @Override
    public R getRight( ) {
      return right;
    }
  }

  private static final class Left<L,R> extends Either<L,R> {
    @Nonnull private final L left;

    private Left( final @Nonnull L left ) {
      Parameters.checkParam( "left", left, notNullValue( ) );
      this.left = left;
    }

    @Override
    public boolean isRight( ) {
      return false;
    }

    @Nonnull
    @Override
    public L getLeft( ) {
      return left;
    }

    @Nonnull
    @Override
    public R getRight ( ) {
      throw new IllegalStateException( "Right value not present" );
    }
  }
}
