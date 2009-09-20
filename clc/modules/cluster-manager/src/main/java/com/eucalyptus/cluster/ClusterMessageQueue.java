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

import java.security.GeneralSecurityException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.ws.client.Client;
import com.eucalyptus.ws.client.NioClient;
import com.eucalyptus.ws.client.pipeline.ClusterClientPipeline;
import com.eucalyptus.ws.client.pipeline.LogClientPipeline;
import com.eucalyptus.ws.client.pipeline.NioClientPipeline;
import com.eucalyptus.ws.handlers.NioResponseHandler;

import edu.ucsb.eucalyptus.cloud.cluster.QueuedEvent;
import edu.ucsb.eucalyptus.cloud.cluster.QueuedEventCallback;
import edu.ucsb.eucalyptus.cloud.cluster.QueuedLogEvent;
import edu.ucsb.eucalyptus.cloud.cluster.StartNetworkCallback;
import edu.ucsb.eucalyptus.cloud.cluster.StopNetworkCallback;

import edu.ucsb.eucalyptus.cloud.cluster.ConfigureNetworkCallback;
import edu.ucsb.eucalyptus.cloud.cluster.QueuedEventCallback.MultiClusterCallback;

public class ClusterMessageQueue implements Runnable {

  private static Logger              LOG              = Logger.getLogger( ClusterMessageQueue.class );
  private BlockingQueue<QueuedEvent> msgQueue;
  private int                        offerInterval    = 500;
  private int                        pollInterval     = 500;
  private final int                  messageQueueSize = 100;
  private AtomicBoolean              finished;
  private ClusterConfiguration       parent;
  private Thread                     thread;

  public ClusterMessageQueue( ClusterConfiguration parent ) {
    this.parent = parent;
    this.finished = new AtomicBoolean( false );
    this.msgQueue = new LinkedBlockingQueue<QueuedEvent>( messageQueueSize );
  }

  public void enqueue( QueuedEvent event ) {
    LOG.debug( "Queued message of type " + event.getCallback( ).getClass( ).getSimpleName( ) + " for cluster " + this.parent.getName( ) );
    boolean inserted = false;
    while ( !inserted )
      try {
        if ( this.msgQueue.contains( event ) ) return;
        if ( this.msgQueue.offer( event, offerInterval, TimeUnit.MILLISECONDS ) ) inserted = true;
      } catch ( InterruptedException e ) {
        LOG.error( e, e );
      }
  }

  public void stop( ) {
    this.finished.lazySet( true );
  }

  @SuppressWarnings( "unchecked" )
  public void run( ) {
    while ( !finished.get( ) ) {
      try {
        long start = System.currentTimeMillis( );
        QueuedEvent event = this.msgQueue.poll( pollInterval, TimeUnit.MILLISECONDS );
        if ( event != null ) // msg == null if the queue was empty
        {
          LOG.trace( "Dequeued message of type " + event.getCallback( ).getClass( ).getSimpleName( ) );
          long msgStart = System.currentTimeMillis( );
          try {
            QueuedEventCallback q = event.getCallback( );
            if( event instanceof QueuedLogEvent ) {
              Client nioClient = getClusterClient( event );
              event.trigger( nioClient );              
            } else {
              if ( q instanceof MultiClusterCallback && !((MultiClusterCallback)q).isSplit( ) ) {
                MultiClusterCallback multi = ( MultiClusterCallback ) q;
                multi.prepare( event.getEvent( ) );
              } else {
                Client nioClient = getClusterClient( event );
                event.trigger( nioClient );
              }
            }
          } catch ( Exception e ) {
            LOG.error( e );
            LOG.debug( e, e );
          }
          LOG.debug( String.format( "[q=%04dms,send=%04dms,qlen=%02d] message type %s, cluster %s", msgStart - start, System.currentTimeMillis( ) - msgStart, this.msgQueue.size( ), event.getCallback( ).getClass( ).getSimpleName( ), this.parent.getName( ) ) );
        }
      } catch ( Exception e ) {
        LOG.error( e, e );
      }
    }
  }

  private NioClient getClusterClient( QueuedEvent event ) throws GeneralSecurityException {
    NioClient nioClient = null;
    NioClientPipeline cp = null; 
    if ( !( event instanceof QueuedLogEvent ) ) {
      cp = new ClusterClientPipeline( new NioResponseHandler( ) );
      nioClient = new NioClient( parent.getHostName(), parent.getPort(), parent.getServicePath(), cp );
    } else {
      cp = new LogClientPipeline( new NioResponseHandler( ) ); 
      nioClient = new NioClient( parent.getHostName(), parent.getPort(), parent.getServicePath(), cp );
    }
    return nioClient;
  }

  @Override
  public String toString( ) {
    return "ClusterMessageQueue{" + "msgQueue=" + msgQueue.size( ) + '}';
  }
}
