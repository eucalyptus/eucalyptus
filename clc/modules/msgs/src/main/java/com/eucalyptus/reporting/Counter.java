/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.reporting;

import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.LongPredicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import com.eucalyptus.util.Assert;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.vavr.Tuple;
import io.vavr.Tuple2;

/**
 *
 */
public class Counter<T,C extends Counter.Counted> {

  private final int periodLength; // how long is each window
  private final int periodCount;  // how many windows

  private final Function<? super T,C> countedFunction;     // build a counted item from an input
  private final Function<? super T,Integer> countFunction; // build the count for the input (perhaps X per item)

  private final Clock clock; // clock for current time

  private final AtomicReference<List<CountPeriod<C>>> periods = new AtomicReference<>( Collections.emptyList( ) );

  public Counter(
      final int periodLength,
      final int periodCount,
      @Nonnull final Function<? super T, C> countedFunction
  ) {
    this( Clock.systemUTC( ), periodLength, periodCount, countedFunction, c -> 1 );
  }

  public Counter(
               final int periodLength,
               final int periodCount,
      @Nonnull final Function<? super T, C> countedFunction,
      @Nonnull final Function<? super T, Integer> countFunction
  ) {
    this( Clock.systemUTC( ), periodLength, periodCount,countedFunction, countFunction );
  }

  public Counter(
      @Nonnull final Clock clock,
               final int periodLength,
               final int periodCount,
      @Nonnull final Function<? super T, C> countedFunction,
      @Nonnull final Function<? super T, Integer> countFunction
  ) {
    this.clock = Assert.notNull( clock, "clock" );
    this.periodLength = periodLength;
    this.periodCount = periodCount;
    this.countedFunction = Assert.notNull( countedFunction, "countedFunction" );
    this.countFunction = Assert.notNull( countFunction, "countFunction" );
  }

  /**
   * Count the given item at the current time.
   */
  public void count( final T t ) {
    count( clock.millis( ), t );
  }

  /**
   * Count the given item at the given time.
   */
  public void count( final long time, final T t ) {
    if ( t != null ) {
      final C counted = countedFunction.apply( t );
      final int count = countFunction.apply( t );
      if ( counted != null && count > 0 ) {
        period( time ).count( counted, count );
      }
    }
  }

  public long lastPeriodEnd( ) {
    return lastPeriodEnd( clock.millis( ) );
  }

  public long lastPeriodEnd( final long time ) {
    final long periodLengthLong = periodLength;
    return  ( time / periodLengthLong ) * periodLengthLong;
  }

  public CounterSnapshot<C> snapshot( ) {
    return snapshot( clock.millis( ) );
  }

  public CounterSnapshot<C> snapshot( final long time ) {
    final List<CountPeriod<C>> periodList = periods.get( ).stream( )
        .filter( p -> p.key.end <= time )
        .collect( Collectors.toList( ) );
    if ( periodList.isEmpty( ) ) {
      periodList.add( newPeriod( time, started( periodList ), time ) );
    }
    return new CounterSnapshot<>(
        periodList.stream( ).map( CountPeriod::snapshot ).collect( Collectors.toList( ) )
    );
  }

  public Tuple2<Long,Integer> total( ) {
    final List<CountPeriod<C>> periodList = periods.get( );
    final int totalCount = periodList.stream( )
        .<Number>flatMap( p -> p.counts.values( ).stream( ) )
        .reduce( 0, ( a, b) -> a.intValue( ) + b.intValue( ) )
        .intValue( );
    return Tuple.of( created( periodList ), totalCount );
  }

  public Tuple2<Long,Integer> accountTotal( final String account ) {
    Assert.notNull( account, "account" );
    final List<CountPeriod<C>> periodList = periods.get( );
    final int totalCount = periodList.stream( )
        .<Number>flatMap( p -> p.counts.entrySet( ).stream( )
            .filter( entry -> account.equals( entry.getKey( ).getAccount( ) ) )
            .map( Entry::getValue ) )
        .reduce( 0, ( a, b) -> a.intValue( ) + b.intValue( ) )
        .intValue( );
    return Tuple.of( created( periodList ), totalCount );
  }

  private long created( final List<CountPeriod<C>> periodList ) {
    return periodList.isEmpty( ) ?
        clock.millis( ) :
        Iterables.getLast( periodList ).key.created;
  }

