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
