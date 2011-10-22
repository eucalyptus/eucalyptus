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
 * @author Sven Mawson
 * @author chris grzegorczyk <grze@eucalyptus.com> Adopted and repurposed to support callable chaining.
 */
package com.eucalyptus.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.google.common.util.concurrent.ExecutionList;
import static org.hamcrest.MatcherAssert.assertThat;
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
public abstract class AbstractListenableFuture<V> extends AbstractFuture<V> implements ListenableFuture<V> {
  private static Logger                           LOG       = Logger.getLogger( AbstractListenableFuture.class );
  protected final ConcurrentLinkedQueue<Runnable> listeners = new ConcurrentLinkedQueue<Runnable>( );
  private final AtomicBoolean                     finished  = new AtomicBoolean( false );
  private static final Runnable                   DONE      = new Runnable( ) {
                                                              @Override
                                                              public void run( ) {}
                                                            };
  
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
    final ExecPair<Object> pair = new ExecPair<Object>( listener, exec );
    this.add( pair );
  }
  
  @Override
  public void addListener( final Runnable listener ) {
    this.addListener( listener, Threads.lookup( Empyrean.class, AbstractListenableFuture.class ) );
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
    return this.addListener( listener, Threads.lookup( Empyrean.class, AbstractListenableFuture.class ) );
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
    
    ExecPair( final Callable callable, final ExecutorService executor ) {
      assertThat( "BUG: callable is null.", callable, notNullValue( ) );
      assertThat( "BUG: executor is null.", executor, notNullValue( ) );
      this.callable = callable;
      this.executor = executor;
    }
    
    ExecPair( final Runnable runnable, final ExecutorService executor ) {
      assertThat( "BUG: runnable is null.", runnable, notNullValue( ) );
      assertThat( "BUG: executor is null.", executor, notNullValue( ) );
      
      this.runnable = runnable;
      this.executor = executor;
    }
    
    @Override
    public void run( ) {
      try {
        if ( this.runnable != null ) {
          EventRecord.here( this.runnable.getClass( ), EventType.FUTURE, "run(" + this.runnable.toString( )
                                                                    + ")" ).exhaust( );
          this.executor.submit( this.runnable, null ).get( );
          this.future.set( null );
        } else {
          EventRecord.here( this.callable.getClass( ), EventType.FUTURE, "call(" + this.callable.toString( )
                                                                    + ")" ).exhaust( );
          this.future.set( this.executor.submit( this.callable ).get( ) );
        }
      } catch ( final InterruptedException ex ) {
        LOG.error( ex, ex );
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
