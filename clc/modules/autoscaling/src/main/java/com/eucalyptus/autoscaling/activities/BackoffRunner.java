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
package com.eucalyptus.autoscaling.activities;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.autoscaling.config.AutoScalingConfiguration;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.google.common.collect.Maps;

/**
 * Runs tasks with duplication checks and back-off
 */
public class BackoffRunner {

  private static final Logger logger = Logger.getLogger( BackoffRunner.class );
  
  private static final ConcurrentMap<String,TaskInfo> tasksInProgress = Maps.newConcurrentMap();

  private static final BackoffRunner instance = new BackoffRunner();
  
  static BackoffRunner getInstance() {
    return instance; 
  }
  
  BackoffRunner(){
  }
  
  boolean taskInProgress( final String uniqueKey ) {
    final long timestamp = timestamp();
    final TaskInfo taskInfo = tasksInProgress.get( uniqueKey );
    return taskInfo != null && !taskInfo.isDone( timestamp );
  }
  
  boolean runTask( final TaskWithBackOff task ) {
    boolean run = doRunTask( task, timestamp() );
    if ( run ) {
      task.runTask();
    } else {
      logger.info( "Not running task " + task );
    }
    return run;
  }

  protected long timestamp() {
    return System.currentTimeMillis();
  }

  private static long getTaskTimeout() {
    return AutoScalingConfiguration.getActivityTimeoutMillis();
  }

  private static long getMaxBackoff() {
    return AutoScalingConfiguration.getActivityMaxBackoffMillis();
  }

  private static long getInitialBackoff() {
    return AutoScalingConfiguration.getActivityInitialBackoffMillis();
  }

  private static boolean doRunTask( final TaskWithBackOff task, final long timestamp ) {
    boolean run = false;
    final TaskInfo previous =
        tasksInProgress.putIfAbsent( task.getUniqueKey(), task.info( new TaskInfo( task, timestamp, 0 ) ) );
    if ( previous == null ) {
      run = true;
    } else if ( previous.canFollow( timestamp, task.getBackoffGroup() ) &&
        tasksInProgress.remove( task.getUniqueKey(), previous ) &&
        tasksInProgress.putIfAbsent(
            task.getUniqueKey(),
            task.info( new TaskInfo( task, timestamp, previous.getNextFailureCount( task.getBackoffGroup() ) ) ) )==null ) {
      run = true;
    }
    return run;
  }

  static abstract class TaskWithBackOff {
    private final String uniqueKey;
    private final String backoffGroup;
    private final CheckedListenableFuture<Boolean> future;
    private volatile TaskInfo taskInfo;

    TaskWithBackOff( final String uniqueKey, 
                     final String backoffGroup ) {
      this.uniqueKey = uniqueKey;
      this.backoffGroup = backoffGroup;
      this.future = Futures.newGenericeFuture();
    }

    TaskInfo info( final TaskInfo taskInfo ) {
      return this.taskInfo = taskInfo;  
    }

    @Nullable
    TaskWithBackOff onSuccess() {
      return null;
    }

    final String getUniqueKey() {
      return uniqueKey;
    }

    final String getBackoffGroup() {
      return backoffGroup;
    }
    
    private Future<Boolean> getFuture() {
      return future;
    }
    
    abstract void runTask( );
    
    final void success() {
      final TaskWithBackOff onSuccess = onSuccess();
      if ( onSuccess != null ) {
        final boolean run = getUniqueKey().equals( onSuccess.getUniqueKey() ) ?
            tasksInProgress.replace( getUniqueKey(), taskInfo, onSuccess.info( new TaskInfo( onSuccess, System.currentTimeMillis(), 0 ) ) ) :
            doRunTask( onSuccess, System.currentTimeMillis() );
        if ( !run ) {
          logger.info( "Unable to create activity: " + onSuccess.getBackoffGroup() + " for " + onSuccess.getUniqueKey() );
        } else {
          future.set( true );
          onSuccess.runTask();
        }
      }
      if ( !future.isDone() ) {
        future.set( true );
      }
    }
    
    final void failure() {
      future.set(false);
    }
    
    public String toString() {
      return uniqueKey + " " + backoffGroup;
    }
  }
  
  private final static class TaskInfo {
    private final String group;
    private final long created;
    private final Future<Boolean> resultFuture;
    private final int failureCount;
    
    private TaskInfo( final TaskWithBackOff task, 
                      final long created,
                      final int failureCount ) {
      this.group = task.getBackoffGroup();
      this.created = created;
      this.resultFuture = task.getFuture();
      this.failureCount = failureCount;
    }

    private boolean canFollow( final long timestamp,
                               final String group ) {
      return 
          isSuccess() ||
          ( isDone( timestamp ) && 
            isBackoffExpired( timestamp, group ) );
          
    }
    
    private boolean isBackoffExpired( final long timestamp,
                                      final String group  ) {
      return !this.group.equals( group ) || // different groups not subject to back off
          timestamp - created > calculateBackoff();
    }
    
    private long calculateBackoff() {
      long backoff = getInitialBackoff();
      for ( int i=0; i<failureCount && backoff < getMaxBackoff() ; i++ ) {
        backoff *= 2;        
      }
      return Math.min( backoff, getMaxBackoff() );
    }
    
    private int getNextFailureCount( final String group ) {
      return this.group.equals( group ) && !isSuccess() ?
          failureCount + 1 :
          0;
    }
    
    private boolean isDone( final long timestamp ) {
      return 
          isTimedOut( timestamp ) ||
          isComplete();
    }
    
    private boolean isTimedOut( final long timestamp ) {
      return timestamp - created > getTaskTimeout();
    }

    private boolean isSuccess() {
      try {
        return
            resultFuture != null &&
            resultFuture.isDone() &&
            !resultFuture.isCancelled() &&
            resultFuture.get();
      } catch ( final ExecutionException e ) {
        return false;
      } catch ( final InterruptedException e ) {
        return false;
      }
    }

    private boolean isComplete() {
      return
          resultFuture != null && 
          resultFuture.isDone();
    }
  }
}