  private long started( final List<CountPeriod<C>> periodList ) {
    final long periodLengthLong = periodLength;
    return periodList.isEmpty( ) ?
        ( clock.millis( ) / periodLengthLong ) * periodLengthLong :
        Iterables.getLast( periodList ).key.start;
  }

  private CountPeriod<C> period( final long time ) {
    Optional<CountPeriod<C>> period = Optional.empty( );
    while ( !period.isPresent() ) {
      period = periods.get( ).stream( ).filter( p -> p.test( time ) ).findFirst( );
      if ( !period.isPresent( ) ) {
        final List<CountPeriod<C>> periodList = periods.get( );
        final List<CountPeriod<C>> newPeriodList = Lists.newArrayList( );
        newPeriodList.add( newPeriod( time ) );
        while( newPeriodList.size( ) < periodCount && !periodList.isEmpty( ) ) {
          if (  newPeriodList.get( newPeriodList.size( ) - 1 ).key.start != periodList.get( 0 ).key.end ) {
            newPeriodList.add( newPeriod( ( newPeriodList.get( newPeriodList.size( ) - 1 ).key.start - periodLength ) ) );
          } else {
            break;
          }
        }
        if( newPeriodList.size( ) < periodCount ) {
          Iterables.addAll( newPeriodList, Iterables.limit( periodList, periodCount - newPeriodList.size( ) ) );
        }
        periods.compareAndSet( periodList, ImmutableList.copyOf( newPeriodList ) );
      }
    }
    return period.get( );
  }

  public String toString( ) {
    final Tuple2<Long,Integer> totals = total( );
    return MoreObjects.toStringHelper( this )
        .add( "totalCount", totals._2( ) )
        .add( "since", totals._1( ) )
        .toString( );
  }

  private CountPeriod<C> newPeriod( long time ) {
    final long now = clock.millis( );
    final long periodLengthLong = periodLength;
    final long start =  ( time / periodLengthLong ) * periodLengthLong;
    final long end = start + periodLength;
    if ( start < (  - ( periodLength * 2 ) ) ) {
      throw new IllegalArgumentException( "Not creating expired period " + time );
    }
    return newPeriod( now, start, end );
  }

  static <C extends Counted> CountPeriod<C> newPeriod( final long created, final long start, final long end ) {
    return new CountPeriod<>( newKey( created, start, end ) );
  }

  static CountPeriodKey newKey( final long created, final long start, final long end ) {
    return new CountPeriodKey( created, start, end );
  }

  static <C extends Counted> List<CounterPeriodSnapshot<C>> since(
      final List<CounterPeriodSnapshot<C>> snapshots,
      final List<CounterPeriodSnapshot<C>> oldSnapshosts ) {
    return snapshots.stream( ).map( s -> s.subtractMatching( oldSnapshosts ) ).collect( Collectors.toList( ) );
  }

  static final class CountPeriodKey implements LongPredicate {
    private final long start;
    private final long end;
    private final long created;

    CountPeriodKey( final long created, final long start, final long end ) {
      this.created = created;
      this.start = start;
      this.end = end;
    }

    @Override
    public boolean test( final long time ) {
      return time >= start && time < end;
    }

    static CountPeriodKey combine( final CountPeriodKey key1, final CountPeriodKey key2 ) {
      return new CountPeriodKey(
        Math.max( key1.created, key2.created ),
        Math.min( key1.start, key2.start ),
        Math.max( key1.end, key2.end )
      );
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper( this )
          .add( "start", start )
          .add( "end", end )
          .add( "created", created )
          .toString( );
    }
  }

  public static final class CounterSnapshot<C extends Counter.Counted> {
    private final CounterPeriodSnapshot<C> aggregate;
    private final List<CounterPeriodSnapshot<C>> periodSnapshots;

    public CounterSnapshot( final List<CounterPeriodSnapshot<C>> periodSnapshots ) {
      this.aggregate = new CountPeriod<>( periodSnapshots ).snapshot( );
      this.periodSnapshots = periodSnapshots;
    }

    public long getPeriodStart( ) {
      return aggregate.key.start;
    }

    public long getPeriodEnd( ) {
      return aggregate.key.end;
    }

    public Iterable<C> counted( ) {
      return aggregate.counts.keySet( );
    }

