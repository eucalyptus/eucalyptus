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
package com.eucalyptus.vm

import com.google.common.base.Optional

import static org.junit.Assert.*
import org.junit.Test

/**
 * 
 */
class MetadataRequestTest {

  @Test
  void testPathCanonicalization( ) {
    assertPath( "a", "b", "a/b" )
    assertPath( "a", "b/c/d", "a/b/c/d" )
    assertPath( "a", "b/c/d/", "a/b/c/d/" )
    assertPath( "a", "b/c/d/", "a//b/c/d/" )
    assertPath( "a", "b/c/d/", "a/b//c/d/" )
    assertPath( "a", "b/c/d/", "a/b/c/d////" )
  }

  private void assertPath( String expectedMetadata, String expectedPath, String input ) {
    MetadataRequest request = new MetadataRequest( "127.0.0.1", input, Optional.absent( ) );

    assertEquals( expectedMetadata, request.getMetadataName() )
    assertEquals( expectedPath, request.getLocalPath() )
  }
}
