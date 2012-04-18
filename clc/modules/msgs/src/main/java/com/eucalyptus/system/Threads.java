/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
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
package com.eucalyptus.system;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.jgroups.util.ThreadFactory;
import com.eucalyptus.bootstrap.OrderedShutdown;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.LinkedTransferQueue;
import com.eucalyptus.util.concurrent.GenericCheckedListenableFuture;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

/**
 * TODO:GRZE: wrong package should be .util
 */
public class Threads {
  private static Logger                                  LOG               = Logger.getLogger( Threads.class );
  private final static String                            PREFIX            = "Eucalyptus.";
  private final static Integer                           NUM_QUEUE_WORKERS = 32;                                          //TODO:GRZE: discover on per-service basis.;
  private final static AtomicInteger                     threadIndex       = new AtomicInteger( 0 );
  private final static ConcurrentMap<String, ThreadPool> execServices      = new ConcurrentHashMap<String, ThreadPool>( );
  
  public static ThreadPool lookup( final Class<? extends ComponentId> group, final Class owningClass ) {
    return lookup( ComponentIds.lookup( group ).name( ) + ":"
                   + owningClass.getSimpleName( ) );
  }
  
  public static ThreadPool lookup( final ServiceConfiguration config ) {
    return lookup( config.getComponentId( ).getClass( ), config.getClass( ), config.getFullName( ).toString( ) );
  }
  
  public static ThreadPool lookup( final Class<? extends ComponentId> group, final Class owningClass, final String name ) {
    return lookup( ComponentIds.lookup( group ).name( ) + ":"
                   + owningClass.getSimpleName( )
                   + ":"
                   + name );
  }
  
  public static ThreadPool lookup( final Class<? extends ComponentId> group ) {
    return lookup( ComponentIds.lookup( group ).name( ) );
  }
  
  private static ThreadPool lookup( final String threadGroupName ) {
    final String groupName = PREFIX + threadGroupName;
    if ( execServices.containsKey( groupName ) ) {
      return execServices.get( groupName );
    } else {
      LOG.trace( "CREATE thread threadpool named: " + groupName );
      final ThreadPool f = new ThreadPool( groupName );
      if ( execServices.putIfAbsent( f.getName( ), f ) != null ) {
        LOG.warn( "SHUTDOWN:" + f.getName( )
                  + " Freeing duplicate thread pool..." );
        f.free( );
      }
    }
    return execServices.get( groupName );
  }
  
  private static final ThreadPool SYSTEM = lookup( "SYSTEM" );
  
  public static Thread newThread( final Runnable r, final String name ) {
    LOG.debug( "CREATE new thread named: " + name
               + " using: "
               + r.getClass( ) );
    return new Thread( SYSTEM.getGroup( ), r, name );
  }
  
  public static Thread newThread( final Runnable r ) {
    LOG.debug( "CREATE new thread using: " + r.getClass( ) );
    return new Thread( SYSTEM.getGroup( ), r );
  }
  
  public static class ThreadPool implements ThreadFactory, ExecutorService {
    private final ThreadGroup                    group;
    private final String                         name;
    private ExecutorService                      pool;
    private Integer                              numThreads = -1;
    private final StackTraceElement[]            creationPoint;
    private final LinkedBlockingQueue<Future<?>> taskQueue  = new LinkedBlockingQueue<Future<?>>( );
    
    private ThreadPool( final String groupPrefix, final Integer threadCount ) {
      this( groupPrefix );
      this.numThreads = threadCount;
    }
    
    private ThreadPool( final String groupPrefix ) {
      this.creationPoint = Thread.currentThread( ).getStackTrace( );
      this.name = groupPrefix;
      this.group = new ThreadGroup( this.name );
      this.pool = this.makePool( );
      OrderedShutdown.registerPostShutdownHook( new Runnable( ) {
        @Override
        public void run( ) {
          LOG.warn( "SHUTDOWN:" + ThreadPool.this.name
                    + " Stopping thread pool..." );
          if ( ThreadPool.this.pool != null ) {
            ThreadPool.this.free( );
          }
        }
      } );
    }
    
