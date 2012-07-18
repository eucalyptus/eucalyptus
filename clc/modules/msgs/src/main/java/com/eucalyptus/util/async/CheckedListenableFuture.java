/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.util.async;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.eucalyptus.util.concurrent.ListenableFuture;

public interface CheckedListenableFuture<R> extends ListenableFuture<R> {
  /**
   * Sets the future to having failed with the given exception. This exception
   * will be wrapped in an ExecutionException and thrown from the get methods.
   * This method will return {@code true} if the exception was successfully set,
   * or {@code false} if the future has already been set or cancelled.
   * 
   * @param t the exception the future should hold.
   * @return true if the exception was successfully set.
   */
  abstract boolean setException( Throwable exception );
  
  /**
   * Blocks until the task is complete or the timeout expires. Throws a
   * {@link TimeoutException} if the timer expires, otherwise behaves like
   * {@link #get()}.
   * 
   * @param timeout
   * @param unit
   * @return R response
   * @throws InterruptedException
   * @throws TimeoutException
   * @throws ExecutionException
   * @throws Exception
   */
  public abstract R get( long timeout, TimeUnit unit ) throws InterruptedException, TimeoutException, ExecutionException;
  
  /**
   * Blocks until {@link #complete(Object, Throwable, int)} has been
   * successfully called. Throws a {@link CancellationException} if the task was
   * cancelled, or a {@link ExecutionException} if the task completed with an
   * error.
   * 
   * @return R response
   * @throws Exception
   */
  public abstract R get( ) throws InterruptedException, ExecutionException;
  
  /**
   * Sets the value of this future. This method will return {@code true} if the
   * value was successfully set, or {@code false} if the future has already been
   * set or cancelled.
   * 
   * @param newValue the value the future should hold.
   * @return true if the value was successfully set.
   */
  abstract boolean set( R reply );
  
  /**
   * <p>
   * A ValueFuture is never considered in the running state, so the
   * {@code mayInterruptIfRunning} argument is ignored.
   */
  public abstract boolean cancel( boolean mayInterruptIfRunning );
  
  /**
   * @return
   */
  public abstract boolean isDone( );
  
  public abstract boolean isCanceled( );
}
