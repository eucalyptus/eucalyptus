/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
