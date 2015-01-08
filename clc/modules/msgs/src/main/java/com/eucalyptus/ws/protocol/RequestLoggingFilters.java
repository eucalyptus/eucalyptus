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
package com.eucalyptus.ws.protocol;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import com.google.common.base.Function;
import com.google.common.base.Functions;

/**
 *
 */
public class RequestLoggingFilters {

  private static final AtomicReference<RequestLoggingFilter> filter = new AtomicReference<>(
    forFunction( Functions.<Collection<String>>identity( ) )
  );

  public static RequestLoggingFilter get( ) {
    return filter.get( );
  }

  public static void register( final RequestLoggingFilter requestLoggingFilter ) {
    RequestLoggingFilter current;
    RequestLoggingFilter chained;
    do {
      current = filter.get( );
      chained = forFunction( Functions.compose( requestLoggingFilter, current ) );
    } while ( !filter.compareAndSet( current, chained ) );
  }

  public static RequestLoggingFilter forFunction( Function<? super Collection<String>, Collection<String>> filter ) {
    return new RequestLoggingFilterFunction( filter );
  }

  private static final class RequestLoggingFilterFunction implements RequestLoggingFilter {
    private final Function<? super Collection<String>,Collection<String>> filter;

    private RequestLoggingFilterFunction( final Function<? super Collection<String>, Collection<String>> filter ) {
      this.filter = filter;
    }

    @Override
    public Collection<String> apply( final Collection<String> parametersOrBody ) {
      return filter.apply( parametersOrBody );
    }
  }
}
