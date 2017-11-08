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
