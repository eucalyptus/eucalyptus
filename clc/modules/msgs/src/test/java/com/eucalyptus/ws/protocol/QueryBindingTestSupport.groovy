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
import java.lang.reflect.Field
import com.eucalyptus.system.Ats
import com.eucalyptus.binding.HttpEmbedded
import com.eucalyptus.binding.Binding
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import com.google.common.collect.Maps
import com.eucalyptus.http.MappingHttpRequest
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.handler.codec.http.HttpMethod
import com.eucalyptus.binding.HttpParameterMapping
import java.text.SimpleDateFormat
import com.eucalyptus.binding.HttpEmbeddeds
import com.eucalyptus.binding.HttpParameterMappings

/**
 * 
 */
class QueryBindingTestSupport {

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

  void assertAnnotationsRecursively( Class target ) {
    target.getDeclaredFields().findAll{ Field field -> !field.getName().startsWith('_') && !field.getName().startsWith('$') && !field.getName().equals("metaClass") }.each{ Field field ->
      Class classToAssert = ArrayList.equals( field.getType() ) ?
        field.getGenericType( ).getActualTypeArguments( )[0] :
        field.getType()
      if ( !isSimpleType( classToAssert ) ) {
        assertTrue( "Only simple types or extensions of EucalyptusData / EucalyptusMessage are supported: " + target.getName() + "." + field.getName(), 
            EucalyptusData.class.isAssignableFrom( classToAssert) ||
            EucalyptusMessage.class.isAssignableFrom( classToAssert ))
        assertAnnotationsRecursively( classToAssert )
      } else {
        assertFalse( "Field does not need HttpEmbedded annotation" + target.getName() + "." + field.getName(),
            Ats.from( field ).has( HttpEmbedded.class ) || Ats.from( field ).has( HttpEmbeddeds.class ) )
      }
    }
  }

  boolean isSimpleType( final Class clazz ) {
    ( clazz != null && (
        String.class.equals( clazz ) ||
        Boolean.class.equals( clazz ) ||
        Integer.class.equals( clazz ) ||
        Integer.TYPE.equals( clazz ) ||
        Long.class.equals( clazz ) ||
        Double.class.equals( clazz ) ||
        Date.class.equals( clazz ) ||
        clazz.isEnum() ) )
  }

  def <T> T bindAndAssertParameters( BaseQueryBinding binding, Class<T> messageClass, String action, Object bean, Map<String,String> parameters ) {
    Object message = bind( binding, action, parameters )
    assertTrue( action + ' message type', messageClass.isInstance( message ) )
    assertRecursiveEquality( action, "", bean, message )
    (T) message
  }

  def <T> T  bindAndAssertObject( BaseQueryBinding binding, Class<T> messageClass, String action, Object bean, int expectedParameterCount ) {
    Map<String,String> parameters = Maps.newHashMap()
    putParameters( "", bean, parameters )
    // sanity check parameter count to ensure we're not missing something ...
    assertEquals( "Parameter count for " + action + ": " + parameters, expectedParameterCount, parameters.size() )
    Object message = bind( binding, action, parameters )
    assertTrue( action + ' message type', messageClass.isInstance( message ) )
    assertRecursiveEquality( action, "", bean, message )
    (T) message
  }

  void assertRecursiveEquality( String action, String prefix, Object expected, Object actual ) {
    expected.class.getDeclaredFields().findAll{ Field field -> isBoundField( field ) }.each { Field field ->
      field.setAccessible(true)
      Object expectedValueObject = field.get( expected )
      Object actualValueObject = field.get( actual )
      if ( EucalyptusData.class.isInstance( expectedValueObject ) ) {
        assertRecursiveEquality( action, prefix + field.getName() + '.', expectedValueObject, actualValueObject )
      } else if ( expectedValueObject instanceof ArrayList ) {
        ((List)expectedValueObject).eachWithIndex { Object item, Integer index ->
          if ( ((List)actualValueObject).size() <= index ) fail( action + " property " + prefix + field.getName() + '.' + (index+1) + ' missing' );
          if ( EucalyptusData.class.isInstance( item ) ) {
            assertRecursiveEquality( action, prefix + field.getName() + '.' + (index+1) + '.', item, ((List)actualValueObject).get(index) )
          } else {
            assertEquals( action + " property " + prefix + field.getName() + '.' + (index+1), item, ((List)actualValueObject).get(index)  )
          }
        }
      } else if ( expectedValueObject != null ) {
        if ( expectedValueObject instanceof Double ) {
          assertEquals( action + " property " + prefix + field.getName(), (Double)expectedValueObject, (Double)actualValueObject, 0 )
        } else {
          assertEquals( action + " property " + prefix + field.getName(), expectedValueObject, actualValueObject )
        }
      } else if ( actualValueObject != null && !(actualValueObject instanceof EucalyptusData) ) {
        fail( "Expected null for " + action + " property " + prefix + field.getName() )
      }
    }
  }

  Object bind( BaseQueryBinding binding, String action, Map<String,String> parameters ) {
    binding.bind( new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.GET, "/service?Action="+action+
        (parameters.inject("",{ params, entry -> params + '&' + entry.key + '=' + entry.getValue() })) ) )
  }

  boolean isBoundField( Field field ) {
    !field.getName().startsWith( '$' ) && !field.getName().startsWith('_')
  }

  void putParameters( String prefix, Object bean, Map<String,String> parameters ) {
    bean.class.getDeclaredFields().findAll{ Field field -> isBoundField( field ) }.each { Field field ->
      field.setAccessible(true)
      Object valueObject = field.get( bean )
      if ( EucalyptusData.class.isInstance( valueObject ) ) {
        putParameters( prefix + name(field) + '.', valueObject, parameters )
      } else if ( valueObject instanceof ArrayList ) {
        ((List)valueObject).eachWithIndex{ Object item, Integer index ->
          if ( EucalyptusData.isInstance( item ) ) {
            putParameters( prefix + name(field) + '.' + (index + 1) + '.', item, parameters )
          } else {
            parameters.put( prefix + name(field) + '.' + (index + 1), value(item) )
          }
        }
      } else if ( value( valueObject ) != null ) {
        parameters.put( prefix + name(field), value( valueObject ) )
      }
    }
  }

  String name( Field field ) {
    String name = (field.getAnnotation( HttpParameterMapping.class )?.parameter() as List<String>)?.get( 0 )
    if ( name == null ) {
      name = ((field.getAnnotation( HttpParameterMappings.class )?.value() as List<HttpParameterMapping>)?.last()?.parameter() as List<String>)?.get( 0 )
      if ( name == null ) {
        name = String.valueOf(field.getName().charAt(0).toUpperCase()) + field.getName().substring(1)
      }
    }
    name
  }

  String value( Object valueObject ) {
    String value = null
    if ( valueObject instanceof Date ) {
      SimpleDateFormat format = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" )
      format.setTimeZone( TimeZone.getTimeZone( 'GMT' ) )
      value = format.format( valueObject )
    } else if ( isSimpleType( valueObject?.class )) {
      value = valueObject.toString();
    }
    value
  }  
  
  static class TestBinding extends Binding {
    private List<Class> requestMessageClasses

    public TestBinding( List<Class> requestMessageClasses ) {
      super( "test_binding" )
      this.requestMessageClasses = requestMessageClasses
    }

    @Override
    Class getElementClass(final String elementName) {
      requestMessageClasses.find{ Class clazz -> clazz.getSimpleName().equals( elementName ) }
    }
  }  
}
