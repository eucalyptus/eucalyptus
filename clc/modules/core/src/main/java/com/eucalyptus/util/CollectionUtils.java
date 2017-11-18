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
package com.eucalyptus.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * Utility functions for collections
 */
public class CollectionUtils {

  /**
   * Apply the given function for each item in the iterable.
   *
   * <p>This method is an anti-pattern as the function is really an effect (it
   * can only be useful for its side effect)</p>
   *
   * @param iterable The iterable
   * @param function The function to apply
   * @param <T> The iterable type
   */
  public static <T> void each( final Iterable<T> iterable,
                               final Function<? super T,?> function ) {
    Iterables.size( Iterables.transform( iterable, function ) ); // transform is lazy
  }

  /**
   * Apply the given predicate for each item in the iterable.
   *
   * <p>This method is an anti-pattern as the predicate is really an effect (it
   * can only be useful for its side effect)</p>
   *
   * @param iterable The iterable
   * @param predicate The predicate to apply
   * @param <T> The iterable type
   */
  public static <T> void each( final Iterable<T> iterable,
                               final Predicate<? super T> predicate ) {
    each( iterable, Functions.forPredicate( predicate ) );
  }

  /**
   * Create a fluent iterable for the given iterable.
   *
   * @param iterable The possibly null iterable
   * @param <T> The iterable type
   * @return the FluentIterable
   */
  @Nonnull
  public static <T> FluentIterable<T> fluent( @Nullable final Iterable<T> iterable ) {
    return FluentIterable.from(
        iterable == null ?
            Collections.<T>emptyList( ) :
            iterable );
  }

  /**
   * Predicate for null checking.
   *
   * @return A non-null matching predicate
   */
  public static <T> CompatPredicate<T> isNotNull( ) {
    return item -> item != null;
  }

  /**
   * Predicate for collections containing the given item.
   *
   * @param item The required item
   * @param <CIT> The item type
   * @param <CT> The collection type
   * @return A collection matching predicate
   */
  public static <CIT,CT extends Collection<? super CIT>> Predicate<CT> contains( final CIT item ) {
    return new Predicate<CT>( ){
      @Override
      public boolean apply( @Nullable final CT collection ) {
        return collection != null && collection.contains( item );
      }
    };
  }

  /**
   * Convenience method for a predicate on a property value.
   *
   * @param propertyValue The property value to match
   * @param propertyFunction The function to extract the property
   * @param <T> The predicate type
   * @param <PT> The property type
   * @return A predicate that extracts a value to compare with the given value.
   */
  public static <T,PT> Predicate<T> propertyPredicate( final PT propertyValue,
                                                       final Function<T,PT> propertyFunction ) {
    return Predicates.compose( Predicates.equalTo( propertyValue ), propertyFunction );
  }

  /**
   * Convenience method for a predicate on a property value.
   *
   * @param propertyValues The property values to match
   * @param propertyFunction The function to extract the property
   * @param <T> The predicate type
   * @param <PT> The property type
   * @return A predicate that extracts a value to compare with the given values.
   */
  public static <T,PT> Predicate<T> propertyPredicate( final Collection<PT> propertyValues,
                                                       final Function<T,PT> propertyFunction ) {
    return Predicates.compose( Predicates.in( propertyValues ), propertyFunction );
  }

  /**
   * Convenience method for a predicate on a property value.
   *
   * @param propertyValue The property value to match
   * @param propertyFunction The function to extract the collection property
   * @param <T> The predicate type
   * @param <PCT> The property collection type
   * @param <PIT> The property collection item type
   * @return A predicate that extracts a value to compare with the given value.
   */
  public static <T,PIT, PCT extends Collection<? super PIT>> Predicate<T> propertyContainsPredicate(
      final PIT propertyValue,
      final Function<T,PCT> propertyFunction
  ) {
    return Predicates.compose( contains( propertyValue ), propertyFunction );
  }

