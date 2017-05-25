/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General License for more details.
 *
 * You should have received a copy of the GNU General License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.cluster.common

import org.jibx.binding.classes.BoundClass
import org.jibx.binding.classes.ClassCache
import org.jibx.binding.classes.ClassFile
import org.jibx.binding.classes.MungedClass
import org.jibx.binding.model.BindingElement
import org.jibx.binding.model.ValidationContext
import org.jibx.util.ClasspathUrlExtender
import org.junit.BeforeClass
import org.junit.Test

import static org.junit.Assert.*


class ClusterBindingTest {

  @BeforeClass
  static void setup() {
    String[ ] paths = (((URLClassLoader)ClusterBindingTest.class.getClassLoader( ) )
        .getURLs( )*.toString( )).grep(~/^.*\//).toArray( new String[0] )
    ClassFile.setPaths( paths )
    ClassCache.setPaths( paths )
    ClasspathUrlExtender.setClassLoader( ClassFile.getClassLoader( ) )
    BoundClass.reset( );
    MungedClass.reset( );
    org.jibx.binding.def.BindingDefinition.reset( );
  }

  void assertValidBindingXml( URL resource, int expectedWarnings=0 ) {
    ValidationContext vctx = BindingElement.newValidationContext( )
    BindingElement root =
        BindingElement.validateBinding( resource.getFile( ), resource, resource.openStream( ), vctx )

    assertNotNull( "Valid binding", root )
    assertEquals( "Fatal error count", 0, vctx.getFatalCount( ) )
    assertEquals( "Error count", 0, vctx.getErrorCount( ) )
    assertEquals( "Warning count", expectedWarnings, vctx.getWarningCount( ) )
  }

  @Test
  void testClusterBinding( ) {
    URL resource = ClusterBindingTest.getResource('/cluster-binding.xml')
    assertValidBindingXml( resource )
  }
}
