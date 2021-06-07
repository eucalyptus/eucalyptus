/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.base.Enums;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.Tuple;
import io.vavr.control.Option;

/**
 * Functional utility methods
 */
public class FUtils {

  /**
   * A function that applies all of the given functions to the input.
   *
   * @param mappers The mapping functions to be applied.
   * @param <R> The result item type.
   * @param <T> The input type
   * @return The function
   */
  public static <R,T> CompatFunction<T,Iterable<R>> applyAll( final Iterable<Function<? super T,? extends  R>> mappers ) {
    return t -> Stream.ofAll( mappers ).map( mapper -> mapper.apply( t ) );
  }

  /**
   * Chain functions, useful for method reference composition.
   */
  public static <I,P,R> java.util.function.Function<P,R> chain(
      final java.util.function.Function<? super P, ? extends I> f1,
      final java.util.function.Function<? super I, R> f2
  ) {
    return f2.compose( f1 );
  }

  /**
   * Negate a predicate, useful for method reference inversion.
   */
  public static <T> java.util.function.Predicate<T> negate(
      final java.util.function.Predicate<T> p
  ) {
    return p.negate( );
  }

  /**
   * Partially apply the function using the given parameter.
   *
   * @return Callable type thunk
   */
  public static <R,P> Callable<R> cpartial( final Function<? super P, ? extends R> function, final P param ) {
    return new Callable<R>( ) {
      @Override
      public R call( ) throws Exception {
        return function.apply( param );
      }
    };
  }

  /**
   * Flatten a nested optional.
   *
   * @param option The optional to flatten
   * @param <T> The resulting optional type
   * @return The optional
   */
  @Nonnull
  public static <T> Optional<T> flatten( @Nullable final Optional<? extends Optional<T>> option ) {
    if ( option != null && option.isPresent( ) ) {
      return option.get( );
    }
    return Optional.absent( );
  }

  /**
   * Replacement for the removed guava Enums#valueOfFunction
   *
   * @param enumClass
   * @param <T>
   * @return
   */
  public static <T extends Enum<T>> CompatFunction<String,T> valueOfFunction( final Class<T> enumClass ) {
    return new CompatFunction<String,T>( ){
      @Nullable
      @Override
      public T apply( @Nullable final String value ) {
        return value == null ? null : Enums.getIfPresent( enumClass, value ).orNull( );
      }
    };
  }

  /**
   * Function that calls the callback and returns the parameter.
   *
   * @param callback The Callback to call
   * @param <T> The Callback type
   * @return A function wrapping the callback
   */
  public static <T> CompatFunction<T,T> function( @Nonnull final Callback<T> callback ) {
    return new CompatFunction<T,T>( ) {
      @Nullable
      @Override
      public T apply( @Nullable final T t ) {
        callback.fire( t );
        return t;
      }
    };
  }

  /**
   * Catch any exception thrown by the function and return an either.
   *
   * @param <T> The function parameter type
   * @param <R> The function return type
   * @param function The function to call
   *
   * @return The exception handling function
   */
  public static <T,R> CompatFunction<T,Either<Throwable,R>> eitherThrowable(
      final java.util.function.Function<T,R> function
  ) {
    return t -> {
      try {
        return Either.right( function.apply( t ) );
      } catch ( final Throwable thrown ) {
        return Either.left( thrown );
      }
    };
  }

  /**
   * Catch any exception thrown by the function and return an option.
   *
   * The returned option is absent in case of exceptions or null return.
   *
   * @param <T> The function parameter type
   * @param <R> The function return type
   * @param function The function to call
   *
   * @return The exception and null handling function
   */
  public static <T,R> CompatFunction<T,Optional<R>> optional(
      final java.util.function.Function<T,R> function
  ) {
    return t -> {
      try {
        return Optional.fromNullable( function.apply( t ) );
      } catch ( final Throwable thrown ) {
        return Optional.absent( );
      }
    };
  }

  public static <T,R> CompatFunction<T,Option<R>> vOption(
      final java.util.function.Function<T,Optional<R>> function
  ) {
    return t -> Option.of( function.apply( t ).orNull( ) );
  }

  /**
   * Tuplize the result of a function with its parameter
   *
   * @param <T> The function parameter type
   * @param <R> The function return type
   * @param function The function to tuplize
   *
   * @return The exception handling function
   */
  public static <T,R> CompatFunction<T, Tuple2<T,R>> tuple(
      final java.util.function.Function<T,R> function
  ) {
    return t -> Tuple.of( t, function.apply( t ) );
  }

  /**
   * Memoize the last successful invocation with non-null parameter/result.
   *
   * @param function The function to memoize
   * @param <F> The parameter type
   * @param <T> The result type
   * @return A function that memoizes the last result
   */
  public static <F,T> CompatFunction<F,T> memoizeLast( @Nonnull final Function<F,T> function ) {
    return new CompatFunction<F, T>( ) {
      private final AtomicReference<Pair<F,T>> cached = new AtomicReference<>( );

      @Nullable
      @Override
      public T apply( @Nullable final F f ) {
        final Pair<F,T> cachedResult = cached.get( );
        if ( cachedResult != null && cachedResult.getLeft( ).equals( f ) ) {
          return cachedResult.getRight( );
        } else {
          final T result = function.apply( f );
          if ( f != null && result != null ) {
            cached.compareAndSet( cachedResult, Pair.pair( f, result ) );
          }
          return result;
        }
      }
    };
  }

  /**
   * Memoize the last successful invocation with non-null parameter/result.
   *
   * Will only call function with one thread at a time.
   *
   * @param function The function to memoize
   * @param <F> The parameter type
   * @param <T> The result type
   * @return A function that memoizes the last result
   */
  public static <F,T> CompatFunction<F,T> memoizeLastSync( @Nonnull final Function<F,T> function ) {
    return new CompatFunction<F, T>( ) {
      private final Lock lock = new ReentrantLock( );
      private final AtomicReference<Pair<F,T>> cached = new AtomicReference<>( );

      @Nullable
      @Override
      public T apply( @Nullable final F f ) {
        final Pair<F,T> cachedResult = cached.get( );
        if ( cachedResult != null && cachedResult.getLeft( ).equals( f ) ) {
          return cachedResult.getRight( );
        } else try ( final LockResource lockResource = LockResource.lock( lock ) ) {
          final Pair<F,T> syncCachedResult = cached.get( );
          if ( syncCachedResult != null && syncCachedResult.getLeft( ).equals( f ) ) {
            return syncCachedResult.getRight( );
          } else {
            final T result = function.apply( f );
            if ( f != null && result != null ) {
              cached.compareAndSet( cachedResult, Pair.pair( f, result ) );
            }
            return result;
          }
        }
      }
    };
  }
}
