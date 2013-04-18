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
import org.junit.Test
import com.eucalyptus.binding.Binding
import com.eucalyptus.http.MappingHttpRequest
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.handler.codec.http.HttpMethod
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage
import com.google.common.collect.Lists
import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.binding.HttpEmbedded
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import com.eucalyptus.binding.HttpEmbeddeds

/**
 * 
 */
class BaseQueryBindingTest {
  
  @Test
  void testSimpleSingleQueryBindings() {
    BaseQueryBinding binding = new TestQueryBinding( new TestBinding() );
    
    Object singleSimpleBoundObject = bind( binding, "/service?Action=SingleSimpleTypes&NativeBooleanValue=true&BooleanValue=false&DateValue=2000-01-01T00:00:00.000Z&NativeDoubleValue=13.333&DoubleValue=12.111&NativeIntegerValue=17&IntegerValue=1&NativeLongValue=9999&LongValue=333333333333&StringValue=texthere" )
    assertTrue( "Simple bound value", singleSimpleBoundObject instanceof SingleSimpleTypes )
    SingleSimpleTypes singleSimpleBound = (SingleSimpleTypes) singleSimpleBoundObject
    assertEquals( "Native boolean value", true, singleSimpleBound.nativeBooleanValue )
    assertEquals( "Boolean value", false, singleSimpleBound.booleanValue )
    assertEquals( "Date value", new Date(946684800000L), singleSimpleBound.dateValue )
    assertEquals( "Native double value", 13.333, singleSimpleBound.nativeDoubleValue, 0 )
    assertEquals( "Double value", 12.111, singleSimpleBound.doubleValue, 0 )
    assertEquals( "Native integer value", 17, singleSimpleBound.nativeIntegerValue )
    assertEquals( "Integer value",1 , singleSimpleBound.integerValue )
    assertEquals( "Native long value", 9999, singleSimpleBound.nativeLongValue )
    assertEquals( "Long value", 333333333333, singleSimpleBound.longValue )
    assertEquals( "String value", "texthere", singleSimpleBound.stringValue )
  }

  @Test
  void testSimpleMultipleQueryBindings() {
    BaseQueryBinding binding = new TestQueryBinding( new TestBinding() );

    // Test binding with a single value, no .1, .2, etc
    Object singleMultipleSimpleBoundObject = bind( binding, "/service?Action=MultipleSimpleTypes&BooleanValue=false&DateValue=2000-01-01T00:00:00.000Z&DoubleValue=12.111&IntegerValue=1&LongValue=333333333333&StringValue=texthere" )
    assertTrue( "Multiple bound value", singleMultipleSimpleBoundObject instanceof MultipleSimpleTypes )
    MultipleSimpleTypes singleMultipleSimpleBound = (MultipleSimpleTypes) singleMultipleSimpleBoundObject
    assertEquals( "Boolean value", [false] as List, singleMultipleSimpleBound.booleanValue )
    assertEquals( "Date value", [new Date(946684800000L)] as List, singleMultipleSimpleBound.dateValue )
    assertEquals( "Double value", [12.111d] as List, singleMultipleSimpleBound.doubleValue )
    assertEquals( "Integer value", [1] as List , singleMultipleSimpleBound.integerValue )
    assertEquals( "Long value", [333333333333] as List, singleMultipleSimpleBound.longValue )
    assertEquals( "String value", ["texthere"] as List, singleMultipleSimpleBound.stringValue )

    // Test binding with .1 values
    Object multipleSimpleBoundObject = bind( binding, "/service?Action=MultipleSimpleTypes&BooleanValue.1=false&DateValue.1=2000-01-01T00:00:00.000Z&DoubleValue.1=12.111&IntegerValue.1=1&LongValue.1=333333333333&StringValue.1=texthere" )
    assertTrue( "Multiple bound value", multipleSimpleBoundObject instanceof MultipleSimpleTypes )
    MultipleSimpleTypes multipleSimpleBound = (MultipleSimpleTypes) multipleSimpleBoundObject
    assertEquals( "Boolean value", [false] as List, multipleSimpleBound.booleanValue )
    assertEquals( "Date value", [new Date(946684800000L)] as List, multipleSimpleBound.dateValue )
    assertEquals( "Double value", [12.111d] as List, multipleSimpleBound.doubleValue )
    assertEquals( "Integer value", [1] as List , multipleSimpleBound.integerValue )
    assertEquals( "Long value", [333333333333] as List, multipleSimpleBound.longValue )
    assertEquals( "String value", ["texthere"] as List, multipleSimpleBound.stringValue )

    // Test binding with multiple values .1 .2 ...
    Object multipleSimpleOrderedBoundObject = bind( binding, "/service?Action=MultipleSimpleTypes&BooleanValue.1=false&BooleanValue.2=false&BooleanValue.3=true&DateValue.2=2000-01-01T00:00:00.002Z&DateValue.1=2000-01-01T00:00:00.001Z&DoubleValue.1=12.111&DoubleValue.2=12.113&DoubleValue.3=12.112&IntegerValue.3=121&IntegerValue.1=1&IntegerValue.2=101&LongValue.1=333333333333&LongValue.2=6&StringValue.1=text1&StringValue.2=text2&StringValue.3=text3" )
    assertTrue( "Multiple bound value", multipleSimpleOrderedBoundObject instanceof MultipleSimpleTypes )
    MultipleSimpleTypes multipleSimpleOrderedBound = (MultipleSimpleTypes) multipleSimpleOrderedBoundObject
    assertEquals( "Boolean value", [false, false, true] as List, multipleSimpleOrderedBound.booleanValue )
    assertEquals( "Date value", [new Date(946684800001L),new Date(946684800002L)] as List, multipleSimpleOrderedBound.dateValue )
    assertEquals( "Double value", [12.111d,12.113d,12.112d] as List, multipleSimpleOrderedBound.doubleValue )
    assertEquals( "Integer value", [1,101,121] as List , multipleSimpleOrderedBound.integerValue )
    assertEquals( "Long value", [333333333333L, 6L] as List, multipleSimpleOrderedBound.longValue )
    assertEquals( "String value", ["text1","text2","text3"] as List, multipleSimpleOrderedBound.stringValue )
  }

