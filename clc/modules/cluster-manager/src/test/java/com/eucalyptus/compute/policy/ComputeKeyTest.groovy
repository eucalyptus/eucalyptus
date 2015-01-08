/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.compute.policy

import static org.junit.Assert.*
import org.junit.Test
import net.sf.json.JSONException

class ComputeKeyTest {

  @Test
  void testArnBasicValidation( ) {
    assertValidArn "arn:aws:ec2:eucalyptus:726724822767:image/emi-239D37F2"
    assertValidArn "arn:aws:ec2"
    assertValidArn "arn:aws:ec2:eucalyptus:726724822767:image/emi-239D37F2"
    assertValidArn "arn:aws:ec2:eucalyptus:726724822767:image/emi-239D37F2"
    assertValidArn "arn:aws:ec2::726724822767:image/emi-239D37F2"
    assertValidArn "arn:aws:ec2:::image/emi-239D37F2"
    assertValidArn "arn:aws:iam::123456789012:instance-profile/application_abc/component_xyz/Database"
    assertValidArn "arn:aws:s3:::bucket/object"
    assertValidArn "arn:aws:sts::123456789012:federated-user/Bob"
    assertValidArn "arn:aws:sns:us-east-1:123456789012:mytopic"
    assertValidArn "arn:aws:sns:us-east-1:123456789012:MyTopic:c7fe3a54-ab0e-4ec2-88e0-db410a0f2bee"
    assertValidArn "arn:aws:autoscaling:us-east-1:803981987763:scalingPolicy:b0dcf5e8-02e6-4e31-9719-0675d0dc31ae:autoScalingGroupName/my-test-asg:policyName/my-scaleout-policy"
  }

  @Test
  void testArnBasicRejection( ) {
    assertInvalidArn "arn"
    assertInvalidArn "arn:aws"
    assertInvalidArn "arn:aws:ec2:euc%alyptus:726724822767:image/emi-239D37F2"
    assertInvalidArn "arn:aws:ec2:eucalyptus:726724@822767:image/emi-239D37F2"
    assertInvalidArn ":aws:ec2:eucalyptus:726724822767:image/emi-239D37F2"
    assertInvalidArn "arn::ec2:eucalyptus:726724822767:image/emi-239D37F2"
    assertInvalidArn "arn:aws::eucalyptus:726724822767:image/emi-239D37F2"
    assertInvalidArn "arn:aws:ec2:::"
  }

  @Test
  void testWildcardValidation( ) {
    assertValidArn "arn:aws:ec2:eucalyptus:726724822767:image/*"
    assertValidArn "arn:aws:ec2:eucalyptus:726724822767:security-group/*"
    assertValidArn "arn:aws:**:eucalyptus:726724822767:image/emi-239D37F2"
    assertValidArn "arn:aws:ec2:*:726724822767:image/emi-239D37F2"
    assertValidArn "arn:aws:ec2:eucalyptus:*:image/emi-239D37F2"
    assertValidArn "arn:aws:ec2:eucalyptus:726724822767:*"
    assertValidArn "*:*:*:*:*:*"
    assertValidArn "arn:aws:???:eucalyptus:726724822767:image/emi-239D37F2"
    assertValidArn "arn:aws:ec2:???:726724822767:image/emi-239D37F2"
    assertValidArn "arn:aws:ec2:eucalyptus:??:image/emi-239D37F2"
    assertValidArn "arn:aws:ec2:eucalyptus:726724822767:?"
    assertValidArn "?:?:?:?:?:?"
    assertValidArn "*?:*?:*?:*?:*?:*?"
  }

  private void assertValidArn( String arn ) {
    assertTrue( "Arn invalid: " + arn, isValidArn( arn ) )
  }

  private void assertInvalidArn( String arn ) {
    assertFalse( "Arn valid: " + arn, isValidArn( arn ) )
  }

  private boolean isValidArn( String arn ) {
    boolean valid = true
    try {
      ComputeKey.Validation.assertArnValue( arn )
    } catch ( JSONException e ) {
      valid = false
    }
    valid
  }

}