    public ThreadPool limitTo( final Integer numThreads ) {
      if ( this.numThreads.equals( numThreads ) ) {
        return this;
      } else {
        synchronized ( this ) {
          if ( this.numThreads.equals( numThreads ) ) {
            return this;
          } else {
            this.numThreads = numThreads;
            final ExecutorService oldExec = this.pool;
            this.pool = null;
            if ( oldExec != null ) {
              oldExec.shutdown( );
            }
            this.pool = this.makePool( );
          }
        }
      }
      return this;
    }
    
    public ThreadGroup getGroup( ) {
      return this.group;
    }
    
    public String getName( ) {
      return this.name;
    }
    
    public ExecutorService getExecutorService( ) {
      if ( this.pool != null ) {
        return this.pool;
      } else {
        synchronized ( this ) {
          if ( ( this.pool == null ) ) {
            this.pool = makePool( );
          }
        }
        return this;
      }
    }
    
    public ExecutorService makePool( ) {
      ExecutorService newPool = ( this.numThreads == -1 )
        ? Executors.newCachedThreadPool( this )
        : Executors.newFixedThreadPool( this.numThreads, this );
      if ( newPool instanceof ThreadPoolExecutor ) {
        ( ( ThreadPoolExecutor ) newPool ).setRejectedExecutionHandler( new ThreadPoolExecutor.CallerRunsPolicy( ) );
      }
      return newPool;
    }
    
    private static final Runnable[] EMPTY = new Runnable[] {};
    
    public List<Runnable> free( ) {
      List<Runnable> ret = Lists.newArrayList( );
      for ( final Runnable r : ( ret = this.pool.shutdownNow( ) ) ) {
        LOG.warn( "SHUTDOWN:" + ThreadPool.this.name
                  + " - Pending task: "
                  + r.getClass( )
                  + " ["
                  + r.toString( )
                  + "]" );
      }
      try {
        for ( int i = 0; ( i < 10 ) && !this.pool.awaitTermination( 1, TimeUnit.SECONDS ); i++ ) {
          LOG.warn( "SHUTDOWN:" + ThreadPool.this.name
                    + " - Waiting for pool to shutdown." );
          if ( i > 2 ) {
            LOG.warn( Joiner.on( "\n\t\t" ).join( this.creationPoint ) );
          }
        }
      } catch ( final InterruptedException e ) {
        Thread.currentThread( ).interrupt( );
        LOG.error( e, e );
      }
      return ret;
    }
    
    @Override
    public Thread newThread( final Runnable r ) {
      return new Thread( this.group, r, this.group.getName( ) + "."
                                        + r.getClass( )
                                        + "#"
                                        + Threads.threadIndex.incrementAndGet( ) );
    }
    
    @Override
    public void execute( final Runnable command ) {
      this.pool.execute( command );
    }
    
    @Override
    public void shutdown( ) {
      this.pool.shutdown( );
      execServices.remove( this.getName( ) );
    }
    
    @Override
    public List<Runnable> shutdownNow( ) {
      execServices.remove( this.getName( ) );
      return this.free( );
    }
    
    @Override
    public boolean isShutdown( ) {
      return this.pool.isShutdown( );
    }
    
    @Override
    public boolean isTerminated( ) {
      return this.pool.isTerminated( );
    }
    
    @Override
    public boolean awaitTermination( final long timeout, final TimeUnit unit ) throws InterruptedException {
      return this.pool.awaitTermination( timeout, unit );
    }
    
    @Override
    public <T> Future<T> submit( final Callable<T> task ) {
      return this.pool.submit( task );
    }
    
    @Override
    public <T> Future<T> submit( final Runnable task, final T result ) {
      return this.pool.submit( task, result );
    }
    
    @Override
    public Future<?> submit( final Runnable task ) {
      return this.pool.submit( task );
    }
    
    @Override
    public <T> List<Future<T>> invokeAll( final Collection<? extends Callable<T>> tasks ) throws InterruptedException {
      return this.pool.invokeAll( tasks );
    }
    
    @Override
    public <T> List<Future<T>> invokeAll( final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit ) throws InterruptedException {
      return this.pool.invokeAll( tasks, timeout, unit );
    }
    
    @Override
    public <T> T invokeAny( final Collection<? extends Callable<T>> tasks ) throws InterruptedException, ExecutionException {
      return this.pool.invokeAny( tasks );
    }
    