  @Test
  void testHttpParameterAnnotation() {
    BaseQueryBinding binding = new TestQueryBinding( new TestBinding() );

    Object httpParameterAnnotatedObject = bind( binding, "/service?Action=HttpParameterAnnotated&Bool.1=false&Bool.2=false&z=value&date=2000-01-01T00:00:00.000" )
    assertTrue( "Multiple bound value", httpParameterAnnotatedObject instanceof HttpParameterAnnotated )
    HttpParameterAnnotated httpParameterAnnotated = (HttpParameterAnnotated) httpParameterAnnotatedObject
    assertEquals( "Boolean value", [false, false] as List, httpParameterAnnotated.booleanValues )
    assertEquals( "Date value", new Date(946684800000L), httpParameterAnnotated.dateValue )
    assertEquals( "String value", "value", httpParameterAnnotated.stringValue )
  }

  @Test
  void testHttpEmbeddedAnnotation() {
    BaseQueryBinding binding = new TestQueryBinding( new TestBinding() );

    Object HttpEmbeddedAnnotatedObject = bind( binding, "/service?Operation=HttpEmbeddedAnnotated&Data.1.embedded.member.1=a&Data.1.embedded.member.2=b&Data.1.embedded.member.3=c&Data.1.embedded.ints.1=3&Data.1.embedded.ints.2=2&Data.1.embedded.ints.3=1" +
                                                                                                "&Data.2.embedded.member.1=z&Data.2.embedded.member.2=y&Data.2.embedded.member.3=x&Data.2.embedded.ints.1=1&Data.2.embedded.ints.2=2&Data.2.embedded.ints.3=3")
    assertTrue( "Multiple bound value", HttpEmbeddedAnnotatedObject instanceof HttpEmbeddedAnnotated )
    HttpEmbeddedAnnotated httpEmbeddedAnnotated = (HttpEmbeddedAnnotated) HttpEmbeddedAnnotatedObject
    assertEquals( "Data value", [ new HttpEmbeddedData( httpEmbeddedData2: new HttpEmbeddedData2( member: ["a","b","c"], ints: [3,2,1] ) ), new HttpEmbeddedData( httpEmbeddedData2: new HttpEmbeddedData2( member: ["z","y","x"], ints: [1,2,3] ) ) ], httpEmbeddedAnnotated.data )
  }

