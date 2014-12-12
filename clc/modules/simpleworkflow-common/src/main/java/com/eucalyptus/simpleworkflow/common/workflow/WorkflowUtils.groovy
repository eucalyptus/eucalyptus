/*************************************************************************
 * Copyright 2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.simpleworkflow.common.workflow

import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.core.Settable
import com.netflix.glisten.DoTry
import com.netflix.glisten.WorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import java.util.concurrent.TimeUnit

/**
 *
 */
@CompileStatic(TypeCheckingMode.SKIP)
class WorkflowUtils {

  private final WorkflowOperations<?> operations

  WorkflowUtils( final WorkflowOperations<?> operations ) {
    this.operations = operations
  }

  def <T> Promise<T> exponentialPollWithTimeout(
      final Integer timeout,
      final Closure<Promise<T>> activity
  ) {
    final Settable<T> result = new Settable<>( )
    final DoTry<Void> timer = (timeout?:0) > 0 ? operations.cancelableTimer( timeout ) : null
    final Promise<Void> timeoutPromise = timer?.result ?: new Settable<Void>( )
    doExponentialPollWithTimeout( 1, result, timer, timeoutPromise, activity )
  }

  private <T> Promise<T> doExponentialPollWithTimeout(
      final Integer invocation,
      final Settable<T> result,
      final DoTry<Void> timerTry,
      final Promise<Void> timeout,
      final Closure<Promise<T>> activity
  ) {
    final Promise<T> activityPromise = activity.call( )
    final Integer nextPollInterval =
        (int) Math.min( (long) ( 10 * Math.pow( 1.25, invocation - 1 ) ), TimeUnit.MINUTES.toSeconds( 10 ) )
    operations.waitFor( operations.anyPromises( timeout, activityPromise  ) ) {
      if ( timeout.ready ) {
        result.set( null )
      } else if ( activityPromise.get( ) ) {
        if ( timerTry ) timerTry.cancel( null )
        result.set( activityPromise.get( ) )
      } else operations.waitFor( operations.anyPromises( timeout, operations.timer( nextPollInterval ) ) ) {
        if ( timeout.ready ) {
          result.set( null )
        } else {
          doExponentialPollWithTimeout( invocation + 1, result, timerTry, timeout, activity )
        }
        result
      }
      result
    }
  }
}