    @Override
    public <T> T invokeAny( final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit ) throws InterruptedException, ExecutionException, TimeoutException {
      return this.pool.invokeAny( tasks, timeout, unit );
    }
    
    @Override
    public Thread newThread( final Runnable r, final String name ) {
      return this.newThread( this.group, r, name );
    }
    
    @Override
    public Thread newThread( final ThreadGroup group, final Runnable r, final String name ) {
      return new Thread( group, r, this.group.getName( ) + "."
                                   + r.getClass( ).getName( )
                                   + "#"
                                   + Threads.threadIndex.incrementAndGet( )
                                   + "#"
                                   + name );
    }
    
    @Override
    public void setPattern( final String pattern ) {}
    
    @Override
    public void setIncludeClusterName( final boolean includeClusterName ) {}
    
    @Override
    public void setClusterName( final String channelName ) {}
    
    /**
     * TODO: DOCUMENT
     * 
     * @see org.jgroups.util.ThreadFactory#setAddress(java.lang.String)
     * @param address
     */
    @Override
    public void setAddress( final String address ) {}
    
    @Override
    public void renameThread( final String base_name, final Thread thread ) {
      thread.setName( base_name );
    }
    
    private <T> LinkedBlockingQueue<Future<?>> getTaskQueue( ) {
      return this.taskQueue;
    }
  }
  
  public static ExecutorService currentThreadExecutor( ) {
    return new AbstractExecutorService( ) {
      private final Lock      lock         = new ReentrantLock( );
      private final Condition termination  = this.lock.newCondition( );
      private int             runningTasks = 0;
      private boolean         shutdown     = false;
      
      @Override
      public void execute( final Runnable command ) {
        this.startTask( );
        try {
          command.run( );
        } finally {
          this.endTask( );
        }
      }
      
      /*@Override*/
      @Override
      public boolean isShutdown( ) {
        this.lock.lock( );
        try {
          return this.shutdown;
        } finally {
          this.lock.unlock( );
        }
      }
      
      /*@Override*/
      @Override
      public void shutdown( ) {
        this.lock.lock( );
        try {
          this.shutdown = true;
        } finally {
          this.lock.unlock( );
        }
      }
      
      // See sameThreadExecutor javadoc for unusual behavior of this method.
      /*@Override*/
      @Override
      public List<Runnable> shutdownNow( ) {
        this.shutdown( );
        return Collections.emptyList( );
      }
      
      /*@Override*/
      @Override
      public boolean isTerminated( ) {
        this.lock.lock( );
        try {
          return this.shutdown && ( this.runningTasks == 0 );
        } finally {
          this.lock.unlock( );
        }
      }
      
      /*@Override*/
      @Override
      public boolean awaitTermination( final long timeout, final TimeUnit unit ) throws InterruptedException {
        long nanos = unit.toNanos( timeout );
        this.lock.lock( );
        try {
          for ( ;; ) {
            if ( this.isTerminated( ) ) {
              return true;
            } else if ( nanos <= 0 ) {
              return false;
            } else {
              nanos = this.termination.awaitNanos( nanos );
            }
          }
        } finally {
          this.lock.unlock( );
        }
      }
      
      /**
       * Checks if the executor has been shut down and increments the running task count.
       * 
       * @throws RejectedExecutionException
       *           if the executor has been previously shutdown
       */
      private void startTask( ) {
        this.lock.lock( );
        try {
          if ( this.isShutdown( ) ) {
            throw new RejectedExecutionException( "Executor already shutdown" );
          }
          this.runningTasks++;
        } finally {
          this.lock.unlock( );
        }
      }
      
      /**
       * Decrements the running task count.
       */
      private void endTask( ) {
        this.lock.lock( );
        try {
          this.runningTasks--;
          if ( this.isTerminated( ) ) {
            this.termination.signalAll( );
          }
        } finally {
          this.lock.unlock( );
        }
      }
    };
  }
  
  enum StackTraceElementTransform implements Function<StackTraceElement, CharSequence> {
    FQNAME {
      @Override
      public CharSequence apply( StackTraceElement input ) {
        return input.getClassName( );
      }
    },
    FILENAME {
      @Override
      public CharSequence apply( StackTraceElement input ) {
        return input.getFileName( );
      }
    };
    public abstract CharSequence apply( StackTraceElement input );
  }
  
