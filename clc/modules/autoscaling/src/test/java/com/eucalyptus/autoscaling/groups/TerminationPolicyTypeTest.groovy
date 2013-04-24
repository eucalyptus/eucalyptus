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
package com.eucalyptus.autoscaling.groups

import static org.junit.Assert.*
import org.junit.Test
import com.eucalyptus.crypto.util.Timestamps
import com.eucalyptus.autoscaling.instances.AutoScalingInstance

import static com.eucalyptus.autoscaling.groups.TerminationPolicyType.*
import com.eucalyptus.autoscaling.instances.AutoScalingInstanceCoreView

/**
 * 
 */
class TerminationPolicyTypeTest {
  
  @Test
  void testOldestInstancePolicy() {
    assertEquals( "No instance", [], OldestInstance.selectForTermination( [] ) )  
    assertEquals( "Single instance", [ "i-00000002" ], ids(OldestInstance.selectForTermination( [
        instance( "i-00000001", "2010-01-01T00:00:01.000Z", "lc1" ),
        instance( "i-00000002", "2010-01-01T00:00:00.000Z", "lc2" ),
        instance( "i-00000003", "2010-01-01T00:00:02.000Z", "lc3" ),
    ] )) )
    assertEquals( "Multiple instances", [ "i-00000001", "i-00000002" ], ids(OldestInstance.selectForTermination( [
        instance( "i-00000001", "2010-01-01T00:00:00.000Z", "lc1"  ),
        instance( "i-00000002", "2010-01-01T00:00:00.000Z", "lc2"  ),
        instance( "i-00000003", "2010-01-01T00:00:02.000Z", "lc1"  ),
    ] )) )
  }
  
  @Test
  void testOldestLaunchConfigurationPolicy() {    
    assertEquals( "No instance", [], OldestLaunchConfiguration.selectForTermination( [] ) )
    assertEquals( "Single instance", [ "i-00000002" ], ids(OldestLaunchConfiguration.selectForTermination( [
        instance( "i-00000001", "2010-01-01T00:00:01.000Z", "lc2"  ),
        instance( "i-00000002", "2010-01-01T00:00:00.000Z", "lc1"  ),
        instance( "i-00000003", "2010-01-01T00:00:02.000Z", "lc2"  ),
    ] )) )
    assertEquals( "Multiple instances", [ "i-00000001", "i-00000002" ], ids(OldestLaunchConfiguration.selectForTermination( [
        instance( "i-00000001", "2010-01-01T00:00:00.000Z", "lc1" ),
        instance( "i-00000002", "2010-01-01T00:00:01.000Z", "lc1" ),
        instance( "i-00000003", "2010-01-01T00:00:02.000Z", "lc2" ),
    ] )) )
    assertEquals( "Multiple instances (one config)", [ "i-00000001", "i-00000002", "i-00000003" ], ids(OldestLaunchConfiguration.selectForTermination( [
        instance( "i-00000001", "2010-01-01T00:00:00.000Z", "lc1" ),
        instance( "i-00000002", "2010-01-01T00:00:01.000Z", "lc1" ),
        instance( "i-00000003", "2010-01-01T00:00:02.000Z", "lc1" ),
    ] )) )
  }

  @Test
  void testNewestInstancePolicy() {
    assertEquals( "No instance", [], NewestInstance.selectForTermination( [] ) )
    assertEquals( "Single instance", [ "i-00000003" ], ids(NewestInstance.selectForTermination( [
        instance( "i-00000001", "2010-01-01T00:00:01.000Z", "lc2"  ),
        instance( "i-00000002", "2010-01-01T00:00:00.000Z", "lc1"  ),
        instance( "i-00000003", "2010-01-01T00:00:02.000Z", "lc2"  ),
    ] )) )
    assertEquals( "Multiple instances", [ "i-00000002", "i-00000003" ], ids(NewestInstance.selectForTermination( [
        instance( "i-00000001", "2010-01-01T00:00:00.000Z", "lc1" ),
        instance( "i-00000002", "2010-01-01T00:00:01.000Z", "lc1" ),
        instance( "i-00000003", "2010-01-01T00:00:01.000Z", "lc2" ),
    ] )) )
  }

