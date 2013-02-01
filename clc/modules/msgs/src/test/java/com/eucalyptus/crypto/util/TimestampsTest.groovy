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
package com.eucalyptus.crypto.util

import static org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import com.eucalyptus.auth.login.AuthenticationException

/**
 * 
 */
@SuppressWarnings("GroovyAccessibility")
class TimestampsTest {

  @Test
  void testParsingAccuracy() {
    Date time = new Date( 1358200169581L ) // any date with non zero millis
    for ( final Timestamps.PatternHolder pattern : Timestamps.iso8601 ) {
      // Date format puts the millis at the end, regex match/replace moves them, e.g. .000581 -> .581000 
      String formatted = sdf( pattern.pattern ).format( time ).replaceFirst( "(.*)\\.(0*)([0-9]*)(Z?)", '$1.$3$2$4' )
      Date result = Timestamps.parseIso8601Timestamp( formatted )
      long accuracy = pattern.pattern.contains( "S" ) ? 1 : 1000
      Date expected = new Date( ( time.getTime() / accuracy as Long ) * accuracy )
      assertEquals( "Round trip date for pattern " + pattern, format(expected), format(result) )
    }
  }

  @Test( expected=AuthenticationException.class )
  void testParsingTrailer() {
    Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.581Z----TRAILER-HERE----" )
  }

  @Test( expected=AuthenticationException.class )
  void testInvalidTimestamp() {
    Timestamps.parseIso8601Timestamp( "invalid" )
  }

  @Test
  void testTimestamps() {    
    assertEquals( new Date(1358200169000L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29" ) )
    assertEquals( new Date(1358200169000L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29Z" )  )
    assertEquals( new Date(1358200169000L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29Z-0000" )  )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.581Z" ) )
    assertEquals( new Date(1358200169000L), Timestamps.parseIso8601Timestamp( "20130114T214929" ) )
    assertEquals( new Date(1358200169000L), Timestamps.parseIso8601Timestamp( "20130114T214929Z" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "20130114T214929.581Z" ) )
    assertEquals( new Date(1358200169500L), Timestamps.parseIso8601Timestamp( "20130114T214929.5" ) )
    assertEquals( new Date(1358200169580L), Timestamps.parseIso8601Timestamp( "20130114T214929.58" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "20130114T214929.581" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "20130114T214929.5819" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "20130114T214929.58199" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "20130114T214929.581999" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "20130114T214929.5819999" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "20130114T214929.58199999" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "20130114T214929.581999999" ) )
    assertEquals( new Date(1358200169500L), Timestamps.parseIso8601Timestamp( "20130114T214929.5Z" ) )
    assertEquals( new Date(1358200169580L), Timestamps.parseIso8601Timestamp( "20130114T214929.58Z" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "20130114T214929.581Z" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "20130114T214929.5819Z" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "20130114T214929.58199Z" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "20130114T214929.581999Z" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "20130114T214929.5819999Z" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "20130114T214929.58199999Z" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "20130114T214929.581999999Z" ) )
    assertEquals( new Date(1358200169500L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.5" ) )
    assertEquals( new Date(1358200169580L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.58" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.581" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.5819" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.58199" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.581999" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.5819999" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.58199999" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.581999999" ) )
    assertEquals( new Date(1358200169500L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.5Z" ) )
    assertEquals( new Date(1358200169580L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.58Z" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.581Z" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.5819Z" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.58199Z" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.581999Z" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.5819999Z" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.58199999Z" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.581999999Z" ) )
  }

  @Test
  void testUnparsedFractionValidated() {
    List<String> timestamps =[
        "2013-01-14T21:49:29.581?",
        "2013-01-14T21:49:29.581_Z",
        "20130114T214929.581!",
        "20130114T214929.581.Z",
        "2013-01-14T21:49:29.581_99999Z",
        "2013-01-14T21:49:29.5819_9999Z",
        "2013-01-14T21:49:29.58199_999Z",
        "2013-01-14T21:49:29.581999_99Z",
        "2013-01-14T21:49:29.5819999_9Z",
        "2013-01-14T21:49:29.58199999_Z",
        "2013-01-14T21:49:29.581*99999",
        "2013-01-14T21:49:29.5819*9999",
        "2013-01-14T21:49:29.58199*999",
        "2013-01-14T21:49:29.581999*99",
        "2013-01-14T21:49:29.5819999*9",
        "2013-01-14T21:49:29.58199999*",
        "20130114T214929.581?00000Z",    
        "20130114T214929.5810?0000Z",    
        "20130114T214929.58100?000Z",    
        "20130114T214929.581000?00Z",    
        "20130114T214929.5810000?0Z",    
        "20130114T214929.58100000?Z",
        "20130114T214929.581?00000",
        "20130114T214929.5810?0000",
        "20130114T214929.58100?000",
        "20130114T214929.581000?00",
        "20130114T214929.5810000?0",
        "20130114T214929.58100000?",
    ]

    timestamps.each { timestamp ->
      try {
        Timestamps.parseIso8601Timestamp( timestamp )
        fail( "Timestamp should fail due to non-numeric fraction: " + timestamp )
      } catch ( AuthenticationException e ) {
        // expected  
      }
    }
    
  }


  private SimpleDateFormat sdf( final String pattern ) {
    final SimpleDateFormat format = new SimpleDateFormat( pattern )
    format.setTimeZone( TimeZone.getTimeZone( "GMT" ) )
    format
  }

  private String format( Date date ) {
    sdf( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" ).format( date )
  }
}
