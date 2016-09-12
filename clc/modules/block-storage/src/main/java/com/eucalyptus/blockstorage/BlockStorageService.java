/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/

package com.eucalyptus.blockstorage;

import com.eucalyptus.blockstorage.msgs.AttachStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.AttachStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.CloneVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.CloneVolumeType;
import com.eucalyptus.blockstorage.msgs.ConvertVolumesResponseType;
import com.eucalyptus.blockstorage.msgs.ConvertVolumesType;
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

  DescribeStorageVolumesResponseType DescribeStorageVolumes( DescribeStorageVolumesType request )
      throws EucalyptusCloudException;

  ConvertVolumesResponseType ConvertVolumes( ConvertVolumesType request ) throws EucalyptusCloudException;

  AttachStorageVolumeResponseType attachVolume( AttachStorageVolumeType request ) throws EucalyptusCloudException;

  DetachStorageVolumeResponseType detachVolume( DetachStorageVolumeType request ) throws EucalyptusCloudException;

  CloneVolumeResponseType CloneVolume( CloneVolumeType request ) throws EucalyptusCloudException;
}
