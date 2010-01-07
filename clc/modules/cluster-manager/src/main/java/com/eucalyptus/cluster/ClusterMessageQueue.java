/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.cluster;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.eucalyptus.util.LogUtil;

import edu.ucsb.eucalyptus.cloud.cluster.QueuedEvent;
import edu.ucsb.eucalyptus.cloud.cluster.TerminateCallback;
import edu.ucsb.eucalyptus.constants.EventType;
import edu.ucsb.eucalyptus.msgs.EventRecord;

public class ClusterMessageQueue implements Runnable {
  
  private static Logger                    LOG              = Logger.getLogger( ClusterMessageQueue.class );
  private final BlockingQueue<QueuedEvent> msgQueue;
  private final int                        offerInterval    = 500;
  private final int                        pollInterval     = 500;
  private final int                        messageQueueSize = 100;
  private final AtomicBoolean              finished;
  private final String                     clusterName;
  
  public ClusterMessageQueue( final String clusterName ) {
    this.finished = new AtomicBoolean( false );
    this.msgQueue = new LinkedBlockingQueue<QueuedEvent>( );
    this.clusterName = clusterName;
  }
  
  public void enqueue( final QueuedEvent event ) {
    LOG.debug( EventRecord.caller( event.getCallback( ).getClass( ), EventType.MSG_PENDING, this.clusterName, event.getEvent().toString() ) );
    LOG.debug( EventRecord.caller( event.getCallback( ).getClass( ), EventType.MSG_PENDING, this.clusterName, event.getEvent().toString() ), new Exception() );  
    LOG.debug( "Queued message of type " + event.getCallback( ).getClass( ).getSimpleName( ) + " for cluster " + this.getClusterName( ) );
    try {
      while ( !this.msgQueue.offer( event, this.offerInterval, TimeUnit.MILLISECONDS ) );
    } catch ( final InterruptedException e ) {
      LOG.debug( e, e );
      Thread.currentThread().interrupt();
    }
  }
  
  public String getClusterName( ) {
    return this.clusterName;
  }
  
  @SuppressWarnings( "unchecked" )
  public void run( ) {
    while ( !this.finished.get( ) ) {
      try {
        final QueuedEvent event = this.msgQueue.poll( this.pollInterval, TimeUnit.MILLISECONDS );
        final long start = System.currentTimeMillis( );
        if ( event != null ) {// msg == null if the queue was empty
          LOG.debug( "-> Dequeued message of type " + event.getCallback( ).getClass( ).getSimpleName( ) );
          try {
//            event.getCallback( ).send( this.clusterName );
            Clusters.sendClusterEvent( this.clusterName, event );
            event.getCallback( ).waitForResponse( );
            //TODO: handle events which raised I/O exceptions to indicate the cluster state.
          } catch ( final Throwable e ) {
            LOG.debug( e, e );
          }
          LOG.debug( EventRecord.here( event.getCallback( ).getClass( ), EventType.QUEUE, this.clusterName, EventType.QUEUE_TIME.name( ), Long.toString( start - event.getStartTime( ) ), EventType.SERVICE_TIME.name( ), Long.toString( System.currentTimeMillis( ) - start ), EventType.QUEUE_LENGTH.name( ), Long.toString( this.msgQueue.size( ) ) ) );  
        }
      } catch ( final Throwable e ) {
        LOG.error( e, e );
      }
    }
  }
  
  public void stop( ) {
    this.finished.lazySet( true );
  }
  
  @Override
  public String toString( ) {
    return "ClusterMessageQueue{" + "msgQueue=" + this.msgQueue.size( ) + '}';
  }
}
