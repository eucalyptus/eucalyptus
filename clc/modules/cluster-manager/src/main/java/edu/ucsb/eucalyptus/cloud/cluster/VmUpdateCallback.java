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
package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.VmDescribeResponseType;
import edu.ucsb.eucalyptus.cloud.VmDescribeType;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.entities.VmType;
import edu.ucsb.eucalyptus.cloud.ws.SystemState;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.ws.client.Client;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.log4j.Logger;

public class VmUpdateCallback extends QueuedEventCallback<VmDescribeType> implements Runnable {
  private static Logger LOG = Logger.getLogger( VmUpdateCallback.class );
  private static int SLEEP_TIMER = 5 * 1000;

  public VmUpdateCallback( ClusterConfiguration config ) {
    super( config );
  }

  public void process( final Client cluster, final VmDescribeType msg ) throws Exception {
    VmDescribeResponseType reply = ( VmDescribeResponseType ) cluster.send( msg );
    if ( reply != null ) reply.setOriginCluster( super.getConfig( ).getName() );
    for ( VmInfo vmInfo : reply.getVms() ) {
      vmInfo.setPlacement( super.getConfig( ).getName() );
      VmTypeInfo typeInfo = vmInfo.getInstanceType();
      if( typeInfo.getName() == null || "".equals( typeInfo.getName() ) ) {
        for( VmType t : VmTypes.list() ) {
          if( t.getCpu().equals( typeInfo.getCores() ) && t.getDisk().equals( typeInfo.getDisk() ) && t.getMemory().equals( typeInfo.getMemory() ) ) {
            typeInfo.setName( t.getName() );
          }
        }
      }
    }
    SystemState.handle( reply );
  }

  public void run() {
    do {
      Cluster cluster = Clusters.getInstance( ).lookup( this.getConfig( ).getName( ) );
      VmDescribeType msg = new VmDescribeType();
      msg.setUserId( Component.eucalyptus.name( ) );
      msg.setEffectiveUserId( Component.eucalyptus.name( ) );
      cluster.getMessageQueue().enqueue( new QueuedEvent( this, msg ) );
      this.waitForEvent();
    } while ( !this.isStopped() && this.sleep( SLEEP_TIMER ) );
  }
}
