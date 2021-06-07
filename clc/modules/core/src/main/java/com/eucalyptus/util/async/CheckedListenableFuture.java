/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
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
