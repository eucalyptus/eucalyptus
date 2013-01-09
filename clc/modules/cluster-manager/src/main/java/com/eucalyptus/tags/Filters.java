/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
package com.eucalyptus.tags;

import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import com.eucalyptus.cloud.CloudMetadata;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Methods for working with {@link Filter}s
 */
public class Filters {

  /**
   * Generate a Filter for the given filters.
   *
   * @param filters The filter items
   * @param resourceType The resource class to be filtered
   * @return The filter
   */
  @Nonnull
  public static Filter generate( final Iterable<edu.ucsb.eucalyptus.msgs.Filter> filters,
                                 final Class<? extends CloudMetadata> resourceType ) {
    final Filter filter;

    final FilterSupport support = FilterSupport.forResource( resourceType );
    if ( support == null ) {
      filter = Filter.alwaysTrue();
    } else {
      filter = support.generate( toMap( filters ) );
    }

    return filter;
  }

  private static Map<String, Set<String>> toMap( final Iterable<edu.ucsb.eucalyptus.msgs.Filter> filters ) {
    final ImmutableMap.Builder<String,Set<String>> filterMapBuilder = ImmutableMap.builder();

    for ( final edu.ucsb.eucalyptus.msgs.Filter filter : filters ) {
      final Set<String> values = ImmutableSet.copyOf( filter.getValueSet() );
      filterMapBuilder.put( filter.getName(), values );
    }

    return filterMapBuilder.build();
  }

}
