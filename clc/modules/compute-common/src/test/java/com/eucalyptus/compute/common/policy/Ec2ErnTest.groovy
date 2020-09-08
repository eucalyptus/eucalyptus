/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.compute.common.policy

import com.eucalyptus.auth.policy.ern.Ern
import org.junit.BeforeClass
import org.junit.Test

import static com.eucalyptus.compute.common.policy.ComputePolicySpec.EC2_RESOURCE_SUBNET
import static com.eucalyptus.compute.common.policy.ComputePolicySpec.VENDOR_EC2
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
