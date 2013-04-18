/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.util

import static org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.text.ParseException

/**
 * 
 */
class IntervalsTest {

  @Test
  void testParsingWithUnits() {
    assertEquals( "milliseconds", 1L, Intervals.parse( "1ms" ) );
    assertEquals( "seconds",   1000L, Intervals.parse( "1s" ) );
    assertEquals( "minutes",  60000L, Intervals.parse( "1m" ) );
    assertEquals( "hours",  3600000L, Intervals.parse( "1h" ) );
    assertEquals( "days",  86400000L, Intervals.parse( "1d" ) );
  }

  @Test
  void testParsingWithUnitsAndDefaultValues() {
    assertEquals( "milliseconds", 1L, Intervals.parse( "1ms", 1 ) );
    assertEquals( "milliseconds", 1L, Intervals.parse( "Xms", 1) );
    assertEquals( "seconds",   1000L, Intervals.parse( "1s", 1000 ) );
    assertEquals( "seconds",   1000L, Intervals.parse( "Xs", 1000 ) );
    assertEquals( "minutes",  60000L, Intervals.parse( "1m", 60000 ) );
    assertEquals( "minutes",  60000L, Intervals.parse( "Xm", 60000 ) );
    assertEquals( "hours",  3600000L, Intervals.parse( "1h", 3600000 ) );
    assertEquals( "hours",  3600000L, Intervals.parse( "Xh", 3600000 ) );
    assertEquals( "days",  86400000L, Intervals.parse( "1d", 86400000 ) );
    assertEquals( "days",  86400000L, Intervals.parse( "Xd", 86400000 ) );
  }

  @Test
  void testParsingWithDefaultUnits() {
    assertEquals( "milliseconds", 1L, Intervals.parse( "1", TimeUnit.MILLISECONDS ) );
    assertEquals( "seconds",   1000L, Intervals.parse( "1", TimeUnit.SECONDS ) );
    assertEquals( "minutes",  60000L, Intervals.parse( "1", TimeUnit.MINUTES ) );
    assertEquals( "hours",  3600000L, Intervals.parse( "1", TimeUnit.HOURS ) );
    assertEquals( "days",  86400000L, Intervals.parse( "1", TimeUnit.DAYS ) );
  }

  @Test
  void testParsingWithDefaultUnitsAndDefaultValues() {
    assertEquals( "milliseconds", 1L, Intervals.parse( "1", TimeUnit.MILLISECONDS, 1 ) );
    assertEquals( "milliseconds", 1L, Intervals.parse( "X", TimeUnit.MILLISECONDS, 1) );
    assertEquals( "seconds",   1000L, Intervals.parse( "1", TimeUnit.SECONDS, 1000 ) );
    assertEquals( "seconds",   1000L, Intervals.parse( "X", TimeUnit.SECONDS, 1000 ) );
    assertEquals( "minutes",  60000L, Intervals.parse( "1", TimeUnit.MINUTES, 60000 ) );
    assertEquals( "minutes",  60000L, Intervals.parse( "X", TimeUnit.MINUTES, 60000 ) );
    assertEquals( "hours",  3600000L, Intervals.parse( "1", TimeUnit.HOURS, 3600000 ) );
    assertEquals( "hours",  3600000L, Intervals.parse( "X", TimeUnit.HOURS, 3600000 ) );
    assertEquals( "days",  86400000L, Intervals.parse( "1", TimeUnit.DAYS, 86400000 ) );
    assertEquals( "days",  86400000L, Intervals.parse( "X", TimeUnit.DAYS, 86400000 ) );
  }

  @Test(expected=ParseException)
  void testInvalidUnits() {
    Intervals.parse( "1day" )
  }

  @Test(expected=ParseException)
  void testInvalidValue() {
    Intervals.parse( "fooms" )
  }

  @Test(expected=ParseException)
  void testInvalidValueLong() {
    Intervals.parse( "111111111111111111111111111111111111111111111111111ms" )
  }

}
