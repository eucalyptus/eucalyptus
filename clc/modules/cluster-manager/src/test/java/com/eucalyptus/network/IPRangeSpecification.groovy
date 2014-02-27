/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.network

import com.eucalyptus.scripting.Groovyness
import spock.lang.Specification

import static com.eucalyptus.network.IPRange.fromSubnet
import static com.eucalyptus.network.IPRange.parse

/**
 *
 */
class IPRangeSpecification extends Specification {

  def 'should support a full range of IPv4'() {
    expect: 'parsed range equals specified range'
    Groovyness.expandoMetaClass( parse( range ) ) == range( lower, upper )
    Groovyness.expandoMetaClass( parse( range ) ).iterator( ).hasNext( )

    where:
    range                     | lower | upper
    '0.0.0.0-255.255.255.255' |  0    | -1
  }

  def 'should allow single values or ranges'() {
    expect: 'parsed range equals specified range'
    Groovyness.expandoMetaClass( parse( range ) ) == range( lower, upper )

    where:
    range                     | lower | upper
    '0.0.0.0'                 |  0    |  0
    '0.0.0.0-255.255.255.255' |  0    | -1
  }

  def 'should allow non-optimal single value'() {
    expect: 'parsed range equals specified range'
    Groovyness.expandoMetaClass( parse( range ) ) == range( lower, upper )

    where:
    range                     | lower | upper
    '0.0.0.0-0.0.0.0'         |  0    |  0
  }

  def 'should tolerate whitespace in ranges'() {
    expect: 'parsed range equals specified range'
    Groovyness.expandoMetaClass( parse( range ) ) == range( lower, upper )

    where:
    range                         | lower | upper
    ' 0.0.0.0-255.255.255.255'    |  0    | -1
    '0.0.0.0-255.255.255.255 '    |  0    | -1
    '0.0.0.0 - 255.255.255.255'   |  0    | -1
    ' 0.0.0.0 - 255.255.255.255 ' |  0    | -1
    '  0.0.0.0-255.255.255.255'   |  0    | -1
    '0.0.0.0-255.255.255.255  '   |  0    | -1
    '0.0.0.0  -  255.255.255.255' |  0    | -1
    '0.0.0.0\t-\t255.255.255.255' |  0    | -1
  }

  def 'should split range'() {
    expect: 'split'
    Groovyness.expandoMetaClass( parse( range ) ).split( ip ).collect{ Groovyness.expandoMetaClass(it) } == expected

    where:
    range                         | ip                | expected
    ' 0.0.0.0-255.255.255.255'    | '0.0.0.0'         | [ range( 1, -1 ) ]
    ' 0.0.0.0-255.255.255.255'    | '255.255.255.255' | [ range( 0, -2 ) ]
    ' 0.0.0.0-255.255.255.255'    | '0.0.0.10'        | [ range( 0, 9 ), range( 11, -1 ) ]
  }

  def 'should be possible to generate from subnet/netmask'() {
    expect: 'parsed range equals specified range'
    Groovyness.expandoMetaClass( fromSubnet( subnet, netmask ) ) == range( lower, upper )

    where:
    subnet        | netmask           | lower          | upper
    '0.0.0.0'     | '0.0.0.0'         |  0             | -1
    '10.20.30.40' | '0.0.0.0'         |  0             | -1
    '10.20.30.40' | '255.0.0.0'       |  0x0A000000    | 0x0AFFFFFF
    '10.10.10.40' | '255.255.255.0'   |  0x0A0A0A00    | 0x0A0A0AFF
    '10.10.10.40' | '255.255.255.128' |  0x0A0A0A00    | 0x0A0A0A7F
  }

  def 'should reject invalid ranges with IllegalArgumentException'() {
    when: 'parsing invalid range'
    Groovyness.expandoMetaClass( parse( range ) )

    then:
    thrown(IllegalArgumentException)

    where:
    range << [
        null,
        '',
        '   ',
        '-',
        'asdf',
        '0',
        '0-1',
        '-0.-1.-2.-3',
        '255.0.0.0-1.2.3.4',
        '1.2.3.5-1.2.3.4',
        '1.2.3.4-1.2.3.4-1.2.3.4',
    ]
  }

  private static IPRange range( lower, upper ) {
    new IPRange( lower, upper )
  }
}
