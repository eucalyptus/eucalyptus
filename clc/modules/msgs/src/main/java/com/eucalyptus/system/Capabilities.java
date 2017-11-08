/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
