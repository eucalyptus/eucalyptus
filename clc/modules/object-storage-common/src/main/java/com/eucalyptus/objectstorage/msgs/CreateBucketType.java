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
import com.eucalyptus.storage.msgs.s3.AccessControlList;

@AdminOverrideAllowed
@RequiresPermission( standard = S3PolicySpec.S3_CREATEBUCKET )
@ResourceType( S3PolicySpec.S3_RESOURCE_BUCKET )
@RequiresACLPermission( object = {}, bucket = {}, ownerOf = { ObjectStorageProperties.Resource.bucket } )
public class CreateBucketType extends ObjectStorageRequestType {

  private AccessControlList accessControlList;
  private String locationConstraint;

  public CreateBucketType( ) {
  }

  public CreateBucketType( String bucket ) {
    this.setBucket( bucket );
  }

  public AccessControlList getAccessControlList( ) {
    return this.accessControlList;
  }

  public void setAccessControlList( AccessControlList accessControlList ) {
    this.accessControlList = accessControlList;
  }

  public String getLocationConstraint( ) {
    return this.locationConstraint;
  }

  public void setLocationConstraint( String locationConstraint ) {
    this.locationConstraint = locationConstraint;
  }
}
