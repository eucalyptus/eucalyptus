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
package com.eucalyptus.cluster;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadFactory;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Authentication;
import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.HasName;
import edu.ucsb.eucalyptus.cloud.NodeInfo;
import edu.ucsb.eucalyptus.msgs.RegisterClusterType;

public class Cluster implements HasName<Cluster> {
  private static Logger                            LOG = Logger.getLogger( Cluster.class );
  private ClusterMessageQueue                      mq;
  private ClusterConfiguration                     configuration;
  private ThreadFactory                            threadFactory;
  private ConcurrentNavigableMap<String, NodeInfo> nodeMap;
  private ClusterState                             state;
  private ClusterNodeState                         nodeState;
  private final ClusterCredentials                       credentials;
  
  public Cluster( ClusterConfiguration configuration, ClusterCredentials credentials ) {
    super( );
    this.configuration = configuration;
    this.state = new ClusterState( configuration.getName( ) );
    this.nodeState = new ClusterNodeState( configuration.getName( ) );
    this.nodeMap = new ConcurrentSkipListMap<String, NodeInfo>( );
    this.credentials = credentials;
    this.mq = new ClusterMessageQueue( configuration.getName( ) );
    this.threadFactory = ClusterThreadFactory.getThreadFactory( this.configuration.getName( ) );
  }
  
  public ClusterCredentials getCredentials( ) {
    return this.credentials;
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
  public int compareTo( Cluster that ) {
    return this.getName( ).compareTo( that.getName( ) );
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
    } catch ( URISyntaxException e ) {}
    return new RegisterClusterType( this.getName( ), host, port );
  }
  
  public ClusterMessageQueue getMessageQueue( ) {
    return this.mq;
  }
  
  public ClusterState getState( ) {
    return state;
  }
  
  public ClusterNodeState getNodeState( ) {
    return nodeState;
  }
  
  public void start( ) {
    this.mq.start( );
  }
  
  public void stop( ) {
    this.mq.stop( );
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( configuration == null ) ? 0 : configuration.hashCode( ) );
    result = prime * result + ( ( state == null ) ? 0 : state.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    Cluster other = ( Cluster ) obj;
    if ( configuration == null ) {
      if ( other.configuration != null ) return false;
    } else if ( !configuration.equals( other.configuration ) ) return false;
    if ( state == null ) {
      if ( other.state != null ) return false;
    } else if ( !state.equals( other.state ) ) return false;
    return true;
  }
  
  public String getUri( ) {
    return configuration.getUri( );
  }
  
  public String getHostName( ) {
    return this.configuration.getHostName( );
  }
  
  public String getInsecureServicePath( ) {
    return this.configuration.getInsecureServicePath( );
  }
  
  public Integer getPort( ) {
    return this.configuration.getPort( );
  }
  
  public String getServicePath( ) {
    return this.configuration.getServicePath( );
  }
  
  public ThreadFactory getThreadFactory( ) {
    return this.threadFactory;
  }
  
  @Override
  public String toString( ) {
    return String.format( "Cluster [configuration=%s, credentials=%s, mq=%s, nodeMap=%s, nodeState=%s, state=%s]", this.configuration, this.credentials,
                          this.mq, this.nodeMap, this.nodeState, this.state );
  }
  
}
