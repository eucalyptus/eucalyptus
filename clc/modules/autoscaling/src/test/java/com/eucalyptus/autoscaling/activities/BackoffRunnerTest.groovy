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
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key", "group" ){
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
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key", "group" ){
      @Override
      void runTask() {
        taskOneEvaluated = true
        failure()
      }
    } )
    assertTrue( "Task one evaluated", taskOneEvaluated )

    boolean taskTwoEvaluated = false
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key", "group" ){
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
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key1", "group" ){
      @Override
      void runTask() {
        taskOneEvaluated = true
        failure()
      }
    } )
    assertTrue( "Task one evaluated", taskOneEvaluated )

    boolean taskTwoEvaluated = false
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key2", "group" ){
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
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key", "group1" ){
      @Override
      void runTask() {
        taskOneEvaluated = true
        failure()
      }
    } )
    assertTrue( "Task one evaluated", taskOneEvaluated )

    boolean taskTwoEvaluated = false
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key", "group2" ){
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
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key", "group" ){
      @Override
      void runTask() {
        taskOneEvaluated = true
        failure()
      }
    } )
    assertTrue( "Task one evaluated", taskOneEvaluated )

    timestamp += TimeUnit.SECONDS.toMillis( 10 )
    
    boolean taskTwoEvaluated = false
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key", "group" ){
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
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key", "group" ){
      @Override
      void runTask() {
        taskOneEvaluated = true
        failure()
      }
    } )
    assertTrue( "Task one evaluated", taskOneEvaluated )

    timestamp += TimeUnit.SECONDS.toMillis( 10 )

    boolean taskTwoEvaluated = false
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key", "group" ){
      @Override
      void runTask() {
        taskTwoEvaluated = true
        failure()
      }
    } )
    assertTrue( "Task two evaluated", taskTwoEvaluated )

    timestamp += TimeUnit.SECONDS.toMillis( 10 )

    boolean taskThreeEvaluated = false
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key", "group" ){
      @Override
      void runTask() {
        taskThreeEvaluated = true
        failure()
      }
    } )
    assertFalse( "Task three evaluated", taskThreeEvaluated )

    timestamp += TimeUnit.SECONDS.toMillis( 10 )

    boolean taskFourEvaluated = false
    runner.runTask( new BackoffRunner.TaskWithBackOff( "key", "group" ){
      @Override
      void runTask() {
        taskFourEvaluated = true
        failure()
      }
    } )
    assertTrue( "Task four evaluated", taskFourEvaluated )
  }

}
