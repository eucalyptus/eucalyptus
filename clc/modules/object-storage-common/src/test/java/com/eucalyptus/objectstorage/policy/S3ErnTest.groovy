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
