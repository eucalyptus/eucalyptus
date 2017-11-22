/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

import com.eucalyptus.network.config.NetworkConfigurationApi
import com.eucalyptus.network.config.ImmutableNetworkConfigurationApi
import com.eucalyptus.util.Pair
import spock.lang.Specification

import static com.eucalyptus.network.ManagedSubnets.addressToIndex
import static com.eucalyptus.network.ManagedSubnets.indexToAddress
import static com.eucalyptus.network.ManagedSubnets.restrictToMaximumSegment
import static com.eucalyptus.network.ManagedSubnets.validSegmentForSubnet

/**
 *
 */
class ManagedSubnetsSpecification extends Specification {

  def 'should allow low subnet values'() {
    expect: 'calculated address equals specified value'
    indexToAddress( managedSubnet( subnet, size ), tag, index ) == address

    and: 'calculated tag/index equals specified address'
    addressToIndex( managedSubnet( subnet, size ), address ) == pair( tag, index )

    where:
    subnet        | size  | tag | index | address
    '0.0.0.0'     |  32   |   2 |    0l | '0.0.0.0'
    '0.0.0.0'     |  32   |   2 |    1l | '0.0.0.1'
    '0.0.0.0'     |  32   |   3 |    1l | '0.0.0.33'
    '8.8.8.0'     |  32   |   3 |    1l | '8.8.8.33'
    '8.8.8.0'     |  32   |  12 |    1l | '8.8.9.65'
    '8.8.8.0'     |  32   |  12 |   10l | '8.8.9.74'
  }

  def 'should allow high subnet values'() {
    expect: 'calculated address equals specified value'
    indexToAddress( managedSubnet( subnet, size ), tag, index ) == address

    and: 'calculated tag/index equals specified address'
    addressToIndex( managedSubnet( subnet, size ), address ) == pair( tag, index )

    where:
    subnet          | size  | tag | index | address
    '255.255.255.0' |  32   |   2 |    1l | '255.255.255.1'
    '255.255.255.0' |  32   |   3 |   10l | '255.255.255.42'
    '255.255.255.0' |  32   |   4 |   20l | '255.255.255.84'
    '255.255.255.0' |  32   |   5 |   30l | '255.255.255.126'
    '255.255.255.0' |  32   |   8 |   30l | '255.255.255.222'
  }

  def 'should allow large size'() {
    expect: 'calculated address equals specified value'
    indexToAddress( managedSubnet( subnet, size ), tag, index ) == address

    and: 'calculated tag/index equals specified address'
    addressToIndex( managedSubnet( subnet, size ), address ) == pair( tag, index )

    where:
    subnet    | size | tag | index | address
    '0.0.0.0' |   64 |   5 |   17l | '0.0.0.209'
    '0.0.0.0' |  128 |   5 |   17l | '0.0.1.145'
    '0.0.0.0' |  256 |   5 |   17l | '0.0.3.17'
    '0.0.0.0' |  512 |   5 |   17l | '0.0.6.17'
    '0.0.0.0' | 1024 |   5 |   17l | '0.0.12.17'
    '0.0.0.0' | 8192 |   5 |   17l | '0.0.96.17'
  }

  def 'should allow high index'() {
    expect: 'calculated address equals specified value'
    indexToAddress( managedSubnet( subnet, size ), tag, index ) == address

    and: 'calculated tag/index equals specified address'
    addressToIndex( managedSubnet( subnet, size ), address ) == pair( tag, index )

    where:
    subnet    | size | tag | index | address
    '0.0.0.0' |   64 |   5 |   63l | '0.0.0.255'
    '0.0.0.0' |  128 |   5 |  127l | '0.0.1.255'
    '0.0.0.0' |  256 |   5 |  255l | '0.0.3.255'
    '0.0.0.0' |  512 |   5 |  511l | '0.0.7.255'
    '0.0.0.0' | 1024 |   5 | 1023l | '0.0.15.255'
    '0.0.0.0' | 8192 |   5 | 8191l | '0.0.127.255'
  }

  def 'should restrict maximum vlan for subnet settings'() {
    expect: 'vlan restricted when appropriate'
    restrictToMaximumSegment( managedSubnet( subnet, netmask, size, 2, maxVlan ), maxVlan ) == restrictedMaxVlan

    where:
    subnet          |         netmask | size | maxVlan | restrictedMaxVlan
    '10.0.0.0'      |     '255.0.0.0' |   32 |    4095 |              4095
    '10.10.10.0'    | '255.255.255.0' |  128 |    4095 |                 1
    '10.10.10.0'    | '255.255.255.0' |    2 |    4095 |               127
    '10.10.10.0'    | '255.255.255.0' |    2 |     100 |               100
  }

  def 'should determine invalid minimum vlan for subnet settings'() {
    expect: 'invalid minimum vlans identified'
    validSegmentForSubnet( managedSubnet( subnet, netmask, size, minVlan, 4096 ), minVlan ) == valid

    where:
    subnet          |         netmask | size | minVlan | valid
    '10.0.0.0'      |     '255.0.0.0' |   32 |    4095 | true
    '10.10.10.0'    | '255.255.255.0' |  128 |    4095 | false
    '10.10.10.0'    | '255.255.255.0' |    2 |     127 | true
    '10.10.10.0'    | '255.255.255.0' |    2 |     128 | false
    '10.10.10.0'    | '255.255.255.0' |    2 |     100 | true
    '10.10.10.0'    | '255.255.255.0' |    2 |       1 | true
    '172.16.0.0'    |   '255.255.0.0' | 2048 |     512 | false
  }

  private static NetworkConfigurationApi.ManagedSubnet managedSubnet(String subnet, Integer size ) {
    ImmutableNetworkConfigurationApi.ManagedSubnet.builder( )
        .setValueName( subnet )
        .setValueSubnet( subnet )
        .setValueSegmentSize( size )
        .setValueMinVlan( 2 )
        .setValueMaxVlan( 4095 )
        .o( )
  }

  private static NetworkConfigurationApi.ManagedSubnet managedSubnet(String subnet, String netmask, Integer size, Integer minVlan, Integer maxVlan ) {
    ImmutableNetworkConfigurationApi.ManagedSubnet.builder( )
        .setValueName( subnet )
        .setValueSubnet( subnet )
        .setValueNetmask( netmask )
        .setValueSegmentSize( size )
        .setValueMinVlan( minVlan )
        .setValueMaxVlan( maxVlan )
        .o( )
  }

  private static Pair<Integer,Long> pair( int tag, long index ) {
    Pair.pair( tag, index )
  }

}
