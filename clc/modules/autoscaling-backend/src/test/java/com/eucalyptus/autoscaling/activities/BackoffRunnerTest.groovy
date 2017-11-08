/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.autoscaling.activities

import static org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * 
 */
class BackoffRunnerTest {
  
  @Test
  void testRunsTask() {
    boolean taskEvaluated = false
    BackoffRunner runner = new BackoffRunner()
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key-runs", "group" ){
      @Override
      void runTask() {
        taskEvaluated = true
        success()
      }
    } )
    assertTrue( "Task evaluated", taskEvaluated )
  }

  @Test
  void testSimpleBackoff() {
    BackoffRunner runner = new BackoffRunner()

    boolean taskOneEvaluated = false
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key-simple-backoff", "group" ){
      @Override
      void runTask() {
        taskOneEvaluated = true
        failure()
      }
    } )
    assertTrue( "Task one evaluated", taskOneEvaluated )

    boolean taskTwoEvaluated = false
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key-simple-backoff", "group" ){
      @Override
      void runTask() {
        taskTwoEvaluated = true
        failure()
      }
    } )
    assertFalse( "Task two evaluated", taskTwoEvaluated )
  }

  @Test
  void testDifferentKeyNotSubjectToBackoff() {
    BackoffRunner runner = new BackoffRunner()

    boolean taskOneEvaluated = false
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key-different-1", "group" ){
      @Override
      void runTask() {
        taskOneEvaluated = true
        failure()
      }
    } )
    assertTrue( "Task one evaluated", taskOneEvaluated )

    boolean taskTwoEvaluated = false
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key-different-2", "group" ){
      @Override
      void runTask() {
        taskTwoEvaluated = true
        failure()
      }
    } )
    assertTrue( "Task two evaluated", taskTwoEvaluated )
  }

  @Test
  void testDifferentGroupNotSubjectToBackoff() {
    BackoffRunner runner = new BackoffRunner()

    boolean taskOneEvaluated = false
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key-different-group", "group1" ){
      @Override
      void runTask() {
        taskOneEvaluated = true
        failure()
      }
    } )
    assertTrue( "Task one evaluated", taskOneEvaluated )

    boolean taskTwoEvaluated = false
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key-different-group", "group2" ){
      @Override
      void runTask() {
        taskTwoEvaluated = true
        failure()
      }
    } )
    assertTrue( "Task two evaluated", taskTwoEvaluated )
  }

  @Test
  void testRunsAfterBackoffExpired() {
    long timestamp = System.currentTimeMillis()
    BackoffRunner runner = new BackoffRunner() {
      @Override
      protected long timestamp() {
        return timestamp;
      }
    }

    boolean taskOneEvaluated = false
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key-backoff-expiry", "group" ){
      @Override
      void runTask() {
        taskOneEvaluated = true
        failure()
      }
    } )
    assertTrue( "Task one evaluated", taskOneEvaluated )

    timestamp += TimeUnit.SECONDS.toMillis( 10 )
    
    boolean taskTwoEvaluated = false
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key-backoff-expiry", "group" ){
      @Override
      void runTask() {
        taskTwoEvaluated = true
        failure()
      }
    } )
    assertTrue( "Task two evaluated", taskTwoEvaluated )
  }

  @Test
  void testBackoffIncreases() {
    long timestamp = System.currentTimeMillis()
    BackoffRunner runner = new BackoffRunner() {
      @Override
      protected long timestamp() {
        return timestamp;
      }
    }

    boolean taskOneEvaluated = false
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key-backoff-increase", "group" ){
      @Override
      void runTask() {
        taskOneEvaluated = true
        failure()
      }
    } )
    assertTrue( "Task one evaluated", taskOneEvaluated )

    timestamp += TimeUnit.SECONDS.toMillis( 10 )

    boolean taskTwoEvaluated = false
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key-backoff-increase", "group" ){
      @Override
      void runTask() {
        taskTwoEvaluated = true
        failure()
      }
    } )
    assertTrue( "Task two evaluated", taskTwoEvaluated )

    timestamp += TimeUnit.SECONDS.toMillis( 10 )

    boolean taskThreeEvaluated = false
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key-backoff-increase", "group" ){
      @Override
      void runTask() {
        taskThreeEvaluated = true
        failure()
      }
    } )
    assertFalse( "Task three evaluated", taskThreeEvaluated )

    timestamp += TimeUnit.SECONDS.toMillis( 10 )

    boolean taskFourEvaluated = false
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key-backoff-increase", "group" ){
      @Override
      void runTask() {
        taskFourEvaluated = true
        failure()
      }
    } )
    assertTrue( "Task four evaluated", taskFourEvaluated )
  }

}
