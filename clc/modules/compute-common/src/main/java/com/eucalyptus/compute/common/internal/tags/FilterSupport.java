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
package com.eucalyptus.compute.common.internal.tags;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static com.eucalyptus.compute.common.internal.tags.FilterSupport.PersistenceFilter.persistenceFilter;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.util.Parameters;
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Filter support class overridden for each resource that supports filtering.
 */
public abstract class FilterSupport<RT> {

  private static final ConcurrentMap<SupportKey,FilterSupport<?>> supportMap = Maps.newConcurrentMap();

  private final Class<RT> resourceClass;
  private final String qualifier;
  private Class<? extends Tag> tagClass;
  private final String tagFieldName;
  private final String resourceFieldName;
  private final Set<String> internalFilters;
  private final Map<String,Function<? super String,Predicate<? super RT>>> predicateFunctions;
  private final Map<String,String> aliases;
  private final Map<String,PersistenceFilter> persistenceFilters;

  /**
   * Create a new instance.
   *
   * @param builder The configuration for the filter support
   * @see #builderFor
   */
  protected FilterSupport( @Nonnull final Builder<RT> builder ) {
    this.resourceClass = builder.resourceClass;
    this.qualifier = builder.qualifier;
    this.tagClass = builder.tagClass;
    this.tagFieldName = builder.tagFieldName;
    this.resourceFieldName = builder.resourceFieldName;
    this.internalFilters = builder.buildInternalFilters();
    this.predicateFunctions = builder.buildPredicateFunctions();
    this.aliases = builder.buildAliases();
    this.persistenceFilters = builder.buildPersistenceFilters();
  }

  /**
   * Create a configuration builder for the specified resource class.
   *
   * @param resourceClass The resource class
   * @param <RT> The resource type
   * @return The builder to use
   */
  protected static <RT> Builder<RT> builderFor( final Class<RT> resourceClass ) {
    return new Builder<RT>( resourceClass, Filters.DEFAULT_FILTERS );
  }

  /**
   * Create a configuration builder for the specified resource class.
   *
   * @param resourceClass The resource class
   * @param qualifier The filter set qualifier
   * @param <RT> The resource type
   * @return The builder to use
   */
  protected static <RT> Builder<RT> qualifierBuilderFor( final Class<RT> resourceClass,
                                                         final String qualifier ) {
    return new Builder<RT>( resourceClass, qualifier );
  }

  /**
   * Configuration builder for a resource class.
   *
   * @param <RT> The resource type
   */
  protected static class Builder<RT> {
    private final Class<RT> resourceClass;
    private final String qualifier;
    private final Set<String> internalFilters = Sets.newHashSet();
    private final Map<String,Function<? super String,Predicate<? super RT>>> predicateFunctions =
        Maps.newHashMap();
    private final Map<String,String> aliases = Maps.newHashMap();
    private final Map<String,PersistenceFilter> persistenceFilters = Maps.newHashMap();
    private Class<? extends Tag> tagClass;
    private String tagFieldName; // usually "tags"
    private String resourceFieldName;

    private Builder( final Class<RT> resourceClass,
                     final String qualifier ) {
      Parameters.checkParam( "Resource class", resourceClass, notNullValue() );
      Parameters.checkParam( "Qualifier", qualifier, not( isEmptyOrNullString() ) );
      this.resourceClass = resourceClass;
      this.qualifier = qualifier;
    }

    /**
     * Enable tag filtering for the resource.
     *
     * @param tagClass The TagSupport subclass
     * @param resourceFieldName The field name linking back to the resource from the tag
     * @param tagFieldName The name of the tag collection field in the resource class.
     * @return This builder for call chaining
     */
    public Builder<RT> withTagFiltering( final Class<? extends Tag> tagClass,
                                         final String resourceFieldName,
                                         final String tagFieldName  ) {
      this.tagClass = tagClass;
      this.resourceFieldName = resourceFieldName;
      this.tagFieldName = tagFieldName;
      return this;
    }

    /**
     * Enable tag filtering for the resource.
     *
     * @param tagClass The TagSupport subclass
     * @param resourceFieldName The field name linking back to the resource from the tag
     * @return This builder for call chaining
     */
    public Builder<RT> withTagFiltering( final Class<? extends Tag> tagClass,
                                         final String resourceFieldName ) {
      return withTagFiltering( tagClass, resourceFieldName, "tags" );
    }

    /**
     * Declare a filterable boolean property.
     *
     * @param filterName The name of the filter
     * @param booleanExtractor Function to extract the property value
     * @return This builder for call chaining
     */
    public Builder<RT> withBooleanProperty( final String filterName,
                                            final Function<? super RT,Boolean> booleanExtractor ) {
      predicateFunctions.put( filterName,  FilterSupport.<RT>booleanFilter( booleanExtractor ) );
      return this;
    }

    /**
     * Declare an internal filterable boolean property.
     *
     * <p>An internal property cannot be accessed via the public API but can be
     * explicitly added.</p>
     *
     * @param filterName The name of the filter
     * @param booleanExtractor Function to extract the property value
     * @return This builder for call chaining
     */
    public Builder<RT> withInternalBooleanProperty( final String filterName,
                                                    final Function<? super RT,Boolean> booleanExtractor ) {
      internalFilters.add( filterName );
      predicateFunctions.put( filterName,  FilterSupport.<RT>booleanFilter( booleanExtractor ) );
      return this;
    }