  @Test
  void testHttpEmbeddedAnnotationZeroIndex() {
    BaseQueryBinding binding = new TestQueryBinding( new TestBinding() );

    Object HttpEmbeddedAnnotatedObject = bind( binding, "/service?Operation=HttpEmbeddedAnnotated&Data.0.embedded.member.0=a&Data.0.embedded.member.1=b&Data.0.embedded.member.2=c&Data.0.embedded.ints.0=3&Data.0.embedded.ints.1=2&Data.0.embedded.ints.2=1" +
        "&Data.1.embedded.member.0=z&Data.1.embedded.member.1=y&Data.1.embedded.member.2=x&Data.1.embedded.ints.0=1&Data.1.embedded.ints.1=2&Data.1.embedded.ints.2=3")
    assertTrue( "Multiple bound value", HttpEmbeddedAnnotatedObject instanceof HttpEmbeddedAnnotated )
    HttpEmbeddedAnnotated httpEmbeddedAnnotated = (HttpEmbeddedAnnotated) HttpEmbeddedAnnotatedObject
    assertEquals( "Data value", [ new HttpEmbeddedData( httpEmbeddedData2: new HttpEmbeddedData2( member: ["a","b","c"], ints: [3,2,1] ) ), new HttpEmbeddedData( httpEmbeddedData2: new HttpEmbeddedData2( member: ["z","y","x"], ints: [1,2,3] ) ) ], httpEmbeddedAnnotated.data )
  }

  @Test
  void testHttpEmbeddedAnnotationLeadingZeroIndex() {
    BaseQueryBinding binding = new TestQueryBinding( new TestBinding() );

    Object HttpEmbeddedAnnotatedObject = bind( binding, "/service?Operation=HttpEmbeddedAnnotated&Data.0000001.embedded.member.01=a&Data.001.embedded.member.002=b&Data.001.embedded.member.003=c&Data.001.embedded.ints.001=3&Data.001.embedded.ints.002=2&Data.001.embedded.ints.003=1" +
        "&Data.002.embedded.member.001=z&Data.002.embedded.member.002=y&Data.002.embedded.member.003=x&Data.002.embedded.ints.001=1&Data.002.embedded.ints.002=2&Data.002.embedded.ints.003=3")
    assertTrue( "Multiple bound value", HttpEmbeddedAnnotatedObject instanceof HttpEmbeddedAnnotated )
    HttpEmbeddedAnnotated httpEmbeddedAnnotated = (HttpEmbeddedAnnotated) HttpEmbeddedAnnotatedObject
    assertEquals( "Data value", [ new HttpEmbeddedData( httpEmbeddedData2: new HttpEmbeddedData2( member: ["a","b","c"], ints: [3,2,1] ) ), new HttpEmbeddedData( httpEmbeddedData2: new HttpEmbeddedData2( member: ["z","y","x"], ints: [1,2,3] ) ) ], httpEmbeddedAnnotated.data )
  }

  @Test
  void testHttpEmbeddedVersionedAnnotationA() {
    BaseQueryBinding binding = new TestQueryBinding( new TestBinding() ){
      @Override protected String getNamespaceForVersion(String bindingVersion) { bindingVersion }
      @Override String getNamespace() { "A" }
    };

    Object HttpEmbeddedAnnotatedObject = bind( binding, "/service?Operation=HttpEmbeddedVersioned&embedded.member.1=a&embedded.member.2=b&embedded.member.3=c&embedded.ints.1=3&embedded.ints.2=2&embedded.ints.3=1")
    assertTrue( "Multiple bound value", HttpEmbeddedAnnotatedObject instanceof HttpEmbeddedVersioned )
    HttpEmbeddedVersioned httpEmbeddedAnnotated = (HttpEmbeddedVersioned) HttpEmbeddedAnnotatedObject
    assertEquals( "Data value", [ new HttpEmbeddedData( httpEmbeddedData2: new HttpEmbeddedData2( member: ["a","b","c"], ints: [3,2,1] ) ) ], httpEmbeddedAnnotated.data )
  }

  @Test
  void testHttpEmbeddedVersionedAnnotationB() {
    BaseQueryBinding binding = new TestQueryBinding( new TestBinding() ){
      @Override protected String getNamespaceForVersion(String bindingVersion) { bindingVersion }
      @Override String getNamespace() { "B" }
    };

    Object HttpEmbeddedAnnotatedObject = bind( binding, "/service?Operation=HttpEmbeddedVersioned&Data.1.embedded.member.1=a&Data.1.embedded.member.2=b&Data.1.embedded.member.3=c&Data.1.embedded.ints.1=3&Data.1.embedded.ints.2=2&Data.1.embedded.ints.3=1" +
        "&Data.2.embedded.member.1=z&Data.2.embedded.member.2=y&Data.2.embedded.member.3=x&Data.2.embedded.ints.1=1&Data.2.embedded.ints.2=2&Data.2.embedded.ints.3=3")
    assertTrue( "Multiple bound value", HttpEmbeddedAnnotatedObject instanceof HttpEmbeddedVersioned )
    HttpEmbeddedVersioned httpEmbeddedAnnotated = (HttpEmbeddedVersioned) HttpEmbeddedAnnotatedObject
    assertEquals( "Data value", [ new HttpEmbeddedData( httpEmbeddedData2: new HttpEmbeddedData2( member: ["a","b","c"], ints: [3,2,1] ) ), new HttpEmbeddedData( httpEmbeddedData2: new HttpEmbeddedData2( member: ["z","y","x"], ints: [1,2,3] ) ) ], httpEmbeddedAnnotated.data )
  }

