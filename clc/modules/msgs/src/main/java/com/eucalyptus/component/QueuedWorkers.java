/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
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

package com.eucalyptus.component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.system.Threads;
import com.eucalyptus.system.Threads.ThreadPool;
import com.google.common.collect.Maps;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

public class QueuedWorkers {
  private static final ConcurrentMap<Class, QueuedWorker> workers = Maps.newConcurrentMap( );
  private static final AtomicLong                         currId  = new AtomicLong( 0 );
  private static Logger                                   LOG     = Logger.getLogger( QueuedWorkers.class );
  
  static class QueuedWorker implements Runnable {
    private final AtomicBoolean           running  = new AtomicBoolean( true );
    private final BlockingQueue<Runnable> msgQueue = new LinkedBlockingQueue<Runnable>( );
    private final Long                    id       = currId.incrementAndGet( );
    private final Class<?>                owner;
    private final int                     numWorkers;
    private final String                  creationStack;
    
    QueuedWorker( final Class<?> owner, final int numWorkers ) {
      this.owner = owner;
      this.numWorkers = numWorkers;
      this.creationStack = Threads.currentStackString( );
      this.lookupThreadPool( ).limitTo( numWorkers );
    }
    
    private boolean start( ) {
      if ( workers.putIfAbsent( this.owner, this ) != null ) {
        this.stop( );
        return false;
      } else {
        for ( int i = 0; i < this.numWorkers; i++ ) {
          this.lookupThreadPool( ).submit( this );
        }
        return true;
      }
    }
    
    private void stop( ) {
      this.running.set( false );
      this.lookupThreadPool( ).free( );
    }
    
    private ThreadPool lookupThreadPool( ) {
      return Threads.lookup( Empyrean.class, this.owner, "work-queue-" + this.id );
    }
    
    public void submit( final Runnable run ) {
      this.msgQueue.add( run );
    }
    
    @Override
    public void run( ) {
      while ( !this.msgQueue.isEmpty( ) || this.running.get( ) ) {
        Runnable event;
        try {
          if ( ( event = this.msgQueue.poll( 2000, TimeUnit.MILLISECONDS ) ) != null ) {
            event.run( );
          }
        } catch ( final InterruptedException e1 ) {
          Thread.currentThread( ).interrupt( );
          return;
        } catch ( final Throwable e ) {
          LOG.error( e, e );
        }
      }
      LOG.debug( "Shutting down worker: " + this.owner.getSimpleName( ) + " in thread " + Thread.currentThread( ).getName( ) );
    }
    
    @Override
    public String toString( ) {
      final StringBuilder builder = new StringBuilder( );
      builder.append( "QueuedWorker:" );
      if ( this.owner != null ) builder.append( this.owner ).append( " " );
      if ( this.id != null ) builder.append( "id=" ).append( this.id ).append( " " );
      builder.append( "#" ).append( this.numWorkers ).append( " " );
      if ( this.running != null ) builder.append( "up=" ).append( this.running ).append( ":" );
      if ( this.msgQueue != null ) builder.append( "queue=" ).append( this.msgQueue );
      return builder.toString( );
    }
    
    private Class<?> getOwner( ) {
      return this.owner;
    }
    
    private String getCreationStack( ) {
      return this.creationStack;
    }
  }
  
  public static QueuedWorker newInstance( final Class owner, final int numWorkers ) {
    if ( workers.containsKey( owner ) ) {
      return workers.get( owner );
    } else {
      QueuedWorker worker = new QueuedWorker( owner, numWorkers );
      if ( !worker.start( ) && workers.containsKey( owner ) ) {
        worker = workers.get( worker );
      }
      return worker;
    }
  }
}
