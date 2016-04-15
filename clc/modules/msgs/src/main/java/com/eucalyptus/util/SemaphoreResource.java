/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.util;

import java.util.concurrent.Semaphore;

/**
 * Semaphore helper supporting try-with-resources use
 */
public class SemaphoreResource implements AutoCloseable {

  private final Semaphore semaphore;
  private final boolean acquired;

  private SemaphoreResource( final Semaphore semaphore,
                             final boolean acquired ) {
    this.semaphore = semaphore;
    this.acquired = acquired;
  }

  public static SemaphoreResource acquire( final Semaphore semaphore ) {
    semaphore.acquireUninterruptibly( );
    return new SemaphoreResource( semaphore, true );
  }

  public boolean isAcquired( ) {
    return acquired;
  }

  @Override
  public void close( ) {
    if ( isAcquired( ) ) {
      semaphore.release( );
    }
  }
}
