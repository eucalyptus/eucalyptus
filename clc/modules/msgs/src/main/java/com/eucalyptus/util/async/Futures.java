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
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.util.async;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.concurrent.GenericCheckedListenableFuture;
import com.eucalyptus.util.concurrent.ListenableFuture;
import com.eucalyptus.util.fsm.Automata;
import com.google.common.util.concurrent.ExecutionList;
import com.google.common.util.concurrent.ForwardingFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class Futures {
  private static Logger LOG = Logger.getLogger( Futures.class );
  
  public static <T> CheckedListenableFuture<T> newGenericeFuture( ) {
    return new GenericCheckedListenableFuture<T>( );
  }
  
  public static <T> CheckedListenableFuture<T> predestinedFuture( final T initValue ) {
    return new GenericCheckedListenableFuture<T>( ) {
      {
        this.set( initValue );
      }
    };
  }
  
  public static <T> CheckedListenableFuture<T> predestinedFailedFuture( final Throwable exValue ) {
    return new GenericCheckedListenableFuture<T>( ) {
      {
        this.setException( exValue );
      }
    };
  }
  
  /**
   * Returns a new {@code Callable} which will execute {@code firstCall} and, if it succeeds,
   * {@code secondCall} in sequence. The resulting {@code resultFuture} will return one of:
   * <ol>
   * <li>{@link Future#get()} returns the result of {@code secondCall}'s future result.</li>
   * <li>{@link Future#get()} throws the exception which caused {@code firstCall} to fail -- in this
   * case {@code secondCall} is not executed.</li>
   * <li>{@link Future#get()} throws the exception which caused {@code secondCall} to fail.</li>
   * </ol>
   * 
   * @param <P>
   * @param firstCall
   * @param secondCall
   * @return resultFuture
   */
  public static <P> Callable<CheckedListenableFuture<P>> combine( final Callable<CheckedListenableFuture<P>> firstCall, final Callable<CheckedListenableFuture<P>> secondCall ) {
    final CheckedListenableFuture<P> resultFuture = Futures.newGenericeFuture( );
    final Callable<CheckedListenableFuture<P>> chainingCallable = new Callable<CheckedListenableFuture<P>>( ) {
      
      @Override
      public CheckedListenableFuture<P> call( ) throws Exception {
        try {
          final CheckedListenableFuture<P> firstFuture = firstCall.call( );
          firstFuture.addListener( new Runnable( ) {
            
            @Override
            public void run( ) {
              try {
                final P val = firstFuture.get( );
                if ( secondCall == null ) {
                  resultFuture.set( val );
                } else {
                  try {
                    final CheckedListenableFuture<P> secondFuture = secondCall.call( );
                    secondFuture.addListener( new Runnable( ) {
                      
                      @Override
                      public void run( ) {
                        try {
                          resultFuture.set( secondFuture.get( ) );
                        } catch ( final ExecutionException ex ) {
                          resultFuture.setException( ex );
                        } catch ( final InterruptedException ex ) {
                          Automata.LOG.error( "BUG BUG BUG Interrupted calling .get() on a Future which isDone(): " + ex.getMessage( ), ex );
                          Thread.currentThread( ).interrupt( );
                          resultFuture.setException( ex );
                        }
                      }
                    } );
                  } catch ( final Exception ex ) {
                    resultFuture.setException( ex );
                  }
                }
                
              } catch ( final ExecutionException ex ) {
                resultFuture.setException( ex.getCause( ) );
              } catch ( final InterruptedException ex ) {
                Automata.LOG.error( "BUG BUG BUG Interrupted calling .get() on a Future which isDone(): " + ex.getMessage( ), ex );
                resultFuture.setException( ex );
              }
            }
          } );
        } catch ( final Exception ex ) {
          Automata.LOG.error( ex, ex );
          resultFuture.setException( ex );
        }
        return resultFuture;
      }
    };
    return chainingCallable;
  }
  
  public static <P> Callable<CheckedListenableFuture<P>> sequence( final Callable<CheckedListenableFuture<P>>... callables ) {
    assertThat( callables, not( emptyArray( ) ) );
    if ( callables.length == 1 ) {
      return callables[0];
    } else if ( callables.length == 2 ) {
      return Futures.combine( callables[0], callables[1] );
    } else {
      final Callable<CheckedListenableFuture<P>>[] nextCallables = Arrays.copyOfRange( callables, 1, callables.length );
      nextCallables[0] = Futures.combine( callables[0], callables[1] );
      return sequence( nextCallables );
    }
  }
  
}
