/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.entities.ClusterInfo;
import org.apache.log4j.Logger;

import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.ws.client.Client;
import com.eucalyptus.ws.client.NioClient;
import com.eucalyptus.ws.client.pipeline.ClusterClientPipeline;
import com.eucalyptus.ws.client.pipeline.LogClientPipeline;
import com.eucalyptus.ws.handlers.NioResponseHandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClusterMessageQueue implements Runnable {

  private static Logger LOG = Logger.getLogger( ClusterMessageQueue.class );
  private Cluster parent;
  private BlockingQueue<QueuedEvent> msgQueue;
  private int offerInterval = 10;
  private int pollInterval = 10;
  private final int messageQueueSize = 100;
  private AtomicBoolean finished;

  public ClusterMessageQueue( Cluster parent ) {
    this.parent = parent;
    this.finished = new AtomicBoolean( false );
    this.msgQueue = new LinkedBlockingQueue<QueuedEvent>( messageQueueSize );
  }

  public void enqueue( QueuedEvent event ) {
    LOG.info( "Queued message of type " + event.getCallback().getClass().getSimpleName() + " for cluster " + this.parent.getClusterInfo( ).getName( ) );
    boolean inserted = false;
    while ( !inserted )
      try {
        if ( this.msgQueue.contains( event ) ) return;
        if ( this.msgQueue.offer( event, offerInterval, TimeUnit.MILLISECONDS ) )
          inserted = true;
      }
      catch ( InterruptedException e ) {
        LOG.error( e, e );
      }
  }

  public void stop() {
    this.finished.lazySet( true );
  }

  public void run() {
    while ( !finished.get() )
      try {
        long start = System.currentTimeMillis();
        //:: consume a message from the request queue :://
        QueuedEvent event = this.msgQueue.poll( pollInterval, TimeUnit.MILLISECONDS );
        if ( event != null ) // msg == null if the queue was empty and we timed out
        {
          LOG.trace( "Dequeued message of type " + event.getCallback().getClass().getSimpleName() );
          long msgStart = System.currentTimeMillis();
          try {
            ClusterConfiguration parentCluster = this.parent.getClusterInfo();
            Client nioClient = new NioClient( parentCluster.getHostName(), parentCluster.getPort(), parentCluster.getServicePath(),
                                              event instanceof QueuedLogEvent ? new LogClientPipeline( new NioResponseHandler() ) : new ClusterClientPipeline( new NioResponseHandler() ) );
            event.trigger( nioClient );
            this.parent.setReachable( true );
          } catch ( Exception e ) {
            LOG.error( e );
            this.parent.setReachable( false );
          } finally {
            event.getCallback().notifyHandler();
          }
          LOG.warn( String.format( "[q=%04dms,send=%04dms,qlen=%02d] message type %s, cluster %s",
                                   msgStart - start, System.currentTimeMillis() - msgStart, this.msgQueue.size(),
                                   event.getCallback().getClass().getSimpleName(), this.parent.getClusterInfo().getName() ) );
        }
      }
      catch ( Exception e ) {
        LOG.error( e, e );
      }
  }

  @Override
  public String toString() {
    return "ClusterMessageQueue{" +
           "msgQueue=" + msgQueue.size() +
           '}';
  }
}
