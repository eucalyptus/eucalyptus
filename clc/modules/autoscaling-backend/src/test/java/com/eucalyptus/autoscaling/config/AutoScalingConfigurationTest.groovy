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
package com.eucalyptus.autoscaling.config

import static org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * 
 */
class AutoScalingConfigurationTest {

  @Test
  void testInstantiable( ) {
    new AutoScalingConfiguration()
  }

  @Test
  void testExpectedValues( ) {
    assertEquals( "Activity expiry millis", TimeUnit.DAYS.toMillis( 42 ), AutoScalingConfiguration.getActivityExpiryMillis() )
    assertEquals( "Activity timeout millis", TimeUnit.MINUTES.toMillis( 5 ), AutoScalingConfiguration.getActivityTimeoutMillis() )
    assertEquals( "Max launch increment", 20, AutoScalingConfiguration.getMaxLaunchIncrement() )
    assertEquals( "Max registration retries", 5, AutoScalingConfiguration.getMaxRegistrationRetries() )
    assertEquals( "Suspended processes", Collections.emptySet(), AutoScalingConfiguration.getSuspendedProcesses() )
    assertEquals( "Suspended tasks", Collections.emptySet(), AutoScalingConfiguration.getSuspendedTasks() )
    assertEquals( "Suspension launch attempts", 15, AutoScalingConfiguration.getSuspensionLaunchAttemptsThreshold() )
    assertEquals( "Suspension timeout", TimeUnit.DAYS.toMillis( 1 ),AutoScalingConfiguration.getSuspensionTimeoutMillis() )
    assertEquals( "Zone failure timeout", TimeUnit.MINUTES.toMillis( 5 ), AutoScalingConfiguration.getZoneFailureThresholdMillis() )
  }
}
