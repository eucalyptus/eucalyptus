package com.eucalyptus.system;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;

public class Threads {
  private static Logger                                     LOG          = Logger.getLogger( Threads.class );
  private final static AtomicInteger                        threadIndex  = new AtomicInteger( 0 );
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
  private static final ThreadPool SYSTEM = lookup("SYSTEM");

  public static ExecutorService getThreadPool( String groupName ) {
    return lookup( groupName ).getPool( );
  }
  
  
  public static Thread newThread( Runnable r, String name ) {
    return new Thread( SYSTEM.getGroup( ), r, name );
  }
  
  public static Thread newThread( Runnable r ) {
    return new Thread( SYSTEM.getGroup( ), r );
  }
  
  public static class ThreadPool implements ThreadFactory {
    private final ThreadGroup group;
    private final String      name;
    private ExecutorService   pool;
    private Integer numThreads = -1;
    
    public ThreadPool limitTo( Integer numThreads ) {
      if( this.numThreads.equals( numThreads ) ) {
        return this;
      } else {
        synchronized ( this ) {
          if( this.numThreads.equals( numThreads ) ) {
            return this;
          } else {          
            this.numThreads = numThreads;
            ExecutorService oldExec = this.pool;
            this.pool = null;
            if( oldExec != null ) {
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
    
    public ExecutorService getPool( ) {
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
    
    public void free( ) {
      for ( Runnable r : this.pool.shutdownNow( ) ) {
        LOG.warn( "SHUTDOWN:" + ThreadPool.this.name + " - Discarded pending task: " + r.getClass( ).getCanonicalName( ) + " [" + r.toString( ) + "]" );
      }
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
  }
  
}
