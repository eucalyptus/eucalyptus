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
package com.eucalyptus.cloudformation.resources

import static org.junit.Assert.*
import org.junit.Test

/**
 *
 */
class ResourceActionHelperTest {
  
  @Test
  void testTruncatedName( ) {
    String id = ResourceActionHelper.getDefaultPhysicalResourceId( "nginx-stack-1", "NginxELB", 32 );
    assertEquals( 'Generated identifier length', 32, id.length( ) )
    assertEquals( 'Generated identifier prefix', 'nginx-sta-NginxELB-', id.substring( 0, 19 ) )
  }

  @Test
  void testFullName( ) {
    String id = ResourceActionHelper.getDefaultPhysicalResourceId( "nginx-stack-1", "NginxELB", 1_000_000 );
    assertEquals( 'Generated identifier length', 36, id.length( ) )
    assertEquals( 'Generated identifier prefix', 'nginx-stack-1-NginxELB-', id.substring( 0, 23 ) )
  }

}
