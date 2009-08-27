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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.auth.Credentials;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;

import edu.ucsb.eucalyptus.cloud.NodeInfo;
import edu.ucsb.eucalyptus.constants.HasName;
import edu.ucsb.eucalyptus.msgs.RegisterClusterType;

public class Cluster implements HasName {
  private static Logger                            LOG = Logger.getLogger( Cluster.class );
  private ClusterThreadGroup                       threadGroup;
  private ClusterConfiguration                     configuration;
  private ConcurrentNavigableMap<String, NodeInfo> nodeMap;
  private ClusterState                             state;
  private ClusterNodeState                         nodeState;
  private ClusterCredentials                       credentials;
  
  public Cluster( ClusterThreadGroup threadGroup, ClusterConfiguration configuration, ClusterCredentials credentials ) {
    super( );
    this.threadGroup = threadGroup;
    this.configuration = configuration;
    this.state = new ClusterState( configuration.getName( ) );
    this.nodeState = new ClusterNodeState( configuration.getName( ) );
    this.nodeMap = new ConcurrentSkipListMap<String, NodeInfo>( );
    this.credentials = credentials;
  }

  public ClusterCredentials getCredentials( ) {
    synchronized(this) {
      if( this.credentials == null ) {
        EntityWrapper<ClusterCredentials> credDb = Credentials.getEntityWrapper( );
        try {
          this.credentials = credDb.getUnique( new ClusterCredentials(this.configuration.getName( )) );
        } catch ( EucalyptusCloudException e ) {
          LOG.error("Failed to load credentials for cluster: " + this.configuration.getName( ) );
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
  
  public NavigableSet<String> getNodeTags() {
    return this.nodeMap.navigableKeySet();
  }
  
  public NodeInfo getNode( String serviceTag ) {
    return this.nodeMap.get( serviceTag );
  }

  @Override
  public int compareTo( Object o ) {
    Cluster that = ( Cluster ) o;
    return this.getName( ).compareTo( that.getName( ) );
  }

  public ClusterThreadGroup getThreadGroup( ) {
    return threadGroup;
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
    return threadGroup.getMessageQueue( );
  }

  public ClusterState getState( ) {
    return state;
  }

  public ClusterNodeState getNodeState( ) {
    return nodeState;
  }

}