    /**
     * Declare a filterable multi-valued boolean property.
     *
     * @param filterName The name of the filter
     * @param booleanSetExtractor Function to extract the property values
     * @return This builder for call chaining
     */
    public Builder<RT> withBooleanSetProperty( final String filterName,
                                               final Function<? super RT,Set<Boolean>> booleanSetExtractor ) {
      predicateFunctions.put( filterName,  FilterSupport.<RT>booleanSetFilter( booleanSetExtractor ) );
      return this;
    }

    /**
     * Declare a filterable date property.
     *
     * @param filterName The name of the filter
     * @param dateExtractor Function to extract the property value
     * @return This builder for call chaining
     */
    public Builder<RT> withDateProperty( final String filterName,
                                         final Function<? super RT,Date> dateExtractor ) {
      predicateFunctions.put( filterName,  FilterSupport.<RT>dateFilter( dateExtractor ) );
      return this;
    }

    /**
     * Declare a filterable multi-valued date property.
     *
     * @param filterName The name of the filter
     * @param dateSetExtractor Function to extract the property values
     * @return This builder for call chaining
     */
    public Builder<RT> withDateSetProperty( final String filterName,
                                            final Function<? super RT,Set<Date>> dateSetExtractor ) {
      predicateFunctions.put( filterName,  FilterSupport.<RT>dateSetFilter( dateSetExtractor ) );
      return this;
    }

    /**
     * Declare a filterable integer property.
     *
     * @param filterName The name of the filter
     * @param integerExtractor Function to extract the property value
     * @return This builder for call chaining
     */
    public Builder<RT> withIntegerProperty( final String filterName,
                                            final Function<? super RT,Integer> integerExtractor ) {
      predicateFunctions.put( filterName,  FilterSupport.<RT>intFilter( integerExtractor ) );
      return this;
    }

    /**
     * Declare a filterable multi-valued integer property.
     *
     * @param filterName The name of the filter
     * @param integerSetExtractor Function to extract the property values
     * @return This builder for call chaining
     */
    public Builder<RT> withIntegerSetProperty( final String filterName,
                                               final Function<? super RT,Set<Integer>> integerSetExtractor ) {
      predicateFunctions.put( filterName, FilterSupport.<RT>intSetFilter( integerSetExtractor ) );
      return this;
    }

    /**
     * Declare a filterable multi-valued integer property.
     *
     * @param filterName The name of the filter
     * @param integerSetExtractor Function to extract the property values
     * @param valueFunction Function to convert filter value
     * @return This builder for call chaining
     */
    public Builder<RT> withIntegerSetProperty( final String filterName,
                                               final Function<? super RT,Set<Integer>> integerSetExtractor,
                                               final Function<String,Integer> valueFunction ) {
      predicateFunctions.put( filterName, FilterSupport.<RT>intSetFilter( integerSetExtractor, valueFunction ) );
      return this;
    }

    /**
     * Declare a filterable long property.
     *
     * @param filterName The name of the filter
     * @param longExtractor Function to extract the property value
     * @return This builder for call chaining
     */
    public Builder<RT> withLongProperty( final String filterName,
                                         final Function<? super RT,Long> longExtractor ) {
      predicateFunctions.put( filterName,  FilterSupport.<RT>longFilter( longExtractor ) );
      return this;
    }

    /**
     * Declare a filterable multi-valued long property.
     *
     * @param filterName The name of the filter
     * @param longSetExtractor Function to extract the property values
     * @return This builder for call chaining
     */
    public Builder<RT> withLongSetProperty( final String filterName,
                                            final Function<? super RT,Set<Long>> longSetExtractor ) {
      predicateFunctions.put( filterName, FilterSupport.<RT>longSetFilter( longSetExtractor ) );
      return this;
    }

    /**
     * Declare a filterable string property.
     *
     * @param filterName The name of the filter
     * @param stringExtractor Function to extract the property value
     * @return This builder for call chaining
     */
    public Builder<RT> withStringProperty( final String filterName,
                                           final Function<? super RT,String> stringExtractor ) {
      predicateFunctions.put( filterName,  FilterSupport.<RT>stringFilter( stringExtractor ) );
      return this;
    }

    /**
     * Declare an internal filterable string property.
     *
     * <p>An internal property cannot be accessed via the public API but can be
     * explicitly added.</p>
     *
     * @param filterName The name of the filter
     * @param stringExtractor Function to extract the property value
     * @return This builder for call chaining
     */
    public Builder<RT> withInternalStringProperty( final String filterName,
                                                   final Function<? super RT,String> stringExtractor ) {
      internalFilters.add( filterName );
      predicateFunctions.put( filterName,  FilterSupport.<RT>stringFilter( stringExtractor ) );
      return this;
    }

    /**
     * Declare a filterable multi-valued string property.
     *
     * @param filterName The name of the filter
     * @param stringSetExtractor Function to extract the property values
     * @return This builder for call chaining
     */
    public Builder<RT> withStringSetProperty( final String filterName,
                                              final Function<? super RT,Set<String>> stringSetExtractor ) {
      predicateFunctions.put( filterName,  FilterSupport.<RT>stringSetFilter( stringSetExtractor ) );
      return this;
    }

