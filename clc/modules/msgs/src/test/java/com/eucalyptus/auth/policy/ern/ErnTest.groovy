package com.eucalyptus.auth.policy.ern

import static org.junit.Assert.*
import org.junit.Test
import static com.eucalyptus.auth.policy.PolicySpec.*

/**
 * 
 */
class ErnTest {

  @Test
  void testRoleArn(  ) {
    final Ern ern = Ern.parse( "arn:aws:iam::013765657871:role/Role1" )
    assertEquals( "Namespace", "013765657871", ern.getNamespace() );
    assertEquals( "Resource type", qualifiedName( VENDOR_IAM, IAM_RESOURCE_ROLE ), ern.getResourceType() );
    assertEquals( "Resource name", "/Role1", ern.getResourceName() );
  }
}
