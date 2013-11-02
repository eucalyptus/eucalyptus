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
package com.eucalyptus.cloudwatch

import org.junit.Test
import static org.junit.Assert.*
import static com.eucalyptus.cloudwatch.CloudWatchResourceName.*

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
