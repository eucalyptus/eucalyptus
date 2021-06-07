/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.blockstorage;

import com.eucalyptus.blockstorage.msgs.AttachStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.AttachStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.CloneVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.CloneVolumeType;
import com.eucalyptus.blockstorage.msgs.CreateStorageSnapshotResponseType;
import com.eucalyptus.blockstorage.msgs.CreateStorageSnapshotType;
import com.eucalyptus.blockstorage.msgs.CreateStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.CreateStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.DeleteStorageSnapshotResponseType;
import com.eucalyptus.blockstorage.msgs.DeleteStorageSnapshotType;
import com.eucalyptus.blockstorage.msgs.DeleteStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.DeleteStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageSnapshotsResponseType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageSnapshotsType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageVolumesResponseType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageVolumesType;
import com.eucalyptus.blockstorage.msgs.DetachStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.DetachStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.ExportVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.ExportVolumeType;
import com.eucalyptus.blockstorage.msgs.GetStorageConfigurationResponseType;
import com.eucalyptus.blockstorage.msgs.GetStorageConfigurationType;
import com.eucalyptus.blockstorage.msgs.GetStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.GetStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.GetVolumeTokenResponseType;
import com.eucalyptus.blockstorage.msgs.GetVolumeTokenType;
import com.eucalyptus.blockstorage.msgs.ModifyStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.ModifyStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.UnexportVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.UnexportVolumeType;
import com.eucalyptus.blockstorage.msgs.UpdateStorageConfigurationResponseType;
import com.eucalyptus.blockstorage.msgs.UpdateStorageConfigurationType;
import com.eucalyptus.util.EucalyptusCloudException;

/**
 *
 */
public interface BlockStorageService {

  UpdateStorageConfigurationResponseType UpdateStorageConfiguration( UpdateStorageConfigurationType request )
      throws EucalyptusCloudException;

  GetStorageConfigurationResponseType GetStorageConfiguration( GetStorageConfigurationType request )
      throws EucalyptusCloudException;

  GetVolumeTokenResponseType GetVolumeToken( GetVolumeTokenType request ) throws EucalyptusCloudException;

  UnexportVolumeResponseType UnexportVolume( UnexportVolumeType request ) throws EucalyptusCloudException;

  ExportVolumeResponseType ExportVolume( ExportVolumeType request ) throws EucalyptusCloudException;

  GetStorageVolumeResponseType GetStorageVolume( GetStorageVolumeType request ) throws EucalyptusCloudException;

  DeleteStorageVolumeResponseType DeleteStorageVolume( DeleteStorageVolumeType request )
      throws EucalyptusCloudException;

  CreateStorageSnapshotResponseType CreateStorageSnapshot( CreateStorageSnapshotType request )
      throws EucalyptusCloudException;

  DescribeStorageSnapshotsResponseType DescribeStorageSnapshots( DescribeStorageSnapshotsType request )
      throws EucalyptusCloudException;

  DeleteStorageSnapshotResponseType DeleteStorageSnapshot( DeleteStorageSnapshotType request )
      throws EucalyptusCloudException;

  CreateStorageVolumeResponseType CreateStorageVolume( CreateStorageVolumeType request )
      throws EucalyptusCloudException;

  ModifyStorageVolumeResponseType ModifyStorageVolume( ModifyStorageVolumeType request )
      throws EucalyptusCloudException;

  DescribeStorageVolumesResponseType DescribeStorageVolumes( DescribeStorageVolumesType request )
      throws EucalyptusCloudException;

  AttachStorageVolumeResponseType attachVolume( AttachStorageVolumeType request ) throws EucalyptusCloudException;

  DetachStorageVolumeResponseType detachVolume( DetachStorageVolumeType request ) throws EucalyptusCloudException;

  CloneVolumeResponseType CloneVolume( CloneVolumeType request ) throws EucalyptusCloudException;
}
