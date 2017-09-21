/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
package com.eucalyptus.compute.common.internal.tags;

import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

/**
 * Methods for working with {@link Filter}s
 */
public class Filters {

  public static final String DEFAULT_FILTERS = "default";

  /**
   * Generate a Filter for the given filters.
   *
   * @param filters The filter items
   * @param resourceType The resource class to be filtered
   * @return The filter
   * @throws InvalidFilterException If a filter is invalid
   */
  @Nonnull
  public static Filter generate( final Iterable<com.eucalyptus.compute.common.Filter> filters,
                                 final Class<?> resourceType ) throws InvalidFilterException {
    return generate( filters, resourceType, DEFAULT_FILTERS );
  }

  /**
    * Generate a Filter for the given filters.
    *
    * @param filters The filter items
    * @param resourceType The resource class to be filtered
    * @param qualifier The filter qualifier (in case of multiple filter sets for a type)
    * @return The filter
    * @throws InvalidFilterException If a filter is invalid
    */
  @Nonnull
  public static Filter generate( final Iterable<com.eucalyptus.compute.common.Filter> filters,
                                 final Class<?> resourceType,
                                 final String qualifier ) throws InvalidFilterException {
    return generateFor( filters, resourceType, qualifier ).generate();
  }


  /**
   * Get a FiltersBuilder for the given filters.
   *
   * @param filters The filter items
   * @param resourceType The resource class to be filtered
   * @return The filter
   */
  @Nonnull
  public static FiltersBuilder generateFor( final Iterable<com.eucalyptus.compute.common.Filter> filters,
                                            final Class<?> resourceType ) {
    return  generateFor( filters, resourceType, DEFAULT_FILTERS );
  }

  /**
   * Get a FiltersBuilder for the given filters.
   *
   * @param filters The filter items
   * @param resourceType The resource class to be filtered
   * @param qualifier The filter qualifier (in case of multiple filter sets for a type)
   * @return The filter
   */
  @Nonnull
  public static FiltersBuilder generateFor( final Iterable<com.eucalyptus.compute.common.Filter> filters,
                                            final Class<?> resourceType,
                                            final String qualifier ) {
    return new FiltersBuilder( filters, resourceType, qualifier );
  }

  /**
   * Escape any wildcards in a filter value so it can be used as a literal.
   *
   * <p>Escapes \ * and ? using a \<\p>
   *
   * @param filterValue The value to escape
   * @return The escaped filter value
   */
  @Nonnull
  public static String escape( @Nonnull final CharSequence filterValue ) {
    final String escaped;
    final CharMatcher syntaxMatcher = CharMatcher.anyOf("\\*?");
    if ( syntaxMatcher.matchesAnyOf( filterValue ) ) {
      final StringBuilder escapedBuffer = new StringBuilder( filterValue.length( ) + 8 );
      for ( int i=0; i<filterValue.length(); i++ ) {
        final char character = filterValue.charAt( i );
        switch ( character ) {
          case '\\':
          case '*':
          case '?':
            escapedBuffer.append( '\\' );
            // fall through
          default:
            escapedBuffer.append( character );
        }
      }
      escaped = escapedBuffer.toString( );
    } else {
      escaped = filterValue.toString( );
    }

    return escaped;
  }

  public static class FiltersBuilder {
    private final Iterable<com.eucalyptus.compute.common.Filter> filters;
    private final Class<?> resourceType;
    private final String qualifier;
    private final Map<String,Set<String>> internalFilters = Maps.newHashMap();

    public FiltersBuilder( final Iterable<com.eucalyptus.compute.common.Filter> filters,
                           final Class<?> resourceType,
                           final String qualifier ) {
      this.filters = filters;
      this.resourceType = resourceType;
      this.qualifier = qualifier;
    }

    /**
     * Add internal filters with wildcard support if any values present.
     *
     * @param name The filter name
     * @param values The filter values with wildcards
     * @return This builder for call chaining
     */
    @Nonnull
    public FiltersBuilder withOptionalInternalFilter( final String name,
                                                      final Iterable<String> values ) {
      if ( values.iterator().hasNext() ) {
        internalFilters.put( name, ImmutableSet.copyOf( values ) );
      }
      return this;
    }

    @Nonnull
    public Filter generate() throws InvalidFilterException {
      Filter filter;

      final FilterSupport<?> support = FilterSupport.forResource( resourceType, qualifier );
      if ( support == null ) {
        filter = Filter.alwaysTrue();
      } else {
        filter = support.generate( toMap( filters ), false );
        if ( !internalFilters.isEmpty() ) {
          filter = filter.and( support.generate( internalFilters, true ) );
        }
      }

      return filter;
    }
  }

  private static Map<String, Set<String>> toMap( final Iterable<com.eucalyptus.compute.common.Filter> filters ) {
    final ImmutableMap.Builder<String,Set<String>> filterMapBuilder = ImmutableMap.builder();

    for ( final com.eucalyptus.compute.common.Filter filter : filters ) {
      final Set<String> values = ImmutableSet.copyOf(
          Iterables.transform( filter.getValueSet(), NullToEmptyString.INSTANCE ) );
      filterMapBuilder.put( filter.getName(), values );
    }

    return filterMapBuilder.build();
  }

  private enum NullToEmptyString implements Function<String,String> {
    INSTANCE;

    @Override
    public String apply( final String text ) {
      return text == null ? "" : text;
    }
  }
}
