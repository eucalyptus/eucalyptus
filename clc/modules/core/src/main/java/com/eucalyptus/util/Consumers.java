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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class Consumers {

  private static final Consumer<Object> DROPPING = new Consumer<Object>( ) {
    @Override
    public void accept( final Object o ) { }
  };

  public static Consumer<Object> drop( ) {
    return DROPPING;
  }

  public static <T> Consumer<T> atomic( final AtomicReference<T> reference ) {
    return new Consumer<T>( ) {
      @Override
      public void accept( final T t ) {
        reference.set( t );
      }
    };
  }

  public static <T> Consumer<T> once( final Consumer<T> consumer ) {
    return new Consumer<T>( ) {
      private final AtomicBoolean accepted = new AtomicBoolean( false );

      @Override
      public void accept( final T t ) {
        if ( accepted.compareAndSet( false, true ) ) {
          consumer.accept( t );
        }
      }
    };
  }

  public static <T> Consumer<T> forRunnable( final Runnable runnable ) {
    return new Consumer<T>( ) {
      @Override
      public void accept( final T t ) {
        runnable.run( );
      }
    };
  }

  public static <T> Runnable partial( final Consumer<T> consumer, final T value ) {
    return new Runnable( ) {
      @Override
      public void run( ) {
        consumer.accept( value );
      }
    };
  }
}
