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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@code CheckedFuture} is an extension of {@link Future} that includes
 * versions of the {@code get} methods that can throw a checked exception and
 * allows listeners to be attached to the future.  This makes it easier to
 * create a future that executes logic which can throw an exception.
 * 
 * <p>Implementations of this interface must adapt the exceptions thrown by
 * {@code Future#get()}: {@link CancellationException},
 * {@link ExecutionException} and {@link InterruptedException} into the type
 * specified by the {@code E} type parameter.
 * 
 * <p>This interface also extends the ListenableFuture interface to allow
 * listeners to be added. This allows the future to be used as a normal
 * {@link Future} or as an asynchronous callback mechanism as needed. This
 * allows multiple callbacks to be registered for a particular task, and the
 * future will guarantee execution of all listeners when the task completes.
 * 
 * @author Sven Mawson
 * @since 1
 */
public interface CheckedFuture<V, E extends Exception>
    extends ListenableFuture<V> {

  /**
   * Exception checking version of {@link Future#get()} that will translate
   * {@link InterruptedException}, {@link CancellationException} and
   * {@link ExecutionException} into application-specific exceptions.
   * 
   * @return the result of executing the future.
   * @throws E on interruption, cancellation or execution exceptions.
   */
  V checkedGet() throws E;
  
  /**
   * Exception checking version of {@link Future#get(long, TimeUnit)} that will
   * translate {@link InterruptedException}, {@link CancellationException} and
   * {@link ExecutionException} into application-specific exceptions.  On
   * timeout this method throws a normal {@link TimeoutException}.
   * 
   * @return the result of executing the future.
   * @throws TimeoutException if retrieving the result timed out.
   * @throws E on interruption, cancellation or execution exceptions.
   */
  V checkedGet(long timeout, TimeUnit unit) throws TimeoutException, E;
}
