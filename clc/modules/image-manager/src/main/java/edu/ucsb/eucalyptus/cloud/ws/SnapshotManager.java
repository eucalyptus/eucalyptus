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
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 *
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package edu.ucsb.eucalyptus.cloud.ws;

import com.google.common.collect.Lists;
import com.eucalyptus.auth.Hashes;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.config.StorageControllerConfiguration;
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

    StorageControllerConfiguration sc;
    try {
      sc = Configuration.getStorageControllerConfiguration( vol.getCluster( ) );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException("Failed to find the storage controller information for volume: " + vol.getDisplayName( ) + " at " + vol.getCluster( ), e );
    }

    CreateStorageSnapshotType scRequest = new CreateStorageSnapshotType( vol.getDisplayName(), newId );
    CreateStorageSnapshotResponseType scReply = null;
    try {
      scReply = StorageProxy.send( sc.getHostName( ), scRequest, CreateStorageSnapshotResponseType.class );
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
      db.delete( snap );
      db.getSession( ).flush( );
      StorageProxy.dispatchAll( new DeleteStorageSnapshotType( snap.getDisplayName() ) );
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
    String userName = request.isAdministrator() ? null : request.getUserId();

    EntityWrapper<Snapshot> db = getEntityWrapper();
    List<Snapshot> snapshots = db.query( Snapshot.ownedBy( userName ) );

    for ( Snapshot v : snapshots ) {
      DescribeStorageSnapshotsType scRequest = new DescribeStorageSnapshotsType( Lists.newArrayList( v.getDisplayName() ) );
      if ( request.getSnapshotSet().isEmpty() || request.getSnapshotSet().contains( v.getDisplayName() ) ) {
        try {
          StorageControllerConfiguration sc = Configuration.getStorageControllerConfiguration( v.getCluster( ) );
          DescribeStorageSnapshotsResponseType snapshotInfo = StorageProxy.send( sc.getHostName( ), scRequest, DescribeStorageSnapshotsResponseType.class );
          for( StorageSnapshot storageSnapshot : snapshotInfo.getSnapshotSet() ) {
            v.setMappedState( storageSnapshot.getStatus() );
            edu.ucsb.eucalyptus.msgs.Snapshot snapReply = v.morph( new edu.ucsb.eucalyptus.msgs.Snapshot() );
            if( storageSnapshot.getProgress() != null )
              snapReply.setProgress( storageSnapshot.getProgress() );
            snapReply.setVolumeId( storageSnapshot.getVolumeId() );
            reply.getSnapshotSet().add( snapReply );
          }
        } catch ( EucalyptusCloudException e ) {
          LOG.warn( "Error getting snapshot information from the Storage Controller: " + e );
          LOG.debug( e, e );
        }
      }
    }
    db.commit();
    LOG.trace( "RESPONSE ============\n" + reply );
    return reply;
  }
}
