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

import static com.eucalyptus.tags.FilterSupport.PersistenceFilter.persistenceFilter;
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
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Filter support class overridden for each resource that supports filtering.
 */
public abstract class FilterSupport<RT> {

  private static final ConcurrentMap<Class<?>,FilterSupport> supportByClass = Maps.newConcurrentMap();

  private final Class<RT> resourceClass;
  private Class<? extends Tag> tagClass;
  private final String tagFieldName;
  private final String resourceFieldName;
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
    this.tagClass = builder.tagClass;
    this.tagFieldName = builder.tagFieldName;
    this.resourceFieldName = builder.resourceFieldName;
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
    return new Builder<RT>( resourceClass );
  }

  /**
   * Configuration builder for a resource class.
   *
   * @param <RT> The resource type
   */
  protected static class Builder<RT> {
    private final Class<RT> resourceClass;
    private final Map<String,Function<? super String,Predicate<? super RT>>> predicateFunctions =
        Maps.newHashMap();
    private final Map<String,String> aliases = Maps.newHashMap();
    private final Map<String,PersistenceFilter> persistenceFilters = Maps.newHashMap();
    private Class<? extends Tag> tagClass;
    private String tagFieldName; // usually "tags"
    private String resourceFieldName;

    private Builder( Class<RT> resourceClass ) {
      this.resourceClass = resourceClass;
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
   * @return The filter representation
   * @throws InvalidFilterException If a filter is invalid
   */
  public Filter generate( final Map<String, Set<String>> filters ) throws InvalidFilterException  {
    final Context ctx = Contexts.lookup();
    final String requestAccountId = ctx.getUserFullName( ).getAccountNumber( );
    return generate( filters, requestAccountId );
  }

  /**
   * Generate a Filter for the given filters.
   *
   * @param filters The map of filter names to (multiple) values
   * @return The filter representation
   * @throws InvalidFilterException If a filter is invalid
   */
  public Filter generate( final Map<String, Set<String>> filters,
                          final String accountId ) throws InvalidFilterException {
    // Construct collection filter
    final List<Predicate<Object>> and = Lists.newArrayList();
    for ( final Map.Entry<String,Set<String>> filter : Iterables.filter( filters.entrySet(), Predicates.not( isTagFilter() ) ) ) {
      final List<Predicate<Object>> or = Lists.newArrayList();
      for ( final String value : filter.getValue() ) {
        final Function<? super String,Predicate<? super RT>> predicateFunction = predicateFunctions.get( filter.getKey() );
        if ( predicateFunction == null ) {
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

  public static FilterSupport forResource( @Nonnull final Class<?> metadataClass ) {
    return supportByClass.get( metadataClass );
  }

  private static <T> Function<? super String, Predicate<? super T>> falseFilter() {
    return Functions.<Predicate<? super T>>constant( Predicates.alwaysFalse() );
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

  private static <T> Function<? super String, Predicate<? super T>> longFilter( final Function<? super T, Long> extractor ) {
    return longSetFilter( Functions.compose( FilterSupport.<Long>toSet(), extractor ) );
  }

  private static <T> Function<? super String, Predicate<? super T>> longSetFilter( final Function<? super T, Set<Long>> extractor ) {
    return typedSetFilter( extractor, PersistenceFilter.Type.Integer );
  }

  private static <T,VT> Function<? super String, Predicate<? super T>> typedSetFilter( final Function<? super T, Set<VT>> extractor, final PersistenceFilter.Type type ) {
    return new Function<String,Predicate<? super T>>() {
      @Override
      public Predicate<T> apply( final String filterValue ) {
        final Predicate<Set<VT>> resourceValuePredicate = resourceValueMatcher( filterValue, type );
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
    supportByClass.put( (Class<?>) filterSupport.getResourceClass(), filterSupport );
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
                                                             final PersistenceFilter.Type type ) {
    final Object value = type.valueFunction().apply( filterPattern );
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

    return Restrictions.eq( property, valueObject );
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
}
