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
package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.msgs.*;

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.ws.client.Client;
import org.apache.log4j.Logger;


import java.util.NoSuchElementException;

public class StopNetworkCallback extends QueuedEventCallback<StopNetworkType> {

  private static Logger LOG = Logger.getLogger( StopNetworkCallback.class );

  private NetworkToken token;

  public StopNetworkCallback( final ClusterConfiguration clusterConfig, final NetworkToken networkToken ) {
    super(clusterConfig);
    this.token = networkToken;
  }

  public void process( final Client clusterClient, final StopNetworkType msg ) throws Exception {
    LOG.debug( "Sending stopNetwork for " + token.getName() + " on cluster " + token.getCluster() );
    try {
      for ( VmInstance v : VmInstances.getInstance().listValues() ) {
        if ( v.getNetworkNames().contains( token.getName() ) && v.getPlacement().equals( token.getCluster() ) ) return;
      }

      StopNetworkResponseType reply = ( StopNetworkResponseType ) clusterClient.send( msg );
      try {
        Network net = Networks.getInstance().lookup( token.getName() );
        Cluster cluster = Clusters.getInstance().lookup( token.getCluster() );
        LOG.debug( "Releasing network token back to cluster: " + token );
        cluster.getState().releaseNetworkAllocation( token );
        LOG.debug( "Removing network token: " + token );
        net.removeToken( token.getCluster() );
      } catch ( NoSuchElementException e1 ) {
        LOG.error( e1 );
      }
    } catch ( Exception e ) {
      LOG.error( e );
      LOG.debug( e, e );
    }
  }

}
