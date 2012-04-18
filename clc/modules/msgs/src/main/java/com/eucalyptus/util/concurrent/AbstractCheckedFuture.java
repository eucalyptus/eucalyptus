/*******************************************************************************
 * Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian Dr., Goleta, CA 93101
 * USA or visit <http://www.eucalyptus.com/licenses/> if you need additional
 * information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 *
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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
