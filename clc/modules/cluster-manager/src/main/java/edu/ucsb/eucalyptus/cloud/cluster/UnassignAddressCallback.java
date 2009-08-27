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

import edu.ucsb.eucalyptus.cloud.entities.Address;
import edu.ucsb.eucalyptus.msgs.UnassignAddressResponseType;
import edu.ucsb.eucalyptus.msgs.UnassignAddressType;

import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.ws.client.Client;
import org.apache.log4j.Logger;

import java.util.NoSuchElementException;

public class UnassignAddressCallback extends QueuedEventCallback<UnassignAddressType> {

  private static Logger LOG = Logger.getLogger( UnassignAddressCallback.class );

  private String pubIp;
  private String vmIp;
  private String vmId;
  
  public UnassignAddressCallback( final ClusterConfiguration clusterConfig, final Address parent ) {
    super(clusterConfig);
    this.vmId = parent.getInstanceId();
    this.pubIp = parent.getName();
    this.vmIp = parent.getInstanceAddress();
  }

  public void process( final Client clusterClient, final UnassignAddressType msg ) throws Exception {
    UnassignAddressResponseType reply = ( UnassignAddressResponseType ) clusterClient.send( msg );
    VmInstance vm = null;
    try {
      vm = VmInstances.getInstance().lookup( vmId );
      LOG.debug( "Unassign [" + pubIp + "] clearing VM " + vmId + ":" + vmIp );
      vm.getNetworkConfig().setIgnoredPublicIp( VmInstance.DEFAULT_IP );
    } catch ( NoSuchElementException e1 ) {}
//    String addr = msg.getSource();
//    try {
//      Address a = Addresses.getInstance().lookup( addr );
//      a.unassign();
//      if( EucalyptusProperties.NAME.equals( a.getUserId() ) ) {
//        new AddressManager().ReleaseAddress( Admin.makeMsg( ReleaseAddressType.class, addr ) );
//      }
//    } catch ( NoSuchElementException e1 ) {
//      LOG.error( e1 );
//    } catch ( EucalyptusCloudException e1 ) {
//      LOG.error( e1 );
//    }
  }

}
