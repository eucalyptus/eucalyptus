/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Restrictions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

/**
 * Filter can be used to filter collections or queries.
 *
 * <p>For query filtering the results should be passed though a collection
 * filter as the database filters cannot always fully restrict the results.</p>
 */
public class Filter {

  @Nonnull private final Map<String,String> aliases;
  @Nonnull private final Criterion criterion;
  @Nonnull private final Predicate<Object> predicate;

  Filter( @Nonnull final Map<String,String> aliases,
          @Nonnull final Criterion criterion,
          @Nonnull final Predicate<Object> predicate ) {
    this.aliases = aliases;
    this.criterion = criterion;
    this.predicate = predicate;
  }

  Filter( @Nonnull final Predicate<Object> predicate ) {
    this( Collections.<String,String>emptyMap(),
        Restrictions.conjunction(),
        predicate);
  }
  
  private Filter() {
    this( Predicates.alwaysTrue() );
  }

  /**
   * Get the aliases for use with query filtering.
   *
   * @return The aliases
   */
  @Nonnull
  public Map<String,String> getAliases() {
    return aliases;   
  }

  /**
   * Filter as a Hibernate Criterion.
   *
   * @return The criterion
   */
  @Nonnull
  public Criterion asCriterion() {
    return criterion;  
  }

  /**
   * Filter as a Hibernate Criterion.
   *
   * @param criterion
   * @return The criterion
   */
  @Nonnull
  public Criterion asCriterionWithConjunction( final Criterion criterion ) {
    return Restrictions.conjunction( ).add( criterion ).add( asCriterion( ) );
  }

  /**
   * Filter as a Guava Predicate.
   *
   * @return The criterion
   */
  @Nonnull
  public Predicate<Object> asPredicate() {
    return predicate;  
  }

  /**
   * Create a Filter that will always pass (filters out nothing)
   */
  static Filter alwaysTrue() {
    return new Filter();
  }

  /**
   * Combine filters.
   *
   * @param filter The filter to combine with
   * @return The new filter
   */
  public Filter and( final Filter filter ) {
    final Map<String,String> aliases = Maps.newHashMap();
    aliases.putAll( this.aliases );
    aliases.putAll( filter.aliases );
    final Junction and = Restrictions.conjunction();
    and.add( this.criterion );
    and.add( filter.criterion );
    return new Filter(
      aliases,
      and,
      Predicates.and( this.predicate, filter.predicate )
    );
  }
}