    public Builder<RT> withLikeExplodedProperty( final String filterName,
                                                 final Function<? super RT, ?> extractor,
                                                 final Function<String, Collection> explodeFunction ) {
      predicateFunctions.put( filterName,
          FilterSupport.<RT>explodedLiteralFilter( extractor, likeWildFunction( explodeFunction ) ) );
      return this;
    }

    /**
     * Declare a constant valued string property.
     *
     * @param filterName The name of the filter
     * @param value The property value
     * @return This builder for call chaining
     */
    public Builder<RT> withConstantProperty( final String filterName,
                                             final String value ) {
      predicateFunctions.put(
          filterName,
          FilterSupport.<RT>stringFilter( Functions.compose(
              Functions.constant( value ),
              Functions.<RT>identity() ) ) );
      return this;
    }

    /**
     * Declare a unsupported property.
     *
     * <p>Unsupported properties will fail to match at runtime</p>
     *
     * @param filterName The name of the filter
     * @return This builder for call chaining
     */
    public Builder<RT> withUnsupportedProperty( final String filterName ) {
      predicateFunctions.put( filterName,  FilterSupport.<RT>falseFilter() );
      return this;
    }

    /**
     * Declare a persistence alias.
     *
     * <p>When a persistence filter traverses tables (join) it is necessary to
     * declare the alias once globally and reference the alias in each filter
     * that uses it.</p>
     *
     * @param path The path for the alias
     * @param alias The value for the alias
     * @return This builder for call chaining
     */
    public Builder<RT> withPersistenceAlias( final String path, final String alias ) {
      aliases.put( path, alias );
      return this;
    }

    /**
     * Declare a property filterable at the persistence layer.
     *
     * @param name The name of the filter and the field
     * @return This builder for call chaining
     */
    public Builder<RT> withPersistenceFilter( final String name ) {
      return withPersistenceFilter( name, name );
    }

    /**
     * Declare a property filterable at the persistence layer.
     *
     * @param filterName The name of the filter
     * @param fieldName The path to the field (dot separated)
     * @return This builder for call chaining
     */
    public Builder<RT> withPersistenceFilter( final String filterName,
                                              final String fieldName ) {
      return withPersistenceFilter( filterName, fieldName, aliases( fieldName ) );
    }

    /**
     * Declare a property filterable at the persistence layer.
     *
     * @param filterName The name of the filter
     * @param fieldName The path to the field (dot separated)
     * @param aliases The aliases used by this filter
     * @return This builder for call chaining
     */
    public Builder<RT> withPersistenceFilter( final String filterName,
                                              final String fieldName,
                                              final Set<String> aliases ) {
      persistenceFilters.put( filterName, persistenceFilter( fieldName, aliases ) );
      return this;
    }

    /**
     * Declare a property filterable at the persistence layer.
     *
     * @param filterName The name of the filter
     * @param fieldName The path to the field (dot separated)
     * @param type The field type for this filter
     * @return This builder for call chaining
     */
    public Builder<RT> withPersistenceFilter( final String filterName,
                                              final String fieldName,
                                              final PersistenceFilter.Type type ) {
      return withPersistenceFilter( filterName, fieldName, aliases( fieldName ), type );
    }

    /**
     * Declare a property filterable at the persistence layer.
     *
     * @param filterName The name of the filter
     * @param fieldName The path to the field (dot separated)
     * @param aliases The aliases used by this filter
     * @param type The field type for this filter
     * @return This builder for call chaining
     */
    public Builder<RT> withPersistenceFilter( final String filterName,
                                              final String fieldName,
                                              final Set<String> aliases,
                                              final PersistenceFilter.Type type ) {
      persistenceFilters.put( filterName, persistenceFilter( fieldName, aliases, type ) );
      return this;
    }

    /**
     * Declare a property filterable at the persistence layer.
     *
     * <p>A value function must be provided if the field is not a string value
     * or one of the supported explicit types. A typical use case is for enum
     * values.</p>
     *
     * @param filterName The name of the filter
     * @param fieldName The path to the field (dot separated)
     * @param valueFunction The value conversion function for this filter
     * @return This builder for call chaining
     * @see com.google.common.base.Enums#valueOfFunction
     */
    public Builder<RT> withPersistenceFilter( final String filterName,
                                              final String fieldName,
                                              final Function<String,?> valueFunction ) {
      return withPersistenceFilter( filterName, fieldName, aliases( fieldName ), valueFunction );
    }

    /**
     * Declare a property filterable at the persistence layer.
     *
     * <p>A value function must be provided if the field is not a string value
     * or one of the supported explicit types. A typical use case is for enum
     * values.</p>
     *
     * @param filterName The name of the filter
     * @param fieldName The path to the field (dot separated)
     * @param aliases The aliases used by this filter
     * @param valueFunction The value conversion function for this filter
     * @return This builder for call chaining
     * @see com.google.common.base.Enums#valueOfFunction
     */
    public Builder<RT> withPersistenceFilter( final String filterName,
                                              final String fieldName,
                                              final Set<String> aliases,
                                              final Function<String,?> valueFunction ) {
      persistenceFilters.put( filterName, persistenceFilter( fieldName, aliases, valueFunction ) );
      return this;
    }

