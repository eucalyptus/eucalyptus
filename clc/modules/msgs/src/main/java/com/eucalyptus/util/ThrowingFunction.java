/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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

/**
 *
 */
@FunctionalInterface
public interface ThrowingFunction<T,R,E extends Throwable> {

  R apply( T t ) throws E;

  static <T,R> CompatFunction<T,R> undeclared( ThrowingFunction<T,R,?> function ) {
    return function.asUndeclaredFunction( );
  }

  default CompatFunction<T,R> asUndeclaredFunction( ) {
    return (T t) -> {
      try {
        return apply( t );
      } catch ( final Throwable throwable ) {
        throw Exceptions.toUndeclared( throwable );
      }
    };
  }

  default <V> ThrowingFunction<V, R, E> compose(Function<? super V, ? extends T> before) {
    Parameters.checkParamNotNull("before", before);
    return (V v) -> apply(before.apply(v));
  }

  default <V> ThrowingFunction<V, R, E> compose(ThrowingFunction<? super V, ? extends T, E> before) {
    Parameters.checkParamNotNull("before", before);
    return (V v) -> apply(before.apply(v));
  }

  default <V> ThrowingFunction<T, V, E> andThen(Function<? super R, ? extends V> after) {
    Parameters.checkParamNotNull("after", after);
    return (T t) -> after.apply(apply(t));
  }

  default <V> ThrowingFunction<T, V, E> andThen(ThrowingFunction<? super R, ? extends V, E> after) {
    Parameters.checkParamNotNull("after", after);
    return (T t) -> after.apply(apply(t));
  }
}
