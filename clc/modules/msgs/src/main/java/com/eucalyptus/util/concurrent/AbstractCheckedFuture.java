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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Copyright (C) 2009 Google Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *   or implied.  See the License for the specific language governing
 *   permissions and limitations under the License.
 ************************************************************************/

package com.eucalyptus.util.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A delegating wrapper around a {@link ListenableFuture} that adds support for
 * the {@link #checkedGet()} and {@link #checkedGet(long, TimeUnit)} methods.
 * 
 * @author Sven Mawson
 * @since 1
 */
public abstract class AbstractCheckedFuture<V, E extends Exception>
    implements CheckedFuture<V, E> {

  /** The delegate, used to pass along all our methods. */
  protected final ListenableFuture<V> delegate;
  
  /**
   * Constructs an {@code AbstractCheckedFuture} that wraps a delegate.
   */
  protected AbstractCheckedFuture(ListenableFuture<V> delegate) {
    this.delegate = delegate;
  }

  /**
   * Translate from an {@link InterruptedException},
   * {@link CancellationException} or {@link ExecutionException} to an exception
   * of type {@code E}.  Subclasses must implement the mapping themselves.
   * 
   * The {@code e} parameter can be an instance of {@link InterruptedException},
   * {@link CancellationException}, or {@link ExecutionException}.
   */
  protected abstract E mapException(Exception e);
  
  /*
   * Just like get but maps the exceptions into appropriate application-specific
   * exceptions.
   */
  public V checkedGet() throws E {
    try {
      return get();
    } catch (InterruptedException e) {
      cancel(true);
      throw mapException(e);
    } catch (CancellationException e) {
      throw mapException(e);
    } catch (java.util.concurrent.ExecutionException e) {
      throw mapException(e);
    }
  }

  /*
   * The timed version of checkedGet maps the interrupted, cancellation or
   * execution exceptions exactly the same as the untimed version does.
   */
  public V checkedGet(long timeout, TimeUnit unit) throws TimeoutException, E {
    try {
      return get(timeout, unit);
    } catch (InterruptedException e) {
      cancel(true);
      throw mapException(e);
    } catch (CancellationException e) {
      throw mapException(e);
    } catch (java.util.concurrent.ExecutionException e) {
      throw mapException(e);
    }
  }

  // Delegate methods for methods defined in the ListenableFuture interface.
  
  public boolean cancel(boolean mayInterruptIfRunning) {
    return delegate.cancel(mayInterruptIfRunning);
  }
  
  public boolean isCancelled() {
    return delegate.isCancelled();
  }
  
  public boolean isDone() {
    return delegate.isDone();
  }
  
  public V get() throws InterruptedException, java.util.concurrent.ExecutionException {
    return delegate.get();
  }
  
  public V get(long timeout, TimeUnit unit) throws InterruptedException,
      java.util.concurrent.ExecutionException, TimeoutException {
    return delegate.get(timeout, unit);
  }
  
  public void addListener(Runnable listener, ExecutorService exec) {
    delegate.addListener(listener, exec);
  }

  public void addListener( Runnable listener ) {
    this.delegate.addListener( listener );
  }
}
