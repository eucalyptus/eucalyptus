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
package com.eucalyptus.tokens.policy;

import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.util.Exceptions;

/**
 *
 */
public class ExternalIdContext {

  private final static ThreadLocal<String> externalIdLocal = new ThreadLocal<String>();

  public static <R,T extends Throwable> R doWithExternalId( @Nullable final String externalId,
                                                            @Nonnull final Class<T> thrown,
                                                            @Nonnull final Callable<R> callback ) throws T {
    final String previousId = externalIdLocal.get();
    externalIdLocal.set( externalId );
    try {
      return callback.call();
    } catch ( final Exception e ) {
      final T rethrow = Exceptions.findCause( e, thrown );
      if ( rethrow != null ) {
        throw rethrow;
      } else {
        throw Exceptions.toUndeclared( e );
      }
    } finally {
      externalIdLocal.set( previousId );
    }
  }

  @Nullable
  static String getExternalId() {
    return externalIdLocal.get();
  }
}
