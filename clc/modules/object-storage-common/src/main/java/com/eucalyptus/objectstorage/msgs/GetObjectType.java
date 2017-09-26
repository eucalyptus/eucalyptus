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
@RequiresPermission( standard = S3PolicySpec.S3_GETOBJECT, version = S3PolicySpec.S3_GETOBJECTVERSION )
@ResourceType( S3PolicySpec.S3_RESOURCE_OBJECT )
@RequiresACLPermission( object = { ObjectStorageProperties.Permission.READ }, bucket = {} )
public class GetObjectType extends ObjectStorageDataGetRequestType {

  private Boolean getMetaData;
  private Boolean inlineData;
  private Boolean deleteAfterGet;
  private Boolean getTorrent;

  public GetObjectType( ) {
  }

  public GetObjectType( final String bucketName, final String key, final Boolean getMetaData, final Boolean inlineData ) {
    super( bucketName, key );
    this.getMetaData = getMetaData;
    this.inlineData = inlineData;
  }

  public Boolean getGetMetaData( ) {
    return getMetaData;
  }

  public void setGetMetaData( Boolean getMetaData ) {
    this.getMetaData = getMetaData;
  }

  public Boolean getInlineData( ) {
    return inlineData;
  }

  public void setInlineData( Boolean inlineData ) {
    this.inlineData = inlineData;
  }

  public Boolean getDeleteAfterGet( ) {
    return deleteAfterGet;
  }

  public void setDeleteAfterGet( Boolean deleteAfterGet ) {
    this.deleteAfterGet = deleteAfterGet;
  }

  public Boolean getGetTorrent( ) {
    return getTorrent;
  }

  public void setGetTorrent( Boolean getTorrent ) {
    this.getTorrent = getTorrent;
  }
}
