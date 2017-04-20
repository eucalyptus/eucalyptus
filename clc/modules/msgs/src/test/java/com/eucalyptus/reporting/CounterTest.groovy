/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.reporting

import com.eucalyptus.reporting.Counter.Counted
import com.eucalyptus.reporting.Counter.CountedS3
import com.eucalyptus.reporting.Counter.CounterSnapshot
import com.eucalyptus.testing.SuppliedClock
import com.google.common.collect.ImmutableMap

import java.util.concurrent.atomic.AtomicLong

import static org.junit.Assert.*
import org.junit.Test

/**
 *
 */
class CounterTest {

  @Test
  void testBasicCounter( ) {
    AtomicLong time = timeOnPeriodStart( 1000 );
    Counter<Integer,Counted> counter =
        new Counter<>( SuppliedClock.of({time.get( )}), 1000, 10, {new Counted( "000000000000", "count" ) }, {it}  )
    counter.count( 10 )
    counter.count( 7 )
    println counter
    assertEquals( 'total', 17, counter.total( )._2() )
  }

  @Test
  void testPeriodRotation( ) {
    AtomicLong time = timeOnPeriodStart( 1000 );
    Counter<Integer,Counted> counter =
        new Counter<>( SuppliedClock.of({time.get( )}), 1000, 3, { new Counted( "000000000000", "count" ) }, {it}  )
    counter.count( 1 )
    time.addAndGet( 1000L )
    counter.count( 1 )
    time.addAndGet( 1000L )
    counter.count( 1 )
    time.addAndGet( 1000L )
    counter.count( 1 )
    println counter
    assertEquals( 'total', 3, counter.total( )._2() )
  }

  @Test
  void testMultipleItems( ) {
    AtomicLong time = timeOnPeriodStart( 1000 );
    Counter<String,Counted> counter =
        new Counter<>( SuppliedClock.of({time.get( )}), 1000, 10, { new Counted( "000000000000", it ) }, {1}  )
    counter.count( 'foo' )
    counter.count( 'foo' )
    time.addAndGet( 1000L )
    counter.count( 'foo' )
    counter.count( 'bar' )
    counter.count( 'baz' )
    println counter
    assertEquals( 'total', 5, counter.total( )._2() )
  }

  @Test
  void testMultipleItemsAndAccounts( ) {
    AtomicLong time = timeOnPeriodStart( 1000 );
    Counter<List<String>,Counted> counter =
        new Counter<>( SuppliedClock.of({time.get( )}), 1000, 10, { new Counted( it.get(0), it.get(1) ) }, {1}  )
    counter.count( ['000000000000', 'foo'] )
    counter.count( ['111111111111', 'foo'] )
    time.addAndGet( 1000L )
    counter.count( ['000000000000', 'foo'] )
    counter.count( ['000000000000', 'bar'] )
    counter.count( ['000000000000', 'bar'] )
    counter.count( ['111111111111', 'baz'] )
    println counter
    assertEquals( '000000000000 account total', 4, counter.accountTotal( '000000000000')._2() )
    assertEquals( '111111111111 account total', 2, counter.accountTotal( '111111111111' )._2() )
    assertEquals( 'total', 6, counter.total( )._2() )
  }

  @Test
  void testMultipleItemsAndAccountsS3( ) {
    AtomicLong time = timeOnPeriodStart( 1000 );
    Counter<List<String>,CountedS3> counter =
        new Counter<>( SuppliedClock.of({time.get( )}), 1000, 10, { new CountedS3( it.get(0), it.get(1), "bucket1", new Long(100L)) }, {2}  )
    counter.count( ['000000000000', 'foo'] )
    counter.count( ['111111111111', 'foo'] )
    time.addAndGet( 1000L )
    counter.count( ['000000000000', 'foo'] )
    counter.count( ['000000000000', 'bar'] )
    counter.count( ['000000000000', 'bar'] )
    counter.count( ['111111111111', 'baz'] )
    println counter
    assertEquals( '000000000000 account total', 8, counter.accountTotal( '000000000000')._2() )
    assertEquals( '111111111111 account total', 4, counter.accountTotal( '111111111111' )._2() )
    assertEquals( 'total', 12, counter.total( )._2() )
  }

