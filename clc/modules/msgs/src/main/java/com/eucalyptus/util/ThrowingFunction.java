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

/**
 *
 */
@FunctionalInterface
public interface ThrowingFunction<T,R,E extends Throwable> {

  R apply( T t ) throws E;

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
