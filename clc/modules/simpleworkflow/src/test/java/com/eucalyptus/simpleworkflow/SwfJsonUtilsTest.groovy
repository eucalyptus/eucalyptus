/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.simpleworkflow

import com.amazonaws.AmazonServiceException
import com.amazonaws.transform.JsonErrorUnmarshaller
import com.eucalyptus.simpleworkflow.common.model.CountClosedWorkflowExecutionsRequest
import com.google.common.collect.ImmutableMap
import groovy.transform.CompileStatic
import org.junit.Test
import static org.junit.Assert.*

/**
 *
 */
@CompileStatic
class SwfJsonUtilsTest {

  @Test
  public void testDateBinding( ) {
    CountClosedWorkflowExecutionsRequest message = SwfJsonUtils.readObject(
        """{
          "closeTimeFilter": {
              "oldestDate": 1408146022,
              "latestDate": "1408146022"
          },
          "startTimeFilter": {
              "oldestDate": 1408146022.999,
              "latestDate": "1408146022.999"
          },
          "domain": "test"
        }""",
        CountClosedWorkflowExecutionsRequest )
    assertEquals( "oldest date", 1408146022000, message.closeTimeFilter.oldestDate.time )
    assertEquals( "latest date", 1408146022000, message.closeTimeFilter.latestDate.time )
    assertEquals( "oldest date", 1408146022999, message.startTimeFilter.oldestDate.time )
    assertEquals( "latest date", 1408146022999, message.startTimeFilter.latestDate.time )
  }

  @Test
  public void testErrorMapBinding( ) {
    String value = SwfJsonUtils.writeObjectAsString ImmutableMap.of( '__type', 'Foo', 'message', 'spoon' )
    println value
    assertEquals( 'error message format', '{"__type":"Foo","message":"spoon"}', value )

    /*AmazonServiceException exception = new JsonErrorUnmarshaller( ).unmarshall( new JSONObject( value ) )
    assertEquals( 'Unmarshalled error code', 'Foo', exception.errorCode )
    assertTrue( 'Unmarshalled error message', exception.message.contains( 'spoon' ) )
    */
  }
}
