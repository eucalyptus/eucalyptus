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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import org.jgroups.util.ThreadFactory;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.google.common.collect.Lists;

public class Threads {
  private static Logger                                  LOG          = Logger.getLogger( Threads.class );
  private final static String                            PREFIX       = "Eucalyptus.";
  private final static AtomicInteger                     threadIndex  = new AtomicInteger( 0 );
  private final static ConcurrentMap<String, ThreadPool> execServices = new ConcurrentHashMap<String, ThreadPool>( );
  
  public static ThreadPool lookup( Class<? extends ComponentId> group, Class owningClass ) {
    return lookup( ComponentIds.lookup( group ).name( ) + "." + owningClass.getSimpleName( ) );
  }

  public static ThreadPool lookup( Class<? extends ComponentId> group, Class owningClass, String name ) {
    return lookup( ComponentIds.lookup( group ).name( ) + "." + owningClass.getSimpleName( ) + "." + name );
  }

  public static ThreadPool lookup( Class<? extends ComponentId> group ) {
    return lookup( ComponentIds.lookup( group ).name( ) );
  }
  
  private static ThreadPool lookup( String threadGroupName ) {
    String groupName = PREFIX + threadGroupName;
    if ( execServices.containsKey( groupName ) ) {
      return execServices.get( groupName );
    } else {
      LOG.debug( "CREATE thread threadpool named: " + groupName );
      ThreadPool f = new ThreadPool( groupName );
      if ( execServices.putIfAbsent( f.getName( ), f ) != null ) {
        LOG.warn( "SHUTDOWN:" + f.getName( ) + " Freeing duplicate thread pool..." );
        f.free( );
      }
    }
    return execServices.get( groupName );
  }
  
  private static final ThreadPool SYSTEM = lookup( "SYSTEM" );
  
  public static Thread newThread( Runnable r, String name ) {
    LOG.debug( "CREATE new thread named: " + name + " using: " + r.getClass( ) );
    return new Thread( SYSTEM.getGroup( ), r, name );
  }
  
  public static Thread newThread( Runnable r ) {
    LOG.debug( "CREATE new thread using: " + r.getClass( ) );
    return new Thread( SYSTEM.getGroup( ), r );
  }
  
  public static class ThreadPool implements ThreadFactory, ExecutorService {
    private final ThreadGroup group;
    private final String      clusterName = "";
    private final String      prefix      = "Eucalyptus.";
    private final String      name;
    private ExecutorService   pool;
    private Integer           numThreads  = -1;
    
    private ThreadPool( String groupPrefix, Integer threadCount ) {
      this( groupPrefix );
      this.numThreads = threadCount;
    }
    
    private ThreadPool( String groupPrefix ) {
      this.name = groupPrefix;
      this.group = new ThreadGroup( this.name );
      this.pool = Executors.newCachedThreadPool( );
      Runtime.getRuntime( ).addShutdownHook( new Thread( ) {
        @Override
        public void run( ) {
          LOG.warn( "SHUTDOWN:" + ThreadPool.this.name + " Stopping thread pool..." );
          if ( ThreadPool.this.pool != null ) {
            ThreadPool.this.free( );
          }
        }
      } );
      
    }
    
