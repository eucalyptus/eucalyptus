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
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.system;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import com.google.common.collect.Lists;

public class Threads {
  private static Logger                                  LOG          = Logger.getLogger( Threads.class );
  private final static AtomicInteger                     threadIndex  = new AtomicInteger( 0 );
  private final static ConcurrentMap<String, ThreadPool> execServices = new ConcurrentHashMap<String, ThreadPool>( );
  
  public static ThreadPool lookup( String groupName ) {
    if ( execServices.containsKey( groupName ) ) {
      return execServices.get( groupName );
    } else {
      ThreadPool f = new ThreadPool( groupName );
      if ( execServices.putIfAbsent( groupName, f ) != null ) {
        LOG.warn( "SHUTDOWN:" + f.getName( ) + " Freeing duplicate thread pool..." );
        f.free( );
      }
    }
    return execServices.get( groupName );
  }
  
  private static final ThreadPool SYSTEM = lookup( "SYSTEM" );
  
  public static ExecutorService getThreadPool( String groupName ) {
    return lookup( groupName ).getExecutorService( );
  }
  
  public static Thread newThread( Runnable r, String name ) {
    LOG.debug( "CREATE new thread named: " + name + " using: " + r.getClass( ).getCanonicalName( ) );
    return new Thread( SYSTEM.getGroup( ), r, name );
  }
  
  public static Thread newThread( Runnable r ) {
    LOG.debug( "CREATE new thread using: " + r.getClass( ).getCanonicalName( ) );
    return new Thread( SYSTEM.getGroup( ), r );
  }
  
  public static class ThreadPool implements ThreadFactory, ExecutorService {
    private final ThreadGroup group;
    private final String      name;
    private ExecutorService   pool;
    private Integer           numThreads = -1;
    
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
          if ( this.pool == null && numThreads == -1 ) {
            this.pool = Executors.newCachedThreadPool( this );
          } else {
            this.pool = Executors.newFixedThreadPool( this.numThreads );
          }
        }
        return this.pool;
      }
    }
    
    public List<Runnable> free( ) {
      List<Runnable> ret = Lists.newArrayList( );
      for ( Runnable r : ( ret = this.pool.shutdownNow( ) ) ) {
        LOG.warn( "SHUTDOWN:" + ThreadPool.this.name + " - Discarded pending task: " + r.getClass( ).getCanonicalName( ) + " [" + r.toString( ) + "]" );
      }
      try {
        while( !this.pool.awaitTermination( 1, TimeUnit.SECONDS ) ) {
          LOG.warn(  "SHUTDOWN:" + ThreadPool.this.name + " - Waiting for pool to shutdown." );
        }
      } catch ( InterruptedException e ) {
        LOG.error( e , e );
      }
      return ret;
    }
    
    private ThreadPool( String groupPrefix, Integer threadCount ) {
      this( groupPrefix );
      this.numThreads = threadCount;
    }
    
    private ThreadPool( String groupPrefix ) {
      this.name = "Eucalyptus." + groupPrefix;
      this.group = new ThreadGroup( this.name );
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
    
    @Override
    public Thread newThread( Runnable r ) {
      return new Thread( this.group, r, this.group.getName( ) + "." + r.getClass( ).getCanonicalName( ) + "#" + Threads.threadIndex.incrementAndGet( ) );
    }
    
    /**
     * @param command
     * @see java.util.concurrent.Executor#execute(java.lang.Runnable)
     */
    public void execute( Runnable command ) {
      this.pool.execute( command );
    }
    
    /**
     * 
     * @see java.util.concurrent.ExecutorService#shutdown()
     */
    public void shutdown( ) {
      this.pool.shutdown( );
    }
    
    /**
     * @return
     * @see java.util.concurrent.ExecutorService#shutdownNow()
     */
    public List<Runnable> shutdownNow( ) {
      return this.free( );
    }
    
    /**
     * @return
     * @see java.util.concurrent.ExecutorService#isShutdown()
     */
    public boolean isShutdown( ) {
      return this.pool.isShutdown( );
    }
    
    /**
     * @return
     * @see java.util.concurrent.ExecutorService#isTerminated()
     */
    public boolean isTerminated( ) {
      return this.pool.isTerminated( );
    }
    
    /**
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     * @see java.util.concurrent.ExecutorService#awaitTermination(long,
     *      java.util.concurrent.TimeUnit)
     */
    public boolean awaitTermination( long timeout, TimeUnit unit ) throws InterruptedException {
      return this.pool.awaitTermination( timeout, unit );
    }
    
    /**
     * @param <T>
     * @param task
     * @return
     * @see java.util.concurrent.ExecutorService#submit(java.util.concurrent.Callable)
     */
    public <T> Future<T> submit( Callable<T> task ) {
      return this.pool.submit( task );
    }
    
    /**
     * @param <T>
     * @param task
     * @param result
     * @return
     * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable,
     *      java.lang.Object)
     */
    public <T> Future<T> submit( Runnable task, T result ) {
      return this.pool.submit( task, result );
    }
    
    /**
     * @param task
     * @return
     * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable)
     */
    public Future<?> submit( Runnable task ) {
      return this.pool.submit( task );
    }
    
    /**
     * @param <T>
     * @param tasks
     * @return
     * @throws InterruptedException
     * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection)
     */
    public <T> List<Future<T>> invokeAll( Collection<? extends Callable<T>> tasks ) throws InterruptedException {
      return this.pool.invokeAll( tasks );
    }
    
    /**
     * @param <T>
     * @param tasks
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection,
     *      long, java.util.concurrent.TimeUnit)
     */
    public <T> List<Future<T>> invokeAll( Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit ) throws InterruptedException {
      return this.pool.invokeAll( tasks, timeout, unit );
    }
    
    /**
     * @param <T>
     * @param tasks
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection)
     */
    public <T> T invokeAny( Collection<? extends Callable<T>> tasks ) throws InterruptedException, ExecutionException {
      return this.pool.invokeAny( tasks );
    }
    
    /**
     * @param <T>
     * @param tasks
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection,
     *      long, java.util.concurrent.TimeUnit)
     */
    public <T> T invokeAny( Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit ) throws InterruptedException, ExecutionException, TimeoutException {
      return this.pool.invokeAny( tasks, timeout, unit );
    }
  }
  
}
