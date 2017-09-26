/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.objectstorage.msgs;

import com.eucalyptus.objectstorage.policy.AdminOverrideAllowed;
import com.eucalyptus.objectstorage.policy.RequiresACLPermission;
import com.eucalyptus.objectstorage.policy.RequiresPermission;
import com.eucalyptus.objectstorage.policy.ResourceType;
import com.eucalyptus.objectstorage.policy.S3PolicySpec;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;

@AdminOverrideAllowed
@RequiresPermission( standard = S3PolicySpec.S3_LISTMULTIPARTUPLOADPARTS )
@ResourceType( S3PolicySpec.S3_RESOURCE_OBJECT )
@RequiresACLPermission( object = {}, bucket = {}, ownerOf = { ObjectStorageProperties.Resource.bucket, ObjectStorageProperties.Resource.object } )
public class ListPartsType extends ObjectStorageDataRequestType {

  private String uploadId;
  private String maxParts;
  private String partNumberMarker;

  public String getUploadId( ) {
    return uploadId;
  }

  public void setUploadId( String uploadId ) {
    this.uploadId = uploadId;
  }

  public String getMaxParts( ) {
    return maxParts;
  }

  public void setMaxParts( String maxParts ) {
    this.maxParts = maxParts;
  }

  public String getPartNumberMarker( ) {
    return partNumberMarker;
  }

  public void setPartNumberMarker( String partNumberMarker ) {
    this.partNumberMarker = partNumberMarker;
  }
}
