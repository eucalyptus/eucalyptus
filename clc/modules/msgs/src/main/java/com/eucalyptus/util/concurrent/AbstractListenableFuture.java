/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.records.Logs;
import com.google.common.base.Predicate;
import org.apache.log4j.Logger;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.google.common.util.concurrent.ExecutionList;

import javax.annotation.Nullable;

import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.notNullValue;

/**
 * <p>
 * An abstract base implementation of the listener support provided by {@link ListenableFuture}.
 * This class uses an {@link ExecutionList} to guarantee that all registered listeners will be
 * executed. Listener/Executor pairs are stored in the execution list and executed in the order in
 * which they were added, but because of thread scheduling issues there is no guarantee that the JVM
 * will execute them in order. In addition, listeners added after the task is complete will be
 * executed immediately, even if some previously added listeners have not yet been executed.
 * 
 * <p>
 * This class uses the {@link AbstractFuture} class to implement the {@code ListenableFuture}
 * interface and simply delegates the {@link #addListener(Runnable, ExecutorService)} and
 * {@link #done()} methods to it.
 * 
 * @author Sven Mawson
 * @author chris grzegorczyk <grze@eucalyptus.com> Adopted and repurposed to support callable
 *         chaining.
 */
@ConfigurableClass( root = "bootstrap.async",
                    description = "Parameters controlling the asynchronous futures and executors." )
public abstract class AbstractListenableFuture<V> extends AbstractFuture<V> implements ListenableFuture<V> {
  private static Logger                             LOG                              = Logger.getLogger( AbstractListenableFuture.class );
  protected final ConcurrentLinkedQueue<Runnable>   listeners                        = new ConcurrentLinkedQueue<Runnable>( );
  private final AtomicBoolean                       finished                         = new AtomicBoolean( false );
  private static final Runnable                     DONE                             = new Runnable( ) {
                                                                                       @Override
                                                                                       public void run( ) {}
                                                                                     };
  private static final ExecutorService              executor                         = Executors.newCachedThreadPool( new ThreadFactory( ) {
                                                                                       @Override
                                                                                       public Thread newThread( Runnable r ) {
                                                                                         Thread s = Executors.defaultThreadFactory( )
                                                                                                             .newThread( r );
                                                                                         s.setName( "AbstractListenableFuture: " + r );
                                                                                         return s;
                                                                                       }
                                                                                     } );
  @ConfigurableField( description = "Number of seconds a future listener can execute before a debug message is logged.",
                      type = ConfigurableFieldType.PRIVATE )
  public static Long                                FUTURE_LISTENER_DEBUG_LIMIT_SECS = 30L;
  @ConfigurableField( description = "Number of seconds a future listener can execute before an info message is logged.",
                      type = ConfigurableFieldType.PRIVATE )
  public static Long                                FUTURE_LISTENER_INFO_LIMIT_SECS  = 60L;
  @ConfigurableField( description = "Number of seconds a future listener can execute before an error message is logged.",
                      type = ConfigurableFieldType.PRIVATE )
  public static Long                                FUTURE_LISTENER_ERROR_LIMIT_SECS = 120L;
  @ConfigurableField( description = "Number of seconds a future listener's executor waits to get() per call.",
                      type = ConfigurableFieldType.PRIVATE )
  public static Long                                FUTURE_LISTENER_GET_TIMEOUT      = 30L;
  @ConfigurableField( description = "Total number of seconds a future listener's executor waits to get().",
                      type = ConfigurableFieldType.PRIVATE )
  public static Integer                             FUTURE_LISTENER_GET_RETRIES      = 8;
  private static final Predicate<StackTraceElement> filter                           = Threads.filterStackByQualifiedName( "com.eucalyptus.*" );
  private final String                              startingStack;

  protected AbstractListenableFuture() {
    this.startingStack = Threads.currentStackString();
  }

  protected <T> void add( final ExecPair<T> pair ) {
    this.listeners.add( pair );
    if ( this.finished.get( ) ) {
      EventRecord.here( pair.getClass( ), EventType.FUTURE, "run(" + pair.toString( )
                                                            + ")" ).exhaust( );
      this.listeners.remove( pair );
      pair.run( );
    } else {
      EventRecord.here( pair.getClass( ), EventType.FUTURE, "add(" + pair.toString( )
                                                            + ")" ).exhaust( );
    }
  }
  
  @Override
  public void addListener( final Runnable listener, final ExecutorService exec ) {
    final ExecPair<Object> pair = new ExecPair<Object>( new Callable() {
      @Override
      public Object call() throws Exception {
        listener.run();
        return null;
      }

      @Override
      public String toString() {
        return "ListenableFuture.ExecPair.listener " + listener + " [" + Thread.currentThread().getStackTrace()[2] + "]";
      }
    }, exec );
    this.add( pair );
  }
  
  @Override
  public void addListener( final Runnable listener ) {
    this.addListener( listener, executor );
  }
  
