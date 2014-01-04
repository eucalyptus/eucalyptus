/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.util

import groovy.transform.CompileStatic

import java.util.concurrent.locks.Lock

/**
 * Lock helper supporting try-with-resources and Groovy approaches
 */
@CompileStatic
class LockResource implements AutoCloseable {

  private final Lock lock

  private LockResource( final Lock lock ) {
    this.lock = lock
  }

  static LockResource lock( Lock lock ) {
    lock.lock()
    new LockResource( lock );
  }
  
  static <V> V withLock( Lock lock, Closure<V> closure ) {
    lock.lock( )
    try {
      closure.call( )
    } finally {
      lock.unlock( )
    }
  }
  
  @Override
  void close( ) {
    lock.unlock( )
  }
}
