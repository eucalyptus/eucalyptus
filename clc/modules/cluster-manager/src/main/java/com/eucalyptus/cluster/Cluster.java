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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.auth.Credentials;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.HasName;

import edu.ucsb.eucalyptus.cloud.NodeInfo;
import edu.ucsb.eucalyptus.msgs.RegisterClusterType;

public class Cluster implements HasName {
  private static Logger                            LOG = Logger.getLogger( Cluster.class );
  private MQ                                       mq;
  private ClusterConfiguration                     configuration;
  private ConcurrentNavigableMap<String, NodeInfo> nodeMap;
  private ClusterState                             state;
  private ClusterNodeState                         nodeState;
  private ClusterCredentials                       credentials;

  public Cluster( ClusterConfiguration configuration, ClusterCredentials credentials ) {
    super( );
    this.configuration = configuration;
    this.state = new ClusterState( configuration.getName( ) );
    this.nodeState = new ClusterNodeState( configuration.getName( ) );
    this.nodeMap = new ConcurrentSkipListMap<String, NodeInfo>( );
    this.credentials = credentials;
    this.mq = new MQ();
  }

  public ClusterCredentials getCredentials( ) {
    synchronized ( this ) {
      if ( this.credentials == null ) {
        EntityWrapper<ClusterCredentials> credDb = Credentials.getEntityWrapper( );
        try {
          this.credentials = credDb.getUnique( new ClusterCredentials( this.configuration.getName( ) ) );
        } catch ( EucalyptusCloudException e ) {
          LOG.error( "Failed to load credentials for cluster: " + this.configuration.getName( ) );
        }
        credDb.rollback( );
      }
    }
    return credentials;
  }

  @Override
  public String getName( ) {
    return this.configuration.getName( );
  }

  public NavigableSet<String> getNodeTags( ) {
    return this.nodeMap.navigableKeySet( );
  }

  public NodeInfo getNode( String serviceTag ) {
    return this.nodeMap.get( serviceTag );
  }

  public void updateNodeInfo( List<String> nodeTags ) {
    NodeInfo ret = null;
    for ( String tag : nodeTags )
      if ( ( ret = this.nodeMap.putIfAbsent( tag, new NodeInfo( tag ) ) ) != null ) ret.touch( );
  }

  @Override
  public int compareTo( Object o ) {
    Cluster that = ( Cluster ) o;
    return this.getName( ).compareTo( that.getName( ) );
  }

  public MQ getThreadGroup( ) {
    return mq;
  }

  public ClusterConfiguration getConfiguration( ) {
    return configuration;
  }

  public RegisterClusterType getWeb( ) {
    String host = this.getConfiguration( ).getHostName( );
    int port = 0;
    try {
      URI uri = new URI( this.getConfiguration( ).getUri( ) );
      host = uri.getHost( );
      port = uri.getPort( );
    } catch ( URISyntaxException e ) {
    }
    return new RegisterClusterType( this.getName( ), host, port );
  }

  public ClusterMessageQueue getMessageQueue( ) {
    return this.mq.getMessageQueue( );
  }

  public ClusterState getState( ) {
    return state;
  }

  public ClusterNodeState getNodeState( ) {
    return nodeState;
  }
  
  public void start() {
    this.mq.startMessageQueue( );
  }

  public void stop( ) {
    this.mq.stopMessageQueue( );
  }

  public class MQ extends ThreadGroup {
    private Thread              mqThread;
    private ClusterMessageQueue messageQueue;
    private boolean             stopped = false;

    public MQ( ) {
      super( configuration.getName( ) );
      this.messageQueue = new ClusterMessageQueue( configuration );
    }

    public void startMessageQueue( ) {
      if ( ( this.mqThread == null || !this.mqThread.isAlive( ) ) && !this.stopped ) {
        this.mqThread = new Thread( this.messageQueue );
        LOG.warn( "-> [ " + this.getName( ) + " ] Starting threads " + this.mqThread.getName( ) );
        this.mqThread.start( );
      }
    }

    public void stopMessageQueue( ) {
      this.messageQueue.stop( );
      this.stopped = true;
      LOG.warn( "-> [ " + this.getName( ) + " ] Stopping threads " + this.mqThread );
    }

    @Override
    public void uncaughtException( Thread t, Throwable e ) {
      LOG.error( "Caught exception from " + t.getName( ) + ": " + e.getClass( ) );
      LOG.error( t.getName( ) + ": " + e.getMessage( ), e );
      super.uncaughtException( t, e );
    }

    public ClusterMessageQueue getMessageQueue( ) {
      return messageQueue;
    }

  }

}
