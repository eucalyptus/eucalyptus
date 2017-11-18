/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.ws.protocol

import edu.ucsb.eucalyptus.msgs.BaseMessages

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

  void assertValidInternalRoundTrip( URL resource, List<String> ignoreMessageClasses=[] ) {
    List<Class> messageClasses = loadMessageClassesFromBindingXml( resource )
    assertFalse( "No classes found for binding", messageClasses.isEmpty() )

    messageClasses.each { Class clazz ->
      if ( !ignoreMessageClasses.contains(clazz.getName()) ) {
        BaseMessage original = (BaseMessage) clazz.newInstance( )
        BaseMessage omTrippy = (BaseMessage) BaseMessages.fromOm( BaseMessages.toOm( original ), clazz )
        assertEquals( 'Round trip lost something', original.toString( ), omTrippy.toString( )
        )
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

  List<Class> loadMessageClassesFromBindingXml( URL resource ) {
    Node binding = new XmlParser().parse( resource.toString() )
    binding.'mapping'.'@class'
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