    public ThreadPool limitTo( Integer numThreads ) {
      if ( this.numThreads.equals( numThreads ) ) {
        return this;
      } else {
        synchronized ( this ) {
          if ( this.numThreads.equals( numThreads ) ) {
            return this;
          } else {
            this.numThreads = numThreads;
            ExecutorService oldExec = this.pool;
            this.pool = null;
            if ( oldExec != null ) {
              oldExec.shutdown( );
            }
            if ( numThreads == -1 ) {
              this.pool = Executors.newCachedThreadPool( this );
            } else {
              this.pool = Executors.newFixedThreadPool( this.numThreads );
            }
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
          if ( this.pool == null && this.numThreads == -1 ) {
            this.pool = Executors.newCachedThreadPool( this );
          } else {
            this.pool = Executors.newFixedThreadPool( this.numThreads );
          }
        }
        return this;
      }
    }
    
    private static final Runnable[] EMPTY = new Runnable[] {};
    
    public List<Runnable> free( ) {
      execServices.remove( this.getName( ) );
      List<Runnable> ret = Lists.newArrayList( );
      for ( Runnable r : ( ret = this.pool.shutdownNow( ) ) ) {
        LOG.warn( "SHUTDOWN:" + ThreadPool.this.name + " - Discarded pending task: " + r.getClass( ) + " [" + r.toString( ) + "]" );
      }
      try {
        while ( !this.pool.awaitTermination( 1, TimeUnit.SECONDS ) ) {
          LOG.warn( "SHUTDOWN:" + ThreadPool.this.name + " - Waiting for pool to shutdown." );
          if ( this.pool instanceof ThreadPoolExecutor ) {
            ThreadPoolExecutor tpe = ( ThreadPoolExecutor ) this.pool;
            for ( Runnable r : tpe.getQueue( ).toArray( EMPTY ) ) {
              LOG.warn( "SHUTDOWN:" + ThreadPool.this.name + " - " + r.getClass( ) );
            }
          }
        }
      } catch ( InterruptedException e ) {
        Thread.currentThread( ).interrupt( );
        LOG.error( e, e );
      }
      return ret;
    }
    
    @Override
    public Thread newThread( Runnable r ) {
      return new Thread( this.group, r, this.group.getName( ) + "." + r.getClass( ) + "#" + Threads.threadIndex.incrementAndGet( ) );
    }
    
    public void execute( Runnable command ) {
      this.pool.execute( command );
    }
    
    public void shutdown( ) {
      this.pool.shutdown( );
    }
    
    public List<Runnable> shutdownNow( ) {
      return this.free( );
    }
    
    public boolean isShutdown( ) {
      return this.pool.isShutdown( );
    }
    
    public boolean isTerminated( ) {
      return this.pool.isTerminated( );
    }
    
    public boolean awaitTermination( long timeout, TimeUnit unit ) throws InterruptedException {
      return this.pool.awaitTermination( timeout, unit );
    }
    
    public <T> Future<T> submit( Callable<T> task ) {
      LOG.debug( "SUBMIT new runnable at: " + Threads.currentStack( 2 ) );
      return this.pool.submit( task );
    }
    
    public <T> Future<T> submit( Runnable task, T result ) {
      LOG.debug( "SUBMIT new runnable at: " + Threads.currentStack( 2 ) );
      return this.pool.submit( task, result );
    }
    
    public Future<?> submit( Runnable task ) {
      LOG.debug( "SUBMIT new runnable at: " + Threads.currentStack( 2 ) );
      return this.pool.submit( task );
    }
    
    public <T> List<Future<T>> invokeAll( Collection<? extends Callable<T>> tasks ) throws InterruptedException {
      return this.pool.invokeAll( tasks );
    }
    
    public <T> List<Future<T>> invokeAll( Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit ) throws InterruptedException {
      return this.pool.invokeAll( tasks, timeout, unit );
    }
    
    public <T> T invokeAny( Collection<? extends Callable<T>> tasks ) throws InterruptedException, ExecutionException {
      return this.pool.invokeAny( tasks );
    }
    
    public <T> T invokeAny( Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit ) throws InterruptedException, ExecutionException, TimeoutException {
      return this.pool.invokeAny( tasks, timeout, unit );
    }
    
    @Override
    public Thread newThread( Runnable r, String name ) {
      return this.newThread( this.group, r, name );
    }
    
    @Override
    public Thread newThread( ThreadGroup group, Runnable r, String name ) {
      return new Thread( group, r, this.group.getName( ) + "." + r.getClass( ).getName( ) + "#" + Threads.threadIndex.incrementAndGet( ) + "#" + name );
    }
    
    @Override
    public void setPattern( String pattern ) {}
    
    @Override
    public void setIncludeClusterName( boolean includeClusterName ) {}
    
    @Override
    public void setClusterName( String channelName ) {}
    
    /**
     * TODO: DOCUMENT
     * 
     * @see org.jgroups.util.ThreadFactory#setAddress(java.lang.String)
     * @param address
     */
    @Override
    public void setAddress( String address ) {}
    
    /**
     * TODO: DOCUMENT
     * 
     * @see org.jgroups.util.ThreadFactory#renameThread(java.lang.String, java.lang.Thread)
     * @param base_name
     * @param thread
     */
    @Override
    public void renameThread( String base_name, Thread thread ) {}
  }
  
  public static ExecutorService currentThreadExecutor( ) {
    return new AbstractExecutorService( ) {
      private final Lock      lock         = new ReentrantLock( );
      private final Condition termination  = this.lock.newCondition( );
      private int             runningTasks = 0;
      private boolean         shutdown     = false;
      
      public void execute( Runnable command ) {
        startTask( );
        try {
          command.run( );
        } finally {
          endTask( );
        }
      }
      
      /*@Override*/
      public boolean isShutdown( ) {
        this.lock.lock( );
        try {
          return this.shutdown;
        } finally {
          this.lock.unlock( );
        }
      }
      
      /*@Override*/
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
      public List<Runnable> shutdownNow( ) {
        shutdown( );
        return Collections.emptyList( );
      }
      
      /*@Override*/
      public boolean isTerminated( ) {
        this.lock.lock( );
        try {
          return this.shutdown && this.runningTasks == 0;
        } finally {
          this.lock.unlock( );
        }
      }
      
      /*@Override*/
      public boolean awaitTermination( long timeout, TimeUnit unit ) throws InterruptedException {
        long nanos = unit.toNanos( timeout );
        this.lock.lock( );
        try {
          for ( ;; ) {
            if ( isTerminated( ) ) {
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
          if ( isShutdown( ) ) {
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
          if ( isTerminated( ) ) {
            this.termination.signalAll( );
          }
        } finally {
          this.lock.unlock( );
        }
      }
    };
  }

  public static StackTraceElement currentStack( int frameOffset ) {
    return Thread.currentThread( ).getStackTrace( ).length > frameOffset ? Thread.currentThread( ).getStackTrace( )[ 1 ] : null;
  }
}
