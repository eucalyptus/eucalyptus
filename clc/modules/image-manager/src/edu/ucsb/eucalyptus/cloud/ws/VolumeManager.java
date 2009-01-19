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
import edu.ucsb.eucalyptus.cloud.cluster.Clusters;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.msgs.*;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.zip.Adler32;

public class VolumeManager {
  private static Logger LOG = Logger.getLogger( VolumeManager.class );

  public CreateVolumeResponseType CreateVolume( CreateVolumeType request ) throws EucalyptusCloudException
  {
    CreateVolumeResponseType reply = ( CreateVolumeResponseType ) request.getReply();

    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    UserInfo user = null;
    try {
      user = db.getUnique(  new UserInfo( request.getUserId() ) );
    } catch ( EucalyptusCloudException e ) {
      db.rollback();
      throw new EucalyptusCloudException( "User does not exist: " + request.getUserId() );
    }

    if( request.getSnapshotId() != null && !user.getSnapshots().contains( new SnapshotInfo( request.getSnapshotId() ) ) ) {
      db.rollback();
      throw new EucalyptusCloudException( "Snapshot does not exist: " + request.getUserId() );
    }

    if( !Clusters.getInstance().contains( request.getAvailabilityZone() ) ) {
      db.rollback();
      throw new EucalyptusCloudException( "Zone does not exist: " + request.getAvailabilityZone() );
    }

    String newId = null;
    while( true ) {
      newId = generateImageId( request.getUserId() );
      try {
        db.recast( VolumeInfo.class ).getUnique( new VolumeInfo( newId ) );
        break;
      } catch ( EucalyptusCloudException e ) {}
    }

    //:: try to create the volume on the SC :://
    VolumeInfo newVol = new VolumeInfo( newId );
    newVol.setSize( Integer.parseInt( request.getSize() ) );
    newVol.setCreateTime( new Date() );
    newVol.setZone( request.getAvailabilityZone() );
    newVol.setUserName( request.getUserId() );

    user.getVolumes().add( newVol );

    db.commit();

    reply.setVolume( newVol.getAsVolume() );

    return reply;
  }

  private String generateImageId( final String userId ) {
    Adler32 hash = new Adler32();
    String key = userId + System.currentTimeMillis();
    hash.update( key.getBytes() );
    String imageId = String.format( "evs-%08X", hash.getValue() );
    return imageId;
  }


  public DeleteVolumeResponseType DeleteVolume( DeleteVolumeType request ) throws EucalyptusCloudException
  {
    DeleteVolumeResponseType reply = ( DeleteVolumeResponseType ) request.getReply();
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    UserInfo user = null;
    try {
      user = db.getUnique(  new UserInfo( request.getUserId() ) );
    } catch ( EucalyptusCloudException e ) {
      db.rollback();
      throw new EucalyptusCloudException( "User does not exist: " + request.getUserId() );
    }

    return reply;
  }

  public DescribeVolumesResponseType DescribeVolumes( DescribeVolumesType request ) throws EucalyptusCloudException
  {
    DescribeVolumesResponseType reply = ( DescribeVolumesResponseType ) request.getReply();
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    UserInfo user = null;
    try {
      user = db.getUnique(  new UserInfo( request.getUserId() ) );
    } catch ( EucalyptusCloudException e ) {
      db.rollback();
      throw new EucalyptusCloudException( "User does not exist: " + request.getUserId() );
    }
    return reply;
  }

  public AttachVolumeResponseType AttachVolume( AttachVolumeType request ) throws EucalyptusCloudException
  {
    AttachVolumeResponseType reply = ( AttachVolumeResponseType ) request.getReply();
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    UserInfo user = null;
    try {
      user = db.getUnique(  new UserInfo( request.getUserId() ) );
    } catch ( EucalyptusCloudException e ) {
      db.rollback();
      throw new EucalyptusCloudException( "User does not exist: " + request.getUserId() );
    }
    return reply;
  }

  public DetachVolumeResponseType DetachVolume( DetachVolumeType request ) throws EucalyptusCloudException
  {
    DetachVolumeResponseType reply = ( DetachVolumeResponseType ) request.getReply();
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    UserInfo user = null;
    try {
      user = db.getUnique(  new UserInfo( request.getUserId() ) );
    } catch ( EucalyptusCloudException e ) {
      db.rollback();
      throw new EucalyptusCloudException( "User does not exist: " + request.getUserId() );
    }
    return reply;
  }

}

