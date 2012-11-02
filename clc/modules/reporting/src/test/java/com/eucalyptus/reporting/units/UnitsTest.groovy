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
