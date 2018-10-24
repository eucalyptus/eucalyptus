/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.walrus.msgs;

import java.util.ArrayList;
import java.util.Date;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;

public class CopyObjectType extends WalrusRequestType {

  private String sourceBucket;
  private String sourceObject;
  private String sourceVersionId;
  private String destinationBucket;
  private String destinationObject;
  private String metadataDirective;
  private ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>( );
  private AccessControlList accessControlList = new AccessControlList( );
  private String copySourceIfMatch;
  private String copySourceIfNoneMatch;
  private Date copySourceIfModifiedSince;
  private Date copySourceIfUnmodifiedSince;

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

  public String getDestinationBucket( ) {
    return destinationBucket;
  }

  public void setDestinationBucket( String destinationBucket ) {
    this.destinationBucket = destinationBucket;
  }

  public String getDestinationObject( ) {
    return destinationObject;
  }

  public void setDestinationObject( String destinationObject ) {
    this.destinationObject = destinationObject;
  }

  public String getMetadataDirective( ) {
    return metadataDirective;
  }

  public void setMetadataDirective( String metadataDirective ) {
    this.metadataDirective = metadataDirective;
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
