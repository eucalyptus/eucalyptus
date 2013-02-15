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
package com.eucalyptus.util;

import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Utility functions for collections
 */
public class CollectionUtils {
  
  public static <T> Function<T,List<T>> listUnit() {
    return new Function<T,List<T>>() {
      @SuppressWarnings( "unchecked" )
      @Override
      public List<T> apply( final T t ) {
        return t == null ? 
            Lists.<T>newArrayList() : 
            Lists.newArrayList( t );
      }
    };    
  }

  public static <T> Function<List<List<T>>,List<T>> listJoin() {
    return new Function<List<List<T>>,List<T>>() {
      @SuppressWarnings( "unchecked" )
      @Override
      public List<T> apply( final List<List<T>> t ) {
        return t == null ?
            Lists.<T>newArrayList() :
            Lists.newArrayList( Iterables.concat( t ) );
      }
    };
  }

  /**
   * Reduce a collection using an initial value and a reduction function.
   * 
   * @param iterable The iterable to be reduced
   * @param initialValue The initial value
   * @param reducer The reduction function
   * @param <T> The result type
   * @param <I> The iterable type
   * @return The final value
   */
  public static <T,I> T reduce( final Iterable<I> iterable, 
                                final T initialValue, 
                                final Function<T,Function<I,T>> reducer ) {
    T value = initialValue;
    for ( I item : iterable ) {
      value = reducer.apply( value ).apply( item );      
    }    
    return value;
  }

  /**
   * Count function suitable for use with reduce.
   * 
   * @param evaluator Predicate matching items to be counted.
   * @param <I> The evaluated type
   * @return The count function.
   */
  public static <I> Function<Integer,Function<I,Integer>> count( final Predicate<I> evaluator ) {
    return sum( new Function<I,Integer>(){
      @Override
      public Integer apply( @Nullable final I item ) {
        return evaluator.apply( item ) ? 1 : 0;
      }
    } );
  }

  /**
   * Sum function suitable for use with reduce.
   *
   * @param evaluator Function to obtain an int from an I
   * @param <I> The evaluated type
   * @return The sum function.
   */
  public static <I> Function<Integer,Function<I,Integer>> sum( final Function<I,Integer> evaluator ) {
    return new Function<Integer,Function<I,Integer>>() {
      @Override
      public Function<I, Integer> apply( final Integer sum ) {
        return new Function<I, Integer>() {
          @Override
          public Integer apply( final I item ) {
            return sum + evaluator.apply( item );
          }
        };
      }
    };
  }

  /**
   * Comparator function suitable for use with reduce.
   *
   * <p>Use with reduce to obtain a min or max value.</p>
   * 
   * @param comparator The comparator to use
   * @param <T> The compared type
   * @return The comparator function.
   * @see com.google.common.collect.Ordering
   */
  public static <T> Function<T,Function<T,T>> comparator( final Comparator<T> comparator ) {
    return new Function<T,Function<T,T>>() {
      @Override
      public Function<T, T> apply( final T t1 ) {
        return new Function<T,T>() {
          @Override
          public T apply( final T t2 ) {
            return comparator.compare( t1, t2 ) < 0 ?
                t1 : 
                t2;
          }
        };
      }
    };
  }
}
