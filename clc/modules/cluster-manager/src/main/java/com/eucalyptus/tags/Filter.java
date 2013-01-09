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

import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

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
        predicate );
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
}

