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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Logs;
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
    final CheckedListenableFuture<P> secondFuture = Futures.newGenericeFuture( );
    final CheckedListenableFuture<P> firstFuture = Futures.newGenericeFuture( );
    
    Runnable first = new Runnable( ) {
      
      @Override
      public void run( ) {
        CheckedListenableFuture<P> res = firstCall.call( );
        
      }
    };
    Runnable second = new Runnable( ) {
      @Override
      public void run( ) {
        
      }
    };

    Threads.lookup( Empyrean.class, Futures.class, firstCall.getClass( ).getCanonicalName( ) ).execute( );
    
    firstFuture.addListener( new Runnable( ) {
      
      @Override
      public void run( ) {

      }
    }, Threads.lookup( Empyrean.class, Futures.class, firstCall.getClass( ).getCanonicalName( ) ) );
    
    Runnable secondFollower = new Runnable( ) {
      
      @Override
      public void run( ) {

      }
    };
    
    final Callable<CheckedListenableFuture<P>> chainingCallable = new Callable<CheckedListenableFuture<P>>( ) {
      
      @Override
      public CheckedListenableFuture<P> call( ) throws Exception {
        try {
          final Future<CheckedListenableFuture<P>> firstFuture = Threads.lookup( Empyrean.class, Futures.class, firstCall.getClass( ).getCanonicalName( ) ).submit( firstCall );
          Runnable firstFollower = new Runnable( ) {
            
            @Override
            public void run( ) {
              try {
                final CheckedListenableFuture<P> val = firstFuture.get( );
                if ( secondCall == null ) {
                  try {
                    resultFuture.set( val.get( ) );
                  } catch ( Exception ex ) {
                    resultFuture.setException( ex );
                  }
                } else {
                  try {
                    final Future<CheckedListenableFuture<P>> secondFuture = Threads.lookup( Empyrean.class, Futures.class,
                                                                                            secondCall.getClass( ).getCanonicalName( ) ).submit( secondCall );
                    
                    Runnable secondFollower = new Runnable( ) {
                      
                      @Override
                      public void run( ) {
                        CheckedListenableFuture<P> res = secondFuture.get( );
                        Runnable secondRunnable = new Runnable( ) {
                          
                          @Override
                          public void run( ) {
                            if ( !secondRes.isDone( ) ) {
                              LOG.error( "BUG BUG Executing listener for a future which is not yet done." );
                            }
                            Exception lastEx = null;
                            for ( int i = 0; i < 10; i++ ) {
                              try {
                                P res = secondFuture.get( 100, TimeUnit.MILLISECONDS );
                                resultFuture.set( secondFuture.get( ) );
                                return;
                              } catch ( final ExecutionException ex ) {
                                resultFuture.setException( ex );
                                return;
                              } catch ( final InterruptedException ex ) {
                                Automata.LOG.error( "BUG BUG BUG Interrupted calling .get() on a Future which isDone(): " + ex.getMessage( ), ex );
                                resultFuture.setException( ex );
                                Thread.currentThread( ).interrupt( );
                                return;
                              } catch ( TimeoutException ex ) {
                                Logs.exhaust( ).error( ex );
                                lastEx = ex;
                                continue;
                              }
                            }
                          }
                        };
                        
                      }
                    };
//                    secondFuture.addListener( , Threads.lookup( Empyrean.class, Futures.class, secondCall.getClass( ).getCanonicalName( ) ) );
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
          };
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
