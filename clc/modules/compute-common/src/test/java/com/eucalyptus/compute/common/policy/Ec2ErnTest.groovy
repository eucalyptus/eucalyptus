package com.eucalyptus.compute.common.policy

import com.eucalyptus.auth.policy.ern.Ern
import org.junit.BeforeClass
import org.junit.Test

import static com.eucalyptus.auth.policy.PolicySpec.EC2_RESOURCE_SUBNET
import static com.eucalyptus.auth.policy.PolicySpec.VENDOR_EC2
import static com.eucalyptus.auth.policy.PolicySpec.qualifiedName
import static org.junit.Assert.assertEquals

/**
 *
 */
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
        'arn:aws:ec2:us-east-1::image/ami-1a2b3c4d',
        'arn:aws:ec2:us-east-1:123456789012:instance/*',
        'arn:aws:ec2:us-east-1:123456789012:volume/*',
        'arn:aws:ec2:us-east-1:123456789012:volume/vol-1a2b3c4d',
    ]
    for ( String arn : arns ) {
      Ern.parse( arn )
    }
  }
}