    /**
     * Declare a property filterable at the persistence layer after expansion.
     *
     * <p>The given function will be passed a expression containing like
     * wildcards and is expected to explode to a collection of literal values
     * to match.</p>
     *
     * @param filterName The name of the filter
     * @param fieldName The path to the field (dot separated)
     * @param explodeFunction Function to expand a given like expression
     * @return This builder for call chaining
     */
    public Builder<RT> withLikeExplodingPersistenceFilter( final String filterName,
                                                           final String fieldName,
                                                           final Function<String, Collection> explodeFunction ) {
      persistenceFilters.put( filterName, persistenceFilter(
          fieldName,
          aliases( fieldName ),
          likeWildFunction( explodeFunction ) ) );
      return this;
    }

    private Set<String> aliases( final String fieldPath ) {
      final Set<String> aliases = Sets.newHashSet();
      final Iterator<String> aliasIterator = Splitter.on( "." ).split( fieldPath ).iterator();
      while ( aliasIterator.hasNext() ) {
        final String value = aliasIterator.next();
        if ( aliasIterator.hasNext() ) { // all but last component
          aliases.add( value );
        }
      }
      return aliases;
    }

    private Map<String,Function<? super String,Predicate<? super RT>>> buildPredicateFunctions() {
      return ImmutableMap.copyOf( predicateFunctions );
    }

    private Set<String> buildInternalFilters() {
      return ImmutableSet.copyOf( internalFilters );
    }

    private Map<String,String> buildAliases() {
      return ImmutableMap.copyOf( aliases );
    }

    private Map<String,PersistenceFilter> buildPersistenceFilters() {
      return ImmutableMap.copyOf( persistenceFilters );
    }
  }

  @Nonnull
  public Class<RT> getResourceClass() {
    return resourceClass;
  }

  @Nonnull
  public String getQualifier() {
    return qualifier;
  }

  @Nullable
  public Class<? extends Tag> getTagClass() {
    return tagClass;
  }

  @Nullable
  public String getTagFieldName() {
    return tagFieldName;
  }

  @Nullable
  public String getResourceFieldName() {
    return resourceFieldName;
  }

  /**
   * Generate a Filter for the given filters.
   *
   * @param filters The map of filter names to (multiple) values
   * @param allowInternalFilters True to allow use of internal filters
   * @return The filter representation
   * @throws InvalidFilterException If a filter is invalid
   */
  public Filter generate( final Map<String, Set<String>> filters,
                          final boolean allowInternalFilters ) throws InvalidFilterException  {
    final Context ctx = Contexts.lookup();
    final String requestAccountId = ctx.getAccountNumber( );
    return generate( filters, allowInternalFilters, requestAccountId );
  }

  /**
   * Generate a Filter for the given filters.
   *
   * @param filters The map of filter names to (multiple) values
   * @param allowInternalFilters True to allow use of internal filters
   * @return The filter representation
   * @throws InvalidFilterException If a filter is invalid
   */
  public Filter generate( final Map<String, Set<String>> filters,
                          final boolean allowInternalFilters,
                          final String accountId ) throws InvalidFilterException {
    // Construct collection filter
    final List<Predicate<Object>> and = Lists.newArrayList();
    for ( final Map.Entry<String,Set<String>> filter : Iterables.filter( filters.entrySet(), Predicates.not( isTagFilter() ) ) ) {
      final List<Predicate<Object>> or = Lists.newArrayList();
      for ( final String value : filter.getValue() ) {
        final Function<? super String,Predicate<? super RT>> predicateFunction = predicateFunctions.get( filter.getKey() );
        if ( predicateFunction == null || (!allowInternalFilters && internalFilters.contains( filter.getKey() ) ) ) {
          throw InvalidFilterException.forName( filter.getKey() );
        }
        final Predicate<? super RT> valuePredicate = predicateFunction.apply( value );
        or.add( typedPredicate( valuePredicate ) );
      }
      and.add( Predicates.or( or ) );
    }

    // Construct database filter and aliases
    final Junction conjunction = Restrictions.conjunction();
    final Map<String,String> aliases = Maps.newHashMap();
    for ( final Map.Entry<String,Set<String>> filter : Iterables.filter( filters.entrySet(), Predicates.not( isTagFilter() ) ) ) {
      final Junction disjunction = Restrictions.disjunction();
      for ( final String value : filter.getValue() ) {
        final PersistenceFilter persistenceFilter = persistenceFilters.get( filter.getKey() );
        if ( persistenceFilter != null ) {
          final Object persistentValue = persistenceFilter.value( value );
          if ( persistentValue != null ) {
            for ( final String alias : persistenceFilter.getAliases() ) aliases.put( alias, this.aliases.get( alias ) );
            disjunction.add( buildRestriction( persistenceFilter.getProperty(), persistentValue ) );
          } // else, there is no valid DB filter for the given value (e.g. wildcard for integer value)
        }
      }
      conjunction.add( disjunction );
    }

    // Construct database filter and aliases for tags
    boolean tagPresent = false;
    final List<Junction> tagJunctions = Lists.newArrayList();
    for ( final Map.Entry<String,Set<String>> filter : Iterables.filter( filters.entrySet(), isTagFilter() ) ) {
      tagPresent = true;
      final Junction disjunction = Restrictions.disjunction();
      final String filterName = filter.getKey();
      for ( final String value : filter.getValue() ) {
        if ( "tag-key".equals( filterName ) ) {
          disjunction.add( buildTagRestriction( value, null, true ) );
        } else if ( "tag-value".equals( filterName ) ) {
          disjunction.add( buildTagRestriction( null, value, true ) );
        } else {
          disjunction.add( buildTagRestriction( filterName.substring(4), value, false ) );
        }
      }
      tagJunctions.add( disjunction );
    }
    if ( tagPresent ) conjunction.add( tagCriterion( accountId, tagJunctions ) );

    return new Filter( aliases, conjunction, Predicates.and( and ), tagPresent );
  }

