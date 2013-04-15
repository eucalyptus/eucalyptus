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
