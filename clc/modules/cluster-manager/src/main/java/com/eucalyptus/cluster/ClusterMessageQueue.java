/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.cluster;

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
import com.eucalyptus.ws.handlers.NioResponseHandler;

import edu.ucsb.eucalyptus.cloud.cluster.QueuedEvent;
import edu.ucsb.eucalyptus.cloud.cluster.QueuedLogEvent;

public class ClusterMessageQueue implements Runnable {

  private static Logger LOG = Logger.getLogger( ClusterMessageQueue.class );
  private BlockingQueue<QueuedEvent> msgQueue;
  private int offerInterval = 10;
  private int pollInterval = 10;
  private final int messageQueueSize = 100;
  private AtomicBoolean finished;
  private ClusterConfiguration parent;
  private Thread thread;
  
  public ClusterMessageQueue( ClusterConfiguration parent ) {
    this.parent = parent;
    this.finished = new AtomicBoolean( false );
    this.msgQueue = new LinkedBlockingQueue<QueuedEvent>( messageQueueSize );
  }

  public void enqueue( QueuedEvent event ) {
    LOG.debug( "Queued message of type " + event.getCallback().getClass().getSimpleName() + " for cluster " + this.parent.getName( ) );
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
            Client nioClient = new NioClient( parent.getHostName(), parent.getPort(), parent.getServicePath(),
                                              event instanceof QueuedLogEvent ? new LogClientPipeline( new NioResponseHandler() ) : new ClusterClientPipeline( new NioResponseHandler() ) );
            event.trigger( nioClient );
          } catch ( Exception e ) {
            LOG.error( e );
          } finally {
            event.getCallback().notifyHandler();
          }
          LOG.debug( String.format( "[q=%04dms,send=%04dms,qlen=%02d] message type %s, cluster %s",
                                   msgStart - start, System.currentTimeMillis() - msgStart, this.msgQueue.size(),
                                   event.getCallback().getClass().getSimpleName(), this.parent.getName() ) );
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