  public static FilterSupport<?> forResource( @Nonnull final Class<?> metadataClass,
                                           @Nonnull final String qualifier ) {
    return supportMap.get( supportKey( metadataClass, qualifier ) );
  }

  /**
   * Will the given like expression match any value?
   *
   * @param expression The expression to test
   * @return True if fully wild
   */
  public static boolean isTotallyWildLikeExpression( final String expression ) {
    return expression.replace("%","").isEmpty();
  }

  protected static Function<String,String> ignoredValueFunction( final String... ignored ) {
    return ignoredValueFunction( Sets.newHashSet( ignored ) );
  }

  protected static Function<String,String> ignoredValueFunction( final Set<String> ignored ) {
    return value -> ignored.contains( value ) ? null : value;
  }

  private static <T> Function<? super String, Predicate<? super T>> falseFilter() {
    return Functions.<Predicate<? super T>>constant( Predicates.alwaysFalse() );
  }


  private static <T> Function<? super String, Predicate<? super T>> explodedLiteralFilter( final Function<? super T,?> extractor,
                                                                                           final Function<String, Collection> explodeFunction ) {
    return new Function<String,Predicate<? super T>>() {
      @SuppressWarnings( "unchecked" )
      @Override
      public Predicate<T> apply( final String filterValue ) {
        Collection values = explodeFunction.apply( filterValue );
        return values == null ?
            Predicates.<T>alwaysTrue() :
            Predicates.compose(
              Predicates.<Object>in( values ),
              Functions.compose( extractor, Functions.<T>identity() ) );
      }
    };
  }

  private static <T> Function<? super String, Predicate<? super T>> stringFilter( final Function<? super T, String> extractor ) {
    return stringSetFilter( Functions.compose( FilterSupport.<String>toSet(), extractor ) );
  }

  private static <T> Function<? super String, Predicate<? super T>> stringSetFilter( final Function<? super T, Set<String>> extractor ) {
    return new Function<String,Predicate<? super T>>() {
      @Override
      public Predicate<T> apply( final String filterValue ) {
        final Predicate<Set<String>> resourceValuePredicate = resourceValueMatcher( filterValue );
        return new Predicate<T>() {
          @Override
          public boolean apply( final T resource ) {
            final Set<String> resourceValues = extractor.apply( resource );
            return resourceValuePredicate.apply( resourceValues );
          }
        };
      }
    };
  }

  private static <T> Function<? super String, Predicate<? super T>> dateFilter( final Function<? super T, Date> extractor ) {
    return dateSetFilter( Functions.compose( FilterSupport.<Date>toSet(), extractor ) );
  }

  private static <T> Function<? super String, Predicate<? super T>> dateSetFilter( final Function<? super T, Set<Date>> extractor ) {
    return typedSetFilter( extractor, PersistenceFilter.Type.Date );
  }

  private static <T> Function<? super String, Predicate<? super T>> booleanFilter( final Function<? super T, Boolean> extractor ) {
    return booleanSetFilter( Functions.compose( FilterSupport.<Boolean>toSet(), extractor ) );
  }

  private static <T> Function<? super String, Predicate<? super T>> booleanSetFilter( final Function<? super T, Set<Boolean>> extractor ) {
    return typedSetFilter( extractor, PersistenceFilter.Type.Boolean );
  }

  private static <T> Function<? super String, Predicate<? super T>> intFilter( final Function<? super T, Integer> extractor ) {
    return intSetFilter( Functions.compose( FilterSupport.<Integer>toSet(), extractor ) );
  }

  private static <T> Function<? super String, Predicate<? super T>> intSetFilter( final Function<? super T, Set<Integer>> extractor ) {
    return typedSetFilter( extractor, PersistenceFilter.Type.Integer );
  }

  private static <T> Function<? super String, Predicate<? super T>> intSetFilter(
      final Function<? super T, Set<Integer>> extractor,
      final Function<String,Integer> valueFunction
  ) {
    return typedSetFilter( extractor, PersistenceFilter.Type.Integer, valueFunction );
  }

  private static <T> Function<? super String, Predicate<? super T>> longFilter( final Function<? super T, Long> extractor ) {
    return longSetFilter( Functions.compose( FilterSupport.<Long>toSet(), extractor ) );
  }

  private static <T> Function<? super String, Predicate<? super T>> longSetFilter( final Function<? super T, Set<Long>> extractor ) {
    return typedSetFilter( extractor, PersistenceFilter.Type.Integer );
  }

