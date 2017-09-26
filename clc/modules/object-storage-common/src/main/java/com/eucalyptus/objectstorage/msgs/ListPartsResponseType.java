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
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.Initiator;
import com.eucalyptus.storage.msgs.s3.Part;

public class ListPartsResponseType extends ObjectStorageDataResponseType {

  private String bucket;
  private String key;
  private String uploadId;
  private Initiator initiator;
  private CanonicalUser owner;
  private String storageClass;
  private int partNumberMarker;
  private int nextPartNumberMarker;
  private int maxParts;
  private Boolean isTruncated;
  private ArrayList<Part> parts = new ArrayList<Part>( );

  public String getBucket( ) {
    return bucket;
  }

  public void setBucket( String bucket ) {
    this.bucket = bucket;
  }

  public String getKey( ) {
    return key;
  }

  public void setKey( String key ) {
    this.key = key;
  }

  public String getUploadId( ) {
    return uploadId;
  }

  public void setUploadId( String uploadId ) {
    this.uploadId = uploadId;
  }

  public Initiator getInitiator( ) {
    return initiator;
  }

  public void setInitiator( Initiator initiator ) {
    this.initiator = initiator;
  }

  public CanonicalUser getOwner( ) {
    return owner;
  }

  public void setOwner( CanonicalUser owner ) {
    this.owner = owner;
  }

  public String getStorageClass( ) {
    return storageClass;
  }

  public void setStorageClass( String storageClass ) {
    this.storageClass = storageClass;
  }

  public int getPartNumberMarker( ) {
    return partNumberMarker;
  }

  public void setPartNumberMarker( int partNumberMarker ) {
    this.partNumberMarker = partNumberMarker;
  }

  public int getNextPartNumberMarker( ) {
    return nextPartNumberMarker;
  }

  public void setNextPartNumberMarker( int nextPartNumberMarker ) {
    this.nextPartNumberMarker = nextPartNumberMarker;
  }

  public int getMaxParts( ) {
    return maxParts;
  }

  public void setMaxParts( int maxParts ) {
    this.maxParts = maxParts;
  }

  public Boolean getIsTruncated( ) {
    return isTruncated;
  }

  public void setIsTruncated( Boolean isTruncated ) {
    this.isTruncated = isTruncated;
  }

  public ArrayList<Part> getParts( ) {
    return parts;
  }

  public void setParts( ArrayList<Part> parts ) {
    this.parts = parts;
  }
}
