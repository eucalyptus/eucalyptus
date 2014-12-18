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

  /**
   * Re-run activity until the result evaluates to true.
   *
   * @param timeout The timeout in seconds
   * @param pollInterval The poll interval in seconds
   * @param activity The activity to perform
   * @return A promise for the activity (null on failure)
   */
  def <T> Promise<T> fixedPollWithTimeout(
      final Integer timeout,
      final Integer pollInterval,
      final Closure<Promise<T>> activity
  ) {
    doPollWithTimeout( timeout, { pollInterval }, activity )
  }

  /**
   * Re-run activity until the result evaluates to true.
   *
   * @param timeout The timeout in seconds
   * @param activity The activity to perform
   * @return A promise for the activity (null on failure)
   */
  def <T> Promise<T> exponentialPollWithTimeout(
      final Integer timeout,
      final Closure<Promise<T>> activity
  ) {
    exponentialPollWithTimeout( timeout, 10, 1.25, (int)TimeUnit.MINUTES.toSeconds( 10 ), activity )
  }

  /**
   * Re-run activity until the result evaluates to true.
   *
   * @param timeout The timeout in seconds
   * @param pollInitialInterval initial time between polls in seconds
   * @param pollBackoffCoefficient polling back-off coefficient
   * @param pollMaximumInterval The maximum time between polls
   * @param activity The activity to perform
   * @return A promise for the activity (null on failure)
   */
  def <T> Promise<T> exponentialPollWithTimeout(
      final Integer timeout,
      final Integer pollInitialInterval,
      final Double pollBackoffCoefficient,
      final Integer pollMaximumInterval,
      final Closure<Promise<T>> activity
  ) {
    doPollWithTimeout( timeout, { Integer invocation ->
      Math.min(
          (int) ( pollInitialInterval * Math.pow( pollBackoffCoefficient, invocation - 1 ) ),
          pollMaximumInterval )
    }, activity )
  }

  private <T> Promise<T> doPollWithTimeout(
      final Integer timeout,
      final Closure<Integer> intervalCalculator,
      final Closure<Promise<T>> activity
  ) {
    final Settable<T> result = new Settable<>( )
    final DoTry<Void> timer = (timeout?:0) > 0 ? operations.cancelableTimer( timeout ) : null
    final Promise<Void> timeoutPromise = timer?.result ?: new Settable<Void>( )
    doPollWithTimeoutRecursive( 1, result, timer, timeoutPromise, intervalCalculator, activity )
  }

  private <T> Promise<T> doPollWithTimeoutRecursive(
      final Integer invocation,
      final Settable<T> result,
      final DoTry<Void> timerTry,
      final Promise<Void> timeout,
      final Closure<Integer> intervalCalculator,
      final Closure<Promise<T>> activity
  ) {
    final Promise<T> activityPromise = activity.call( )
    final Integer nextPollInterval = intervalCalculator.call( invocation )
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
          doPollWithTimeoutRecursive( invocation + 1, result, timerTry, timeout, intervalCalculator, activity )
        }
        result
      }
      result
    }
  }
}
