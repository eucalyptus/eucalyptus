/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package edu.ucsb.eucalyptus.msgs

import com.eucalyptus.empyrean.DescribeServicesType
import com.google.common.collect.Lists
import org.hamcrest.Matchers

import static org.junit.Assert.*
import org.junit.Test

/**
 *
 */
class BaseMessagesTest {

  @Test
  void testXmlRoundTrip( ) {
    BaseMessage message = BaseMessages.fromOm( BaseMessages.toOm( new BaseMessage( ).markFailed( ) ), BaseMessage )
    assertFalse( 'return', message.get_return( ) )
  }

  @Test
  void testXmlNullElements( ) {
    DescribeServicesType message = BaseMessages.fromOm( BaseMessages.toOm( new DescribeServicesType( ) ), DescribeServicesType )
    assertNotNull( 'services list empty not null', message.services )
  }

  @Test
  void testToString( ) {
    String text = new BaseMessage( ).markFailed( ).toString( )
    assertThat( 'contains failed', text, Matchers.containsString( 'false' ) )
  }

  @Test
  void testReadOnlyConvenienceGetters( ) {
    FooMessage message = BaseMessages.fromOm( BaseMessages.toOm( new FooMessage(
        bars: [
            new BarData( barValue: 'value 1' ),
            new BarData( barValue: 'value 2' ),
        ]
    ) ), FooMessage )
    assertNotNull( 'bars not null', message.bars )
    assertEquals( 'bars length 2', 2, message.bars.size( ) )
    assertEquals( 'convenience bars length 2', 2, message.barValues.size( ) )
  }

  static class FooMessage extends BaseMessage {
    ArrayList<BarData> bars = Lists.newArrayList( )
    ArrayList<String> getBarValues( ) {
      Lists.newArrayList( bars*.barValue )
    }
  }

  static class BarData {
    String barValue
  }
}
