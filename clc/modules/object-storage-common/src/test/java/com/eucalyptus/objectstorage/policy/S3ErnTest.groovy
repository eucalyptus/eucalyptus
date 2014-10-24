package com.eucalyptus.objectstorage.policy

import com.eucalyptus.auth.policy.ern.Ern
import org.junit.BeforeClass
import org.junit.Test

import static com.eucalyptus.auth.policy.PolicySpec.S3_RESOURCE_OBJECT
import static com.eucalyptus.auth.policy.PolicySpec.VENDOR_S3
import static com.eucalyptus.auth.policy.PolicySpec.qualifiedName
import static org.junit.Assert.assertEquals

/**
 *
 */
class S3ErnTest {

  @BeforeClass
  static void beforeClass( ) {
    Ern.registerServiceErnBuilder( new S3ErnBuilder( ) )
  }

  @Test
  void testObjectArn( ) {
    final Ern ern = Ern.parse( "arn:aws:s3:::my_corporate_bucket/*" )
    assertEquals( "Resource type", qualifiedName( VENDOR_S3, S3_RESOURCE_OBJECT ), ern.getResourceType( ) );
    assertEquals( "Resource name", "my_corporate_bucket/*", ern.getResourceName( ) );
  }

  @Test
  void testParseValid( ) {
    List<String> arns = [
        'arn:aws:s3:::my_corporate_bucket',
        'arn:aws:s3:::my_corporate_bucket/*',
        'arn:aws:s3:::my_corporate_bucket/Development/*',
    ]
    for ( String arn : arns ) {
      Ern.parse( arn )
    }
  }
}