  private static <T,VT> Function<? super String, Predicate<? super T>> typedSetFilter(
      final Function<? super T, Set<VT>> extractor,
      final PersistenceFilter.Type type
  ) {
    return typedSetFilter( extractor, type, type.valueFunction( ) );
  }

  private static <T,VT> Function<? super String, Predicate<? super T>> typedSetFilter(
      final Function<? super T, Set<VT>> extractor,
      final PersistenceFilter.Type type,
      final Function<String,?> valueFunction
  ) {
    return new Function<String,Predicate<? super T>>() {
      @Override
      public Predicate<T> apply( final String filterValue ) {
        final Predicate<Set<VT>> resourceValuePredicate = resourceValueMatcher( filterValue, type, valueFunction );
        return new Predicate<T>() {
          @Override
          public boolean apply( final T resource ) {
            final Set<VT> resourceValues = extractor.apply( resource );
            return resourceValuePredicate.apply( resourceValues );
          }
        };
      }
    };
  }

  @SuppressWarnings( "unchecked" )
  static void registerFilterSupport( @Nonnull final FilterSupport filterSupport ) {
    supportMap.put(
        supportKey( filterSupport.getResourceClass(), filterSupport.getQualifier() ),
        filterSupport );
  }

  private static SupportKey supportKey( @Nonnull final Class<?> resourceClass,
                                        @Nonnull final String qualifier ) {
    Parameters.checkParam( "Resource class", resourceClass, notNullValue() );
    Parameters.checkParam( "Qualifier", qualifier, not( isEmptyOrNullString() ) );
    return new SupportKey( resourceClass, qualifier );
  }

  private Predicate<Object> typedPredicate( final Predicate<? super RT> predicate ) {
    return new Predicate<Object>() {
      @Override
      public boolean apply( final Object object ) {
        return getResourceClass().isInstance( object ) &&
            predicate != null &&
            predicate.apply( getResourceClass().cast( object ) );
      }
    };
  }

  private static <T> Function<T,Set<T>> toSet() {
    return new Function<T,Set<T>>() {
      @Override
      public Set<T> apply( final T value ) {
        return value == null ?
            Collections.<T>emptySet() :
            Collections.singleton( value );
      }
    };
  }

  private static <T> Function<String,T> likeWildFunction( final Function<String,T> delegate ) {
    return new Function<String, T>() {
      @Override
      public T apply( final String expression ) {
        final StringBuilder likeValueBuilder = new StringBuilder();
        translateWildcards( expression, likeValueBuilder, "_", "%", SyntaxEscape.Like );
        final String likeExpression = likeValueBuilder.toString();
        if ( isTotallyWildLikeExpression( likeExpression ) ) {
          return null;
        } else {
          return delegate.apply( likeExpression );
        }
      }
    };
  }

  private boolean isTagFilter( final String filter ) {
    return isTagFilteringEnabled() && (
          filter.startsWith("tag:") ||
          "tag-key".equals( filter ) ||
          "tag-value".equals( filter )
        );
  }

  private boolean isTagFilteringEnabled() {
    return tagFieldName != null;
  }

  private Predicate<Map.Entry<String,?>> isTagFilter() {
    return !isTagFilteringEnabled() ?
        Predicates.<Map.Entry<String,?>>alwaysFalse() :
        new Predicate<Map.Entry<String,?>>() {
          @Override
          public boolean apply( final Map.Entry<String, ?> stringEntry ) {
            return isTagFilter( stringEntry.getKey() );
          }
        };
  }

  private static <T> Predicate<Set<T>> resourceValueMatcher( final String filterPattern,
                                                             final PersistenceFilter.Type type,
                                                             final Function<String,?> valueFunction ) {
    final Object value = valueFunction.apply( filterPattern );
    return value == null ?
        Predicates.<Set<T>>alwaysFalse() :
        new Predicate<Set<T>>() {
      @SuppressWarnings( "SuspiciousMethodCalls" )
      @Override
      public boolean apply( final Set<T> resourceValues ) {
        boolean match = false;
        for ( final T resourceValue : resourceValues ) {
          if ( type.matches( value, resourceValue ) ) {
            match = true;
            break;
          }
        }
        return match;
      }
    };
  }

  /**
   * Construct a predicate from a filter pattern.
   *
   * A Pattern is constructed from the given filter, as per AWS:
   *
   *  Filters support the following wildcards:
   *
   *    *: Matches zero or more characters
   *    ?: Matches exactly one character
   *
   *   Your search can include the literal values of the wildcard characters; you just need to escape
   *   them with a backslash before the character. For example, a value of \*amazon\?\\ searches for
   *   the literal string *amazon?\.
   *
   * Wildcards are translated to regular expression wildcards:
   *
   *   .*: Matches zero or more characters
   *   . : Matches exactly one character
   *
   * Any other regular expression syntax from the filter value is escaped (Pattern.quote)
   */
  private static Predicate<Set<String>> resourceValueMatcher( final String filterPattern ) {
    final StringBuilder regexBuilder = new StringBuilder();
    if ( translateWildcards( filterPattern, regexBuilder, ".", ".*", SyntaxEscape.Regex ) ) {
      return new Predicate<Set<String>>() {
        private final Pattern pattern = Pattern.compile( regexBuilder.toString() );
        @Override
        public boolean apply( final Set<String> values ) {
          return Iterables.any( values, new Predicate<String>() {
            @Override
            public boolean apply( final String value ) {
              return value != null && pattern.matcher( value ).matches();
            }
          } );
        }
      };
    }

    // even if no regex, may contain \ escapes that must be removed
    final String processedFilterPattern = filterPattern.replaceAll( "\\\\", Matcher.quoteReplacement( "\\" ) );

    return new Predicate<Set<String>>() {
      @Override
      public boolean apply( final Set<String> values ) {
        return values.contains( processedFilterPattern );
      }
    };
  }