  /**
   * @see com.eucalyptus.util.concurrent.ListenableFuture#addListener(java.util.concurrent.Callable,
   *      ExecutorService)
   */
  @Override
  public <T> CheckedListenableFuture<T> addListener( final Callable<T> listener, final ExecutorService executor ) {
    final ExecPair<T> pair = new ExecPair<T>( listener, executor );
    this.add( pair );
    return pair.getFuture( );
  }
  
  /**
   * @see com.eucalyptus.util.concurrent.ListenableFuture#addListener(java.util.concurrent.Callable)
   */
  @Override
  public <T> CheckedListenableFuture<T> addListener( final Callable<T> listener ) {
    return this.addListener( listener, executor );
  }
  
  @Override
  protected void done( ) {
    this.listeners.add( DONE );
    if ( this.finished.compareAndSet( false, true ) ) {
      while ( this.listeners.peek( ) != DONE ) {
        this.listeners.poll( ).run( );
      }
    }
  }
  
  @Override
  public boolean set( final V value ) {
    return super.set( value );
  }
  
  @Override
  public boolean setException( final Throwable throwable ) {
    return super.setException( throwable );
  }

  class ExecPair<C> implements Runnable {
    private Callable<C>                      callable;
    private Runnable                         runnable;
    private final CheckedListenableFuture<C> future = Futures.newGenericeFuture( );
    private final ExecutorService            executor;

    ExecPair( final Callable callable, final ExecutorService executor  ) {
      checkParam( "BUG: callable is null.", callable, notNullValue() );
      checkParam( "BUG: executor is null.", executor, notNullValue() );
      this.callable = callable;
      this.executor = executor;
    }

    ExecPair( final Callable<C> callable ) {
      this( callable, AbstractListenableFuture.executor );
    }

    private static final String message = "Listener failed to execute within the time limit (%d): %s using executor %s";
    @Override
    public void run( ) {
      try {
        final long startTime = System.currentTimeMillis();
        Predicate<Callable<C>> timeoutLogger = new Predicate<Callable<C>>() {
          @Override
          public boolean apply( @Nullable Callable<C> input ) {
            try {
              long elapsed = System.currentTimeMillis() - startTime;
              long seconds = TimeUnit.MILLISECONDS.toSeconds( elapsed );
              String details = ExecPair.this.callable.toString() + " [" + Threads.filteredStack( filter ).iterator().next() + "]";
              if ( seconds > FUTURE_LISTENER_DEBUG_LIMIT_SECS ) {
                LOG.debug( String.format( message, FUTURE_LISTENER_DEBUG_LIMIT_SECS, details, executor.toString() ) );
                return true;
              } else if ( seconds > FUTURE_LISTENER_INFO_LIMIT_SECS ) {
                LOG.info( String.format( message, FUTURE_LISTENER_INFO_LIMIT_SECS, details, executor.toString() ) );
                return true;
              } else if ( seconds > FUTURE_LISTENER_ERROR_LIMIT_SECS ) {
                LOG.error( String.format( message, FUTURE_LISTENER_ERROR_LIMIT_SECS, details, executor.toString() ) );
                return true;
              }
              LOG.trace( String.format( "Listener still within time limit (%d): %s using executor %s", FUTURE_LISTENER_ERROR_LIMIT_SECS, details, executor.toString() ) );
            } catch ( Exception e ) {
              LOG.error( e );
            }
            return false;
          }
        };
        Future<C> execFuture = this.executor.submit( this.callable );
        for ( int iterations = 0; ( !Bootstrap.isOperational( ) && !Bootstrap.isShuttingDown( ) )
                                  || ( iterations < FUTURE_LISTENER_GET_RETRIES ); iterations++ ) {
          try {
            C outcome = execFuture.get( FUTURE_LISTENER_GET_TIMEOUT, TimeUnit.SECONDS );
            this.future.set( outcome );
            break;
          } catch ( TimeoutException e ) {
            continue;
          } finally {
            if (timeoutLogger.apply(this.callable)) {
              Logs.exhaust().debug( "Intial Stack: \n" + AbstractListenableFuture.this.startingStack );
              Logs.exhaust().debug( "Current Stack: \n" + Threads.currentStackString() );
            }
          }
        }
        if ( !this.future.isDone() ) {
          String message = "Failed to invoke listener for " + AbstractListenableFuture.this + " of type: " + (this.runnable != null ? this.runnable : this.callable);
          LOG.error( message );
          LOG.error( startingStack );
          throw new TimeoutException( message );
        }
      } catch ( final InterruptedException ex ) {
        LOG.error( ex );
        Thread.currentThread( ).interrupt( );
        this.future.setException( ex );
      } catch ( final ExecutionException ex ) {
        LOG.error( ex, ex );
        this.future.setException( ex.getCause( ) );
      } catch ( final Exception ex ) {
        LOG.error( ex, ex );
        this.future.setException( ex.getCause( ) );
      }
    }
    
    CheckedListenableFuture<C> getFuture( ) {
      return this.future;
    }
    
    protected ExecutorService getExecutor( ) {
      return this.executor;
    }
    
    @Override
    public String toString( ) {
      return String.format( "ExecPair:callable=%s:runnable=%s", this.callable, this.runnable );
    }
    
  }
  
}
