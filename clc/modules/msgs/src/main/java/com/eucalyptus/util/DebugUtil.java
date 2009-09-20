package com.eucalyptus.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.hibernate.ejb.EntityManagerFactoryImpl;

import com.google.common.collect.Multimaps;
import com.google.common.collect.SortedSetMultimap;

public class DebugUtil {
  private static Logger LOG   = Logger.getLogger( DebugUtil.class );
  public static boolean DEBUG = true;

  public static StackTraceElement getMyStackTraceElement( ) {
    Exception e = new Exception( );
    e.fillInStackTrace( );
    for ( StackTraceElement ste : e.getStackTrace( ) ) {
      if ( ste.getClassName( ).startsWith( EntityWrapper.class.getCanonicalName( ).replaceAll( "\\.EntityWrapper.*", "" ) ) || ste.getMethodName( ).equals( "getEntityWrapper" ) ) {
        continue;
      } else {
        return ste;
      }
    }
    throw new RuntimeException( "BUG: Reached bottom of stack trace without finding any relevent frames." );
  }

  public static Throwable checkForCauseOfInterest( Throwable e, Class<? extends Throwable>... interestingExceptions ) {
    Throwable cause = e;
    Throwable rootCause = e;
    List interesting = Arrays.asList( interestingExceptions );
    for ( int i = 0; i < 100 && cause != null; i++, rootCause = cause, cause = cause.getCause( ) ) {
      if ( interesting.contains( cause.getClass( ) ) ) {
        return cause;
      }
    }
    LOG.trace( "-> Ignoring unrelated exception: ex=" + e.getClass( ) + " root=" + rootCause );
    return new ExceptionNotRelatedException( interesting.toString( ), e );
  }
  
  public static void updateThreadStatus() {
    Times.update( );
  }

  static class Times implements Comparable<Times> {
    private static SortedSetMultimap<Long, Times> threadTimers = Multimaps.newTreeMultimap( );
    Long threadId;
    Long timestamp;
    Long cpuTime;
    Long userTime;

    private Times( ) {}

    private Times( Long threadId, Long timestamp, Long cpuTime, Long userTime ) {
      this.threadId = threadId;
      this.timestamp = timestamp;
      this.cpuTime = cpuTime;
      this.userTime = userTime;
    }

    @Override
    public int compareTo( Times o ) {
      return this.getTimestamp( ).compareTo( o.getTimestamp( ) );
    }

    public Long getThreadId( ) {
      return threadId;
    }

    public void setThreadId( long threadId ) {
      this.threadId = threadId;
    }

    public Long getTimestamp( ) {
      return timestamp;
    }

    public void setTimestamp( long timestamp ) {
      this.timestamp = timestamp;
    }

    public Long getCpuTime( ) {
      return cpuTime;
    }

    public void setCpuTime( long cpuTime ) {
      this.cpuTime = cpuTime;
    }

    public Long getUserTime( ) {
      return userTime;
    }

    public void setUserTime( long userTime ) {
      this.userTime = userTime;
    }

    public synchronized static void print( ) {
      for( Long tid : Times.threadTimers.keySet( ) ) {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
        ThreadInfo info = bean.getThreadInfo( tid );
        LOG.debug( LogUtil.subheader( info.getThreadName( ) + " " + info.getStackTrace( )[0] ) );
        for( Times t : threadTimers.get( tid ) ) {
          LOG.debug( LogUtil.dumpObject( t ) );
        }
      }
    }

    @Override
    public int hashCode( ) {
      final int prime = 31;
      int result = 1;
      result = prime * result + ( ( threadId == null ) ? 0 : threadId.hashCode( ) );
      result = prime * result + ( ( timestamp == null ) ? 0 : timestamp.hashCode( ) );
      return result;
    }

    @Override
    public boolean equals( Object obj ) {
      if ( this == obj ) return true;
      if ( obj == null ) return false;
      if ( getClass( ) != obj.getClass( ) ) return false;
      Times other = ( Times ) obj;
      if ( threadId == null ) {
        if ( other.threadId != null ) return false;
      } else if ( !threadId.equals( other.threadId ) ) return false;
      if ( timestamp == null ) {
        if ( other.timestamp != null ) return false;
      } else if ( !timestamp.equals( other.timestamp ) ) return false;
      return true;
    }
    private static Lock timersLock = new ReentrantLock( );
    public static void update( ) {
      ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
      long[] ids = bean.getAllThreadIds( );
      for ( long id : ids ) {
        final long c = bean.getThreadCpuTime( id );
        final long u = bean.getThreadUserTime( id );
        if ( c == -1 || u == -1 ) continue;
        timersLock.lock( );
        try {
          SortedSet<Times> times = threadTimers.get( id );
          threadTimers.put( id, new Times( id, System.currentTimeMillis( ), c, u ) );
          if( times.size( ) > 3 ) {
            times.remove( times.first( ) );
          }
        } finally {
          timersLock.unlock( );
        }
      }
    }

  }

  public static void debug( ) {
    if( DEBUG ) {
      printDebugDetails( );
    }
  }

  public static void printDebugDetails( ) {
    for( String persistenceContext : DatabaseUtil.getPersistenceContexts( ) ) {
      EntityManagerFactoryImpl anemf = ( EntityManagerFactoryImpl ) DatabaseUtil.getEntityManagerFactory( persistenceContext );
      LOG.debug( LogUtil.subheader( persistenceContext + " hibernate statistics: " + anemf.getSessionFactory( ).getStatistics( ) ) );
    }
    Times.print( );
    TxHandle.printTxStatus( );
    DatabaseUtil.printConnectionPoolStatus( );
  }

}
