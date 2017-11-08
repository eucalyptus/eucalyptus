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

package com.eucalyptus.objectstorage.policy

import com.eucalyptus.auth.policy.ern.Ern
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import org.junit.BeforeClass
import org.junit.Test

import static com.eucalyptus.objectstorage.policy.S3PolicySpec.S3_RESOURCE_BUCKET
import static com.eucalyptus.objectstorage.policy.S3PolicySpec.S3_RESOURCE_OBJECT
import static com.eucalyptus.objectstorage.policy.S3PolicySpec.VENDOR_S3
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
  void testWildcardResourceArn( ) {
    final Ern ern = Ern.parse( "arn:aws:s3:::*" )
    assertEquals( "Resource type", qualifiedName( VENDOR_S3, S3_RESOURCE_BUCKET ), ern.getResourceType( ) );
    assertEquals( "Resource name", "*", ern.getResourceName( ) );
    final Collection<Ern> erns = Lists.newArrayList( ern.explode( ) )
    erns.remove( ern )
    assertEquals( "Erns size", 1, erns.size( ) )
    assertEquals( "Resource type", qualifiedName( VENDOR_S3, S3_RESOURCE_OBJECT ), Iterables.getOnlyElement(erns).getResourceType( ) );
    assertEquals( "Resource name", "*/*", Iterables.getOnlyElement(erns).getResourceName( ) );
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
