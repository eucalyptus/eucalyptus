/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.entities.EntityWrapper;
import edu.ucsb.eucalyptus.cloud.state.Volume;
import edu.ucsb.eucalyptus.cloud.state.Snapshot;
import edu.ucsb.eucalyptus.keys.Hashes;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.util.*;
import org.apache.log4j.Logger;

import java.util.List;

public class SnapshotManager {

  private static Logger LOG = Logger.getLogger( SnapshotManager.class );
  private static String ID_PREFIX = "snap-";
  public static EntityWrapper<Snapshot> getEntityWrapper() {
    return new EntityWrapper<Snapshot>( VolumeManager.PERSISTENCE_CONTEXT );
  }


  public CreateSnapshotResponseType CreateSnapshot( CreateSnapshotType request ) throws EucalyptusCloudException
  {
    EntityWrapper<Snapshot> db = SnapshotManager.getEntityWrapper();
    String userName = request.isAdministrator()?null:request.getUserId();
    Volume vol = db.recast( Volume.class ).getUnique( new Volume( userName, request.getVolumeId() ) );

    String newId = null;
    while ( true ) {
      newId = Hashes.generateId( request.getUserId(), ID_PREFIX );
      try {
        db.getUnique( new Snapshot( null, newId ) );
        break;
      } catch ( EucalyptusCloudException e ) {}
    }

    CreateStorageSnapshotType scRequest = new CreateStorageSnapshotType( newId, vol.getDisplayName() );
    CreateStorageSnapshotResponseType scReply = null;
    try {
      scReply = ( CreateStorageSnapshotResponseType ) Messaging.send( StorageProperties.STORAGE_REF, scRequest );
    } catch ( EucalyptusCloudException e ) {
      LOG.debug( e, e );
      db.rollback();
      throw new EucalyptusCloudException( "Error calling CreateStorageSnapshot:" + e.getMessage() );
    }

    Snapshot snap = new Snapshot( request.getUserId(), newId, vol.getDisplayName() );
    db.add( snap );
    db.commit();

    CreateSnapshotResponseType reply = (CreateSnapshotResponseType ) request.getReply();
    reply.setSnapshot( snap.morph( new edu.ucsb.eucalyptus.msgs.Snapshot() ));

    return reply;
  }

  public DeleteSnapshotResponseType DeleteSnapshot( DeleteSnapshotType request ) throws EucalyptusCloudException
  {
    DeleteSnapshotResponseType reply = ( DeleteSnapshotResponseType ) request.getReply();
    reply.set_return( false );
    EntityWrapper<Snapshot> db = SnapshotManager.getEntityWrapper();
    String userName = request.isAdministrator() ? null : request.getUserId();
    try {
      Snapshot snap = db.getUnique( new Snapshot( userName, request.getSnapshotId() ) );
      //:: TODO-1.5: state checks and snapshot tree check here :://
      Messaging.dispatch( StorageProperties.STORAGE_REF, new DeleteStorageSnapshotType( snap.getDisplayName() ) );
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

  public DescribeSnapshotsResponseType DescribeSnapshots( DescribeSnapshotsType request ) throws EucalyptusCloudException
  {
    DescribeSnapshotsResponseType reply = ( DescribeSnapshotsResponseType ) request.getReply();
    EntityWrapper<Snapshot> db = getEntityWrapper();
    String userName = request.isAdministrator() ? null : request.getUserId();
    List<Snapshot> Snapshots = db.query( new Snapshot( userName, null ) );
    for ( Snapshot v : Snapshots ) {
      if ( request.getSnapshotSet().isEmpty() || request.getSnapshotSet().contains( v.getDisplayName() ) ) {
        reply.getSnapshotSet().add( v.morph( new edu.ucsb.eucalyptus.msgs.Snapshot() ) );
      }
    }
    db.commit();
    return reply;
  }
}
"" +
    ""