  /**
   * Construct a criterion from a filter pattern.
   *
   * A Pattern is constructed from the given filter, as per AWS:
   *
   *  Filters support the following wildcards:
   *
   *    *: Matches zero or more characters
   *    ?: Matches exactly one character
   *
   *   Your search can include the literal values of the wildcard characters; you just need to escape
   *   them with a backslash before the character. For example, a value of \*amazon\?\\ searches for
   *   the literal string *amazon?\.
   *
   * Wildcards are translated for use with 'like':
   *
   *   % : Matches zero or more characters
   *   _: Matches exactly one character
   *
   * In both cases wildcards can be escaped (to allow literal values) with a backslash (\).
   *
   * Translation of wildcards for direct DB filtering must support literal values from each grammar.
   */
  private Criterion buildRestriction( final String property, final Object persistentValue ) {
    final Object valueObject;
    if ( persistentValue instanceof String ) {
      final String value = persistentValue.toString();
      final StringBuilder likeValueBuilder = new StringBuilder();
      translateWildcards( value, likeValueBuilder, "_", "%", SyntaxEscape.Like );
      final String likeValue = likeValueBuilder.toString();

      if ( !value.equals( likeValue ) ) { // even if no regex, may contain \ escapes that must be removed
        return Restrictions.like( property, likeValue );
      }

      valueObject = persistentValue;
    } else {
      valueObject = persistentValue;
    }

    if ( persistentValue instanceof Collection ) {
      if ( ((Collection) persistentValue).isEmpty() ) {
        return Restrictions.not( Restrictions.conjunction() ); // always false
      } else {
        return Restrictions.in( property, (Collection) persistentValue );
      }
    } else {
      return Restrictions.eq( property, valueObject );
    }
  }

  Map<String, PersistenceFilter> getPersistenceFilters() {
    return persistenceFilters;
  }

  Map<String, String> getAliases() {
    return aliases;
  }

  /**
   * Build a criterion that uses sub-selects to match the given tag restrictions
   */
  private Criterion tagCriterion( final String accountId,
                                  final List<Junction> junctions ) {
    final Junction conjunction = Restrictions.conjunction();

    for ( final Junction criterion : junctions ) {
      final DetachedCriteria criteria = DetachedCriteria.forClass( tagClass )
          .add( Restrictions.eq( "ownerAccountNumber", accountId ) )
          .add( criterion )
          .setProjection( Projections.property( resourceFieldName ) );
      conjunction.add( Property.forName( tagFieldName ).in( criteria ) );
    }

    return conjunction;
  }

  /**
   * Build a restriction for a tag key and/or value.
   */
  private Criterion buildTagRestriction( @Nullable final String key,
                                         @Nullable final String value,
                                         final boolean keyWildcards ) {

    final Junction criteria = Restrictions.conjunction();

    if ( key != null  ) {
      criteria.add( keyWildcards ?
          buildRestriction( "displayName", key ) :
          Restrictions.eq( "displayName", key ) );
    }

    if ( value != null  ) {
      criteria.add( buildRestriction( "value", value ) );
    }

    return criteria;
  }

  /**
   * Translate wildcards from AWS to some other syntax
   */
  static boolean translateWildcards( final String filterPattern,
                                     final StringBuilder translated,
                                     final String matchOne,
                                     final String matchZeroOrMore,
                                     final Function<String,String> escapeFunction ) {
    boolean foundWildcard = false;
    final CharMatcher syntaxMatcher = CharMatcher.anyOf("\\*?");
    if ( syntaxMatcher.matchesAnyOf( filterPattern ) ) {
      // Process for wildcards
      boolean escaped = false;
      for ( final char character : filterPattern.toCharArray() ) {
        switch ( character ) {
          case '\\':
          case '?':
          case '*':
            if ( !escaped ) {
              switch ( character ) {
                case '\\':
                  escaped = true;
                  break;
                case '?':
                  foundWildcard = true;
                  translated.append( matchOne );
                  break;
                case '*':
                  foundWildcard = true;
                  translated.append( matchZeroOrMore );
                  break;
              }
              break;
            }
            escaped = false;
          default:
            if ( escaped ) {
              translated.append( escapeFunction.apply( "\\" ) );
            }
            escaped = false;
            translated.append( escapeFunction.apply( Character.toString( character ) ) );
        }
      }
      if ( escaped ) {
        translated.append( escapeFunction.apply( "\\" ) );
      }
    } else {
      translated.append( escapeFunction.apply( filterPattern ) );
    }

    return foundWildcard;
  }

