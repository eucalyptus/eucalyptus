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
