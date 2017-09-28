/*************************************************************************
 * Copyright 2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudformation.workflow

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