  /**
   * Escape wildcards for like literals
   *
   * Escapes \ % and _ using a \
   */
  static String escapeLikeWildcards( final String literalExpression ) {
    final String escaped;
    final CharMatcher syntaxMatcher = CharMatcher.anyOf("\\%_");
    if ( syntaxMatcher.matchesAnyOf( literalExpression ) ) {
      final StringBuilder escapedBuffer = new StringBuilder();
      for ( final char character : literalExpression.toCharArray() ) {
        switch ( character ) {
          case '\\':
          case '_':
          case '%':
            escapedBuffer.append( '\\' );
          default:
            escapedBuffer.append( character );
        }
      }
      escaped = escapedBuffer.toString();
    } else {
      escaped = literalExpression;
    }

    return escaped;
  }

  /**
   * An instance of PersistenceFilter is created for each property.
   */
  public static class PersistenceFilter {
    public enum Type {
      Integer {
        @Override
        public Function<String, ?> valueFunction() {
          return new Function<String,Integer>() {
            @Override
            public Integer apply( final String textValue ) {
              try {
                return java.lang.Integer.valueOf( textValue );
              } catch ( NumberFormatException e ) {
                return null;
              }
            }
          };
        }
      },
      Long {
        @Override
        public Function<String, ?> valueFunction() {
          return new Function<String,Long>() {
            @Override
            public Long apply( final String textValue ) {
              try {
                return java.lang.Long.valueOf( textValue );
              } catch ( NumberFormatException e ) {
                return null;
              }
            }
          };
        }
      },
      Date {
        @Override
        public Function<String, ?> valueFunction() {
          return new Function<String,java.util.Date>() {
            @Override
            public java.util.Date apply( final String textValue ) {
              try {
                return Timestamps.parseIso8601Timestamp( textValue );
              } catch ( AuthenticationException e ) {
                return null;
              }
            }
          };
        }
        @Override
        boolean matches( final Object targetValue, final Object resourceValue ) {
          boolean match = false;
          if ( resourceValue instanceof Date && targetValue instanceof Date ) {
            match = ((Date) resourceValue).getTime() == ((Date) targetValue).getTime();
          }
          return match;
        }
      },
      Boolean {
        @Override
        public Function<String, ?> valueFunction() {
          return new Function<String,Boolean>() {
            @Override
            public java.lang.Boolean apply( final String textValue ) {
              java.lang.Boolean value = null;
              if ( java.lang.Boolean.TRUE.toString().equals( textValue ) ) {
                value = java.lang.Boolean.TRUE;
              } else if ( java.lang.Boolean.FALSE.toString().equals( textValue ) ) {
                value = java.lang.Boolean.FALSE;
              }
              return value;
            }
          };
        }
      };

      public abstract Function<String,?> valueFunction();

      boolean matches( final Object targetValue,
                       final Object resourceValue  ) {
        return targetValue.equals( resourceValue );
      }
    }

    @Nonnull private final String property;
    @Nonnull private final Set<String> aliases;
    @Nonnull private final Function<String,?> valueFunction;

    public static PersistenceFilter persistenceFilter( final String property,
                                                       final Set<String> aliases ) {
      return persistenceFilter( property, aliases, Functions.<String>identity() );
    }

    public static PersistenceFilter persistenceFilter( final String property,
                                                       final Set<String> aliases,
                                                       final Function<String,?> valueFunction ) {
      return new PersistenceFilter( property, aliases, valueFunction );
    }

    public static PersistenceFilter persistenceFilter( final String property,
                                                       final Set<String> aliases,
                                                       final Type type ) {
      return new PersistenceFilter( property, aliases, type.valueFunction() );
    }

    @Nonnull
    public String getProperty() {
      return property;
    }

    @Nonnull
    public Set<String> getAliases() {
      return aliases;
    }

    @Nullable
    public Object value( final String textValue ) {
      return valueFunction.apply( textValue );
    }

    @Nonnull
    Function<String, ?> getValueFunction() {
      return valueFunction;
    }

    private PersistenceFilter( @Nonnull final String property,
                               @Nonnull final Set<String> aliases,
                               @Nonnull final Function<String,?> valueFunction ) {
      this.property = property;
      this.aliases = aliases;
      this.valueFunction = valueFunction;
    }
  }

  enum SyntaxEscape implements Function<String,String> {
    Regex {
      @Override
      public String apply( final String text ) {
        return Pattern.quote( text );
      }
    },
    Like {
      @Override
      public String apply( final String text ) {
        return escapeLikeWildcards( text );
      }
    }
  }

  private static final class SupportKey {
    private final Class<?> resourceClass;
    private final String qualifier;

    private SupportKey( final Class<?> resourceClass,
                        final String qualifier ) {
      this.resourceClass = resourceClass;
      this.qualifier = qualifier;
    }

    public Class<?> getResourceClass() {
      return resourceClass;
    }

    public String getQualifier() {
      return qualifier;
    }

    @SuppressWarnings( "RedundantIfStatement" )
    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass() != o.getClass() ) return false;

      final SupportKey that = (SupportKey) o;

      if ( !qualifier.equals( that.qualifier ) ) return false;
      if ( !resourceClass.equals( that.resourceClass ) ) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = resourceClass.hashCode();
      result = 31 * result + qualifier.hashCode();
      return result;
    }
  }
}
