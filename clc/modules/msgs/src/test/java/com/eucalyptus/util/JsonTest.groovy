/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.util

import org.hamcrest.CoreMatchers
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat


/**
 *
 */
class JsonTest {

  @Test
  void testParseWithObject( ) {
    Json.parse( '{}' )
  }

  @Test
  void testParseWithText( ) {
    Json.parse( '"asfd"' )
  }

  @Test
  void testParseObjectWithObject( ) {
    Json.parseObject( '{}' )
  }

  @Test(expected = IOException)
  void testParseObjectWithText( ) {
    Json.parseObject( '"asfd"' )
  }

  @Test
  void testParseAndWriteObject( ) {
    String json = '{"foo":"bar baz"}'
    String written = Json.writeObjectAsString( Json.parseObject( json  ) )
    assertEquals( "Expected output matches input", json, written );
  }

  @Test(expected = IOException)
  void testValidJsonTrailer( ) {
    try {
      Json.parse( '{}"' )
    } catch( Exception e ) {
      assertThat( "Exception message matches", e.getMessage( ), CoreMatchers.startsWith( 'Unexpected trailing content at ' ) )
      throw e
    }
  }

  @Test(expected = IOException)
  void testInvalidJsonTrailer( ) {
    try {
      Json.parse( '{}asdf' )
    } catch( Exception e ) {
      assertThat( "Exception message matches", e.getMessage( ), CoreMatchers.startsWith( 'Unexpected trailing content at ' ) )
      throw e
    }
  }
}
