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

import java.util.ArrayList;
import java.util.List;
import com.eucalyptus.objectstorage.policy.AdminOverrideAllowed;
import com.eucalyptus.objectstorage.policy.RequiresACLPermission;
import com.eucalyptus.objectstorage.policy.RequiresPermission;
import com.eucalyptus.objectstorage.policy.ResourceType;
import com.eucalyptus.objectstorage.policy.S3PolicySpec;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;

@AdminOverrideAllowed
@RequiresPermission( standard = S3PolicySpec.S3_PUTOBJECT )
@ResourceType( S3PolicySpec.S3_RESOURCE_OBJECT )
@RequiresACLPermission( object = {}, bucket = { ObjectStorageProperties.Permission.WRITE } )
public class PostObjectType extends ObjectStorageDataRequestType {

  private String contentLength;
  private ArrayList<MetaDataEntry> metaData = new ArrayList<>( );
  private AccessControlList accessControlList = new AccessControlList( );
  private String storageClass;
  private String successActionRedirect;
  private Integer successActionStatus;
  private String contentType;

  public String getContentLength( ) {
    return contentLength;
  }

  public void setContentLength( String contentLength ) {
    this.contentLength = contentLength;
  }

  public ArrayList<MetaDataEntry> getMetaData( ) {
    return metaData;
  }

  public void setMetaData( ArrayList<MetaDataEntry> metaData ) {
    this.metaData = metaData;
  }

  public AccessControlList getAccessControlList( ) {
    return accessControlList;
  }

  public void setAccessControlList( AccessControlList accessControlList ) {
    this.accessControlList = accessControlList;
  }

  public String getStorageClass( ) {
    return storageClass;
  }

  public void setStorageClass( String storageClass ) {
    this.storageClass = storageClass;
  }

  public String getSuccessActionRedirect( ) {
    return successActionRedirect;
  }

  public void setSuccessActionRedirect( String successActionRedirect ) {
    this.successActionRedirect = successActionRedirect;
  }

  public Integer getSuccessActionStatus( ) {
    return successActionStatus;
  }

  public void setSuccessActionStatus( Integer successActionStatus ) {
    this.successActionStatus = successActionStatus;
  }

  public String getContentType( ) {
    return contentType;
  }

  public void setContentType( String contentType ) {
    this.contentType = contentType;
  }
}
