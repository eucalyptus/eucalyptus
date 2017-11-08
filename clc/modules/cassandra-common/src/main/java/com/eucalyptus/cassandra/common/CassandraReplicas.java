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