  public static Predicate<StackTraceElement> filterStackByQualifiedName( final String pattern ) {
    return filterStack( pattern, StackTraceElementTransform.FQNAME );
  }
  
  public static Predicate<StackTraceElement> filterStackByFileName( final String pattern ) {
    return filterStack( pattern, StackTraceElementTransform.FQNAME );
  }
  
  public static Predicate<StackTraceElement> filterStack( final String pattern, final Function<StackTraceElement, CharSequence> toMatch ) {
    return new Predicate<StackTraceElement>( ) {
      final Pattern p = Pattern.compile( pattern );
      
      @Override
      public boolean apply( StackTraceElement input ) {
        return p.matcher( toMatch.apply( input ) ).matches( );
      }
    };
  }
  
  public static Collection<StackTraceElement> filteredStack( Predicate<StackTraceElement> filter ) {
    return Collections2.filter( Arrays.asList( Thread.currentThread( ).getStackTrace( ) ), filter );
  }
  
  public static StackTraceElement currentStackFrame( final int offset ) {
    return Thread.currentThread( ).getStackTrace( )[2 + offset];
  }
  
  public static StackTraceElement currentStackFrame( ) {
    return Thread.currentThread( ).getStackTrace( )[2];
  }
  
  public static String currentStackRange( int start, int end ) {
    final StackTraceElement[] stack = Thread.currentThread( ).getStackTrace( );
    final int len = stack.length;
    start = Ints.min( Ints.max( 1, start ), len - 1 );
    end = Ints.min( Ints.max( 1, end ), len - 1 );
    return Joiner.on( "\t\n" ).join( Arrays.copyOfRange( stack, start, end ) );
  }
  
  public static String currentStackString( ) {
    return currentStackRange( 0, Integer.MAX_VALUE );
  }
  
  private static final ConcurrentMap<String, Queue<?>> workers = Maps.newConcurrentMap( );
  private static final AtomicLong                      currId  = new AtomicLong( 0 );
  
  static class Queue<T extends HasFullName<T>> implements Runnable {
    private final AtomicBoolean                running  = new AtomicBoolean( true );
    private final BlockingQueue<FutureTask<?>> msgQueue = new LinkedTransferQueue<FutureTask<?>>( );
    private final T                            owner;
    private final Class<?>                     ownerType;
    private final int                          numWorkers;
    private final String                       creationStack;
    private final Class<? extends ComponentId> componentId;
    private final String                       name;
    private FutureTask<?>                      currentTask;
    
    Queue( final Class<? extends ComponentId> componentId, final T owner, final int numWorkers ) {
      this.componentId = componentId;
      this.owner = owner;
      this.ownerType = owner.getClass( );
      this.name = owner.getFullName( ).toString( );
      this.numWorkers = numWorkers;
      this.creationStack = Threads.currentStackString( );
    }
    
    private boolean start( ) {
      this.threadPool( ).limitTo( this.numWorkers );
      if ( workers.putIfAbsent( this.key( ), this ) != null ) {
        this.stop( );
        return false;
      } else {
        for ( int i = 0; i < this.numWorkers; i++ ) {
          this.threadPool( ).submit( this );
        }
        return true;
      }
    }
    
    private String key( ) {
      return this.componentId.getSimpleName( ) + ":"
             + this.ownerType.getSimpleName( )
             + ":"
             + this.owner.getFullName( )
             + "[workers]";
    }
    
    private void stop( ) {
      this.running.set( false );
    }
    
    private ThreadPool threadPool( ) {
      return Threads.lookup( this.componentId, this.owner.getClass( ), this.name );
    }
    
    private <C> Future<C> submit( final Runnable run ) {
      final GenericCheckedListenableFuture<C> f = new GenericCheckedListenableFuture<C>( );
      final Callable<C> call = new Callable<C>( ) {
        
        @Override
        public C call( ) throws Exception {
          try {
            run.run( );
            f.set( null );
          } catch ( Exception ex ) {
            f.setException( ex );
          }
          return null;
        }
        
        @Override
        public String toString( ) {
          return run.toString( ) + super.toString( );
        }
      };
      return submit( call );
    }
    
    private <C> Future<C> submit( final Callable<C> call ) {
      FutureTask<C> f = new FutureTask<C>( call ) {
        @Override
        public String toString( ) {
          return Thread.currentThread( ).getName( ) + ":" + super.toString( ) + " " + call.toString( );
        }
      };
      this.msgQueue.add( f );
      return f;
    }
    
