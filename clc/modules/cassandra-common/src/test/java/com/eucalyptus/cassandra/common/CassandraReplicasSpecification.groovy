/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
