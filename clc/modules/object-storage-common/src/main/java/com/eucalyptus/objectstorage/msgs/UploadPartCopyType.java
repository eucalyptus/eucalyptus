/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.objectstorage.msgs;

import com.eucalyptus.objectstorage.policy.AdminOverrideAllowed;
import com.eucalyptus.objectstorage.policy.RequiresACLPermission;
import com.eucalyptus.objectstorage.policy.RequiresPermission;
import com.eucalyptus.objectstorage.policy.ResourceType;
import com.eucalyptus.objectstorage.policy.S3PolicySpec;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.util.Strings;
import com.google.common.collect.Maps;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.util.Date;
import java.util.HashMap;
import javax.annotation.Nullable;

@AdminOverrideAllowed
@RequiresPermission( standard = S3PolicySpec.S3_PUTOBJECT )
@ResourceType( S3PolicySpec.S3_RESOURCE_OBJECT )
@RequiresACLPermission( object = { ObjectStorageProperties.Permission.READ }, bucket = {}, ownerOf = { ObjectStorageProperties.Resource.object } )
public class UploadPartCopyType extends ObjectStorageDataRequestType {

  private String uploadId;
  private String partNumber;
  private HashMap<String, String> copiedHeaders = Maps.newHashMap( );

  private String sourceBucket;
  private String sourceObject;
  private String sourceVersionId;

  private String copySourceRange;
  private String copySourceIfMatch;
  private String copySourceIfNoneMatch;
  private Date copySourceIfModifiedSince;
  private Date copySourceIfUnmodifiedSince;

  public GetObjectType getGetObjectRequest( ) {
    final GetObjectType request = new GetObjectType( );
    request.setBucket( this.sourceBucket );
    request.setKey( this.sourceObject );
    request.setVersionId( this.sourceVersionId );

    // common elements
    request.setCorrelationId( this.getCorrelationId( ) );
    request.setEffectiveUserId( this.getEffectiveUserId( ) );

    return request;
  }

  @Nullable
  public Tuple2<Long,Long> copySourceRangeAsTuple() {
    Tuple2<Long,Long> range = null;
    if (copySourceRange != null) {
      final String bytesRange = Strings.trimPrefix("bytes=", copySourceRange);
      final int dashIndex = bytesRange.indexOf('-');
      if ( dashIndex > -1 && dashIndex < bytesRange.length() - 1) {
        String firstByte = bytesRange.substring(0, dashIndex);
        String lastByte = bytesRange.substring(dashIndex + 1);
        range = Tuple.of(Long.parseLong(firstByte), Long.parseLong(lastByte));
      }
    }
    return range;
  }

  public String getUploadId( ) {
    return uploadId;
  }

  public void setUploadId( String uploadId ) {
    this.uploadId = uploadId;
  }

  public String getPartNumber( ) {
    return partNumber;
  }

  public void setPartNumber( String partNumber ) {
    this.partNumber = partNumber;
  }

  public HashMap<String, String> getCopiedHeaders( ) {
    return copiedHeaders;
  }

  public void setCopiedHeaders( HashMap<String, String> copiedHeaders ) {
    this.copiedHeaders = copiedHeaders;
  }

  public String getSourceBucket( ) {
    return sourceBucket;
  }

  public void setSourceBucket( String sourceBucket ) {
    this.sourceBucket = sourceBucket;
  }

  public String getSourceObject( ) {
    return sourceObject;
  }

  public void setSourceObject( String sourceObject ) {
    this.sourceObject = sourceObject;
  }

  public String getSourceVersionId( ) {
    return sourceVersionId;
  }

  public void setSourceVersionId( String sourceVersionId ) {
    this.sourceVersionId = sourceVersionId;
  }

  public String getCopySourceRange( ) {
    return copySourceRange;
  }

  public void setCopySourceRange( String copySourceRange ) {
    this.copySourceRange = copySourceRange;
  }

  public String getCopySourceIfMatch( ) {
    return copySourceIfMatch;
  }

  public void setCopySourceIfMatch( String copySourceIfMatch ) {
    this.copySourceIfMatch = copySourceIfMatch;
  }

  public String getCopySourceIfNoneMatch( ) {
    return copySourceIfNoneMatch;
  }

  public void setCopySourceIfNoneMatch( String copySourceIfNoneMatch ) {
    this.copySourceIfNoneMatch = copySourceIfNoneMatch;
  }

  public Date getCopySourceIfModifiedSince( ) {
    return copySourceIfModifiedSince;
  }

  public void setCopySourceIfModifiedSince( Date copySourceIfModifiedSince ) {
    this.copySourceIfModifiedSince = copySourceIfModifiedSince;
  }

  public Date getCopySourceIfUnmodifiedSince( ) {
    return copySourceIfUnmodifiedSince;
  }

  public void setCopySourceIfUnmodifiedSince( Date copySourceIfUnmodifiedSince ) {
    this.copySourceIfUnmodifiedSince = copySourceIfUnmodifiedSince;
  }
}