  @Test
  void testClosestToNextInstanceHourPolicy() {
    assertEquals( "No instance", [], ClosestToNextInstanceHour.selectForTermination( [] ) )
    assertEquals( "Single instance", [ "i-00000003" ], ids(ClosestToNextInstanceHour.selectForTermination( [
        instance( "i-00000001", "2010-01-01T00:00:01.000Z", "lc2"  ),
        instance( "i-00000002", "2010-01-01T00:00:00.000Z", "lc1"  ),
        instance( "i-00000003", "2010-01-01T00:00:02.000Z", "lc2"  ),
    ] )) )
    assertEquals( "Multiple instances", [ "i-00000002", "i-00000003" ], ids(ClosestToNextInstanceHour.selectForTermination( [
        instance( "i-00000001", "2010-01-01T00:00:00.000Z", "lc1" ),
        instance( "i-00000002", "2010-01-01T01:00:01.000Z", "lc1" ),
        instance( "i-00000003", "2010-01-01T00:00:01.000Z", "lc2" ),
    ] )) )
  }

  @Test
  void testDefaultPolicy() {
    assertEquals( "No instance", [], Default.selectForTermination( [] ) )
    assertEquals( "Single instance - oldest lc", [ "i-00000002" ], ids(Default.selectForTermination( [
        instance( "i-00000001", "2010-01-01T00:00:01.000Z", "lc2"  ),
        instance( "i-00000002", "2010-01-01T00:00:00.000Z", "lc1"  ),
        instance( "i-00000003", "2010-01-01T00:00:02.000Z", "lc2"  ),
    ] )) )
    assertEquals( "Single instance - oldest lc + closest to instance hour", [ "i-00000002" ], ids(Default.selectForTermination( [
        instance( "i-00000001", "2010-01-01T00:00:01.000Z", "lc1"  ),
        instance( "i-00000002", "2010-01-01T00:59:59.999Z", "lc1"  ),
        instance( "i-00000003", "2010-01-01T00:00:02.000Z", "lc2"  ),
    ] )) )
    assertEquals( "Single random instance", 1, Default.selectForTermination( [
        instance( "i-00000001", "2010-01-01T00:00:00.000Z", "lc1"  ),
        instance( "i-00000002", "2010-01-01T00:00:00.000Z", "lc1"  ),
        instance( "i-00000003", "2010-01-01T00:00:00.000Z", "lc1"  ),
    ] ).size() )
  }
  
  @Test
  void testTerminationPolicyCombinations() {
    assertEquals( "No policy",  "i-00000001", selectForTermination( [], [
        instance( "i-00000001", "2010-01-01T00:00:01.000Z", "lc2"  ),
    ] ).instanceId )
    assertEquals( "Instance hour + oldest",  "i-00000002", selectForTermination( [
        ClosestToNextInstanceHour, OldestInstance 
    ], [
        instance( "i-00000001", "2010-01-01T01:00:05.000Z", "lc2"  ),
        instance( "i-00000002", "2010-01-01T00:00:05.000Z", "lc2"  ),
        instance( "i-00000003", "2010-01-01T00:00:01.000Z", "lc2"  ),
    ] ).instanceId )
    assertEquals( "Instance hour + oldest config + newest",  "i-00000004", selectForTermination( [
        ClosestToNextInstanceHour, OldestLaunchConfiguration, NewestInstance
    ], [
        instance( "i-00000001", "2010-01-01T04:00:05.000Z", "lc2"  ),
        instance( "i-00000002", "2010-01-01T00:00:05.000Z", "lc1"  ),
        instance( "i-00000003", "2010-01-01T00:00:01.000Z", "lc1"  ),
        instance( "i-00000004", "2010-01-01T02:00:05.000Z", "lc1"  ),
        instance( "i-00000005", "2010-01-01T00:00:01.000Z", "lc1"  ),
    ] ).instanceId )
  }

  List<String> ids( List<AutoScalingInstanceCoreView> instances ) {
    instances.collect{ AutoScalingInstanceCoreView instance -> instance.getInstanceId() }
  }
  
  Date date( String dateText ) {
    Timestamps.parseIso8601Timestamp( dateText )
  }

  @SuppressWarnings("GroovyAccessibility")
  AutoScalingInstanceCoreView instance( String id,
                                        String dateText,
                                        String launchConfigurationName ) {
    new AutoScalingInstanceCoreView( new AutoScalingInstance(
        displayName: id, 
        creationTimestamp: date( dateText ),
        launchConfigurationName: launchConfigurationName
    ) );
  }
}