/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.compute.common.policy

import com.eucalyptus.auth.policy.ern.Ern
import org.junit.BeforeClass
import org.junit.Test

import static com.eucalyptus.auth.policy.PolicySpec.EC2_RESOURCE_SUBNET
import static com.eucalyptus.auth.policy.PolicySpec.VENDOR_EC2
import static com.eucalyptus.auth.policy.PolicySpec.qualifiedName
import static org.junit.Assert.assertEquals

class Ec2ErnTest {
  @BeforeClass
  static void beforeClass( ) {
    Ern.registerServiceErnBuilder( new Ec2ErnBuilder( ) )
  }

  @Test
  void testSubnetArn( ) {
    final Ern ern = Ern.parse( "arn:aws:ec2::332895979617:subnet/subnet-75d75dc3" )
    assertEquals( "Resource type", qualifiedName( VENDOR_EC2, EC2_RESOURCE_SUBNET ), ern.getResourceType( ) );
    assertEquals( "Resource name", "subnet-75d75dc3", ern.getResourceName( ) );
  }

  @Test
  void testParseValid( ) {
    List<String> arns = [
        'arn:aws:ec2:*::image/ami-1a2b3c4d',
        'arn:aws:ec2:*:123456789012:instance/*',
        'arn:aws:ec2:*:123456789012:volume/*',
        'arn:aws:ec2:*:123456789012:volume/vol-1a2b3c4d',
    ]
    for ( String arn : arns ) {
      Ern.parse( arn )
    }
  }
}
