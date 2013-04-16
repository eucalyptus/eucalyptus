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
