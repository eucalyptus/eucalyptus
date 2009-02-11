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
import edu.ucsb.eucalyptus.transport.Axis2MessageDispatcher;
import edu.ucsb.eucalyptus.transport.client.*;
import edu.ucsb.eucalyptus.transport.config.Key;
import edu.ucsb.eucalyptus.transport.util.Defaults;
import edu.ucsb.eucalyptus.util.BaseDirectory;
import org.apache.log4j.Logger;
import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;

import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClusterMessageQueue implements Runnable {

  private static Logger LOG = Logger.getLogger( ClusterMessageQueue.class );
  private Cluster parent;
  private BasicClient client;
  private BasicClient logClient;
  private BlockingQueue<QueuedEvent> msgQueue;
  private int offerInterval = 10;
  private int pollInterval = 10;
  private final int messageQueueSize = 100;
  private AtomicBoolean finished;

  public ClusterMessageQueue( Cluster parent )
  {
    this.parent = parent;
    this.finished = new AtomicBoolean( false );
    this.msgQueue = new LinkedBlockingQueue<QueuedEvent>(messageQueueSize);
    OutboundEndpoint endpoint = Defaults.getDefaultOutboundEndpoint( this.parent.getClusterInfo().getUri(), ClusterInfo.NAMESPACE, 15, 10, 20 );
    Axis2MessageDispatcherFactory clientFactory = new Axis2MessageDispatcherFactory();
    try
    {
      Axis2MessageDispatcher dispatcher = ( Axis2MessageDispatcher ) clientFactory.create( endpoint );
      this.client = dispatcher.getClient();
    }
    catch ( MuleException e )
    {
      LOG.error( e, e );
    }
    OutboundEndpoint logEndpoint = Defaults.getDefaultOutboundEndpoint( this.parent.getClusterInfo().getUri().replaceAll( "EucalyptusCC", "EucalyptusGL" ), ClusterInfo.NAMESPACE, 15, 10, 20 );
    logEndpoint.getProperties().remove( Key.WSSEC_POLICY.getKey() );
    logEndpoint.getProperties().put( Key.WSSEC_POLICY.getKey(), BaseDirectory.CONF.toString() + File.separator + "off-policy.xml" );
    try
    {
      Axis2MessageDispatcher dispatcher = ( Axis2MessageDispatcher ) clientFactory.create( logEndpoint );
      this.logClient = dispatcher.getClient();
    }
    catch ( MuleException e )
    {
      LOG.error( e, e );
    }
    LOG.info( "Created message queue for cluster " + this.parent.getClusterInfo().getName() );

  }

  public void enqueue( QueuedEvent event )
  {
    LOG.info( "Queued message of type " + event.getCallback().getClass().getSimpleName() + " for cluster " + this.parent.getClusterInfo().getName() );
    boolean inserted = false;
    while ( !inserted )
      try
      {
        if ( this.msgQueue.contains( event ) ) return;
        if(this.msgQueue.offer( event, offerInterval, TimeUnit.MILLISECONDS ))
	        inserted = true;
      }
      catch ( InterruptedException e )
      {
        LOG.error( e, e );
      }
  }

  public void stop()
  {
    this.finished.lazySet( true );
  }

  public void run()
  {
    while ( !finished.get() )
      try
      {
        long start = System.currentTimeMillis();
        //:: consume a message from the request queue :://
        QueuedEvent event = this.msgQueue.poll( pollInterval, TimeUnit.MILLISECONDS );
        if ( event != null ) // msg == null if the queue was empty and we timed out
        {
          LOG.warn( "Dequeued message of type " + event.getCallback().getClass().getSimpleName() );
          long msgStart = System.currentTimeMillis();
          try {
            event.trigger( event instanceof QueuedLogEvent ? this.logClient : this.client );
          } catch ( Exception e ) {
            LOG.error( e );
          } finally {
            event.getCallback().notifyHandler();
          }
          LOG.warn( String.format( "[q=%04dms,send=%04dms,qlen=%02d] message type %s, cluster %s",
                                   msgStart -start, System.currentTimeMillis() - msgStart, this.msgQueue.size(),
                                   event.getCallback().getClass().getSimpleName(), this.parent.getClusterInfo().getName() ) );
        }
      }
      catch ( Exception e )
      {
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
