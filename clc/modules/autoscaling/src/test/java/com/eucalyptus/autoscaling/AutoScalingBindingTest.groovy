package com.eucalyptus.autoscaling

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
class AutoScalingBindingTest {

  @BeforeClass
  static void setup() {
    String[] paths = (((URLClassLoader)AutoScalingBindingTest.class.getClassLoader() ).getURLs()*.toString()).grep(~/^.*\//).toArray(new String[0])
    ClassFile.setPaths( paths )
    ClassCache.setPaths( paths )
    ClasspathUrlExtender.setClassLoader( ClassFile.getClassLoader() )
    BoundClass.reset();
    MungedClass.reset();
    BindingDefinition.reset();
  }

  @Test
  void testValidBinding() {
    URL resource = AutoScalingBindingTest.class.getResource( '/autoscaling-binding.xml' )
    ValidationContext vctx = BindingElement.newValidationContext()
    BindingElement root = BindingElement.validateBinding( 'autoscaling-binding.xml', resource, resource.openStream(), vctx)

    assertNotNull( "Valid binding", root )
    assertEquals( "Fatal error count", 0, vctx.getFatalCount() )
    assertEquals( "Error count", 0, vctx.getErrorCount() )
    assertEquals( "Warning count", 0, vctx.getWarningCount() )
  }  
}
