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
class TimestampsTest {

  @Test
  void testParsingAccuracy() {
    Date time = new Date( 1358200169581L ) // any date with non zero millis
    for ( final String pattern : Timestamps.iso8601 ) {
      Date result = Timestamps.parseIso8601Timestamp( sdf( pattern ).format( time ) )
      long accuracy = pattern.contains( "SSS" ) ? 1 : 1000
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
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "2013-01-14T21:49:29.581Z" ) )
    assertEquals( new Date(1358200169000L), Timestamps.parseIso8601Timestamp( "20130114T214929" ) )
    assertEquals( new Date(1358200169000L), Timestamps.parseIso8601Timestamp( "20130114T214929Z" ) )
    assertEquals( new Date(1358200169581L), Timestamps.parseIso8601Timestamp( "20130114T214929.581Z" ) )
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