    @Override
    public void run( ) {
      do {
        try {
          final FutureTask<?> futureTask = this.msgQueue.take( );
          if ( futureTask != null ) {
            Logs.extreme( ).debug( EventType.QUEUE + " " + ( this.currentTask = futureTask ) + " " + Thread.currentThread( ).getName( ) );
            try {
              futureTask.run( );
            } catch ( final Exception ex ) {
              Exceptions.maybeInterrupted( ex );
              Logs.extreme( ).error( ex, ex );
            }
          }
        } catch ( final InterruptedException e ) {
          Exceptions.maybeInterrupted( e );
          break;
        } finally {
          this.currentTask = null;
        }
      } while ( !this.msgQueue.isEmpty( ) || this.running.get( ) );
      Logs.extreme( ).debug( "Shutting down worker: " + this.owner
                 + ":"
                 + this.name
                 + " in thread "
                 + Thread.currentThread( ).getName( ) );
    }
    
    private Object getOwner( ) {
      return this.owner;
    }
    
    private String getCreationStack( ) {
      return this.creationStack;
    }
    
    private AtomicBoolean getRunning( ) {
      return this.running;
    }
    
    private int getNumWorkers( ) {
      return this.numWorkers;
    }
    
    private Class<? extends ComponentId> getComponentId( ) {
      return this.componentId;
    }
    
    private String getName( ) {
      return this.name;
    }
    
    @Override
    public String toString( ) {
      final StringBuilder builder = new StringBuilder( );
      builder.append( "QueuedWorker " );
      if ( this.componentId != null ) builder.append( this.componentId.getSimpleName( ) ).append( " " );
      if ( this.name != null ) builder.append( " " ).append( this.name ).append( ":" );
      builder.append( this.numWorkers ).append( ":" );
      if ( this.running != null ) builder.append( this.running.get( )
        ? "RUNNING"
        : "STOPPED" );
      if ( this.msgQueue != null ) builder.append( ":[" ).append( this.msgQueue.size( ) ).append( "]" );
      return builder.toString( );
    }
  }
  
  static String key( final Class<? extends ComponentId> compId, final Object o ) {
    return ( o instanceof HasFullName
      ? o.getClass( ).toString( ) + ":"
        + ( ( HasFullName ) o ).getFullName( ).toString( )
      : ( o instanceof Class
        ? ( ( Class ) o ).getCanonicalName( )
        : o.toString( ) ) );
  }
  
  private static <T extends HasFullName<T>> Queue<T> queue( final Class<? extends ComponentId> componentId, final T owner, final int numWorkers ) {
    final Queue<T> worker = new Queue<T>( componentId, owner, numWorkers );
    if ( workers.containsKey( worker.key( ) ) ) {
      return ( Queue<T> ) workers.get( worker.key( ) );
    } else {
      if ( !worker.start( ) && workers.containsKey( worker.key( ) ) ) {
        return ( Queue<T> ) workers.get( worker.key( ) );
      } else {
        workers.put( worker.key( ), worker );
        return worker;
      }
    }
  }

  public static <C> Future<C> enqueue( final Class<? extends ComponentId> compId, final Class<?> ownerType, final Callable<C> callable ) {
    return enqueue( compId, ownerType, NUM_QUEUE_WORKERS, callable );
  }
  
  public static <C> Future<C> enqueue( final Class<? extends ComponentId> compId, final Class<?> ownerType, final Integer workers, final Callable<C> callable ) {
    return enqueue( ServiceConfigurations.createBogus( compId, ownerType ), workers, callable );
  }
  
  @SuppressWarnings( "unchecked" )
  public static <C> Future<C> enqueue( final ServiceConfiguration config, final Callable<C> callable ) {
    return ( Future<C> ) queue( config.getComponentId( ).getClass( ), config, NUM_QUEUE_WORKERS ).submit( callable );
  }
  
  @SuppressWarnings( "unchecked" )
  public static <C> Future<C> enqueue( final ServiceConfiguration config, final Integer workers, final Callable<C> callable ) {
    return ( Future<C> ) queue( config.getComponentId( ).getClass( ), config, workers ).submit( callable );
  }
}
