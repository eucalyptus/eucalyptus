/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

import com.eucalyptus.network.config.ManagedSubnet
import com.eucalyptus.util.Pair
import spock.lang.Specification

import static com.eucalyptus.network.ManagedSubnets.addressToIndex
import static com.eucalyptus.network.ManagedSubnets.indexToAddress

/**
 *
 */
class ManagedSubnetsSpecification extends Specification {

  def 'should allow low subnet values'() {
    expect: 'calculated address equals specified value'
    indexToAddress( subnet( subnet, size ), tag, index ) == address

    and: 'calculated tag/index equals specified address'
    addressToIndex( subnet( subnet, size ), address ) == pair( tag, index )

    where:
    subnet        | size  | tag | index | address
    '0.0.0.0'     |  32   |   0 |    0l | '0.0.0.0'
    '0.0.0.0'     |  32   |   0 |    1l | '0.0.0.1'
    '0.0.0.0'     |  32   |   1 |    1l | '0.0.0.33'
    '8.8.8.0'     |  32   |   1 |    1l | '8.8.8.33'
    '8.8.8.0'     |  32   |  10 |    1l | '8.8.9.65'
    '8.8.8.0'     |  32   |  10 |   10l | '8.8.9.74'
  }

  def 'should allow high subnet values'() {
    expect: 'calculated address equals specified value'
    indexToAddress( subnet( subnet, size ), tag, index ) == address

    and: 'calculated tag/index equals specified address'
    addressToIndex( subnet( subnet, size ), address ) == pair( tag, index )

    where:
    subnet          | size  | tag | index | address
    '255.255.255.0' |  32   |   0 |    1l | '255.255.255.1'
    '255.255.255.0' |  32   |   1 |   10l | '255.255.255.42'
    '255.255.255.0' |  32   |   2 |   20l | '255.255.255.84'
    '255.255.255.0' |  32   |   3 |   30l | '255.255.255.126'
    '255.255.255.0' |  32   |   6 |   30l | '255.255.255.222'
  }

  def 'should allow large size'() {
    expect: 'calculated address equals specified value'
    indexToAddress( subnet( subnet, size ), tag, index ) == address

    and: 'calculated tag/index equals specified address'
    addressToIndex( subnet( subnet, size ), address ) == pair( tag, index )

    where:
    subnet    | size | tag | index | address
    '0.0.0.0' |   64 |   3 |   17l | '0.0.0.209'
    '0.0.0.0' |  128 |   3 |   17l | '0.0.1.145'
    '0.0.0.0' |  256 |   3 |   17l | '0.0.3.17'
    '0.0.0.0' |  512 |   3 |   17l | '0.0.6.17'
    '0.0.0.0' | 1024 |   3 |   17l | '0.0.12.17'
    '0.0.0.0' | 8192 |   3 |   17l | '0.0.96.17'
  }

  def 'should allow high index'() {
    expect: 'calculated address equals specified value'
    indexToAddress( subnet( subnet, size ), tag, index ) == address

    and: 'calculated tag/index equals specified address'
    addressToIndex( subnet( subnet, size ), address ) == pair( tag, index )

    where:
    subnet    | size | tag | index | address
    '0.0.0.0' |   64 |   3 |   63l | '0.0.0.255'
    '0.0.0.0' |  128 |   3 |  127l | '0.0.1.255'
    '0.0.0.0' |  256 |   3 |  255l | '0.0.3.255'
    '0.0.0.0' |  512 |   3 |  511l | '0.0.7.255'
    '0.0.0.0' | 1024 |   3 | 1023l | '0.0.15.255'
    '0.0.0.0' | 8192 |   3 | 8191l | '0.0.127.255'
  }

  private static ManagedSubnet subnet( subnet, size ) {
    new ManagedSubnet(
        name: subnet,
        subnet: subnet,
        segmentSize: size,
        minVlan: 2,
        maxVlan: 4095
    )
  }

  private static Pair<Integer,Long> pair( int tag, long index ) {
    Pair.pair( tag, index )
  }

}