  Object bind( BaseQueryBinding binding, String url ) {
    binding.bind( new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.GET, url ) )
  }

  static class TestBinding extends Binding {
    private List<Class> requestMessageClasses = [
        SingleSimpleTypes.class,
        MultipleSimpleTypes.class,
        HttpParameterAnnotated.class,
        HttpEmbeddedAnnotated.class,
        HttpEmbeddedData.class,
        HttpEmbeddedData2.class,
        HttpEmbeddedVersioned.class,
    ]

    TestBinding() {
      super( "test_binding" )
    }

    @Override
    Class getElementClass(final String elementName) {
      requestMessageClasses.find{ Class clazz -> clazz.getSimpleName().equals( elementName.toString() ) }
    }
  }  
}

class SingleSimpleTypes extends EucalyptusMessage {
  boolean nativeBooleanValue
  Boolean booleanValue
  Date dateValue
  double nativeDoubleValue
  Double doubleValue
  int nativeIntegerValue
  Integer integerValue
  long nativeLongValue
  Long longValue
  String stringValue
}

class MultipleSimpleTypes extends EucalyptusMessage {
  ArrayList<Boolean> booleanValue = Lists.newArrayList()
  ArrayList<Date> dateValue = Lists.newArrayList()
  ArrayList<Double> doubleValue = Lists.newArrayList()
  ArrayList<Integer> integerValue = Lists.newArrayList()
  ArrayList<Long> longValue = Lists.newArrayList()
  ArrayList<String> stringValue = Lists.newArrayList()
}

class HttpParameterAnnotated extends EucalyptusMessage {
  @HttpParameterMapping(parameter="Bool")
  ArrayList<Boolean> booleanValues = Lists.newArrayList()
  @HttpParameterMapping(parameter="date")
  Date dateValue
  @HttpParameterMapping(parameter="z")
  String stringValue
}

class HttpEmbeddedData2 extends EucalyptusData {
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = Lists.newArrayList()
  @HttpParameterMapping(parameter="ints")
  ArrayList<Integer> ints = Lists.newArrayList()

  boolean equals(final o) {
    if (this.is(o)) return true
    if (getClass() != o.class) return false

    final HttpEmbeddedData2 that = (HttpEmbeddedData2) o

    if (ints != that.ints) return false
    if (member != that.member) return false

    return true
  }

  int hashCode() {
    int result
    result = member.hashCode()
    result = 31 * result + ints.hashCode()
    return result
  }
}

class HttpEmbeddedData extends EucalyptusData {
  @HttpEmbedded
  @HttpParameterMapping(parameter="embedded")
  HttpEmbeddedData2 httpEmbeddedData2

  boolean equals(final o) {
    if (this.is(o)) return true
    if (getClass() != o.class) return false

    final HttpEmbeddedData that = (HttpEmbeddedData) o

    if (httpEmbeddedData2 != that.httpEmbeddedData2) return false

    return true
  }

  int hashCode() {
    return (httpEmbeddedData2 != null ? httpEmbeddedData2.hashCode() : 0)
  }
}

class HttpEmbeddedAnnotated extends EucalyptusMessage {
  @HttpEmbedded(multiple=true)
  ArrayList<HttpEmbeddedData> data = Lists.newArrayList()

  boolean equals(final o) {
    if (this.is(o)) return true
    if (getClass() != o.class) return false

    final HttpEmbeddedAnnotated that = (HttpEmbeddedAnnotated) o

    if (data != that.data) return false

    return true
  }

  int hashCode() {
    return data.hashCode()
  }
}

class HttpEmbeddedVersioned extends EucalyptusMessage {
  @HttpEmbeddeds([
    @HttpEmbedded( version="A" ),
    @HttpEmbedded( version="B", multiple = true )
  ])
  ArrayList<HttpEmbeddedData> data = Lists.newArrayList()

  boolean equals(final o) {
    if (this.is(o)) return true
    if (getClass() != o.class) return false

    final HttpEmbeddedAnnotated that = (HttpEmbeddedAnnotated) o

    if (data != that.data) return false

    return true
  }

  int hashCode() {
    return data.hashCode()
  }
}
