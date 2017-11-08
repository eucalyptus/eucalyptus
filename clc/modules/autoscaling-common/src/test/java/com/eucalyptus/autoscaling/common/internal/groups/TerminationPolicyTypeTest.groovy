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
package com.eucalyptus.autoscaling.common.internal.groups

import static org.junit.Assert.*
import org.junit.Test
import com.eucalyptus.crypto.util.Timestamps
import com.eucalyptus.autoscaling.common.internal.instances.AutoScalingInstance

import static com.eucalyptus.autoscaling.common.internal.groups.TerminationPolicyType.*
import com.eucalyptus.autoscaling.common.internal.instances.AutoScalingInstanceCoreView

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