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
 *******************************************************************************
 * @author Sven Mawson
 * @author Nishant Thakkar
 * @author chris grzegorczyk <grze@eucalyptus.com> Adopted and repurposed to support callable chaining.
 */

package com.eucalyptus.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import com.eucalyptus.util.async.CheckedListenableFuture;

/**
 * <p>
 * This interface defines a future that has listeners attached to it, which is useful for asynchronous workflows. Each listener has an associated executor, and
 * is invoked using this executor once the {@code Future}'s computation is {@linkplain Future#isDone() complete}. The listener will be executed even if it is
 * added after the computation is complete.
 * 
 * <p>
 * Usage:
 * 
 * <pre>
 * {
 *   &#064;code
 *   final ListenableFuture&lt;?&gt; future = myService.async( myRequest );
 *   future.addListener( new Runnable( ) {
 *     public void run( ) {
 *       System.out.println( &quot;Operation Complete.&quot; );
 *       try {
 *         System.out.println( &quot;Result: &quot; + future.get( ) );
 *       } catch ( Exception e ) {
 *         System.out.println( &quot;Error: &quot; + e.message( ) );
 *       }
 *     }
 *   }, exec );
 * }
 * </pre>
 * 
 * @author Sven Mawson
 * @author Nishant Thakkar
 * @author chris grzegorczyk <grze@eucalyptus.com> Adopted and repurposed to support callable chaining.
 */
public interface ListenableFuture<V> extends Future<V> {
  /**
   * <p>
   * Adds a listener and executor to the ListenableFuture. The listener will be {@linkplain Executor#execute(Runnable) passed
   * to the executor} for execution when the {@code Future}'s computation is {@linkplain Future#isDone() complete}.
   * 
   * <p>
   * There is no guaranteed ordering of execution of listeners, they may get called in the order they were added and they may get called out of order, but any
   * listener added through this method is guaranteed to be called once the computation is complete.
   * 
   * @param listener the listener to run when the computation is complete.
   * @param exec the executor to run the listener in.
   * @throws NullPointerException if the executor or listener was null.
   * @throws RejectedExecutionException if we tried to execute the listener
   *           immediately but the executor rejected it.
   */
  void addListener( Runnable listener, ExecutorService exec );
  
  void addListener( Runnable listener );
  
  <T> CheckedListenableFuture<T> addListener( Callable<T> listener, ExecutorService exec );
  
  <T> CheckedListenableFuture<T> addListener( Callable<T> listener );
}
