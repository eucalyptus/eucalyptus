/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