  @Test
  void testEmptySnapshot( ) {
    AtomicLong time = timeOnPeriodStart(1000);
    Counter<List<String>, Counted> counter =
        new Counter<>(SuppliedClock.of({ time.get() }), 1000, 10, { new Counted(it.get(0), it.get(1)) }, { 1 })
    CounterSnapshot snapshot = counter.snapshot( )
    println snapshot
    assertEquals( 'snapshot total', 0, snapshot.total( ) )
    assertEquals( 'snapshot start', time.get( ), snapshot.periodStart )
    assertEquals( 'snapshot end', time.get( ), snapshot.periodEnd )
    assertEquals( 'snapshot counts',
        Collections.emptyMap( ),
        snapshot.counts( ).collectEntries( new HashMap<>(), { t -> [ t._1(), t._2( ) ] } ) )
  }

  @Test
  void testSnapshot( ) {
    AtomicLong time = timeOnPeriodStart( 1000 );
    Counter<List<String>,Counted> counter =
        new Counter<>( SuppliedClock.of({time.get( )}), 1000, 10, { new Counted( it.get(0), it.get(1) ) }, {1}  )
    counter.count( ['000000000000', 'foo'] )
    counter.count( ['111111111111', 'foo'] )
    time.addAndGet( 1000L )
    counter.count( ['000000000000', 'foo'] )
    counter.count( ['000000000000', 'bar'] )
    counter.count( ['000000000000', 'bar'] )
    counter.count( ['111111111111', 'baz'] )
    println counter
    assertEquals( 'total', 6, counter.total( )._2() )
    time.addAndGet( 1000L )
    CounterSnapshot snapshot = counter.snapshot( )
    println snapshot
    assertEquals( 'snapshot total', 6, snapshot.total( ) )
    assertEquals( 'snapshot start', time.get( ) - 2000, snapshot.periodStart )
    assertEquals( 'snapshot end', time.get( ), snapshot.periodEnd )
    assertEquals( 'snapshot counts',
        ImmutableMap.of(
            new Counted( '000000000000', 'foo' ), 2,
            new Counted( '111111111111', 'foo' ), 1,
            new Counted( '000000000000', 'bar' ), 2,
            new Counted( '111111111111', 'baz' ), 1
        ),
        snapshot.counts( ).collectEntries( new HashMap<>(), { t -> [ t._1(), t._2( ) ] } ) )
  }

  @Test
  void testSnapshotSince( ) {
    AtomicLong time = timeOnPeriodStart( 1000 );
    Counter<List<String>,Counted> counter =
        new Counter<>( SuppliedClock.of({time.get( )}), 1000, 10, { new Counted( it.get(0), it.get(1) ) }, {1}  )
    counter.count( ['000000000000', 'foo'] )
    counter.count( ['111111111111', 'foo'] )
    time.addAndGet( 1000L )
    counter.count( ['000000000000', 'foo'] )
    counter.count( ['000000000000', 'bar'] )
    counter.count( ['000000000000', 'bar'] )
    counter.count( ['111111111111', 'baz'] )
    println counter
    assertEquals( 'total', 6, counter.total( )._2() )
    time.addAndGet( 1000L )
    CounterSnapshot snapshot1 = counter.snapshot( )
    println snapshot1
    counter.count( ['000000000000', 'foo'] )
    counter.count( ['000000000000', 'bar'] )
    counter.count( ['000000000000', 'bar'] )
    counter.count( ['111111111111', 'baz'] )
    println counter
    assertEquals( 'total', 10, counter.total( )._2() )
    time.addAndGet( 1000L )
    CounterSnapshot snapshot2 = counter.snapshot( )
    println snapshot2
    assertEquals( 'snapshot2 total', 10, snapshot2.total( ) )
    assertEquals( 'snapshot2 start', time.get( ) - 3000, snapshot2.periodStart )
    assertEquals( 'snapshot2 end', time.get( ), snapshot2.periodEnd )
    assertEquals( 'snapshot2 counts',
        ImmutableMap.of(
            new Counted( '000000000000', 'foo' ), 3,
            new Counted( '111111111111', 'foo' ), 1,
            new Counted( '000000000000', 'bar' ), 4,
            new Counted( '111111111111', 'baz' ), 2
        ),
        snapshot2.counts( ).collectEntries( new HashMap<>(), { t -> [ t._1(), t._2( ) ] } ) )
    CounterSnapshot snapshotSince = snapshot2.since( snapshot1 )
    assertEquals( 'snapshotSince total', 4, snapshotSince.total( ) )
    assertEquals( 'snapshotSince start', time.get( ) - 3000, snapshotSince.periodStart )
    assertEquals( 'snapshotSince end', time.get( ), snapshotSince.periodEnd )
    assertEquals( 'snapshotSince counts',
        ImmutableMap.of(
            new Counted( '000000000000', 'foo' ), 1,
            new Counted( '111111111111', 'foo' ), 0,
            new Counted( '000000000000', 'bar' ), 2,
            new Counted( '111111111111', 'baz' ), 1
        ),
        snapshotSince.counts( ).collectEntries( new HashMap<>(), { t -> [ t._1(), t._2( ) ] } ) )

    CounterSnapshot snapshotSince2 = snapshot2.since( snapshot2 )
    assertEquals( 'snapshotSince2 total', 0, snapshotSince2.total( ) )
    assertEquals( 'snapshotSince2 start', time.get( ) - 3000, snapshotSince2.periodStart )
    assertEquals( 'snapshotSince2 end', time.get( ), snapshotSince2.periodEnd )
    assertEquals( 'snapshotSince2 counts',
        ImmutableMap.of(
            new Counted( '000000000000', 'foo' ), 0,
            new Counted( '111111111111', 'foo' ), 0,
            new Counted( '000000000000', 'bar' ), 0,
            new Counted( '111111111111', 'baz' ), 0
        ),
        snapshotSince2.counts( ).collectEntries( new HashMap<>(), { t -> [ t._1(), t._2( ) ] } ) )
  }

