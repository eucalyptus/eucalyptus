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
package com.eucalyptus.cloudwatch

import com.eucalyptus.cloudwatch.common.CloudWatchResourceName
import org.junit.Test
import static org.junit.Assert.*
import static com.eucalyptus.cloudwatch.common.CloudWatchResourceName.*

/**
 * 
 */
class CloudWatchResourceNameTest {

  /**
   * arn:aws:cloudwatch:us-west-1:429942273585:alarm:TestAlarm
   */
  @Test
  void testAlarmArn() {
    assertTrue( "Valid resource name", isResourceName().apply( "arn:aws:cloudwatch:us-west-1:429942273585:alarm:TestAlarm" ) )
    assertFalse( "Short name", isResourceName().apply( "Test" ) )

    CloudWatchResourceName name = parse( "arn:aws:cloudwatch:us-west-1:429942273585:alarm:TestAlarm" )
    assertEquals( "Name", "arn:aws:cloudwatch:us-west-1:429942273585:alarm:TestAlarm", name.resourceName )
    assertEquals( "Account number", "429942273585", name.namespace )
    assertEquals( "Service name", "cloudwatch", name.service )
    assertEquals( "Type name", "alarm", name.type )
    assertEquals( "Short name", "TestAlarm", name.name )

    CloudWatchResourceName name2 = parse( "arn:aws:cloudwatch:us-west-1:429942273585:alarm:arn:aws:cloudwatch:us-west-1:429942273585:alarm:TestAlarm" )
    assertEquals( "Name", "arn:aws:cloudwatch:us-west-1:429942273585:alarm:arn:aws:cloudwatch:us-west-1:429942273585:alarm:TestAlarm", name2.resourceName )
    assertEquals( "Account number", "429942273585", name2.namespace )
    assertEquals( "Service name", "cloudwatch", name2.service )
    assertEquals( "Type name", "alarm", name2.type )
    assertEquals( "Short name", "arn:aws:cloudwatch:us-west-1:429942273585:alarm:TestAlarm", name2.name )
  }
}
