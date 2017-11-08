/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