    public int total( ) {
      return aggregate.counts.values( ).stream( ).reduce( 0, Integer::sum );
    }

    public Iterable<Tuple2<C,Integer>> counts( ) {
      return ()->aggregate.counts.entrySet( ).stream( ).map( e -> Tuple.of( e.getKey( ), e.getValue( ) ) ).iterator( );
    }

    public CounterSnapshot<C> since( final CounterSnapshot<C> old ) {
      if ( Assert.notNull( old, "old" ).aggregate.key.end > aggregate.key.end ) {
        throw new IllegalArgumentException( "Old snapshot is newer than current" );
      }
      return new CounterSnapshot<>( Counter.since( periodSnapshots, old.periodSnapshots ) );
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper( this )
          .add( "key", aggregate.key )
          .add( "countSize", aggregate.counts.size( ) )
          .toString( );
    }
  }

  static final class CounterPeriodSnapshot<C extends Counter.Counted> {
    private final CountPeriodKey key;
    private final Map<C,Integer> counts;

    CounterPeriodSnapshot( final CountPeriod<C> period ) {
      this(
          period.key,
          period.counts.entrySet( ).stream( )
              .collect( Collectors.toMap( Entry::getKey, e -> e.getValue( ).intValue( ) ) ) );
    }

    CounterPeriodSnapshot( final CountPeriodKey key, final Map<C, Integer> counts ) {
      this.key = key;
      this.counts = ImmutableMap.copyOf( counts );
    }

    CounterPeriodSnapshot<C> subtractMatching( final List<CounterPeriodSnapshot<C>> oldSnapshosts ) {
      final Optional<CounterPeriodSnapshot<C>> matching =
          oldSnapshosts.stream( ).filter( s -> s.key.equals( key ) ).findFirst( );
      if ( matching.isPresent( ) ) {
        final CounterPeriodSnapshot<C> oldSnapshot = matching.get( );
        return new CounterPeriodSnapshot<>( key, counts.entrySet( ).stream( ).map( entry -> {
          final Integer oldCount = oldSnapshot.counts.get( entry.getKey( ) );
          return Tuple.of( entry.getKey( ), oldCount==null ? entry.getValue( ) : entry.getValue( ) - oldCount  );
        } ).collect( Collectors.toMap( Tuple2::_1, Tuple2::_2 ) ) );
      } else {
        return this;
      }
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper( this )
          .add( "key", key )
          .add( "countSize", counts.size( ) )
          .toString( );
    }
  }

  static final class CountPeriod<C extends Counter.Counted> implements LongPredicate {
    private final CountPeriodKey key;

    private final ConcurrentMap<C,AtomicInteger> counts = Maps.newConcurrentMap( );

    CountPeriod( final CountPeriodKey key ) {
      this.key = key;
    }

    /**
     * Create a period from a non empty list of snapshots
     */
    CountPeriod( final List<CounterPeriodSnapshot<C>> snapshots ) {
      this.key = snapshots.stream( ).map( s -> s.key ).reduce( CountPeriodKey::combine ).get( );
      snapshots.forEach( s -> s.counts.entrySet( ).forEach( entry -> count( entry.getKey( ), entry.getValue( ) ) ) );
    }

    int count( C counted, int count ) {
      return counts.computeIfAbsent( counted, c -> new AtomicInteger( ) ).addAndGet( count );
    }

    CounterPeriodSnapshot<C> snapshot( ) {
      return new CounterPeriodSnapshot<>( this );
    }

    @Override
    public boolean test( final long time ) {
      return key.test( time );
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper( this )
          .add( "key", key )
          .add( "countSize", counts.size( ) )
          .toString( );
    }
  }

  public static class Counted {
    private final String account;
    private final String item;

    public Counted( final String account, final String item ) {
      this.account = account;
      this.item = item;
    }

    public String getAccount( ) {
      return account;
    }

    public String getItem( ) {
      return item;
    }

    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass( ) != o.getClass( ) ) return false;
      final Counted counted = (Counted) o;
      return Objects.equals( account, counted.account ) &&
          Objects.equals( item, counted.item );
    }

    @Override
    public int hashCode() {
      return Objects.hash( account, item );
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper( this )
          .add( "account", account )
          .add( "item", item )
          .toString( );
    }
  }
}
