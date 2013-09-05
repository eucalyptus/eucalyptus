/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 ************************************************************************/
package com.eucalyptus.system;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.log4j.Logger;
import com.eucalyptus.util.async.Futures;

/**
 * Run tasks with capabilities
 */
public class Capabilities {
  private static final Logger logger = Logger.getLogger( Capabilities.class );

  private static final LinkedBlockingQueue<RunnableFuture<?>> capabilitiesRunnableQueue = new LinkedBlockingQueue<>( );

  private static final ReadWriteLock capabilitiesRunnerLock = new ReentrantReadWriteLock( );
  
  private static volatile Thread capabilitiesRunner = null;

  public static <T> T runWithCapabilities( final Callable<T> callable ) throws Exception {
    checkInitialized( );
    final RunnableFuture<T> future = Futures.resultOf( callable );
    capabilitiesRunnableQueue.add( future );
    return future.get( );
  }

  public static void initialize( ) {
    capabilitiesRunnerLock.writeLock( ).lock( );
    try {
      if ( capabilitiesRunner == null ) {
        final Thread runner = new Thread( new CapabilitiesRunnable( ), "Capabilities-Thread" );
        runner.start( );
        capabilitiesRunner = runner;
      }
    } finally {
      capabilitiesRunnerLock.writeLock( ).unlock( );
    }
  }

  private static void checkInitialized( ) {
    capabilitiesRunnerLock.readLock( ).lock( );
    try {
      if ( capabilitiesRunner == null ) throw new IllegalStateException( "Not initialized" );
    } finally {
      capabilitiesRunnerLock.readLock( ).unlock( );
    }    
  }
  
  private static final class CapabilitiesRunnable implements Runnable {
    @Override
    public void run( ) {
      logger.debug( "Running capabilities executor" );
      while ( true ) {
        try {
          final RunnableFuture<?> startFuture = capabilitiesRunnableQueue.take( );
          if ( startFuture != null ) {
            logger.debug( "Running capabilities task: " + startFuture );
            startFuture.run( );
          }
        } catch ( InterruptedException e ) {
          Thread.interrupted( );
          logger.debug( "Exiting capabilities executor (interrupted)" );
          return;
        }
      }
    }
  }


}
