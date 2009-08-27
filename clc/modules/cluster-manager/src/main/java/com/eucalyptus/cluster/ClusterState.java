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
package com.eucalyptus.cluster;

import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.log4j.Logger;

import edu.ucsb.eucalyptus.cloud.NetworkToken;
import edu.ucsb.eucalyptus.cloud.cluster.NetworkAlreadyExistsException;
import edu.ucsb.eucalyptus.cloud.cluster.NotEnoughResourcesAvailable;

public class ClusterState {
  private static Logger LOG = Logger.getLogger( ClusterState.class );
  private String clusterName;
  private NavigableSet<Integer> availableVlans;

  public ClusterState( String clusterName ) {
    this.clusterName = clusterName;
    this.availableVlans = new ConcurrentSkipListSet<Integer>();
    for ( int i = 10; i < 4096; i++ ) this.availableVlans.add( i );
  }


  public NetworkToken extantAllocation( String userName, String networkName, int vlan ) throws NetworkAlreadyExistsException {
    NetworkToken netToken = new NetworkToken( this.clusterName, userName, networkName, vlan );
    if ( !this.availableVlans.remove( vlan ) ) {
      throw new NetworkAlreadyExistsException();
    }
    return netToken;
  }

  public NetworkToken getNetworkAllocation( String userName, String networkName ) throws NotEnoughResourcesAvailable {
    if ( this.availableVlans.isEmpty() ) throw new NotEnoughResourcesAvailable();
    int vlan = this.availableVlans.first();
    this.availableVlans.remove( vlan );
    NetworkToken token = new NetworkToken( this.clusterName, userName, networkName, vlan );
    return token;
  }

  public void releaseNetworkAllocation( NetworkToken token ) {
    this.availableVlans.add( token.getVlan() );
  }


  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    ClusterState cluster = ( ClusterState ) o;

    if ( !this.getClusterName( ).equals( cluster.getClusterName( ) ) ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return this.getClusterName( ).hashCode();
  }


  public String getClusterName( ) {
    return clusterName;
  }

  

}