  public static <T> CompatFunction<T,List<T>> listUnit() {
    return new CompatFunction<T,List<T>>() {
      @SuppressWarnings( "unchecked" )
      @Override
      public List<T> apply( final T t ) {
        return t == null ?
            Lists.<T>newArrayList() :
            Lists.newArrayList( t );
      }
    };
  }

  public static <T> CompatFunction<List<List<T>>,List<T>> listJoin() {
    return new CompatFunction<List<List<T>>,List<T>>() {
      @SuppressWarnings( "unchecked" )
      @Override
      public List<T> apply( final List<List<T>> t ) {
        return t == null ?
            Lists.<T>newArrayList() :
            Lists.newArrayList( Iterables.concat( t ) );
      }
    };
  }

  public static <T> CompatFunction<T,Optional<T>> optionalUnit( ) {
    return new CompatFunction<T,Optional<T>>() {
      @Override
      public Optional<T> apply( final T t ) {
        return Optional.fromNullable( t );
      }
    };
  }

  public static <T> Function<Optional<T>,T> optionalOrNull() {
    return new Function<Optional<T>,T>( ) {
      @Nullable
      @Override
      public T apply( final Optional<T> optional ) {
        return optional == null ?
            null :
            optional.orNull( );
      }
    };
  }

  public static <T> Function<Optional<T>,T> optionalOr( final T value ) {
    return new Function<Optional<T>,T>( ) {
      @Nullable
      @Override
      public T apply( final Optional<T> optional ) {
        return optional == null ?
            null :
            optional.or( value );
      }
    };
  }

