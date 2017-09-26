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

import java.util.Date;
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
public class GetObjectExtendedType extends ObjectStorageDataGetRequestType {

  private Boolean getMetaData;
  private Boolean inlineData;
  private Long byteRangeStart;
  private Long byteRangeEnd;
  private Date ifModifiedSince;
  private Date ifUnmodifiedSince;
  private String ifMatch;
  private String ifNoneMatch;
  private Boolean returnCompleteObjectOnConditionFailure;

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

  public Long getByteRangeStart( ) {
    return byteRangeStart;
  }

  public void setByteRangeStart( Long byteRangeStart ) {
    this.byteRangeStart = byteRangeStart;
  }

  public Long getByteRangeEnd( ) {
    return byteRangeEnd;
  }

  public void setByteRangeEnd( Long byteRangeEnd ) {
    this.byteRangeEnd = byteRangeEnd;
  }

  public Date getIfModifiedSince( ) {
    return ifModifiedSince;
  }

  public void setIfModifiedSince( Date ifModifiedSince ) {
    this.ifModifiedSince = ifModifiedSince;
  }

  public Date getIfUnmodifiedSince( ) {
    return ifUnmodifiedSince;
  }

  public void setIfUnmodifiedSince( Date ifUnmodifiedSince ) {
    this.ifUnmodifiedSince = ifUnmodifiedSince;
  }

  public String getIfMatch( ) {
    return ifMatch;
  }

  public void setIfMatch( String ifMatch ) {
    this.ifMatch = ifMatch;
  }

  public String getIfNoneMatch( ) {
    return ifNoneMatch;
  }

  public void setIfNoneMatch( String ifNoneMatch ) {
    this.ifNoneMatch = ifNoneMatch;
  }

  public Boolean getReturnCompleteObjectOnConditionFailure( ) {
    return returnCompleteObjectOnConditionFailure;
  }

  public void setReturnCompleteObjectOnConditionFailure( Boolean returnCompleteObjectOnConditionFailure ) {
    this.returnCompleteObjectOnConditionFailure = returnCompleteObjectOnConditionFailure;
  }
}
