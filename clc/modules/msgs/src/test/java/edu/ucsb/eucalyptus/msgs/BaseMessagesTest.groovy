/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
