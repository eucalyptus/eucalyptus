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
package com.eucalyptus.network

import com.eucalyptus.util.Cidr
import spock.lang.Specification

import static com.eucalyptus.network.IPRange.fromCidr
import static com.eucalyptus.network.IPRange.fromSubnet
import static com.eucalyptus.network.IPRange.parse

/**
 *
 */
class IPRangeSpecification extends Specification {

  def 'should support a full range of IPv4'() {
    expect: 'parsed range equals specified range'
    parse( range ) == range( lower, upper )
    parse( range ).iterator( ).hasNext( )

    where:
    range                     | lower | upper
    '0.0.0.0-255.255.255.255' |  0    | -1
  }

  def 'should allow single values or ranges'() {
    expect: 'parsed range equals specified range'
    parse( range ) == range( lower, upper )

    where:
    range                     | lower | upper
    '0.0.0.0'                 |  0    |  0
    '0.0.0.0-255.255.255.255' |  0    | -1
  }

  def 'should allow non-optimal single value'() {
    expect: 'parsed range equals specified range'
    parse( range ) == range( lower, upper )

    where:
    range                     | lower | upper
    '0.0.0.0-0.0.0.0'         |  0    |  0
  }

  def 'should tolerate whitespace in ranges'() {
    expect: 'parsed range equals specified range'
    parse( range ) == range( lower, upper )

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
    parse( range ).split( ip ) == expected

    where:
    range                         | ip                | expected
    ' 0.0.0.0-255.255.255.255'    | '0.0.0.0'         | [ range( 1, -1 ) ]
    ' 0.0.0.0-255.255.255.255'    | '255.255.255.255' | [ range( 0, -2 ) ]
    ' 0.0.0.0-255.255.255.255'    | '0.0.0.10'        | [ range( 0, 9 ), range( 11, -1 ) ]
  }

  def 'should be possible to generate from subnet/netmask'() {
    expect: 'parsed range equals specified range'
    fromSubnet( subnet, netmask ) == range( lower, upper )

    where:
    subnet        | netmask           | lower          | upper
    '0.0.0.0'     | '0.0.0.0'         |  1             | -2
    '10.20.30.40' | '0.0.0.0'         |  1             | -2
    '10.20.30.40' | '255.0.0.0'       |  0x0A000001    | 0x0AFFFFFE
    '10.10.10.40' | '255.255.255.0'   |  0x0A0A0A01    | 0x0A0A0AFE
    '10.10.10.40' | '255.255.255.128' |  0x0A0A0A01    | 0x0A0A0A7E
    '10.10.10.0'  | '255.255.255.252' |  0x0A0A0A01    | 0x0A0A0A02
    '10.10.10.0'  | '255.255.255.254' |  0x0A0A0A00    | 0x0A0A0A01 // range too small to shrink
  }

  def 'should be possible to generate from cidr'() {
    expect: 'parsed range equals specified range'
    fromCidr( Cidr.parse( cidr ) ) == range( lower, upper )

    where:
    cidr             | lower          | upper
    '0.0.0.0/0'      |  1             | -2
    '10.0.0.0/8'     |  0x0A000001    | 0x0AFFFFFE
    '10.10.10.0/24'  |  0x0A0A0A01    | 0x0A0A0AFE
    '10.10.10.0/25'  |  0x0A0A0A01    | 0x0A0A0A7E
    '10.10.10.0/30'  |  0x0A0A0A01    | 0x0A0A0A02
    '10.10.10.0/31'  |  0x0A0A0A00    | 0x0A0A0A01 // range too small to shrink
  }

  def 'should reject invalid ranges with IllegalArgumentException'() {
    when: 'parsing invalid range'
    parse( range )

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

  def 'should be able to check for sub ranges'(){
    expect: 'parsed range containment check has expected result'
    parse( range ).contains( parse( perhapsContains ) ) == result

    where:
    range               | perhapsContains           | result
    '10.1.0.0-10.2.0.0' | '10.1.0.0'                | true
    '10.1.0.0-10.2.0.0' | '10.2.0.0'                | true
    '10.1.0.0-10.2.0.0' | '10.1.0.0-10.2.0.0'       | true
    '10.1.0.0-10.2.0.0' | '10.0.0.255-10.2.0.0'     | false
    '10.1.0.0-10.2.0.0' | '10.1.0.0-10.2.0.1'       | false
    '10.1.0.0-10.2.0.0' | '10.1.1.0-10.1.255.255'   | true
    '10.1.0.0-10.2.0.0' | '10.1.1.1'                | true
    '10.1.0.0'          | '10.1.0.0'                | true
    '10.1.0.0'          | '10.1.0.1'                | false
  }

  def 'should expose range size'(){
    expect: 'range size correctly calculated'
    parse( range ).size( ) == size

    where:
    range                 | size
    '10.1.0.0'            | 1
    '10.1.0.0-10.1.0.1'   | 2
    '10.1.0.0-10.1.0.255' | 256
    '10.1.0.0-10.1.1.0'   | 257
    '10.1.0.0-10.1.1.255' | 512
    '10.1.0.0-10.2.0.0'   | 65537
  }

  private static IPRange range( lower, upper ) {
    new IPRange( lower, upper )
  }
}
