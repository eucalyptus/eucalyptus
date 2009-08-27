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
package edu.ucsb.eucalyptus.admin.server;

import com.google.common.collect.Sets;
import com.google.gwt.user.client.rpc.SerializableException;
import edu.ucsb.eucalyptus.admin.client.ClusterInfoWeb;
import edu.ucsb.eucalyptus.admin.client.StorageInfoWeb;
import edu.ucsb.eucalyptus.admin.client.VmTypeWeb;
import edu.ucsb.eucalyptus.admin.client.WalrusInfoWeb;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.cluster.VmTypes;
import edu.ucsb.eucalyptus.cloud.entities.VmType;
import edu.ucsb.eucalyptus.msgs.RegisterClusterType;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.ws.util.Messaging;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RemoteInfoHandler {

  private static Logger LOG = Logger.getLogger( RemoteInfoHandler.class );

  public static synchronized void setClusterList( List<ClusterInfoWeb> newClusterList )
  {
    List<RegisterClusterType> list = new ArrayList<RegisterClusterType>();
    for ( ClusterInfoWeb cw : newClusterList ) {
      LOG.info( "Adding cluster for update: " + cw.getName() + " - " + cw.getHost() + ":" + cw.getPort() );
      list.add( new RegisterClusterType( cw.getName(), cw.getHost(), cw.getPort() ) );
    }
    Messaging.dispatch( Component.clusters.getUri( ).toASCIIString( ), list );
  }

  public static synchronized List<ClusterInfoWeb> getClusterList()
  {
    List<ClusterInfoWeb> clusterList = new ArrayList<ClusterInfoWeb>();
    for ( RegisterClusterType c : Clusters.getInstance().getClusters() )
      clusterList.add( new ClusterInfoWeb( c.getName(), c.getHost(), c.getPort()) ); 
    return clusterList;
  }

  public static synchronized void setStorageList( List<StorageInfoWeb> newStorageList )
  {
	  //TODO: Chris do messaging stuff. new UpdateStorageConfigurationType(storageStateType) and send that.
  }

  public static synchronized List<StorageInfoWeb> getStorageList()
  {
    List<StorageInfoWeb> storageList = new ArrayList<StorageInfoWeb>();
	  //TODO: Chris iterate over StorageStateType and construct storageList
    return storageList;
  }

  public static synchronized void setWalrusList( List<WalrusInfoWeb> walrusInfo)
  {
	  //TODO: Chris do messaging stuff. new UpdateWalrusConfigurationType(walrusStateType) and send that.
  }

  public static List<VmTypeWeb> getVmTypes()
  {
    List<VmTypeWeb> ret = new ArrayList<VmTypeWeb>();
    for( VmType v : VmTypes.list() )
      ret.add( new VmTypeWeb( v.getName(), v.getCpu(), v.getMemory(), v.getDisk() ) );
    return ret;
  }

  public static void setVmTypes( final List<VmTypeWeb> vmTypes ) throws SerializableException {
    Set<VmType> newVms = Sets.newTreeSet();
    for ( VmTypeWeb vmw : vmTypes ) {
      newVms.add( new VmType( vmw.getName(), vmw.getCpu(), vmw.getDisk(), vmw.getMemory() ) );
    }
    try {
      VmTypes.update( newVms );
    }
    catch ( EucalyptusCloudException e ) {
      throw new SerializableException( e.getMessage() );
    }
  }
}
