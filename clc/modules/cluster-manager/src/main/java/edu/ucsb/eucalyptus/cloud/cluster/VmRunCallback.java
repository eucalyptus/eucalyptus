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

import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.VmRunResponseType;
import edu.ucsb.eucalyptus.cloud.VmRunType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesType;

import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.ws.client.Client;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;

import org.apache.log4j.Logger;

class VmRunCallback extends QueuedEventCallback<VmRunType> {

  private static Logger LOG = Logger.getLogger( VmRunCallback.class );

  private ClusterAllocator parent;
  private ResourceToken token;

  public VmRunCallback( final ClusterConfiguration clusterConfig,final ClusterAllocator parent, final ResourceToken token ) {
    super(clusterConfig);
    this.parent = parent;
    this.token = token;
  }

  public void process( final Client clusterClient, final VmRunType msg ) throws Exception {
    LOG.info( String.format( EucalyptusProperties.DEBUG_FSTRING, EucalyptusProperties.TokenState.submitted, token ) );
    Clusters.getInstance().lookup( token.getCluster() ).getNodeState().submitToken( token );
    ClusterConfiguration config = Clusters.getInstance( ).lookup( token.getCluster( ) ).getConfiguration( );
    for ( String vmId : msg.getInstanceIds() )
      parent.msgMap.put( ClusterAllocator.State.ROLLBACK,
                         new QueuedEvent<TerminateInstancesType>(
                             new TerminateCallback( config ),
                             new TerminateInstancesType( vmId, msg ) ) );
    VmRunResponseType reply = null;
    try {
      reply = ( VmRunResponseType ) clusterClient.send( msg );
      Clusters.getInstance().lookup( token.getCluster() ).getNodeState().redeemToken( token );
      LOG.info( String.format( EucalyptusProperties.DEBUG_FSTRING, EucalyptusProperties.TokenState.redeemed, token ) );
      if ( reply.get_return() ) {
        for ( VmInfo vmInfo : reply.getVms() ) {
          VmInstance vm = VmInstances.getInstance().lookup( vmInfo.getInstanceId() );
          vm.getNetworkConfig().setIpAddress( vmInfo.getNetParams().getIpAddress() );
        }
        this.parent.setupAddressMessages( Lists.newArrayList( this.token.getAddresses() ), Lists.newArrayList( reply.getVms() ) );
        for ( VmInfo vmInfo : reply.getVms() ) {
          VmInstance vm = VmInstances.getInstance().lookup( vmInfo.getInstanceId() );
          if( VmInstance.DEFAULT_IP.equals( vm.getNetworkConfig().getIgnoredPublicIp() ) )
            vm.getNetworkConfig().setIgnoredPublicIp( vmInfo.getNetParams().getIgnoredPublicIp() );
        }
      } else {
        this.parent.getRollback().lazySet( true );
      }
    } catch ( Exception e ) { throw e; }
  }

}
