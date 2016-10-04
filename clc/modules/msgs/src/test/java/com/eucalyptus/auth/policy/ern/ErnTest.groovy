/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth.policy.ern

import org.junit.BeforeClass

import static org.junit.Assert.*
import org.junit.Test
import static com.eucalyptus.auth.policy.PolicySpec.*

/**
 *
 */
class ErnTest {

  @BeforeClass
  static void beforeClass( ) {
    Ern.registerServiceErnBuilder( new EuareErnBuilder( ) )
  }

  @Test
  void testRoleArn( ) {
    final Ern ern = Ern.parse( "arn:aws:iam::013765657871:role/Role1" )
    assertEquals( "Namespace", "013765657871", ern.getAccount() );
    assertEquals( "Resource type", qualifiedName( VENDOR_IAM, IAM_RESOURCE_ROLE ), ern.getResourceType() );
    assertEquals( "Resource name", "/Role1", ern.getResourceName( ) );
  }

  @Test
  void testOidcProviderArn( ) {
    final Ern ern = Ern.parse( 'arn:aws:iam::123456789012:oidc-provider/auth.globus.org/path/goes/here' )
    assertEquals( "Namespace", "123456789012", ern.getAccount() );
    assertEquals( "Resource type", qualifiedName( VENDOR_IAM, IAM_RESOURCE_OPENID_CONNECT_PROVIDER ), ern.getResourceType() );
    assertEquals( "Resource name", "/auth.globus.org/path/goes/here", ern.getResourceName( ) );
  }

  @Test
  void testParseValid( ) {
    List<String> arns = [
      'arn:aws:iam::123456789012:user/Bob',
      'arn:aws:iam::123456789012:user/division_abc/subdivision_xyz/Bob',
      'arn:aws:iam::123456789012:group/Developers',
      'arn:aws:iam::123456789012:group/division_abc/subdivision_xyz/product_A/Developers',
      'arn:aws:iam::123456789012:role/S3Access',
      'arn:aws:iam::123456789012:role/application_abc/component_xyz/S3Access',
      'arn:aws:iam::123456789012:instance-profile/Webserver',
      'arn:aws:iam::123456789012:oidc-provider/auth.globus.org',
      'arn:aws:iam::123456789012:oidc-provider/auth.globus.org/path/goes/here',
      'arn:aws:iam::123456789012:server-certificate/ProdServerCert',
      'arn:aws:iam::123456789012:server-certificate/division_abc/subdivision_xyz/ProdServerCert',
    ]
    for ( String arn : arns ) {
      Ern.parse( arn )
    }
  }
}
