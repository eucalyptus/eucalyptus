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
import com.eucalyptus.storage.msgs.s3.CommonPrefixesEntry;
import com.eucalyptus.storage.msgs.s3.Upload;

public class ListMultipartUploadsResponseType extends ObjectStorageDataResponseType {

  private String bucket;
  private String keyMarker;
  private String uploadIdMarker;
  private String nextKeyMarker;
  private String nextUploadIdMarker;
  private String delimiter;
  private String prefix;
  private Integer maxUploads;
  private Boolean isTruncated;
  private ArrayList<Upload> uploads = new ArrayList<>( );
  private ArrayList<CommonPrefixesEntry> commonPrefixes = new ArrayList<>( );

  public String getBucket( ) {
    return bucket;
  }

  public void setBucket( String bucket ) {
    this.bucket = bucket;
  }

  public String getKeyMarker( ) {
    return keyMarker;
  }

  public void setKeyMarker( String keyMarker ) {
    this.keyMarker = keyMarker;
  }

  public String getUploadIdMarker( ) {
    return uploadIdMarker;
  }

  public void setUploadIdMarker( String uploadIdMarker ) {
    this.uploadIdMarker = uploadIdMarker;
  }

  public String getNextKeyMarker( ) {
    return nextKeyMarker;
  }

  public void setNextKeyMarker( String nextKeyMarker ) {
    this.nextKeyMarker = nextKeyMarker;
  }

  public String getNextUploadIdMarker( ) {
    return nextUploadIdMarker;
  }

  public void setNextUploadIdMarker( String nextUploadIdMarker ) {
    this.nextUploadIdMarker = nextUploadIdMarker;
  }

  public String getDelimiter( ) {
    return delimiter;
  }

  public void setDelimiter( String delimiter ) {
    this.delimiter = delimiter;
  }

  public String getPrefix( ) {
    return prefix;
  }

  public void setPrefix( String prefix ) {
    this.prefix = prefix;
  }

  public Integer getMaxUploads( ) {
    return maxUploads;
  }

  public void setMaxUploads( Integer maxUploads ) {
    this.maxUploads = maxUploads;
  }

  public Boolean getIsTruncated( ) {
    return isTruncated;
  }

  public void setIsTruncated( Boolean isTruncated ) {
    this.isTruncated = isTruncated;
  }

  public ArrayList<Upload> getUploads( ) {
    return uploads;
  }

  public void setUploads( ArrayList<Upload> uploads ) {
    this.uploads = uploads;
  }

  public ArrayList<CommonPrefixesEntry> getCommonPrefixes( ) {
    return commonPrefixes;
  }

  public void setCommonPrefixes( ArrayList<CommonPrefixesEntry> commonPrefixes ) {
    this.commonPrefixes = commonPrefixes;
  }
}
