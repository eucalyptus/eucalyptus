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

import java.util.concurrent.Executor;

/**
 * <p>An abstract base implementation of the listener support provided by
 * {@link ListenableFuture}. This class uses an {@link ExecutionList} to
 * guarantee that all registered listeners will be executed. Listener/Executor
 * pairs are stored in the execution list and executed in the order in which
 * they were added, but because of thread scheduling issues there is no
 * guarantee that the JVM will execute them in order. In addition, listeners
 * added after the task is complete will be executed immediately, even if some
 * previously added listeners have not yet been executed.
 * 
 * <p>This class uses the {@link AbstractFuture} class to implement the
 * {@code ListenableFuture} interface and simply delegates the
 * {@link #addListener(Runnable, Executor)} and {@link #done()} methods to it.
 * 
 * @author Sven Mawson
 * @since 1
 */
public abstract class AbstractListenableFuture<V>
    extends AbstractFuture<V> implements ListenableFuture<V> {

  // The execution list to hold our executors.
  private final ExecutionList executionList = new ExecutionList();

  /*
   * Adds a listener/executor pair to execution list to execute when this task
   * is completed.
   */
  public void addListener(Runnable listener, Executor exec) {
    executionList.add(listener, exec);
  }

  public void addListener(Runnable listener) {
    executionList.add(listener, MoreExecutors.sameThreadExecutor( ));
  }

  /*
   * Override the done method to execute the execution list.
   */
  @Override
  protected void done() {
    executionList.run();
  }
}
