/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.ws.protocol

import static org.junit.Assert.*
import org.junit.BeforeClass
import org.jibx.binding.classes.ClassFile
import org.jibx.binding.classes.ClassCache
import org.jibx.util.ClasspathUrlExtender
import org.jibx.binding.classes.BoundClass
import org.jibx.binding.classes.MungedClass
import org.jibx.binding.def.BindingDefinition
import org.jibx.binding.model.ValidationContext
import org.jibx.binding.model.BindingElement
import edu.ucsb.eucalyptus.msgs.BaseMessage
import com.eucalyptus.binding.Binding

/**
 *
 */
class QueryBindingTestSupport extends QueryRequestBindingTestSupport {

  @BeforeClass
  static void setup() {
    String[] paths = (((URLClassLoader)QueryBindingTestSupport.class.getClassLoader() )
        .getURLs()*.toString()).grep(~/^.*\//).toArray(new String[0])
    ClassFile.setPaths( paths )
    ClassCache.setPaths( paths )
    ClasspathUrlExtender.setClassLoader( ClassFile.getClassLoader() )
    BoundClass.reset();
    MungedClass.reset();
    BindingDefinition.reset();
  }

  void assertValidBindingXml( URL resource, int expectedWarnings=0 ) {
    ValidationContext vctx = BindingElement.newValidationContext()
    BindingElement root =
      BindingElement.validateBinding( resource.getFile(), resource, resource.openStream(), vctx )

    assertNotNull( "Valid binding", root )
    assertEquals( "Fatal error count", 0, vctx.getFatalCount() )
    assertEquals( "Error count", 0, vctx.getErrorCount() )
    assertEquals( "Warning count", expectedWarnings, vctx.getWarningCount() )
  }

  void assertValidQueryBinding( URL resource, List<String> ignoreMessageClasses=[] ) {
    List<Class> requestMessageClasses = loadRequestMessageClassesFromBindingXml( resource )
    assertFalse( "No classes found for binding", requestMessageClasses.isEmpty() )

    requestMessageClasses.each { Class clazz ->
      if ( !ignoreMessageClasses.contains(clazz.getName()) ) {
        assertAnnotationsRecursively( clazz )
      }
    }
  }

  List<Class> loadRequestMessageClassesFromBindingXml( URL resource ) {
    Node binding = new XmlParser().parse( resource.toString() )
    binding.'mapping'.'@class'
        .findAll { String className -> !className.endsWith( "ResponseType" ) && !className.endsWith( "Response" ) }
        .collect { String className -> Class.forName( className ) }
        .findAll { Class clazz -> BaseMessage.class.isAssignableFrom( clazz ) }
  }

  Binding createTestBindingFromXml( URL resource, String elementClass ) {
    List<Class> requestMessageClasses = loadRequestMessageClassesFromBindingXml( resource )

    assertFalse( "No classes found for binding", requestMessageClasses.isEmpty() )

    requestMessageClasses.find { Class clazz -> clazz.getSimpleName().equals(elementClass) } != null ?
      new TestBinding( requestMessageClasses  ) :
      null
  }
}
