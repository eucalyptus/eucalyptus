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
package com.eucalyptus.cassandra.common;

import static com.eucalyptus.cassandra.common.CassandraReplicas.ReplicaOperation.*;
import java.util.function.BinaryOperator;

/**
 * Replica calculation strategies
 */
public enum CassandraReplicas {
  /**
   * One replica
   */
  ONE( 1, LIMIT ),

  /**
   * Two replicas or as close to two as possible
   */
  TWO( 2, LIMIT ),

  /**
   * Three replicas or as close to three as possible
   */
  THREE( 3, LIMIT ),

  /**
   * Replicas on the majority of nodes
   */
  MAJORITY( 50, PERCENT ),

  /**
   * Replicas on all available nodes
   */
  ALL( -1, THEIRS ),
  ;

  enum ReplicaOperation {
    LIMIT( Math::min ),
    PERCENT( ( r, n ) -> ( ( n * r ) / 100 ) + 1 ),
    THEIRS( ( r, n ) -> n  ),
    ;

    private final BinaryOperator<Integer> alg;

    ReplicaOperation( final BinaryOperator<Integer> alg ) {
      this.alg = alg;
    }
  }

  private final int replicas;
  private final ReplicaOperation op;

  CassandraReplicas( final int replicas, final ReplicaOperation op ) {
    this.replicas = replicas;
    this.op = op;
  }

  public int replicas( final int nodes ) {
    return op.alg.apply( replicas, nodes );
  }
}
