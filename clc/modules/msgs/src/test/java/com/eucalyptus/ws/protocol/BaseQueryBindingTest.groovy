/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

import com.eucalyptus.binding.HttpValue

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
  void testNestedData() {
    BaseQueryBinding binding = new TestQueryBinding( new TestBinding() );

    Object nestedDataObject = bind( binding, "/service?Operation=NestedData&Data.1.child.member.1=a&Data.1.child.member.2=b&Data.1.child.member.3=c&Data.1.child.ints.1=3&Data.1.child.ints.2=2&Data.1.child.ints.3=1" +
                                                                                                "&Data.2.child.member.1=z&Data.2.child.member.2=y&Data.2.child.member.3=x&Data.2.child.ints.1=1&Data.2.child.ints.2=2&Data.2.child.ints.3=3")
    assertTrue( "Multiple bound value", nestedDataObject instanceof NestedData )
    NestedData nestedData = (NestedData) nestedDataObject
    assertEquals( "Data value", [ new NestedDataChild( child: new NestedDataGrandChild( member: [ new NestedDataGrandChildValue(value: "a"),new NestedDataGrandChildValue(value: "b"),new NestedDataGrandChildValue(value: "c")], ints: [3,2,1] ) ), new NestedDataChild( child: new NestedDataGrandChild( member: [new NestedDataGrandChildValue(value: "z"),new NestedDataGrandChildValue(value: "y"),new NestedDataGrandChildValue(value: "x")], ints: [1,2,3] ) ) ], nestedData.data )
  }

  @Test
  void testHttpEmbeddedAnnotation() {
    BaseQueryBinding binding = new TestQueryBinding( new TestBinding() );

    Object HttpEmbeddedAnnotatedObject = bind( binding, "/service?Operation=HttpEmbeddedAnnotated&Data.1.embeddedMember.1=a&Data.1.embeddedMember.2=b&Data.1.embeddedMember.3=c&Data.1.embeddedInts.1=3&Data.1.embeddedInts.2=2&Data.1.embeddedInts.3=1" +
        "&Data.2.embeddedMember.1=z&Data.2.embeddedMember.2=y&Data.2.embeddedMember.3=x&Data.2.embeddedInts.1=1&Data.2.embeddedInts.2=2&Data.2.embeddedInts.3=3")
    assertTrue( "Multiple bound value", HttpEmbeddedAnnotatedObject instanceof HttpEmbeddedAnnotated )
    HttpEmbeddedAnnotated httpEmbeddedAnnotated = (HttpEmbeddedAnnotated) HttpEmbeddedAnnotatedObject
    assertEquals( "Data value", [ new HttpEmbeddedData( httpEmbeddedData2: new HttpEmbeddedData2( member: ["a","b","c"], ints: [3,2,1] ) ), new HttpEmbeddedData( httpEmbeddedData2: new HttpEmbeddedData2( member: ["z","y","x"], ints: [1,2,3] ) ) ], httpEmbeddedAnnotated.data )
  }

  @Test
  void testHttpEmbeddedAnnotationZeroIndex() {
    BaseQueryBinding binding = new TestQueryBinding( new TestBinding() );

    Object HttpEmbeddedAnnotatedObject = bind( binding, "/service?Operation=HttpEmbeddedAnnotated&Data.0.embeddedMember.0=a&Data.0.embeddedMember.1=b&Data.0.embeddedMember.2=c&Data.0.embeddedInts.0=3&Data.0.embeddedInts.1=2&Data.0.embeddedInts.2=1" +
        "&Data.1.embeddedMember.0=z&Data.1.embeddedMember.1=y&Data.1.embeddedMember.2=x&Data.1.embeddedInts.0=1&Data.1.embeddedInts.1=2&Data.1.embeddedInts.2=3")
    assertTrue( "Multiple bound value", HttpEmbeddedAnnotatedObject instanceof HttpEmbeddedAnnotated )
    HttpEmbeddedAnnotated httpEmbeddedAnnotated = (HttpEmbeddedAnnotated) HttpEmbeddedAnnotatedObject
    assertEquals( "Data value", [ new HttpEmbeddedData( httpEmbeddedData2: new HttpEmbeddedData2( member: ["a","b","c"], ints: [3,2,1] ) ), new HttpEmbeddedData( httpEmbeddedData2: new HttpEmbeddedData2( member: ["z","y","x"], ints: [1,2,3] ) ) ], httpEmbeddedAnnotated.data )
  }

  @Test
  void testHttpEmbeddedAnnotationLeadingZeroIndex() {
    BaseQueryBinding binding = new TestQueryBinding( new TestBinding() );

    Object HttpEmbeddedAnnotatedObject = bind( binding, "/service?Operation=HttpEmbeddedAnnotated&Data.0000001.embeddedMember.01=a&Data.001.embeddedMember.002=b&Data.001.embeddedMember.003=c&Data.001.embeddedInts.001=3&Data.001.embeddedInts.002=2&Data.001.embeddedInts.003=1" +
        "&Data.002.embeddedMember.001=z&Data.002.embeddedMember.002=y&Data.002.embeddedMember.003=x&Data.002.embeddedInts.001=1&Data.002.embeddedInts.002=2&Data.002.embeddedInts.003=3")
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

    Object HttpEmbeddedAnnotatedObject = bind( binding, "/service?Operation=HttpEmbeddedVersioned&embeddedMember.1=a&embeddedMember.2=b&embeddedMember.3=c&embeddedInts.1=3&embeddedInts.2=2&embeddedInts.3=1")
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

    Object HttpEmbeddedAnnotatedObject = bind( binding, "/service?Operation=HttpEmbeddedVersioned&Data.1.embeddedMember.1=a&Data.1.embeddedMember.2=b&Data.1.embeddedMember.3=c&Data.1.embeddedInts.1=3&Data.1.embeddedInts.2=2&Data.1.embeddedInts.3=1" +
        "&Data.2.embeddedMember.1=z&Data.2.embeddedMember.2=y&Data.2.embeddedMember.3=x&Data.2.embeddedInts.1=1&Data.2.embeddedInts.2=2&Data.2.embeddedInts.3=3")
    assertTrue( "Multiple bound value", HttpEmbeddedAnnotatedObject instanceof HttpEmbeddedVersioned )
    HttpEmbeddedVersioned httpEmbeddedAnnotated = (HttpEmbeddedVersioned) HttpEmbeddedAnnotatedObject
    assertEquals( "Data value", [ new HttpEmbeddedData( httpEmbeddedData2: new HttpEmbeddedData2( member: ["a","b","c"], ints: [3,2,1] ) ), new HttpEmbeddedData( httpEmbeddedData2: new HttpEmbeddedData2( member: ["z","y","x"], ints: [1,2,3] ) ) ], httpEmbeddedAnnotated.data )
  }

  @Test
  void testHttpValueMessageProperty() {
    BaseQueryBinding binding = new TestQueryBinding( new TestBinding( ) )
    Object HttpValueTypesObject = bind( binding, "/service?Operation=HttpValueTypes&DataValue=value" )
    assertTrue( "Bound type", HttpValueTypesObject instanceof HttpValueTypes )
    HttpValueTypes httpValueTypes = (HttpValueTypes) HttpValueTypesObject
    assertEquals( "Data value", "value", httpValueTypes?.dataValue?.value  )
    assertNull( "Data value wrapper", httpValueTypes.dataValueWrapper  )
  }

  @Test
  void testHttpValueMessageNestedProperty( ) {
    BaseQueryBinding binding = new TestQueryBinding( new TestBinding( ) )
    Object HttpValueTypesObject = bind( binding, "/service?Operation=HttpValueTypes&DataValueWrapper.DataValue=value" )
    assertTrue( "Bound type", HttpValueTypesObject instanceof HttpValueTypes )
    HttpValueTypes httpValueTypes = (HttpValueTypes) HttpValueTypesObject
    assertEquals( "Data value wrapper", "value", httpValueTypes?.dataValueWrapper?.dataValue?.value  )
    assertNull( "Data value", httpValueTypes.dataValue  )
  }

  @Test
  void testHttpValueMessageNestedAliasProperty( ) {
    BaseQueryBinding binding = new TestQueryBinding( new TestBinding( ) )
    Object HttpValueTypesObject = bind( binding, "/service?Operation=HttpValueTypes&DataValueWrapper.DataValue.Value=value" )
    assertTrue( "Bound type", HttpValueTypesObject instanceof HttpValueTypes )
    HttpValueTypes httpValueTypes = (HttpValueTypes) HttpValueTypesObject
    assertEquals( "Data value wrapper", "value", httpValueTypes?.dataValueWrapper?.dataValue?.value  )
    assertNull( "Data value", httpValueTypes.dataValue  )
  }

  Object bind( BaseQueryBinding binding, String url ) {
    binding.bind( new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.GET, url ) )
  }

  static class TestBinding extends Binding {
    private List<Class> requestMessageClasses = [
        SingleSimpleTypes,
        MultipleSimpleTypes,
        HttpParameterAnnotated,
        HttpEmbeddedAnnotated,
        HttpEmbeddedData,
        HttpEmbeddedData2,
        HttpEmbeddedVersioned,
        NestedData,
        HttpValueTypes,
        DataValue,
        DataValueWrapper
    ]

    TestBinding() {
      super( "test_binding" )
    }

    @Override
    Class getElementClass(final String elementName) {
      requestMessageClasses.find{ Class clazz -> clazz.getSimpleName().equals( elementName.toString() ) }
    }
  }

  @Test
  void testReplaceStringPrefixIfExists( ) {
    assertEquals(BaseQueryBinding.replaceStringPrefixIfExists("DataValue.Value","Value",""),"DataValue.Value");
    assertEquals(BaseQueryBinding.replaceStringPrefixIfExists("Value.Value","Value.",""),"Value");
    assertEquals(BaseQueryBinding.replaceStringPrefixIfExists("DataValue.Value","DataValue.",""),"Value");
    assertEquals(BaseQueryBinding.replaceStringPrefixIfExists("DataValue.Value.Value","DataValue.","NewPrefix."),"NewPrefix.Value.Value");
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
  @HttpParameterMapping(parameter="embeddedMember")
  ArrayList<String> member = Lists.newArrayList()
  @HttpParameterMapping(parameter="embeddedInts")
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

class NestedData extends EucalyptusMessage {
  @HttpEmbedded(multiple=true)
  ArrayList<NestedDataChild> data = Lists.newArrayList()

  boolean equals(final o) {
    if (this.is(o)) return true
    if (getClass() != o.class) return false

    final NestedData that = (NestedData) o

    if (data != that.data) return false

    return true
  }

  int hashCode() {
    return data.hashCode()
  }
}

class HttpValueTypes extends EucalyptusMessage {
  DataValue dataValue
  DataValueWrapper dataValueWrapper
}

class NestedDataChild extends EucalyptusData {
  @HttpParameterMapping(parameter="child")
  NestedDataGrandChild child

  boolean equals(final o) {
    if (this.is(o)) return true
    if (getClass() != o.class) return false

    final NestedDataChild that = (NestedDataChild) o

    if (child != that.child) return false

    return true
  }

  int hashCode() {
    return (child != null ? child.hashCode() : 0)
  }
}

class NestedDataGrandChild extends EucalyptusData {
  @HttpParameterMapping(parameter="member")
  @HttpEmbedded(multiple = true)
  ArrayList<NestedDataGrandChildValue> member = Lists.newArrayList()
  @HttpParameterMapping(parameter="ints")
  ArrayList<Integer> ints = Lists.newArrayList()

  boolean equals(final o) {
    if (this.is(o)) return true
    if (getClass() != o.class) return false

    final NestedDataGrandChild that = (NestedDataGrandChild) o

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

class DataValueWrapper extends EucalyptusData {
  @HttpParameterMapping(parameter = [ "DataValue", "DataValue.Value" ])
  DataValue dataValue
}

class DataValue extends EucalyptusData {
  @HttpValue
  String value
}

class NestedDataGrandChildValue extends EucalyptusData {
  @HttpValue
  String value

  boolean equals(final o) {
    if (this.is(o)) return true
    if (getClass() != o.class) return false

    final NestedDataGrandChildValue that = (NestedDataGrandChildValue) o

    if (value != that.value) return false

    return true
  }

  int hashCode() {
    return value.hashCode()
  }
}
