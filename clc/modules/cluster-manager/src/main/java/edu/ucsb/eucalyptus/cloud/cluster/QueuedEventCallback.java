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
/**
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.cloud.cluster;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.client.Client;

public abstract class QueuedEventCallback<TYPE> {
  private static Logger    LOG = Logger.getLogger( QueuedEventCallback.class );
  private Lock             canHas;
  private Condition        jobPending;
  private volatile boolean e   = false;
  private volatile boolean failed   = false;
  private volatile boolean stopped = false;

  protected QueuedEventCallback( ) {
    this.canHas = new ReentrantLock( );
    this.jobPending = canHas.newCondition( );
  }

  public void notifyHandler( ) {
    canHas.lock( );
    e = true;
    this.jobPending.signalAll( );
    canHas.unlock( );
  }

  public void waitForEvent( ) {
    canHas.lock( );
    try {
      while ( !e )
        this.jobPending.await( );
    } catch ( InterruptedException e ) {} finally {
      e = false;
      canHas.unlock( );
    }
  }

  public boolean isStopped( ) {
    return stopped;
  }

  public void stop( ) {
    this.stopped = true;
  }

  protected boolean sleep( int ms ) {
    long startTime = System.currentTimeMillis( );
    while ( ( System.currentTimeMillis( ) - startTime ) < ms )
      try {
        Thread.sleep( 1000 );
      } catch ( InterruptedException e ) {}
    return true;
  }

  public abstract void process( Client cluster, TYPE msg ) throws Exception;

  public abstract static class MultiClusterCallback<TYPE> extends QueuedEventCallback<TYPE> {
    private boolean split = false;    
    public abstract void prepare( TYPE msg ) throws Exception;
    public boolean isSplit( ) {
      return split;
    }
    public void markSplit( ) {
      this.split = true;
    }
    protected void fireEventAsyncToAllClusters( TYPE msg ) {
      for ( Cluster c : Clusters.getInstance( ).listValues( ) ) {
        LOG.info( "-> Sending " + msg.getClass( ).getSimpleName( ) + " network to: " + c.getUri( ) );
        LOG.debug( LogUtil.lineObject( msg ) );
        try {
          c.fireEventAsync( QueuedEvent.make( this, msg ) );
        } catch ( Throwable e ) {
          LOG.error( "Error while sending to: " + c.getUri( ) + " " + msg.getClass( ).getSimpleName( ) );
          LOG.debug( LogUtil.dumpObject( msg ) );
        }
      }      
    }
  }
  
  public boolean isFailed( ) {
    return failed;
  }

  public void setFailed( boolean failed ) {
    this.failed = failed;
  }

}