  @Test
  void testSnapshotSincePeriodRotated( ) {
    AtomicLong time = timeOnPeriodStart( 1000 );
    Counter<List<String>,Counted> counter =
        new Counter<>( SuppliedClock.of({time.get( )}), 1000, 4, { new Counted( it.get(0), it.get(1) ) }, {1}  )
    counter.count( ['000000000000', 'foo'] )
    counter.count( ['111111111111', 'foo'] )
    time.addAndGet( 1000L )
    counter.count( ['000000000000', 'foo'] )
    counter.count( ['000000000000', 'bar'] )
    time.addAndGet( 1000L )
    CounterSnapshot snapshot1 = counter.snapshot( )
    println snapshot1
    time.addAndGet( 1000L )
    counter.count( ['000000000000', 'bar'] )
    counter.count( ['111111111111', 'baz'] )
    println counter
    assertEquals( 'total', 6, counter.total( )._2() )
    time.addAndGet( 1000L )
    counter.count( ['000000000000', 'foo'] )
    counter.count( ['000000000000', 'bar'] )
    counter.count( ['000000000000', 'bar'] )
    counter.count( ['111111111111', 'baz'] )
    println counter
    assertEquals( 'total', 8, counter.total( )._2() )
    time.addAndGet( 1000L )
    CounterSnapshot snapshot2 = counter.snapshot( )
    println snapshot2
    assertEquals( 'snapshot2 total', 8, snapshot2.total( ) )
    assertEquals( 'snapshot2 start', time.get( ) - 4000, snapshot2.periodStart )
    assertEquals( 'snapshot2 end', time.get( ), snapshot2.periodEnd )
    assertEquals( 'snapshot2 counts',
        ImmutableMap.of(
            new Counted( '000000000000', 'foo' ), 2,
            new Counted( '000000000000', 'bar' ), 4,
            new Counted( '111111111111', 'baz' ), 2
        ),
        snapshot2.counts( ).collectEntries( new HashMap<>(), { t -> [ t._1(), t._2( ) ] } ) )
    CounterSnapshot snapshotSince = snapshot2.since( snapshot1 )
    assertEquals( 'snapshotSince total', 6, snapshotSince.total( ) )
    assertEquals( 'snapshotSince start', time.get( ) - 4000, snapshotSince.periodStart )
    assertEquals( 'snapshotSince end', time.get( ), snapshotSince.periodEnd )
    assertEquals( 'snapshotSince counts',
        ImmutableMap.of(
            new Counted( '000000000000', 'foo' ), 1,
            new Counted( '000000000000', 'bar' ), 3,
            new Counted( '111111111111', 'baz' ), 2
        ),
        snapshotSince.counts( ).collectEntries( new HashMap<>(), { t -> [ t._1(), t._2( ) ] } ) )
  }

  private static timeOnPeriodStart( long period ) {
    new AtomicLong( BigInteger.valueOf( System.currentTimeMillis( ) ).intdiv( period ).longValue( ) * period )
  }
}
