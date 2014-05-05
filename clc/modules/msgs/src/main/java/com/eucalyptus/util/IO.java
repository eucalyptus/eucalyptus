/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

import java.io.Closeable;
import java.util.List;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 *
 */
public class IO {

  /**
   * Close given closables, return errors.
   *
   * @param closeables The closeables to close
   * @return The error list (same length as closeables)
   * @see Optional#presentInstances(Iterable)
   */
  public static Iterable<Optional<Throwable>> close( final Iterable<? extends Closeable> closeables ) {
    final List<Optional<Throwable>> results = Lists.newArrayList( );
    if ( closeables != null ) for ( final Closeable closeable : closeables ) {
      Optional<Throwable> error = Optional.absent( );
      try {
        closeable.close( );
      } catch ( Throwable e ) {
        error = Optional.of( e );
      }
      results.add( error );
    }
    return results;
  }
}
