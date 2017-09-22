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
