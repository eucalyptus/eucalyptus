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
package com.eucalyptus.autoscaling.common

import static org.junit.Assert.*
import org.junit.Test

import static com.eucalyptus.autoscaling.common.AutoScalingResourceName.*

/**
 * Unit tests for Auto Scaling ARNs 
 */
class AutoScalingResourceNameTest {

  /**
   * arn:aws:autoscaling::013765657871:launchConfiguration:6789b01a-a9c9-489f-9d24-ed39533fca61:launchConfigurationName/Test
   */
  @Test
  public void testLaunchConfigurationArn() {
    assertTrue( "Valid resource name", isResourceName().apply( "arn:aws:autoscaling::013765657871:launchConfiguration:6789b01a-a9c9-489f-9d24-ed39533fca61:launchConfigurationName/Test" ) )
    assertFalse( "Short name", isResourceName().apply( "Test" ) )
    AutoScalingResourceName name = parse( "arn:aws:autoscaling::013765657871:launchConfiguration:6789b01a-a9c9-489f-9d24-ed39533fca61:launchConfigurationName/Test" )
    assertEquals( "Name", "arn:aws:autoscaling::013765657871:launchConfiguration:6789b01a-a9c9-489f-9d24-ed39533fca61:launchConfigurationName/Test", name.resourceName )
    assertEquals( "Account number", "013765657871", name.namespace )
    assertEquals( "Service name", "autoscaling", name.service )
    assertEquals( "Type name", "launchConfiguration", name.type )
    assertEquals( "Uuid", "6789b01a-a9c9-489f-9d24-ed39533fca61", name.uuid )
    assertEquals( "Short name", "Test", name.getName( AutoScalingResourceName.Type.launchConfiguration ) )
  }

  /**
   * arn:aws:autoscaling::013765657871:autoScalingGroup:ee6258d4-a9a5-46e0-9896-1a7b3294c12e:autoScalingGroupName/Test
   */
  @Test
  public void testAutoScalingGroupArn() {
    assertTrue( "Valid resource name", isResourceName().apply( "arn:aws:autoscaling::013765657871:autoScalingGroup:ee6258d4-a9a5-46e0-9896-1a7b3294c12e:autoScalingGroupName/Test" ) )
    assertFalse( "Short name", isResourceName().apply( "Test" ) )
    AutoScalingResourceName name = parse( "arn:aws:autoscaling::013765657871:autoScalingGroup:ee6258d4-a9a5-46e0-9896-1a7b3294c12e:autoScalingGroupName/Test" )
    assertEquals( "Name", "arn:aws:autoscaling::013765657871:autoScalingGroup:ee6258d4-a9a5-46e0-9896-1a7b3294c12e:autoScalingGroupName/Test", name.resourceName )
    assertEquals( "Account number", "013765657871", name.namespace )
    assertEquals( "Service name", "autoscaling", name.service )
    assertEquals( "Type name", "autoScalingGroup", name.type )
    assertEquals( "Uuid", "ee6258d4-a9a5-46e0-9896-1a7b3294c12e", name.uuid )
    assertEquals( "Short name", "Test", name.getName( AutoScalingResourceName.Type.autoScalingGroup ) )
  }

  /**
   * arn:aws:autoscaling::013765657871:scalingPolicy:2886a285-6fdf-4018-8305-7aa037ed0d38:autoScalingGroupName/Test:policyName/TestUp
   */
  @Test
  public void testScalingPolicyArn() {
    assertTrue( "Valid resource name", isResourceName().apply( "arn:aws:autoscaling::013765657871:scalingPolicy:2886a285-6fdf-4018-8305-7aa037ed0d38:autoScalingGroupName/Test:policyName/TestUp" ) )
    assertFalse( "Short name", isResourceName().apply( "Test" ) )
    AutoScalingResourceName name = parse( "arn:aws:autoscaling::013765657871:scalingPolicy:2886a285-6fdf-4018-8305-7aa037ed0d38:autoScalingGroupName/Test:policyName/TestUp" )
    assertEquals( "Name", "arn:aws:autoscaling::013765657871:scalingPolicy:2886a285-6fdf-4018-8305-7aa037ed0d38:autoScalingGroupName/Test:policyName/TestUp", name.resourceName )
    assertEquals( "Account number", "013765657871", name.namespace )
    assertEquals( "Service name", "autoscaling", name.service )
    assertEquals( "Type name", "scalingPolicy", name.type )
    assertEquals( "Uuid", "2886a285-6fdf-4018-8305-7aa037ed0d38", name.uuid )
    assertEquals( "Scope name", "Test", name.getScope( AutoScalingResourceName.Type.scalingPolicy ) )
    assertEquals( "Short name", "TestUp", name.getName( AutoScalingResourceName.Type.scalingPolicy ) )
  }
}
