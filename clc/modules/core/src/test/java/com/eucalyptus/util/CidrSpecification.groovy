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
package com.eucalyptus.util

import com.google.common.base.Functions
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.net.InetAddresses
import spock.lang.Specification

import static com.eucalyptus.util.Cidr.fromAddress
import static com.eucalyptus.util.Cidr.parse

/**
 *
 */
class CidrSpecification extends Specification {

  def 'should support full range of prefix values'() {
    expect: 'parsed cidr equals specified cidr'
    parse( cidr ) == cidr( ip, prefix )

    where:
    cidr         | ip       | prefix
    '0.0.0.0/0'  | 0        | 0
    '1.1.1.1'    | 16843009 | 32
    '1.1.1.1/32' | 16843009 | 32
  }

  def 'should support lax parsing'() {
    expect: 'parsed and normalized cidr equals specified cidr'
    parse( cidr, true ) == cidr( ip, prefix )

    where:
    cidr                 | ip         | prefix
    '255.255.255.255/0'  | 0          | 0
    '10.0.0.0/0'         | 0          | 0
    '127.255.255.255/16' | 2147418112 | 16
    '1.1.1.1'            | 16843009   | 32
    '1.1.1.1/32'         | 16843009   | 32
  }

  def 'should reject invalid cidrs with IllegalArgumentException'() {
    when: 'parsing invalid cidr'
    parse( cidr )

    then:
    thrown(IllegalArgumentException)

    where:
    cidr << [
        null,
        '',
        '   ',
        '/',
        'asdf',
        '0',
        '/0',
        '0/0',
        '0.0.0.0/-1',
        '0.0.0.0/33',
        '-1.-1.-1.-1/1',
        '1.1.1.1/32/',
        '1.1.1.1/31',
        ' 1.1.1.1/32',
        '1.1.1.1/32 ',
        '1.1.1.1 / 32',
        '1.1.1.1 /32',
        '1.1.1.1/\t32'
    ]
  }

  def 'should be able to check for contained ips'(){
    expect: 'parsed cidr containment check has expected result'
    parse( cidr ).contains( perhapsContains ) == result

    where:
    cidr                | perhapsContains   | result
    '0.0.0.0/0'         | '10.1.0.0'        | true
    '10.1.0.0/32'       | '10.1.0.0'        | true
    '10.1.0.0/16'       | '10.1.0.0'        | true
    '10.1.0.0/24'       | '10.1.1.0'        | false
    '10.1.0.0/32'       | '10.1.0.1'        | false
    '10.0.0.0/8'        | '10.1.0.1'        | true
  }

  def 'should be able to check for contained cidrs'(){
    expect: 'parsed cidr containment check has expected result'
    parse( cidr ).contains( parse( perhapsContains ) ) == result

    where:
    cidr                | perhapsContains   | result
    '0.0.0.0/0'         | '10.1.0.0/32'     | true
    '0.0.0.0/0'         | '0.0.0.0/0'       | true
    '0.0.0.0/0'         | '0.0.0.0/1'       | true
    '10.1.0.0/32'       | '10.1.0.0/32'     | true
    '10.1.0.0/16'       | '10.1.0.0/16'     | true
    '10.1.0.0/16'       | '10.0.0.0/15'     | false
    '10.1.0.0/16'       | '10.1.0.0/17'     | true
    '10.1.0.0/24'       | '10.1.1.0/24'     | false
    '10.1.0.0/32'       | '10.1.0.1/32'     | false
    '10.0.0.0/8'        | '10.1.0.1/32'     | true
    '10.10.0.0/16'      | '10.11.0.0/16'    | false
  }

  def 'should have a string representation'(){
    expect: 'parsed cidr string conversion check'
    parse( cidr, true ).toString( ) == result

    where:
    cidr                | result
    '0.0.0.0/0'         | '0.0.0.0/0'
    '10.0.0.0/0'        | '0.0.0.0/0'
    '10.1.0.0/32'       | '10.1.0.0/32'
    '10.0.0.0/8'        | '10.0.0.0/8'
  }

  def 'should fail for invalid prefix when not parsing as lax'(){
    when: 'cidr with invalid prefix parsed'
    parse( cidr )

    then: 'an exception is thrown'
    thrown IllegalArgumentException

    where:
    cidr << [
      '10.0.0.0/0',
      '255.255.255.255/31'
    ]
  }

  def 'should allow use as a map key'(){
    given: 'a map using a cidr key'
    Map<Cidr,String> map = Maps.newHashMap( )
    map.put( parse( cidr ), value  )

    expect: 'get value from map successful'
    map.get( parse( cidr ) ) == value

    where:
    cidr                | value
    '0.0.0.0/0'         | '1'
    '10.1.0.0/32'       | '10.1.0.0/32'
    '10.0.0.0/8'        | 'value'
  }

  def 'should be able to create CIDR for an address and prefix'(){
    expect: 'parsed cidr containment check has expected result'
    fromAddress( InetAddresses.forString( address ), prefix ) == parse( result )

    where:
    address           | prefix    | result
    '1.2.3.4'         | 0         | '0.0.0.0/0'
    '1.2.3.4'         | 8         | '1.0.0.0/8'
    '1.2.3.4'         | 16        | '1.2.0.0/16'
    '1.2.3.4'         | 24        | '1.2.3.0/24'
    '1.2.3.4'         | 32        | '1.2.3.4/32'
    '255.255.255.255' | 1         | '128.0.0.0/1'
    '255.255.255.255' | 31        | '255.255.255.254/31'
    '0.0.0.0'         | 32        | '0.0.0.0/32'
    '0.0.0.0'         | 0         | '0.0.0.0/0'
    '0.0.0.0'         | 1         | '0.0.0.0/1'
  }

  def 'should be able to split a CIDR'(){
    expect: 'cidr split has expected result'
    Lists.newArrayList( Iterables.transform( parse( cidr ).split( split ), Functions.toStringFunction( ) ) ) == result

    where:
    cidr              | split     | result
    '172.31.0.0/16'   | 16        | [ '172.31.0.0/20', '172.31.16.0/20', '172.31.32.0/20', '172.31.48.0/20', '172.31.64.0/20', '172.31.80.0/20', '172.31.96.0/20', '172.31.112.0/20', '172.31.128.0/20', '172.31.144.0/20', '172.31.160.0/20', '172.31.176.0/20', '172.31.192.0/20', '172.31.208.0/20', '172.31.224.0/20', '172.31.240.0/20' ]
    '172.31.0.0/16'   | 1         | [ '172.31.0.0/16' ]
    '10.0.0.0/8'      | 2         | [ '10.0.0.0/9', '10.128.0.0/9' ]
  }


  private static Cidr cidr( int ip, int prefix ) {
    Cidr.of( ip, prefix )
  }
}
