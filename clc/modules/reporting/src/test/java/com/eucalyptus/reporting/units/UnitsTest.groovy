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
