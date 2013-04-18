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
package com.eucalyptus.reporting.units

import static org.junit.Assert.*
import org.junit.Test

/**
 * 
 */
class UnitsTest {

  @Test
  void testTimeUnits() {
    // Time unit naming was inconsistent, so a short, medium and full
    // name is added for each unit.
    assertUnits( TimeUnit.MS, [ "ms", "millis", "milliseconds" ] )
    assertUnits( TimeUnit.SECS, [ "s", "secs", "seconds" ] )
    assertUnits( TimeUnit.MINS, [ "m", "mins", "minutes" ] )
    assertUnits( TimeUnit.HOURS, [ "h", "hrs", "hours" ] )
    assertUnits( TimeUnit.DAYS, [ "d", "days" ] )
    assertUnits( TimeUnit.YEARS, [ "y", "years" ] )
  }

  @Test
  void testSizeUnits() {
    assertUnits( SizeUnit.B, [ "b", "bytes" ] ) // bytes is for backwards compatibility
    assertUnits( SizeUnit.KB, [ "kb" ] )
    assertUnits( SizeUnit.MB, [ "mb" ] )
    assertUnits( SizeUnit.GB, [ "gb" ] )
    assertUnits( SizeUnit.TB, [ "tb" ] )
    assertUnits( SizeUnit.PB, [ "pb" ] )
  }

  @Test
  void testNullUnits() {
    assertEquals( "SizeUnit for null", SizeUnit.B, SizeUnit.fromString( null, SizeUnit.B ) )
    assertEquals( "TimeUnit for null", TimeUnit.MS, TimeUnit.fromString( null, TimeUnit.MS ) )
  }

  private void assertUnits( final Enum enumValue,
                            final List<String> values ) {
    List<String> allValues = [] + values
    allValues += values.collect{ value -> value.toUpperCase() }
    allValues.each{ value ->
      assertEquals( "Enum for " + value ,
          enumValue,
          enumValue.getClass().getMethod( "fromString", String.class, enumValue.getClass() )
              .invoke( null, value, null ) )
    }
  }
}
