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

import edu.ucsb.eucalyptus.msgs.AttachVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.AttachVolumeType;
import edu.ucsb.eucalyptus.msgs.AttachedVolume;

import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.ws.client.Client;
import org.apache.log4j.Logger;

import java.util.NoSuchElementException;

public class VolumeAttachCallback extends QueuedEventCallback<AttachVolumeType> {

  private static Logger LOG = Logger.getLogger( VolumeAttachCallback.class );

  public VolumeAttachCallback( final ClusterConfiguration clusterConfig ) {
    super(clusterConfig);
  }

  public void process( final Client cluster, final AttachVolumeType msg ) throws Exception {
    AttachVolumeResponseType reply = ( AttachVolumeResponseType ) cluster.send( msg );
    if ( !reply.get_return( ) ) {
      LOG.debug( "Trying to remove invalid volume attachment " + msg.getVolumeId( ) + " from instance " + msg.getInstanceId( ) );
      try {
        VmInstance vm = VmInstances.getInstance( ).lookup( msg.getInstanceId( ) );
        AttachedVolume failVol = new AttachedVolume( msg.getVolumeId( ) );
        vm.getVolumes( ).remove( failVol );
        LOG.debug( "Removed failed attachment: " + failVol.getVolumeId( ) + " -> " + vm.getInstanceId( ) );
        LOG.debug( "Final volume attachments for " + vm.getInstanceId( ) + " " + vm.getVolumes( ) );
      } catch ( NoSuchElementException e1 ) {
      }
    } else {
      try {
        VmInstance vm = VmInstances.getInstance( ).lookup( msg.getInstanceId( ) );
        AttachedVolume attachedVol = new AttachedVolume( msg.getVolumeId( ) );
        LOG.debug( "Volumes marked as attached " + vm.getVolumes( ) + " to " + vm.getInstanceId( ) );
        attachedVol.setStatus( "attached" );
      } catch ( NoSuchElementException e1 ) {
      }
    }
  }

}
