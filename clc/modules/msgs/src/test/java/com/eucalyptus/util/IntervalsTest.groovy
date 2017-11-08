/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