  /**
   * Unchecked cast function.
   *
   * @param target The type to cast to
   * @param <F> The source type
   * @param <T> The result type
   * @return A function that casts to the given type
   * @see Predicates#instanceOf(Class)
   * @see Iterables#filter(Iterable, Class)
   */
  public static <F,T> Function<F,T> cast( final Class<T> target ) {
    //noinspection unchecked
    return (Function<F,T>) Functions.identity( );
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
  public static <T,I> T reduce( final Iterable<? extends I> iterable,
                                final T initialValue,
                                final Function<T,Function<I,T>> reducer ) {
    T value = initialValue;
    for ( I item : iterable ) {
      value = reducer.apply( value ).apply( item );
    }
    return value;
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
  public static <T,I> T reduce( final Iterable<? extends I> iterable,
                                final T initialValue,
                                final BiFunction<T,I,T> reducer ) {
    T value = initialValue;
    for ( I item : iterable ) {
      value = reducer.apply( value, item );
    }
    return value;
  }

  public static <I,K,V> Map<K,V> putAll( final Iterable<? extends I> iterable,
                                         final Map<K,V> targetMap,
                                         final Function<? super I, K> keyFunction,
                                         final Function<? super I, V> valueFunction ) {
    if ( iterable != null ) for ( final I item : iterable ) {
      targetMap.put( keyFunction.apply( item ), valueFunction.apply( item ) );
    }
    return targetMap;
  }

  public static <I,K,V> Multimap<K,V> putAll( final Iterable<? extends I> iterable,
                                              final Multimap<K,V> targetMap,
                                              final Function<? super I, K> keyFunction,
                                              final Function<? super I, V> valueFunction ) {
    if ( iterable != null ) for ( final I item : iterable ) {
      targetMap.put( keyFunction.apply( item ), valueFunction.apply( item ) );
    }
    return targetMap;
  }

  /**
   * Transform the given map, ignoring mapped entries with null keys or values.
   *
   * <p>WARNING! if the transform produces duplicate keys entries will be
   * overwritten.</p>
   *
   * @param map The map to transform
   * @param targetMap The result map
   * @param keyFunction The key transform
   * @param valueFunction The value transform
   * @param <K1> The source key type
   * @param <V1> The source value type
   * @param <K2> The target key type
   * @param <V2> The target value type
   * @return The supplied target map
   */
  public static <K1,V1,K2,V2> Map<K2,V2> transform( final Map<K1,V1> map,
                                                    final Map<K2,V2> targetMap,
                                                    final Function<? super K1, K2> keyFunction,
                                                    final Function<? super V1, V2> valueFunction ) {
    if ( map != null ) for ( final Map.Entry<K1,V1> entry : map.entrySet( ) ) {
      final K2 targetKey = keyFunction.apply( entry.getKey( ) );
      final V2 targetValue = valueFunction.apply( entry.getValue( ) );
      if ( targetKey != null && targetValue != null ) {
        targetMap.put( targetKey, targetValue );
      }
    }
    return targetMap;
  }

  /**
   * Min function suitable for use with reduce.
   *
   * @return The min function.
   */
  public static NonNullFunction<Integer,Function<Integer,Integer>> min() {
    return new NonNullFunction<Integer,Function<Integer,Integer>>() {
      @Nonnull
      @Override
      public Function<Integer, Integer> apply( final Integer integer1 ) {
        return new Function<Integer, Integer>(){
          @Override
          public Integer apply( final Integer integer2 ) {
            return Math.min( integer1, integer2 );
          }
        };
      }
    };
  }

  /**
   * Min function suitable for use with reduce.
   *
   * @return The long min function.
   */
  public static NonNullFunction<Long,Function<Long,Long>> lmin() {
    return new NonNullFunction<Long,Function<Long,Long>>() {
      @Nonnull
      @Override
      public Function<Long, Long> apply( final Long long1 ) {
        return new Function<Long, Long>(){
          @Override
          public Long apply( final Long long2 ) {
            return Math.min( long1, long2 );
          }
        };
      }
    };
  }

  /**
   * Count function suitable for use with reduce.
   *
   * @param evaluator Predicate matching items to be counted.
   * @param <I> The evaluated type
   * @return The count function.
   */
  public static <I> Function<Integer,Function<I,Integer>> count( final Predicate<? super I> evaluator ) {
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
   * Function suitable for use with reduce that enforces a single item.
   *
   * @param <I> The evaluated type
   * @return The reduction function.
   */
  public static <I> Function<I,Function<I,I>> unique( ) {
    return new Function<I,Function<I,I>>( ) {
      @Override
      public Function<I, I> apply( final I item1 ) {
        return new Function<I, I>() {
          @Override
          public I apply( final I item2 ) {
            if ( item1 != null && item2 != null ) {
              throw new IllegalArgumentException( "Not unique" );
            }
            return item1 == null ?
                item2 :
                item1;
          }
        };
      }
    };
  }

  /**
   * Function suitable for use with reduce that add to the initial collection.
   *
   * @param <I> The evaluated type
   * @return The reduction function.
   */
  public static <IT,I extends Collection<IT>> Function<I,Function<I,I>> addAll( ) {
    return new Function<I,Function<I,I>>( ) {
      @Override
      public Function<I, I> apply( final I reduction ) {
        return new Function<I, I>() {
          @Override
          public I apply( final I collection ) {
            if ( collection != null ) {
              reduction.addAll( collection );
            }
            return reduction;
          }
        };
      }
    };
  }

  /**
   * Flip parameter order for curried function.
   *
   * @param curried The function to flip
   * @param <F1> The first parameter type
   * @param <F2> The second parameter type
   * @param <T> The result type
   * @return The flipped function
   */
  public static <F1,F2,T> NonNullFunction<F2,Function<F1,T>> flipCurried( final Function<F1,Function<F2,T>> curried )  {
    return new NonNullFunction<F2,Function<F1,T>>( ){
      @Nonnull
      @Override
      public Function<F1, T> apply( @Nullable final F2 f2 ) {
        return new Function<F1, T>( ){
          @Override
          public T apply( @Nullable final F1 f1 ) {
            //noinspection ConstantConditions
            return curried.apply( f1 ).apply( f2 );
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
