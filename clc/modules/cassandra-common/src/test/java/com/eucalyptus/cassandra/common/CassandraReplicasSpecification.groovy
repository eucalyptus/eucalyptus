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
package com.eucalyptus.cassandra.common

import spock.lang.Specification

/**
 *
 */
class CassandraReplicasSpecification extends Specification {

  def 'should have expected constant replica values over limit'() {
    expect: 'enums have expected values for input over limit'
    replicas.replicas( nodes ) == factor

    where:
    replicas                | nodes | factor
    CassandraReplicas.ONE   |     1 |      1
    CassandraReplicas.ONE   |   101 |      1
    CassandraReplicas.TWO   |     2 |      2
    CassandraReplicas.THREE |     3 |      3
    CassandraReplicas.THREE |    99 |      3
    CassandraReplicas.THREE |  9999 |      3
  }

  def 'should have expected nodes values below limit'() {
    expect: 'enums have expected values for nodes less than limit'
    replicas.replicas( nodes ) == factor

    where:
    replicas                | nodes | factor
    CassandraReplicas.TWO   |     1 |      1
    CassandraReplicas.THREE |     1 |      1
    CassandraReplicas.THREE |     2 |      2
  }

  def 'should have expected constant node values'() {
    expect: 'enums have expected values for any input'
    replicas.replicas( nodes ) == nodes

    where:
    replicas                | nodes
    CassandraReplicas.ALL   |     1
    CassandraReplicas.ALL   |     4
    CassandraReplicas.ALL   |    11
    CassandraReplicas.ALL   |   647
  }

  def 'should have expected percentage value'() {
    expect: 'enums have expected correct values derived from input'
    replicas.replicas( nodes ) == factor

    where:
    replicas                   | nodes | factor
    CassandraReplicas.MAJORITY |     1 |      1
    CassandraReplicas.MAJORITY |     2 |      2
    CassandraReplicas.MAJORITY |     3 |      2
    CassandraReplicas.MAJORITY |     4 |      3
    CassandraReplicas.MAJORITY |     5 |      3
    CassandraReplicas.MAJORITY |     6 |      4
    CassandraReplicas.MAJORITY |     7 |      4
    CassandraReplicas.MAJORITY |     8 |      5
    CassandraReplicas.MAJORITY |     9 |      5
    CassandraReplicas.MAJORITY |    13 |      7
    CassandraReplicas.MAJORITY |    26 |     14
    CassandraReplicas.MAJORITY |    75 |     38
  }

}
