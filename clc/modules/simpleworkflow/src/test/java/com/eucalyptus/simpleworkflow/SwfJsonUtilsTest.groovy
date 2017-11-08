/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
