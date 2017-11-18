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

import java.io.Closeable;
import java.util.List;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 *
 */
public class IO {

  /**
   * Close a closable, returning any error.
   *
   * @param closeable The closeable to close
   * @return The optional error
   */
  public static Optional<Throwable> close( final Closeable closeable ) {
    Optional<Throwable> error = Optional.absent( );
    if ( closeable != null ) try {
      closeable.close( );
    } catch ( Throwable e ) {
      error = Optional.of( e );
    }
    return error;
  }

  /**
   * Close given closables, return errors.
   *
   * @param closeables The closeables to close
   * @return The error list (same length as closeables)
   * @see Optional#presentInstances(Iterable)
   */
  public static Iterable<Optional<Throwable>> close( final Iterable<? extends Closeable> closeables ) {
    final List<Optional<Throwable>> results = Lists.newArrayList( );
    if ( closeables != null ) for ( final Closeable closeable : closeables ) {
      results.add( close( closeable ) );
    }
    return results;
  }
}
