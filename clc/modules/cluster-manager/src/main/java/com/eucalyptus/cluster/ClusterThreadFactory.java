package com.eucalyptus.cluster;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import com.eucalyptus.system.Threads;

public class ClusterThreadFactory implements ThreadFactory {
  private static Map<String,ThreadFactory> factories = new HashMap<String,ThreadFactory>();
  private final String        threadName;
  private final AtomicInteger threadIndex;
  
  private ClusterThreadFactory( final String threadName ) {
    this.threadName = threadName;
    this.threadIndex = new AtomicInteger( 0 );
  }
  
  @Override
  public Thread newThread( final Runnable r ) {
    return Threads.newThread( r, this.threadName + "-" + r.getClass( ).getSimpleName( ) + "-" + this.threadIndex.addAndGet( 1 )  );
  }
  
  public static ThreadFactory getThreadFactory( String clusterName ) {
    ThreadFactory ret = null;
    if( factories.containsKey( clusterName ) ) {
      ret = factories.get( clusterName );
    } else {
      synchronized(ClusterThreadFactory.class) {
        if( !factories.containsKey( clusterName ) ) {
          factories.put( clusterName, new ClusterThreadFactory( clusterName ) );
        }
        ret = factories.get( clusterName );
      }
    }
    return ret;
  }
  
}
