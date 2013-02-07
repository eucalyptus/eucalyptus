package com.eucalyptus.cloudwatch

import org.junit.BeforeClass
import org.jibx.binding.classes.ClassFile
import org.jibx.binding.classes.ClassCache
import org.jibx.util.ClasspathUrlExtender
import org.jibx.binding.classes.BoundClass
import org.jibx.binding.classes.MungedClass
import org.jibx.binding.def.BindingDefinition
import org.junit.Test
import org.jibx.binding.model.ValidationContext
import org.jibx.binding.model.BindingElement

import static org.junit.Assert.assertNotNull

import static org.junit.Assert.assertEquals

/**
 * 
 */
class CloudWatchBindingTest {

  @BeforeClass
  static void setup() {
    String[] paths = (((URLClassLoader)CloudWatchBindingTest.class.getClassLoader() ).getURLs()*.toString()).grep(~/^.*\//).toArray(new String[0])
    ClassFile.setPaths( paths )
    ClassCache.setPaths( paths )
    ClasspathUrlExtender.setClassLoader( ClassFile.getClassLoader() )
    BoundClass.reset();
    MungedClass.reset();
    BindingDefinition.reset();
  }

  @Test
  void testValidBinding() {
    URL resource = CloudWatchBindingTest.class.getResource( '/cloudwatch-binding.xml' )
    ValidationContext vctx = BindingElement.newValidationContext()
    BindingElement root = BindingElement.validateBinding( 'cloudwatch-binding.xml', resource, resource.openStream(), vctx)

    assertNotNull( "Valid binding", root )
    assertEquals( "Fatal error count", 0, vctx.getFatalCount() )
    assertEquals( "Error count", 0, vctx.getErrorCount() )
    assertEquals( "Warning count", 0, vctx.getWarningCount() )
  }  
}
