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
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.ws;

import com.google.common.collect.Lists;
import com.eucalyptus.auth.Hashes;
import com.eucalyptus.images.StorageProxy;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.StorageProperties;

import edu.ucsb.eucalyptus.cloud.state.Snapshot;
import edu.ucsb.eucalyptus.cloud.state.Volume;
import edu.ucsb.eucalyptus.msgs.CreateSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.CreateSnapshotType;
import edu.ucsb.eucalyptus.msgs.CreateStorageSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.CreateStorageSnapshotType;
import edu.ucsb.eucalyptus.msgs.DeleteSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteSnapshotType;
import edu.ucsb.eucalyptus.msgs.DeleteStorageSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteStorageSnapshotType;
import edu.ucsb.eucalyptus.msgs.DescribeSnapshotsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeSnapshotsType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageSnapshotsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageSnapshotsType;
import edu.ucsb.eucalyptus.msgs.StorageSnapshot;
import com.eucalyptus.ws.util.Messaging;
import org.apache.log4j.Logger;

import java.util.List;

public class SnapshotManager {

  private static Logger LOG = Logger.getLogger( SnapshotManager.class );
  private static String ID_PREFIX = "snap";

  public static EntityWrapper<Snapshot> getEntityWrapper() {
    return new EntityWrapper<Snapshot>( VolumeManager.PERSISTENCE_CONTEXT );
  }

  public CreateSnapshotResponseType CreateSnapshot( CreateSnapshotType request ) throws EucalyptusCloudException {
    EntityWrapper<Snapshot> db = SnapshotManager.getEntityWrapper();
    String userName = request.isAdministrator() ? null : request.getUserId();
    Volume vol = db.recast( Volume.class ).getUnique( Volume.named( userName, request.getVolumeId() ) );

    String newId = null;
    Snapshot snap = null;
    while ( true ) {
      newId = Hashes.generateId( request.getUserId(), ID_PREFIX );
      try {
           db.getUnique( Snapshot.ownedBy( newId ) );
      } catch ( EucalyptusCloudException e ) {
           snap = new Snapshot( request.getUserId(), newId, vol.getDisplayName() );
           db.add( snap );
           break;
      }
    }

    CreateStorageSnapshotType scRequest = new CreateStorageSnapshotType( vol.getDisplayName(), newId );
    CreateStorageSnapshotResponseType scReply = null;
    try {
      scReply = StorageProxy.send( vol.getCluster( ), scRequest, CreateStorageSnapshotResponseType.class );
      snap.setMappedState( scReply.getStatus() );
      LOG.debug(scReply);
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback();
      throw new EucalyptusCloudException( "Error calling CreateStorageSnapshot:" + e.getMessage() );
    }
    db.commit();

    CreateSnapshotResponseType reply = ( CreateSnapshotResponseType ) request.getReply();
    edu.ucsb.eucalyptus.msgs.Snapshot snapMsg = snap.morph( new edu.ucsb.eucalyptus.msgs.Snapshot() );
    snapMsg.setProgress( "0%" );
    reply.setSnapshot( snapMsg );
    return reply;
  }

  public DeleteSnapshotResponseType DeleteSnapshot( DeleteSnapshotType request ) throws EucalyptusCloudException {
    DeleteSnapshotResponseType reply = ( DeleteSnapshotResponseType ) request.getReply();
    reply.set_return( false );
    EntityWrapper<Snapshot> db = SnapshotManager.getEntityWrapper();
    String userName = request.isAdministrator() ? null : request.getUserId();
    try {
      Snapshot snap = db.getUnique( Snapshot.named( userName, request.getSnapshotId() ) );
//TODO: make this multi-cluster aware.
      //      DeleteStorageSnapshotResponseType scReply = Messaging.send( StorageProperties.STORAGE_REF, new DeleteStorageSnapshotType( snap.getDisplayName() ), DeleteStorageSnapshotResponseType.class );
//      LOG.debug( scReply );
      db.delete( snap );
      db.commit();
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback();
      throw new EucalyptusCloudException( "Error deleting storage volume:" + e.getMessage() );
    }
    reply.set_return( true );
    return reply;
  }

  public DescribeSnapshotsResponseType DescribeSnapshots( DescribeSnapshotsType request ) throws EucalyptusCloudException {
    DescribeSnapshotsResponseType reply = ( DescribeSnapshotsResponseType ) request.getReply();
    EntityWrapper<Snapshot> db = getEntityWrapper();
    String userName = request.isAdministrator() ? null : request.getUserId();
    List<Snapshot> snapshots = db.query( Snapshot.ownedBy( userName ) );
    for ( Snapshot v : snapshots ) {
      if ( request.getSnapshotSet().isEmpty() || request.getSnapshotSet().contains( v.getDisplayName() ) ) {
//TODO: make this multi-cluster aware.
        //        DescribeStorageSnapshotsResponseType snapshotInfo = null;
//        try {
//          snapshotInfo = ( DescribeStorageSnapshotsResponseType ) Messaging.send( StorageProperties.STORAGE_REF, new DescribeStorageSnapshotsType( Lists.newArrayList( v.getDisplayName() ) ) );
//          for( StorageSnapshot storageSnapshot : snapshotInfo.getSnapshotSet() ) {
//            v.setMappedState( storageSnapshot.getStatus() );
//            edu.ucsb.eucalyptus.msgs.Snapshot snapReply = v.morph( new edu.ucsb.eucalyptus.msgs.Snapshot() );
//            if( storageSnapshot.getProgress() != null )
//              snapReply.setProgress( storageSnapshot.getProgress() );
//            snapReply.setVolumeId( storageSnapshot.getVolumeId() );
//            reply.getSnapshotSet().add( snapReply );
//          }
//        } catch ( EucalyptusCloudException e ) {
//          LOG.debug( e, e );
//          throw e;
//        }
      }
    }
    db.commit();
    LOG.warn( "RESPONSE ============\n" + reply );
    return reply;
  }
}
