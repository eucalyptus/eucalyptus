/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import groovy.lang.Closure;

/**
 * Lock helper supporting try-with-resources and Groovy approaches
 */
public class LockResource implements AutoCloseable {

  private final Lock lock;
  private boolean locked;

  private LockResource( final Lock lock, final boolean locked ) {
    this.lock = lock;
    this.locked = locked;
  }

  public static LockResource lock( Lock lock ) {
    lock.lock( );
    return new LockResource( lock, true );
  }

  /**
   * Try to acquire lock for the given time.
   *
   * <p>WARNING: Caller must check if lock was acquired</p>
   *
   * @see #isLocked()
   */
  public static LockResource tryLock( Lock lock, long time, TimeUnit unit ) throws InterruptedException {
    boolean locked = lock.tryLock( time, unit );
    return new LockResource( lock, locked );
  }

  /**
   * Try to acquire lock.
   *
   * <p>WARNING: Caller must check if lock was acquired</p>
   *
   * @see #isLocked()
   */
  public static LockResource tryLock( Lock lock ) {
    boolean locked = lock.tryLock( );
    return new LockResource( lock, locked );
  }

  public static <V> V withLock( Lock lock, Closure<V> closure ) {
    lock.lock( );
    try {
      return closure.call( );
    } finally {
      lock.unlock( );
    }

  }

  public boolean isLocked( ) {
    return locked;
  }

  @Override
  public void close( ) {
    if ( isLocked( ) ) {
      lock.unlock( );
      locked = false;
    }
  }
}
